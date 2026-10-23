package offline;

import no.stelar7.cdragon.types.ogg.OGGParser;
import no.stelar7.cdragon.types.ogg.data.*;
import no.stelar7.cdragon.types.wem.WEMParser;
import no.stelar7.cdragon.types.wem.data.*;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Wwise Vorbis (WEM) to Ogg Vorbis conversion on synthetic WEMs built in code.
 * <p>
 * The expected .ogg files under src/test/resources/offline/audio were produced by ww2ogg 0.24 from the same synthetic WEMs
 * (see the README there), so the conversions are checked byte for byte; every output is also checked structurally
 * (OggS pages, page CRCs, Vorbis header packets, granule positions).
 */
public class OggTest
{
    static final class BitWriter
    {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private       int                   buf;
        private       int                   n;

        void bits(long v, int count)
        {
            for (int i = 0; i < count; i++)
            {
                if (((v >>> i) & 1) != 0)
                {
                    buf |= 1 << n;
                }
                if (++n == 8)
                {
                    out.write(buf);
                    buf = 0;
                    n = 0;
                }
            }
        }

        byte[] bytes()
        {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            o.writeBytes(out.toByteArray());
            if (n > 0)
            {
                o.write(buf);
            }
            return o.toByteArray();
        }
    }

    // ---------------------------------------------------------------- codebook helpers

    static byte[] resource(String name)
    {
        try (InputStream is = OggTest.class.getClassLoader().getResourceAsStream(name))
        {
            assertNotNull(is, "missing resource " + name);
            return is.readAllBytes();
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The stripped (Wwise style) bits of codebook 0 in the aoTuV library, so it can be inlined
     */
    static long[] strippedCodebook0Bits()
    {
        byte[]             lib = resource("codebook/packed_codebooks_aoTuV_603.bin");
        RandomAccessReader r   = new RandomAccessReader(lib, ByteOrder.LITTLE_ENDIAN);
        r.seek(lib.length - 4);
        int tableOffset = r.readInt();
        r.seek(tableOffset);
        int off0 = r.readInt();
        int off1 = r.readInt();
        r.seek(off0);
        CodebookLibrary.rebuild(r, off1 - off0, new OGGStream());
        int                bitCount = r.getTotalBitsRead();
        RandomAccessReader r2       = new RandomAccessReader(lib, ByteOrder.LITTLE_ENDIAN);
        r2.seek(off0);
        long[] bits = new long[bitCount];
        for (int i = 0; i < bitCount; i++)
        {
            bits[i] = r2.readBits(1);
        }
        return bits;
    }

    /**
     * The standard Vorbis bits of codebook 0, as rebuilt from the aoTuV library
     */
    static long[] standardCodebook0Bits()
    {
        OGGStream s = new OGGStream();
        CodebookLibrary.getDefault().rebuild(0, s);
        int    whole = s.getPayloadBytes();
        byte[] page  = s.getPageBuffer();
        long[] bits  = new long[whole * 8 + s.getBitsStored()];
        int    k     = 0;
        for (int i = 0; i < whole; i++)
        {
            int b = page[OGGStream.getHeaderBytes() + OGGStream.getMaxSegments() + i] & 0xFF;
            for (int j = 0; j < 8; j++)
            {
                bits[k++] = (b >>> j) & 1;
            }
        }
        for (int j = 0; j < s.getBitsStored(); j++)
        {
            bits[k++] = (s.getBitBuffer() >>> j) & 1;
        }
        return bits;
    }

    static CodebookLibrary.CodebookInfo info(CodebookLibrary lib, int id)
    {
        return lib.rebuild(id, new OGGStream());
    }

    /**
     * A codebook the synthetic setup can use as residue classbook (2 classifications) and residue book
     */
    static boolean usable(CodebookLibrary.CodebookInfo i)
    {
        return i.lookupType() == 1 && i.dimensions() >= 1 && Math.pow(2, i.dimensions()) <= i.entries();
    }

    /**
     * First id usable in the aoTuV library
     */
    static int aotuvId()
    {
        for (int id = 0; id < 598; id++)
        {
            if (usable(info(CodebookLibrary.getDefault(), id)))
            {
                return id;
            }
        }
        throw new IllegalStateException("no usable aoTuV codebook");
    }

    /**
     * First id that is usable in the standard library but not in the aoTuV library
     */
    static int standardOnlyId()
    {
        for (int id = 0; id < 598; id++)
        {
            if (usable(info(CodebookLibrary.getStandard(), id)) && !usable(info(CodebookLibrary.getDefault(), id)))
            {
                return id;
            }
        }
        throw new IllegalStateException("no standard-only codebook");
    }

    // ---------------------------------------------------------------- synthetic WEM builders

    static void strippedTail(BitWriter w)
    {
        // floors
        w.bits(0, 6);
        w.bits(0, 5); // partitions
        w.bits(0, 3); // class dim
        w.bits(0, 2); // subclasses
        w.bits(0, 8); // subclass book +1
        w.bits(0, 2); // multiplier
        w.bits(0, 4); // rangebits
        // residues
        w.bits(0, 6);
        w.bits(0, 2); // type
        w.bits(0, 24);
        w.bits(0, 24);
        w.bits(0, 24);
        w.bits(1, 6); // 2 classifications
        w.bits(0, 8); // classbook
        // classification 0: low 0, flag 1, high 1 -> cascade 8 (one book)
        w.bits(0, 3);
        w.bits(1, 1);
        w.bits(1, 5);
        // classification 1: low 0, flag 0 -> cascade 0 (high bits must reset)
        w.bits(0, 3);
        w.bits(0, 1);
        w.bits(0, 8); // residue book for cascade[0] bit 3
        // mappings
        w.bits(0, 6);
        w.bits(0, 1); // submaps flag
        w.bits(1, 1); // square polar
        w.bits(0, 8); // 1 coupling step
        w.bits(0, 1); // magnitude
        w.bits(1, 1); // angle
        w.bits(0, 2); // reserved
        w.bits(0, 8); // time
        w.bits(0, 8); // floor
        w.bits(0, 8); // residue
        // modes
        w.bits(1, 6);
        w.bits(0, 1);
        w.bits(0, 8);
        w.bits(1, 1);
        w.bits(0, 8);
    }

    static byte[] setupPacket(String kind, int externalId)
    {
        BitWriter w = new BitWriter();
        w.bits(0, 8); // 1 codebook
        switch (kind)
        {
            case "external", "vorbext" -> w.bits(externalId, 10);
            case "inline" ->
            {
                for (long b : strippedCodebook0Bits())
                {
                    w.bits(b, 1);
                }
            }
            case "full" ->
            {
                for (long b : standardCodebook0Bits())
                {
                    w.bits(b, 1);
                }
            }
            default -> throw new IllegalArgumentException(kind);
        }
        if (kind.equals("full"))
        {
            // time count etc, raw copied
            w.bits(0x2A5, 13);
            w.bits(1, 1);
        } else
        {
            strippedTail(w);
        }
        return w.bytes();
    }

    static byte[] packet(Random rnd, int size, int mode)
    {
        byte[] p = new byte[size];
        rnd.nextBytes(p);
        if (size > 0)
        {
            p[0] = (byte) ((p[0] & 0xFE) | mode);
        }
        return p;
    }

    static final int[] AUDIO_SIZES = {40, 300, 1, 70, 600};

    /**
     * kind: external (fmt chunk of 0x42 bytes, 2 byte packet headers, modified packets), vorbext/inline/full (vorb chunk, 6 byte packet headers)
     */
    static byte[] buildWem(String kind, boolean padded, int externalId)
    {
        Random  rnd   = new Random(kind.hashCode() + externalId);
        LE      data  = new LE();
        byte[]  setup = setupPacket(kind, externalId);
        boolean mod   = kind.equals("external");
        boolean gran  = !mod;

        data.i16(setup.length);
        if (gran)
        {
            data.i32(0);
        }
        data.bytes(setup);
        int   firstAudio = data.size();
        int[] modes      = {1, 0, 1, 1, 0};
        for (int i = 0; i < AUDIO_SIZES.length; i++)
        {
            data.i16(AUDIO_SIZES[i]);
            if (gran)
            {
                data.i32(1000 * (i + 1));
            }
            data.bytes(packet(rnd, AUDIO_SIZES[i], modes[i]));
        }

        LE fmt  = new LE();
        LE vorb = null;
        if (mod)
        {
            fmt.i16(0xFFFF).i16(2).i32(48000).i32(12000).i16(0).i16(0).i16(0x30).i16(0).i32(3);
            fmt.i32(12345).i32(0xD9).i32(0).i32(0).i32(0).i32(firstAudio).i32(0).i32(0).i32(0).i32(0xABCD).u8(8).u8(11);
            assertEquals(0x42, fmt.size());
        } else
        {
            fmt.i16(0xFFFF).i16(2).i32(44100).i32(16000).i16(0).i16(0).i16(0);
            vorb = new LE();
            vorb.i32(12345).zeros(0x18 - 4);
            vorb.i32(0).i32(firstAudio).zeros(0x2C - 0x20);
            vorb.i32(0xABCD).u8(8).u8(11).zeros(0x34 - 0x32);
        }

        LE body = new LE().ascii("WAVE");
        body.ascii("fmt ").i32(fmt.size()).bytes(fmt.toBytes());
        if (padded)
        {
            body.ascii("hash").i32(3).bytes(new byte[]{1, 2, 3}).u8(0);
        } else
        {
            body.ascii("hash").i32(4).bytes(new byte[]{1, 2, 3, 4});
        }
        if (vorb != null)
        {
            body.ascii("vorb").i32(vorb.size()).bytes(vorb.toBytes());
        }
        body.ascii("data").i32(data.size()).bytes(data.toBytes());

        return new LE().ascii("RIFF").i32(body.size()).bytes(body.toBytes()).toBytes();
    }

    /**
     * A wem with the three standard vorbis header packets stored as-is (8 byte packet headers)
     */
    static byte[] buildTriadWem()
    {
        Random rnd  = new Random(7);
        LE     data = new LE();

        byte[] info = new byte[30];
        rnd.nextBytes(info);
        info[0] = 1;
        byte[] comment = new byte[20];
        rnd.nextBytes(comment);
        comment[0] = 3;

        BitWriter w = new BitWriter();
        w.bits(5, 8);
        for (char c : "vorbis".toCharArray())
        {
            w.bits(c, 8);
        }
        w.bits(0, 8);
        for (long b : standardCodebook0Bits())
        {
            w.bits(b, 1);
        }
        w.bits(0x1234567, 27);
        byte[] setup = w.bytes();

        for (byte[] p : new byte[][]{info, comment, setup})
        {
            data.i32(p.length).i32(0).bytes(p);
        }
        int   firstAudio = data.size();
        int[] sizes      = {10, 256, 3};
        for (int i = 0; i < sizes.length; i++)
        {
            data.i32(sizes[i]).i32(500 * (i + 1));
            data.bytes(packet(rnd, sizes[i], 0));
        }

        LE fmt  = new LE().i16(0xFFFF).i16(1).i32(22050).i32(8000).i16(0).i16(0).i16(0);
        LE vorb = new LE().i32(999).zeros(0x18 - 4).i32(0).i32(firstAudio).zeros(0x2C - 0x20);

        LE body = new LE().ascii("WAVE");
        body.ascii("fmt ").i32(fmt.size()).bytes(fmt.toBytes());
        body.ascii("vorb").i32(vorb.size()).bytes(vorb.toBytes());
        body.ascii("data").i32(data.size()).bytes(data.toBytes());
        return new LE().ascii("RIFF").i32(body.size()).bytes(body.toBytes()).toBytes();
    }

    // ---------------------------------------------------------------- ogg structure

    static int bitwiseOggCrc(byte[] b, int len)
    {
        int crc = 0;
        for (int i = 0; i < len; i++)
        {
            crc ^= (b[i] & 0xFF) << 24;
            for (int j = 0; j < 8; j++)
            {
                crc = (crc & 0x80000000) != 0 ? (crc << 1) ^ 0x04C11DB7 : crc << 1;
            }
        }
        return crc;
    }

    record Page(int headerType, long granule, int serial, int sequence) {}

    /**
     * Walks the pages, checks the capture pattern, the CRC (against a bitwise reference) and the sequence numbers, and splits out the packets
     */
    static List<byte[]> checkPages(byte[] ogg, List<Page> pages)
    {
        ByteBuffer            bb      = ByteBuffer.wrap(ogg).order(ByteOrder.LITTLE_ENDIAN);
        List<byte[]>          packets = new ArrayList<>();
        ByteArrayOutputStream cur     = new ByteArrayOutputStream();
        int                   pos     = 0;
        while (pos < ogg.length)
        {
            assertEquals("OggS", new String(ogg, pos, 4, StandardCharsets.US_ASCII), "capture pattern at " + pos);
            assertEquals(0, ogg[pos + 4], "stream structure version");
            int segs = ogg[pos + 26] & 0xFF;
            int len  = 27 + segs;
            for (int i = 0; i < segs; i++)
            {
                len += ogg[pos + 27 + i] & 0xFF;
            }
            byte[] page = Arrays.copyOfRange(ogg, pos, pos + len);
            int    crc  = bb.getInt(pos + 22);
            page[22] = page[23] = page[24] = page[25] = 0;
            assertEquals(bitwiseOggCrc(page, len), crc, "page crc at " + pos);

            Page p = new Page(ogg[pos + 5], bb.getLong(pos + 6), bb.getInt(pos + 14), bb.getInt(pos + 18));
            assertEquals(pages.size(), p.sequence(), "page sequence number");
            pages.add(p);

            int data = pos + 27 + segs;
            for (int i = 0; i < segs; i++)
            {
                int l = ogg[pos + 27 + i] & 0xFF;
                cur.write(ogg, data, l);
                data += l;
                if (l < 255)
                {
                    packets.add(cur.toByteArray());
                    cur.reset();
                }
            }
            pos += len;
        }
        assertEquals(ogg.length, pos, "pages cover the whole file");
        assertEquals(0, cur.size(), "no unfinished packet");
        return packets;
    }

    /**
     * Structural checks of a converted stream: pages, CRCs, BOS/EOS flags, the three vorbis headers, audio packet count and granules
     */
    static void checkStructure(byte[] ogg, int channels, int rate, int audioPackets, boolean synthesizedHeaders)
    {
        List<Page>   pages   = new ArrayList<>();
        List<byte[]> packets = checkPages(ogg, pages);

        assertEquals(2, pages.get(0).headerType(), "first page is BOS");
        assertEquals(4, pages.getLast().headerType() & 4, "last page is EOS");
        for (Page p : pages)
        {
            assertEquals(pages.get(0).serial(), p.serial(), "single logical stream");
        }

        // granule positions never go backwards
        long last = 0;
        for (Page p : pages)
        {
            if (p.granule() != -1)
            {
                assertTrue(p.granule() >= last, "granule decreased");
                last = p.granule();
            }
        }

        assertEquals(3 + audioPackets, packets.size(), "3 headers + audio packets");
        if (!synthesizedHeaders)
        {
            // the header triad is copied from the wem as-is (random bytes in the synthetic test file)
            return;
        }
        for (int i = 0; i < 3; i++)
        {
            byte[] h = packets.get(i);
            assertEquals(2 * i + 1, h[0], "header packet type");
            assertEquals("vorbis", new String(h, 1, 6, StandardCharsets.US_ASCII));
        }

        ByteBuffer id = ByteBuffer.wrap(packets.get(0)).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, id.getInt(7), "vorbis version");
        assertEquals(channels, id.get(11));
        assertEquals(rate, id.getInt(12));
        assertEquals(1, packets.get(0)[29] & 1, "identification framing bit");
    }

    static byte[] convert(byte[] wem)
    {
        WEMFile file = new WEMParser().parse(new ByteArray(wem));
        return new OGGParser().parse(file.getData()).getData().toByteArray();
    }

    static void assertMatchesWw2ogg(String name, byte[] wem, int channels, int rate, int audioPackets)
    {
        byte[] mine = convert(wem);
        checkStructure(mine, channels, rate, audioPackets, !name.equals("triad"));
        byte[] reference = resource("offline/audio/" + name + ".ww2ogg.ogg");
        checkStructure(reference, channels, rate, audioPackets, !name.equals("triad"));
        assertArrayEquals(reference, mine, name + ": byte identical to ww2ogg");
    }

    // ---------------------------------------------------------------- tests

    @Test
    public void testCrc()
    {
        byte[] rnd = new byte[1000];
        new Random(1).nextBytes(rnd);
        assertEquals(bitwiseOggCrc(rnd, rnd.length), OGGStream.oggCRC(rnd, rnd.length), "table crc == bitwise crc");
        // CRC-32 poly 0x04C11DB7, init 0, no reflection, no xorout
        assertEquals(0x89A1897F, OGGStream.oggCRC("123456789".getBytes(StandardCharsets.US_ASCII), 9));
    }

    @Test
    public void testCodebookLibraries()
    {
        for (CodebookLibrary lib : List.of(CodebookLibrary.getDefault(), CodebookLibrary.getStandard()))
        {
            // 598 codebooks each (vgmstream vcb_list_count_standard / vcb_list_count_aotuv603)
            assertEquals(598, lib.getCodebookCount() - 1);
            for (int id = 0; id < 598; id++)
            {
                int finalId = id;
                assertDoesNotThrow(() -> info(lib, finalId), "codebook " + id);
            }
            assertThrows(ParseException.class, () -> info(lib, 598));
        }

        int differ = 0;
        for (int id = 0; id < 598; id++)
        {
            if (!info(CodebookLibrary.getDefault(), id).equals(info(CodebookLibrary.getStandard(), id)))
            {
                differ++;
            }
        }
        assertTrue(differ > 0, "standard and aoTuV libraries differ");
    }

    @Test
    public void testExternalCodebooks()
    {
        // Wwise 2012+ layout: everything in the fmt chunk, 2 byte packet headers, modified packets, aoTuV codebooks
        assertMatchesWw2ogg("external", buildWem("external", false, aotuvId()), 2, 48000, AUDIO_SIZES.length);
    }

    @Test
    public void testOldVorbChunkStandardCodebooks()
    {
        // old vorb chunk (v48-v53) external setup: standard codebooks
        assertMatchesWw2ogg("vorbext", buildWem("vorbext", false, standardOnlyId()), 2, 44100, AUDIO_SIZES.length);
    }

    @Test
    public void testStandardLibraryFallback()
    {
        // fmt-only (v56+) external setup whose ids only make sense in the standard library: aoTuV is rejected, standard is used
        byte[] wem = buildWem("external", false, standardOnlyId());
        assertMatchesWw2ogg("fallback", wem, 2, 48000, AUDIO_SIZES.length);

        WEMData data = new WEMParser().parse(new ByteArray(wem)).getData();
        ParseException e = assertThrows(ParseException.class, () -> new OGGParser().parse(data, OGGParser.SetupType.EXTERNAL_CODEBOOKS));
        assertTrue(e.getMessage().contains("wrong codebook library"), e.getMessage());
    }

    @Test
    public void testInlineCodebooks()
    {
        assertMatchesWw2ogg("inline", buildWem("inline", false, aotuvId()), 2, 44100, AUDIO_SIZES.length);
    }

    @Test
    public void testFullSetup()
    {
        assertMatchesWw2ogg("full", buildWem("full", false, aotuvId()), 2, 44100, AUDIO_SIZES.length);
    }

    @Test
    public void testStandardHeaderTriad()
    {
        assertMatchesWw2ogg("triad", buildTriadWem(), 1, 22050, 3);
    }

    @Test
    public void testPaddedUnknownChunk()
    {
        // odd sized unknown chunk with a RIFF pad byte is skipped
        int id = aotuvId();
        assertArrayEquals(convert(buildWem("external", false, id)), convert(buildWem("external", true, id)));
    }

    @Test
    public void testInvalidCodebookId()
    {
        byte[]  bad        = buildWem("external", false, aotuvId());
        int     setupStart = new WEMData(bad).getDataChunkOffset() + 2;
        bad[setupStart + 1] = (byte) 0xFF; // codebook id bits -> huge id
        bad[setupStart + 2] |= 0x03;
        WEMData        data = new WEMData(bad);
        ParseException e    = assertThrows(ParseException.class, () -> new OGGParser().parse(data, OGGParser.SetupType.EXTERNAL_CODEBOOKS));
        assertTrue(e.getMessage().contains("Invalid codebook id"), e.getMessage());
    }
}

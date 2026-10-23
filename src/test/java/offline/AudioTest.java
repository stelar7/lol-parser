package offline;

import no.stelar7.cdragon.types.bnk.BNKHIRCObject;
import no.stelar7.cdragon.types.bnk.BNKParser;
import no.stelar7.cdragon.types.bnk.data.*;
import no.stelar7.cdragon.types.wem.WEMParser;
import no.stelar7.cdragon.types.wem.data.*;
import no.stelar7.cdragon.types.wpk.WPKParser;
import no.stelar7.cdragon.types.wpk.data.WPKFile;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WEM chunk parsing, WPK containers and BNK sound banks on synthetic data (Vorbis conversion is in {@link OggTest})
 */
public class AudioTest
{
    // ---------------------------------------------------------------- WEM

    @Test
    public void testWemChunks()
    {
        byte[]  wem  = OggTest.buildWem("external", false, OggTest.aotuvId());
        WEMData data = new WEMParser().parse(new ByteArray(wem)).getData();
        assertEquals(2, data.getChannelCount());
        assertEquals(48000, data.getSampleRate());
        assertEquals(12000, data.getBytesPerSecond());
        assertEquals(12345, data.getSampleCount());
        assertEquals(0x42, data.getFmtChunkSize());
        assertTrue(data.isModPackets(), "mod signal 0xD9 means modified packets");
        assertTrue(data.isNoGranule(), "fmt-only layout has no granules");
        assertFalse(data.isHeaderTriadPresent());

        WEMData vorb = new WEMParser().parse(new ByteArray(OggTest.buildWem("inline", false, 0))).getData();
        assertEquals(44100, vorb.getSampleRate());
        assertEquals(0x34, vorb.getVorbChunkSize());
        assertFalse(vorb.isNoGranule());
        assertFalse(vorb.isModPackets());

        assertTrue(new WEMParser().parse(new ByteArray(OggTest.buildTriadWem())).getData().isHeaderTriadPresent());
    }

    @Test
    public void testWemMissingDataChunk()
    {
        byte[]         noData = new LE().ascii("RIFF").i32(4 + 8 + 0x42).ascii("WAVE").ascii("fmt ").i32(0x42).zeros(0x42).toBytes();
        ParseException e      = assertThrows(ParseException.class, () -> new WEMData(noData));
        assertTrue(e.getMessage().contains("fmt and data"), e.getMessage());
    }

    // ---------------------------------------------------------------- WPK

    static byte[] buildWpk(byte[]... wems)
    {
        String[] names       = new String[wems.length];
        int      headerSize  = 12 + 4 * wems.length;
        int      entriesSize = 0;
        for (int i = 0; i < wems.length; i++)
        {
            names[i] = (char) ('a' + i) + ".wem";
            entriesSize += 12 + names[i].length() * 2;
        }

        LE    entries      = new LE();
        int[] entryOffsets = new int[wems.length];
        int   dataPos      = headerSize + entriesSize;
        for (int i = 0; i < wems.length; i++)
        {
            entryOffsets[i] = headerSize + entries.size();
            entries.i32(dataPos).i32(wems[i].length).i32(names[i].length()).bytes(names[i].getBytes(StandardCharsets.UTF_16LE));
            dataPos += wems[i].length;
        }

        LE f = new LE().ascii("r3d2").i32(1).i32(wems.length);
        for (int o : entryOffsets)
        {
            f.i32(o);
        }
        f.bytes(entries.toBytes());
        for (byte[] w : wems)
        {
            f.bytes(w);
        }
        return f.toBytes();
    }

    @Test
    public void testWpk()
    {
        byte[] good = OggTest.buildWem("external", false, OggTest.aotuvId());
        byte[] junk = "not a wem at all".getBytes(StandardCharsets.US_ASCII);
        byte[] wpk  = buildWpk(good, junk);

        WPKFile w = new WPKParser().parse(new ByteArray(wpk));
        assertEquals(2, w.getWEMFiles().size(), "both entries kept");

        WEMFile first = w.getWEMFiles().get(0);
        assertEquals("a.wem", first.getFilename());
        assertNotNull(first.getData(), "good wem parsed");
        assertEquals(48000, first.getData().getSampleRate());

        WEMFile second = w.getWEMFiles().get(1);
        assertEquals("b.wem", second.getFilename());
        assertNull(second.getData(), "bad wem not parsed");
        assertNotNull(second.getParseError());
        assertArrayEquals(junk, second.getRawData(), "bad wem kept raw");
    }

    @Test
    public void testWpkValidation()
    {
        byte[] wpk = buildWpk(OggTest.buildWem("external", false, OggTest.aotuvId()));

        byte[] badMagic = wpk.clone();
        badMagic[0] = 'x';
        assertThrows(ParseException.class, () -> new WPKParser().parse(new ByteArray(badMagic)));

        byte[] badVersion = wpk.clone();
        badVersion[4] = 9;
        assertThrows(ParseException.class, () -> new WPKParser().parse(new ByteArray(badVersion)));
    }

    // ---------------------------------------------------------------- BNK

    static void section(LE out, String name, byte[] body)
    {
        out.ascii(name).i32(body.length).bytes(body);
    }

    static BNKFile parseBnk(byte[] data)
    {
        return new BNKParser().parse(new ByteArray(data));
    }

    @Test
    public void testBnkSections()
    {
        LE bnk = new LE();
        section(bnk, "BKHD", new LE().i32(134).i32(0x1111).i32(0).i32(0).i32(0).toBytes());
        section(bnk, "DIDX", new LE().i32(0xAAAA).i32(0).i32(4).i32(0xBBBB).i32(16).i32(3).toBytes());
        section(bnk, "DATA", new LE().bytes(new byte[]{1, 2, 3, 4}).zeros(12).bytes(new byte[]{5, 6, 7}).toBytes());
        LE hirc = new LE().i32(4);
        // known type with garbage internals
        hirc.u8(2).i32(4 + 3).i32(0x10).bytes(new byte[]{1, 2, 3});
        // unknown types 0x30, 0xFF
        hirc.u8(0x30).i32(4 + 2).i32(0x20).bytes(new byte[]{9, 9});
        hirc.u8(0xFF).i32(4).i32(0x30);
        // setting with unsigned count
        hirc.u8(1).i32(4 + 1 + 1 + 4).i32(0x40).u8(1).u8(3).f32(0.5f);
        section(bnk, "HIRC", hirc.toBytes());
        section(bnk, "STID", new LE().i32(1).i32(2).i32(0x1111).u8(4).ascii("Init").i32(0x2222).u8(5).ascii("Music").toBytes());
        section(bnk, "INIT", new LE().i32(0).i32(1).toBytes());
        section(bnk, "STMG", new LE().i32(7).toBytes());
        section(bnk, "PLAT", new LE().i32(7).toBytes());

        BNKFile f = parseBnk(bnk.toBytes());

        List<BNKDATAWEMFile> wems = f.getData().getWemFiles();
        assertEquals(2, wems.size(), "DIDX/DATA");
        assertEquals(0xAAAA, wems.get(0).getFileId());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, wems.get(0).getData());
        assertEquals(0xBBBB, wems.get(1).getFileId());
        assertArrayEquals(new byte[]{5, 6, 7}, wems.get(1).getData());

        List<BNKHIRCObject> objs = f.getObjectHierarchy().getHircData();
        assertEquals(4, objs.size(), "hirc objects stored");

        assertEquals(BNKHIRCType.SFX_VOICE, objs.get(0).getType());
        assertNull(objs.get(0).getSound(), "garbage sfx is not parsed");
        assertNotNull(objs.get(0).getParseError(), "garbage sfx keeps its error");

        assertEquals(BNKHIRCType.UNKNOWN, objs.get(1).getType());
        assertEquals(0x30, objs.get(1).getRawType());
        assertEquals(0x20, objs.get(1).getId());
        assertEquals(0x30, objs.get(2).getId(), "stream stays in sync after unknown types");

        assertNotNull(objs.get(3).getSettings());
        assertEquals(0.5f, objs.get(3).getSettings().get(BNKHIRCSettingType.VOICE_LOW_PASS_FILTER), "setting parsed");

        assertEquals("Init", f.getBankIds().getBankNames().get(0x1111));
        assertEquals("Music", f.getBankIds().getBankNames().get(0x2222));
        assertEquals(3, f.getOtherSections().size(), "INIT/STMG/PLAT kept raw");
    }

    @Test
    public void testBnkDataWithoutIndex()
    {
        LE bnk = new LE();
        section(bnk, "BKHD", new LE().i32(134).i32(1).i32(0).i32(0).toBytes());
        section(bnk, "DATA", new byte[8]);
        BNKFile f = parseBnk(bnk.toBytes());
        assertNotNull(f.getData());
        assertTrue(f.getData().getWemFiles().isEmpty());
    }

    @Test
    public void testHircTypeIdsByVersion()
    {
        assertEquals(BNKHIRCType.AUX_BUS, BNKHIRCType.fromValue(0x12, 134));
        assertEquals(BNKHIRCType.AUX_BUS, BNKHIRCType.fromValue(0x14, 120));
        assertEquals(BNKHIRCType.MOTION_BUS, BNKHIRCType.fromValue(0x10, 120));
        assertEquals(BNKHIRCType.TIME_MODULATOR, BNKHIRCType.fromValue(0x16, 145));
        assertEquals(BNKHIRCType.UNKNOWN, BNKHIRCType.fromValue(0x15, 72));
        assertEquals(BNKHIRCType.LFO_MODULATOR, BNKHIRCType.fromValue(0x15, 88));
        assertEquals(BNKHIRCType.EFFECT, BNKHIRCType.fromValue(0x10, 134));
    }

    record Sfx(int version, int plugin, int stream, int source, int file, long offset, long size, int cache, int bits, byte[] params, int fxCount, int metaCount, int bus, int parent) {}

    /**
     * CAkSound body (after the id), per wwiser's LoadSource + NodeBaseParams; followed by junk standing in for the rest of the object
     */
    static byte[] sfxBody(Sfx s)
    {
        int v = s.version();
        LE  o = new LE();
        o.i32(s.plugin());
        if (v <= 89)
        {
            o.i32(s.stream());
        } else
        {
            o.u8(s.stream());
        }
        if (v <= 46)
        {
            o.i32(44100).i32(0x1102);
        }
        o.i32(s.source());
        if (v <= 88)
        {
            o.i32(s.file());
            if (s.stream() != 1)
            {
                o.i32((int) s.offset()).i32((int) s.size());
            }
        } else if (v <= 89)
        {
            o.i32(s.file());
            if (s.stream() != 1)
            {
                o.i32((int) s.offset());
            }
            o.i32((int) s.size());
        } else if (v <= 112)
        {
            o.i32(s.file());
            if (s.stream() != 2)
            {
                o.i32((int) s.offset());
            }
            o.i32((int) s.size());
        } else if (v <= 150)
        {
            o.i32((int) s.size());
        } else
        {
            o.i32(s.cache()).i32((int) s.size());
        }
        o.u8(s.bits());
        int pt = s.plugin() & 0xF;
        if (pt == 2 || (v <= 126 && pt == 5))
        {
            o.i32(s.params().length).bytes(s.params());
        }
        // fx
        o.u8(1).u8(s.fxCount());
        if (s.fxCount() > 0)
        {
            o.u8(0x3);
            for (int i = 0; i < s.fxCount(); i++)
            {
                o.u8(i).i32(0xF000 + i);
                if (v <= 48)
                {
                    o.u8(0).i32(3).bytes(new byte[]{1, 2, 3});
                    if (v > 46)
                    {
                        o.i32(1).i32(5).i32(6);
                    }
                } else if (v <= 145)
                {
                    o.u8(1).u8(0);
                } else
                {
                    o.u8(0x2);
                }
            }
        }
        if (v > 136)
        {
            o.u8(0).u8(s.metaCount());
            for (int i = 0; i < s.metaCount(); i++)
            {
                o.u8(i).i32(0xE000 + i).u8(1);
            }
        }
        if (v > 89 && v <= 145)
        {
            o.u8(1);
        }
        o.i32(s.bus()).i32(s.parent());
        // rest of the node base params (priority, props, positioning, ...) is not parsed
        o.bytes(new byte[]{0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F});
        return o.toBytes();
    }

    static byte[] sfxBank(int version, byte[] body)
    {
        LE hirc = new LE().i32(2);
        if (version <= 48)
        {
            hirc.i32(2);
        } else
        {
            hirc.u8(2);
        }
        hirc.i32(4 + body.length).i32(0xAB).bytes(body);
        // a second object to check the stream stays in sync
        if (version <= 48)
        {
            hirc.i32(4);
        } else
        {
            hirc.u8(4);
        }
        hirc.i32(4).i32(0xCD);
        LE bnk = new LE();
        section(bnk, "BKHD", new LE().i32(version).i32(1).i32(0).i32(0).toBytes());
        section(bnk, "HIRC", hirc.toBytes());
        return bnk.toBytes();
    }

    @Test
    public void testSfxSoundVersions()
    {
        List<Sfx> cases = List.of(
                new Sfx(44, 0x00040001, 0, 0x101, 0x201, 0x40, 0x1000, 0, 0, null, 1, 0, 0x301, 0x401),
                new Sfx(48, 0x00040001, 0, 0x101, 0x201, 0x40, 0x1000, 0, 0, null, 2, 0, 0x301, 0x401),
                new Sfx(72, 0x00040001, 1, 0x102, 0x202, -1, -1, 0, 1, null, 0, 0, 0x302, 0x402),
                new Sfx(88, 0x00040001, 0, 0x103, 0x203, 0x80, 0x2000, 0, 0, null, 1, 0, 0x303, 0x403),
                new Sfx(89, 0x00040001, 1, 0x104, 0x204, -1, 0x300, 0, 0, null, 0, 0, 0x304, 0x404),
                new Sfx(112, 0x00040001, 2, 0x105, 0x205, -1, 0x400, 0, 2, null, 2, 0, 0x305, 0x405),
                new Sfx(112, 0x00040001, 1, 0x106, 0x206, 0x10, 0x500, 0, 0, null, 0, 0, 0x306, 0x406),
                new Sfx(120, 0x00650005, 0, 0x107, 0x107, -1, 0, 0, 0, new byte[]{1, 2, 3, 4, 5}, 0, 0, 0x307, 0x407),
                new Sfx(134, 0x00040001, 2, 0x108, 0x108, -1, 0x600, 0, 0x01, null, 1, 0, 0x308, 0x408),
                new Sfx(134, 0x00660002, 0, 0x109, 0x109, -1, 0, 0, 0, new byte[]{9, 8, 7}, 0, 0, 0x309, 0x409),
                new Sfx(135, 0x00040001, 1, 0x10A, 0x10A, -1, 0x700, 0, 0x02, null, 0, 0, 0x30A, 0x40A),
                new Sfx(140, 0x00040001, 0, 0x10B, 0x10B, -1, 0x800, 0, 0, null, 2, 1, 0x30B, 0x40B),
                new Sfx(145, 0x00040001, 2, 0x10C, 0x10C, -1, 0x900, 0, 0x80, null, 1, 2, 0x30C, 0x40C),
                new Sfx(150, 0x00040001, 2, 0x10D, 0x10D, -1, 0xA00, 0, 0x81, null, 3, 0, 0x30D, 0x40D),
                new Sfx(154, 0x00130001, 0, 0x10E, 0x10E, -1, 0xB00, 0xCAFE, 0, null, 1, 1, 0x30E, 0x40E)
        );
        for (Sfx c : cases)
        {
            String              v    = "bank v" + c.version() + " stream " + c.stream();
            byte[]              body = sfxBody(c);
            List<BNKHIRCObject> objs = parseBnk(sfxBank(c.version(), body)).getObjectHierarchy().getHircData();
            assertEquals(BNKHIRCType.SFX_VOICE, objs.get(0).getType(), v);
            assertEquals(0xCD, objs.get(1).getId(), v + ": next object in sync");

            BNKHIRCSound s = objs.get(0).getSound();
            assertNotNull(s, v + ": " + objs.get(0).getParseError());
            assertEquals(c.plugin(), s.getPluginId(), v);
            assertEquals(c.stream(), s.getStreamType(), v);
            assertEquals(c.source(), s.getSourceId(), v);
            assertEquals(c.file(), s.getFileId(), v);
            assertEquals(c.offset(), s.getFileOffset(), v);
            assertEquals(c.size(), s.getInMemoryMediaSize(), v);
            assertEquals(c.cache(), s.getCacheId(), v);
            assertEquals(c.bits(), s.getSourceBits(), v);
            assertArrayEquals(c.params(), s.getPluginParams(), v);
            assertTrue(s.hasParentInfo(), v);
            assertEquals(c.bus(), s.getOverrideBusId(), v);
            assertEquals(c.parent(), s.getDirectParentId(), v);

            // truncated to just before the end of the parent id: error, raw data kept, next object still read
            byte[]              cut   = Arrays.copyOf(body, body.length - 7 - 2);
            List<BNKHIRCObject> objs2 = parseBnk(sfxBank(c.version(), cut)).getObjectHierarchy().getHircData();
            assertNull(objs2.get(0).getSound(), v + ": truncated");
            assertNotNull(objs2.get(0).getParseError(), v + ": truncated");
            assertArrayEquals(cut, objs2.get(0).getData(), v + ": truncated keeps raw data");
            assertEquals(0xCD, objs2.get(1).getId(), v + ": truncated, next object in sync");
        }
    }
}

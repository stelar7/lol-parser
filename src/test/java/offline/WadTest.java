package offline;

import com.github.luben.zstd.Zstd;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.*;
import no.stelar7.cdragon.types.wad.data.content.*;
import no.stelar7.cdragon.types.wad.data.header.*;
import no.stelar7.cdragon.util.handlers.HashHandler;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WAD parsing on synthetic files: header versions, entry layouts, every compression type and subchunked (ZSTD_MULTI) entries
 */
public class WadTest
{
    @BeforeAll
    public static void offlineHashes()
    {
        OfflineHashes.useEmpty();
    }

    private static final int V3_HEADER_SIZE = 4 + 256 + 8 + 4;

    /**
     * One 2.x/3.x entry
     */
    private record Entry(long hash, int offset, int compressedSize, int size, int type, int subChunkCount, int byte5, int u16)
    {
        void write(LE out)
        {
            out.i64(hash).i32(offset).i32(compressedSize).i32(size).u8(type | (subChunkCount << 4)).u8(byte5).i16(u16).i64(0);
        }
    }

    private static LE v3Header(int minor, int entryCount)
    {
        return new LE().ascii("RW").u8(3).u8(minor).zeros(256).i64(0).i32(entryCount);
    }

    private static byte[] gzip(byte[] data) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out))
        {
            gz.write(data);
        }
        return out.toByteArray();
    }

    @Test
    public void testByteArrayAppend()
    {
        ByteArray ba = new ByteArray();
        for (int i = 0; i < 1000; i++)
        {
            ba.append(new byte[]{(byte) i, (byte) (i >> 8)});
        }
        assertEquals(2000, ba.size());
        assertEquals(2000, ba.getDataRaw().length);
        assertEquals((byte) 999, ba.getDataRaw()[1998]);
        assertEquals(new ByteArray(new byte[]{0, 0}), ba.copyOfRange(0, 2));
    }

    @Test
    public void testCanParse()
    {
        WADParser p = new WADParser();
        assertTrue(p.canParse(new byte[]{'R', 'W', 1, 0}));
        assertTrue(p.canParse(new byte[]{'R', 'W', 2, 0}));
        assertTrue(p.canParse(new byte[]{'R', 'W', 3, 4}));
        assertFalse(p.canParse(new byte[]{'R', 'W', 3, 5}));
        assertFalse(p.canParse(new byte[]{'R', 'W', 4, 0}));
        assertFalse(p.canParse(new byte[]{'R', 'X', 3, 1}));
        assertFalse(p.canParse(new byte[]{'R', 'W', 3}));
        assertFalse(p.canParse(null));
    }

    @Test
    public void testCompressionTypes() throws IOException
    {
        byte[] plain = "plain entry data".getBytes(StandardCharsets.UTF_8);
        byte[] text  = "zstd entry data ".repeat(20).getBytes(StandardCharsets.UTF_8);
        byte[] zstd  = Zstd.compress(text);
        byte[] gz    = gzip(text);
        byte[] ref   = new LE().str32("data/other/file.bin").toBytes();

        int entryCount = 4;
        int dataStart  = V3_HEADER_SIZE + entryCount * 32;
        int off        = dataStart;

        LE wad = v3Header(1, entryCount);
        new Entry(1, off, plain.length, plain.length, 0, 0, 0, 0).write(wad);
        off += plain.length;
        new Entry(2, off, zstd.length, text.length, 3, 0, 0, 0).write(wad);
        off += zstd.length;
        new Entry(3, off, gz.length, text.length, 1, 0, 0, 0).write(wad);
        off += gz.length;
        new Entry(4, off, ref.length, ref.length, 2, 0, 1, 0).write(wad);
        wad.bytes(plain).bytes(zstd).bytes(gz).bytes(ref);

        WADFile file = new WADParser().parse(new ByteArray(wad.toBytes()));
        assertEquals(3, file.getHeader().getMajor());
        assertEquals(1, file.getHeader().getMinor());
        assertEquals(4, file.getHeader().getFileCount());

        List<WADContentHeaderV1> headers = file.getContentHeaders();
        assertEquals("0000000000000001", headers.get(0).getPathHash());
        assertEquals(WADCompressionType.NONE, headers.get(0).getCompressionType());
        assertEquals(WADCompressionType.ZSTD, headers.get(1).getCompressionType());
        assertEquals(WADCompressionType.GZIP, headers.get(2).getCompressionType());
        assertEquals(WADCompressionType.REFERENCE, headers.get(3).getCompressionType());
        assertTrue(((WADContentHeaderV2) headers.get(3)).isDuplicate());
        assertFalse(file.hasSubChunks());

        assertArrayEquals(plain, file.readContentFromHeaderData(headers.get(0)));
        assertArrayEquals(text, file.readContentFromHeaderData(headers.get(1)));
        assertArrayEquals(text, file.readContentFromHeaderData(headers.get(2)));
        assertEquals("data/other/file.bin", new String(file.readContentFromHeaderData(headers.get(3)), StandardCharsets.UTF_8));
    }

    @Test
    public void testVersion1And2Headers()
    {
        byte[] plain = {1, 2, 3, 4, 5};

        // 1.x: u16 entryHeaderOffset, u16 cellSize, u32 count, then 24 byte entries
        LE v1 = new LE().ascii("RW").u8(1).u8(0).i16(12).i16(24).i32(1);
        v1.i64(0xABCDL).i32(12 + 24).i32(plain.length).i32(plain.length).i32(0).bytes(plain);
        WADFile f1 = new WADParser().parse(new ByteArray(v1.toBytes()));
        assertInstanceOf(WADHeaderV1.class, f1.getHeader());
        assertEquals(24, ((WADHeaderV1) f1.getHeader()).getEntryHeaderCellSize());
        assertEquals("000000000000ABCD", f1.getContentHeaders().get(0).getPathHash());
        assertFalse(f1.getContentHeaders().get(0) instanceof WADContentHeaderV2);
        assertArrayEquals(plain, f1.readContentFromHeaderData(f1.getContentHeaders().get(0)));

        // 2.x: u8 signature length, 83 bytes signature + padding, u64 checksum, u16, u16, u32 count, then 32 byte entries
        int headerSize = 4 + 1 + 83 + 8 + 2 + 2 + 4;
        LE  v2         = new LE().ascii("RW").u8(2).u8(0).u8(3).ascii("sig").zeros(80).i64(0x77).i16(headerSize).i16(32).i32(1);
        new Entry(0xEFL, headerSize + 32, plain.length, plain.length, 0, 0, 0, 0).write(v2);
        v2.bytes(plain);
        WADFile f2 = new WADParser().parse(new ByteArray(v2.toBytes()));
        WADHeaderV2 h2 = (WADHeaderV2) f2.getHeader();
        assertEquals(3, h2.getECDSALength());
        assertArrayEquals("sig".getBytes(StandardCharsets.UTF_8), h2.getECDSA());
        assertEquals(0x77, h2.getFileChecksum());
        assertArrayEquals(plain, f2.readContentFromHeaderData(f2.getContentHeaders().get(0)));

        // a signature longer than the 83 byte field
        byte[] badSignature = new LE().ascii("RW").u8(2).u8(0).u8(84).zeros(100).toBytes();
        assertThrows(ParseException.class, () -> new WADParser().parse(new ByteArray(badSignature)));
    }

    @Test
    public void testVersion33UnsignedFields()
    {
        // the subchunk index is a u16, the offset a u32, and the count the high nibble of the type byte
        LE w33 = v3Header(3, 1);
        new Entry(1, 0xF0000000, 10, 10, 4, 15, 1, 0xFFF0).write(w33);
        WADFile            f33 = new WADParser().parse(new ByteArray(w33.toBytes()));
        WADContentHeaderV2 h33 = (WADContentHeaderV2) f33.getContentHeaders().get(0);
        assertEquals(0xFFF0, h33.getSubChunkOffset());
        assertEquals(15, h33.getSubChunkCount());
        assertTrue(h33.isDuplicate());
        assertEquals(0xF0000000L, h33.getOffset());
        assertEquals(0xFFF0 + 15, f33.getTotalSubChunkCount());

        // the entry points outside the file
        assertThrows(ParseException.class, () -> f33.readContentFromHeaderData(h33));
    }

    @Test
    public void testInvalidFiles()
    {
        WADParser p = new WADParser();
        assertThrows(ParseException.class, () -> p.parse(new ByteArray(v3Header(5, 0).toBytes())), "3.5 is not supported");
        assertThrows(ParseException.class, () -> p.parse(new ByteArray(new LE().ascii("XW").u8(3).u8(1).zeros(268).toBytes())), "bad magic");
        assertThrows(ParseException.class, () -> p.parse(new ByteArray(new byte[]{'R', 'W'})), "too small");
        assertThrows(ParseException.class, () -> p.parse(new ByteArray(v3Header(1, 1000).toBytes())), "more entries than the file can hold");
    }

    /**
     * A 3.4 WAD with a ZSTD_MULTI entry, its .subchunktoc and a decoy entry that looks like a subchunk toc too
     */
    private static byte[] buildMultiWad(byte[] expectedOut, int firstChunk, int chunkCount, boolean withDecoy) throws IOException
    {
        Random                rnd      = new Random(1);
        byte[][]              raw      = new byte[chunkCount][];
        byte[][]              stored   = new byte[chunkCount][];
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        for (int i = 0; i < chunkCount; i++)
        {
            raw[i] = new byte[1000 + i * 10];
            if (i % 2 == 0)
            {
                // compressible
                Arrays.fill(raw[i], (byte) i);
                stored[i] = Zstd.compress(raw[i]);
            } else
            {
                // random data is stored raw
                rnd.nextBytes(raw[i]);
                stored[i] = raw[i];
            }
            expected.write(raw[i]);
        }
        System.arraycopy(expected.toByteArray(), 0, expectedOut, 0, expected.size());

        int tocEntries = firstChunk + chunkCount;
        LE  toc        = new LE();
        for (int i = 0; i < tocEntries; i++)
        {
            int idx = i - firstChunk;
            if (idx >= 0)
            {
                toc.i32(stored[idx].length).i32(raw[idx].length).i64(0);
            } else
            {
                toc.i32(1).i32(1).i64(0);
            }
        }
        byte[] tocBytes = toc.toBytes();
        // same size as the toc, and also a "valid" toc
        byte[] decoy = new byte[withDecoy ? tocBytes.length : 0];

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        for (byte[] s : stored)
        {
            data.write(s);
        }
        byte[] multiData = data.toByteArray();

        int entryCount = withDecoy ? 3 : 2;
        int off        = V3_HEADER_SIZE + entryCount * 32;

        LE   wad     = v3Header(4, entryCount);
        long tocHash = HashHandler.computeXXHash64AsLong("data/final/test.wad.subchunktoc");

        // 3.4: the byte after the type is the high part of the 24 bit subchunk index
        new Entry(0x1234, off, multiData.length, expected.size(), 4, chunkCount, firstChunk >> 16, firstChunk & 0xFFFF).write(wad);
        off += multiData.length;
        if (withDecoy)
        {
            // decoy first, so the size heuristic sees two candidates
            new Entry(0x5678, off + tocBytes.length, decoy.length, decoy.length, 0, 0, 0, 0).write(wad);
        }
        new Entry(tocHash, off, tocBytes.length, tocBytes.length, 0, 0, 0, 0).write(wad);
        wad.bytes(multiData).bytes(tocBytes).bytes(decoy);
        return wad.toBytes();
    }

    @Test
    public void testVersion34SubChunks() throws IOException
    {
        int    chunkCount = 9;
        int    firstChunk = 0x10005; // needs more than 16 bits
        byte[] expected   = new byte[9000 + 360];
        byte[] wadBytes   = buildMultiWad(expected, firstChunk, chunkCount, true);

        Path dir     = Files.createTempDirectory("cdragon-wad");
        Path wadPath = dir.resolve("DATA").resolve("FINAL").resolve("Test.wad.client");
        try
        {
            Files.createDirectories(wadPath.getParent());
            Files.write(wadPath, wadBytes);

            WADFile            file  = new WADParser().parse(wadPath);
            WADContentHeaderV2 multi = (WADContentHeaderV2) file.getContentHeaders().get(0);
            assertEquals(9, multi.getSubChunkCount(), "a count >= 8 is not negative");
            assertEquals(firstChunk, multi.getSubChunkOffset(), "24 bit subchunk index");
            assertFalse(multi.isDuplicate());
            assertEquals(WADCompressionType.ZSTD_MULTI, multi.getCompressionType());
            assertEquals(firstChunk + chunkCount, file.getTotalSubChunkCount());
            assertTrue(file.hasSubChunks());

            // mixed raw/zstd chunks; the toc is found by the wad path ("data/final/test.wad.subchunktoc") despite the decoy
            assertArrayEquals(expected, file.readContentFromHeaderData(multi));
            assertEquals(firstChunk + chunkCount, file.getSubChunkMap(file.getTotalSubChunkCount()).size());
        } finally
        {
            Files.delete(wadPath);
            Files.delete(wadPath.getParent());
            Files.delete(wadPath.getParent().getParent());
            Files.delete(dir);
        }

        // same bytes without a path: the size heuristic finds two candidates and gives up
        WADFile anon = new WADParser().parse(new ByteArray(wadBytes));
        assertThrows(ParseException.class, () -> anon.getSubChunkMap(anon.getTotalSubChunkCount()));
        assertThrows(ParseException.class, () -> anon.readContentFromHeaderData(anon.getContentHeaders().get(0)), "not plain zstd frames either");
    }

    @Test
    public void testSubChunkHeuristic() throws IOException
    {
        // without a path and without the decoy, the only toc sized entry is used
        byte[]  expected = new byte[3000 + 30];
        byte[]  wadBytes = buildMultiWad(expected, 2, 3, false);
        WADFile anon     = new WADParser().parse(new ByteArray(wadBytes));
        assertArrayEquals(expected, anon.readContentFromHeaderData(anon.getContentHeaders().get(0)));
        assertEquals(5, anon.getSubChunkMap(5).size());
    }

    @Test
    public void testMultiWithoutTocIsPlainZstd()
    {
        // ZSTD_MULTI entries in a wad without a toc can be a series of zstd frames
        byte[] a = "first frame ".repeat(10).getBytes(StandardCharsets.UTF_8);
        byte[] b = "second frame ".repeat(10).getBytes(StandardCharsets.UTF_8);
        byte[] frames = new LE().bytes(Zstd.compress(a)).bytes(Zstd.compress(b)).toBytes();

        LE wad = v3Header(3, 1);
        new Entry(9, V3_HEADER_SIZE + 32, frames.length, a.length + b.length, 4, 2, 0, 0).write(wad);
        wad.bytes(frames);

        WADFile file = new WADParser().parse(new ByteArray(wad.toBytes()));
        byte[]  out  = file.readContentFromHeaderData(file.getContentHeaders().get(0));
        assertEquals(new String(a, StandardCharsets.UTF_8) + new String(b, StandardCharsets.UTF_8), new String(out, StandardCharsets.UTF_8));
    }
}

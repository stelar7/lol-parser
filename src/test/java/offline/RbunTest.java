package offline;

import no.stelar7.cdragon.types.rbun.*;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RBUN (patcher bundle) table of contents and footer on synthetic bundles
 */
public class RbunTest
{
    @BeforeAll
    public static void offlineHashes()
    {
        OfflineHashes.useEmpty();
    }

    /**
     * Chunk data, toc entries (u64 id, u32 uncompressed, u32 compressed) and footer (u64 id, u32 count, u32 version, "RBUN")
     */
    private static LE bundle(int version, String magic)
    {
        return new LE().zeros(30)
                       .i64(0xAAL).i32(5).i32(10)
                       .i64(0xBBL).i32(6).i32(11)
                       .i64(0xCCL).i32(7).i32(9)
                       .i64(0x1122334455667788L).i32(3).i32(version).ascii(magic);
    }

    @Test
    public void testParse()
    {
        RBUNFile rbun = new RBUNParser().parse(new ByteArray(bundle(1, "RBUN").toBytes()));

        assertEquals("RBUN", rbun.getMagic());
        assertEquals(1, rbun.getVersion());
        assertEquals("1122334455667788", rbun.getBundleId());
        assertEquals(3, rbun.getChunkCount());
        assertEquals(20 + 3 * 16, rbun.getMetadataSize());

        // entries are in file order
        RBUNChunkInfo first = rbun.getChunks().get(0);
        assertEquals("00000000000000AA", first.getChunkId());
        assertEquals(5, first.getUncompressedSize());
        assertEquals(10, first.getCompressedSize());
        assertEquals("00000000000000BB", rbun.getChunks().get(1).getChunkId());
        assertEquals("00000000000000CC", rbun.getChunks().get(2).getChunkId());
        assertEquals(9, rbun.getChunks().get(2).getCompressedSize());
    }

    @Test
    public void testVersionFFFFFFFF()
    {
        // newer bundles use 0xFFFFFFFF as the version, with a checksum in place of the id
        RBUNFile rbun = new RBUNParser().parse(new ByteArray(bundle(0xFFFFFFFF, "RBUN").toBytes()));
        assertEquals(0xFFFFFFFF, rbun.getVersion());
        assertEquals(3, rbun.getChunks().size());
    }

    @Test
    public void testEmptyBundle()
    {
        RBUNFile rbun = new RBUNParser().parse(new ByteArray(new LE().i64(1).i32(0).i32(1).ascii("RBUN").toBytes()));
        assertTrue(rbun.getChunks().isEmpty());
    }

    @Test
    public void testInvalid()
    {
        RBUNParser p = new RBUNParser();
        assertThrows(ParseException.class, () -> p.parse(new ByteArray(bundle(1, "RBUX").toBytes())), "magic");
        assertThrows(ParseException.class, () -> p.parse(new ByteArray(bundle(2, "RBUN").toBytes())), "version");
        assertThrows(ParseException.class, () -> p.parse(new ByteArray(new byte[10])), "too small");
        // 1000 entries do not fit in the file
        assertThrows(ParseException.class, () -> p.parse(new ByteArray(new LE().i64(1).i32(1000).i32(1).ascii("RBUN").toBytes())), "toc larger than the file");
    }
}

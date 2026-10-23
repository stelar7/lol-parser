package offline;

import net.openhft.hashing.LongHashFunction;
import no.stelar7.cdragon.util.handlers.HashHandler;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HashHandler against known vectors.
 * The xxHash64/xxHash32 values were checked with an independent reference implementation of the xxHash spec,
 * the FNV/CRC/ELF/SDBM values with straightforward reimplementations of the algorithms,
 * and the "list line" values are lines from CommunityDragon's hashes.*.txt lists (made with python-xxhash).
 */
public class HashTest
{
    @BeforeAll
    public static void offlineHashes()
    {
        OfflineHashes.useEmpty();
    }

    @Test
    public void testXXHash64()
    {
        assertEquals(0xEF46DB3751D8E999L, HashHandler.computeXXHash64AsLong(""));
        assertEquals(0xD24EC4F1A98C6E5BL, HashHandler.computeXXHash64AsLong("a"));
        assertEquals(0x44BC2CF5AD770999L, HashHandler.computeXXHash64AsLong("abc"));
        assertEquals(0xA847C7A46BC6730EL, HashHandler.computeXXHash64AsLong("data/characters/ahri/ahri.bin"));
        // > 32 bytes, uses the 4 lane loop
        assertEquals(0x5C39ED61F54BF855L, HashHandler.computeXXHash64AsLong("plugins/rcp-fe-lol-loot/global/default/trans.json"));
        // line from hashes.binentries/game lists: "857d37e768df4620 10year_startup_tip_1000"
        assertEquals(Long.parseUnsignedLong("857d37e768df4620", 16), HashHandler.computeXXHash64AsLong("10year_startup_tip_1000"));

        assertEquals(HashHandler.computeXXHash64AsLong("abc"), HashHandler.computeXXHash64AsLong("abc".getBytes(StandardCharsets.UTF_8)));
        // the string form is 16 upper case hex digits, leading zeros kept
        assertEquals("EF46DB3751D8E999", HashHandler.computeXXHash64(""));
        assertEquals("44BC2CF5AD770999", HashHandler.computeXXHash64("abc"));
    }

    @Test
    public void testXXHash64MatchesOtherImplementation()
    {
        // lz4-java (used by HashHandler) vs zero-allocation-hashing
        LongHashFunction other = LongHashFunction.xx();
        for (String s : new String[]{"", "x", "mspell", "data/characters/ahri/ahri.bin", "plugins/rcp-fe-lol-loot/global/default/trans.json", "æøå/ü.png"})
        {
            assertEquals(other.hashBytes(s.getBytes(StandardCharsets.UTF_8)), HashHandler.computeXXHash64AsLong(s), s);
        }
    }

    @Test
    public void testXXHash32()
    {
        assertEquals(0x02CC5D05L, HashHandler.computeXXHash32AsLong(""));
        assertEquals(0x550D7456L, HashHandler.computeXXHash32AsLong("a"));
        assertEquals(0x32D153FFL, HashHandler.computeXXHash32AsLong("abc"));
        assertEquals(0x3B02A5FFL, HashHandler.computeXXHash32AsLong("plugins/rcp-fe-lol-loot/global/default/trans.json"));
        assertEquals("02CC5D05", HashHandler.computeXXHash32(""));
    }

    @Test
    public void testXXH3()
    {
        assertEquals(0x2D06800538D394C2L, HashHandler.computeXXH3AsLong(""));
        // line from hashes.rst.xxh3.txt: "639a544e280d2eb4 11thhourpower_objective"
        assertEquals(Long.parseUnsignedLong("639a544e280d2eb4", 16), HashHandler.computeXXH3AsLong("11thhourpower_objective"));
    }

    @Test
    public void testFNV()
    {
        // FNV-1a 32 bit, used for bin hashes
        assertEquals(0x811C9DC5L, HashHandler.computeFNV1ALowercase(""));
        assertEquals(0xE40C292CL, HashHandler.computeFNV1ALowercase("a"));
        assertEquals(0xBF9CF968L, HashHandler.computeFNV1ALowercase("foobar"));
        // computeFNV1A lowercases first, the lowercase variant does not
        assertEquals(0xB086171EL, HashHandler.computeFNV1A("Characters/Ahri"));
        assertEquals(0x9A15129EL, HashHandler.computeFNV1ALowercase("Characters/Ahri"));

        // FNV-1 32 bit (multiply, then xor) of the lowercased input
        assertEquals(0x050C5D7EL, HashHandler.computeFNV("A"));
        assertEquals(0x31F0B262L, HashHandler.computeFNV("foobar"));
        assertEquals(0x6D5351E0L, HashHandler.computeFNV("Characters/Ahri"));
    }

    @Test
    public void testOtherHashes()
    {
        // CRC-32, polynomial 0x04C11DB7, MSB first, init 0, no final xor
        assertEquals(0L, HashHandler.computeCCITT32(""));
        assertEquals(0xA864DB20L, HashHandler.computeCCITT32("a"));
        assertEquals(0x97457E4CL, HashHandler.computeCCITT32("foobar"));
        assertEquals(0x97457E4CL, HashHandler.computeCCITT32("foobarXYZ".getBytes(StandardCharsets.UTF_8), 6), "only the first size bytes");

        // ELF hash of the lowercased input (inibin keys)
        assertEquals(0x61L, HashHandler.computeELFHash("A"));
        assertEquals(0x06D65882L, HashHandler.computeELFHash("foobar"));
        assertEquals(0x01E2BCA9L, HashHandler.computeELFHash("Characters/Ahri"));

        // SDBM, case sensitive
        assertEquals(0xA6437B0DL, HashHandler.computeSDBMHash("foobar") & 0xFFFFFFFFL);
        assertEquals(0x4199C223L, HashHandler.computeSDBMHash("Characters/Ahri") & 0xFFFFFFFFL);
    }

    @Test
    public void testToHex()
    {
        assertEquals("000000000000ABCD", HashHandler.toHex(0xABCDL, 16));
        assertEquals("FFFFFFFFFFFFFFFF", HashHandler.toHex(-1L, 16));
        assertEquals("BCD", HashHandler.toHex(0xABCDL, 1, 3), "max length keeps the low digits");
        assertEquals("00FF10", HashHandler.toHex(new byte[]{0, (byte) 0xFF, 0x10}).toUpperCase());

        for (String s : new String[]{"", "x", "data/characters/ahri/ahri.bin"})
        {
            long h = HashHandler.computeXXHash64AsLong(s);
            assertEquals(h, Long.parseUnsignedLong(HashHandler.toHex(h, 16), 16), "hex round trip of " + s);
        }
    }
}

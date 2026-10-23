package offline;

import no.stelar7.cdragon.types.filemanifest.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.*;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The magic-less file manifests (v0/v1/v2/atlas) and file type sniffing of them and of near misses
 */
public class ManifestTest
{
    private final ManifestContentParser parser = new ManifestContentParser();

    @BeforeAll
    public static void offlineHashes()
    {
        // BINParser/FileTypeHandler would otherwise load the bundled hash tables (or fall back to the network)
        OfflineHashes.useEmpty();
    }

    private static byte[] v0()
    {
        return new LE().i32(3).i32(1).i32(-2).i32(70000).toBytes();
    }

    private static byte[] v1()
    {
        return new LE().i32(3).str32("a/b.lua").str32("").str32("æøå").toBytes();
    }

    private static byte[] v2()
    {
        return new LE().i32(3)
                       .str32("hdrB").i32(2).i32(9).str32("x").i32(8).str32("y")
                       .str32("hdrA").i32(1).i32(7).str32("z")
                       .str32("hdrB").i32(1).i32(6).str32("w")
                       .toBytes();
    }

    private static byte[] atlas()
    {
        // version, atlas file, count, (name, big endian x, y, w, h, z)
        LE header = new LE().i32(1).str32("abc").i32(1).str32("x");
        LE rect   = new LE(ByteOrder.BIG_ENDIAN).i32(1).i32(2).i32(3).i32(4).i32(5);
        return header.bytes(rect.toBytes()).toBytes();
    }

    @Test
    public void testParse()
    {
        assertEquals(List.of(1, -2, 70000), new ArrayList<>(parser.parseV0(new ByteArray(v0())).getItems()));
        assertEquals(List.of("a/b.lua", "", "æøå"), new ArrayList<>(parser.parseV1(new ByteArray(v1())).getItems()));

        Map<String, List<String>> v2 = parser.parseV2(new ByteArray(v2())).getItems();
        assertEquals(List.of("x", "y", "w"), v2.get("hdrB"), "repeated headers are merged");
        assertEquals(List.of("z"), v2.get("hdrA"));

        RandomAccessReader       reader = new RandomAccessReader(atlas(), ByteOrder.LITTLE_ENDIAN);
        ManifestContentFileAtlas a      = parser.parseAtlas(reader);
        assertEquals(ByteOrder.LITTLE_ENDIAN, reader.getEndian(), "byte order is restored");
        assertEquals("abc", a.getAtlasFile());
        Map<String, Object> item = a.getItems().get(0);
        assertEquals(1, item.get("x"));
        assertEquals(2, item.get("y"));
        assertEquals(3, item.get("w"));
        assertEquals(4, item.get("h"));
        assertEquals(5, item.get("z"));
    }

    @Test
    public void testCanParse()
    {
        byte[] v0 = v0();
        byte[] v1 = v1();
        byte[] v2 = v2();
        byte[] at = atlas();

        assertTrue(parser.canParseV0(v0));
        assertFalse(parser.canParseV1(v0));
        assertFalse(parser.canParseV2(v0));
        assertFalse(parser.canParseAtlas(v0));

        assertTrue(parser.canParseV1(v1));
        assertFalse(parser.canParseV2(v1));

        assertTrue(parser.canParseV2(v2));
        assertTrue(parser.canParseAtlas(at));
        assertTrue(parser.canParseV1(at), "every atlas is also a valid v1 manifest");

        for (byte[] b : new byte[][]{null, new byte[0], new byte[3]})
        {
            assertFalse(parser.canParseV0(b));
            assertFalse(parser.canParseV1(b));
            assertFalse(parser.canParseV2(b));
            assertFalse(parser.canParseAtlas(b));
        }
    }

    @Test
    public void testInvalid()
    {
        byte[] hugeLength = new LE().i32(1).i32(999999).toBytes();
        assertFalse(parser.canParseV1(hugeLength));
        assertThrows(ParseException.class, () -> parser.parseV1(new ByteArray(hugeLength)));

        byte[] hugeCount = new LE().i32(100).i32(1).toBytes();
        assertThrows(ParseException.class, () -> parser.parseV0(new ByteArray(hugeCount)));
        assertThrows(ParseException.class, () -> parser.parseV2(new ByteArray(hugeCount)));

        byte[] negative = new LE().i32(-1).toBytes();
        assertThrows(ParseException.class, () -> parser.parseV0(new ByteArray(negative)));

        byte[] truncatedV2 = Arrays.copyOf(v2(), v2().length - 2);
        assertFalse(parser.canParseV2(truncatedV2));
        assertThrows(ParseException.class, () -> parser.parseV2(new ByteArray(truncatedV2)));

        byte[] atlasV2 = atlas();
        atlasV2[0] = 2;
        assertFalse(parser.canParseAtlas(atlasV2));
        assertThrows(ParseException.class, () -> parser.parseAtlas(new ByteArray(atlasV2)));

        byte[] truncatedAtlas = Arrays.copyOf(atlas(), atlas().length - 1);
        assertFalse(parser.canParseAtlas(truncatedAtlas));
        assertThrows(ParseException.class, () -> parser.parseAtlas(new ByteArray(truncatedAtlas)));
    }

    @Test
    public void testSniffing()
    {
        assertEquals("manifestv0", FileTypeHandler.findFileType(new ByteArray(v0())));
        assertEquals("manifestv1", FileTypeHandler.findFileType(new ByteArray(v1())));
        assertEquals("manifestv2", FileTypeHandler.findFileType(new ByteArray(v2())));
        assertEquals("atlas", FileTypeHandler.findFileType(new ByteArray(atlas())));
        assertEquals("empty", FileTypeHandler.findFileType(new ByteArray(new byte[0])));
        assertFalse(FileTypeHandler.findFileType(new ByteArray("hello world".getBytes(StandardCharsets.UTF_8))).startsWith("manifest"));

        // near misses must not be detected as a manifest of the wrong kind
        assertNotEquals("manifestv0", FileTypeHandler.findFileType(new ByteArray(new LE().i32(5).u8(1).toBytes())));
        assertNotEquals("manifestv2", FileTypeHandler.findFileType(new ByteArray(new LE().i32(1).i32(2).str32("x").toBytes())));
    }

    @Test
    public void testSniffingRandomData()
    {
        // small counts followed by random data: sniffing and parsing must never fail with anything but a ParseException
        Random rnd = new Random(7);
        for (int n = 0; n < 2000; n++)
        {
            byte[] b = new byte[5 + rnd.nextInt(40)];
            rnd.nextBytes(b);
            b[0] = (byte) rnd.nextInt(4);
            b[1] = 0;
            b[2] = 0;
            b[3] = 0;
            if (rnd.nextBoolean())
            {
                b[5 % b.length] = (byte) rnd.nextInt(6);
                b[6 % b.length] = 0;
                b[7 % b.length] = 0;
                b[8 % b.length] = 0;
            }

            String type = FileTypeHandler.findFileType(new ByteArray(b));
            assertNotNull(type);

            checkConsistent(b, parser.canParseV0(b), () -> parser.parseV0(new ByteArray(b)));
            checkConsistent(b, parser.canParseV1(b), () -> parser.parseV1(new ByteArray(b)));
            checkConsistent(b, parser.canParseV2(b), () -> parser.parseV2(new ByteArray(b)));
            checkConsistent(b, parser.canParseAtlas(b), () -> parser.parseAtlas(new ByteArray(b)));
        }
    }

    /**
     * canParse true means parse succeeds, canParse false means parse throws a ParseException
     */
    private static void checkConsistent(byte[] data, boolean canParse, Runnable parse)
    {
        if (canParse)
        {
            assertDoesNotThrow(parse::run, () -> "canParse true but parse failed for " + HexFormat.of().formatHex(data));
        } else
        {
            assertThrows(ParseException.class, parse::run, () -> "canParse false but parse did not throw a ParseException for " + HexFormat.of().formatHex(data));
        }
    }
}

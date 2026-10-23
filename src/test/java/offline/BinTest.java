package offline;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.ByteArray;
import no.stelar7.cdragon.util.types.math.*;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BIN (PROP v1-3, PTCH, headerless) write/parse round trips on a synthetic file covering every value type
 */
public class BinTest
{
    @BeforeAll
    public static void offlineHashes()
    {
        // BINParser/FileTypeHandler would otherwise load the bundled hash tables (or fall back to the network)
        OfflineHashes.useEmpty();
    }

    private static BINValue v(String hash, BINValueType t, Object o)
    {
        return new BINValue(hash, t, o);
    }

    private static BINContainer stringContainer()
    {
        BINContainer bc = new BINContainer(BINValueType.STRING, 0, 0, new ArrayList<>());
        bc.getData().add("x");
        bc.getData().add("yy");
        return bc;
    }

    private static BINFile sample()
    {
        BINFile f = new BINFile();
        f.setHeader(new BINHeader("PROP", 3, 0, true, new ArrayList<>(), 1, 0));
        f.getLinkedFiles().add("data/a.bin");
        f.getLinkedFiles().add("data/æøå.bin");

        BINEntry e = new BINEntry(0, "1234ABCD", "FFFFFFFF", (short) 0, new ArrayList<>());
        e.getValues().add(v("00000001", BINValueType.BOOLEAN, true));
        e.getValues().add(v("00000002", BINValueType.BYTE, (byte) 0xF0));
        e.getValues().add(v("00000003", BINValueType.SHORT, (short) 0xFFF0));
        e.getValues().add(v("00000004", BINValueType.INT, 0xF0000000));
        e.getValues().add(v("00000005", BINValueType.LONG, -2L));
        e.getValues().add(v("00000006", BINValueType.FLOAT, 1.5f));
        Vector3f v3 = new Vector3f();
        v3.x = 1;
        v3.y = 2;
        v3.z = Float.NaN;
        e.getValues().add(v("00000007", BINValueType.V3_FLOAT, v3));
        Matrix4f m = new Matrix4f();
        m.m01(5);
        e.getValues().add(v("00000008", BINValueType.M4X4_FLOAT, m));
        Vector4b c = new Vector4b();
        c.setX((byte) 255);
        c.setW((byte) 1);
        e.getValues().add(v("00000009", BINValueType.RGBA_BYTE, c));
        e.getValues().add(v("0000000A", BINValueType.STRING, "a\\b\"c æ"));
        e.getValues().add(v("0000000B", BINValueType.STRING_HASH, "DEADBEEF"));
        e.getValues().add(v("0000000C", BINValueType.WAD_LINK, "0123456789ABCDEF"));
        e.getValues().add(v("0000000D", BINValueType.LINK_OFFSET, "CAFEBABE"));
        e.getValues().add(v("0000000E", BINValueType.CONTAINER2, stringContainer()));

        BINContainer bc2   = new BINContainer(BINValueType.EMBEDDED, 0, 0, new ArrayList<>());
        BINStruct    inner = new BINStruct("ABABABAB", 0, (short) 0, new ArrayList<>());
        inner.getData().add(v("00000010", BINValueType.FLOAT, 3f));
        bc2.getData().add(inner);
        e.getValues().add(v("0000000F", BINValueType.CONTAINER, bc2));
        e.getValues().add(v("00000011", BINValueType.STRUCTURE, new BINStruct("00000000", 0, (short) 0, new ArrayList<>())));

        BINData opt = new BINData(BINValueType.STRING_HASH, (byte) 0, new ArrayList<>());
        opt.getData().add("11112222");
        e.getValues().add(v("00000012", BINValueType.OPTIONAL_DATA, opt));
        e.getValues().add(v("00000013", BINValueType.OPTIONAL_DATA, new BINData(BINValueType.INT, (byte) 0, new ArrayList<>())));

        BINMap map = new BINMap(BINValueType.STRING, BINValueType.EMBEDDED, 0, 0, new ArrayList<>());
        map.getData().add(new Vector2<>("k\"ey", inner));
        e.getValues().add(v("00000014", BINValueType.PAIR, map));
        e.getValues().add(v("00000015", BINValueType.BOOLEAN_FLAGS, false));
        e.getValues().add(v("00000016", BINValueType.V3_SHORT, new Vector3s()));
        f.getEntries().add(e);

        // objects of this class have 64-bit hash fields (patch 16.21+)
        BINEntry wide = new BINEntry(0, Integer.toHexString(BINParser.WIDE_HASH_CLASS).toUpperCase(Locale.ROOT), "00000077", (short) 0, new ArrayList<>());
        wide.getValues().add(v("00000001", BINValueType.STRING_HASH, "0123456789ABCDEF"));
        wide.getValues().add(v("00000002", BINValueType.STRING_HASH, "00000099"));
        f.getEntries().add(wide);

        f.getPatches().add(new BINPatchEntry(0x1234, "path.to.thing", BINValueType.STRING, "patched"));
        f.getPatches().add(new BINPatchEntry(0x1235, "other", BINValueType.LINK_OFFSET, "00000042"));
        return f;
    }

    @Test
    public void testPatchRoundTrip()
    {
        byte[]    bytes  = sample().toBytes();
        BINParser parser = new BINParser(true);
        assertTrue(parser.canParse(bytes));
        assertEquals("PTCH", new String(bytes, 0, 4, StandardCharsets.ISO_8859_1));

        BINFile parsed = parser.parse(new ByteArray(bytes));
        assertArrayEquals(bytes, parsed.toBytes(), "write(parse(write(x))) is byte identical");

        assertTrue(parsed.getHeader().isPatch());
        assertEquals(3, parsed.getHeader().getVersion());
        assertEquals(List.of("data/a.bin", "data/æøå.bin"), parsed.getLinkedFiles());
        assertEquals(2, parsed.getEntries().size());
        assertEquals(2, parsed.getPatches().size());
        assertEquals("patched", parsed.getPatches().get(0).getValue());

        BINEntry e = parsed.getEntries().get(0);
        assertEquals("1234ABCD", e.getType());
        assertEquals("FFFFFFFF", e.getHash());
        assertEquals(21, e.getValues().size());
        assertEquals(true, e.getIfPresent("00000001").getValue());
        assertEquals((byte) 0xF0, e.getIfPresent("00000002").getValue());
        assertEquals((short) 0xFFF0, e.getIfPresent("00000003").getValue());
        assertEquals(0xF0000000, e.getIfPresent("00000004").getValue());
        assertEquals(-2L, e.getIfPresent("00000005").getValue());
        assertEquals(1.5f, e.getIfPresent("00000006").getValue());
        assertTrue(Float.isNaN(((Vector3f) e.getIfPresent("00000007").getValue()).z));
        assertEquals("a\\b\"c æ", e.getIfPresent("0000000A").getValue());
        assertEquals("DEADBEEF", e.getIfPresent("0000000B").getValue());
        assertEquals("0123456789ABCDEF", e.getIfPresent("0000000C").getValue());
        assertEquals("CAFEBABE", e.getIfPresent("0000000D").getValue());
        assertEquals(List.of("x", "yy"), ((BINContainer) e.getIfPresent("0000000E").getValue()).getData());

        BINContainer embedded = (BINContainer) e.getIfPresent("0000000F").getValue();
        assertEquals(BINValueType.EMBEDDED, embedded.getType());
        assertEquals(3f, ((BINStruct) embedded.getData().get(0)).getIfPresent("00000010").getValue());

        assertEquals(List.of("11112222"), ((BINData) e.getIfPresent("00000012").getValue()).getData());
        assertTrue(((BINData) e.getIfPresent("00000013").getValue()).getData().isEmpty());
        BINMap map = (BINMap) e.getIfPresent("00000014").getValue();
        assertTrue(map.get("k\"ey").isPresent());

        BINEntry wide = parsed.getEntries().get(1);
        assertEquals("0123456789ABCDEF", wide.getIfPresent("00000001").getValue(), "wide hash fields are 64-bit");

        assertFalse(parser.getCollectedHashes().isEmpty(), "hashes are collected");
    }

    @Test
    public void testJson()
    {
        BINFile parsed = new BINParser().parse(new ByteArray(sample().toBytes()));
        String  json   = parsed.toJson();
        assertNotNull(json);
        // escaped strings must survive in valid json
        com.google.gson.JsonElement tree = com.google.gson.JsonParser.parseString(json);
        assertTrue(tree.isJsonObject() || tree.isJsonArray());
        assertTrue(json.contains("a\\\\b\\\"c æ"), "backslash and quote are escaped");
    }

    @Test
    public void testOlderPropVersions()
    {
        BINParser parser = new BINParser();
        for (int ver = 1; ver <= 2; ver++)
        {
            BINFile f = sample();
            f.setHeader(new BINHeader("PROP", ver, 0, false, new ArrayList<>(), 0, 0));
            byte[]  b = f.toBytes();
            BINFile p = parser.parse(new ByteArray(b));
            assertArrayEquals(b, p.toBytes(), "v" + ver + " round trip");
            assertEquals(ver, p.getHeader().getVersion());
            assertFalse(p.getHeader().isPatch());
            assertTrue(p.getPatches().isEmpty(), "patches only exist in PTCH files");
            if (ver == 1)
            {
                assertTrue(p.getLinkedFiles().isEmpty(), "v1 has no linked files");
            } else
            {
                assertEquals(2, p.getLinkedFiles().size());
            }
            assertEquals(2, p.getEntries().size());
        }
    }

    @Test
    public void testHeaderless()
    {
        BINFile  hl = new BINFile();
        BINEntry he = new BINEntry(0, null, null, (short) 0, new ArrayList<>());
        he.getValues().add(v("00000000", BINValueType.CONTAINER, stringContainer()));
        hl.getEntries().add(he);
        byte[] hb = hl.toBytes();

        BINParser parser = new BINParser();
        assertTrue(parser.canParse(hb));
        BINFile        hp     = parser.parse(new ByteArray(hb));
        List<BINValue> values = hp.getEntries().get(0).getValues();
        assertEquals(1, values.size());
        assertEquals(List.of("x", "yy"), ((BINContainer) values.get(0).getValue()).getData());
    }

    @Test
    public void testNotABin()
    {
        BINParser parser = new BINParser();
        for (byte[] notBin : new byte[][]{"hello world, not a bin file".getBytes(StandardCharsets.UTF_8), new byte[2], new byte[0]})
        {
            assertFalse(parser.canParse(notBin));
            assertThrows(ParseException.class, () -> parser.parse(new ByteArray(notBin)));
        }
    }

    @Test
    public void testCorrupt()
    {
        BINParser parser = new BINParser();
        byte[]    bytes  = sample().toBytes();

        byte[] truncated = Arrays.copyOf(bytes, bytes.length - 3);
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(truncated)));

        // PTCH(4) + patch version(4) + unknown(4) + PROP(4) + version(4) + link count(4) + links + entry count(4) + 2 entry types(8)
        int    lengthOffset = 4 + 4 + 4 + 4 + 4 + 4 + (2 + 10) + (2 + "data/æøå.bin".getBytes(StandardCharsets.UTF_8).length) + 4 + 8;
        byte[] badLength    = bytes.clone();
        badLength[lengthOffset] += 1;
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(badLength)), "entry length mismatch");

        byte[] badVersion = bytes.clone();
        badVersion[16] = 9;
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(badVersion)), "version 9");
    }
}

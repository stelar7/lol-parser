package offline;

import no.stelar7.cdragon.types.rst.*;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RST string tables, versions 2-5, with 38/39/40 hash bits, built in code
 */
public class RstTest
{
    /**
     * strings: "hello" at 0, "" at 6, "wörld" at 7, and a binary (trenc) entry after them for versions before 5
     */
    private static byte[] rst(int version, int bits, boolean config)
    {
        LE data = new LE().cstr("hello").cstr("").cstr("wörld");
        int trencOffset = data.size();
        if (version < 5)
        {
            data.u8(0xFF).i16(3).u8(1).u8(2).u8(3);
        }
        long[][] entries = {{0, 0x1111}, {6, 0x2222}, {7, 0x3333}, {trencOffset, 0x4444}};

        LE w = new LE().ascii("RST").u8(version);
        if (version == 2)
        {
            w.u8(config ? 1 : 0);
            if (config)
            {
                w.str32("conf");
            }
        }

        int n = version < 5 ? 4 : 3;
        w.i32(n);
        for (int k = 0; k < n; k++)
        {
            w.i64((entries[k][0] << bits) | entries[k][1]);
        }
        if (version < 5)
        {
            w.u8(1);
        }
        return w.bytes(data.toBytes()).toBytes();
    }

    @Test
    public void testVersions()
    {
        RSTParser parser = new RSTParser();
        int[][]   cases  = {{2, 40}, {3, 40}, {4, 39}, {4, 38}, {5, 38}, {5, 39}};
        for (int[] c : cases)
        {
            int    version = c[0];
            int    bits    = c[1];
            String name    = "v" + version + " " + bits + " bits";

            byte[] bytes = rst(version, bits, true);
            assertTrue(parser.canParse(bytes), name);
            RSTFile f = parser.parse(new ByteArray(bytes));
            assertEquals(version, f.getMajor(), name);
            assertEquals(bits, f.getHashBits(), name + ": detected hash bits");

            Map<Long, String> e = f.getEntries();
            assertEquals("hello", e.get(0x1111L), name);
            assertEquals("", e.get(0x2222L), name);
            assertEquals("wörld", e.get(0x3333L), name);
            if (version < 5)
            {
                assertEquals(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}), e.get(0x4444L), name + ": binary entry as base64");
            } else
            {
                assertEquals(3, e.size(), name);
            }

            if (version == 2)
            {
                assertEquals("conf", f.getConfig());
            } else
            {
                assertNull(f.getConfig());
            }
        }
    }

    @Test
    public void testVersion2WithoutConfig()
    {
        RSTFile f = new RSTParser().parse(new ByteArray(rst(2, 40, false)));
        assertNull(f.getConfig());
        assertEquals("hello", f.getEntries().get(0x1111L));
    }

    @Test
    public void testNamedEntries()
    {
        RSTFile f = new RSTParser().parse(new ByteArray(rst(3, 40, false)));
        assertEquals("hello", f.getFromHash(0x1111L, "missing"));
        assertEquals("missing", f.getFromHash(0x9999L, "missing"));

        // full 64-bit hashes are masked to the hash bits of the file; lines that are not "hash key" are skipped
        Map<Long, String> names = RSTKeyNames.parse(Stream.of("ABCD000000001111 greeting", "0000000000003333 planet", "not a hash line", ""));
        assertEquals(2, names.size());
        Map<String, String> named = f.getNamedEntries(names);
        assertEquals("hello", named.get("greeting"));
        assertEquals("wörld", named.get("planet"));
        assertEquals("", named.get("{0000002222}"), "unnamed entries are keyed by their hex hash");
    }

    @Test
    public void testInvalid()
    {
        RSTParser parser = new RSTParser();
        byte[]    hello  = "hello".getBytes(StandardCharsets.UTF_8);
        assertFalse(parser.canParse(hello));
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(hello)));
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(new byte[]{'R', 'S'})));

        byte[] v6 = rst(3, 40, false);
        v6[3] = 6;
        assertTrue(parser.canParse(v6), "canParse only checks the magic");
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(v6)), "unsupported version");

        byte[] truncated = Arrays.copyOf(rst(3, 40, false), 20);
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(truncated)));

        // an entry whose offset points past the string data
        byte[] badOffset = new LE().ascii("RST").u8(3).i32(1).i64((100L << 40) | 1).u8(0).cstr("x").toBytes();
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(badOffset)));
    }
}

package offline;

import no.stelar7.cdragon.types.inibin.InibinParser;
import no.stelar7.cdragon.types.inibin.data.InibinFile;
import no.stelar7.cdragon.types.packagemanifest.PackagemanifestParser;
import no.stelar7.cdragon.types.packagemanifest.data.*;
import no.stelar7.cdragon.types.raf.RAFParser;
import no.stelar7.cdragon.types.raf.data.RAFFile;
import no.stelar7.cdragon.types.releasemanifest.ReleasemanifestParser;
import no.stelar7.cdragon.types.releasemanifest.data.*;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.Test;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Old client formats built in code: inibin, RAF archives, RLSM release manifests and PKG1 package manifests
 */
public class LegacyFormatsTest
{
    @Test
    public void testInibin()
    {
        int mask = 1 | (1 << 2) | (1 << 4) | (1 << 5) | (1 << 11) | (1 << 12);
        LE  ini  = new LE().u8(2).i16(10).i16(mask);
        // int32
        ini.i16(1).i32(100).i32(-5);
        // u8 / 10
        ini.i16(1).i32(101).u8(250);
        // u8
        ini.i16(1).i32(102).u8(200);
        // 9 booleans, packed 8 per byte
        ini.i16(9);
        for (int k = 0; k < 9; k++)
        {
            ini.i32(200 + k);
        }
        ini.u8(0b10000001).u8(1);
        // float vec4 (raw bytes)
        ini.i16(1).i32(300).f32(1, 2, 3, 4);
        // strings: u16 offsets relative to the end of the offset table
        ini.i16(2).i32(400).i32(0xF0000001).i16(0).i16(4).cstr("abc").cstr("de");

        InibinFile          f = new InibinParser().parse(new ByteArray(ini.toBytes()));
        Map<String, Object> k = f.getKeys();
        assertEquals(2, f.getHeader().getVersion());
        assertEquals(15, k.size());
        assertEquals(-5, k.get("100"));
        assertEquals(25f, k.get("101"));
        assertEquals(200, k.get("102"));
        assertEquals("true", k.get("200"));
        assertEquals("false", k.get("201"));
        assertEquals("true", k.get("207"));
        assertEquals("true", k.get("208"), "the ninth bool is the first bit of the second byte");
        assertEquals(16, ((byte[]) k.get("300")).length);
        assertEquals("abc", k.get("400"));
        assertEquals("de", k.get(Integer.toUnsignedString(0xF0000001)), "keys are unsigned");
    }

    @Test
    public void testInibinInvalid()
    {
        InibinParser parser = new InibinParser();
        byte[]       bad    = new LE().u8(2).i16(0).i16(1 << 13).i16(0).toBytes();
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(bad)), "unknown value set bit");

        byte[] version = new LE().u8(1).i16(0).i16(0).toBytes();
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(version)), "unsupported version");

        byte[] count = new LE().u8(2).i16(0).i16(1).i16(1000).i32(1).toBytes();
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(count)), "key count larger than the data");

        byte[] truncated = new LE().u8(2).i16(0).i16(1).i16(1).i32(1).i16(0).toBytes();
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(truncated)), "truncated value");
    }

    @Test
    public void testRaf() throws Exception
    {
        // header: magic, version, manager index, files offset, paths offset (the path list comes before the file list here)
        LE raf = new LE().i32(RAFParser.MAGIC).i32(1).i32(0).i32(0).i32(0);
        int pathsOffset = raf.size();
        // path list: size, count, entries (offset, length) relative to the path list, strings
        String[] names    = {"DATA/a.txt", "DATA/bb.txt"};
        int      strStart = 8 + names.length * 8;
        raf.i32(0).i32(names.length).i32(strStart).i32(names[0].length() + 1).i32(strStart + names[0].length() + 1).i32(names[1].length() + 1);
        raf.cstr(names[0]).cstr(names[1]);
        raf.u8(0xAA).u8(0xBB);
        int filesOffset = raf.size();
        // files: count, (hash, offset in .dat, size, path index)
        raf.i32(2).i32(0x1234).i32(0).i32(5).i32(1).i32(0x5678).i32(5).i32(3).i32(0);
        byte[] bytes = raf.toBytes();
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(12, filesOffset).putInt(16, pathsOffset);

        Path dir = Files.createTempDirectory("cdragon-raf");
        try
        {
            Path rafPath = dir.resolve("test.raf");
            Files.write(rafPath, bytes);
            Path datPath = dir.resolve("test.raf.dat");
            Files.write(datPath, "helloabc".getBytes(StandardCharsets.UTF_8));

            RAFFile rf = new RAFParser().parse(rafPath);
            assertEquals(List.of(names), rf.getStrings());
            assertEquals(2, rf.getFileCount());
            assertEquals(1, rf.getFiles().get(0).getPathIndex());
            assertEquals(0, rf.getFiles().get(1).getPathIndex());
            assertEquals(5, rf.getFiles().get(0).getSize());
            assertEquals("hello", new String(rf.readContentFromData(rf.getFiles().get(0)), StandardCharsets.UTF_8));
            assertEquals("abc", new String(rf.readContentFromData(rf.getFiles().get(1)), StandardCharsets.UTF_8));

            // files offset outside the file
            byte[] badOffset = bytes.clone();
            ByteBuffer.wrap(badOffset).order(ByteOrder.LITTLE_ENDIAN).putInt(12, 100000);
            Path badPath = dir.resolve("bad.raf");
            Files.write(badPath, badOffset);
            assertThrows(ParseException.class, () -> new RAFParser().parse(badPath));
        } finally
        {
            deleteTree(dir);
        }
    }

    private static byte[] releaseManifest()
    {
        LE rl = new LE().ascii("RLSM").i32(0).i32(0).i32(0);
        // directories: (name, subdir start, subdir count, file start, file count); root has one subdirectory with one file
        rl.i32(2).i32(0).i32(1).i32(1).i32(0).i32(0).i32(1).i32(0).i32(0).i32(0).i32(1);
        // files: name, version, md5, flags, size, compressed size, unknown, type, padding
        byte[] md5 = new byte[16];
        for (int i = 0; i < md5.length; i++)
        {
            md5[i] = (byte) (i * 17);
        }
        rl.i32(1).i32(2).i32(0).bytes(md5).i32(0).i32(42).i32(0).i32(0).i16(0).i16(0);
        // strings: count, size, data
        LE strings = new LE().cstr("").cstr("sub").cstr("file.txt");
        rl.i32(3).i32(strings.size()).bytes(strings.toBytes());
        return rl.toBytes();
    }

    @Test
    public void testReleasemanifest()
    {
        ReleasemanifestParser    parser = new ReleasemanifestParser();
        byte[]                   bytes  = releaseManifest();
        ReleasemanifestDirectory root   = parser.parse(new ByteArray(bytes));
        assertTrue(parser.canParse(bytes));
        assertEquals(List.of("/sub/file.txt"), root.generateFilelist("", ""));

        ReleasemanifestContentFile file = root.getSubDirectories().get(0).getFiles().get(0);
        assertEquals("file.txt", file.getName());
        assertEquals(42, file.getSize());
        assertEquals("00112233445566778899aabbccddeeff", file.getHash().toLowerCase(Locale.ROOT));
    }

    @Test
    public void testReleasemanifestInvalid()
    {
        ReleasemanifestParser parser = new ReleasemanifestParser();

        // make the child directory point back at the root
        byte[] cycle = releaseManifest();
        ByteBuffer.wrap(cycle).order(ByteOrder.LITTLE_ENDIAN).putInt(16 + 4 + 20 + 4, 0).putInt(16 + 4 + 20 + 8, 1);
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(cycle)), "directory loop");

        byte[] notRlsm = "0123456789abcdefgh".getBytes(StandardCharsets.UTF_8);
        assertFalse(parser.canParse(notRlsm));
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(notRlsm)));

        byte[] truncated = Arrays.copyOf(releaseManifest(), 60);
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(truncated)));
    }

    @Test
    public void testPackagemanifest()
    {
        String              text = "PKG1\r\n/a/b.dds,BIN_0x1,10,20,0\r\n/c.dds,BIN_0x2,30,40,1\n\n";
        PackagemanifestFile pm   = new PackagemanifestParser().parse(new ByteArray(text.getBytes(StandardCharsets.UTF_8)));
        assertEquals("PKG1", pm.getHeader());
        assertEquals(2, pm.getFiles().size());

        PackagemanifestLine pl = pm.getFiles().get(0);
        assertEquals("/a/b.dds", pl.getFilePath());
        assertEquals("BIN_0x1", pl.getContainedInFile());
        assertEquals(10, pl.getContainedOffset());
        assertEquals(20, pl.getFileSize());
        assertEquals(1, pm.getFiles().get(1).getUnknown());

        PackagemanifestParser parser = new PackagemanifestParser();
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray("NOPE\r\n".getBytes(StandardCharsets.UTF_8))));
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray("PKG1\r\n/a,b,1\r\n".getBytes(StandardCharsets.UTF_8))));
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray("PKG1\r\n/a,b,x,1,0\r\n".getBytes(StandardCharsets.UTF_8))));
    }

    static void deleteTree(Path dir) throws Exception
    {
        try (var walk = Files.walk(dir))
        {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList())
            {
                Files.deleteIfExists(p);
            }
        }
    }
}

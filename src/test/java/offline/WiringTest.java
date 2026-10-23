package offline;

import com.github.luben.zstd.Zstd;
import com.google.gson.*;
import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.atlas.AtlasParser;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.types.releasemanifest.ReleasemanifestParser;
import no.stelar7.cdragon.types.rst.RSTParser;
import no.stelar7.cdragon.types.scb.SCBParser;
import no.stelar7.cdragon.types.scb.data.SCBFile;
import no.stelar7.cdragon.types.sco.SCOParser;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.types.wad.data.content.WADContentHeaderV2;
import no.stelar7.cdragon.types.wad.data.header.WADHeaderV3;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * File type wiring: canParse/parse of the sniffable parsers, FileTypeHandler detection, and how WAD entries are written when extracting
 * (TEX -> png only, js written byte-identical, files without a name sniffed).
 * <p>
 * The WAD and BIN hash name tables are replaced by empty maps while these tests run, so nothing reads the large hash json files or the network.
 */
public class WiringTest
{
    private static final byte[] JUNK = "hello world, this is not any of these formats".getBytes(StandardCharsets.UTF_8);
    private static final String SCO  = "[ObjectBegin]\nName= box\nCentralPoint= 0 0 0\nVerts= 3\n0 0 0\n1 0 0\n0 1 0\nFaces= 1\n3 0 1 2 mat 0 1 0 0 0 1\n[ObjectEnd]\n";
    // PROP v3, no linked files, no entries
    private static final byte[] PROP = {'P', 'R', 'O', 'P', 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private Path temp;

    @BeforeAll
    public static void useEmptyHashTables()
    {
        // must run before BINParser is initialized, its static block preloads the bin hashes
        OfflineHashes.useEmpty();
    }

    @BeforeEach
    public void createTemp() throws IOException
    {
        temp = Files.createTempDirectory("cdragon-wiring");
    }

    @AfterEach
    public void deleteTemp() throws IOException
    {
        try (Stream<Path> s = Files.walk(temp))
        {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList())
            {
                Files.delete(p);
            }
        }
    }

    private static byte[] atlas()
    {
        return new LE().i32(1).str32("assets/bundle.dds").i32(1).str32("icon").f32(0, 0, 0.5f, 0.25f).i32(0).toBytes();
    }

    private static byte[] tex(int format)
    {
        // 4x4, one mip, one DXT1 block: color0 = pure red, every pixel index 0
        return new LE().ascii("TEX").u8(0).i16(4).i16(4).u8(1).u8(format).u8(0).u8(0)
                       .i16(0xF800).i16(0x001F).i32(0)
                       .toBytes();
    }

    private static byte[] scb()
    {
        return new LE().ascii("r3d2Mesh").i16(3).i16(2).fixed("box", 128)
                       .i32(3).i32(1).i32(0)
                       .f32(0, 0, 0, 1, 1, 0)
                       .i32(0)
                       .f32(0, 0, 0, 1, 0, 0, 0, 1, 0)
                       .f32(0, 0, 0)
                       .i32(0).i32(1).i32(2)
                       .fixed("mat", 64)
                       .f32(0, 1, 0, 0, 0, 1)
                       .toBytes();
    }

    private static byte[] headerlessBin()
    {
        // a container of 2 u32: element type, size, count, values
        return new LE().u8(BINValueType.INT.toRaw()).i32(4 + 8).i32(2).i32(7).i32(9).toBytes();
    }

    private static byte[] wad(List<byte[]> files, long[] hashes)
    {
        int headerSize = 4 + 256 + 8 + 4;
        int pos        = headerSize + files.size() * 32;
        LE  toc        = new LE();
        LE  data       = new LE();
        for (int i = 0; i < files.size(); i++)
        {
            byte[] f = files.get(i);
            toc.i64(hashes[i]).i32(pos).i32(f.length).i32(f.length).u8(0).u8(0).i16(0).i64(0x1122334455667788L + i);
            data.bytes(f);
            pos += f.length;
        }
        return new LE().ascii("RW").u8(3).u8(1).zeros(256).i64(42).i32(files.size()).bytes(toc.toBytes()).bytes(data.toBytes()).toBytes();
    }

    private static void assertParseThrows(Parseable<?> p, byte[] data)
    {
        assertThrows(ParseException.class, () -> p.parse(new ByteArray(data)));
    }

    @Test
    public void testAtlasCanParse()
    {
        AtlasParser ap = new AtlasParser();
        assertTrue(ap.canParse(atlas()));
        assertEquals("icon", ap.parse(new ByteArray(atlas())).getData().get("assets/bundle.dds").get(0).getName());
        assertFalse(ap.canParse(JUNK));
        assertFalse(ap.canParse(new byte[0]));
        assertParseThrows(ap, JUNK);

        byte[] truncated = Arrays.copyOf(atlas(), atlas().length - 2);
        assertTrue(ap.canParse(truncated), "a truncated atlas still looks like an atlas");
        assertParseThrows(ap, truncated);
    }

    @Test
    public void testBinCanParse() throws IOException
    {
        BINParser bp = new BINParser();
        assertTrue(bp.canParse(PROP));
        assertNotNull(bp.parse(new ByteArray(PROP)));
        assertTrue(bp.canParse(headerlessBin()));
        assertEquals(1, bp.parse(new ByteArray(headerlessBin())).getEntries().size());
        assertFalse(bp.canParse(JUNK));
        assertFalse(bp.canParse(new byte[3]));
        assertParseThrows(bp, JUNK);
        assertParseThrows(bp, atlas());
        assertFalse(bp.canParse(atlas()), "bin does not claim atlases");
        assertFalse(new AtlasParser().canParse(PROP), "atlas does not claim bins");

        Files.write(temp.resolve("real.bin"), PROP);
        Files.write(temp.resolve("fake.bin"), JUNK);
        Files.write(temp.resolve("other.txt"), PROP);
        List<BINFile> walked;
        try (Stream<Path> s = Files.list(temp))
        {
            walked = s.filter(UtilHandler.IS_BIN_PREDICATE).map(bp::tryParse).filter(Objects::nonNull).collect(Collectors.toList());
        }
        assertEquals(1, walked.size(), "tryParse skips .bin files that are not bins");
    }

    @Test
    public void testMagicParsersCanParse()
    {
        RSTParser rp = new RSTParser();
        assertFalse(rp.canParse(JUNK));
        assertFalse(rp.canParse("RST".getBytes(StandardCharsets.US_ASCII)));
        assertTrue(rp.canParse(new byte[]{'R', 'S', 'T', 4}));
        assertParseThrows(rp, JUNK);

        ReleasemanifestParser rmp = new ReleasemanifestParser();
        assertFalse(rmp.canParse(JUNK));
        assertTrue(rmp.canParse("RLSM0123456789ab".getBytes(StandardCharsets.US_ASCII)));
        assertParseThrows(rmp, JUNK);

        SCBParser scbp = new SCBParser();
        assertTrue(scbp.canParse(scb()));
        assertFalse(scbp.canParse("r3d".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(scbp.canParse(JUNK));
        assertParseThrows(scbp, JUNK);

        SCOParser scop = new SCOParser();
        assertTrue(scop.canParse(SCO.getBytes(StandardCharsets.UTF_8)));
        assertTrue(scop.canParse(("﻿\r\n" + SCO).getBytes(StandardCharsets.UTF_8)), "BOM and leading newline");
        assertFalse(scop.canParse("[Obj".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(scop.canParse(JUNK));
        assertParseThrows(scop, JUNK);

        WADParser wp = new WADParser();
        assertFalse(wp.canParse(JUNK));
        assertFalse(wp.canParse(new byte[]{'R', 'W', 3, 5}), "unsupported version");
        assertTrue(wp.canParse(new byte[]{'R', 'W', 3, 4}));
        assertParseThrows(wp, JUNK);
    }

    @Test
    public void testParseCompressedWad() throws IOException
    {
        WADParser wp            = new WADParser();
        Path      notCompressed = temp.resolve("not.wad.compressed");
        Files.write(notCompressed, JUNK);
        assertThrows(ParseException.class, () -> wp.parseCompressed(notCompressed));

        Path compressed = temp.resolve("ok.wad.compressed");
        Files.write(compressed, Zstd.compress(wad(List.of(JUNK), new long[]{1})));
        assertEquals(1, wp.parseCompressed(compressed).getContentHeaders().size());
    }

    @Test
    public void testScbAndScoToObj()
    {
        SCBFile fromScb = new SCBParser().parse(new ByteArray(scb()));
        SCBFile fromSco = new SCOParser().parse(new ByteArray(SCO.getBytes(StandardCharsets.UTF_8)));
        String  obj     = fromSco.toOBJ();
        assertTrue(obj.contains("v 1 0 0"), obj);
        assertTrue(obj.contains("vt 1 0"), obj);
        assertTrue(obj.contains("usemtl mat"), obj);
        assertTrue(obj.contains("f 1/1 2/2 3/3"), obj);
        assertEquals(obj, fromScb.toOBJ(), "scb and sco of the same mesh give the same obj");
    }

    @Test
    public void testFileTypeDetection()
    {
        Map<String, byte[]> expected = new LinkedHashMap<>();
        expected.put("empty", new byte[0]);
        expected.put("atlas", atlas());
        expected.put("bin", headerlessBin());
        expected.put("sco", SCO.getBytes(StandardCharsets.UTF_8));
        expected.put("scb", scb());
        expected.put("tex", tex(0x0A));
        expected.put("ogg", new byte[]{'O', 'g', 'g', 'S', 0, 2, 0, 0});
        expected.put("png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
        expected.put("dds", new byte[]{'D', 'D', 'S', ' ', 124, 0, 0, 0});
        expected.put("ktx", KTXMagic());
        expected.put("json", "{\"a\": [1, 2, 3]}".getBytes(StandardCharsets.UTF_8));
        for (Map.Entry<String, byte[]> e : expected.entrySet())
        {
            assertEquals(e.getKey(), FileTypeHandler.findFileType(new ByteArray(e.getValue())), "type of " + e.getKey());
        }
        // a PROP bin is found by its magic
        assertEquals("bin", FileTypeHandler.findFileType(new ByteArray(PROP)));
    }

    private static byte[] KTXMagic()
    {
        return new LE().bytes(no.stelar7.cdragon.types.ktx.ktx.KTX11Parser.MAGIC).i32(0x04030201).toBytes();
    }

    @Test
    public void testWadHeaders()
    {
        WADFile wad = new WADParser().parse(new ByteArray(wad(List.of(JUNK, PROP, atlas()), new long[]{1, 2, 3})));
        WADHeaderV3 header = assertInstanceOf(WADHeaderV3.class, wad.getHeader());
        assertEquals(3, header.getFileCount());
        assertEquals(42, header.getChecksum());
        assertTrue(wad.getContentHeaders().stream().allMatch(h -> h instanceof WADContentHeaderV2), "3.x entries are V2");
        assertEquals(0x1122334455667788L + 2, ((WADContentHeaderV2) wad.getContentHeaders().get(2)).getSha256());
        assertArrayEquals(PROP, wad.readContentFromHeaderData(wad.getContentHeaders().get(1)));

        // json field names of the (immutable) wad headers are part of the exported json
        Gson       gson  = JsonHandler.getGson();
        JsonObject head  = gson.toJsonTree(wad.getHeader()).getAsJsonObject();
        JsonObject entry = gson.toJsonTree(wad.getContentHeaders().get(0)).getAsJsonObject();
        assertEquals(Set.of("ECDSA", "checksum", "magic", "major", "minor", "fileCount"), head.keySet());
        assertEquals(List.of("duplicate", "subChunkOffset", "sha256", "pathHash", "offset", "compressedFileSize", "fileSize", "compressionType", "subChunkCount"), new ArrayList<>(entry.keySet()));
    }

    @Test
    public void testWadExtractionNaming() throws IOException
    {
        String       js     = "function a(){return 1}\nvar b=2;";
        List<byte[]> files  = List.of(tex(0x0A), tex(0x7F), SCO.getBytes(StandardCharsets.UTF_8), atlas(), headerlessBin(), ("var x = " + js).getBytes(StandardCharsets.UTF_8));
        long[]       hashes = {0x0123456789ABCDEFL, 0x1123456789ABCDEFL, 0x2123456789ABCDEFL, 0x3123456789ABCDEFL, 0x4123456789ABCDEFL, 0x5123456789ABCDEFL};
        Path         path   = temp.resolve("synthetic.wad.client");
        Files.write(path, wad(files, hashes));

        WADFile wad     = new WADParser().parse(path);
        Path    extract = temp.resolve("extract");
        wad.extractFiles(extract, "synthetic");

        Map<String, Path> written = new TreeMap<>();
        try (Stream<Path> s = Files.list(extract.resolve("unknown").resolve("synthetic")))
        {
            s.forEach(p -> written.put(p.getFileName().toString().toLowerCase(Locale.ENGLISH), p));
        }

        assertEquals(6, written.size(), written.keySet().toString());
        assertTrue(written.containsKey("0123456789abcdef.png"), "decodable tex is written as png: " + written.keySet());
        assertFalse(written.containsKey("0123456789abcdef.tex"), "and not as tex");
        assertFalse(written.containsKey("0123456789abcdef.dds"), "and not as dds");
        BufferedImage img = ImageIO.read(written.get("0123456789abcdef.png").toFile());
        assertEquals(4, img.getWidth());
        assertEquals(0xFF0000, img.getRGB(1, 1) & 0xFFFFFF, "png is the decoded red block");

        assertTrue(written.containsKey("1123456789abcdef.tex"), "undecodable tex keeps the original");
        assertTrue(written.containsKey("2123456789abcdef.sco"));
        assertTrue(written.containsKey("3123456789abcdef.atlas"));
        assertTrue(written.containsKey("4123456789abcdef.bin"), "headerless bin");
        assertTrue(written.containsKey("5123456789abcdef.js"));
        assertArrayEquals(files.get(5), Files.readAllBytes(written.get("5123456789abcdef.js")), "js is written byte-identical (not beautified)");
    }

    @Test
    public void testSaveFileWithNoExtension() throws Exception
    {
        // a known name without an extension is sniffed (only atlas and bin, else .unknown)
        WADFile wad = new WADParser().parse(new ByteArray(wad(List.of(JUNK), new long[]{1})));
        Method  m   = WADFile.class.getDeclaredMethod("saveFileWithNoExtension", String.class, Path.class, byte[].class);
        m.setAccessible(true);
        m.invoke(wad, "a", temp, atlas());
        m.invoke(wad, "b", temp, PROP);
        m.invoke(wad, "c", temp, headerlessBin());
        m.invoke(wad, "d", temp, JUNK);
        assertTrue(Files.exists(temp.resolve("a.atlas")));
        assertTrue(Files.exists(temp.resolve("b.bin")));
        assertTrue(Files.exists(temp.resolve("c.bin")));
        assertTrue(Files.exists(temp.resolve("d.unknown")));
    }
}

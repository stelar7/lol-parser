package offline;

import com.github.luben.zstd.Zstd;
import no.stelar7.cdragon.types.rman.RMANParser;
import no.stelar7.cdragon.types.rman.data.*;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.*;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RMAN (patcher manifest) parsing and file extraction, using a hand built FlatBuffers body.
 * The body exercises absent fields (vtables shorter than the table), u64 file sizes, u8 language ids,
 * nested directories and files spread over several bundles.
 */
public class RmanTest
{
    @BeforeAll
    public static void offlineHashes()
    {
        OfflineHashes.useEmpty();
    }

    private static final byte[] C1_RAW = "hello world ".repeat(100).getBytes(StandardCharsets.UTF_8);
    private static final byte[] C2_RAW = "second chunk".repeat(50).getBytes(StandardCharsets.UTF_8);
    private static final byte[] C1     = Zstd.compress(C1_RAW);
    private static final byte[] C2     = Zstd.compress(C2_RAW);

    /**
     * Minimal FlatBuffers writer: tables and vtables are placed upwards from the start of the buffer
     */
    private static final class FlatBuilder
    {
        private final ByteBuffer b   = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
        private       int        top = 4;

        int alloc(int n)
        {
            int p = (top + 7) & ~7;
            top = p + n;
            return p;
        }

        /**
         * vtable with the given field offsets (0 = absent), returns its position
         */
        int vtable(int tableSize, int... offsets)
        {
            int p = alloc(4 + offsets.length * 2);
            b.putShort(p, (short) (4 + offsets.length * 2));
            b.putShort(p + 2, (short) tableSize);
            for (int i = 0; i < offsets.length; i++)
            {
                b.putShort(p + 4 + i * 2, (short) offsets[i]);
            }
            return p;
        }

        int table(int vtable, int size)
        {
            int t = alloc(size);
            b.putInt(t, t - vtable);
            return t;
        }

        void ref(int fieldPos, int target)
        {
            b.putInt(fieldPos, target - fieldPos);
        }

        int string(String s)
        {
            byte[] d = s.getBytes(StandardCharsets.UTF_8);
            int    p = alloc(4 + d.length + 1);
            b.putInt(p, d.length);
            b.put(p + 4, d, 0, d.length);
            return p;
        }

        int vector(int... tables)
        {
            int p = alloc(4 + 4 * tables.length);
            b.putInt(p, tables.length);
            for (int i = 0; i < tables.length; i++)
            {
                ref(p + 4 + 4 * i, tables[i]);
            }
            return p;
        }

        byte[] bytes()
        {
            return Arrays.copyOf(b.array(), top);
        }
    }

    private static byte[] buildBody()
    {
        FlatBuilder f = new FlatBuilder();
        ByteBuffer  b = f.b;

        // root: only 4 fields in the vtable, keys/unknown are absent
        int root = f.table(f.vtable(20, 4, 8, 12, 16), 20);
        b.putInt(0, root);

        // chunks: the second chunk's vtable only has 2 fields, so its uncompressed size is absent (0)
        int chunkVt = f.vtable(20, 4, 12, 16);
        int chunk1  = f.table(chunkVt, 20);
        b.putLong(chunk1 + 4, 0xC1L).putInt(chunk1 + 12, C1.length).putInt(chunk1 + 16, C1_RAW.length);
        int chunk2 = f.table(f.vtable(16, 4, 12), 16);
        b.putLong(chunk2 + 4, 0xC2L).putInt(chunk2 + 12, C2.length);
        // the third chunk lives in another bundle, and is the first chunk of the file
        int chunk3 = f.table(chunkVt, 20);
        b.putLong(chunk3 + 4, 0xC3L).putInt(chunk3 + 12, C2.length).putInt(chunk3 + 16, C2_RAW.length);

        int bundleVt = f.vtable(16, 4, 12);
        int bundle1  = f.table(bundleVt, 16);
        b.putLong(bundle1 + 4, 0xB1L);
        f.ref(bundle1 + 12, f.vector(chunk1, chunk2));
        int bundle2 = f.table(bundleVt, 16);
        b.putLong(bundle2 + 4, 0xB2L);
        f.ref(bundle2 + 12, f.vector(chunk3));
        f.ref(root + 4, f.vector(bundle1, bundle2));

        // language: u8 id at +7 (as flatc lays it out, the bytes before it are padding), name at +8
        int lang = f.table(f.vtable(12, 7, 8), 12);
        b.put(lang + 4, (byte) 0x55).put(lang + 5, (byte) 0x66).put(lang + 6, (byte) 0x77).put(lang + 7, (byte) 3);
        f.ref(lang + 8, f.string("en_US"));
        f.ref(root + 8, f.vector(lang));

        // file: fields 0..9 and 12 (permissions)
        int file = f.table(f.vtable(52, 4, 12, 20, 28, 32, 0, 0, 40, 0, 44, 0, 0, 48), 52);
        b.putLong(file + 4, 0xF1L).putLong(file + 12, 7).putLong(file + 20, 5_000_000_000L).putLong(file + 32, 0b100);
        f.ref(file + 28, f.string("test.bin"));
        b.put(file + 48, (byte) 0x6D);
        int chunks = f.alloc(4 + 8 * 3);
        b.putInt(chunks, 3).putLong(chunks + 4, 0xC3L).putLong(chunks + 12, 0xC1L).putLong(chunks + 20, 0xC2L);
        f.ref(file + 40, chunks);
        f.ref(file + 44, f.string("link"));
        f.ref(root + 12, f.vector(file));

        // directories: root (id 0, no name), DATA (id 7, parent 0)
        int dirVt   = f.vtable(24, 4, 12, 20);
        int rootDir = f.table(dirVt, 24);
        f.ref(rootDir + 20, f.string(""));
        int dataDir = f.table(dirVt, 24);
        b.putLong(dataDir + 4, 7).putLong(dataDir + 12, 0);
        f.ref(dataDir + 20, f.string("DATA"));
        f.ref(root + 16, f.vector(rootDir, dataDir));

        return f.bytes();
    }

    private static byte[] buildManifest(byte major, byte[] body)
    {
        byte[] compressed = Zstd.compress(body);
        return new LE().ascii("RMAN").u8(major).u8(1).u8(0).u8(0)
                       .i32(32).i32(compressed.length).i64(0x1234).i32(body.length).i32(0)
                       .bytes(compressed)
                       .toBytes();
    }

    @Test
    public void testParse()
    {
        RMANFile manifest = new RMANParser().parse(new ByteArray(buildManifest((byte) 2, buildBody())));

        assertEquals("RMAN", manifest.getHeader().getMagic());
        assertEquals(0x1234, manifest.getHeader().getManifestId());
        assertNull(manifest.getSignature());

        RMANFileBody body = manifest.getBody();
        assertEquals(2, body.getBundles().size());
        assertEquals("00000000000000B1", body.getBundles().get(0).getBundleId());
        assertEquals(2, body.getBundles().get(0).getChunks().size());

        RMANFileBodyBundleChunk chunk2 = body.getBundles().get(0).getChunks().get(1);
        assertEquals(0, chunk2.getUncompressedSize(), "a field past the end of the vtable is absent");
        assertEquals(C2.length, chunk2.getCompressedSize());

        assertEquals(-1, body.getHeader().getKeyHeaderOffset(), "absent root field");
        assertEquals(-1, body.getHeader().getUnknownOffset(), "absent root field");

        assertEquals(1, body.getLanguages().size());
        assertEquals(3, body.getLanguages().get(0).getId(), "language id is a u8");
        assertEquals("en_US", body.getLanguages().get(0).getName());

        RMANFileBodyFile file = body.getFiles().get(0);
        assertEquals(0xF1L, file.getFileId());
        assertEquals(5_000_000_000L, file.getFileSize(), "file size is a u64");
        assertEquals("test.bin", file.getName());
        assertEquals("link", file.getSymlink());
        assertEquals(4, file.getLanguageFlags());
        assertEquals(0x6D, file.getPermissions());
        assertEquals(List.of(0xC3L, 0xC1L, 0xC2L), file.getChunkIds());
        assertEquals("DATA/test.bin", file.getFullFilepath(manifest));

        assertEquals(2, body.getDirectories().size());
        assertEquals("DATA", manifest.getDirectory(7).getName());

        // chunk offsets are the sum of the compressed sizes before them in the bundle
        assertEquals(0, manifest.getChunkMap().get(0xC1L).getOffsetToChunk());
        assertEquals(C1.length, manifest.getChunkMap().get(0xC2L).getOffsetToChunk());
        assertEquals("00000000000000B2", manifest.getChunkMap().get(0xC3L).getBundleId());
        assertEquals(2, manifest.getBundlesForFile(file).size());

        assertEquals(1, manifest.getUnchangedFiles(manifest).size());
        assertTrue(manifest.getChangedFiles(manifest).isEmpty());
    }

    @Test
    public void testExtractFile() throws Exception
    {
        RMANFile         manifest = new RMANParser().parse(new ByteArray(buildManifest((byte) 2, buildBody())));
        RMANFileBodyFile file     = manifest.getBody().getFiles().get(0);

        // the uncompressed size of chunk 2 was left out of the manifest on purpose, fill it in for extraction
        RMANFileBodyBundleChunkInfo old2 = manifest.getChunkMap().get(0xC2L);
        manifest.getChunkMap().put(0xC2L, new RMANFileBodyBundleChunkInfo(old2.getBundleId(), old2.getChunkId(), old2.getOffsetToChunk(), old2.getCompressedSize(), C2_RAW.length));

        Path dir = Files.createTempDirectory("cdragon-rman");
        try
        {
            Path bundles = Files.createDirectories(dir.resolve("bundles"));
            Files.write(bundles.resolve("00000000000000B1.bundle"), new LE().bytes(C1).bytes(C2).toBytes());
            Files.write(bundles.resolve("00000000000000B2.bundle"), C2);

            Path out = dir.resolve("out");
            manifest.extractFile(file, bundles, out);

            byte[] expected = new LE().bytes(C2_RAW).bytes(C1_RAW).bytes(C2_RAW).toBytes();
            assertArrayEquals(expected, Files.readAllBytes(out.resolve("DATA/test.bin")), "chunks from both bundles, in file order");

            // the bundle files are closed after extraction
            Files.delete(bundles.resolve("00000000000000B1.bundle"));
        } finally
        {
            try (Stream<Path> files = Files.walk(dir))
            {
                files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @Test
    public void testInvalid()
    {
        RMANParser p    = new RMANParser();
        byte[]     body = buildBody();

        assertThrows(ParseException.class, () -> p.parse(new ByteArray(buildManifest((byte) 3, body))), "unsupported major version");

        byte[] badMagic = buildManifest((byte) 2, body);
        badMagic[0] = 'X';
        assertThrows(ParseException.class, () -> p.parse(new ByteArray(badMagic)), "magic");

        byte[] truncated = Arrays.copyOf(buildManifest((byte) 2, body), 40);
        assertThrows(ParseException.class, () -> p.parse(new ByteArray(truncated)), "body outside the file");

        assertThrows(ParseException.class, () -> p.parse(new ByteArray(new byte[10])), "too small");
    }

    private static RMANFile manifestWithFiles(List<RMANFileBodyFile> files)
    {
        List<RMANFileBodyDirectory> directories = List.of(new RMANFileBodyDirectory("pluginA", 1, 0), new RMANFileBodyDirectory("pluginB", 2, 0), new RMANFileBodyDirectory("pluginC", 3, 0));
        return new RMANFile(null, new RMANFileBody(0, null, List.of(), List.of(), new ArrayList<>(files), directories), null, null);
    }

    private static RMANFileBodyFile file(String name, long directoryId, long chunkId)
    {
        return new RMANFileBodyFile(name, "", directoryId * 100 + chunkId, directoryId, 1, 0, 0, new long[]{chunkId});
    }

    @Test
    public void testChangedFilesAreMatchedByPath()
    {
        // every LCU plugin has its own index.html, so the name alone does not say which file to compare with
        RMANFile oldManifest = manifestWithFiles(List.of(file("index.html", 1, 1), file("index.html", 2, 2)));

        RMANFileBodyFile unchanged = file("index.html", 1, 1);
        RMANFileBodyFile changed   = file("index.html", 2, 3);
        RMANFileBodyFile added     = file("new.json", 3, 4);
        RMANFile         manifest  = manifestWithFiles(List.of(unchanged, changed, added));

        assertEquals(List.of(changed, added), manifest.getChangedFiles(oldManifest));
        assertEquals(List.of(unchanged), manifest.getUnchangedFiles(oldManifest));
    }
}

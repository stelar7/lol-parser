package offline;

import net.jpountz.lz4.LZ4Factory;
import no.stelar7.cdragon.types.bbq.*;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.Test;
import org.tukaani.xz.*;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unity asset bundles built in code: UnityFS bundles (uncompressed + LZ4 blocks, block info at the start or the end, UnityCN encryption, LZHAM),
 * UnityRaw / UnityWeb containers, and the serialized files inside them (format 22, and format 26 with a shared type sub tree)
 */
public class BbqTest
{
    private static final byte[] TREE_HASH   = filled(16, 0x11);
    private static final byte[] SHARED_HASH = filled(16, 0x22);
    private static final String EXTERNAL    = "archive:/CAB-other/CAB-other";
    private static final byte[] RESOURCE    = "texture bytes that live in the .resS file".getBytes(StandardCharsets.UTF_8);

    @Test
    public void testUnityFS()
    {
        for (boolean infoAtEnd : new boolean[]{false, true})
        {
            byte[] cab = serializedFile(22);
            FS     fs  = new FS();
            fs.infoAtEnd = infoAtEnd;
            // the first block ends inside the serialized file header, so reads cross the block boundary
            fs.blockSizes = new int[]{30, -1};
            fs.blockFlags = new int[]{0, 2};
            fs.nodes = List.of(new Node("CAB-test", 4, cab), new Node("CAB-test.resS", 0, RESOURCE));

            BBQFile   file   = parse(fs.build());
            BBQHeader header = file.getHeader();
            assertEquals("UnityFS", header.getSignature());
            assertEquals(8, header.getFormatVersion());
            assertEquals("2022.3.5f1", header.getGeneratorVersion());
            assertEquals(BBQCompressionType.LZ4, header.getCompressionMode());
            assertEquals(infoAtEnd, header.isMetadataAtEnd());
            assertFalse(header.isEncrypted());

            assertEquals(2, file.getEntries().size());
            BBQAsset asset = file.getEntries().get(0);
            BBQAsset res   = file.getEntries().get(1);
            assertEquals("CAB-test", asset.getName());
            assertEquals("CAB-test.resS", res.getName());
            assertTrue(res.isResource());
            assertArrayEquals(RESOURCE, res.readAllBytes());
            assertArrayEquals(cab, asset.readAllBytes());
            checkSerializedFile(asset, 22);
        }
    }

    @Test
    public void testSharedSubtrees()
    {
        FS fs = new FS();
        fs.nodes = List.of(new Node("CAB-shared", 4, serializedFile(26)));

        BBQAsset asset = parse(fs.build()).getEntries().get(0);
        checkSerializedFile(asset, 26);

        BBQTypeTree list = asset.getTree().getTypeTrees().get(49).getChildren().get(3);
        assertTrue(list.usesSharedSubtree());
        assertEquals("Array", list.getChildren().get(0).getName(), "the children come from the shared sub tree");
        assertEquals(1, asset.getTree().getSharedSubtrees().size());
    }

    @Test
    public void testWebRaw() throws IOException
    {
        byte[] cab = serializedFile(22);
        for (String signature : new String[]{"UnityRaw", "UnityWeb"})
        {
            // directory: count, (name, offset, size) relative to the start of the data
            int    dirSize = 4 + ("CAB-test".length() + 9) + ("CAB-test.resS".length() + 9);
            LE     data    = new LE(ByteOrder.BIG_ENDIAN).i32(2);
            data.cstr("CAB-test").i32(dirSize).i32(cab.length);
            data.cstr("CAB-test.resS").i32(dirSize + cab.length).i32(RESOURCE.length);
            data.bytes(cab).bytes(RESOURCE);
            byte[] raw    = data.toBytes();
            byte[] stored = signature.equals("UnityWeb") ? lzma(raw) : raw;

            // format 3: no hash / crc, but the complete file size and the file info header size
            int headerSize = 64;
            LE  b          = new LE(ByteOrder.BIG_ENDIAN).cstr(signature).i32(3).cstr("3.x.x").cstr("4.7.2f1");
            b.i32(stored.length).i32(headerSize).i32(1).i32(1).i32(stored.length).i32(raw.length).i32(headerSize + stored.length).i32(0);
            b.zeros(headerSize - b.size()).bytes(stored);

            BBQFile file = parse(b.toBytes());
            assertEquals(signature, file.getHeader().getSignature());
            assertEquals(signature.equals("UnityWeb") ? BBQCompressionType.LZMA : BBQCompressionType.NONE, file.getHeader().getCompressionMode());
            assertEquals(2, file.getEntries().size());
            assertArrayEquals(RESOURCE, file.getEntries().get(1).readAllBytes());
            checkSerializedFile(file.getEntries().get(0), 22);
        }
    }

    @Test
    public void testUnityCN()
    {
        byte[] key = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
        byte[] cab = serializedFile(22);

        FS fs = new FS();
        fs.generator = "2021.3.20f1";
        // 0x1000: encrypted (2021.3.2+ flags), 0x100: the block info is encrypted too
        fs.extraFlags = 0x1000 | 0x100;
        fs.cn = new CNWriter(key);
        fs.blockSizes = new int[]{100, -1};
        fs.blockFlags = new int[]{0x102, 0x102};
        fs.nodes = List.of(new Node("CAB-test", 4, cab), new Node("CAB-test.resS", 0, RESOURCE));
        byte[] bundle = fs.build();

        BBQFile file = new BBQParser(key).parse(new ByteArray(bundle));
        assertTrue(file.getHeader().isEncrypted());
        assertEquals(0x12345678L, file.getHeader().getUnityCN().getUnknown());
        assertArrayEquals(cab, file.getEntries().get(0).readAllBytes());
        assertArrayEquals(RESOURCE, file.getEntries().get(1).readAllBytes());
        checkSerializedFile(file.getEntries().get(0), 22);

        assertThrows(ParseException.class, () -> new BBQParser().parse(new ByteArray(bundle)), "no key");
        assertThrows(ParseException.class, () -> new BBQParser("fedcba9876543210".getBytes(StandardCharsets.US_ASCII)).parse(new ByteArray(bundle)), "wrong key");
    }

    @Test
    public void testLzham()
    {
        FS info = new FS();
        info.infoCompression = 4;
        info.nodes = List.of(new Node("CAB-test", 4, serializedFile(22)));
        ParseException e = assertThrows(ParseException.class, () -> parse(info.build()), "LZHAM block info");
        assertTrue(e.getMessage().contains("LZHAM"), e.getMessage());

        FS block = new FS();
        block.blockFlags = new int[]{4};
        block.nodes = info.nodes;
        e = assertThrows(ParseException.class, () -> parse(block.build()), "LZHAM block");
        assertTrue(e.getMessage().contains("LZHAM"), e.getMessage());
    }

    @Test
    public void testInvalid()
    {
        assertThrows(ParseException.class, () -> parse("not a bundle at all".getBytes(StandardCharsets.UTF_8)), "bad signature");
        assertThrows(ParseException.class, () -> parse(new LE(ByteOrder.BIG_ENDIAN).cstr("UnityArchive").i32(6).cstr("5.x.x").cstr("2017.1.0f1").zeros(32).toBytes()), "unsupported signature");
        assertThrows(ParseException.class, () -> parse(new byte[0]), "empty");

        FS fs = new FS();
        fs.blockSizes = new int[]{30, -1};
        fs.blockFlags = new int[]{0, 2};
        fs.nodes = List.of(new Node("CAB-test.resS", 0, RESOURCE), new Node("CAB-test", 4, serializedFile(22)));
        byte[] bundle = fs.build();

        // every cut ends inside data that is read while parsing: the header, the block info, or the blocks holding the serialized file
        List<String> wrong = new ArrayList<>();
        for (int length = 0; length < bundle.length; length++)
        {
            try
            {
                parse(Arrays.copyOf(bundle, length));
                wrong.add(length + ": no exception");
            } catch (ParseException e)
            {
                // expected
            } catch (RuntimeException e)
            {
                wrong.add(length + ": " + e);
            }
        }
        assertEquals(List.of(), wrong, "truncated bundles (of " + bundle.length + " bytes) should throw a ParseException");

        // a corrupt (huge) block count in the block info
        FS huge = new FS();
        huge.infoCompression = 0;
        huge.blockCountOverride = 0x7FFFFFFF;
        huge.nodes = fs.nodes;
        assertThrows(ParseException.class, () -> parse(huge.build()), "huge block count");
    }

    @Test
    public void testMalformedObjects()
    {
        byte[] first  = new LE().i32(42).f32(1.5f).str32("hello").zeros(3).i32(2).i32(10).i32(-20).toBytes();
        byte[] second = new LE().i32(7).f32(2f).str32("ab").zeros(2).i32(0).toBytes();

        // the string node lost its Array / size / data children
        List<TreeNode> noStringChildren = typeNodes(false);
        noStringChildren.subList(4, 7).clear();
        assertMalformed(serializedFile(22, noStringChildren, first, second, second.length), false, "string without children");

        // the vector's Array node has a size child but no data child
        List<TreeNode> noArrayData = typeNodes(false);
        noArrayData.removeLast();
        assertMalformed(serializedFile(22, noArrayData, first, second, second.length), false, "array without a data child");

        byte[] hugeList = new LE().i32(7).f32(2f).str32("ab").zeros(2).i32(0x7FFFFFFF).toBytes();
        assertMalformed(serializedFile(22, typeNodes(false), first, hugeList, hugeList.length), "huge array size");

        byte[] negativeList = new LE().i32(7).f32(2f).str32("ab").zeros(2).i32(-1).toBytes();
        assertMalformed(serializedFile(22, typeNodes(false), first, negativeList, negativeList.length), "negative array size");

        byte[] hugeString = new LE().i32(7).f32(2f).i32(0x7FFFFFF0).ascii("ab").zeros(2).i32(0).toBytes();
        assertMalformed(serializedFile(22, typeNodes(false), first, hugeString, hugeString.length), "huge string length");

        // the data ends inside the float, so the reader runs out of bytes
        byte[] cut = Arrays.copyOf(second, 6);
        assertMalformed(serializedFile(22, typeNodes(false), first, cut, cut.length), "object shorter than its type");

        // the object claims more bytes than the file has
        assertMalformed(serializedFile(22, typeNodes(false), first, second, 1000), "object size past the end of the file");
    }

    /**
     * The bundle parses and the second object throws a ParseException; if the type tree is intact, the first object still reads
     */
    private static void assertMalformed(byte[] cab, String message)
    {
        assertMalformed(cab, true, message);
    }

    private static void assertMalformed(byte[] cab, boolean firstReads, String message)
    {
        FS fs = new FS();
        fs.nodes = List.of(new Node("CAB-test", 4, cab));
        Map<Long, BBQObjectInfo> objects = parse(fs.build()).getEntries().get(0).getObjects();
        BBQObjectInfo            second  = objects.get(-5L);
        assertThrows(ParseException.class, second::read, message);
        if (firstReads)
        {
            assertEquals(42, ((Map<?, ?>) objects.get(1L).read()).get("m_Value"), message);
        }
    }

    private static BBQFile parse(byte[] data)
    {
        return new BBQParser().parse(new ByteArray(data));
    }

    @SuppressWarnings("unchecked")
    private static void checkSerializedFile(BBQAsset asset, int format)
    {
        assertTrue(asset.isLoaded());
        assertFalse(asset.isResource());
        assertEquals(format, asset.getFormat());
        assertEquals(ByteOrder.LITTLE_ENDIAN, asset.getEndian());
        assertEquals("info", asset.getUserInformation());
        assertEquals("2022.3.5f1", asset.getTree().getGeneratorVersion());
        assertEquals(BBQRuntimePlatform.WINDOWS_PLAYER, asset.getTree().getTargetPlatform());
        assertTrue(asset.getTree().hasTypeTree());
        assertEquals(List.of(49), asset.getTree().getClassIds());

        BBQTypeTree root = asset.getTree().getTypeTrees().get(49);
        assertEquals("MyThing", root.getType());
        assertEquals("Base", root.getName());
        assertEquals(List.of("m_Value", "m_Scale", "m_Name", "m_List"), root.getChildren().stream().map(BBQTypeTree::getName).toList());
        assertEquals(List.of("int", "float", "string", "vector"), root.getChildren().stream().map(BBQTypeTree::getType).toList());
        BBQTypeTree array = root.getChildren().get(2).getChildren().get(0);
        assertEquals("Array", array.getType());
        assertEquals(1, array.getTypeFlags() & 1);
        assertEquals(List.of("size", "data"), array.getChildren().stream().map(BBQTypeTree::getName).toList());

        assertEquals(1, asset.getAssetReferences().size());
        assertEquals(EXTERNAL, asset.getAssetReferences().get(0).getFilePath());
        assertEquals(2, asset.getAssetReferences().get(0).getType());
        assertTrue("000102030405060708090a0b0c0d0e0f".equalsIgnoreCase(asset.getAssetReferences().get(0).getGuid()));

        Map<Long, BBQObjectInfo> objects = asset.getObjects();
        assertEquals(Set.of(1L, -5L), objects.keySet());
        assertEquals(32, objects.get(1L).readBytes().length);
        assertEquals(20, objects.get(-5L).readBytes().length);

        Map<String, Object> first = (Map<String, Object>) objects.get(1L).read();
        assertEquals(42, first.get("m_Value"));
        assertEquals(1.5f, first.get("m_Scale"));
        assertEquals("hello", first.get("m_Name"));
        assertEquals(List.of(10, -20), first.get("m_List"));

        Map<String, Object> second = (Map<String, Object>) objects.get(-5L).read();
        assertEquals(7, second.get("m_Value"));
        assertEquals(2f, second.get("m_Scale"));
        assertEquals("ab", second.get("m_Name"));
        assertEquals(List.of(), second.get("m_List"));
    }

    /**
     * A little-endian serialized file with one type (class 49, "MyThing": int, float, string, vector of int) and two objects (path ids 1 and -5).
     * Format 26 stores the type tree in the 23+ wrapper and moves the children of m_List into a shared sub tree.
     */
    private static final List<TreeNode> LIST_CHILDREN = List.of(
            new TreeNode(1, 1, "Array", "Array", -1, 0x4000, 0),
            new TreeNode(2, 0, "int", "size", 4, 0, 0),
            new TreeNode(2, 0, "int", "data", 4, 0, 0));

    private static List<TreeNode> typeNodes(boolean shared)
    {
        List<TreeNode> nodes = new ArrayList<>(List.of(
                new TreeNode(0, 0, "MyThing", "Base", -1, 0, 0),
                new TreeNode(1, 0, "int", "m_Value", 4, 0, 0),
                new TreeNode(1, 0, "float", "m_Scale", 4, 0, 0),
                new TreeNode(1, 0, "string", "m_Name", -1, 0, 0),
                new TreeNode(2, 1, "Array", "Array", -1, 0x4000, 0),
                new TreeNode(3, 0, "int", "size", 4, 0, 0),
                new TreeNode(3, 0, "char", "data", 1, 0, 0),
                new TreeNode(1, shared ? 0x20 : 0, "vector", "m_List", -1, 0, 0)));
        if (!shared)
        {
            for (TreeNode child : LIST_CHILDREN)
            {
                nodes.add(new TreeNode(child.level + 1, child.typeFlags, child.type, child.name, child.size, child.meta, 0));
            }
        }
        return nodes;
    }

    private static byte[] serializedFile(int format)
    {
        byte[] first  = new LE().i32(42).f32(1.5f).str32("hello").zeros(3).i32(2).i32(10).i32(-20).toBytes();
        byte[] second = new LE().i32(7).f32(2f).str32("ab").zeros(2).i32(0).toBytes();
        return serializedFile(format, typeNodes(format >= 26), first, second, second.length);
    }

    /**
     * Same layout with other type tree nodes / object data; the second object claims secondSize bytes
     */
    private static byte[] serializedFile(int format, List<TreeNode> nodes, byte[] first, byte[] second, int secondSize)
    {
        boolean shared = format >= 26;

        LE meta = new LE().cstr("2022.3.5f1").i32(2).u8(1).i32(1);
        // type: class id, stripped, script index, hash, tree, dependencies
        meta.i32(49).u8(0).i16(-1).bytes(TREE_HASH);
        typeTree(meta, format, TREE_HASH, nodes, shared ? List.of(SHARED_HASH) : List.of());
        meta.i32(0);

        // objects: (aligned) path id, offset, size, type index; the metadata starts at 48, so aligning it on its own is the same
        long[] ids     = {1, -5};
        int[]  offsets = {0, first.length};
        int[]  sizes   = {first.length, secondSize};
        meta.i32(2);
        for (int i = 0; i < 2; i++)
        {
            meta.zeros((4 - meta.size() % 4) % 4).i64(ids[i]).i64(offsets[i]).i32(sizes[i]).i32(0);
        }

        // scripts, externals, ref types
        meta.i32(0);
        meta.i32(1).cstr("").bytes(sequence(16)).i32(2).cstr(EXTERNAL);
        meta.i32(0);
        if (shared)
        {
            List<TreeNode> sharedTree = new ArrayList<>();
            sharedTree.add(new TreeNode(0, 0, "vector", "m_List", -1, 0, 0));
            sharedTree.addAll(LIST_CHILDREN);
            meta.i32(1);
            typeTree(meta, format, SHARED_HASH, sharedTree, List.of());
        }
        meta.cstr("info");

        LE data = new LE().bytes(first).bytes(second);

        int    headerSize = 48;
        int    metaSize   = meta.size();
        int    dataOffset = (headerSize + metaSize + 15) & ~15;
        long   fileSize   = dataOffset + data.size();
        LE     header     = new LE(ByteOrder.BIG_ENDIAN).i32(0).i32(0).i32(format).i32(0).u8(0).zeros(3);
        header.i32(metaSize).i64(fileSize).i64(dataOffset).i64(0);

        return header.bytes(meta.toBytes()).zeros(dataOffset - headerSize - metaSize).bytes(data.toBytes()).toBytes();
    }

    private record TreeNode(int level, int typeFlags, String type, String name, int size, int meta, long refTypeHash)
    {
    }

    /**
     * The type tree blob (32 byte nodes), wrapped in the hash / size / "mhtt" header for format 23+
     */
    private static void typeTree(LE out, int format, byte[] hash, List<TreeNode> nodes, List<byte[]> refHashes)
    {
        LE                   strings = new LE();
        Map<String, Integer> offsets = new HashMap<>();
        LE                   blob    = new LE();
        for (TreeNode node : nodes)
        {
            for (String s : new String[]{node.type, node.name})
            {
                if (!offsets.containsKey(s))
                {
                    offsets.put(s, strings.size());
                    strings.cstr(s);
                }
            }
        }
        blob.i32(nodes.size()).i32(strings.size());
        for (int i = 0; i < nodes.size(); i++)
        {
            TreeNode node = nodes.get(i);
            blob.i16(1).u8(node.level).u8(node.typeFlags).i32(offsets.get(node.type)).i32(offsets.get(node.name)).i32(node.size).i32(i).i32(node.meta).i64(node.refTypeHash);
        }
        blob.bytes(strings.toBytes());

        if (format < 23)
        {
            out.bytes(blob.toBytes());
            return;
        }

        LE info = new LE().ascii("mhtt").i32(format).bytes(blob.toBytes()).i32(refHashes.size());
        refHashes.forEach(info::bytes);
        out.bytes(hash).i32(info.size()).bytes(info.toBytes());
    }

    private record Node(String name, int flags, byte[] data)
    {
    }

    /**
     * Builds a format 8 UnityFS bundle; the block info is always combined with the directory (flag 0x40)
     */
    private static class FS
    {
        String     generator          = "2022.3.5f1";
        int        infoCompression    = 2;
        int        extraFlags         = 0;
        boolean    infoAtEnd          = false;
        // block sizes (-1 = the rest of the data) and flags
        int[]      blockSizes         = {-1};
        int[]      blockFlags         = {0};
        int        blockCountOverride = -1;
        CNWriter   cn;
        List<Node> nodes;

        byte[] build()
        {
            ByteArrayOutputStream all = new ByteArrayOutputStream();
            nodes.forEach(n -> all.writeBytes(n.data));
            byte[] data = all.toByteArray();

            LE  info   = new LE(ByteOrder.BIG_ENDIAN).zeros(16).i32(blockCountOverride >= 0 ? blockCountOverride : blockSizes.length);
            LE  blocks = new LE();
            int pos    = 0;
            for (int i = 0; i < blockSizes.length; i++)
            {
                int    size  = blockSizes[i] < 0 ? data.length - pos : blockSizes[i];
                byte[] block = compress(blockFlags[i] & 0x3F, Arrays.copyOfRange(data, pos, pos + size));
                if (cn != null && (blockFlags[i] & 0x100) != 0)
                {
                    cn.encrypt(block, i);
                }
                info.i32(size).i32(block.length).i16(blockFlags[i]);
                blocks.bytes(block);
                pos += size;
            }

            info.i32(nodes.size());
            long offset = 0;
            for (Node node : nodes)
            {
                info.i64(offset).i64(node.data.length).i32(node.flags).cstr(node.name);
                offset += node.data.length;
            }

            byte[] rawInfo    = info.toBytes();
            byte[] storedInfo = compress(infoCompression, rawInfo);
            if (cn != null && (extraFlags & 0x100) != 0)
            {
                cn.encrypt(storedInfo, 0);
            }

            byte[] header = header(0, storedInfo.length, rawInfo.length);
            long   total  = header.length + storedInfo.length + blocks.size();
            LE     out    = new LE().bytes(header(total, storedInfo.length, rawInfo.length));
            if (infoAtEnd)
            {
                out.bytes(blocks.toBytes()).bytes(storedInfo);
            } else
            {
                out.bytes(storedInfo).bytes(blocks.toBytes());
            }
            return out.toBytes();
        }

        private byte[] header(long total, int compressed, int uncompressed)
        {
            int flags = infoCompression | 0x40 | (infoAtEnd ? 0x80 : 0) | extraFlags;
            LE  h     = new LE(ByteOrder.BIG_ENDIAN).cstr("UnityFS").i32(8).cstr("5.x.x").cstr(generator);
            h.i64(total).i32(compressed).i32(uncompressed).i32(flags);
            if (cn != null)
            {
                h.bytes(cn.header());
            }
            // format 7+ aligns the block info to 16 bytes
            return h.zeros((16 - h.size() % 16) % 16).toBytes();
        }
    }

    /**
     * 0 = stored, 2 / 3 = LZ4 / LZ4HC; anything else (LZHAM) is stored as is
     */
    private static byte[] compress(int type, byte[] data)
    {
        return switch (type)
        {
            case 2 -> LZ4Factory.fastestInstance().fastCompressor().compress(data);
            case 3 -> LZ4Factory.fastestInstance().highCompressor().compress(data);
            default -> data.clone();
        };
    }

    /**
     * Classic .lzma stream (properties + 8 byte size header), as stored by UnityWeb
     */
    private static byte[] lzma(byte[] data) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (LZMAOutputStream lzma = new LZMAOutputStream(out, new LZMA2Options(), data.length))
        {
            lzma.write(data);
        }
        return out.toByteArray();
    }

    /**
     * The inverse of BBQUnityCN: writes the encryption header for a key, and encrypts the structure bytes of LZ4 blocks.
     * The index table is a permutation of the 16 nibbles, so each encrypted nibble is index^-1[(plain + count) & 0xF].
     */
    private static class CNWriter
    {
        private final byte[] key;
        private final byte[] table      = new byte[16];
        private final int[]  inverse    = new int[16];
        private final int[]  substitute = new int[16];

        CNWriter(byte[] key)
        {
            this.key = key;

            int[] nibbles = new int[32];
            for (int i = 0; i < 16; i++)
            {
                nibbles[i] = (i * 7 + 3) & 0xF;
                nibbles[16 + i] = (i * 5 + 1) & 0xF;
                inverse[nibbles[i]] = i;
            }
            for (int i = 0; i < 16; i++)
            {
                table[i] = (byte) ((nibbles[i * 2] << 4) | nibbles[i * 2 + 1]);
            }
            for (int j = 0; j < 4; j++)
            {
                for (int i = 0; i < 4; i++)
                {
                    substitute[j * 4 + i] = nibbles[0x10 + i * 4 + j];
                }
            }
        }

        byte[] header()
        {
            byte[] vKey   = filled(16, 0x5A);
            byte[] keySig = sequence(16);
            // AES(x) ^ y is its own inverse in y
            byte[] data    = BBQUnityCN.decryptKey(vKey, table, key);
            byte[] dataSig = BBQUnityCN.decryptKey(keySig, BBQUnityCN.SIGNATURE, key);
            return new LE(ByteOrder.BIG_ENDIAN).i32(0x12345678).bytes(data).bytes(vKey).u8(0).bytes(dataSig).bytes(keySig).u8(0).toBytes();
        }

        void encrypt(byte[] data, int blockIndex)
        {
            long index  = blockIndex;
            int  offset = 0;
            while (offset < data.length)
            {
                offset += encryptSequence(data, offset, index++, data.length - offset);
            }
        }

        private int encryptByte(byte[] data, int offset, long index)
        {
            int count = substitute[(int) (((index >> 2) & 3) + 4)]
                        + substitute[(int) (index & 3)]
                        + substitute[(int) (((index >> 4) & 3) + 8)]
                        + substitute[(int) (((index & 0xFF) >> 6) + 12)];
            int plain = data[offset] & 0xFF;
            data[offset] = (byte) ((inverse[((plain >> 4) + count) & 0xF] << 4) | inverse[((plain & 0xF) + count) & 0xF]);
            return plain;
        }

        private int encryptSequence(byte[] data, int start, long index, int remaining)
        {
            int  offset = start;
            int  token  = encryptByte(data, offset++, index++);
            long high   = token >> 4;
            if (high == 0xF)
            {
                int b;
                do
                {
                    b = encryptByte(data, offset++, index++);
                    high += b;
                } while (b == 0xFF);
            }

            long skipped = (offset - start) + high;
            if (skipped < remaining)
            {
                offset = (int) (start + skipped);
                encryptByte(data, offset++, index++);
                encryptByte(data, offset++, index++);
                if ((token & 0xF) == 0xF)
                {
                    int b;
                    do
                    {
                        b = encryptByte(data, offset++, index++);
                    } while (b == 0xFF);
                }
                return offset - start;
            }
            return (int) skipped;
        }
    }

    private static byte[] filled(int length, int value)
    {
        byte[] b = new byte[length];
        Arrays.fill(b, (byte) value);
        return b;
    }

    private static byte[] sequence(int length)
    {
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++)
        {
            b[i] = (byte) i;
        }
        return b;
    }
}

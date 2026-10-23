package offline;

import com.github.luben.zstd.Zstd;
import no.stelar7.cdragon.types.atlas.AtlasParser;
import no.stelar7.cdragon.types.atlas.data.AtlasFile;
import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.ktx.ktx.KTX11Parser;
import no.stelar7.cdragon.types.ktx.ktx.data.KTX11File;
import no.stelar7.cdragon.types.ktx.ktx2.KTX2Parser;
import no.stelar7.cdragon.types.ktx.ktx2.data.KTX2File;
import no.stelar7.cdragon.types.lockfile.LockfileParser;
import no.stelar7.cdragon.types.lockfile.data.Lockfile;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DDS (DXT1/3/5, BGRA), KTX 1.1 (ETC1, RGB/RGBA, BC1, cubemaps, both byte orders), KTX2 (zstd, kvd, dfd), lockfile and atlas
 * parsing, on textures built in code. The newer block formats (BC6H/BC7/ETC2/ASTC/UASTC) are in TestTextureDecoders.
 */
public class TextureFormatsTest
{
    // color0 = pure red (0xF800), color1 = pure blue (0x001F), rows use index 0, 1, 2, 3
    private static final byte[] DXT1_BLOCK = {0x00, (byte) 0xF8, 0x1F, 0x00, 0x00, 0x55, (byte) 0xAA, (byte) 0xFF};

    private static void assertPixel(BufferedImage img, int x, int y, int expected)
    {
        int got = img.getRGB(x, y);
        assertEquals(expected, got, String.format("pixel (%d,%d) expected %08X got %08X", x, y, expected, got));
    }

    private static byte[] dds(String fourCC, int width, int height, byte[] data)
    {
        LE x = new LE().ascii("DDS ").i32(124).i32(0x1007).i32(height).i32(width).i32(0).i32(0).i32(1);
        x.zeros(11 * 4);
        x.i32(32).i32(4).ascii(fourCC).i32(0).i32(0).i32(0).i32(0).i32(0);
        x.i32(0x1000).i32(0).i32(0).i32(0).i32(0);
        assertEquals(128, x.size(), "dds header size");
        return x.bytes(data).toBytes();
    }

    @Test
    public void testDXT1()
    {
        DDSParser     dp  = new DDSParser();
        BufferedImage img = dp.parse(new ByteArray(dds("DXT1", 4, 4, DXT1_BLOCK)));
        assertPixel(img, 0, 0, 0xFFFF0000);
        assertPixel(img, 3, 1, 0xFF0000FF);
        assertPixel(img, 1, 2, 0xFFAA0055);
        assertPixel(img, 2, 3, 0xFF5500AA);

        // color0 <= color1: 3 color mode, index 3 is transparent black
        byte[] threeColor = {0x1F, 0x00, 0x00, (byte) 0xF8, (byte) 0xFF, 0, 0, 0};
        img = dp.parse(new ByteArray(dds("DXT1", 4, 4, threeColor)));
        assertPixel(img, 0, 0, 0x00000000);
        assertPixel(img, 0, 1, 0xFF0000FF);

        // a size that is not a multiple of 4 still uses one block
        img = dp.parse(new ByteArray(dds("DXT1", 2, 2, DXT1_BLOCK)));
        assertEquals(2, img.getWidth());
        assertEquals(2, img.getHeight());
    }

    @Test
    public void testDXT3AndDXT5()
    {
        DDSParser dp = new DDSParser();

        // interpolated alpha a0=255 a1=0; indices p0=0, p1=1, p2=2 -> 255, 0, 218
        byte[] dxt5 = new byte[16];
        dxt5[0] = (byte) 255;
        dxt5[2] = (byte) 0x88;
        System.arraycopy(DXT1_BLOCK, 0, dxt5, 8, 8);
        BufferedImage img = dp.parse(new ByteArray(dds("DXT5", 4, 4, dxt5)));
        assertPixel(img, 0, 0, 0xFFFF0000);
        assertPixel(img, 1, 0, 0x00FF0000);
        assertPixel(img, 2, 0, 0xDAFF0000);

        // explicit 4 bit alpha: p0 = 0xF, p1 = 0x5
        byte[] dxt3 = new byte[16];
        dxt3[0] = (byte) 0x5F;
        System.arraycopy(DXT1_BLOCK, 0, dxt3, 8, 8);
        img = dp.parse(new ByteArray(dds("DXT3", 4, 4, dxt3)));
        assertPixel(img, 0, 0, 0xFFFF0000);
        assertPixel(img, 1, 0, 0x55FF0000);
    }

    @Test
    public void testUncompressedBGRA()
    {
        LE x = new LE().ascii("DDS ").i32(124).i32(0x100F).i32(1).i32(2).i32(8).i32(0).i32(1);
        x.zeros(11 * 4);
        x.i32(32).i32(0x41).i32(0).i32(32).i32(0x00FF0000).i32(0x0000FF00).i32(0x000000FF).i32(0xFF000000);
        x.i32(0x1000).i32(0).i32(0).i32(0).i32(0);
        x.bytes(new byte[]{0x10, 0x20, 0x30, 0x40, (byte) 0xFF, 0x00, 0x00, (byte) 0x80});

        BufferedImage img = new DDSParser().parse(new ByteArray(x.toBytes()));
        assertPixel(img, 0, 0, 0x40302010);
        assertPixel(img, 1, 0, 0x800000FF);
    }

    @Test
    public void testDDSGarbageThrows()
    {
        assertThrows(ParseException.class, () -> new DDSParser().parse(new ByteArray(new byte[]{1, 2, 3, 4, 5})));
    }

    private static LE ktx1Header(ByteOrder order, int glType, int typeSize, int glFormat, int glInternal, int glBase, int w, int h, int levels, int arrays, int faces, int kvBytes)
    {
        return new LE(order).bytes(KTX11Parser.MAGIC).i32(0x04030201).i32(glType).i32(typeSize).i32(glFormat).i32(glInternal).i32(glBase)
                            .i32(w).i32(h).i32(0).i32(arrays).i32(faces).i32(levels).i32(kvBytes);
    }

    private static void pad4(LE x)
    {
        while (x.size() % 4 != 0)
        {
            x.u8(0);
        }
    }

    private static byte[] ktx1(ByteOrder order, int glType, int typeSize, int glFormat, int glInternal, int glBase, int w, int h, int levels, byte[][] data, boolean kv)
    {
        byte[] kvPair = "KTXorientation\0S=r,T=d\0".getBytes(StandardCharsets.US_ASCII);
        int    kvLen  = kv ? 4 + kvPair.length + (3 - ((kvPair.length + 3) % 4)) : 0;
        LE     x      = ktx1Header(order, glType, typeSize, glFormat, glInternal, glBase, w, h, levels, 0, 1, kvLen);
        if (kv)
        {
            x.i32(kvPair.length).bytes(kvPair);
            pad4(x);
        }
        for (byte[] d : data)
        {
            x.i32(d.length).bytes(d);
            pad4(x);
        }
        return x.toBytes();
    }

    @Test
    public void testKTX1ETC1()
    {
        // individual mode: R1=15,R2=0 G1=0,G2=15 B=0, tables 0, no flip, all indices 0 -> +2
        byte[] individual   = {(byte) 0xF0, 0x0F, 0x00, 0x00, 0, 0, 0, 0};
        // differential: R 16 (+1), G 0, B 31 (-1), table1 = 1, table2 = 0, flip, pixel (0,0) index msb=1 lsb=1 -> -b
        byte[] differential = {(byte) ((16 << 3) | 1), 0, (byte) ((31 << 3) | 7), (byte) ((1 << 5) | 2 | 1), 0x00, 0x01, 0x00, 0x01};
        byte[] etc          = new byte[16];
        System.arraycopy(individual, 0, etc, 0, 8);
        System.arraycopy(differential, 0, etc, 8, 8);

        byte[]        ktx = ktx1(ByteOrder.LITTLE_ENDIAN, 0, 1, 0, 0x8D64, 0x1907, 8, 4, 1, new byte[][]{etc}, false);
        BufferedImage img = new DDSParser().parse(new ByteArray(ktx));
        assertPixel(img, 0, 0, 0xFFFF0202);
        assertPixel(img, 3, 3, 0xFF02FF02);
        // r1 = expand5(16) = 132, table1 = {5,17}, -17 -> 115; b = 255 - 17 = 238
        assertPixel(img, 4, 0, 0xFF7300EE);
        // flipped second sub block: r2 = expand5(17) = 140, b2 = expand5(30) = 247, +2
        assertPixel(img, 4, 3, 0xFF8E02F9);

        KTX11File file = new KTX11Parser().parse(new ByteArray(ktx));
        assertEquals(8 * 4 * 3, file.decodeETC1(0).remaining(), "decodeETC1 gives RGB bytes");
    }

    @Test
    public void testKTX1RGBBothByteOrders()
    {
        for (ByteOrder order : new ByteOrder[]{ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN})
        {
            // 3x2 RGB8 with rows padded to 12 bytes, 2 mip levels, one key/value pair
            byte[] level0 = new byte[12 * 2];
            for (int y = 0; y < 2; y++)
            {
                for (int x = 0; x < 3; x++)
                {
                    level0[y * 12 + x * 3] = (byte) (x * 10);
                    level0[y * 12 + x * 3 + 1] = (byte) (y * 10);
                    level0[y * 12 + x * 3 + 2] = 7;
                }
            }
            byte[]    level1 = {1, 2, 3, 0};
            byte[]    bytes  = ktx1(order, 0x1401, 1, 0x1907, 0x1907, 0x1907, 3, 2, 2, new byte[][]{level0, level1}, true);
            KTX11File f      = new KTX11Parser().parse(new ByteArray(bytes));

            assertEquals(1, f.getKeyValueData().size(), order + " kv count");
            assertTrue(new String(f.getKeyValueData().get(0).getKeyValue(), StandardCharsets.UTF_8).startsWith("KTXorientation"), order + " kv key");
            assertEquals(1, f.getMipMaps().getTextureData().get(1).getWidth(), order + " mip 1 width");
            assertPixel(f.toBufferedImage(0), 2, 1, 0xFF140A07);
            assertPixel(f.toBufferedImage(1), 0, 0, 0xFF010203);
        }
    }

    @Test
    public void testKTX1MipChainsAndCubemap()
    {
        // non square full chain 4x1 -> 2x1 -> 1x1
        byte[] chain = ktx1(ByteOrder.LITTLE_ENDIAN, 0x1401, 1, 0x1908, 0x1908, 0x1908, 4, 1, 3, new byte[][]{new byte[16], new byte[8], new byte[4]}, false);
        assertEquals(3, new KTX11Parser().parse(new ByteArray(chain)).getMipMaps().getTextureData().size());

        // BC1 6x6 (2x2 blocks) -> 3x3 -> 1x1
        byte[] level0 = new byte[4 * 8];
        for (int k = 0; k < 4; k++)
        {
            System.arraycopy(DXT1_BLOCK, 0, level0, k * 8, 8);
        }
        byte[]        bc1 = ktx1(ByteOrder.LITTLE_ENDIAN, 0, 1, 0, 0x83F0, 0x1907, 6, 6, 3, new byte[][]{level0, DXT1_BLOCK, DXT1_BLOCK}, false);
        BufferedImage img = new DDSParser().parse(new ByteArray(bc1));
        assertEquals(6, img.getWidth());
        assertEquals(6, img.getHeight());
        assertPixel(img, 4, 5, 0xFF0000FF);

        // cubemap (not an array): imageSize is per face
        LE cube = ktx1Header(ByteOrder.LITTLE_ENDIAN, 0x1401, 1, 0x1908, 0x1908, 0x1908, 1, 1, 1, 0, 6, 0).i32(4);
        for (int face = 0; face < 6; face++)
        {
            cube.bytes(new byte[]{(byte) face, 0, 0, (byte) 255});
        }
        KTX11File f = new KTX11Parser().parse(new ByteArray(cube.toBytes()));
        assertEquals(5, f.getMipMaps().getTextureData().get(0).getImage(0, 5, 0)[0], "cubemap face 5");
    }

    @Test
    public void testKTX1UnknownFormatThrows()
    {
        byte[] bytes = ktx1(ByteOrder.LITTLE_ENDIAN, 0, 1, 0, 0x1234, 0x1907, 4, 4, 1, new byte[][]{new byte[8]}, false);
        assertThrows(ParseException.class, () -> new KTX11Parser().parse(new ByteArray(bytes)));
    }

    private static byte[] ktx2(int vkFormat, int w, int h, int levels, byte[][] stored, byte[][] uncompressed)
    {
        boolean zstd = uncompressed != null;

        // DFD: 4 + 24 + 4 * 16 = 92 bytes, RGBA8 (color model 1)
        LE dfd = new LE().i32(92).i32(0).i16(2).i16(88).bytes(new byte[]{1, 1, 2, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0});
        for (int k = 0; k < 4; k++)
        {
            dfd.i16(k * 8).u8(7).u8(k == 3 ? 15 : k).zeros(4).i32(0).i32(255);
        }
        byte[] dfdBytes = dfd.toBytes();

        LE kvd = new LE();
        for (String entry : new String[]{"KTXorientation\0rd\0", "KTXwriter\0test\0"})
        {
            byte[] e = entry.getBytes(StandardCharsets.US_ASCII);
            kvd.i32(e.length).bytes(e);
            pad4(kvd);
        }
        byte[] kvdBytes = kvd.toBytes();

        int dfdOffset  = 80 + 24 * levels;
        int kvdOffset  = dfdOffset + dfdBytes.length;
        int dataOffset = kvdOffset + kvdBytes.length;
        while (dataOffset % 8 != 0)
        {
            dataOffset++;
        }

        LE x = new LE().bytes(KTX2Parser.MAGIC).i32(vkFormat).i32(1).i32(w).i32(h).i32(0).i32(0).i32(1).i32(levels).i32(zstd ? 2 : 0);
        x.i32(dfdOffset).i32(dfdBytes.length).i32(kvdOffset).i32(kvdBytes.length).i64(0).i64(0);

        // levels are stored smallest first
        long[] offsets = new long[levels];
        long   current = dataOffset;
        for (int k = levels - 1; k >= 0; k--)
        {
            offsets[k] = current;
            current += stored[k].length;
            while (current % 8 != 0)
            {
                current++;
            }
        }
        for (int k = 0; k < levels; k++)
        {
            x.i64(offsets[k]).i64(stored[k].length).i64(zstd ? uncompressed[k].length : stored[k].length);
        }
        x.bytes(dfdBytes).bytes(kvdBytes);
        for (int k = levels - 1; k >= 0; k--)
        {
            x.zeros((int) (offsets[k] - x.size()));
            x.bytes(stored[k]);
        }
        return x.toBytes();
    }

    @Test
    public void testKTX2Zstd()
    {
        byte[] level0 = new byte[16];
        for (int k = 0; k < 4; k++)
        {
            level0[k * 4] = (byte) (k * 50);
            level0[k * 4 + 1] = 1;
            level0[k * 4 + 2] = 2;
            level0[k * 4 + 3] = (byte) 255;
        }
        byte[] level1 = {9, 8, 7, (byte) 255};
        byte[] bytes  = ktx2(37, 2, 2, 2, new byte[][]{Zstd.compress(level0), Zstd.compress(level1)}, new byte[][]{level0, level1});

        KTX2File f = new KTX2Parser().parse(new ByteArray(bytes));
        assertEquals(0, f.getHeader().getPixelDepth());
        assertEquals(1, f.getHeader().getFaceCount());
        assertEquals(2, f.getHeader().getLevelCount());
        assertEquals(KTX2File.SUPERCOMPRESSION_ZSTD, f.getHeader().getSupercompressionScheme());
        assertEquals("rd", f.getKeyValueString("KTXorientation"));
        assertEquals("test", f.getKeyValueString("KTXwriter"));
        assertEquals(1, f.getDataFormatBlocks().size());
        assertEquals(4, f.getDataFormatBlocks().get(0).getSamples().size());
        assertEquals(1, f.getDataFormatBlocks().get(0).getColorModel());
        assertEquals(92, f.getDfdTotalSize());
        assertPixel(f.toImage(0), 1, 1, 0xFF960102);
        assertPixel(f.toImage(1), 0, 0, 0xFF090807);

        assertEquals(2, new DDSParser().parse(new ByteArray(bytes)).getWidth(), "DDSParser also reads KTX2");
    }

    @Test
    public void testKTX2BC1AndUndefinedFormat()
    {
        KTX2File f = new KTX2Parser().parse(new ByteArray(ktx2(131, 4, 4, 1, new byte[][]{DXT1_BLOCK}, null)));
        assertPixel(f.toImage(0), 0, 0, 0xFFFF0000);

        byte[] undefined = ktx2(0, 4, 4, 1, new byte[][]{DXT1_BLOCK}, null);
        assertThrows(ParseException.class, () -> new KTX2Parser().parse(new ByteArray(undefined)).toImage(0));
    }

    @Test
    public void testLockfile()
    {
        Lockfile lf = new LockfileParser().parse(new ByteArray("LeagueClient:1234:5678:pa:ss:word:https".getBytes(StandardCharsets.UTF_8)));
        assertEquals("LeagueClient", lf.getProcess());
        assertEquals(1234, lf.getPID());
        assertEquals(5678, lf.getPort());
        assertEquals("pa:ss:word", lf.getPassword(), "password may contain colons");
        assertEquals("https", lf.getProtocol());

        assertThrows(ParseException.class, () -> new LockfileParser().parse(new ByteArray("a:b".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void testAtlas()
    {
        AtlasParser ap = new AtlasParser();
        byte[] atlas = new LE().i32(2).str32("a.t").str32("b.t")
                               .i32(1).str32("icon").f32(0, 0, .5f, .5f).i32(1)
                               .toBytes();
        assertTrue(ap.canParse(atlas));
        AtlasFile af = ap.parse(new ByteArray(atlas));
        assertEquals("icon", af.getData().get("b.t").get(0).getName());
        assertFalse(af.getData().containsKey("a.t"));

        byte[] prop = "PROP....garbage".getBytes(StandardCharsets.US_ASCII);
        assertFalse(ap.canParse(prop));
        assertFalse(ap.canParse(new byte[3]));
        assertThrows(ParseException.class, () -> ap.parse(new ByteArray(prop)));
        assertThrows(ParseException.class, () -> ap.parse(new ByteArray(new byte[3])));

        byte[] badBundle = new LE().i32(1).str32("a.t").i32(1).str32("icon").f32(0, 0, 0, 0).i32(5).toBytes();
        assertThrows(ParseException.class, () -> ap.parse(new ByteArray(badBundle)), "bundle index out of range");
    }
}

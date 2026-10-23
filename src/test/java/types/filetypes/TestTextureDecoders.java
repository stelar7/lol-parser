package types.filetypes;

import no.stelar7.cdragon.types.dds.*;
import no.stelar7.cdragon.types.tex.TEXParser;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import org.junit.jupiter.api.*;

import java.awt.image.BufferedImage;
import java.nio.*;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Self contained decoder vectors. The expected pixels come from the format specs (UASTC example blocks), from independent
 * decoders (texture2ddecoder for ETC2/BC7, ARM astcenc with DECODE_UNORM8 for ASTC), or are hand-computed from the spec
 * formulas for hand-encoded blocks.
 */
public class TestTextureDecoders
{
    private static byte[] hex(String s)
    {
        return HexFormat.of().parseHex(s);
    }

    private static int[] pixels(BufferedImage img)
    {
        return img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
    }

    /**
     * 0xRRGGBBAA to ARGB
     */
    private static int[] fromRGBA(int... rgba)
    {
        int[] out = new int[rgba.length];
        for (int i = 0; i < rgba.length; i++)
        {
            out[i] = (rgba[i] >>> 8) | (rgba[i] << 24);
        }
        return out;
    }

    /**
     * UASTC's "R at bit 0, A at bit 24" to ARGB
     */
    private static int[] fromABGR(long... abgr)
    {
        int[] out = new int[abgr.length];
        for (int i = 0; i < abgr.length; i++)
        {
            int v = (int) abgr[i];
            out[i] = (v & 0xFF00FF00) | ((v & 0xFF) << 16) | ((v >> 16) & 0xFF);
        }
        return out;
    }

    /**
     * LSB first bit writer for hand-encoded 128 bit blocks
     */
    private static final class Bits
    {
        final byte[] data = new byte[16];
        int pos;

        Bits put(int value, int count)
        {
            for (int i = 0; i < count; i++, pos++)
            {
                if (((value >> i) & 1) != 0)
                {
                    data[pos >> 3] |= (byte) (1 << (pos & 7));
                }
            }
            return this;
        }
    }

    @Test
    public void testUASTCSpecExamples()
    {
        // examples 0, 1, 2 and 18 (invalid -> magenta) of the "64 UASTC Block Examples" in the UASTC LDR 4x4 spec
        byte[][] blocks = {
                {0x8, 0x18, 0x43, 0x67, 0x57, 0x4F, (byte) 0xB0, (byte) 0x8A, 0x5E, (byte) 0xB4, 0x62, 0x35, 0x42, (byte) 0xE2, 0xE, 0x22},
                {(byte) 0xF1, (byte) 0xF0, 0x18, (byte) 0x8F, (byte) 0xE4, 0x6B, 0x3C, 0x3A, (byte) 0x90, (byte) 0xFB, (byte) 0x89, 0x5D, 0x56, (byte) 0x82, (byte) 0x9B, 0x6C},
                {(byte) 0x84, (byte) 0x9F, (byte) 0x81, (byte) 0x8D, (byte) 0xF5, (byte) 0xD3, 0x64, 0x4, 0x5B, (byte) 0xBB, 0x43, (byte) 0x87, 0x31, (byte) 0xAF, (byte) 0xC1, 0x3D},
                {(byte) 0xBD, (byte) 0xF8, (byte) 0xF6, 0x25, 0x70, (byte) 0xD0, (byte) 0xF5, 0x71, 0x63, 0x2, 0x3E, 0x63, (byte) 0xD1, 0x10, 0x46, 0x35},
                };
        long[][] expected = {
                {0x255CF1C4L, 0x255CF1C4L, 0x1061EFC4L, 0x5151F4AEL, 0x3B56F3D9L, 0x3B56F3C4L, 0x255CF1C4L, 0x5151F4D9L, 0x3B56F3D9L, 0x1061EFC4L, 0x3B56F3D9L, 0x3B56F398L, 0x3B56F398L, 0x1061EFD9L, 0x3B56F3D9L, 0x3B56F3D9L},
                {0xFFDD964AL, 0xFFEA8066L, 0xFFED7C6CL, 0xFFF27278L, 0xFFEA8066L, 0xFFE98263L, 0xFFF07672L, 0xFFE48A59L, 0xFFE6885CL, 0xFFE48A59L, 0xFFE09250L, 0xFFE98263L, 0xFFED7C6CL, 0xFFEA8066L, 0xFFEE796FL, 0xFFE6885CL},
                {0xFAB92099L, 0xCAD06299L, 0x67FFEA93L, 0x67FFEA93L, 0x67FFEA9EL, 0xFAB92099L, 0x67FFEA99L, 0xFAB92093L, 0xCAD0629EL, 0x67FFEA9EL, 0x67FFEA8EL, 0x97E8A893L, 0xCAD0629EL, 0xFAB9208EL, 0xCAD0628EL, 0x67FFEA9EL},
                {0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL, 0xFFFF00FFL},
                };
        for (int i = 0; i < blocks.length; i++)
        {
            assertArrayEquals(fromABGR(expected[i]), pixels(TextureDecoder.decode(TextureDecoder.Format.UASTC_4x4, blocks[i], 0, 4, 4)), "UASTC example " + i);
        }
    }

    @Test
    public void testETC2Modes()
    {
        // T, H and planar mode blocks, expected values from texture2ddecoder
        assertArrayEquals(fromRGBA(0xFF6677FF, 0xFF6677FF, 0xF96071FF, 0xFF6C7DFF, 0xFF6C7DFF, 0xF96071FF, 0xFF6677FF, 0xFF6677FF, 0xF96071FF, 0xFF6655FF, 0xF96071FF, 0xFF6655FF, 0xFF6677FF, 0xF96071FF, 0xFF6C7DFF, 0xF96071FF),
                          pixels(TextureDecoder.decode(TextureDecoder.Format.ETC2_RGB, hex("fb65f673a7bd9da6"), 0, 4, 4)));
        assertArrayEquals(fromRGBA(0xAE6A00FF, 0xFB5195FF, 0xAE6A00FF, 0xAE6A00FF, 0xFB5195FF, 0x7B0015FF, 0xFFEA51FF, 0xAE6A00FF, 0x7B0015FF, 0xAE6A00FF, 0xAE6A00FF, 0xFFEA51FF, 0xAE6A00FF, 0xAE6A00FF, 0xFFEA51FF, 0xFFEA51FF),
                          pixels(TextureDecoder.decode(TextureDecoder.Format.ETC2_RGB, hex("f504d8af003635ed"), 0, 4, 4)));
        assertArrayEquals(fromRGBA(0x6D105DFF, 0x8F1E60FF, 0xB02B63FF, 0xD23966FF, 0x632262FF, 0x853065FF, 0xA63D68FF, 0xC84B6BFF, 0x593467FF, 0x7B426AFF, 0x9C4F6DFF, 0xBE5D70FF, 0x4F466CFF, 0x71546FFF, 0x926172FF, 0xB46F75FF),
                          pixels(TextureDecoder.decode(TextureDecoder.Format.ETC2_RGB, hex("b610f3fa46d22b1c"), 0, 4, 4)));
    }

    @Test
    public void testBC7()
    {
        // random mode 1 and mode 5 blocks, expected values from texture2ddecoder (ImageMagick and Pillow agree)
        assertArrayEquals(fromRGBA(0xBA8038FF, 0xB79336FF, 0xB320B6FF, 0xAB1C9FFF, 0xBD6E39FF, 0xB320B6FF, 0xAB1C9FFF, 0xC82140FF, 0xC5333FFF, 0xB320B6FF, 0xC027DFFF, 0xBD6E39FF, 0xAB1C9FFF, 0xC72AF3FF, 0xB79336FF, 0xBD6E39FF),
                          pixels(TextureDecoder.decode(TextureDecoder.Format.BC7, hex("6ead1c97e4a0100dc46387a9d15d4440"), 0, 4, 4)));
        assertArrayEquals(fromRGBA(0xDC1C724C, 0xEBD23676, 0xE4585362, 0xD51C8F38, 0xD51C8F38, 0xD5588F38, 0xDCD2724C, 0xDC1C724C, 0xD5968F38, 0xD5D28F38, 0xDC1C724C, 0xDC1C724C, 0xD5D28F38, 0xE41C5362, 0xE4585362, 0xD5968F38),
                          pixels(TextureDecoder.decode(TextureDecoder.Format.BC7, hex("a0ea3a6777dc70485fa0a0501c340e93"), 0, 4, 4)));

        // hand-encoded mode 6 block
        Bits b = new Bits().put(1 << 6, 7);
        b.put(10, 7).put(100, 7).put(127, 7).put(0, 7).put(50, 7).put(50, 7).put(127, 7).put(64, 7).put(1, 1).put(0, 1);
        b.put(0, 3).put(15, 4).put(7, 4);
        int[] px = pixels(TextureDecoder.decode(TextureDecoder.Format.BC7, b.data, 0, 4, 4));
        assertEquals(0xFF15FF65, px[0]);
        assertEquals(0x80C80064, px[1]);
        assertEquals(0xC3698765, px[2]);
        assertEquals(px[0], px[3]);

        // reserved mode: transparent black
        assertEquals(0, pixels(TextureDecoder.decode(TextureDecoder.Format.BC7, new byte[16], 0, 4, 4))[0]);
    }

    @Test
    public void testBC6H()
    {
        // hand-encoded mode 11 (10 bit direct endpoints): w = 0, x = 462 -> unquantized 29600 -> half 0x3801 (0.50049) -> 128
        Bits b = new Bits().put(0b00011, 5);
        b.put(0, 10).put(0, 10).put(0, 10).put(462, 10).put(462, 10).put(462, 10);
        b.put(0, 3).put(15, 4);
        int[] px = pixels(TextureDecoder.decode(TextureDecoder.Format.BC6H_UF16, b.data, 0, 4, 4));
        assertEquals(0xFF000000, px[0]);
        assertEquals(0xFF808080, px[1]);

        // signed: 462 unquantizes to ((462 << 15) + 0x4000) >> 9 = 29600, (29600 * 31) >> 5 = half 0x7003 (> 1) -> 255
        px = pixels(TextureDecoder.decode(TextureDecoder.Format.BC6H_SF16, b.data, 0, 4, 4));
        assertEquals(0xFF000000, px[0]);
        assertEquals(0xFFFFFFFF, px[1]);

        // signed with a negative endpoint: x = -50 (10 bit two's complement) -> negative half -> clamped to 0
        b = new Bits().put(0b00011, 5);
        b.put(0, 10).put(0, 10).put(0, 10).put(1024 - 50, 10).put(1024 - 50, 10).put(1024 - 50, 10);
        b.put(0, 3).put(15, 4);
        assertEquals(0xFF000000, pixels(TextureDecoder.decode(TextureDecoder.Format.BC6H_SF16, b.data, 0, 4, 4))[1]);
    }

    @Test
    public void testASTC()
    {
        // 2 partition block encoded by astcenc, expected values from astcenc (DECODE_UNORM8)
        int[] px = pixels(TextureDecoder.decode(TextureDecoder.Format.ASTC_4x4, hex("420001fe0100fe0100000000fcfcfcfc"), 0, 4, 4));
        for (int i = 0; i < 16; i++)
        {
            assertEquals((i & 3) == 3 ? 0xFF0000FF : 0xFFFF0000, px[i]);
        }

        // hand-encoded LDR void extent: R 0x1234, G 0xABCD, B 0x00FF, A 0xFFFF -> top 8 bits
        ByteBuffer v = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).putLong(0xFFFFFFFFFFFFFDFCL).putShort((short) 0x1234).putShort((short) 0xABCD).putShort((short) 0x00FF).putShort((short) 0xFFFF);
        px = pixels(TextureDecoder.decode(TextureDecoder.Format.ASTC_8x8, v.array(), 0, 8, 8));
        for (int p : px)
        {
            assertEquals(0xFF12AB00, p);
        }

        // reserved block mode -> error color
        assertEquals(0xFFFF00FF, pixels(TextureDecoder.decode(TextureDecoder.Format.ASTC_6x6, new byte[16], 0, 6, 6))[0]);
    }

    @Test
    public void testRGBA16()
    {
        ByteBuffer snorm = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putShort((short) 32767).putShort((short) -32768).putShort((short) 0).putShort((short) 16384);
        // (r, g, b, a) = (255, 0, 128, 191)
        assertEquals(0xBFFF0080, pixels(TextureDecoder.decode(TextureDecoder.Format.RGBA16_SNORM, snorm.array(), 0, 1, 1))[0]);

        ByteBuffer unorm = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putShort((short) 65535).putShort((short) 0).putShort((short) 32768).putShort((short) 257);
        assertEquals(0x01FF0080, pixels(TextureDecoder.decode(TextureDecoder.Format.RGBA16_UNORM, unorm.array(), 0, 1, 1))[0]);

        ByteBuffer half = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putShort((short) 0x3C00).putShort((short) 0xBC00).putShort((short) 0x3800).putShort((short) 0x7C00);
        assertEquals(0xFFFF0080, pixels(TextureDecoder.decode(TextureDecoder.Format.RGBA16_FLOAT, half.array(), 0, 1, 1))[0]);
    }

    @Test
    public void testTEX()
    {
        // BC7 4x4 with mipmaps (stored smallest first): 1x1, 2x2, then the full image
        byte[]     full = hex("6ead1c97e4a0100dc46387a9d15d4440");
        ByteBuffer tex  = ByteBuffer.allocate(12 + 16 * 3).order(ByteOrder.LITTLE_ENDIAN);
        tex.put("TEX\0".getBytes()).putShort((short) 4).putShort((short) 4).put((byte) 1).put((byte) TEXParser.FORMAT_BC7).put((byte) 0).put((byte) 1);
        tex.put(new byte[32]).put(full);

        BufferedImage expected = TextureDecoder.decode(TextureDecoder.Format.BC7, full, 0, 4, 4);
        BufferedImage parsed   = new TEXParser().parse(new RandomAccessReader(tex.array(), ByteOrder.LITTLE_ENDIAN));
        assertArrayEquals(pixels(expected), pixels(parsed));
        assertArrayEquals(pixels(expected), pixels(new DDSParser().parse(TEXParser.toDDS(tex.array()))));

        // RGBA16, no mipmaps
        ByteBuffer rgba16 = ByteBuffer.allocate(12 + 8).order(ByteOrder.LITTLE_ENDIAN);
        rgba16.put("TEX\0".getBytes()).putShort((short) 1).putShort((short) 1).put((byte) 1).put((byte) TEXParser.FORMAT_RGBA16).put((byte) 0).put((byte) 0);
        rgba16.putShort((short) 32767).putShort((short) -32768).putShort((short) 0).putShort((short) 16384);
        BufferedImage img = new TEXParser().parse(new RandomAccessReader(rgba16.array(), ByteOrder.LITTLE_ENDIAN));
        assertEquals(pixels(TextureDecoder.decode(TEXParser.RGBA16_FORMAT, rgba16.array(), 12, 1, 1))[0], img.getRGB(0, 0));
        assertEquals(img.getRGB(0, 0), new DDSParser().parse(TEXParser.toDDS(rgba16.array())).getRGB(0, 0));

        // DXT1 in the 3 color mode (color0 <= color1) with every index 3: transparent black, both directly and through the dds
        byte[] dxt1 = tex4x4(TEXParser.FORMAT_DXT1, new byte[]{0, 0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        assertEquals(0, new TEXParser().parse(new RandomAccessReader(dxt1, ByteOrder.LITTLE_ENDIAN)).getRGB(0, 0));
        assertEquals(0, new DDSParser().parse(TEXParser.toDDS(dxt1)).getRGB(0, 0));

        // BC5 is written as BC5_UNORM (83), which DDSParser reads back
        byte[] bc5 = tex4x4(TEXParser.FORMAT_BC5, hex("ff000000000000004000000000000000"));
        assertEquals(83, ByteBuffer.wrap(TEXParser.toDDS(bc5), 128, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
        assertArrayEquals(pixels(new TEXParser().parse(new RandomAccessReader(bc5, ByteOrder.LITTLE_ENDIAN))), pixels(new DDSParser().parse(TEXParser.toDDS(bc5))));
    }

    private static byte[] tex4x4(int format, byte[] block)
    {
        ByteBuffer tex = ByteBuffer.allocate(12 + block.length).order(ByteOrder.LITTLE_ENDIAN);
        tex.put("TEX\0".getBytes()).putShort((short) 4).putShort((short) 4).put((byte) 1).put((byte) format).put((byte) 0).put((byte) 0);
        return tex.put(block).array();
    }
}

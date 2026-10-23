package no.stelar7.cdragon.types.dds;

import no.stelar7.cdragon.util.readers.ParseException;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Decodes raw (headerless) texture data to a BufferedImage.
 * <p>
 * Supports the S3TC/RGTC/BPTC block formats (BC1-BC5, BC6H, BC7), ETC1, ETC2/EAC, ASTC LDR (all 2D block sizes), UASTC,
 * a set of 8-bit-per-channel uncompressed layouts and 16 bit RGBA (UNORM, SNORM, half float).
 * Block formats are decoded per the D3D10/D3D11 / EXT_texture_compression_s3tc / OES_compressed_ETC1_RGB8_texture specs
 * and the Khronos Data Format spec. Output is always 8 bit ARGB; formats with a wider range are converted as documented
 * on their {@link Format} constant.
 */
public final class TextureDecoder
{
    public enum Format
    {
        BC1(4, 4, 8),
        /**
         * BC1 where the 3-color mode produces a transparent black pixel
         */
        BC1A(4, 4, 8),
        BC2(4, 4, 16),
        BC3(4, 4, 16),
        BC4(4, 4, 8),
        BC5(4, 4, 16),
        /**
         * BC6H unsigned float, channels clamped to [0, 1]
         */
        BC6H_UF16(4, 4, 16),
        /**
         * BC6H signed float, channels clamped to [0, 1]
         */
        BC6H_SF16(4, 4, 16),
        BC7(4, 4, 16),
        ETC1(4, 4, 8),
        ETC2_RGB(4, 4, 8),
        /**
         * ETC2 RGB8 with punch-through (1 bit) alpha
         */
        ETC2_RGB_A1(4, 4, 8),
        /**
         * ETC2 RGB8 with an EAC alpha block
         */
        ETC2_RGBA(4, 4, 16),
        /**
         * Unsigned EAC R11, written as grey
         */
        EAC_R11(4, 4, 8),
        /**
         * Unsigned EAC RG11
         */
        EAC_RG11(4, 4, 16),
        ASTC_4x4(4, 4, 16),
        ASTC_5x4(5, 4, 16),
        ASTC_5x5(5, 5, 16),
        ASTC_6x5(6, 5, 16),
        ASTC_6x6(6, 6, 16),
        ASTC_8x5(8, 5, 16),
        ASTC_8x6(8, 6, 16),
        ASTC_8x8(8, 8, 16),
        ASTC_10x5(10, 5, 16),
        ASTC_10x6(10, 6, 16),
        ASTC_10x8(10, 8, 16),
        ASTC_10x10(10, 10, 16),
        ASTC_12x10(12, 10, 16),
        ASTC_12x12(12, 12, 16),
        /**
         * UASTC LDR 4x4 (Basis Universal), see {@link UASTCDecoder}
         */
        UASTC_4x4(4, 4, 16),
        RGBA8(1, 1, 4),
        BGRA8(1, 1, 4),
        RGB8(1, 1, 3),
        BGR8(1, 1, 3),
        R8(1, 1, 1),
        RG8(1, 1, 2),
        /**
         * 16 bit unsigned normalized RGBA (DXGI 11), rounded to 8 bits
         */
        RGBA16_UNORM(1, 1, 8),
        /**
         * 16 bit signed normalized RGBA (DXGI 13). [-1, 1] is remapped to [0, 255] as {@code (v + 1) / 2},
         * the same SNORM to UNORM conversion DirectXTex/texconv applies
         */
        RGBA16_SNORM(1, 1, 8),
        /**
         * 16 bit half float RGBA (DXGI 10), channels clamped to [0, 1]
         */
        RGBA16_FLOAT(1, 1, 8);

        private final int blockWidth;
        private final int blockHeight;
        private final int blockBytes;

        Format(int blockWidth, int blockHeight, int blockBytes)
        {
            this.blockWidth = blockWidth;
            this.blockHeight = blockHeight;
            this.blockBytes = blockBytes;
        }

        public boolean isBlockCompressed()
        {
            return blockWidth > 1;
        }

        public int getBlockBytes()
        {
            return blockBytes;
        }

        /**
         * The tightly packed size of one width x height image in this format
         */
        public long getImageSize(int width, int height)
        {
            long bw = (width + blockWidth - 1) / blockWidth;
            long bh = (height + blockHeight - 1) / blockHeight;
            return bw * bh * blockBytes;
        }

        public int getBlockWidth()
        {
            return blockWidth;
        }

        public int getBlockHeight()
        {
            return blockHeight;
        }

        /**
         * The ASTC format with this block size, or null
         */
        public static Format astc(int blockWidth, int blockHeight)
        {
            for (Format format : values())
            {
                if (format.name().startsWith("ASTC_") && format.blockWidth == blockWidth && format.blockHeight == blockHeight)
                {
                    return format;
                }
            }
            return null;
        }
    }

    private TextureDecoder()
    {
    }

    /**
     * Decodes tightly packed data
     */
    public static BufferedImage decode(Format format, byte[] data, int offset, int width, int height)
    {
        return decode(format, data, offset, width, height, 0);
    }

    /**
     * @param rowPitch bytes between the start of two rows for uncompressed formats, 0 for tightly packed. Ignored for block formats.
     */
    public static BufferedImage decode(Format format, byte[] data, int offset, int width, int height, int rowPitch)
    {
        if (width <= 0 || height <= 0)
        {
            throw new ParseException("Invalid texture size " + width + "x" + height, offset);
        }

        long needed = format.isBlockCompressed() ? format.getImageSize(width, height) : (long) (rowPitch > 0 ? rowPitch : width * format.blockBytes) * (height - 1) + (long) width * format.blockBytes;
        if (offset < 0 || offset + needed > data.length)
        {
            throw new ParseException("Texture data too short for " + width + "x" + height + " " + format + ": need " + needed + " bytes, have " + (data.length - offset), offset);
        }

        BufferedImage image  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[]         pixels = pixels(image);
        if (!format.isBlockCompressed())
        {
            decodeUncompressed(format, data, offset, width, height, rowPitch > 0 ? rowPitch : width * format.blockBytes, pixels);
            return image;
        }

        int   bw      = format.blockWidth;
        int   bh      = format.blockHeight;
        int   blocksX = (width + bw - 1) / bw;
        int   blocksY = (height + bh - 1) / bh;
        int[] block   = new int[bw * bh];
        int   pos     = offset;
        for (int by = 0; by < blocksY; by++)
        {
            for (int bx = 0; bx < blocksX; bx++)
            {
                decodeBlock(format, data, pos, block);
                pos += format.blockBytes;

                int x    = bx * bw;
                int copy = Math.min(bw, width - x);
                int rows = Math.min(bh, height - by * bh);
                for (int py = 0; py < rows; py++)
                {
                    System.arraycopy(block, py * bw, pixels, (by * bh + py) * width + x, copy);
                }
            }
        }

        return image;
    }

    /**
     * The backing ARGB array of a TYPE_INT_ARGB image, written to directly instead of going through setRGB
     */
    private static int[] pixels(BufferedImage image)
    {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    /**
     * Decodes one block of a block compressed format into blockWidth * blockHeight ARGB pixels (row major)
     */
    static void decodeBlock(Format format, byte[] data, int pos, int[] block)
    {
        switch (format)
        {
            case BC1 -> decodeColorBlock(data, pos, block, false, false);
            case BC1A -> decodeColorBlock(data, pos, block, false, true);
            case BC2 ->
            {
                decodeColorBlock(data, pos + 8, block, true, false);
                decodeExplicitAlpha(data, pos, block);
            }
            case BC3 ->
            {
                decodeColorBlock(data, pos + 8, block, true, false);
                int[] alpha = decodeInterpolatedChannel(data, pos);
                for (int i = 0; i < 16; i++)
                {
                    block[i] = (block[i] & 0x00FFFFFF) | (alpha[i] << 24);
                }
            }
            case BC4 ->
            {
                int[] red = decodeInterpolatedChannel(data, pos);
                for (int i = 0; i < 16; i++)
                {
                    block[i] = 0xFF000000 | (red[i] << 16) | (red[i] << 8) | red[i];
                }
            }
            case BC5 ->
            {
                int[] red   = decodeInterpolatedChannel(data, pos);
                int[] green = decodeInterpolatedChannel(data, pos + 8);
                for (int i = 0; i < 16; i++)
                {
                    block[i] = 0xFF000000 | (red[i] << 16) | (green[i] << 8);
                }
            }
            case BC6H_UF16 -> BC6HDecoder.decodeBlock(data, pos, false, block);
            case BC6H_SF16 -> BC6HDecoder.decodeBlock(data, pos, true, block);
            case BC7 -> BC7Decoder.decodeBlock(data, pos, block);
            case ETC1 -> ETC2Decoder.decodeETC1(data, pos, block);
            case ETC2_RGB -> ETC2Decoder.decodeRGB(data, pos, block, false);
            case ETC2_RGB_A1 -> ETC2Decoder.decodeRGB(data, pos, block, true);
            case ETC2_RGBA -> ETC2Decoder.decodeRGBA(data, pos, block);
            case EAC_R11 ->
            {
                ETC2Decoder.decodeEAC11(data, pos, block);
                for (int i = 0; i < 16; i++)
                {
                    int r = ETC2Decoder.eac11To8(block[i]);
                    block[i] = 0xFF000000 | (r << 16) | (r << 8) | r;
                }
            }
            case EAC_RG11 ->
            {
                int[] green = new int[16];
                ETC2Decoder.decodeEAC11(data, pos, block);
                ETC2Decoder.decodeEAC11(data, pos + 8, green);
                for (int i = 0; i < 16; i++)
                {
                    block[i] = 0xFF000000 | (ETC2Decoder.eac11To8(block[i]) << 16) | (ETC2Decoder.eac11To8(green[i]) << 8);
                }
            }
            case UASTC_4x4 -> UASTCDecoder.decodeBlock(data, pos, block);
            default ->
            {
                if (format.name().startsWith("ASTC_"))
                {
                    ASTCDecoder.decodeBlock(data, pos, format.blockWidth, format.blockHeight, block);
                } else
                {
                    throw new IllegalStateException(format.name());
                }
            }
        }
    }

    private static void decodeUncompressed(Format format, byte[] data, int offset, int width, int height, int rowPitch, int[] pixels)
    {
        for (int y = 0; y < height; y++)
        {
            int pos = offset + y * rowPitch;
            for (int x = 0; x < width; x++)
            {
                int argb = switch (format)
                {
                    case RGBA8 -> argb(u8(data, pos), u8(data, pos + 1), u8(data, pos + 2), u8(data, pos + 3));
                    case BGRA8 -> argb(u8(data, pos + 2), u8(data, pos + 1), u8(data, pos), u8(data, pos + 3));
                    case RGB8 -> argb(u8(data, pos), u8(data, pos + 1), u8(data, pos + 2), 255);
                    case BGR8 -> argb(u8(data, pos + 2), u8(data, pos + 1), u8(data, pos), 255);
                    case R8 -> argb(u8(data, pos), u8(data, pos), u8(data, pos), 255);
                    case RG8 -> argb(u8(data, pos), u8(data, pos + 1), 0, 255);
                    case RGBA16_UNORM -> argb(unorm16(data, pos), unorm16(data, pos + 2), unorm16(data, pos + 4), unorm16(data, pos + 6));
                    case RGBA16_SNORM -> argb(snorm16(data, pos), snorm16(data, pos + 2), snorm16(data, pos + 4), snorm16(data, pos + 6));
                    case RGBA16_FLOAT -> argb(HalfFloat.toUnorm8(u16le(data, pos)), HalfFloat.toUnorm8(u16le(data, pos + 2)), HalfFloat.toUnorm8(u16le(data, pos + 4)), HalfFloat.toUnorm8(u16le(data, pos + 6)));
                    default -> throw new IllegalStateException(format.name());
                };
                pixels[y * width + x] = argb;
                pos += format.blockBytes;
            }
        }
    }

    /**
     * Decodes uncompressed pixels described by DDS-style channel bit masks (little endian pixels of bitCount bits).
     * A zero mask means the channel is absent (alpha defaults to 255, color to 0).
     */
    public static BufferedImage decodeMasked(byte[] data, int offset, int width, int height, int bitCount, int rowPitch, long rMask, long gMask, long bMask, long aMask, boolean luminance)
    {
        if (bitCount != 8 && bitCount != 16 && bitCount != 24 && bitCount != 32)
        {
            throw new ParseException("Unsupported uncompressed bit count " + bitCount, offset);
        }

        int bytesPerPixel = bitCount / 8;
        if (rowPitch <= 0)
        {
            rowPitch = width * bytesPerPixel;
        }
        long needed = (long) rowPitch * (height - 1) + (long) width * bytesPerPixel;
        if (width <= 0 || height <= 0 || offset < 0 || offset + needed > data.length)
        {
            throw new ParseException("Texture data too short for " + width + "x" + height + " at " + bitCount + "bpp", offset);
        }

        BufferedImage image  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[]         pixels = pixels(image);
        for (int y = 0; y < height; y++)
        {
            int pos = offset + y * rowPitch;
            for (int x = 0; x < width; x++)
            {
                long pixel = 0;
                for (int i = 0; i < bytesPerPixel; i++)
                {
                    pixel |= (long) u8(data, pos + i) << (8 * i);
                }
                pos += bytesPerPixel;

                int r = extract(pixel, rMask, 0);
                int g = luminance ? r : extract(pixel, gMask, 0);
                int b = luminance ? r : extract(pixel, bMask, 0);
                int a = extract(pixel, aMask, 255);
                pixels[y * width + x] = argb(r, g, b, a);
            }
        }
        return image;
    }

    private static int extract(long pixel, long mask, int fallback)
    {
        mask &= 0xFFFFFFFFL;
        if (mask == 0)
        {
            return fallback;
        }
        int  shift = Long.numberOfTrailingZeros(mask);
        long max   = mask >>> shift;
        long value = (pixel & mask) >>> shift;
        return (int) ((value * 255 + max / 2) / max);
    }

    private static int u8(byte[] data, int pos)
    {
        return data[pos] & 0xFF;
    }

    private static int argb(int r, int g, int b, int a)
    {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int unorm16(byte[] data, int pos)
    {
        return (u16le(data, pos) * 255 + 32767) / 65535;
    }

    /**
     * SNORM to UNORM like DirectXTex: max(v / 32767, -1) remapped from [-1, 1] to [0, 1], then rounded to 8 bits
     */
    private static int snorm16(byte[] data, int pos)
    {
        int   v = (short) u16le(data, pos);
        float f = Math.max(v / 32767f, -1f);
        return HalfFloat.unitToUnorm8(f * 0.5f + 0.5f);
    }

    private static int u16le(byte[] data, int pos)
    {
        return u8(data, pos) | (u8(data, pos + 1) << 8);
    }

    /**
     * Decodes an 8 byte BC1 color block into 16 ARGB pixels (row major)
     *
     * @param forceFourColor BC2/BC3 always use the 4 color mode
     * @param punchThrough   the 3 color mode's 4th color is transparent instead of opaque black
     */
    private static void decodeColorBlock(byte[] data, int pos, int[] out, boolean forceFourColor, boolean punchThrough)
    {
        int c0 = u16le(data, pos);
        int c1 = u16le(data, pos + 2);

        int r0 = expand5((c0 >> 11) & 0x1F);
        int g0 = expand6((c0 >> 5) & 0x3F);
        int b0 = expand5(c0 & 0x1F);
        int r1 = expand5((c1 >> 11) & 0x1F);
        int g1 = expand6((c1 >> 5) & 0x3F);
        int b1 = expand5(c1 & 0x1F);

        int[] palette = new int[4];
        palette[0] = argb(r0, g0, b0, 255);
        palette[1] = argb(r1, g1, b1, 255);
        if (forceFourColor || c0 > c1)
        {
            palette[2] = argb((2 * r0 + r1) / 3, (2 * g0 + g1) / 3, (2 * b0 + b1) / 3, 255);
            palette[3] = argb((r0 + 2 * r1) / 3, (g0 + 2 * g1) / 3, (b0 + 2 * b1) / 3, 255);
        } else
        {
            palette[2] = argb((r0 + r1) / 2, (g0 + g1) / 2, (b0 + b1) / 2, 255);
            palette[3] = argb(0, 0, 0, punchThrough ? 0 : 255);
        }

        int indices = u16le(data, pos + 4) | (u16le(data, pos + 6) << 16);
        for (int i = 0; i < 16; i++)
        {
            out[i] = palette[(indices >>> (2 * i)) & 3];
        }
    }

    /**
     * BC2 alpha: 16 explicit 4 bit values
     */
    private static void decodeExplicitAlpha(byte[] data, int pos, int[] out)
    {
        for (int i = 0; i < 16; i++)
        {
            int value = (u8(data, pos + i / 2) >> ((i & 1) * 4)) & 0xF;
            out[i] = (out[i] & 0x00FFFFFF) | ((value * 17) << 24);
        }
    }

    /**
     * BC3 alpha / BC4 / BC5 channel block: two 8 bit endpoints followed by 16 3-bit indices
     */
    private static int[] decodeInterpolatedChannel(byte[] data, int pos)
    {
        int   v0      = u8(data, pos);
        int   v1      = u8(data, pos + 1);
        int[] palette = new int[8];
        palette[0] = v0;
        palette[1] = v1;
        if (v0 > v1)
        {
            for (int i = 1; i < 7; i++)
            {
                palette[i + 1] = ((7 - i) * v0 + i * v1) / 7;
            }
        } else
        {
            for (int i = 1; i < 5; i++)
            {
                palette[i + 1] = ((5 - i) * v0 + i * v1) / 5;
            }
            palette[6] = 0;
            palette[7] = 255;
        }

        long bits = 0;
        for (int i = 0; i < 6; i++)
        {
            bits |= (long) u8(data, pos + 2 + i) << (8 * i);
        }

        int[] out = new int[16];
        for (int i = 0; i < 16; i++)
        {
            out[i] = palette[(int) ((bits >>> (3 * i)) & 7)];
        }
        return out;
    }

    private static int expand5(int v)
    {
        return (v << 3) | (v >> 2);
    }

    private static int expand6(int v)
    {
        return (v << 2) | (v >> 4);
    }
}

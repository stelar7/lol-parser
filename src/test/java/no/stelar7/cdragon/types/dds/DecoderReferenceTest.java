package no.stelar7.cdragon.types.dds;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compares the optimized decoder paths against straightforward reference versions on random blocks:
 * the BC7 bit reader (read whole fields instead of bit by bit), ETC1 (shared with the ETC2 sub block code),
 * the BC1 color block (packed palette) and the copy of partial edge blocks into the image.
 */
public class DecoderReferenceTest
{
    private static final int BLOCKS = 20000;

    @Test
    public void testBitReaderMatchesBitByBit()
    {
        Random random = new Random(1);
        byte[] block  = new byte[16];
        for (int n = 0; n < BLOCKS; n++)
        {
            random.nextBytes(block);
            BC7Decoder.BitReader fast = new BC7Decoder.BitReader(block, 0);
            int                  pos  = 0;
            while (true)
            {
                int count = random.nextInt(33);
                if (pos + count > 128)
                {
                    break;
                }
                assertEquals(referenceBits(block, pos, count), fast.read(count), "bits " + pos + ".." + (pos + count));
                pos += count;
                assertEquals(pos, fast.position());
            }
        }
    }

    @Test
    public void testBitReaderPastTheEndReadsZero()
    {
        byte[] block = new byte[16];
        java.util.Arrays.fill(block, (byte) 0xFF);
        BC7Decoder.BitReader bits = new BC7Decoder.BitReader(block, 0);
        bits.read(30);
        bits.read(30);
        bits.read(30);
        bits.read(30);
        assertEquals(0xFF, bits.read(16), "the last 8 bits, then zeros");
        assertEquals(0, bits.read(8));
    }

    private static int referenceBits(byte[] block, int pos, int count)
    {
        int value = 0;
        for (int i = 0; i < count; i++)
        {
            int p = pos + i;
            value |= ((block[p >> 3] >> (p & 7)) & 1) << i;
        }
        return value;
    }

    @Test
    public void testETC1MatchesReference()
    {
        Random random = new Random(2);
        byte[] block  = new byte[8];
        int[]  fast   = new int[16];
        int[]  slow   = new int[16];
        for (int n = 0; n < BLOCKS; n++)
        {
            random.nextBytes(block);
            TextureDecoder.decodeBlock(TextureDecoder.Format.ETC1, block, 0, fast);
            referenceETC1(block, slow);
            assertArrayEquals(slow, fast, "block " + n);
        }
    }

    @Test
    public void testBC1To3MatchReference()
    {
        Random random = new Random(3);
        byte[] block  = new byte[16];
        int[]  fast   = new int[16];
        int[]  slow   = new int[16];
        for (int n = 0; n < BLOCKS; n++)
        {
            random.nextBytes(block);
            if (n % 2 == 0)
            {
                // make the 3 color mode (c0 <= c1) as likely as the 4 color one
                block[1] = 0;
                block[3] = (byte) 0xFF;
            }

            TextureDecoder.decodeBlock(TextureDecoder.Format.BC1, block, 0, fast);
            referenceColorBlock(block, 0, slow, false, false);
            assertArrayEquals(slow, fast, "BC1 block " + n);

            TextureDecoder.decodeBlock(TextureDecoder.Format.BC1A, block, 0, fast);
            referenceColorBlock(block, 0, slow, false, true);
            assertArrayEquals(slow, fast, "BC1A block " + n);

            TextureDecoder.decodeBlock(TextureDecoder.Format.BC2, block, 0, fast);
            referenceColorBlock(block, 8, slow, true, false);
            for (int i = 0; i < 16; i++)
            {
                assertEquals(slow[i] & 0xFFFFFF, fast[i] & 0xFFFFFF, "BC2 color, block " + n);
            }
        }
    }

    @Test
    public void testPartialEdgeBlocks()
    {
        Random random = new Random(4);
        for (TextureDecoder.Format format : new TextureDecoder.Format[]{TextureDecoder.Format.BC7, TextureDecoder.Format.ASTC_5x4, TextureDecoder.Format.ASTC_12x12})
        {
            int    width   = 29;
            int    height  = 17;
            int    blocksX = (width + format.getBlockWidth() - 1) / format.getBlockWidth();
            int    blocksY = (height + format.getBlockHeight() - 1) / format.getBlockHeight();
            byte[] data    = new byte[blocksX * blocksY * format.getBlockBytes()];
            random.nextBytes(data);

            BufferedImage image = TextureDecoder.decode(format, data, 0, width, height);
            int[]         block = new int[format.getBlockWidth() * format.getBlockHeight()];
            for (int by = 0; by < blocksY; by++)
            {
                for (int bx = 0; bx < blocksX; bx++)
                {
                    TextureDecoder.decodeBlock(format, data, (by * blocksX + bx) * format.getBlockBytes(), block);
                    for (int py = 0; py < format.getBlockHeight(); py++)
                    {
                        for (int px = 0; px < format.getBlockWidth(); px++)
                        {
                            int x = bx * format.getBlockWidth() + px;
                            int y = by * format.getBlockHeight() + py;
                            if (x < width && y < height)
                            {
                                assertEquals(block[py * format.getBlockWidth() + px], image.getRGB(x, y), format + " pixel " + x + "," + y);
                            }
                        }
                    }
                }
            }
        }
    }

    // the ETC1 decoder as it was before it moved into ETC2Decoder

    private static final int[][] ETC1_MODIFIERS = {{2, 8}, {5, 17}, {9, 29}, {13, 42}, {18, 60}, {24, 80}, {33, 106}, {47, 183}};

    private static void referenceETC1(byte[] data, int[] out)
    {
        int b0 = data[0] & 0xFF;
        int b1 = data[1] & 0xFF;
        int b2 = data[2] & 0xFF;
        int b3 = data[3] & 0xFF;

        int r1, g1, bl1, r2, g2, bl2;
        if ((b3 & 2) != 0)
        {
            int rBase = b0 >> 3;
            int gBase = b1 >> 3;
            int bBase = b2 >> 3;
            r1 = expand5(rBase);
            g1 = expand5(gBase);
            bl1 = expand5(bBase);
            r2 = expand5((rBase + signExtend3(b0 & 7)) & 0x1F);
            g2 = expand5((gBase + signExtend3(b1 & 7)) & 0x1F);
            bl2 = expand5((bBase + signExtend3(b2 & 7)) & 0x1F);
        } else
        {
            r1 = expand4(b0 >> 4);
            r2 = expand4(b0 & 0xF);
            g1 = expand4(b1 >> 4);
            g2 = expand4(b1 & 0xF);
            bl1 = expand4(b2 >> 4);
            bl2 = expand4(b2 & 0xF);
        }

        int     table1  = (b3 >> 5) & 7;
        int     table2  = (b3 >> 2) & 7;
        boolean flipped = (b3 & 1) != 0;
        int     msb     = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
        int     lsb     = ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);

        for (int y = 0; y < 4; y++)
        {
            for (int x = 0; x < 4; x++)
            {
                boolean second = flipped ? y >= 2 : x >= 2;
                int     k      = x * 4 + y;
                int     index  = (((msb >> k) & 1) << 1) | ((lsb >> k) & 1);
                int[]   row    = ETC1_MODIFIERS[second ? table2 : table1];
                int modifier = switch (index)
                {
                    case 0 -> row[0];
                    case 1 -> row[1];
                    case 2 -> -row[0];
                    default -> -row[1];
                };
                out[y * 4 + x] = argb(clamp((second ? r2 : r1) + modifier), clamp((second ? g2 : g1) + modifier), clamp((second ? bl2 : bl1) + modifier), 255);
            }
        }
    }

    // the BC1 color block as it was before the palette was packed

    private static void referenceColorBlock(byte[] data, int pos, int[] out, boolean forceFourColor, boolean punchThrough)
    {
        int c0 = (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8);
        int c1 = (data[pos + 2] & 0xFF) | ((data[pos + 3] & 0xFF) << 8);

        int[] r = new int[4];
        int[] g = new int[4];
        int[] b = new int[4];
        int[] a = {255, 255, 255, 255};
        r[0] = expand5((c0 >> 11) & 0x1F);
        g[0] = expand6((c0 >> 5) & 0x3F);
        b[0] = expand5(c0 & 0x1F);
        r[1] = expand5((c1 >> 11) & 0x1F);
        g[1] = expand6((c1 >> 5) & 0x3F);
        b[1] = expand5(c1 & 0x1F);
        if (forceFourColor || c0 > c1)
        {
            r[2] = (2 * r[0] + r[1]) / 3;
            g[2] = (2 * g[0] + g[1]) / 3;
            b[2] = (2 * b[0] + b[1]) / 3;
            r[3] = (r[0] + 2 * r[1]) / 3;
            g[3] = (g[0] + 2 * g[1]) / 3;
            b[3] = (b[0] + 2 * b[1]) / 3;
        } else
        {
            r[2] = (r[0] + r[1]) / 2;
            g[2] = (g[0] + g[1]) / 2;
            b[2] = (b[0] + b[1]) / 2;
            a[3] = punchThrough ? 0 : 255;
        }

        long indices = 0;
        for (int i = 0; i < 4; i++)
        {
            indices |= (long) (data[pos + 4 + i] & 0xFF) << (8 * i);
        }
        for (int i = 0; i < 16; i++)
        {
            int idx = (int) ((indices >>> (2 * i)) & 3);
            out[i] = argb(r[idx], g[idx], b[idx], a[idx]);
        }
    }

    private static int argb(int r, int g, int b, int a)
    {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int signExtend3(int v)
    {
        return (v & 4) != 0 ? v - 8 : v;
    }

    private static int clamp(int v)
    {
        return Math.max(0, Math.min(255, v));
    }

    private static int expand4(int v)
    {
        return (v << 4) | v;
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

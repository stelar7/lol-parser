package no.stelar7.cdragon.types.dds;

/**
 * ETC2 / EAC block decoders, following the Khronos Data Format spec (ETC2 and EAC sections) / OpenGL ES 3.0 Annex C.
 * <ul>
 *     <li>ETC2 RGB8: individual, differential, T, H and planar modes</li>
 *     <li>ETC2 RGB8 punch-through alpha (RGB8A1)</li>
 *     <li>ETC2 RGBA8: EAC alpha block followed by an ETC2 RGB8 block</li>
 *     <li>EAC R11 / RG11 (unsigned), converted to 8 bit with rounding</li>
 * </ul>
 * Pixels are written row major into a 16 entry ARGB array.
 */
final class ETC2Decoder
{
    private ETC2Decoder()
    {
    }

    private static final int[][] MODIFIERS = {
            {2, 8},
            {5, 17},
            {9, 29},
            {13, 42},
            {18, 60},
            {24, 80},
            {33, 106},
            {47, 183},
            };

    private static final int[] DISTANCES = {3, 6, 11, 16, 23, 32, 41, 64};

    static final int[][] EAC_MODIFIERS = {
            {-3, -6, -9, -15, 2, 5, 8, 14},
            {-3, -7, -10, -13, 2, 6, 9, 12},
            {-2, -5, -8, -13, 1, 4, 7, 12},
            {-2, -4, -6, -13, 1, 3, 5, 12},
            {-3, -6, -8, -12, 2, 5, 7, 11},
            {-3, -7, -9, -11, 2, 6, 8, 10},
            {-4, -7, -8, -11, 3, 6, 7, 10},
            {-3, -5, -8, -11, 2, 4, 7, 10},
            {-2, -6, -8, -10, 1, 5, 7, 9},
            {-2, -5, -8, -10, 1, 4, 7, 9},
            {-2, -4, -8, -10, 1, 3, 7, 9},
            {-2, -5, -7, -10, 1, 4, 6, 9},
            {-3, -4, -7, -10, 2, 3, 6, 9},
            {-1, -2, -3, -10, 0, 1, 2, 9},
            {-4, -6, -8, -9, 3, 5, 7, 8},
            {-3, -5, -7, -9, 2, 4, 6, 8},
            };

    /**
     * Decodes an 8 byte ETC1 block. Unlike ETC2, an out of range differential color wraps around instead of selecting the T, H or planar mode.
     */
    static void decodeETC1(byte[] data, int pos, int[] out)
    {
        int b0 = data[pos] & 0xFF;
        int b1 = data[pos + 1] & 0xFF;
        int b2 = data[pos + 2] & 0xFF;
        int b3 = data[pos + 3] & 0xFF;

        int msb = ((data[pos + 4] & 0xFF) << 8) | (data[pos + 5] & 0xFF);
        int lsb = ((data[pos + 6] & 0xFF) << 8) | (data[pos + 7] & 0xFF);

        int[] c1;
        int[] c2;
        if ((b3 & 2) != 0)
        {
            // differential mode: 5 bit base + signed 3 bit delta
            c1 = new int[]{expand5(b0 >> 3), expand5(b1 >> 3), expand5(b2 >> 3)};
            c2 = new int[]{expand5(((b0 >> 3) + signExtend3(b0 & 7)) & 0x1F), expand5(((b1 >> 3) + signExtend3(b1 & 7)) & 0x1F), expand5(((b2 >> 3) + signExtend3(b2 & 7)) & 0x1F)};
        } else
        {
            // individual mode: two 4 bit colors
            c1 = new int[]{expand4(b0 >> 4), expand4(b1 >> 4), expand4(b2 >> 4)};
            c2 = new int[]{expand4(b0 & 0xF), expand4(b1 & 0xF), expand4(b2 & 0xF)};
        }
        decodeSubBlocks(b3, msb, lsb, c1, c2, true, out);
    }

    /**
     * Decodes an 8 byte ETC2 RGB block
     *
     * @param punchThrough decode as RGB8A1: bit 33 is the "opaque" flag instead of the "diff" flag
     */
    static void decodeRGB(byte[] data, int pos, int[] out, boolean punchThrough)
    {
        int b0 = data[pos] & 0xFF;
        int b1 = data[pos + 1] & 0xFF;
        int b2 = data[pos + 2] & 0xFF;
        int b3 = data[pos + 3] & 0xFF;

        boolean flag   = (b3 & 2) != 0;
        boolean opaque = !punchThrough || flag;
        boolean diff   = punchThrough || flag;

        int msb = ((data[pos + 4] & 0xFF) << 8) | (data[pos + 5] & 0xFF);
        int lsb = ((data[pos + 6] & 0xFF) << 8) | (data[pos + 7] & 0xFF);

        if (!diff)
        {
            int[] c1 = {expand4(b0 >> 4), expand4(b1 >> 4), expand4(b2 >> 4)};
            int[] c2 = {expand4(b0 & 0xF), expand4(b1 & 0xF), expand4(b2 & 0xF)};
            decodeSubBlocks(b3, msb, lsb, c1, c2, true, out);
            return;
        }

        int r = (b0 >> 3) + signExtend3(b0 & 7);
        int g = (b1 >> 3) + signExtend3(b1 & 7);
        int b = (b2 >> 3) + signExtend3(b2 & 7);

        if (r < 0 || r > 31)
        {
            // T mode
            int[] c1    = {expand4((((b0 >> 3) & 3) << 2) | (b0 & 3)), expand4(b1 >> 4), expand4(b1 & 0xF)};
            int[] c2    = {expand4(b2 >> 4), expand4(b2 & 0xF), expand4(b3 >> 4)};
            int   d     = DISTANCES[(((b3 >> 2) & 3) << 1) | (b3 & 1)];
            int[] paint = {
                    rgb(c1[0], c1[1], c1[2]),
                    rgb(c2[0] + d, c2[1] + d, c2[2] + d),
                    rgb(c2[0], c2[1], c2[2]),
                    rgb(c2[0] - d, c2[1] - d, c2[2] - d),
                    };
            decodePaint(paint, msb, lsb, opaque, out);
            return;
        }

        if (g < 0 || g > 31)
        {
            // H mode
            int r1 = (b0 >> 3) & 0xF;
            int g1 = ((b0 & 7) << 1) | ((b1 >> 4) & 1);
            int bl1 = (b1 & 8) | ((b1 & 3) << 1) | (b2 >> 7);
            int r2 = (b2 >> 3) & 0xF;
            int g2 = ((b2 & 7) << 1) | (b3 >> 7);
            int bl2 = (b3 >> 3) & 0xF;

            int v1 = (r1 << 8) | (g1 << 4) | bl1;
            int v2 = (r2 << 8) | (g2 << 4) | bl2;
            int d  = DISTANCES[(((b3 >> 2) & 1) << 2) | ((b3 & 1) << 1) | (v1 >= v2 ? 1 : 0)];

            r1 = expand4(r1);
            g1 = expand4(g1);
            bl1 = expand4(bl1);
            r2 = expand4(r2);
            g2 = expand4(g2);
            bl2 = expand4(bl2);
            int[] paint = {
                    rgb(r1 + d, g1 + d, bl1 + d),
                    rgb(r1 - d, g1 - d, bl1 - d),
                    rgb(r2 + d, g2 + d, bl2 + d),
                    rgb(r2 - d, g2 - d, bl2 - d),
                    };
            decodePaint(paint, msb, lsb, opaque, out);
            return;
        }

        if (b < 0 || b > 31)
        {
            decodePlanar(data, pos, out);
            return;
        }

        int[] c1 = {expand5(b0 >> 3), expand5(b1 >> 3), expand5(b2 >> 3)};
        int[] c2 = {expand5(r), expand5(g), expand5(b)};
        decodeSubBlocks(b3, msb, lsb, c1, c2, opaque, out);
    }

    private static void decodeSubBlocks(int b3, int msb, int lsb, int[] c1, int[] c2, boolean opaque, int[] out)
    {
        int     table1  = (b3 >> 5) & 7;
        int     table2  = (b3 >> 2) & 7;
        boolean flipped = (b3 & 1) != 0;

        for (int y = 0; y < 4; y++)
        {
            for (int x = 0; x < 4; x++)
            {
                boolean second = flipped ? y >= 2 : x >= 2;
                int     k      = x * 4 + y;
                int     index  = (((msb >> k) & 1) << 1) | ((lsb >> k) & 1);
                int[]   row    = MODIFIERS[second ? table2 : table1];
                int[]   c      = second ? c2 : c1;

                if (!opaque && index == 2)
                {
                    out[y * 4 + x] = 0;
                    continue;
                }

                int modifier = switch (index)
                {
                    case 0 -> opaque ? row[0] : 0;
                    case 1 -> row[1];
                    case 2 -> -row[0];
                    default -> -row[1];
                };
                out[y * 4 + x] = rgb(c[0] + modifier, c[1] + modifier, c[2] + modifier);
            }
        }
    }

    private static void decodePaint(int[] paint, int msb, int lsb, boolean opaque, int[] out)
    {
        for (int y = 0; y < 4; y++)
        {
            for (int x = 0; x < 4; x++)
            {
                int k     = x * 4 + y;
                int index = (((msb >> k) & 1) << 1) | ((lsb >> k) & 1);
                out[y * 4 + x] = (!opaque && index == 2) ? 0 : paint[index];
            }
        }
    }

    private static void decodePlanar(byte[] data, int pos, int[] out)
    {
        int b0 = data[pos] & 0xFF;
        int b1 = data[pos + 1] & 0xFF;
        int b2 = data[pos + 2] & 0xFF;
        int b3 = data[pos + 3] & 0xFF;
        int b4 = data[pos + 4] & 0xFF;
        int b5 = data[pos + 5] & 0xFF;
        int b6 = data[pos + 6] & 0xFF;
        int b7 = data[pos + 7] & 0xFF;

        int ro = expand6((b0 >> 1) & 0x3F);
        int go = expand7(((b0 & 1) << 6) | ((b1 >> 1) & 0x3F));
        int bo = expand6(((b1 & 1) << 5) | (b2 & 0x18) | ((b2 & 3) << 1) | (b3 >> 7));
        int rh = expand6((((b3 >> 2) & 0x1F) << 1) | (b3 & 1));
        int gh = expand7(b4 >> 1);
        int bh = expand6(((b4 & 1) << 5) | (b5 >> 3));
        int rv = expand6(((b5 & 7) << 3) | (b6 >> 5));
        int gv = expand7(((b6 & 0x1F) << 2) | (b7 >> 6));
        int bv = expand6(b7 & 0x3F);

        for (int y = 0; y < 4; y++)
        {
            for (int x = 0; x < 4; x++)
            {
                int r = (x * (rh - ro) + y * (rv - ro) + 4 * ro + 2) >> 2;
                int g = (x * (gh - go) + y * (gv - go) + 4 * go + 2) >> 2;
                int b = (x * (bh - bo) + y * (bv - bo) + 4 * bo + 2) >> 2;
                out[y * 4 + x] = rgb(r, g, b);
            }
        }
    }

    /**
     * Decodes a 16 byte ETC2 RGBA8 block (EAC alpha, then ETC2 RGB)
     */
    static void decodeRGBA(byte[] data, int pos, int[] out)
    {
        decodeRGB(data, pos + 8, out, false);
        int[] alpha = new int[16];
        decodeEACAlpha(data, pos, alpha);
        for (int i = 0; i < 16; i++)
        {
            out[i] = (out[i] & 0x00FFFFFF) | (alpha[i] << 24);
        }
    }

    /**
     * Decodes an 8 byte EAC block with 8 bit precision (the ETC2 RGBA8 alpha channel), row major
     */
    static void decodeEACAlpha(byte[] data, int pos, int[] out)
    {
        int  base       = data[pos] & 0xFF;
        int  multiplier = (data[pos + 1] & 0xFF) >> 4;
        int[] table     = EAC_MODIFIERS[data[pos + 1] & 0xF];
        long bits       = indexBits(data, pos);

        for (int i = 0; i < 16; i++)
        {
            int index = (int) ((bits >>> (45 - 3 * i)) & 7);
            int x     = i >> 2;
            int y     = i & 3;
            out[y * 4 + x] = clamp(base + table[index] * multiplier);
        }
    }

    /**
     * Decodes an 8 byte unsigned EAC R11 block to 11 bit values (0..2047), row major
     */
    static void decodeEAC11(byte[] data, int pos, int[] out)
    {
        int   base       = data[pos] & 0xFF;
        int   multiplier = (data[pos + 1] & 0xFF) >> 4;
        int[] table      = EAC_MODIFIERS[data[pos + 1] & 0xF];
        long  bits       = indexBits(data, pos);

        for (int i = 0; i < 16; i++)
        {
            int index = (int) ((bits >>> (45 - 3 * i)) & 7);
            int value = multiplier == 0 ? base * 8 + 4 + table[index] : base * 8 + 4 + table[index] * multiplier * 8;
            int x     = i >> 2;
            int y     = i & 3;
            out[y * 4 + x] = Math.max(0, Math.min(2047, value));
        }
    }

    static int eac11To8(int value)
    {
        return (value * 255 + 1023) / 2047;
    }

    private static long indexBits(byte[] data, int pos)
    {
        long bits = 0;
        for (int i = 2; i < 8; i++)
        {
            bits = (bits << 8) | (data[pos + i] & 0xFF);
        }
        return bits;
    }

    private static int rgb(int r, int g, int b)
    {
        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
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

    private static int expand7(int v)
    {
        return (v << 1) | (v >> 6);
    }
}

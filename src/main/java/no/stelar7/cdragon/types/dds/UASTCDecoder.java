package no.stelar7.cdragon.types.dds;

import java.util.Arrays;

/**
 * UASTC LDR 4x4 (Basis Universal) block decoder, following the "UASTC LDR 4x4 Texture Specification"
 * (github.com/BinomialLLC/basis_universal/wiki). UASTC is a re-packed subset of 4x4 LDR ASTC: endpoints are dequantized and
 * interpolated exactly like ASTC (decode_unorm8, srgb = false, which is what the reference encoder assumes).
 * Invalid blocks (reserved mode 19, out of range partition pattern) decode to opaque magenta. Over-range trit/quint group
 * codes are not rejected (their digits are used as-is), which matches the spec's 64 example blocks.
 */
final class UASTCDecoder
{
    private UASTCDecoder()
    {
    }

    private static final int[] HUFF_MODES = {
            11, 0, 10, 3, 11, 15, 12, 7, 11, 18, 10, 5, 11, 14, 12, 9, 11, 0, 10, 4, 11, 16, 12, 8, 11, 18, 10, 6, 11, 2, 12, 13,
            11, 0, 10, 3, 11, 17, 12, 7, 11, 18, 10, 5, 11, 14, 12, 9, 11, 0, 10, 4, 11, 1, 12, 8, 11, 18, 10, 6, 11, 2, 12, 13,
            11, 0, 10, 3, 11, 19, 12, 7, 11, 18, 10, 5, 11, 14, 12, 9, 11, 0, 10, 4, 11, 16, 12, 8, 11, 18, 10, 6, 11, 2, 12, 13,
            11, 0, 10, 3, 11, 17, 12, 7, 11, 18, 10, 5, 11, 14, 12, 9, 11, 0, 10, 4, 11, 1, 12, 8, 11, 18, 10, 6, 11, 2, 12, 13,
            };

    private static final int[] MODE_CODE_LENGTH = {4, 6, 5, 5, 5, 5, 5, 5, 5, 5, 3, 2, 3, 5, 5, 7, 6, 6, 4, 7};

    // per mode 0..18 (mode 8 is the solid color mode)
    private static final int[] WEIGHT_BITS     = {4, 2, 3, 2, 2, 3, 2, 2, 0, 2, 4, 2, 3, 1, 2, 4, 2, 2, 5};
    private static final int[] WEIGHT_RANGE    = {8, 2, 5, 2, 2, 5, 2, 2, 0, 2, 8, 2, 5, 0, 2, 8, 2, 2, 11};
    private static final int[] ENDPOINT_RANGE  = {19, 20, 8, 7, 12, 20, 18, 12, 0, 8, 13, 13, 19, 20, 20, 20, 20, 20, 11};
    private static final int[] SUBSETS         = {1, 1, 2, 3, 2, 1, 1, 2, 0, 2, 1, 1, 1, 1, 1, 1, 2, 1, 1};
    private static final int[] PLANES          = {1, 1, 1, 1, 1, 1, 2, 1, 0, 1, 1, 2, 1, 2, 1, 1, 1, 2, 1};
    private static final int[] COMPS           = {3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 2, 2, 2, 3};
    private static final int[] HAS_ETC1_BIAS   = {1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1};
    private static final int[] HAS_BC1_HINT0   = {1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    private static final int[] HAS_BC1_HINT1   = {1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1};
    private static final int[] HAS_ALPHA       = {0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0};

    private static final int[] TRIT_GROUP_BITS  = {0, 2, 4, 5, 7, 8};
    private static final int[] QUINT_GROUP_BITS = {0, 3, 5, 7};

    // subset index per texel (x + y * 4)
    private static final String[] PATTERNS2 = {
            "0011001100110011", "0001000100010001", "1000100010001000", "0001001100110111", "1111111011101100",
            "0011011101111111", "1110110010000000", "1111111011001000", "0000000000010011", "1100100000000000",
            "0000000101111111", "1111111111101000", "1110100000000000", "1111111100000000", "0000111111111111",
            "1111111111110000", "1000111011111111", "1111111101110001", "0111001100010000", "0011000100000000",
            "0000100011001110", "1111111101110011", "1000110011001110", "0011000100010000", "1111011101110011",
            "0110011001100110", "1111000000001111", "1010101010101010", "1111000011110000", "1001001101101100",
            };

    private static final String[] PATTERNS3 = {
            "0000000011221122", "1111111100002222", "1111000000002222", "1111222200000000", "1120112011201120",
            "0112011201120112", "0211021102110211", "2000200021112111", "2012201220122012", "1111000022221111",
            "0022001100110022",
            };

    // mode 7: 2 subset ASTC patterns that map to BC7 3 subset patterns
    private static final String[] PATTERNS2_BC73 = {
            "0000111100000000", "0010001000100010", "1100110010000000", "0000000100110011", "1111111100001111",
            "0100010001000100", "0001001111111111", "0111001100110011", "1100000000111100", "0111011100000000",
            "0000000011101110", "1100000000001100", "0111001100000000", "0000000111111111", "1111111111110110",
            "1100110011001000", "1111111110001000", "0011011011001000", "1111011100000000",
            };

    private static final int[][] ANCHORS2 = {
            {0, 2}, {0, 3}, {1, 0}, {0, 3}, {7, 0}, {0, 2}, {3, 0}, {7, 0}, {0, 11}, {2, 0}, {0, 7}, {11, 0}, {3, 0}, {8, 0}, {0, 4},
            {12, 0}, {1, 0}, {8, 0}, {0, 1}, {0, 2}, {0, 4}, {8, 0}, {1, 0}, {0, 2}, {4, 0}, {0, 1}, {4, 0}, {1, 0}, {4, 0}, {1, 0},
            };

    private static final int[][] ANCHORS3 = {
            {0, 8, 10}, {8, 0, 12}, {4, 0, 12}, {8, 0, 4}, {3, 0, 2}, {0, 1, 3}, {0, 2, 1}, {1, 9, 0}, {1, 2, 0}, {4, 0, 8}, {0, 6, 2},
            };

    private static final int[][] ANCHORS2_BC73 = {
            {0, 4}, {0, 2}, {2, 0}, {0, 7}, {8, 0}, {0, 1}, {0, 3}, {0, 1}, {2, 0}, {0, 1}, {0, 8}, {2, 0}, {0, 1}, {0, 7}, {12, 0},
            {2, 0}, {9, 0}, {0, 2}, {4, 0},
            };

    /**
     * Decodes one 16 byte block into 16 ARGB pixels (row major)
     */
    static void decodeBlock(byte[] data, int pos, int[] out)
    {
        if (!decode(new BC7Decoder.BitReader(data, pos), data[pos] & 0x7F, out))
        {
            Arrays.fill(out, 0, 16, ASTCDecoder.ERROR_COLOR);
        }
    }

    private static boolean decode(BC7Decoder.BitReader bits, int firstBits, int[] out)
    {
        int mode = HUFF_MODES[firstBits];
        if (mode == 19)
        {
            return false;
        }
        bits.read(MODE_CODE_LENGTH[mode]);

        if (mode == 8)
        {
            int r = bits.read(8);
            int g = bits.read(8);
            int b = bits.read(8);
            int a = bits.read(8);
            Arrays.fill(out, 0, 16, (a << 24) | (r << 16) | (g << 8) | b);
            return true;
        }

        // transcoding hints, not needed to decode
        bits.read(HAS_BC1_HINT0[mode] + HAS_BC1_HINT1[mode] + 8 + (HAS_ETC1_BIAS[mode] != 0 ? 5 : 0) + (HAS_ALPHA[mode] != 0 ? 8 : 0));

        int      subsets  = SUBSETS[mode];
        int[]    subsetOf = new int[16];
        int[]    anchors  = {0};
        if (subsets > 1)
        {
            String[] patterns;
            int[][]  anchorTable;
            int      patternBits;
            if (mode == 7)
            {
                patterns = PATTERNS2_BC73;
                anchorTable = ANCHORS2_BC73;
                patternBits = 5;
            } else if (subsets == 3)
            {
                patterns = PATTERNS3;
                anchorTable = ANCHORS3;
                patternBits = 4;
            } else
            {
                patterns = PATTERNS2;
                anchorTable = ANCHORS2;
                patternBits = 5;
            }
            int pattern = bits.read(patternBits);
            if (pattern >= patterns.length)
            {
                return false;
            }
            for (int i = 0; i < 16; i++)
            {
                subsetOf[i] = patterns[pattern].charAt(i) - '0';
            }
            anchors = anchorTable[pattern];
        }

        int planes = PLANES[mode];
        int ccs    = -1;
        if (planes == 2)
        {
            ccs = mode == 17 ? 3 : bits.read(2);
        }

        // endpoints: trit/quint group codes first, then each value's bits
        int   range      = ENDPOINT_RANGE[mode];
        int   comps      = COMPS[mode];
        int   valueCount = comps * 2 * subsets;
        int[] tq         = new int[valueCount];
        if (ASTCDecoder.TRITS[range] != 0)
        {
            for (int i = 0; i < valueCount; i += 5)
            {
                int groupSize = Math.min(5, valueCount - i);
                int v         = bits.read(TRIT_GROUP_BITS[groupSize]);
                for (int j = 0; j < groupSize; j++)
                {
                    tq[i + j] = v % 3;
                    v /= 3;
                }
            }
        } else if (ASTCDecoder.QUINTS[range] != 0)
        {
            for (int i = 0; i < valueCount; i += 3)
            {
                int groupSize = Math.min(3, valueCount - i);
                int v         = bits.read(QUINT_GROUP_BITS[groupSize]);
                for (int j = 0; j < groupSize; j++)
                {
                    tq[i + j] = v % 5;
                    v /= 5;
                }
            }
        }

        int   valueBits = ASTCDecoder.BITS[range];
        int[] values    = new int[valueCount];
        for (int i = 0; i < valueCount; i++)
        {
            int raw = (tq[i] << valueBits) | bits.read(valueBits);
            values[i] = ASTCDecoder.unquantizeColor(raw, range);
        }

        // weights: plain bits, anchors stored with one bit less (in both planes)
        int   weightBits = WEIGHT_BITS[mode];
        int[] weights    = new int[16 * planes];
        for (int t = 0; t < 16; t++)
        {
            boolean anchor = false;
            for (int a : anchors)
            {
                anchor |= a == t;
            }
            for (int p = 0; p < planes; p++)
            {
                int w = bits.read(anchor ? weightBits - 1 : weightBits);
                weights[t * planes + p] = ASTCDecoder.unquantizeWeight(w, WEIGHT_RANGE[mode]);
            }
        }

        // endpoints per subset as RGBA low/high, no blue contraction
        int[][] endpoints = new int[subsets][8];
        for (int s = 0; s < subsets; s++)
        {
            int   base = s * comps * 2;
            int[] e    = endpoints[s];
            for (int c = 0; c < 4; c++)
            {
                int source = switch (comps)
                {
                    case 2 -> c == 3 ? 1 : 0;
                    case 3 -> c == 3 ? -1 : c;
                    default -> c;
                };
                e[c] = source < 0 ? 255 : values[base + source * 2];
                e[4 + c] = source < 0 ? 255 : values[base + source * 2 + 1];
            }
        }

        int[] shift = {16, 8, 0, 24};
        for (int t = 0; t < 16; t++)
        {
            int[] e    = endpoints[subsetOf[t]];
            int   argb = 0;
            for (int c = 0; c < 4; c++)
            {
                int w  = weights[t * planes + (c == ccs ? 1 : 0)];
                int c0 = e[c] * 257;
                int c1 = e[4 + c] * 257;
                int v  = (c0 * (64 - w) + c1 * w + 32) >> 6;
                argb |= (v >> 8) << shift[c];
            }
            out[t] = argb;
        }
        return true;
    }
}

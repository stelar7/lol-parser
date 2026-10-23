package no.stelar7.cdragon.types.dds;

/**
 * BC7 (BPTC UNORM) block decoder, all 8 modes.
 * <p>
 * Follows the D3D11 functional spec / Khronos Data Format spec "BC7" section. Blocks with a reserved mode (first byte 0)
 * decode to transparent black, as D3D does.
 */
final class BC7Decoder
{
    private BC7Decoder()
    {
    }

    // mode: subsets, partition bits, rotation bits, index selection bits, color bits, alpha bits, endpoint pbits, shared pbits, index bits, secondary index bits
    private static final int[][] MODES = {
            {3, 4, 0, 0, 4, 0, 1, 0, 3, 0},
            {2, 6, 0, 0, 6, 0, 0, 1, 3, 0},
            {3, 6, 0, 0, 5, 0, 0, 0, 2, 0},
            {2, 6, 0, 0, 7, 0, 1, 0, 2, 0},
            {1, 0, 2, 1, 5, 6, 0, 0, 2, 3},
            {1, 0, 2, 0, 7, 8, 0, 0, 2, 2},
            {1, 0, 0, 0, 7, 7, 1, 0, 4, 0},
            {2, 6, 0, 0, 5, 5, 1, 0, 2, 0},
            };

    static final int[] WEIGHTS2 = {0, 21, 43, 64};
    static final int[] WEIGHTS3 = {0, 9, 18, 27, 37, 46, 55, 64};
    static final int[] WEIGHTS4 = {0, 4, 9, 13, 17, 21, 26, 30, 34, 38, 43, 47, 51, 55, 60, 64};

    /**
     * Partition tables for 2 and 3 subsets, shared with BC6H. Each entry packs 16 2-bit subset indices, pixel 0 in the lowest bits.
     */
    static final int[] PARTITIONS2 = packPartitions(new String[]{
            "0011001100110011", "0001000100010001", "0111011101110111", "0001001100110111",
            "0000000100010011", "0011011101111111", "0001001101111111", "0000000100110111",
            "0000000000010011", "0011011111111111", "0000000101111111", "0000000000010111",
            "0001011111111111", "0000000011111111", "0000111111111111", "0000000000001111",
            "0000100011101111", "0111000100000000", "0000000010001110", "0111001100010000",
            "0011000100000000", "0000100011001110", "0000000010001100", "0111001100110001",
            "0011000100010000", "0000100010001100", "0110011001100110", "0011011001101100",
            "0001011111101000", "0000111111110000", "0111000110001110", "0011100110011100",
            "0101010101010101", "0000111100001111", "0101101001011010", "0011001111001100",
            "0011110000111100", "0101010110101010", "0110100101101001", "0101101010100101",
            "0111001111001110", "0001001111001000", "0011001001001100", "0011101111011100",
            "0110100110010110", "0011110011000011", "0110011010011001", "0000011001100000",
            "0100111001000000", "0010011100100000", "0000001001110010", "0000010011100100",
            "0110110010010011", "0011011011001001", "0110001110011100", "0011100111000110",
            "0110110011001001", "0110001100111001", "0111111010000001", "0001100011100111",
            "0000111100110011", "0011001111110000", "0010001011101110", "0100010001110111",
            });

    static final int[] PARTITIONS3 = packPartitions(new String[]{
            "0011001102212222", "0001001122112221", "0000200122112211", "0222002200110111",
            "0000000011221122", "0011001100220022", "0022002211111111", "0011001122112211",
            "0000000011112222", "0000111111112222", "0000111122222222", "0012001200120012",
            "0112011201120112", "0122012201220122", "0011011211221222", "0011200122002220",
            "0001001101121122", "0111001120012200", "0000112211221122", "0022002200221111",
            "0111011102220222", "0001000122212221", "0000001101220122", "0000110022102210",
            "0122012200110000", "0012001211222222", "0110122112210110", "0000011012211221",
            "0022110211020022", "0110011020022222", "0011012201220011", "0000200022112221",
            "0000000211221222", "0222002200120011", "0011001200220222", "0120012001200120",
            "0000111122220000", "0120120120120120", "0120201212010120", "0011220011220011",
            "0011112222000011", "0101010122222222", "0000000021212121", "0022112200221122",
            "0022001100220011", "0220122102201221", "0101222222220101", "0000212121212121",
            "0101010101012222", "0222011102220111", "0002111200021112", "0000211221122112",
            "0222011101110222", "0002111211120002", "0110011001102222", "0000000021122112",
            "0110011022222222", "0022001100110022", "0022112211220022", "0000000000002112",
            "0002000100020001", "0222122202221222", "0101222222222222", "0111201122012220",
            });

    static final int[] ANCHOR2 = {
            15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
            15, 2, 8, 2, 2, 8, 8, 15, 2, 8, 2, 2, 8, 8, 2, 2,
            15, 15, 6, 8, 2, 8, 15, 15, 2, 8, 2, 2, 2, 15, 15, 6,
            6, 2, 6, 8, 15, 15, 2, 2, 15, 15, 15, 15, 15, 2, 2, 15,
            };

    static final int[] ANCHOR3_2 = {
            3, 3, 15, 15, 8, 3, 15, 15, 8, 8, 6, 6, 6, 5, 3, 3,
            3, 3, 8, 15, 3, 3, 6, 10, 5, 8, 8, 6, 8, 5, 15, 15,
            8, 15, 3, 5, 6, 10, 8, 15, 15, 3, 15, 5, 15, 15, 15, 15,
            3, 15, 5, 5, 5, 8, 5, 10, 5, 10, 8, 13, 15, 12, 3, 3,
            };

    static final int[] ANCHOR3_3 = {
            15, 8, 8, 3, 15, 15, 3, 8, 15, 15, 15, 15, 15, 15, 15, 8,
            15, 8, 15, 3, 15, 8, 15, 8, 3, 15, 6, 10, 15, 15, 10, 8,
            15, 3, 15, 10, 10, 8, 9, 10, 6, 15, 8, 15, 3, 6, 6, 8,
            15, 3, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 3, 15, 15, 8,
            };

    private static int[] packPartitions(String[] rows)
    {
        int[] out = new int[rows.length];
        for (int i = 0; i < rows.length; i++)
        {
            int v = 0;
            for (int p = 0; p < 16; p++)
            {
                v |= (rows[i].charAt(p) - '0') << (2 * p);
            }
            out[i] = v;
        }
        return out;
    }

    static int subsetOf(int subsets, int partition, int pixel)
    {
        return switch (subsets)
        {
            case 1 -> 0;
            case 2 -> (PARTITIONS2[partition] >>> (2 * pixel)) & 3;
            default -> (PARTITIONS3[partition] >>> (2 * pixel)) & 3;
        };
    }

    /**
     * Is this pixel the anchor (first index, stored with one bit less) of its subset
     */
    static boolean isAnchor(int subsets, int partition, int pixel)
    {
        if (pixel == 0)
        {
            return true;
        }
        return switch (subsets)
        {
            case 2 -> ANCHOR2[partition] == pixel;
            case 3 -> ANCHOR3_2[partition] == pixel || ANCHOR3_3[partition] == pixel;
            default -> false;
        };
    }

    static int interpolate(int e0, int e1, int weight)
    {
        return ((64 - weight) * e0 + weight * e1 + 32) >> 6;
    }

    /**
     * Decodes one 16 byte block into 16 ARGB pixels (row major)
     */
    static void decodeBlock(byte[] data, int pos, int[] out)
    {
        BitReader bits = new BitReader(data, pos);

        int mode = 0;
        while (mode < 8 && bits.read(1) == 0)
        {
            mode++;
        }
        if (mode == 8)
        {
            java.util.Arrays.fill(out, 0, 16, 0);
            return;
        }

        int[] m             = MODES[mode];
        int   subsets       = m[0];
        int   partition     = bits.read(m[1]);
        int   rotation      = bits.read(m[2]);
        int   indexSelect   = bits.read(m[3]);
        int   colorBits     = m[4];
        int   alphaBits     = m[5];
        int   endpointPBits = m[6];
        int   sharedPBits   = m[7];
        int   indexBits     = m[8];
        int   indexBits2    = m[9];

        int   endpoints = subsets * 2;
        int[] e         = new int[endpoints * 4]; // [endpoint][rgba]
        for (int c = 0; c < 3; c++)
        {
            for (int i = 0; i < endpoints; i++)
            {
                e[i * 4 + c] = bits.read(colorBits);
            }
        }
        if (alphaBits > 0)
        {
            for (int i = 0; i < endpoints; i++)
            {
                e[i * 4 + 3] = bits.read(alphaBits);
            }
        }

        int[] pbits = new int[endpoints];
        if (endpointPBits != 0)
        {
            for (int i = 0; i < endpoints; i++)
            {
                pbits[i] = bits.read(1);
            }
        } else if (sharedPBits != 0)
        {
            for (int s = 0; s < subsets; s++)
            {
                int p = bits.read(1);
                pbits[s * 2] = p;
                pbits[s * 2 + 1] = p;
            }
        }
        boolean hasPBit = endpointPBits != 0 || sharedPBits != 0;

        for (int i = 0; i < endpoints; i++)
        {
            for (int c = 0; c < 4; c++)
            {
                int n = c == 3 ? alphaBits : colorBits;
                if (n == 0)
                {
                    e[i * 4 + c] = 255;
                    continue;
                }
                int v = e[i * 4 + c];
                if (hasPBit)
                {
                    v = (v << 1) | pbits[i];
                    n++;
                }
                v <<= 8 - n;
                e[i * 4 + c] = v | (v >>> n);
            }
        }

        int[] index  = new int[16];
        int[] index2 = new int[16];
        for (int p = 0; p < 16; p++)
        {
            index[p] = bits.read(isAnchor(subsets, partition, p) ? indexBits - 1 : indexBits);
        }
        if (indexBits2 > 0)
        {
            for (int p = 0; p < 16; p++)
            {
                index2[p] = bits.read(p == 0 ? indexBits2 - 1 : indexBits2);
            }
        }

        for (int p = 0; p < 16; p++)
        {
            int s  = subsetOf(subsets, partition, p);
            int e0 = s * 2 * 4;
            int e1 = e0 + 4;

            int colorWeight;
            int alphaWeight;
            if (indexBits2 == 0)
            {
                colorWeight = weight(indexBits, index[p]);
                alphaWeight = colorWeight;
            } else if (indexSelect == 0)
            {
                colorWeight = weight(indexBits, index[p]);
                alphaWeight = weight(indexBits2, index2[p]);
            } else
            {
                colorWeight = weight(indexBits2, index2[p]);
                alphaWeight = weight(indexBits, index[p]);
            }

            int r = interpolate(e[e0], e[e1], colorWeight);
            int g = interpolate(e[e0 + 1], e[e1 + 1], colorWeight);
            int b = interpolate(e[e0 + 2], e[e1 + 2], colorWeight);
            int a = interpolate(e[e0 + 3], e[e1 + 3], alphaWeight);

            switch (rotation)
            {
                case 1 ->
                {
                    int t = a;
                    a = r;
                    r = t;
                }
                case 2 ->
                {
                    int t = a;
                    a = g;
                    g = t;
                }
                case 3 ->
                {
                    int t = a;
                    a = b;
                    b = t;
                }
                default ->
                {
                }
            }
            out[p] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    private static int weight(int bits, int index)
    {
        return switch (bits)
        {
            case 2 -> WEIGHTS2[index];
            case 3 -> WEIGHTS3[index];
            default -> WEIGHTS4[index];
        };
    }

    /**
     * Reads bits LSB first from a 128 bit little endian block
     */
    static final class BitReader
    {
        private final long lo;
        private final long hi;
        private       int  pos;

        BitReader(byte[] data, int offset)
        {
            long l = 0;
            long h = 0;
            for (int i = 7; i >= 0; i--)
            {
                l = (l << 8) | (data[offset + i] & 0xFF);
                h = (h << 8) | (data[offset + 8 + i] & 0xFF);
            }
            this.lo = l;
            this.hi = h;
        }

        /**
         * Reads up to 32 bits, bits past the end of the block read as 0
         */
        int read(int count)
        {
            if (count == 0)
            {
                return 0;
            }

            long window;
            if (pos >= 128)
            {
                window = 0;
            } else if (pos >= 64)
            {
                window = hi >>> (pos - 64);
            } else if (pos == 0)
            {
                window = lo;
            } else
            {
                window = (lo >>> pos) | (hi << (64 - pos));
            }

            pos += count;
            return (int) (window & ((1L << count) - 1));
        }

        int position()
        {
            return pos;
        }
    }
}

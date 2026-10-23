package no.stelar7.cdragon.types.dds;

/**
 * BC6H (BPTC float) block decoder for the signed and unsigned variants, following the D3D11 functional spec.
 * <p>
 * The block decodes to 16 bit half floats ({@link #decodeBlockHalf}). For the 8 bit {@link java.awt.image.BufferedImage}
 * output ({@link #decodeBlock}) each channel is converted to float, <b>clamped to [0, 1]</b> (negative values and HDR
 * values above 1 are lost, no tone mapping is applied) and scaled to 0..255 with rounding. Alpha is always 255.
 * Reserved modes decode to black, as D3D does.
 */
final class BC6HDecoder
{
    private BC6HDecoder()
    {
    }

    // endpoint bits, delta bits r/g/b, transformed, regions
    private static final int[][] MODE_INFO = {
            {10, 5, 5, 5, 1, 2},
            {7, 6, 6, 6, 1, 2},
            {11, 5, 4, 4, 1, 2},
            {11, 4, 5, 4, 1, 2},
            {11, 4, 4, 5, 1, 2},
            {9, 5, 5, 5, 1, 2},
            {8, 6, 5, 5, 1, 2},
            {8, 5, 6, 5, 1, 2},
            {8, 5, 5, 6, 1, 2},
            {6, 6, 6, 6, 0, 2},
            {10, 10, 10, 10, 0, 1},
            {11, 9, 9, 9, 1, 1},
            {12, 8, 8, 8, 1, 1},
            {16, 4, 4, 4, 1, 1},
            };

    // 5 bit mode values (2 bit for the first two) in the same order as MODE_INFO
    private static final int[] MODE_VALUES = {0b00, 0b01, 0b00010, 0b00110, 0b01010, 0b01110, 0b10010, 0b10110, 0b11010, 0b11110, 0b00011, 0b00111, 0b01011, 0b01111};

    /**
     * Bit layout after the mode bits, per the D3D11 spec tables. "rw0-9" reads bits 0..9 of red endpoint w in order,
     * "rw15-10" reads them in reverse (first bit read is bit 15).
     */
    private static final String[] LAYOUTS = {
            "gy4 by4 bz4 rw0-9 gw0-9 bw0-9 rx0-4 gz4 gy0-3 gx0-4 bz0 gz0-3 bx0-4 bz1 by0-3 ry0-4 bz2 rz0-4 bz3",
            "gy5 gz4 gz5 rw0-6 bz0 bz1 by4 gw0-6 by5 bz2 gy4 bw0-6 bz3 bz5 bz4 rx0-5 gy0-3 gx0-5 gz0-3 bx0-5 by0-3 ry0-5 rz0-5",
            "rw0-9 gw0-9 bw0-9 rx0-4 rw10 gy0-3 gx0-3 gw10 bz0 gz0-3 bx0-3 bw10 bz1 by0-3 ry0-4 bz2 rz0-4 bz3",
            "rw0-9 gw0-9 bw0-9 rx0-3 rw10 gz4 gy0-3 gx0-4 gw10 gz0-3 bx0-3 bw10 bz1 by0-3 ry0-3 bz0 bz2 rz0-3 gy4 bz3",
            "rw0-9 gw0-9 bw0-9 rx0-3 rw10 by4 gy0-3 gx0-3 gw10 bz0 gz0-3 bx0-4 bw10 by0-3 ry0-3 bz1 bz2 rz0-3 bz4 bz3",
            "rw0-8 by4 gw0-8 gy4 bw0-8 bz4 rx0-4 gz4 gy0-3 gx0-4 bz0 gz0-3 bx0-4 bz1 by0-3 ry0-4 bz2 rz0-4 bz3",
            "rw0-7 gz4 by4 gw0-7 bz2 gy4 bw0-7 bz3 bz4 rx0-5 gy0-3 gx0-4 bz0 gz0-3 bx0-4 bz1 by0-3 ry0-5 rz0-5",
            "rw0-7 bz0 by4 gw0-7 gy5 gy4 bw0-7 gz5 bz4 rx0-4 gz4 gy0-3 gx0-5 gz0-3 bx0-4 bz1 by0-3 ry0-4 bz2 rz0-4 bz3",
            "rw0-7 bz1 by4 gw0-7 by5 gy4 bw0-7 bz5 bz4 rx0-4 gz4 gy0-3 gx0-4 bz0 gz0-3 bx0-5 by0-3 ry0-4 bz2 rz0-4 bz3",
            "rw0-5 gz4 bz0 bz1 by4 gw0-5 gy5 by5 bz2 gy4 bw0-5 gz5 bz3 bz5 bz4 rx0-5 gy0-3 gx0-5 gz0-3 bx0-5 by0-3 ry0-5 rz0-5",
            "rw0-9 gw0-9 bw0-9 rx0-9 gx0-9 bx0-9",
            "rw0-9 gw0-9 bw0-9 rx0-8 rw10 gx0-8 gw10 bx0-8 bw10",
            "rw0-9 gw0-9 bw0-9 rx0-7 rw11-10 gx0-7 gw11-10 bx0-7 bw11-10",
            "rw0-9 gw0-9 bw0-9 rx0-3 rw15-10 gx0-3 gw15-10 bx0-3 bw15-10",
            };

    // compiled layouts: per mode a list of (endpointIndex * 3 + channel, bit) pairs, endpoint w=0 x=1 y=2 z=3, channel r=0 g=1 b=2
    private static final int[][] FIELDS = new int[LAYOUTS.length][];

    static
    {
        for (int m = 0; m < LAYOUTS.length; m++)
        {
            java.util.List<Integer> list = new java.util.ArrayList<>();
            for (String token : LAYOUTS[m].split(" "))
            {
                int    channel  = "rgb".indexOf(token.charAt(0));
                int    endpoint = "wxyz".indexOf(token.charAt(1));
                String range    = token.substring(2);
                int    from;
                int    to;
                int    dash     = range.indexOf('-');
                if (dash < 0)
                {
                    from = to = Integer.parseInt(range);
                } else
                {
                    from = Integer.parseInt(range.substring(0, dash));
                    to = Integer.parseInt(range.substring(dash + 1));
                }
                int step = to >= from ? 1 : -1;
                for (int bit = from; ; bit += step)
                {
                    list.add(endpoint * 3 + channel);
                    list.add(bit);
                    if (bit == to)
                    {
                        break;
                    }
                }
            }
            FIELDS[m] = list.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    /**
     * Decodes one 16 byte block to 16 RGB half float triples (48 values, row major)
     */
    static void decodeBlockHalf(byte[] data, int pos, boolean signed, int[] out)
    {
        BC7Decoder.BitReader bits = new BC7Decoder.BitReader(data, pos);

        int modeValue = bits.read(2);
        if (modeValue >= 2)
        {
            modeValue |= bits.read(3) << 2;
        }

        int mode = -1;
        for (int i = 0; i < MODE_VALUES.length; i++)
        {
            if (MODE_VALUES[i] == modeValue)
            {
                mode = i;
                break;
            }
        }
        if (mode < 0)
        {
            java.util.Arrays.fill(out, 0, 48, 0);
            return;
        }

        int[] e      = new int[12]; // [endpoint][rgb]
        int[] fields = FIELDS[mode];
        for (int i = 0; i < fields.length; i += 2)
        {
            e[fields[i]] |= bits.read(1) << fields[i + 1];
        }

        int[]   info        = MODE_INFO[mode];
        int     epBits      = info[0];
        boolean transformed = info[4] != 0;
        int     regions     = info[5];
        int     partition   = regions == 2 ? bits.read(5) : 0;
        int     endpoints   = regions * 2;

        for (int c = 0; c < 3; c++)
        {
            if (signed)
            {
                e[c] = signExtend(e[c], epBits);
            }
            for (int i = 1; i < endpoints; i++)
            {
                int idx = i * 3 + c;
                if (transformed || signed)
                {
                    e[idx] = signExtend(e[idx], info[1 + c]);
                }
                if (transformed)
                {
                    e[idx] = (e[0 * 3 + c] + e[idx]) & ((1 << epBits) - 1);                    if (signed)
                    {
                        e[idx] = signExtend(e[idx], epBits);
                    }
                }
            }
        }

        for (int i = 0; i < endpoints * 3; i++)
        {
            e[i] = unquantize(e[i], epBits, signed);
        }

        int indexBits = regions == 2 ? 3 : 4;
        for (int p = 0; p < 16; p++)
        {
            boolean anchor = p == 0 || (regions == 2 && BC7Decoder.ANCHOR2[partition] == p);
            int     index  = bits.read(anchor ? indexBits - 1 : indexBits);
            int     weight = regions == 2 ? BC7Decoder.WEIGHTS3[index] : BC7Decoder.WEIGHTS4[index];
            int     s      = regions == 2 ? BC7Decoder.subsetOf(2, partition, p) : 0;
            for (int c = 0; c < 3; c++)
            {
                int a = e[(s * 2) * 3 + c];
                int b = e[(s * 2 + 1) * 3 + c];
                int v = ((64 - weight) * a + weight * b + 32) >> 6;
                out[p * 3 + c] = finishUnquantize(v, signed);
            }
        }
    }

    /**
     * Decodes one 16 byte block into 16 ARGB pixels, clamping each channel to [0, 1]
     */
    static void decodeBlock(byte[] data, int pos, boolean signed, int[] out)
    {
        int[] half = new int[48];
        decodeBlockHalf(data, pos, signed, half);
        for (int p = 0; p < 16; p++)
        {
            int r = HalfFloat.toUnorm8(half[p * 3]);
            int g = HalfFloat.toUnorm8(half[p * 3 + 1]);
            int b = HalfFloat.toUnorm8(half[p * 3 + 2]);
            out[p] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    private static int signExtend(int value, int bits)
    {
        int shift = 32 - bits;
        return (value << shift) >> shift;
    }

    private static int unquantize(int value, int bits, boolean signed)
    {
        if (!signed)
        {
            if (bits >= 15)
            {
                return value;
            }
            if (value == 0)
            {
                return 0;
            }
            if (value == (1 << bits) - 1)
            {
                return 0xFFFF;
            }
            return ((value << 16) + 0x8000) >> bits;
        }

        if (bits >= 16)
        {
            return value;
        }
        boolean negative = value < 0;
        if (negative)
        {
            value = -value;
        }
        int result;
        if (value == 0)
        {
            result = 0;
        } else if (value >= (1 << (bits - 1)) - 1)
        {
            result = 0x7FFF;
        } else
        {
            result = ((value << 15) + 0x4000) >> (bits - 1);
        }
        return negative ? -result : result;
    }

    private static int finishUnquantize(int value, boolean signed)
    {
        if (!signed)
        {
            return (value * 31) >> 6;
        }
        if (value < 0)
        {
            return 0x8000 | (((-value) * 31) >> 5);
        }
        return (value * 31) >> 5;
    }
}

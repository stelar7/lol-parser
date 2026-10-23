package no.stelar7.cdragon.types.dds;

import java.util.Arrays;

/**
 * ASTC LDR 2D block decoder for every block size, following the Khronos Data Format spec "ASTC" section
 * (with the ARM astc-encoder decompressor as the reference for the details).
 * <p>
 * Output follows the spec's decode_unorm8 mode for the linear LDR profile: endpoints are expanded to 16 bits as
 * {@code (e << 8) | e}, and the top 8 bits of the interpolated value are kept (bit exact with astcenc using
 * ASTCENC_FLG_USE_DECODE_UNORM8). sRGB textures are decoded the same way (no sRGB-to-linear conversion is wanted for PNG
 * output); the spec's sRGB expansion {@code (e << 8) | 0x80} can differ by one step. Blocks that are invalid, or that use
 * HDR endpoint modes / HDR void extents, decode to the error color (opaque magenta), as the spec requires for the LDR profile.
 */
final class ASTCDecoder
{
    private ASTCDecoder()
    {
    }

    static final int ERROR_COLOR = 0xFFFF00FF;

    // per quantization level index (also the UASTC BISE range): number of levels, trits, quints, bits
    private static final int[] LEVELS = {2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24, 32, 40, 48, 64, 80, 96, 128, 160, 192, 256};
    static final         int[] TRITS  = {0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0};
    static final         int[] QUINTS = {0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0};
    static final         int[] BITS   = {1, 0, 2, 0, 1, 3, 1, 2, 4, 2, 3, 5, 3, 4, 6, 4, 5, 7, 5, 6, 8};

    private static final int QUANT_6 = 4;

    /**
     * The number of bits used to store count values of quantization level quant with bounded integer sequence encoding
     */
    static int iseBits(int count, int quant)
    {
        int bits = count * BITS[quant];
        if (TRITS[quant] != 0)
        {
            bits += (count * 8 + 4) / 5;
        }
        if (QUINTS[quant] != 0)
        {
            bits += (count * 7 + 2) / 3;
        }
        return bits;
    }

    /**
     * A 128 bit block, bits addressed LSB first
     */
    private record Bits(long lo, long hi)
    {
        int get(int start, int count)
        {
            if (count == 0)
            {
                return 0;
            }
            long value;
            if (start >= 64)
            {
                value = hi >>> (start - 64);
            } else if (start + count <= 64)
            {
                value = lo >>> start;
            } else
            {
                value = (lo >>> start) | (hi << (64 - start));
            }
            return (int) (value & ((1L << count) - 1));
        }

        Bits reversed()
        {
            return new Bits(Long.reverse(hi), Long.reverse(lo));
        }
    }

    /**
     * Decodes one 16 byte block into blockWidth * blockHeight ARGB pixels (row major)
     */
    static void decodeBlock(byte[] data, int pos, int blockWidth, int blockHeight, int[] out)
    {
        long lo = 0;
        long hi = 0;
        for (int i = 7; i >= 0; i--)
        {
            lo = (lo << 8) | (data[pos + i] & 0xFF);
            hi = (hi << 8) | (data[pos + 8 + i] & 0xFF);
        }
        int texels = blockWidth * blockHeight;
        if (!decode(new Bits(lo, hi), blockWidth, blockHeight, out))
        {
            Arrays.fill(out, 0, texels, ERROR_COLOR);
        }
    }

    private static boolean decode(Bits block, int bw, int bh, int[] out)
    {
        int texels = bw * bh;
        int mode   = block.get(0, 11);

        if ((mode & 0x1FF) == 0x1FC)
        {
            return decodeVoidExtent(block, texels, out);
        }

        // block mode
        int     r;
        int     weightW;
        int     weightH;
        boolean highPrecision = (mode & 0x200) != 0;
        boolean dualPlane     = (mode & 0x400) != 0;
        int     a             = (mode >> 5) & 3;
        if ((mode & 3) != 0)
        {
            r = ((mode >> 4) & 1) | ((mode & 3) << 1);
            int b = (mode >> 7) & 3;
            switch ((mode >> 2) & 3)
            {
                case 0 ->
                {
                    weightW = b + 4;
                    weightH = a + 2;
                }
                case 1 ->
                {
                    weightW = b + 8;
                    weightH = a + 2;
                }
                case 2 ->
                {
                    weightW = a + 2;
                    weightH = b + 8;
                }
                default ->
                {
                    if ((mode & 0x100) == 0)
                    {
                        weightW = a + 2;
                        weightH = (b & 1) + 6;
                    } else
                    {
                        weightW = (b & 1) + 2;
                        weightH = a + 2;
                    }
                }
            }
        } else
        {
            r = ((mode >> 4) & 1) | (((mode >> 2) & 3) << 1);
            if ((mode & 0xF) == 0)
            {
                return false;
            }
            int b = (mode >> 9) & 3;
            switch ((mode >> 7) & 3)
            {
                case 0 ->
                {
                    weightW = 12;
                    weightH = a + 2;
                }
                case 1 ->
                {
                    weightW = a + 2;
                    weightH = 12;
                }
                case 2 ->
                {
                    weightW = a + 6;
                    weightH = b + 6;
                    highPrecision = false;
                    dualPlane = false;
                }
                default ->
                {
                    switch (a)
                    {
                        case 0 ->
                        {
                            weightW = 6;
                            weightH = 10;
                        }
                        case 1 ->
                        {
                            weightW = 10;
                            weightH = 6;
                        }
                        default ->
                        {
                            return false;
                        }
                    }
                }
            }
        }

        int weightQuant = (r - 2) + (highPrecision ? 6 : 0);
        int planes      = dualPlane ? 2 : 1;
        int weightCount = weightW * weightH * planes;
        if (weightW > bw || weightH > bh || weightCount > 64)
        {
            return false;
        }
        int weightBits = iseBits(weightCount, weightQuant);
        if (weightBits < 24 || weightBits > 96)
        {
            return false;
        }

        int partitions = block.get(11, 2) + 1;
        if (dualPlane && partitions == 4)
        {
            return false;
        }

        int[] cem            = new int[partitions];
        int   partitionIndex = 0;
        int   colorStart;
        int   extraCemBits   = 0;
        if (partitions == 1)
        {
            cem[0] = block.get(13, 4);
            colorStart = 17;
        } else
        {
            partitionIndex = block.get(13, 10);
            int cemField = block.get(23, 6);
            colorStart = 29;
            if ((cemField & 3) == 0)
            {
                Arrays.fill(cem, cemField >> 2);
            } else
            {
                extraCemBits = 3 * partitions - 4;
                int extra    = block.get(128 - weightBits - extraCemBits, extraCemBits);
                int combined = (cemField >> 2) | (extra << 4);
                int base     = (cemField & 3) - 1;
                for (int i = 0; i < partitions; i++)
                {
                    int c = (combined >> i) & 1;
                    int m = (combined >> (partitions + 2 * i)) & 3;
                    cem[i] = ((base + c) << 2) | m;
                }
            }
        }

        int colorEnd = 128 - weightBits - extraCemBits - (dualPlane ? 2 : 0);
        int ccs      = dualPlane ? block.get(colorEnd, 2) : -1;

        int valueCount = 0;
        for (int c : cem)
        {
            valueCount += ((c >> 2) + 1) * 2;
        }
        if (valueCount > 18)
        {
            return false;
        }

        int available  = colorEnd - colorStart;
        int colorQuant = -1;
        for (int q = LEVELS.length - 1; q >= 0; q--)
        {
            if (iseBits(valueCount, q) <= available)
            {
                colorQuant = q;
                break;
            }
        }
        if (colorQuant < QUANT_6)
        {
            return false;
        }

        int[] colorValues = decodeIse(block, colorStart, valueCount, colorQuant);
        for (int i = 0; i < valueCount; i++)
        {
            colorValues[i] = unquantizeColor(colorValues[i], colorQuant);
        }

        int[] weightValues = decodeIse(block.reversed(), 0, weightCount, weightQuant);
        for (int i = 0; i < weightCount; i++)
        {
            weightValues[i] = unquantizeWeight(weightValues[i], weightQuant);
        }

        // endpoints: [partition][endpoint][rgba]
        int[][]   endpoints = new int[partitions][8];
        boolean[] hdr       = new boolean[partitions];
        int       vi        = 0;
        for (int p = 0; p < partitions; p++)
        {
            int count = ((cem[p] >> 2) + 1) * 2;
            hdr[p] = !decodeEndpoints(cem[p], Arrays.copyOfRange(colorValues, vi, vi + count), endpoints[p]);
            vi += count;
        }

        // infill the weights
        int[][] texelWeights = new int[planes][texels];
        int     ds           = (1024 + bw / 2) / (bw - 1);
        int     dt           = (1024 + bh / 2) / (bh - 1);
        for (int t = 0; t < bh; t++)
        {
            for (int s = 0; s < bw; s++)
            {
                int gs = (ds * s * (weightW - 1) + 32) >> 6;
                int gt = (dt * t * (weightH - 1) + 32) >> 6;
                int js = gs >> 4;
                int fs = gs & 0xF;
                int jt = gt >> 4;
                int ft = gt & 0xF;

                int w11 = (fs * ft + 8) >> 4;
                int w10 = ft - w11;
                int w01 = fs - w11;
                int w00 = 16 - fs - ft + w11;

                int v0 = js + jt * weightW;
                for (int plane = 0; plane < planes; plane++)
                {
                    int p00 = weightAt(weightValues, v0, planes, plane, weightCount);
                    int p01 = weightAt(weightValues, v0 + 1, planes, plane, weightCount);
                    int p10 = weightAt(weightValues, v0 + weightW, planes, plane, weightCount);
                    int p11 = weightAt(weightValues, v0 + weightW + 1, planes, plane, weightCount);
                    texelWeights[plane][t * bw + s] = (p00 * w00 + p01 * w01 + p10 * w10 + p11 * w11 + 8) >> 4;
                }
            }
        }

        boolean small = texels < 31;
        for (int t = 0; t < bh; t++)
        {
            for (int s = 0; s < bw; s++)
            {
                int i = t * bw + s;
                int p = partitions == 1 ? 0 : selectPartition(partitionIndex, s, t, 0, partitions, small);
                if (hdr[p])
                {
                    out[i] = ERROR_COLOR;
                    continue;
                }

                int[] e    = endpoints[p];
                int   argb = 0;
                for (int c = 0; c < 4; c++)
                {
                    int w  = (c == ccs) ? texelWeights[1][i] : texelWeights[0][i];
                    int c0 = e[c] * 257;
                    int c1 = e[4 + c] * 257;
                    int v  = (c0 * (64 - w) + c1 * w + 32) >> 6;
                    argb |= unorm16To8(v) << CHANNEL_SHIFT[c];
                }
                out[i] = argb;
            }
        }
        return true;
    }

    // r, g, b, a position in an ARGB int
    private static final int[] CHANNEL_SHIFT = {16, 8, 0, 24};

    // bits of the packed trit / quint block that follow each value
    private static final int[] TRIT_BIT_COUNT  = {2, 2, 1, 2, 1};
    private static final int[] QUINT_BIT_COUNT = {3, 2, 2};

    // every packed trit block (8 bits) and quint block (7 bits), decoded once
    private static final int[][] TRITS_TABLE  = new int[256][];
    private static final int[][] QUINTS_TABLE = new int[128][];

    static
    {
        for (int i = 0; i < TRITS_TABLE.length; i++)
        {
            TRITS_TABLE[i] = decodeTrits(i);
        }
        for (int i = 0; i < QUINTS_TABLE.length; i++)
        {
            QUINTS_TABLE[i] = decodeQuints(i);
        }
    }

    private static int weightAt(int[] weights, int index, int planes, int plane, int weightCount)
    {
        int i = index * planes + plane;
        return i < weightCount ? weights[i] : 0;
    }

    /**
     * decode_unorm8: the top 8 bits of the 16 bit result
     */
    static int unorm16To8(int v)
    {
        return v >> 8;
    }

    private static boolean decodeVoidExtent(Bits block, int texels, int[] out)
    {
        if ((block.get(9, 1)) != 0)
        {
            // HDR void extent, not representable in the LDR profile
            return false;
        }

        int minS = block.get(12, 13);
        int maxS = block.get(25, 13);
        int minT = block.get(38, 13);
        int maxT = block.get(51, 13);
        boolean allOnes = minS == 0x1FFF && maxS == 0x1FFF && minT == 0x1FFF && maxT == 0x1FFF;
        if (!allOnes && (minS >= maxS || minT >= maxT))
        {
            return false;
        }

        int r    = unorm16To8(block.get(64, 16));
        int g    = unorm16To8(block.get(80, 16));
        int b    = unorm16To8(block.get(96, 16));
        int a    = unorm16To8(block.get(112, 16));
        int argb = (a << 24) | (r << 16) | (g << 8) | b;
        Arrays.fill(out, 0, texels, argb);
        return true;
    }

    /**
     * Bounded integer sequence decoding. Returns raw values: (trit or quint) * 2^bits + bits.
     */
    static int[] decodeIse(Bits block, int start, int count, int quant)
    {
        int   bits   = BITS[quant];
        int   end    = start + iseBits(count, quant);
        int[] values = new int[count];
        int   pos    = start;

        if (TRITS[quant] != 0)
        {
            int[] m = new int[5];
            for (int i = 0; i < count; i += 5)
            {
                int t    = 0;
                int tPos = 0;
                for (int j = 0; j < 5; j++)
                {
                    m[j] = read(block, pos, bits, end);
                    pos += bits;
                    t |= read(block, pos, TRIT_BIT_COUNT[j], end) << tPos;
                    pos += TRIT_BIT_COUNT[j];
                    tPos += TRIT_BIT_COUNT[j];
                }
                int[] trits = TRITS_TABLE[t];
                for (int j = 0; j < 5 && i + j < count; j++)
                {
                    values[i + j] = (trits[j] << bits) | m[j];
                }
            }
        } else if (QUINTS[quant] != 0)
        {
            int[] m = new int[3];
            for (int i = 0; i < count; i += 3)
            {
                int q    = 0;
                int qPos = 0;
                for (int j = 0; j < 3; j++)
                {
                    m[j] = read(block, pos, bits, end);
                    pos += bits;
                    q |= read(block, pos, QUINT_BIT_COUNT[j], end) << qPos;
                    pos += QUINT_BIT_COUNT[j];
                    qPos += QUINT_BIT_COUNT[j];
                }
                int[] quints = QUINTS_TABLE[q];
                for (int j = 0; j < 3 && i + j < count; j++)
                {
                    values[i + j] = (quints[j] << bits) | m[j];
                }
            }
        } else
        {
            for (int i = 0; i < count; i++)
            {
                values[i] = block.get(pos, bits);
                pos += bits;
            }
        }
        return values;
    }

    /**
     * Reads bits that may extend past the end of the sequence, those read as 0
     */
    private static int read(Bits block, int pos, int count, int end)
    {
        if (pos >= end || count == 0)
        {
            return 0;
        }
        int available = Math.min(count, end - pos);
        return block.get(pos, available);
    }

    private static int bit(int v, int n)
    {
        return (v >> n) & 1;
    }

    private static int[] decodeTrits(int t)
    {
        int c;
        int t3;
        int t4;
        if (((t >> 2) & 7) == 7)
        {
            c = (((t >> 5) & 7) << 2) | (t & 3);
            t4 = 2;
            t3 = 2;
        } else
        {
            c = t & 0x1F;
            if (((t >> 5) & 3) == 3)
            {
                t4 = 2;
                t3 = bit(t, 7);
            } else
            {
                t4 = bit(t, 7);
                t3 = (t >> 5) & 3;
            }
        }

        int t0;
        int t1;
        int t2;
        if ((c & 3) == 3)
        {
            t2 = 2;
            t1 = bit(c, 4);
            t0 = (bit(c, 3) << 1) | (bit(c, 2) & ~bit(c, 3) & 1);
        } else if (((c >> 2) & 3) == 3)
        {
            t2 = 2;
            t1 = 2;
            t0 = c & 3;
        } else
        {
            t2 = bit(c, 4);
            t1 = (c >> 2) & 3;
            t0 = (bit(c, 1) << 1) | (bit(c, 0) & ~bit(c, 1) & 1);
        }
        return new int[]{t0, t1, t2, t3, t4};
    }

    private static int[] decodeQuints(int q)
    {
        int q0;
        int q1;
        int q2;
        if (((q >> 1) & 3) == 3 && ((q >> 5) & 3) == 0)
        {
            q2 = (bit(q, 0) << 2) | ((bit(q, 4) & ~bit(q, 0) & 1) << 1) | (bit(q, 3) & ~bit(q, 0) & 1);
            q1 = 4;
            q0 = 4;
        } else
        {
            int c;
            if (((q >> 1) & 3) == 3)
            {
                q2 = 4;
                c = (((q >> 3) & 3) << 3) | ((~(q >> 5) & 3) << 1) | bit(q, 0);
            } else
            {
                q2 = (q >> 5) & 3;
                c = q & 0x1F;
            }

            if ((c & 7) == 5)
            {
                q1 = 4;
                q0 = (c >> 3) & 3;
            } else
            {
                q1 = (c >> 3) & 3;
                q0 = c & 7;
            }
        }
        return new int[]{q0, q1, q2};
    }

    static int unquantizeColor(int value, int quant)
    {
        int bits = BITS[quant];
        if (TRITS[quant] == 0 && QUINTS[quant] == 0)
        {
            // bit replication
            int v      = value << (8 - bits);
            int filled = bits;
            while (filled < 8)
            {
                v |= v >> filled;
                filled *= 2;
            }
            return v & 0xFF;
        }

        int d   = value >> bits;
        int low = value & ((1 << bits) - 1);
        int a   = (low & 1) != 0 ? 0x1FF : 0;
        int x   = low >> 1; // bits b, c, d, ... (b in the lowest position)
        int b;
        int c;
        if (TRITS[quant] != 0)
        {
            switch (bits)
            {
                case 1 ->
                {
                    b = 0;
                    c = 204;
                }
                case 2 ->
                {
                    b = (x << 8) | (x << 4) | (x << 2) | (x << 1);
                    c = 93;
                }
                case 3 ->
                {
                    b = (x << 7) | (x << 2) | x;
                    c = 44;
                }
                case 4 ->
                {
                    b = (x << 6) | x;
                    c = 22;
                }
                case 5 ->
                {
                    b = (x << 5) | (x >> 2);
                    c = 11;
                }
                default ->
                {
                    b = (x << 4) | (x >> 4);
                    c = 5;
                }
            }
        } else
        {
            switch (bits)
            {
                case 1 ->
                {
                    b = 0;
                    c = 113;
                }
                case 2 ->
                {
                    b = (x << 8) | (x << 3) | (x << 2);
                    c = 54;
                }
                case 3 ->
                {
                    b = (x << 7) | (x << 1) | (x >> 1);
                    c = 26;
                }
                case 4 ->
                {
                    b = (x << 6) | (x >> 1);
                    c = 13;
                }
                default ->
                {
                    b = (x << 5) | (x >> 3);
                    c = 6;
                }
            }
        }

        int t = d * c + b;
        t ^= a;
        return ((a & 0x80) | (t >> 2)) & 0xFF;
    }

    static int unquantizeWeight(int value, int quant)
    {
        int bits = BITS[quant];
        int result;
        if (TRITS[quant] == 0 && QUINTS[quant] == 0)
        {
            int v      = value << (6 - bits);
            int filled = bits;
            while (filled < 6)
            {
                v |= v >> filled;
                filled *= 2;
            }
            result = v & 0x3F;
        } else if (bits == 0)
        {
            result = TRITS[quant] != 0 ? new int[]{0, 32, 63}[value] : new int[]{0, 16, 32, 47, 63}[value];
        } else
        {
            int d   = value >> bits;
            int low = value & ((1 << bits) - 1);
            int a   = (low & 1) != 0 ? 0x7F : 0;
            int x   = low >> 1;
            int b;
            int c;
            if (TRITS[quant] != 0)
            {
                switch (bits)
                {
                    case 1 ->
                    {
                        b = 0;
                        c = 50;
                    }
                    case 2 ->
                    {
                        b = (x << 6) | (x << 2) | x;
                        c = 23;
                    }
                    default ->
                    {
                        b = (x << 5) | x;
                        c = 11;
                    }
                }
            } else
            {
                switch (bits)
                {
                    case 1 ->
                    {
                        b = 0;
                        c = 28;
                    }
                    default ->
                    {
                        b = (x << 6) | (x << 1);
                        c = 13;
                    }
                }
            }
            int t = d * c + b;
            t ^= a;
            result = (a & 0x20) | (t >> 2);
        }
        return result > 32 ? result + 1 : result;
    }

    /**
     * Decodes LDR endpoints into e (r0 g0 b0 a0 r1 g1 b1 a1), returns false for HDR endpoint modes
     */
    static boolean decodeEndpoints(int cem, int[] v, int[] e)
    {
        switch (cem)
        {
            case 0 -> set(e, v[0], v[0], v[0], 255, v[1], v[1], v[1], 255);
            case 1 ->
            {
                int l0 = (v[0] >> 2) | (v[1] & 0xC0);
                int l1 = Math.min(255, l0 + (v[1] & 0x3F));
                set(e, l0, l0, l0, 255, l1, l1, l1, 255);
            }
            case 4 -> set(e, v[0], v[0], v[0], v[2], v[1], v[1], v[1], v[3]);
            case 5 ->
            {
                int[] t1 = bitTransferSigned(v[1], v[0]);
                int[] t3 = bitTransferSigned(v[3], v[2]);
                set(e, t1[1], t1[1], t1[1], t3[1], t1[1] + t1[0], t1[1] + t1[0], t1[1] + t1[0], t3[1] + t3[0]);
            }
            case 6 -> set(e, (v[0] * v[3]) >> 8, (v[1] * v[3]) >> 8, (v[2] * v[3]) >> 8, 255, v[0], v[1], v[2], 255);
            case 8 ->
            {
                if (v[1] + v[3] + v[5] >= v[0] + v[2] + v[4])
                {
                    set(e, v[0], v[2], v[4], 255, v[1], v[3], v[5], 255);
                } else
                {
                    blueContract(e, 0, v[1], v[3], v[5], 255);
                    blueContract(e, 4, v[0], v[2], v[4], 255);
                }
            }
            case 9 ->
            {
                int[] r = bitTransferSigned(v[1], v[0]);
                int[] g = bitTransferSigned(v[3], v[2]);
                int[] b = bitTransferSigned(v[5], v[4]);
                if (r[0] + g[0] + b[0] >= 0)
                {
                    set(e, r[1], g[1], b[1], 255, r[1] + r[0], g[1] + g[0], b[1] + b[0], 255);
                } else
                {
                    blueContract(e, 0, r[1] + r[0], g[1] + g[0], b[1] + b[0], 255);
                    blueContract(e, 4, r[1], g[1], b[1], 255);
                }
            }
            case 10 -> set(e, (v[0] * v[3]) >> 8, (v[1] * v[3]) >> 8, (v[2] * v[3]) >> 8, v[4], v[0], v[1], v[2], v[5]);
            case 12 ->
            {
                if (v[1] + v[3] + v[5] >= v[0] + v[2] + v[4])
                {
                    set(e, v[0], v[2], v[4], v[6], v[1], v[3], v[5], v[7]);
                } else
                {
                    blueContract(e, 0, v[1], v[3], v[5], v[7]);
                    blueContract(e, 4, v[0], v[2], v[4], v[6]);
                }
            }
            case 13 ->
            {
                int[] r  = bitTransferSigned(v[1], v[0]);
                int[] g  = bitTransferSigned(v[3], v[2]);
                int[] b  = bitTransferSigned(v[5], v[4]);
                int[] al = bitTransferSigned(v[7], v[6]);
                if (r[0] + g[0] + b[0] >= 0)
                {
                    set(e, r[1], g[1], b[1], al[1], r[1] + r[0], g[1] + g[0], b[1] + b[0], al[1] + al[0]);
                } else
                {
                    blueContract(e, 0, r[1] + r[0], g[1] + g[0], b[1] + b[0], al[1] + al[0]);
                    blueContract(e, 4, r[1], g[1], b[1], al[1]);
                }
            }
            default ->
            {
                return false;
            }
        }
        for (int i = 0; i < 8; i++)
        {
            e[i] = Math.max(0, Math.min(255, e[i]));
        }
        return true;
    }

    private static void set(int[] e, int... values)
    {
        System.arraycopy(values, 0, e, 0, 8);
    }

    private static void blueContract(int[] e, int offset, int r, int g, int b, int a)
    {
        e[offset] = (r + b) >> 1;
        e[offset + 1] = (g + b) >> 1;
        e[offset + 2] = b;
        e[offset + 3] = a;
    }

    /**
     * Returns {a, b} after bit_transfer_signed(a, b): a becomes a signed 6 bit offset, b the 8 bit base
     */
    private static int[] bitTransferSigned(int a, int b)
    {
        b >>= 1;
        b |= a & 0x80;
        a >>= 1;
        a &= 0x3F;
        if ((a & 0x20) != 0)
        {
            a -= 0x40;
        }
        return new int[]{a, b};
    }

    private static int hash52(int p)
    {
        p ^= p >>> 15;
        p *= 0xEEDE0891;
        p ^= p >>> 5;
        p += p << 16;
        p ^= p >>> 7;
        p ^= p >>> 3;
        p ^= p << 6;
        p ^= p >>> 17;
        return p;
    }

    static int selectPartition(int seed, int x, int y, int z, int partitionCount, boolean smallBlock)
    {
        if (smallBlock)
        {
            x <<= 1;
            y <<= 1;
            z <<= 1;
        }

        seed += (partitionCount - 1) * 1024;
        int rnum = hash52(seed);

        int seed1  = rnum & 0xF;
        int seed2  = (rnum >>> 4) & 0xF;
        int seed3  = (rnum >>> 8) & 0xF;
        int seed4  = (rnum >>> 12) & 0xF;
        int seed5  = (rnum >>> 16) & 0xF;
        int seed6  = (rnum >>> 20) & 0xF;
        int seed7  = (rnum >>> 24) & 0xF;
        int seed8  = (rnum >>> 28) & 0xF;
        int seed9  = (rnum >>> 18) & 0xF;
        int seed10 = (rnum >>> 22) & 0xF;
        int seed11 = (rnum >>> 26) & 0xF;
        int seed12 = ((rnum >>> 30) | (rnum << 2)) & 0xF;

        seed1 *= seed1;
        seed2 *= seed2;
        seed3 *= seed3;
        seed4 *= seed4;
        seed5 *= seed5;
        seed6 *= seed6;
        seed7 *= seed7;
        seed8 *= seed8;
        seed9 *= seed9;
        seed10 *= seed10;
        seed11 *= seed11;
        seed12 *= seed12;

        int sh1;
        int sh2;
        if ((seed & 1) != 0)
        {
            sh1 = (seed & 2) != 0 ? 4 : 5;
            sh2 = partitionCount == 3 ? 6 : 5;
        } else
        {
            sh1 = partitionCount == 3 ? 6 : 5;
            sh2 = (seed & 2) != 0 ? 4 : 5;
        }
        int sh3 = (seed & 0x10) != 0 ? sh1 : sh2;

        seed1 >>= sh1;
        seed2 >>= sh2;
        seed3 >>= sh1;
        seed4 >>= sh2;
        seed5 >>= sh1;
        seed6 >>= sh2;
        seed7 >>= sh1;
        seed8 >>= sh2;
        seed9 >>= sh3;
        seed10 >>= sh3;
        seed11 >>= sh3;
        seed12 >>= sh3;

        int a = seed1 * x + seed2 * y + seed11 * z + (rnum >>> 14);
        int b = seed3 * x + seed4 * y + seed12 * z + (rnum >>> 10);
        int c = seed5 * x + seed6 * y + seed9 * z + (rnum >>> 6);
        int d = seed7 * x + seed8 * y + seed10 * z + (rnum >>> 2);

        a &= 0x3F;
        b &= 0x3F;
        c &= 0x3F;
        d &= 0x3F;

        if (partitionCount < 4)
        {
            d = 0;
        }
        if (partitionCount < 3)
        {
            c = 0;
        }

        if (a >= b && a >= c && a >= d)
        {
            return 0;
        }
        if (b >= c && b >= d)
        {
            return 1;
        }
        if (c >= d)
        {
            return 2;
        }
        return 3;
    }
}

package no.stelar7.cdragon.types.dds;

/**
 * IEEE 754 binary16 helpers
 */
final class HalfFloat
{
    private HalfFloat()
    {
    }

    static float toFloat(int half)
    {
        return Float.float16ToFloat((short) half);
    }

    /**
     * Converts a half float to 0..255 by clamping to [0, 1] (NaN becomes 0) and rounding to nearest
     */
    static int toUnorm8(int half)
    {
        return unitToUnorm8(toFloat(half));
    }

    static int unitToUnorm8(float f)
    {
        if (!(f > 0))
        {
            return 0;
        }
        if (f >= 1)
        {
            return 255;
        }
        return (int) (f * 255 + 0.5f);
    }
}

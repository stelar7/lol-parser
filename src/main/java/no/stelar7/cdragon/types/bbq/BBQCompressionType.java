package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.ParseException;

public enum BBQCompressionType
{
    NONE(0),
    LZMA(1),
    LZ4(2),
    LZ4HC(3),
    LZHAM(4);

    private final int flag;

    BBQCompressionType(int flag)
    {
        this.flag = flag;
    }

    public static BBQCompressionType from(int flag)
    {
        for (BBQCompressionType value : values())
        {
            if (value.flag == flag)
            {
                return value;
            }
        }

        throw new ParseException("Unknown bundle compression type " + flag, -1);
    }
}

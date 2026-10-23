package no.stelar7.cdragon.types.inibin;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.inibin.data.*;
import no.stelar7.cdragon.util.handlers.CompressionHandler;
import no.stelar7.cdragon.util.readers.*;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

/**
 * Parser for version 2 inibin files.
 * Value set layout from LeagueToolkit InibinSet.cs (https://github.com/LeagueToolkit/LeagueToolkit/blob/main/src/LeagueToolkit/Core/Legacy/IO/Inibin/InibinSet.cs).
 * <p>
 * The parser keeps no state between calls, so it is thread-safe.
 */
public final class InibinParser implements Parseable<InibinFile>
{
    private static final int BIT_INT32       = 0;
    private static final int BIT_FLOAT32     = 1;
    private static final int BIT_U8_FIXED    = 2;
    private static final int BIT_INT16       = 3;
    private static final int BIT_U8          = 4;
    private static final int BIT_BOOL        = 5;
    private static final int BIT_U8_FIXED_V3 = 6;
    private static final int BIT_FLOAT32_V3  = 7;
    private static final int BIT_U8_FIXED_V2 = 8;
    private static final int BIT_FLOAT32_V2  = 9;
    private static final int BIT_U8_FIXED_V4 = 10;
    private static final int BIT_FLOAT32_V4  = 11;
    private static final int BIT_STRING      = 12;

    @Override
    public InibinFile parse(RandomAccessReader raf)
    {
        try
        {
            InibinHeader header = parseHeader(raf);
            return new InibinFile(header, parseKeys(raf, header));
        } catch (IndexOutOfBoundsException e)
        {
            throw new ParseException("Unexpected end of inibin data", raf.pos(), e);
        }
    }

    public InibinFile parseCompressed(Path path)
    {
        try
        {
            byte[] dataBytes = CompressionHandler.uncompressDEFLATE(Files.readAllBytes(path));
            return parse(new RandomAccessReader(dataBytes, ByteOrder.LITTLE_ENDIAN));
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, Object> parseKeys(RandomAccessReader raf, InibinHeader header)
    {
        Map<String, Object> keys    = new HashMap<>();
        int                 bitmask = header.getBitmask();

        for (int bit = 0; bit < 16; bit++)
        {
            if ((bitmask & (1 << bit)) == 0)
            {
                continue;
            }

            if (bit > BIT_STRING)
            {
                throw new ParseException("Unknown inibin value set (bit " + bit + ")", raf.pos());
            }

            int count = raf.readU16();
            if ((long) count * Integer.BYTES > raf.remaining())
            {
                throw new ParseException("Invalid inibin key count " + count, raf.pos());
            }

            int[] segmentKeys = raf.readIntArray(count);

            // strings are stored as u16 offsets relative to the end of the offset table
            long stringStart = raf.pos() + (long) count * Short.BYTES;
            int  bits        = 0;

            for (int i = 0; i < count; i++)
            {
                Object value;
                switch (bit)
                {
                    case BIT_INT32:
                        value = raf.readInt();
                        break;
                    case BIT_FLOAT32:
                        value = raf.readFloat();
                        break;
                    case BIT_U8_FIXED:
                        value = raf.readU8() / 10f;
                        break;
                    case BIT_INT16:
                        value = raf.readShort();
                        break;
                    case BIT_U8:
                        value = raf.readU8();
                        break;
                    case BIT_BOOL:
                    {
                        bits = (i % 8 == 0) ? raf.readU8() : bits >> 1;
                        value = String.valueOf((bits & 0x1) > 0);
                        break;
                    }
                    case BIT_U8_FIXED_V3:
                        value = raf.readBytes(3);
                        break;
                    case BIT_FLOAT32_V3:
                        value = raf.readBytes(3 * Float.BYTES);
                        break;
                    case BIT_U8_FIXED_V2:
                        value = raf.readBytes(2);
                        break;
                    case BIT_FLOAT32_V2:
                        value = raf.readBytes(2 * Float.BYTES);
                        break;
                    case BIT_U8_FIXED_V4:
                        value = raf.readBytes(4);
                        break;
                    case BIT_FLOAT32_V4:
                        value = raf.readBytes(4 * Float.BYTES);
                        break;
                    case BIT_STRING:
                        value = readString(raf, stringStart + raf.readU16());
                        break;
                    default:
                        throw new ParseException("Unknown inibin value set (bit " + bit + ")", raf.pos());
                }

                keys.put(Integer.toUnsignedString(segmentKeys[i]), value);
            }
        }

        return keys;
    }

    private static String readString(RandomAccessReader raf, long offset)
    {
        if (offset >= raf.size())
        {
            throw new ParseException("Inibin string offset " + offset + " is outside the file", raf.pos());
        }

        long pos = raf.pos();
        raf.seek(offset);
        String value = raf.readString();
        raf.seek(pos);
        return value;
    }

    private InibinHeader parseHeader(RandomAccessReader raf)
    {
        int version = raf.readU8();
        if (version != 2)
        {
            throw new ParseException(String.format("Unsupported inibin version %d, only version 2 is supported", version), 0);
        }

        int tableLength = raf.readU16();
        int bitmask     = raf.readU16();

        return new InibinHeader(version, tableLength, bitmask);

    }
}

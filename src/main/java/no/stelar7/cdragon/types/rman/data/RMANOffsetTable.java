package no.stelar7.cdragon.types.rman.data;

import no.stelar7.cdragon.util.readers.*;

import java.util.*;
import java.util.function.*;

/**
 * Reads a FlatBuffers vector of tables: u32 count, followed by count u32 offsets (relative to the offset itself) to the tables
 */
public class RMANOffsetTable
{
    public static List<Map<String, Object>> parseOffsetTable(RandomAccessReader raf, Map<String, String> fieldsToParse, BiFunction<RandomAccessReader, Map<String, String>, Map<String, Object>> parseFunction)
    {
        return parseOffsetTable(raf, r -> parseFunction.apply(r, fieldsToParse));
    }

    public static <T> List<T> parseOffsetTable(RandomAccessReader raf, Function<RandomAccessReader, T> parseFunction)
    {
        long start = raf.pos();
        long count = raf.readU32();
        if (count * 4 > raf.remaining())
        {
            throw new ParseException("Table of " + count + " entries is larger than the data", start);
        }

        List<T> offsetEntries = new ArrayList<>((int) count);
        for (long i = 0; i < count; i++)
        {
            long current = raf.pos();
            long offset  = raf.readInt();
            raf.seek(current + offset);
            offsetEntries.add(parseFunction.apply(raf));
            raf.seek(current + 4);
        }

        return offsetEntries;
    }
}

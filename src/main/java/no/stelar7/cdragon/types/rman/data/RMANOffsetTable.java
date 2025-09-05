package no.stelar7.cdragon.types.rman.data;

import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.util.*;
import java.util.function.BiFunction;

public class RMANOffsetTable
{
    public static List<Map<String, Object>> parseOffsetTable(RandomAccessReader raf, Map<String, String> fieldsToParse, BiFunction<RandomAccessReader, Map<String, String>, Map<String, Object>> parseFunction)
    {
        List<Map<String, Object>> offsetEntries = new ArrayList<>();
        
        int count = raf.readInt();
        for (int i = 0; i < count; i++)
        {
            int current = (int) raf.pos();
            int offset  = raf.readInt();
            raf.seek(current + offset);
            offsetEntries.add(parseFunction.apply(raf, fieldsToParse));
            raf.seek(current + 4);
        }
        
        return offsetEntries;
    }
    
}

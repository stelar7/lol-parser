package no.stelar7.cdragon.interfaces;

import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;
import java.nio.file.Path;

@FunctionalInterface
public interface Parseable<T>
{
    default T parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    default T parse(byte[] data)
    {
        return parse(new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN));
    }
    
    T parse(RandomAccessReader raf);
}

package no.stelar7.cdragon.interfaces;

import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;

@FunctionalInterface
public interface Parseable<T>
{
    default T parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }

    default T parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN));
    }

    /**
     * Parses the data.
     *
     * @throws no.stelar7.cdragon.util.readers.ParseException if the data is not in this format, or is corrupt or unsupported
     */
    T parse(RandomAccessReader raf);

    /**
     * A cheap check whether the data looks like this format (magic number, or a plausibility check for formats without one).
     * Use it for file type sniffing instead of calling {@link #parse} and checking for errors.
     * <p>
     * This does not validate the whole file, so {@link #parse} can still throw for data where this returns true.
     * The default returns true, meaning "unknown, try to parse it"; parsers of formats used for sniffing override it.
     */
    default boolean canParse(byte[] data)
    {
        return true;
    }
}

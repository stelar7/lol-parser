package no.stelar7.cdragon.types.filemanifest;

import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Parsers for the small "file manifest" formats. These formats have no magic number.
 * <p>
 * The canParse methods check the whole structure (counts and string lengths against the remaining data) without building anything,
 * so for these formats canParse returning true means the matching parse method succeeds on the same data.
 * The parse methods throw a {@link ParseException} for data that is not in the format.
 * Note that almost anything starting with a small count is a valid v0/v1 manifest, so prefer formats with a magic number when sniffing.
 */
public class ManifestContentParser
{
    private static final int ATLAS_ENTRY_MIN_SIZE = Integer.BYTES + 5 * Integer.BYTES;

    /**
     * True if the data is a v0 manifest (an int count, followed by that many ints)
     */
    public boolean canParseV0(byte[] data)
    {
        if (data == null)
        {
            return false;
        }

        ByteBuffer buf = wrap(data);
        return skipCount(buf, Integer.BYTES) >= 0;
    }

    /**
     * True if the data is a v1 manifest (an int count, followed by that many length-prefixed strings)
     */
    public boolean canParseV1(byte[] data)
    {
        if (data == null)
        {
            return false;
        }

        ByteBuffer buf   = wrap(data);
        int        count = skipCount(buf, Integer.BYTES);
        if (count < 0)
        {
            return false;
        }

        for (int i = 0; i < count; i++)
        {
            if (!skipString(buf))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * True if the data is a v2 manifest (a count of headers, each with a count of (int, string) items)
     */
    public boolean canParseV2(byte[] data)
    {
        if (data == null)
        {
            return false;
        }

        ByteBuffer buf   = wrap(data);
        int        count = skipCount(buf, 2 * Integer.BYTES);
        if (count < 0)
        {
            return false;
        }

        for (int i = 0; i < count; i++)
        {
            if (!skipString(buf))
            {
                return false;
            }

            int innerCount = skipCount(buf, 2 * Integer.BYTES);
            if (innerCount < 0)
            {
                return false;
            }

            for (int j = 0; j < innerCount; j++)
            {
                if (buf.remaining() < Integer.BYTES)
                {
                    return false;
                }
                buf.position(buf.position() + Integer.BYTES);
                if (!skipString(buf))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * True if the data is a version 1 atlas manifest
     */
    public boolean canParseAtlas(byte[] data)
    {
        if (data == null)
        {
            return false;
        }

        ByteBuffer buf = wrap(data);
        if (buf.remaining() < Integer.BYTES || buf.getInt() != 1)
        {
            return false;
        }

        if (!skipString(buf))
        {
            return false;
        }

        int count = skipCount(buf, ATLAS_ENTRY_MIN_SIZE);
        if (count < 0)
        {
            return false;
        }

        for (int i = 0; i < count; i++)
        {
            if (!skipString(buf) || buf.remaining() < 5 * Integer.BYTES)
            {
                return false;
            }
            buf.position(buf.position() + 5 * Integer.BYTES);
        }
        return true;
    }

    public ManifestContentFileV0 parseV0(Path path)
    {
        return parseV0(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }

    public ManifestContentFileV0 parseV0(ByteArray data)
    {
        return parseV0(new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN));
    }

    public ManifestContentFileV0 parseV0(RandomAccessReader raf)
    {
        int           count = readCount(raf, Integer.BYTES);
        List<Integer> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            items.add(raf.readInt());
        }

        return new ManifestContentFileV0(items);
    }


    public ManifestContentFileV1 parseV1(Path path)
    {
        return parseV1(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }

    public ManifestContentFileV1 parseV1(ByteArray data)
    {
        return parseV1(new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN));
    }

    public ManifestContentFileV1 parseV1(RandomAccessReader raf)
    {
        int          count = readCount(raf, Integer.BYTES);
        List<String> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            items.add(readString(raf));
        }

        return new ManifestContentFileV1(items);
    }

    public ManifestContentFileV2 parseV2(Path path)
    {
        return parseV2(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }

    public ManifestContentFileV2 parseV2(ByteArray data)
    {
        return parseV2(new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN));
    }

    public ManifestContentFileV2 parseV2(RandomAccessReader raf)
    {
        Map<String, List<String>> items = new HashMap<>();

        int count = readCount(raf, 2 * Integer.BYTES);
        for (int i = 0; i < count; i++)
        {
            String       header     = readString(raf);
            int          innerCount = readCount(raf, 2 * Integer.BYTES);
            List<String> values     = items.computeIfAbsent(header, k -> new ArrayList<>());
            for (int i1 = 0; i1 < innerCount; i1++)
            {
                if (raf.remaining() < Integer.BYTES)
                {
                    throw new ParseException("Unexpected end of manifest data", raf);
                }
                
                int    unknown = raf.readInt();
                String value   = readString(raf);
                values.add(value);
            }
        }

        return new ManifestContentFileV2(items);
    }

    public ManifestContentFileAtlas parseAtlas(Path path)
    {
        return parseAtlas(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }

    public ManifestContentFileAtlas parseAtlas(ByteArray data)
    {
        return parseAtlas(new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN));
    }

    /**
     * The header and the string lengths are little endian, the rectangle of each entry is big endian.
     * The byte order of the reader is restored afterwards.
     */
    public ManifestContentFileAtlas parseAtlas(RandomAccessReader raf)
    {
        ByteOrder original = raf.getEndian();
        try
        {
            raf.setEndian(ByteOrder.LITTLE_ENDIAN);

            if (raf.remaining() < Integer.BYTES)
            {
                throw new ParseException("Unexpected end of manifest data", raf);
            }

            int version = raf.readInt();
            if (version != 1)
            {
                throw new ParseException("Unsupported atlas manifest version " + version, raf.pos() - 4);
            }

            String atlasFile = readString(raf);

            int                       count = readCount(raf, ATLAS_ENTRY_MIN_SIZE);
            List<Map<String, Object>> items = new ArrayList<>(count);
            for (int i = 0; i < count; i++)
            {
                raf.setEndian(ByteOrder.LITTLE_ENDIAN);
                String entryName = readString(raf);

                if (raf.remaining() < 5 * Integer.BYTES)
                {
                    throw new ParseException("Unexpected end of manifest data", raf);
                }

                raf.setEndian(ByteOrder.BIG_ENDIAN);
                int x = raf.readInt();
                int y = raf.readInt();
                int w = raf.readInt();
                int h = raf.readInt();
                int z = raf.readInt();

                items.add(ManifestContentFileAtlas.item(entryName, x, y, z, w, h));
            }

            return new ManifestContentFileAtlas(atlasFile, items);
        } finally
        {
            raf.setEndian(original);
        }
    }

    /**
     * Reads a count, and makes sure that many items of at least minItemSize bytes fit in the remaining data
     */
    private static int readCount(RandomAccessReader raf, int minItemSize)
    {
        if (raf.remaining() < Integer.BYTES)
        {
            throw new ParseException("Unexpected end of manifest data", raf);
        }

        int count = raf.readInt();
        if (count < 0 || (long) count * minItemSize > raf.remaining())
        {
            throw new ParseException("Invalid manifest count " + count, raf.pos() - 4);
        }
        return count;
    }

    /**
     * Reads a string prefixed by its length as an int
     */
    private static String readString(RandomAccessReader raf)
    {
        if (raf.remaining() < Integer.BYTES)
        {
            throw new ParseException("Unexpected end of manifest data", raf);
        }

        int length = raf.readInt();
        if (length < 0 || length > raf.remaining())
        {
            throw new ParseException("Invalid manifest string length " + length, raf.pos() - 4);
        }
        return raf.readString(length);
    }

    private static ByteBuffer wrap(byte[] data)
    {
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * The canParse version of {@link #readCount}: the count, or -1 if it is invalid
     */
    private static int skipCount(ByteBuffer buf, int minItemSize)
    {
        if (buf.remaining() < Integer.BYTES)
        {
            return -1;
        }

        int count = buf.getInt();
        if (count < 0 || (long) count * minItemSize > buf.remaining())
        {
            return -1;
        }
        return count;
    }

    /**
     * The canParse version of {@link #readString}: skips the string, false if it is invalid
     */
    private static boolean skipString(ByteBuffer buf)
    {
        if (buf.remaining() < Integer.BYTES)
        {
            return false;
        }

        int length = buf.getInt();
        if (length < 0 || length > buf.remaining())
        {
            return false;
        }
        buf.position(buf.position() + length);
        return true;
    }
}

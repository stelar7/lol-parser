package no.stelar7.cdragon.types.releasemanifest;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.releasemanifest.data.*;
import no.stelar7.cdragon.util.handlers.HashHandler;
import no.stelar7.cdragon.util.readers.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parser for RLSM release manifests.
 * Throws {@link ParseException} if the data is not a release manifest (see {@link #canParse(byte[])}), or is corrupt.
 */
public class ReleasemanifestParser implements Parseable<ReleasemanifestDirectory>
{
    private static final int DIRECTORY_SIZE = 5 * Integer.BYTES;
    private static final int FILE_SIZE      = 44;
    private static final int HEADER_SIZE    = 16;

    /**
     * True if the data starts with "RLSM" and is big enough for the header
     */
    @Override
    public boolean canParse(byte[] data)
    {
        return data != null && data.length >= HEADER_SIZE && data[0] == 'R' && data[1] == 'L' && data[2] == 'S' && data[3] == 'M';
    }

    @Override
    public ReleasemanifestDirectory parse(RandomAccessReader raf)
    {
        long start = raf.pos();
        if (raf.remaining() < HEADER_SIZE)
        {
            throw new ParseException("Not a release manifest (too small)", start);
        }

        try
        {
            ReleasemanifestHeader header = parseHeader(raf);
            if (!"RLSM".equals(header.getMagic()))
            {
                throw new ParseException("Not a release manifest (magic is " + header.getMagic() + ")", start);
            }

            List<ReleasemanifestDataDirectory>   directories = parseDirectories(raf);
            List<ReleasemanifestDataContentFile> files       = parseFiles(raf);
            List<String>                         strings     = parseStrings(raf);
            ReleasemanifestDataFile              file        = new ReleasemanifestDataFile(header, directories, files, strings);

            if (file.getDirectories().isEmpty())
            {
                throw new ParseException("Release manifest has no root directory", raf.pos());
            }

            return solveDirectory(file, 0, new BitSet());
        } catch (IndexOutOfBoundsException e)
        {
            throw new ParseException("Unexpected end of release manifest data", raf.pos(), e);
        }
    }

    private ReleasemanifestDirectory solveDirectory(ReleasemanifestDataFile data, int index, BitSet visited)
    {
        // every directory can only be visited once, this stops loops in corrupt files
        if (visited.get(index))
        {
            throw new ParseException("Release manifest directory " + index + " is referenced more than once", -1);
        }
        visited.set(index);

        ReleasemanifestDataDirectory dir  = data.getDirectories().get(index);
        String                       name = getString(data, dir.getNameIndex());

        checkRange("file", dir.getFileStartIndex(), dir.getFileCount(), data.getFiles().size());
        List<ReleasemanifestContentFile> files = new ArrayList<>();
        for (int i = dir.getFileStartIndex(); i < dir.getFileStartIndex() + dir.getFileCount(); i++)
        {
            ReleasemanifestDataContentFile file = data.getFiles().get(i);
            files.add(new ReleasemanifestContentFile(getString(data, file.getNameIndex()), file.getSize(), file.getHash()));
        }

        checkRange("directory", dir.getSubdirectoryStartIndex(), dir.getSubdirectoryCount(), data.getDirectories().size());
        List<ReleasemanifestDirectory> subDirectories = new ArrayList<>();
        for (int i = dir.getSubdirectoryStartIndex(); i < dir.getSubdirectoryStartIndex() + dir.getSubdirectoryCount(); i++)
        {
            subDirectories.add(solveDirectory(data, i, visited));
        }

        return new ReleasemanifestDirectory(name, files, subDirectories);
    }

    private static String getString(ReleasemanifestDataFile data, int index)
    {
        if (index < 0 || index >= data.getStrings().size())
        {
            throw new ParseException("Release manifest string index " + index + " is out of range", -1);
        }
        return data.getStrings().get(index);
    }

    private static void checkRange(String what, int start, int count, int size)
    {
        if (start < 0 || count < 0 || (long) start + count > size)
        {
            throw new ParseException("Release manifest " + what + " range " + start + "+" + count + " is out of range (" + size + ")", -1);
        }
    }

    private static int readCount(RandomAccessReader raf, int itemSize)
    {
        long count = raf.readU32();
        if (count * itemSize > raf.remaining())
        {
            throw new ParseException("Invalid release manifest count " + count, raf.pos() - 4);
        }
        return (int) count;
    }

    private ReleasemanifestHeader parseHeader(RandomAccessReader raf)
    {
        String magic   = raf.readString(4);
        int    type    = raf.readInt();
        int    entries = raf.readInt();
        int    version = raf.readInt();

        return new ReleasemanifestHeader(magic, type, entries, version);
    }


    private List<ReleasemanifestDataDirectory> parseDirectories(RandomAccessReader raf)
    {
        int count = readCount(raf, DIRECTORY_SIZE);

        List<ReleasemanifestDataDirectory> dirs = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
        {
            int nameIndex              = raf.readInt();
            int subdirectoryStartIndex = raf.readInt();
            int subdirectoryCount      = raf.readInt();
            int fileStartIndex         = raf.readInt();
            int fileCount              = raf.readInt();

            dirs.add(new ReleasemanifestDataDirectory(nameIndex, subdirectoryStartIndex, subdirectoryCount, fileStartIndex, fileCount));
        }

        return dirs;
    }

    private List<ReleasemanifestDataContentFile> parseFiles(RandomAccessReader raf)
    {
        int count = readCount(raf, FILE_SIZE);

        List<ReleasemanifestDataContentFile> files = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
        {
            int    nameIndex      = raf.readInt();
            int    version        = raf.readInt();
            String hash           = HashHandler.toHex(raf.readBytes(16));
            int    flags          = raf.readInt();
            int    size           = raf.readInt();
            int    compressedSize = raf.readInt();
            int    unknown        = raf.readInt();
            short  type           = raf.readShort();
            short  padding        = raf.readShort();

            files.add(new ReleasemanifestDataContentFile(nameIndex, version, hash, flags, size, compressedSize, unknown, type, padding));

        }

        return files;
    }

    /**
     * The string table is a count, the size in bytes of the table, and then that many bytes of 0x00 terminated strings
     */
    private List<String> parseStrings(RandomAccessReader raf)
    {
        long count      = raf.readU32();
        long stringSize = raf.readU32();
        if (stringSize > raf.remaining() || count > stringSize)
        {
            throw new ParseException("Invalid release manifest string table (" + count + " strings in " + stringSize + " bytes)", raf.pos() - 8);
        }

        byte[]       table   = raf.readBytes(stringSize);
        List<String> strings = new ArrayList<>((int) count);

        int start = 0;
        for (int i = 0; i < count; i++)
        {
            if (start >= table.length)
            {
                throw new ParseException("Release manifest string table only has " + i + " of " + count + " strings", raf.pos());
            }

            int end = start;
            while (end < table.length && table[end] != 0)
            {
                end++;
            }

            strings.add(new String(table, start, end - start, StandardCharsets.UTF_8).trim());
            start = end + 1;
        }

        return strings;
    }
}

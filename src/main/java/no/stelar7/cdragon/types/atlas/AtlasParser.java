package no.stelar7.cdragon.types.atlas;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.atlas.data.*;
import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.nio.charset.*;
import java.util.*;

/**
 * The atlas format has no magic or version, so the file is identified by checking that the bundle table is plausible.
 * <p>
 * Use {@link #canParse(byte[])} to check if data looks like an atlas file (this is used for file type sniffing).
 * {@link #parse(RandomAccessReader)} throws a {@link ParseException} if the data is not an atlas file, or if the entries are corrupt or truncated.
 */
public class AtlasParser implements Parseable<AtlasFile>
{
    private static final int MAX_BUNDLES     = 4096;
    private static final int MAX_NAME_LENGTH = 4096;
    private static final int ENTRY_MIN_SIZE  = 4 + 4 * 4 + 4;

    /**
     * True if the data starts with a plausible bundle table, followed by an entry count that fits in the file (the format has no magic)
     */
    @Override
    public boolean canParse(byte[] data)
    {
        if (data == null)
        {
            return false;
        }

        RandomAccessReader raf = new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN);
        if (parseBundleNames(raf) == null)
        {
            return false;
        }

        int entryCount = raf.readInt();
        return entryCount >= 0 && (long) entryCount * ENTRY_MIN_SIZE <= raf.remaining();
    }

    @Override
    public AtlasFile parse(RandomAccessReader raf)
    {
        long         start       = raf.pos();
        List<String> bundleNames = parseBundleNames(raf);
        if (bundleNames == null)
        {
            throw new ParseException("Not an atlas file (invalid bundle table)", start);
        }

        Map<String, List<AtlasEntry>> data = new HashMap<>();
        for (AtlasEntry entry : parseEntries(raf, bundleNames.size()))
        {
            data.computeIfAbsent(bundleNames.get(entry.getBundleIndex()), k -> new ArrayList<>()).add(entry);
        }

        AtlasFile file = new AtlasFile();
        file.setData(data);
        return file;
    }

    /**
     * Reads the bundle table, or returns null if it does not look like one
     */
    private List<String> parseBundleNames(RandomAccessReader raf)
    {
        if (raf.remaining() < 8)
        {
            return null;
        }

        int bundleCount = raf.readInt();
        if (bundleCount <= 0 || bundleCount > MAX_BUNDLES || (long) bundleCount * 5 > raf.remaining())
        {
            return null;
        }

        List<String> bundleNames = new ArrayList<>();
        for (int i = 0; i < bundleCount; i++)
        {
            String name = readName(raf);
            if (name == null || name.isEmpty())
            {
                return null;
            }
            bundleNames.add(name);
        }

        // the entry count must follow
        if (raf.remaining() < 4)
        {
            return null;
        }

        return bundleNames;
    }

    /**
     * Reads a length prefixed printable string, or null if the data at the current position is not one
     */
    private String readName(RandomAccessReader raf)
    {
        if (raf.remaining() < 4)
        {
            return null;
        }

        int length = raf.readInt();
        if (length < 0 || length > MAX_NAME_LENGTH || length > raf.remaining())
        {
            return null;
        }

        byte[] bytes = raf.readBytes(length);
        for (byte b : bytes)
        {
            // control characters (including 0x00) never appear in names
            if (b >= 0 && b < 0x20)
            {
                return null;
            }
        }

        try
        {
            return StandardCharsets.UTF_8.newDecoder()
                                         .onMalformedInput(CodingErrorAction.REPORT)
                                         .onUnmappableCharacter(CodingErrorAction.REPORT)
                                         .decode(java.nio.ByteBuffer.wrap(bytes))
                                         .toString();
        } catch (CharacterCodingException e)
        {
            return null;
        }
    }

    private List<AtlasEntry> parseEntries(RandomAccessReader raf, int bundleCount)
    {
        long countOffset      = raf.pos();
        int  bundleEntryCount = raf.readInt();
        if (bundleEntryCount < 0 || (long) bundleEntryCount * ENTRY_MIN_SIZE > raf.remaining())
        {
            throw new ParseException("Invalid atlas entry count " + bundleEntryCount, countOffset);
        }

        List<AtlasEntry> entries = new ArrayList<>();
        for (int j = 0; j < bundleEntryCount; j++)
        {
            long entryOffset = raf.pos();

            AtlasEntry entry = new AtlasEntry();
            String     name  = readName(raf);
            if (name == null)
            {
                throw new ParseException("Invalid atlas entry name for entry " + j, entryOffset);
            }
            entry.setName(name);

            if (raf.remaining() < 4 * 4 + 4)
            {
                throw new ParseException("Atlas entry " + j + " is truncated", raf);
            }

            entry.setStartX(raf.readFloat());
            entry.setStartY(raf.readFloat());
            entry.setEndX(raf.readFloat());
            entry.setEndY(raf.readFloat());
            entry.setBundleIndex(raf.readInt());

            if (!Float.isFinite(entry.getStartX()) || !Float.isFinite(entry.getStartY()) || !Float.isFinite(entry.getEndX()) || !Float.isFinite(entry.getEndY()))
            {
                throw new ParseException("Atlas entry " + j + " has non-finite coordinates", entryOffset);
            }

            if (entry.getBundleIndex() < 0 || entry.getBundleIndex() >= bundleCount)
            {
                throw new ParseException("Atlas entry " + j + " references bundle " + entry.getBundleIndex() + ", but there are only " + bundleCount, entryOffset);
            }

            entries.add(entry);
        }

        return entries;
    }
}

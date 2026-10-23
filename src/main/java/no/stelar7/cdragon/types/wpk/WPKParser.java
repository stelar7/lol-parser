package no.stelar7.cdragon.types.wpk;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.wem.data.*;
import no.stelar7.cdragon.types.wpk.data.*;
import no.stelar7.cdragon.util.readers.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class WPKParser implements Parseable<WPKFile>
{
    private static final String MAGIC   = "r3d2";
    private static final int    VERSION = 1;

    @Override
    public WPKFile parse(RandomAccessReader raf)
    {
        try
        {
            WPKHeader     header  = parseHeader(raf);
            List<Integer> offsets = parseOffsets(raf, header);
            return new WPKFile(header, offsets, parseData(raf, offsets));
        } catch (IndexOutOfBoundsException e)
        {
            throw new ParseException("WPK truncated", raf.pos(), e);
        }
    }

    private List<WEMFile> parseData(RandomAccessReader raf, List<Integer> offsets)
    {
        List<WEMFile> wems = new ArrayList<>();
        for (Integer offset : offsets)
        {
            long entryOffset = Integer.toUnsignedLong(offset);
            if (entryOffset == 0)
            {
                // empty slot
                continue;
            }

            if (entryOffset + 12 > raf.size())
            {
                throw new ParseException("WPK entry out of range", entryOffset);
            }

            raf.seek(entryOffset);

            long    dataOffset = raf.readU32();
            long    dataLength = raf.readU32();
            long    nameLength = raf.readU32() * 2;

            if (dataOffset + dataLength > raf.size())
            {
                throw new ParseException("WPK entry data out of range", entryOffset);
            }

            if (nameLength > raf.remaining())
            {
                throw new ParseException("WPK entry name out of range", entryOffset);
            }

            String filename = raf.readString(nameLength, StandardCharsets.UTF_16LE);

            raf.seek(dataOffset);
            byte[] data = raf.readBytes(dataLength);

            // one bad wem should not make the whole archive unreadable; keep the raw bytes and the reason instead
            WEMData parsed     = null;
            String  parseError = null;
            try
            {
                parsed = new WEMData(data);
            } catch (ParseException e)
            {
                parseError = e.getMessage();
            }

            wems.add(new WEMFile((int) dataOffset, (int) dataLength, filename, parsed, data, parseError));
        }
        return wems;
    }

    private List<Integer> parseOffsets(RandomAccessReader raf, WPKHeader header)
    {
        long count = Integer.toUnsignedLong(header.getFileCount());
        if (count * 4 > raf.remaining())
        {
            throw new ParseException("WPK file count out of range: " + count, raf);
        }

        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < count; i++)
        {
            offsets.add(raf.readInt());
        }
        return offsets;
    }

    private WPKHeader parseHeader(RandomAccessReader raf)
    {
        if (raf.remaining() < 12)
        {
            throw new ParseException("File too small to be a WPK", raf);
        }

        String magic = raf.readString(4);
        if (!MAGIC.equals(magic))
        {
            throw new ParseException("Invalid WPK magic: " + magic, 0);
        }

        int version = raf.readInt();
        if (version != VERSION)
        {
            throw new ParseException("Unsupported WPK version: " + version, 4);
        }

        return new WPKHeader(magic, version, raf.readInt());
    }

}

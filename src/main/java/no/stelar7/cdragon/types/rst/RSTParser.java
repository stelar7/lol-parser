package no.stelar7.cdragon.types.rst;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.util.readers.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parser for RST (fontconfig) files, version 2 to 5.
 * Layout from CDTB rstfile.py: https://github.com/CommunityDragon/CDTB/blob/master/cdtb/rstfile.py
 * <p>
 * Throws {@link ParseException} if the data is not an RST file (see {@link #canParse(byte[])}), for unsupported versions, or corrupt data.
 */
public class RSTParser implements Parseable<RSTFile>
{
    /**
     * True if the data starts with "RST" and a version byte
     */
    @Override
    public boolean canParse(byte[] data)
    {
        return data != null && data.length >= 4 && data[0] == 'R' && data[1] == 'S' && data[2] == 'T';
    }

    @Override
    public RSTFile parse(RandomAccessReader raf)
    {
        long start = raf.pos();
        if (raf.remaining() < 4)
        {
            throw new ParseException("Not an RST file (too small)", start);
        }

        String magic = raf.readString(3);
        if (!magic.equals("RST"))
        {
            throw new ParseException("Not an RST file (magic is " + magic + ")", start);
        }

        try
        {
            return parseBody(raf, magic);
        } catch (IndexOutOfBoundsException e)
        {
            throw new ParseException("Unexpected end of RST data", raf.pos(), e);
        }
    }

    private RSTFile parseBody(RandomAccessReader raf, String magic)
    {
        int    major  = raf.readU8();
        int    minor  = 0;
        String config = null;

        switch (major)
        {
            case 2:
            {
                // a flag telling if there is a font config string
                int hasConfig = raf.readU8();
                minor = hasConfig;
                if (hasConfig != 0)
                {
                    long configLength = raf.readU32();
                    checkRemaining(raf, configLength);
                    config = raf.readString(configLength);
                }
                break;
            }
            case 3:
            case 4:
            case 5:
                break;
            default:
                throw new ParseException("Unsupported RST version " + major, raf.pos() - 1);
        }

        long entryCount = raf.readU32();
        checkRemaining(raf, entryCount * Long.BYTES);
        long[] rawEntries = raf.readLongArray((int) entryCount);

        // version 5 dropped the "has trenc" byte
        boolean hasTrenc = false;
        if (major < 5)
        {
            hasTrenc = raf.readU8() != 0;
        }

        byte[] data     = raf.readRemaining();
        int    hashBits = major >= 4 ? detectHashBits(major, rawEntries, data, hasTrenc) : 40;
        Map<Long, String> entries  = new HashMap<>();
        long              hashMask = (1L << hashBits) - 1;
        for (long raw : rawEntries)
        {
            long offset = raw >>> hashBits;
            long hash   = raw & hashMask;

            if (offset >= data.length)
            {
                throw new ParseException("RST string offset " + offset + " is outside the string data (" + data.length + " bytes)", -1);
            }

            entries.put(hash, readEntry(data, (int) offset, hasTrenc));
        }

        return new RSTFile(magic, major, minor, config, hashBits, entries);

    }

    private static String readEntry(byte[] data, int offset, boolean hasTrenc)
    {
        if (hasTrenc && (data[offset] & 0xFF) == 0xFF && offset + 3 <= data.length)
        {
            // binary (encrypted) entry: 0xFF, u16 size, data
            int size = (data[offset + 1] & 0xFF) | ((data[offset + 2] & 0xFF) << 8);
            int end  = Math.min(data.length, offset + 3 + size);

            byte[] raw = new byte[end - (offset + 3)];
            System.arraycopy(data, offset + 3, raw, 0, raw.length);
            return Base64.getEncoder().encodeToString(raw);
        }

        int end = offset;
        while (end < data.length && data[end] != 0)
        {
            end++;
        }

        return new String(data, offset, end - offset, StandardCharsets.UTF_8);
    }

    /**
     * Version 4 and 5 use 39 hash bits, but files from patch 15.2 and newer use 38 (see CDTB).
     * The file itself does not say which, so use the one where the offsets point at the start of strings.
     */
    private static int detectHashBits(int major, long[] rawEntries, byte[] data, boolean hasTrenc)
    {
        int score38 = scoreHashBits(38, rawEntries, data, hasTrenc);
        int score39 = scoreHashBits(39, rawEntries, data, hasTrenc);

        if (score38 != score39)
        {
            return score38 > score39 ? 38 : 39;
        }

        return major >= 5 ? 38 : 39;
    }

    private static int scoreHashBits(int bits, long[] rawEntries, byte[] data, boolean hasTrenc)
    {
        int score = 0;
        for (long raw : rawEntries)
        {
            long offset = raw >>> bits;
            if (offset >= data.length)
            {
                // invalid offsets rule out this choice
                return -1;
            }

            int off = (int) offset;
            if (off == 0 || data[off - 1] == 0 || (hasTrenc && (data[off] & 0xFF) == 0xFF))
            {
                score++;
            }
        }
        return score;
    }

    private static void checkRemaining(RandomAccessReader raf, long needed)
    {
        if (needed < 0 || needed > raf.remaining())
        {
            throw new ParseException("RST size " + needed + " is larger than the remaining " + raf.remaining() + " bytes", raf.pos());
        }
    }
}

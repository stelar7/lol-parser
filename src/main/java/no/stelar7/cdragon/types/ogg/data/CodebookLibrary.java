package no.stelar7.cdragon.types.ogg.data;

import no.stelar7.cdragon.util.readers.*;

import java.io.*;
import java.nio.ByteOrder;

/**
 * Port of ww2ogg's codebook_library (codebook.cpp / codebook.h)
 */
public class CodebookLibrary
{
    private static final String DEFAULT_CODEBOOKS  = "codebook/packed_codebooks_aoTuV_603.bin";
    /**
     * The older "standard" Wwise codebooks (ww2ogg's packed_codebooks.bin), generated from vgmstream's vcb_list_standard.
     * Regenerating the aoTuV set the same way from vcb_list_aotuv603 gives a file byte identical to DEFAULT_CODEBOOKS.
     */
    private static final String STANDARD_CODEBOOKS = "codebook/packed_codebooks.bin";

    /**
     * The shape of a rebuilt codebook, used to check that the setup uses it the way a vorbis decoder allows
     *
     * @param lookupType 0 = no value mapping (only usable for scalar lookups), 1 = lattice value mapping
     */
    public record CodebookInfo(int dimensions, int entries, int lookupType)
    {
    }

    private final int    codebookCount;
    private final int[]  codebookOffsets;
    private final byte[] codebookData;

    private static final class DefaultHolder
    {
        private static final CodebookLibrary INSTANCE = load(DEFAULT_CODEBOOKS);
    }

    private static final class StandardHolder
    {
        private static final CodebookLibrary INSTANCE = load(STANDARD_CODEBOOKS);
    }

    /**
     * The packed aoTuV 6.03 codebooks bundled with this library, loaded once.
     * Used by Wwise 2012+ (.wem with the vorbis info in the fmt chunk, vgmstream's WWVORBIS_V62).
     */
    public static CodebookLibrary getDefault()
    {
        return DefaultHolder.INSTANCE;
    }

    /**
     * The packed "standard" (pre aoTuV) Wwise codebooks, loaded once.
     * Used by old Wwise with a vorb chunk (vgmstream's WWVORBIS_V48 - V53) and some early fmt-only files (WWVORBIS_V56).
     */
    public static CodebookLibrary getStandard()
    {
        return StandardHolder.INSTANCE;
    }

    private static CodebookLibrary load(String resource)
    {
        try (InputStream is = CodebookLibrary.class.getClassLoader().getResourceAsStream(resource))
        {
            if (is == null)
            {
                throw new IllegalStateException("Missing codebook resource: " + resource);
            }
            return new CodebookLibrary(is.readAllBytes());
        } catch (IOException e)
        {
            throw new UncheckedIOException("Failed to read codebook resource: " + resource, e);
        }
    }

    /**
     * @param packed a packed codebook file (codebook data, followed by a table of offsets, followed by the offset of that table)
     */
    public CodebookLibrary(byte[] packed)
    {
        if (packed.length < 4)
        {
            throw new ParseException("Codebook library too small", -1);
        }

        RandomAccessReader raf = new RandomAccessReader(packed, ByteOrder.LITTLE_ENDIAN);
        raf.seek(packed.length - 4);
        long offsetOffset = raf.readU32();
        if (offsetOffset > packed.length - 4)
        {
            throw new ParseException("Invalid codebook library offset table", packed.length - 4);
        }

        codebookCount = (int) ((packed.length - offsetOffset) / 4);
        codebookOffsets = new int[codebookCount];
        codebookData = packed;

        raf.seek(offsetOffset);
        for (int i = 0; i < codebookCount; i++)
        {
            codebookOffsets[i] = raf.readInt();
        }
    }

    /**
     * @return the number of entries in the offset table; the last entry is the end of the data, so valid ids are 0 to getCodebookCount() - 2
     */
    public int getCodebookCount()
    {
        return codebookCount;
    }

    /**
     * Rebuilds the codebook with the given id from this library
     *
     * @throws ParseException if there is no codebook with that id
     */
    public CodebookInfo rebuild(int codeId, OGGStream stream)
    {
        int codebookSize = getCodebookSize(codeId);
        if (codebookSize < 0)
        {
            throw new ParseException("Invalid codebook id " + codeId, -1);
        }

        RandomAccessReader reader = new RandomAccessReader(codebookData, ByteOrder.LITTLE_ENDIAN);
        reader.seek(codebookOffsets[codeId]);
        return rebuild(reader, codebookSize, stream);
    }

    /**
     * Rebuilds a stripped codebook into a standard vorbis codebook
     *
     * @param codebookSize the expected size of the codebook in bytes, or 0 to not check the size (for an inline codebook)
     */
    public static CodebookInfo rebuild(RandomAccessReader bitStream, int codebookSize, OGGStream ogg)
    {
        int startBits = bitStream.getTotalBitsRead();

        int dimensions = bitStream.readBits(4);
        int entries    = bitStream.readBits(14);

        ogg.bitWrite(0x564342, 24);
        ogg.bitWrite(dimensions, 16);
        ogg.bitWrite(entries, 24);

        int orderedFlag = bitStream.readBits(1);
        ogg.writeBit(orderedFlag);

        if (orderedFlag == 1)
        {
            copyOrderedLengths(bitStream, ogg, entries);
        } else
        {
            int codewordLengthLength = bitStream.readBits(3);
            int sparse               = bitStream.readBits(1);

            if (codewordLengthLength == 0 || codewordLengthLength > 5)
            {
                throw new ParseException("Nonsense codeword length", bitStream);
            }

            ogg.writeBit(sparse);

            for (int i = 0; i < entries; i++)
            {
                boolean present = true;

                if (sparse == 1)
                {
                    int sparsePresence = bitStream.readBits(1);
                    ogg.writeBit(sparsePresence);
                    present = (sparsePresence == 1);
                }

                if (present)
                {
                    int codewordLength = bitStream.readBits(codewordLengthLength);
                    ogg.bitWrite(codewordLength, 5);
                }
            }
        }

        int lookupType = bitStream.readBits(1);
        ogg.bitWrite(lookupType, 4);
        if (lookupType == 1)
        {
            copyLookupTable(bitStream, ogg, entries, dimensions);
        }

        int bitsRead = bitStream.getTotalBitsRead() - startBits;
        // if all bits are used in the last byte there will be one extra 0 byte
        if (codebookSize != 0 && (bitsRead / 8) + 1 != codebookSize)
        {
            throw new ParseException("Codebook size mismatch, expected " + codebookSize + " got " + ((bitsRead / 8) + 1), bitStream);
        }

        return new CodebookInfo(dimensions, entries, lookupType);
    }

    /**
     * Copies a standard vorbis codebook as-is
     */
    public static CodebookInfo copy(RandomAccessReader bitStream, OGGStream ogg)
    {
        int id         = bitStream.readBits(24);
        int dimensions = bitStream.readBits(16);
        int entries    = bitStream.readBits(24);

        if (id != 0x564342)
        {
            throw new ParseException("Invalid codebook identifier", bitStream);
        }

        ogg.bitWrite(id, 24);
        ogg.bitWrite(dimensions, 16);
        ogg.bitWrite(entries, 24);

        int orderedFlag = bitStream.readBits(1);
        ogg.writeBit(orderedFlag);
        if (orderedFlag == 1)
        {
            copyOrderedLengths(bitStream, ogg, entries);
        } else
        {
            int sparse = bitStream.readBits(1);
            ogg.writeBit(sparse);

            for (int i = 0; i < entries; i++)
            {
                boolean present = true;
                if (sparse == 1)
                {
                    int sparsePresence = bitStream.readBits(1);
                    ogg.writeBit(sparsePresence);
                    present = (sparsePresence == 1);
                }

                if (present)
                {
                    int codewordLength = bitStream.readBits(5);
                    ogg.bitWrite(codewordLength, 5);
                }
            }
        }

        int lookupType = bitStream.readBits(4);
        ogg.bitWrite(lookupType, 4);
        if (lookupType == 1)
        {
            copyLookupTable(bitStream, ogg, entries, dimensions);
        } else if (lookupType == 2)
        {
            throw new ParseException("Didn't expect lookup type 2", bitStream);
        } else if (lookupType != 0)
        {
            throw new ParseException("Invalid lookup type " + lookupType, bitStream);
        }

        return new CodebookInfo(dimensions, entries, lookupType);
    }

    private static void copyOrderedLengths(RandomAccessReader bitStream, OGGStream ogg, int entries)
    {
        int initLength = bitStream.readBits(5);
        ogg.bitWrite(initLength, 5);

        int currentEntry = 0;
        while (currentEntry < entries)
        {
            int bitCount = ilog(entries - currentEntry);
            int number   = bitStream.readBits(bitCount);
            ogg.bitWrite(number, bitCount);
            currentEntry += number;
        }

        if (currentEntry > entries)
        {
            throw new ParseException("Codebook current_entry out of range", bitStream);
        }
    }

    private static void copyLookupTable(RandomAccessReader bitStream, OGGStream ogg, int entries, int dimensions)
    {
        ogg.bitWrite(bitStream.readBits(32), 32);
        ogg.bitWrite(bitStream.readBits(32), 32);

        int valueLength = bitStream.readBits(4);
        ogg.bitWrite(valueLength, 4);
        ogg.writeBit(bitStream.readBits(1));

        int quantValues = getQuantValues(entries, dimensions);
        for (int i = 0; i < quantValues; i++)
        {
            int value = bitStream.readBits(valueLength + 1);
            ogg.bitWrite(value, valueLength + 1);
        }
    }

    /**
     * ilog from Tremor; the number of bits needed to store v
     */
    public static int ilog(int v)
    {
        return 32 - Integer.numberOfLeadingZeros(v);
    }

    // _book_maptype1_quantvals from Tremor
    private static int getQuantValues(int entries, int dimensions)
    {
        if (dimensions == 0 || entries == 0)
        {
            throw new ParseException("Invalid codebook lookup dimensions", -1);
        }

        int bits   = ilog(entries);
        int values = entries >> ((bits - 1) * (dimensions - 1) / dimensions);

        while (true)
        {
            long acc  = 1;
            long acc1 = 1;

            for (int i = 0; i < dimensions; i++)
            {
                acc *= values;
                acc1 *= values + 1;
            }

            if (acc <= entries && acc1 > entries)
            {
                return values;
            } else if (acc > entries)
            {
                values--;
            } else
            {
                values++;
            }
        }
    }

    /**
     * @return the size of the codebook, or -1 if there is no codebook with this id
     */
    private int getCodebookSize(int index)
    {
        if (index < 0 || index >= codebookCount - 1)
        {
            return -1;
        }

        return codebookOffsets[index + 1] - codebookOffsets[index];
    }
}

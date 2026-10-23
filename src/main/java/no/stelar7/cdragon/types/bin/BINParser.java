package no.stelar7.cdragon.types.bin;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.util.handlers.HashHandler;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.math.Vector2;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parser for PROP (version 1-3) and PTCH bin files.
 * <p>
 * Instances are stateless (apart from the optional, thread-safe, hash collection), so one parser can be shared between threads.
 * <p>
 * {@link #parse} throws {@link ParseException} if the data is not a bin file, or is a corrupt or unsupported bin file.
 * Use {@link #canParse(byte[])} to check the type first, or {@link #tryParse(Path)} to get null for non-bin data.
 */
public class BINParser implements Parseable<BINFile>
{
    /**
     * Since patch 16.21, HASH fields directly in objects of this class are 64-bit (see CDTB binfile.py)
     */
    public static final int WIDE_HASH_CLASS = 0xFF9D3409;

    private static final String NULL_HASH = "00000000";

    private final boolean                  collectHashes;
    private final boolean                  wideHashFields;
    private final Map<String, Set<String>> hashes = new ConcurrentHashMap<>();

    static
    {
        // Preload bin hashes
        HashHandler.getBinHashes();
    }

    public BINParser()
    {
        this(false);
    }

    /**
     * @param collectHashes if true, every hash seen while parsing is collected, see {@link #getCollectedHashes()}
     */
    public BINParser(boolean collectHashes)
    {
        this(collectHashes, true);
    }

    /**
     * @param collectHashes  if true, every hash seen while parsing is collected, see {@link #getCollectedHashes()}
     * @param wideHashFields if true, HASH fields in {@link #WIDE_HASH_CLASS} objects are read as 64-bit (patch 16.21+). Set to false for older files.
     */
    public BINParser(boolean collectHashes, boolean wideHashFields)
    {
        this.collectHashes = collectHashes;
        this.wideHashFields = wideHashFields;
    }

    /**
     * The hashes seen by this parser, by category (type, entry, value, string, wad, struct, offset).
     * Only filled if the parser was created with collectHashes = true.
     */
    public Map<String, Set<String>> getCollectedHashes()
    {
        return hashes;
    }

    private void collect(String category, String hash)
    {
        if (collectHashes)
        {
            hashes.computeIfAbsent(category, k -> ConcurrentHashMap.newKeySet()).add(hash);
        }
    }

    /**
     * True for PROP/PTCH files, and for headerless files that are a single container/structure covering the whole data.
     * For headerless data, the container/structure is read to check it, so this is only cheap for files with a header.
     */
    @Override
    public boolean canParse(byte[] data)
    {
        if (data == null || data.length < 4)
        {
            return false;
        }

        if (hasMagic(data))
        {
            return true;
        }

        return parseHeaderless(new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN), 0) != null;
    }

    private static boolean hasMagic(byte[] data)
    {
        String magic = new String(data, 0, 4, StandardCharsets.ISO_8859_1);
        return "PROP".equals(magic) || "PTCH".equals(magic);
    }

    /**
     * Parses the file, or returns null if it is not a bin file (eg. a .bin file in another format).
     * Use this when walking a folder of .bin files; corrupt or unsupported bin files still throw {@link ParseException}.
     */
    public BINFile tryParse(Path path)
    {
        byte[] data;
        try
        {
            data = Files.readAllBytes(path);
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        if (!canParse(data))
        {
            return null;
        }
        return parse(new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN));
    }

    /**
     * @throws ParseException if the data is not a bin file (see {@link #canParse(byte[])}), or is corrupt or unsupported
     */
    @Override
    public BINFile parse(RandomAccessReader raf)
    {
        long start = raf.pos();
        if (raf.remaining() < 4)
        {
            throw new ParseException("Not a BIN file (too small)", start);
        }

        String magic = raf.readString(4);
        if (!"PROP".equals(magic) && !"PTCH".equals(magic))
        {
            BINFile headerless = parseHeaderless(raf, start);
            if (headerless == null)
            {
                throw new ParseException("Not a BIN file (no PROP/PTCH magic, and not a headerless container/structure)", start);
            }
            return headerless;
        }

        try
        {
            BINFile file = new BINFile();
            file.setWideHashFields(wideHashFields);
            file.setHeader(parseHeader(file, raf, magic));
            parseEntries(file, raf);

            if (file.getHeader().isPatch() && file.getHeader().getVersion() >= 3)
            {
                parsePatches(file, raf);
            }
            return file;
        } catch (IndexOutOfBoundsException e)
        {
            throw new ParseException("Unexpected end of BIN data", raf.pos(), e);
        } catch (IllegalArgumentException e)
        {
            throw new ParseException("Invalid BIN data: " + e.getMessage(), raf.pos(), e);
        }
    }

    /**
     * Some bin files are just a single container or structure, without a header.
     * Only data where the size field of the container/structure exactly covers the rest of the data is considered.
     */
    private BINFile parseHeaderless(RandomAccessReader raf, long start)
    {
        long         size = raf.size() - start;
        BINValueType type = null;

        if (size >= 9)
        {
            raf.seek(start);
            BINValueType elementType = BINValueType.fromRaw(raf.readByte());
            long         declared    = raf.readU32();
            if (elementType != null && declared == size - 5)
            {
                type = BINValueType.CONTAINER;
            }
        }

        if (type == null && size >= 10)
        {
            raf.seek(start);
            int  hash     = raf.readInt();
            long declared = raf.readU32();
            if (hash != 0 && declared == size - 8)
            {
                type = BINValueType.STRUCTURE;
            }
        }

        if (type == null)
        {
            return null;
        }

        Object data;
        try
        {
            raf.seek(start);
            data = readByType(type, raf);
        } catch (ParseException | IndexOutOfBoundsException | IllegalArgumentException e)
        {
            return null;
        }

        if (raf.hasMoreBytes())
        {
            return null;
        }

        BINValue value = new BINValue(NULL_HASH, type, data);

        List<BINValue> values = new ArrayList<>();
        values.add(value);
        BINEntry entry = new BINEntry(0, "headerless container", NULL_HASH, (short) 1, values);

        BINFile file = new BINFile();
        file.getEntries().add(entry);
        return file;
    }

    private BINHeader parseHeader(BINFile file, RandomAccessReader raf, String magic)
    {
        boolean isPatch      = false;
        int     patchVersion = 0;
        int     patchUnknown = 0;

        if ("PTCH".equals(magic))
        {
            isPatch = true;
            patchVersion = raf.readInt();
            patchUnknown = raf.readInt();

            magic = raf.readString(4);
            if (!"PROP".equals(magic))
            {
                throw new ParseException("Expected PROP after PTCH header, got " + magic, raf.pos() - 4);
            }
        }

        int version = raf.readInt();
        if (version < 1 || version > 3)
        {
            throw new ParseException("Unsupported BIN version " + version, raf.pos() - 4);
        }

        if (version >= 2)
        {
            long fileCount = raf.readU32();
            checkCount(raf, fileCount, Short.BYTES);
            for (int i = 0; i < fileCount; i++)
            {
                file.getLinkedFiles().add(raf.readShortString());
            }
        }

        long entryCount = raf.readU32();
        checkCount(raf, entryCount, Integer.BYTES);
        List<Integer> entryTypes = new ArrayList<>();
        for (int i = 0; i < entryCount; i++)
        {
            entryTypes.add(raf.readInt());
        }

        return new BINHeader(magic, version, (int) entryCount, isPatch, entryTypes, patchVersion, patchUnknown);
    }

    private void parseEntries(BINFile file, RandomAccessReader raf)
    {
        for (int i = 0; i < file.getHeader().getEntryCount(); i++)
        {
            int entryType = file.getHeader().getEntryTypes().get(i);

            String typeHash = HashHandler.getBINHash(entryType);
            int    length   = raf.readInt();
            long   start    = raf.pos();

            String entryHash  = HashHandler.getBINHash(raf.readInt());
            short  valueCount = raf.readShort();

            collect("type", typeHash);
            collect("entry", entryHash);

            int            count  = Short.toUnsignedInt(valueCount);
            List<BINValue> values = new ArrayList<>(count);
            for (int j = 0; j < count; j++)
            {
                values.add(readValue(raf, entryType));
            }

            checkSize(raf, "entry " + entryHash, start, Integer.toUnsignedLong(length));
            file.getEntries().add(new BINEntry(length, typeHash, entryHash, valueCount, values));
        }
    }

    private BINValue readValue(RandomAccessReader raf, int parentClass)
    {
        String valueHash = HashHandler.getBINHash(raf.readInt());
        collect("value", valueHash);
        BINValueType type = readType(raf);

        Object value;
        if (wideHashFields && parentClass == WIDE_HASH_CLASS && type == BINValueType.STRING_HASH)
        {
            value = HashHandler.toHex(raf.readLong(), 16);
        } else
        {
            value = readByType(type, raf);
        }

        return new BINValue(valueHash, type, value);
    }

    private BINValueType readType(RandomAccessReader raf)
    {
        byte         raw  = raf.readByte();
        BINValueType type = BINValueType.fromRaw(raw);
        if (type == null)
        {
            throw new ParseException("Unknown BIN value type " + Byte.toUnsignedInt(raw), raf.pos() - 1);
        }
        return type;
    }

    private Object readByType(BINValueType type, RandomAccessReader raf)
    {
        switch (type)
        {
            case V3_SHORT:
                return raf.readVec3S();
            case BOOLEAN:
            case BOOLEAN_FLAGS:
                return raf.readBoolean();
            case SIGNED_BYTE:
            case BYTE:
                return raf.readByte();
            case SIGNED_SHORT:
            case SHORT:
                return raf.readShort();
            case SIGNED_INT:
            case INT:
                return raf.readInt();
            case SIGNED_LONG:
            case LONG:
                return raf.readLong();
            case FLOAT:
                return raf.readFloat();
            case V2_FLOAT:
                return raf.readVec2F();
            case V3_FLOAT:
                return raf.readVec3F();
            case V4_FLOAT:
                return raf.readVec4F();
            case M4X4_FLOAT:
                return raf.readMatrix4x4();
            case RGBA_BYTE:
                return raf.readVec4B();
            case STRING:
            {
                int length = raf.readU16();
                checkCount(raf, length, 1);
                return raf.readString(length);
            }
            case STRING_HASH:
            {
                String hash = HashHandler.getBINHash(raf.readInt());
                collect("string", hash);
                return hash;
            }
            case WAD_LINK:
            {
                String hexed = HashHandler.toHex(raf.readLong(), 16);
                collect("wad", hexed);
                return hexed;
            }
            case CONTAINER:
            case CONTAINER2:
            {
                BINValueType itemType = readType(raf);
                int          size     = raf.readInt();
                long         start    = raf.pos();
                int          count    = raf.readInt();
                checkCount(raf, Integer.toUnsignedLong(count), 1);

                List<Object> data = new ArrayList<>();
                for (int i = 0; i < count; i++)
                {
                    data.add(readByType(itemType, raf));
                }

                checkSize(raf, "container", start, Integer.toUnsignedLong(size));
                return new BINContainer(itemType, size, count, data);
            }
            case STRUCTURE:
            case EMBEDDED:
            {
                int    classHash = raf.readInt();
                String hash      = HashHandler.getBINHash(classHash);

                // a null pointer has no size or fields, embedded values are always present
                if (classHash == 0 && type == BINValueType.STRUCTURE)
                {
                    return new BINStruct(hash, 0, (short) 0, new ArrayList<>());
                }

                collect("struct", hash);
                int   size       = raf.readInt();
                long  start      = raf.pos();
                short fieldCount = raf.readShort();

                int            count = Short.toUnsignedInt(fieldCount);
                List<BINValue> data  = new ArrayList<>(count);
                for (int i = 0; i < count; i++)
                {
                    data.add(readValue(raf, classHash));
                }

                checkSize(raf, "structure " + hash, start, Integer.toUnsignedLong(size));
                return new BINStruct(hash, size, fieldCount, data);
            }
            case LINK_OFFSET:
            {
                String hash = HashHandler.getBINHash(raf.readInt());
                collect("offset", hash);
                return hash;
            }
            case OPTIONAL_DATA:
            {
                BINValueType itemType = readType(raf);
                byte         count    = raf.readByte();
                List<Object> data     = new ArrayList<>();
                if (count != 0)
                {
                    data.add(readByType(itemType, raf));
                }

                return new BINData(itemType, count, data);
            }
            case PAIR:
            {
                BINValueType keyType   = readType(raf);
                BINValueType valueType = readType(raf);
                int          size      = raf.readInt();
                long         start     = raf.pos();
                int          count     = raf.readInt();
                checkCount(raf, Integer.toUnsignedLong(count), 2);

                List<Vector2<Object, Object>> data = new ArrayList<>();
                for (int i = 0; i < count; i++)
                {
                    Object key   = readByType(keyType, raf);
                    Object value = readByType(valueType, raf);
                    data.add(new Vector2<>(key, value));
                }

                checkSize(raf, "map", start, Integer.toUnsignedLong(size));
                return new BINMap(keyType, valueType, size, count, data);

            }
            default:
                throw new ParseException("Unknown BIN value type " + type, raf.pos());
        }
    }

    private void parsePatches(BINFile file, RandomAccessReader raf)
    {
        long patchCount = raf.readU32();
        checkCount(raf, patchCount, 11);
        for (int i = 0; i < patchCount; i++)
        {
            int  hash        = raf.readInt();
            long patchLength = raf.readU32();
            long start       = raf.pos();

            BINValueType type  = readType(raf);
            String       name  = raf.readShortString();
            Object       value = readByType(type, raf);

            checkSize(raf, "patch " + name, start, patchLength);
            file.getPatches().add(new BINPatchEntry(hash, name, type, value));
        }
    }

    /**
     * Makes sure count items of at least minItemSize bytes can fit in the remaining data
     */
    private static void checkCount(RandomAccessReader raf, long count, int minItemSize)
    {
        if (count < 0 || count * minItemSize > raf.remaining())
        {
            throw new ParseException("Invalid BIN count " + count + ", only " + raf.remaining() + " bytes left", raf.pos());
        }
    }

    private static void checkSize(RandomAccessReader raf, String what, long start, long expected)
    {
        long actual = raf.pos() - start;
        if (actual != expected)
        {
            throw new ParseException("Wrong size for " + what + ": read " + actual + " bytes, expected " + expected, raf.pos());
        }
    }
}

package no.stelar7.cdragon.types.rman.data;

import com.google.common.primitives.Longs;
import no.stelar7.cdragon.util.readers.*;

import java.util.*;

/**
 * Reads FlatBuffers tables, as used by the body of RMAN files.
 * <p>
 * A table starts with an int32 (soffset) pointing backwards to its vtable.
 * The vtable is: u16 vtable size (in bytes, including these 4 bytes), u16 table size, then (vtableSize - 4) / 2 u16 field offsets.
 * A field whose index is past the end of the vtable, or whose offset is 0, is absent.
 */
public class RMANVTable
{
    /**
     * A FlatBuffers table at a fixed position in the reader.
     * Every accessor seeks, so the reader position is undefined after using it.
     */
    public static final class Table
    {
        private final RandomAccessReader raf;
        private final long               tablePos;
        private final int                vtableOffset;
        private final int[]              fieldOffsets;

        private Table(RandomAccessReader raf, long tablePos, int vtableOffset, int[] fieldOffsets)
        {
            this.raf = raf;
            this.tablePos = tablePos;
            this.vtableOffset = vtableOffset;
            this.fieldOffsets = fieldOffsets;
        }

        /**
         * Reads the table that starts at the current position of the reader
         */
        public static Table read(RandomAccessReader raf)
        {
            long tablePos     = raf.pos();
            int  vtableOffset = raf.readInt();
            long vtablePos    = tablePos - vtableOffset;
            if (vtablePos < 0 || vtablePos + 4 > raf.size())
            {
                throw new ParseException("Invalid vtable offset " + vtableOffset, tablePos);
            }

            raf.seek(vtablePos);
            int vtableSize = raf.readU16();
            raf.readU16(); // size of the table itself
            if (vtableSize < 4 || (vtableSize & 1) != 0 || vtablePos + vtableSize > raf.size())
            {
                throw new ParseException("Invalid vtable size " + vtableSize, vtablePos);
            }

            int[] fieldOffsets = raf.readU16Array((vtableSize - 4) / 2);
            return new Table(raf, tablePos, vtableOffset, fieldOffsets);
        }

        public long getPosition()
        {
            return tablePos;
        }

        /**
         * The raw soffset at the start of the table
         */
        public int getVTableOffset()
        {
            return vtableOffset;
        }

        public int getFieldCount()
        {
            return fieldOffsets.length;
        }

        public boolean has(int field)
        {
            return field < fieldOffsets.length && fieldOffsets[field] != 0;
        }

        /**
         * Seeks to the field, returns false if it is absent
         */
        private boolean seek(int field)
        {
            if (!has(field))
            {
                return false;
            }
            raf.seek(tablePos + fieldOffsets[field]);
            return true;
        }

        public int u8(int field, int defaultValue)
        {
            return seek(field) ? raf.readU8() : defaultValue;
        }

        public int u16(int field, int defaultValue)
        {
            return seek(field) ? raf.readU16() : defaultValue;
        }

        public int i32(int field, int defaultValue)
        {
            return seek(field) ? raf.readInt() : defaultValue;
        }

        public long u32(int field, long defaultValue)
        {
            return seek(field) ? raf.readU32() : defaultValue;
        }

        public long u64(int field, long defaultValue)
        {
            return seek(field) ? raf.readLong() : defaultValue;
        }

        /**
         * Absolute position of the object (vector/table/string) referenced by the field, or -1 if absent
         */
        public long offset(int field)
        {
            if (!seek(field))
            {
                return -1;
            }
            // FlatBuffers offsets are unsigned and point forward, but CDTB and moonshadow565/rman read them as int32; do the same
            long pos = raf.pos();
            return pos + raf.readInt();
        }

        public String string(int field, String defaultValue)
        {
            long pos = offset(field);
            if (pos < 0)
            {
                return defaultValue;
            }
            raf.seek(pos);
            return raf.readString(raf.readU32());
        }

        public long[] u64Vector(int field)
        {
            long pos = offset(field);
            if (pos < 0)
            {
                return new long[0];
            }
            raf.seek(pos);
            long count = raf.readU32();
            if (count * Long.BYTES > raf.remaining())
            {
                throw new ParseException("Vector of " + count + " u64 is larger than the data", pos);
            }
            return raf.readLongArray((int) count);
        }
    }

    /**
     * Generic (slow) table reader, returns the fields in tableDefinition by index.
     * Absent fields are given a default value (0, "" or an empty list), fields of type "0" are skipped.
     */
    public static Map<String, Object> parseVTable(RandomAccessReader raf, Map<String, String> tableDefinition)
    {
        Table               table = Table.read(raf);
        Map<String, Object> data  = new HashMap<>();

        int i = 0;
        for (Map.Entry<String, String> entry : tableDefinition.entrySet())
        {
            int    index = i++;
            String key   = entry.getKey();
            switch (entry.getValue())
            {
                case "0" ->
                {
                    // Do nothing
                }
                case "1" -> data.put(key, table.u8(index, 0));
                case "2" -> data.put(key, (short) table.u16(index, 0));
                case "4" -> data.put(key, table.i32(index, 0));
                case "u4" -> data.put(key, table.u32(index, 0));
                case "8" -> data.put(key, table.u64(index, 0));
                case "offset4" -> data.put(key, (int) table.offset(index));
                case "list8" -> data.put(key, Longs.asList(table.u64Vector(index)));
                case "string" -> data.put(key, table.string(index, ""));
                default -> throw new IllegalArgumentException("Unhandled read size: " + key + ": " + entry.getValue());
            }
        }

        return data;
    }

    private static Map<String, String> fields(String... nameAndType)
    {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < nameAndType.length; i += 2)
        {
            map.put(nameAndType[i], nameAndType[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }

    // field indexes, in vtable order (matching CDTB's patcher.py)
    public static final int BODY_BUNDLES     = 0;
    public static final int BODY_LANGUAGES   = 1;
    public static final int BODY_FILES       = 2;
    public static final int BODY_DIRECTORIES = 3;
    public static final int BODY_KEYS        = 4;
    public static final int BODY_UNKNOWN     = 5;

    public static final int BUNDLE_ID     = 0;
    public static final int BUNDLE_CHUNKS = 1;

    public static final int CHUNK_ID                = 0;
    public static final int CHUNK_COMPRESSED_SIZE   = 1;
    public static final int CHUNK_UNCOMPRESSED_SIZE = 2;

    public static final int LANGUAGE_ID   = 0;
    public static final int LANGUAGE_NAME = 1;

    public static final int FILE_ID           = 0;
    public static final int FILE_DIRECTORY_ID = 1;
    public static final int FILE_SIZE         = 2;
    public static final int FILE_NAME         = 3;
    public static final int FILE_LANGUAGES    = 4;
    public static final int FILE_CHUNKS       = 7;
    public static final int FILE_SYMLINK      = 9;
    public static final int FILE_PERMISSIONS  = 12;

    public static final int DIRECTORY_ID        = 0;
    public static final int DIRECTORY_PARENT_ID = 1;
    public static final int DIRECTORY_NAME      = 2;

    private static final Map<String, String> BUNDLE_FIELDS    = fields("bundle_id", "8", "chunks_offset", "offset4");
    private static final Map<String, String> CHUNK_FIELDS     = fields("chunk_id", "8", "compressed_size", "u4", "uncompressed_size", "u4");
    private static final Map<String, String> DIRECTORY_FIELDS = fields("directory_id", "8", "parent_id", "8", "name", "string");
    private static final Map<String, String> LANGUAGE_FIELDS  = fields("language_id", "1", "name", "string");
    private static final Map<String, String> FILE_FIELDS      = fields("file_id", "8",
                                                                       "directory_id", "8",
                                                                       "file_size", "8",
                                                                       "name", "string",
                                                                       "language_id", "8",
                                                                       "unknown", "0",
                                                                       "unknown1", "0",
                                                                       "chunks", "list8",
                                                                       "unknown2", "0",
                                                                       "symlink", "string",
                                                                       "unknown3", "0",
                                                                       "unknown4", "0",
                                                                       "unknown5", "0");

    public static Map<String, String> getBundleFields()
    {
        return BUNDLE_FIELDS;
    }

    public static Map<String, String> getChunkFields()
    {
        return CHUNK_FIELDS;
    }

    public static Map<String, String> getDirectoryFields()
    {
        return DIRECTORY_FIELDS;
    }

    public static Map<String, String> getLanguageFields()
    {
        return LANGUAGE_FIELDS;
    }

    public static Map<String, String> getFileFields()
    {
        return FILE_FIELDS;
    }
}

package no.stelar7.cdragon.types.bin.data;

import com.google.gson.*;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.math.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class BINFile
{
    private BINHeader           header;
    private List<BINEntry>      entries     = new ArrayList<>();
    private List<String>        linkedFiles = new ArrayList<>();
    private List<BINPatchEntry> patches     = new ArrayList<>();
    private String              json;
    
    // whether HASH fields in BINParser.WIDE_HASH_CLASS objects are 64-bit, must match how the file was parsed
    private transient boolean wideHashFields = true;

    public BINEntry getIfPresent(String hash)
    {
        return entries.stream().filter(v -> v.getHash().equalsIgnoreCase(hash)).findFirst().get();
    }

    public Optional<BINEntry> get(String hash)
    {
        return entries.stream().filter(v -> v.getHash().equalsIgnoreCase(hash)).findFirst();
    }

    public List<BINEntry> getByType(String type)
    {
        return entries.stream().filter(v -> v.getType().equalsIgnoreCase(type)).collect(Collectors.toList());
    }

    public Stream<BINEntry> stream()
    {
        return entries.stream();
    }

    public BINHeader getHeader()
    {
        return header;
    }

    public void setHeader(BINHeader header)
    {
        this.header = header;
    }

    public List<BINEntry> getEntries()
    {
        return entries;
    }

    public void setEntries(List<BINEntry> entries)
    {
        this.entries = entries;
    }

    public List<String> getLinkedFiles()
    {
        return linkedFiles;
    }

    public void setLinkedFiles(List<String> linkedFiles)
    {
        this.linkedFiles = linkedFiles;
    }

    public List<BINPatchEntry> getPatches()
    {
        return patches;
    }

    public void setPatches(List<BINPatchEntry> patches)
    {
        this.patches = patches;
    }

    public boolean isWideHashFields()
    {
        return wideHashFields;
    }
    
    public void setWideHashFields(boolean wideHashFields)
    {
        this.wideHashFields = wideHashFields;
    }
    
    public void write(Path output)
    {
        try
        {
            Files.write(output, toBytes());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Serializes this file, using the version (and PTCH header) it was read with.
     * Sizes and counts are computed from the data, so modified files are written correctly.
     */
    public byte[] toBytes()
    {
        Output out = new Output();

        if (header == null)
        {
            // headerless container, just write the single value
            if (entries.size() != 1 || entries.get(0).getValues().size() != 1)
            {
                throw new IllegalStateException("A headerless bin file must contain exactly one value");
            }
            BINValue value = entries.get(0).getValues().get(0);
            writeBinValue(value.getType(), value.getValue(), out);
            return out.toByteArray();
        }

        int version = header.getVersion() == 0 ? 2 : header.getVersion();

        if (header.isPatch())
        {
            out.writeAscii("PTCH");
            out.writeInt(header.getPatchVersion() == 0 ? 1 : header.getPatchVersion());
            out.writeInt(header.getPatchUnknown());
        }

        out.writeAscii("PROP");
        out.writeInt(version);

        if (version >= 2)
        {
            out.writeInt(linkedFiles.size());
            for (String s : linkedFiles)
            {
                out.writeShortString(s);
            }
        }

        int[] entryTypes = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++)
        {
            entryTypes[i] = hashToInt(entries.get(i).getType());
        }

        out.writeInt(entries.size());
        for (int type : entryTypes)
        {
            out.writeInt(type);
        }

        for (int i = 0; i < entries.size(); i++)
        {
            BINEntry e = entries.get(i);

            int lengthPos = out.reserveInt();
            out.writeInt(hashToInt(e.getHash()));
            out.writeShort(e.getValues().size());
            for (BINValue v : e.getValues())
            {
                writeField(v, entryTypes[i], out);
            }
            out.patchSize(lengthPos);
        }

        if (header.isPatch() && version >= 3)
        {
            out.writeInt(patches.size());
            for (BINPatchEntry patch : patches)
            {
                if (patch.getType() == null)
                {
                    throw new IllegalStateException("Patch entry " + patch.getName() + " has no type, so it can not be written");
                }

                out.writeInt(hashToInt(patch.getHash()));
                int lengthPos = out.reserveInt();
                out.writeByte(patch.getType().toRaw());
                out.writeShortString(patch.getName());
                writeBinValue(patch.getType(), patch.getValue(), out);
                out.patchSize(lengthPos);
            }
        }

        return out.toByteArray();
    }

    private void writeField(BINValue v, int parentClass, Output out)
    {
        out.writeInt(hashToInt(v.getHash()));
        out.writeByte(v.getType().toRaw());

        if (wideHashFields && v.getType() == BINValueType.STRING_HASH && parentClass == BINParser.WIDE_HASH_CLASS)
        {
            String value = String.valueOf(v.getValue());
            out.writeLong(isHex(value, 16) ? Long.parseUnsignedLong(value, 16) : Integer.toUnsignedLong(hashToInt(value)));
            return;
        }

        writeBinValue(v.getType(), v.getValue(), out);
    }

    private void writeBinValue(BINValueType type, Object datapoint, Output out)
    {
        switch (type)
        {
            case BOOLEAN:
            case BOOLEAN_FLAGS:
            {
                out.writeByte((Boolean) datapoint ? 1 : 0);
                break;
            }
            case BYTE:
            case SIGNED_BYTE:
            {
                out.writeByte(((Number) datapoint).byteValue());
                break;
            }
            case SHORT:
            case SIGNED_SHORT:
            {
                out.writeShort(((Number) datapoint).shortValue());
                break;
            }
            case INT:
            case SIGNED_INT:
            {
                out.writeInt(((Number) datapoint).intValue());
                break;
            }
            case LONG:
            case SIGNED_LONG:
            {
                out.writeLong(((Number) datapoint).longValue());
                break;
            }
            case FLOAT:
            {
                out.writeFloat(((Number) datapoint).floatValue());
                break;
            }
            case V2_FLOAT:
            {
                Vector2f v = (Vector2f) datapoint;
                out.writeFloat(v.x);
                out.writeFloat(v.y);
                break;
            }
            case V3_FLOAT:
            {
                Vector3f v = (Vector3f) datapoint;
                out.writeFloat(v.x);
                out.writeFloat(v.y);
                out.writeFloat(v.z);
                break;
            }
            case V3_SHORT:
            {
                Vector3s v = (Vector3s) datapoint;
                out.writeShort(v.getX());
                out.writeShort(v.getY());
                out.writeShort(v.getZ());
                break;
            }
            case V4_FLOAT:
            {
                Vector4f v = (Vector4f) datapoint;
                out.writeFloat(v.x);
                out.writeFloat(v.y);
                out.writeFloat(v.z);
                out.writeFloat(v.w);
                break;
            }
            case M4X4_FLOAT:
            {
                Matrix4f m = (Matrix4f) datapoint;
                float[]  f = {
                        m.m00(), m.m01(), m.m02(), m.m03(),
                        m.m10(), m.m11(), m.m12(), m.m13(),
                        m.m20(), m.m21(), m.m22(), m.m23(),
                        m.m30(), m.m31(), m.m32(), m.m33(),
                        };
                for (float value : f)
                {
                    out.writeFloat(value);
                }
                break;
            }
            case RGBA_BYTE:
            {
                Vector4b v = (Vector4b) datapoint;
                out.writeByte(v.getX());
                out.writeByte(v.getY());
                out.writeByte(v.getZ());
                out.writeByte(v.getW());
                break;
            }
            case STRING:
            {
                out.writeShortString((String) datapoint);
                break;
            }
            case STRING_HASH:
            case LINK_OFFSET:
            {
                out.writeInt(hashToInt(String.valueOf(datapoint)));
                break;
            }
            case WAD_LINK:
            {
                out.writeLong(wadHashToLong(String.valueOf(datapoint)));
                break;
            }
            case CONTAINER:
            case CONTAINER2:
            {
                BINContainer bc = (BINContainer) datapoint;
                out.writeByte(bc.getType().toRaw());
                int sizePos = out.reserveInt();
                out.writeInt(bc.getData().size());
                bc.getData().forEach(bv -> writeBinValue(bc.getType(), bv, out));
                out.patchSize(sizePos);
                break;
            }
            case STRUCTURE:
            case EMBEDDED:
            {
                BINStruct bs        = (BINStruct) datapoint;
                int       classHash = hashToInt(bs.getHash());
                out.writeInt(classHash);

                // a null pointer has no size or fields
                if (classHash == 0 && type == BINValueType.STRUCTURE)
                {
                    break;
                }

                int sizePos = out.reserveInt();
                out.writeShort(bs.getData().size());
                bs.getData().forEach(bv -> writeField(bv, classHash, out));
                out.patchSize(sizePos);
                break;
            }
            case OPTIONAL_DATA:
            {
                BINData bd = (BINData) datapoint;
                out.writeByte(bd.getType().toRaw());
                out.writeByte(bd.getData().isEmpty() ? 0 : 1);
                if (!bd.getData().isEmpty())
                {
                    writeBinValue(bd.getType(), bd.getData().get(0), out);
                }
                break;
            }
            case PAIR:
            {
                BINMap bm = (BINMap) datapoint;
                out.writeByte(bm.getType1().toRaw());
                out.writeByte(bm.getType2().toRaw());
                int sizePos = out.reserveInt();
                out.writeInt(bm.getData().size());
                bm.getData().forEach(bv -> {
                    writeBinValue(bm.getType1(), bv.getFirst(), out);
                    writeBinValue(bm.getType2(), bv.getSecond(), out);
                });
                out.patchSize(sizePos);
                break;
            }
            default:
                throw new IllegalStateException("Unable to write bin value of type " + type);
        }
    }

    private static boolean isHex(String s, int length)
    {
        if (s == null || s.length() != length)
        {
            return false;
        }

        for (int i = 0; i < s.length(); i++)
        {
            if (Character.digit(s.charAt(i), 16) < 0)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Turns a value from {@link HashHandler#getBINHash(int)} (either the 8 char hex hash, or the unhashed name) back into the hash
     */
    static int hashToInt(String value)
    {
        if (isHex(value, 8))
        {
            // the hex form is only ambiguous if the value is also the known name of its own FNV hash
            String asName = HashHandler.getBinHashes().get(HashHandler.toHex(HashHandler.computeFNV1A(value), 8));
            if (!value.equals(asName))
            {
                return Integer.parseUnsignedInt(value, 16);
            }
        }

        return (int) HashHandler.computeFNV1A(value);
    }

    private static long wadHashToLong(String value)
    {
        if (isHex(value, 16))
        {
            return Long.parseUnsignedLong(value, 16);
        }
        return HashHandler.computeXXHash64AsLong(value.toLowerCase(Locale.ENGLISH));
    }

    public String toJson()
    {
        if (json == null)
        {
            JsonObject root = new JsonObject();
            for (BINEntry entry : entries)
            {
                JsonArray byType = root.getAsJsonArray(entry.getType());
                if (byType == null)
                {
                    byType = new JsonArray();
                    root.add(entry.getType(), byType);
                }

                JsonObject entryJson = new JsonObject();
                entryJson.add(entry.getHash(), fieldsToJson(entry.getValues()));
                byType.add(entryJson);
            }

            JsonArray links = new JsonArray();
            for (String link : this.linkedFiles)
            {
                links.add(link);
            }

            JsonArray patchJson = new JsonArray();
            for (BINPatchEntry entry : this.patches)
            {
                JsonObject obj = new JsonObject();
                obj.addProperty("hash", entry.getHash());
                obj.addProperty("name", entry.getName());
                if (entry.getType() != null)
                {
                    obj.addProperty("type", entry.getType().name());
                    obj.add("value", toJsonValue(entry.getType(), entry.getValue()));
                } else
                {
                    obj.add("value", UtilHandler.getGson().toJsonTree(entry.getValue()));
                }
                patchJson.add(obj);
            }

            root.add("linkedBinFiles", links);
            root.add("patches", patchJson);

            json = UtilHandler.getGson().toJson(root);
        }

        return json;
    }

    private JsonObject fieldsToJson(List<BINValue> values)
    {
        JsonObject obj = new JsonObject();
        for (BINValue value : values)
        {
            obj.add(value.getHash(), toJsonValue(value.getType(), value.getValue()));
        }
        return obj;
    }

    private JsonElement toJsonValue(BINValueType type, Object data)
    {
        if (data == null)
        {
            return JsonNull.INSTANCE;
        }

        switch (type)
        {
            case STRING:
                return new JsonPrimitive(data.toString());
            case BOOLEAN:
            case BOOLEAN_FLAGS:
                return new JsonPrimitive((Boolean) data);
            case SIGNED_BYTE:
            case SIGNED_SHORT:
            case SIGNED_INT:
            case SIGNED_LONG:
            case FLOAT:
                return new JsonPrimitive((Number) data);
            case BYTE:
                return new JsonPrimitive(Byte.toUnsignedInt(((Number) data).byteValue()));
            case SHORT:
                return new JsonPrimitive(Short.toUnsignedInt(((Number) data).shortValue()));
            case INT:
                return new JsonPrimitive(Integer.toUnsignedLong(((Number) data).intValue()));
            case LONG:
                return new JsonPrimitive(new BigInteger(Long.toUnsignedString(((Number) data).longValue())));
            case V2_FLOAT:
            {
                Vector2f v = (Vector2f) data;
                return vector(v.x, v.y);
            }
            case V3_FLOAT:
            {
                Vector3f v = (Vector3f) data;
                return vector(v.x, v.y, v.z);
            }
            case V4_FLOAT:
            {
                Vector4f v = (Vector4f) data;
                return vector(v.x, v.y, v.z, v.w);
            }
            case V3_SHORT:
            {
                Vector3s v = (Vector3s) data;
                return vector(v.getX(), v.getY(), v.getZ());
            }
            case RGBA_BYTE:
            {
                Vector4b v = (Vector4b) data;
                return vector(Byte.toUnsignedInt(v.getX()), Byte.toUnsignedInt(v.getY()), Byte.toUnsignedInt(v.getZ()), Byte.toUnsignedInt(v.getW()));
            }
            case M4X4_FLOAT:
            {
                Matrix4f  m    = (Matrix4f) data;
                JsonArray rows = new JsonArray();
                rows.add(row(m.m00(), m.m01(), m.m02(), m.m03()));
                rows.add(row(m.m10(), m.m11(), m.m12(), m.m13()));
                rows.add(row(m.m20(), m.m21(), m.m22(), m.m23()));
                rows.add(row(m.m30(), m.m31(), m.m32(), m.m33()));
                return rows;
            }
            case CONTAINER:
            case CONTAINER2:
            {
                BINContainer bc  = (BINContainer) data;
                JsonArray    arr = new JsonArray();
                for (Object o : bc.getData())
                {
                    arr.add(toJsonValue(bc.getType(), o));
                }
                return arr;
            }
            case STRUCTURE:
            case EMBEDDED:
            {
                BINStruct  bs  = (BINStruct) data;
                JsonObject obj = new JsonObject();
                obj.add(bs.getHash(), fieldsToJson(bs.getData()));
                return obj;
            }
            case OPTIONAL_DATA:
            {
                BINData   bd  = (BINData) data;
                JsonArray arr = new JsonArray();
                for (Object o : bd.getData())
                {
                    arr.add(toJsonValue(bd.getType(), o));
                }
                return arr;
            }
            case PAIR:
            {
                BINMap     bm  = (BINMap) data;
                JsonObject obj = new JsonObject();
                for (Vector2<Object, Object> pair : bm.getData())
                {
                    JsonElement key   = toJsonValue(bm.getType1(), pair.getFirst());
                    String      name  = key.isJsonPrimitive() ? key.getAsString() : key.toString();
                    obj.add(name, toJsonValue(bm.getType2(), pair.getSecond()));
                }
                return obj;
            }
            case LINK_OFFSET:
            {
                String other = data.toString();
                return new JsonPrimitive("LINK_OFFSET: " + HashHandler.getBinHashes().getOrDefault(other, other));
            }
            case WAD_LINK:
            {
                String other = data.toString();
                return new JsonPrimitive("WAD_LINK: " + HashHandler.getWADHashes().getOrDefault(other, other));
            }
            case STRING_HASH:
            {
                String other    = data.toString();
                String testHash = HashHandler.getBinHashes().getOrDefault(other, other);
                String output   = (other.equalsIgnoreCase(testHash)) ? "STRING_HASH: " : "";
                return new JsonPrimitive(output + testHash);
            }
            default:
                return UtilHandler.getGson().toJsonTree(data);
        }
    }

    private static JsonObject vector(Number... values)
    {
        String[]   names = {"x", "y", "z", "w"};
        JsonObject obj   = new JsonObject();
        for (int i = 0; i < values.length; i++)
        {
            obj.addProperty(names[i], values[i]);
        }
        return obj;
    }

    private static JsonArray row(float... values)
    {
        JsonArray arr = new JsonArray();
        for (float value : values)
        {
            arr.add(value);
        }
        return arr;
    }

    /**
     * Minimal little endian output that can go back and fill in size fields
     */
    private static final class Output
    {
        private byte[] data = new byte[1024];
        private int    size = 0;

        private void ensure(int extra)
        {
            if (size + extra > data.length)
            {
                data = Arrays.copyOf(data, Math.max(data.length * 2, size + extra));
            }
        }

        void writeByte(int value)
        {
            ensure(1);
            data[size++] = (byte) value;
        }

        void writeShort(int value)
        {
            writeByte(value);
            writeByte(value >>> 8);
        }

        void writeInt(int value)
        {
            ensure(4);
            putInt(size, value);
            size += 4;
        }

        void writeLong(long value)
        {
            writeInt((int) value);
            writeInt((int) (value >>> 32));
        }

        void writeFloat(float value)
        {
            writeInt(Float.floatToRawIntBits(value));
        }

        void writeAscii(String value)
        {
            writeBytes(value.getBytes(StandardCharsets.US_ASCII));
        }

        void writeShortString(String value)
        {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > 0xFFFF)
            {
                throw new IllegalStateException("String is too long for a bin file (" + bytes.length + " bytes)");
            }
            writeShort(bytes.length);
            writeBytes(bytes);
        }

        void writeBytes(byte[] bytes)
        {
            ensure(bytes.length);
            System.arraycopy(bytes, 0, data, size, bytes.length);
            size += bytes.length;
        }

        /**
         * Writes a placeholder u32 and returns its position
         */
        int reserveInt()
        {
            int pos = size;
            writeInt(0);
            return pos;
        }

        /**
         * Fills the placeholder at pos with the number of bytes written after it
         */
        void patchSize(int pos)
        {
            putInt(pos, size - (pos + 4));
        }

        private void putInt(int pos, int value)
        {
            data[pos] = (byte) value;
            data[pos + 1] = (byte) (value >>> 8);
            data[pos + 2] = (byte) (value >>> 16);
            data[pos + 3] = (byte) (value >>> 24);
        }

        byte[] toByteArray()
        {
            return Arrays.copyOf(data, size);
        }
    }
}

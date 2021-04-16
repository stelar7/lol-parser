package no.stelar7.cdragon.types.bin.data;

import com.google.gson.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.math.*;
import no.stelar7.cdragon.util.writers.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class BINFile
{
    private BINHeader      header;
    private List<BINEntry> entries     = new ArrayList<>();
    private List<String>   linkedFiles = new ArrayList<>();
    
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
    
    private String json;
    
    public void write(Path output)
    {
        try (ByteWriter bw = new ByteWriter())
        {
            bw.writeString("PROP");
            bw.writeInt(2);
            bw.writeInt(this.getLinkedFiles().size());
            for (String s : this.getLinkedFiles())
            {
                bw.writeStringWithLength(s);
            }
            bw.writeInt(this.header.getEntryCount());
            for (Integer i : this.header.getEntryTypes())
            {
                bw.writeInt(i);
            }
            
            this.entries.forEach(e -> {
                bw.writeInt(e.getLength());
                bw.writeInt(Integer.parseInt(HashHandler.getBinKeyForHash(e.getHash()), 16));
                bw.writeShort(e.getValueCount());
                e.getValues().forEach(v -> writeBinValue(v.getType(), v, bw, true));
            });
            
            Files.write(output, bw.toByteArray());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void writeBinValue(BINValueType type, Object v, ByteWriter bw, boolean writeHeader)
    {
        if (writeHeader)
        {
            BINValue t = (BINValue) v;
            bw.writeInt(Integer.parseInt(HashHandler.getBinKeyForHash(t.getHash()), 16));
            bw.writeByte((byte) type.value);
        }
        
        Object datapoint = v instanceof BINValue ? ((BINValue) v).getValue() : v;
        
        switch (type)
        {
            case BOOLEAN:
            case BOOLEAN_FLAGS:
            {
                bw.writeBoolean((boolean) datapoint);
                break;
            }
            case BYTE:
            case SIGNED_BYTE:
            {
                bw.writeByte((byte) datapoint);
                break;
            }
            case SHORT:
            case SIGNED_SHORT:
            {
                bw.writeShort((short) datapoint);
                break;
            }
            case INT:
            case SIGNED_INT:
            case LINK_OFFSET:
            {
                bw.writeInt((Integer) datapoint);
                break;
            }
            case LONG:
            case SIGNED_LONG:
            {
                bw.writeLong((Long) datapoint);
                break;
            }
            case FLOAT:
            {
                bw.writeFloat((Float) datapoint);
                break;
            }
            case V2_FLOAT:
            {
                bw.writeVec2F((Vector2f) datapoint);
                break;
            }
            case V3_FLOAT:
            {
                bw.writeVec3F((Vector3f) datapoint);
                break;
            }
            case V3_SHORT:
            {
                bw.writeVec3S((Vector3s) datapoint);
                break;
            }
            case V4_FLOAT:
            {
                bw.writeVec4F((Vector4f) datapoint);
                break;
            }
            case M4X4_FLOAT:
            {
                bw.writeFloat4x4((Matrix4f) datapoint);
                break;
            }
            case RGBA_BYTE:
            {
                bw.writeVec4B((Vector4b) datapoint);
                break;
            }
            case STRING:
            {
                bw.writeStringWithLength((String) datapoint);
                break;
            }
            case STRING_HASH:
            {
                bw.writeInt(Integer.parseInt(HashHandler.getBinKeyForHash(String.valueOf(datapoint)), 16));
                break;
            }
            case CONTAINER:
            {
                BINContainer bc = (BINContainer) datapoint;
                bw.writeByte((byte) bc.getType().value);
                bw.writeInt(bc.getSize());
                bw.writeInt(bc.getCount());
                bc.getData().forEach(bv -> writeBinValue(bc.getType(), bv, bw, false));
                break;
            }
            case STRUCTURE:
            case EMBEDDED:
            {
                BINStruct bs = (BINStruct) datapoint;
                bw.writeInt(Integer.parseInt(HashHandler.getBinKeyForHash(bs.getHash()), 16));
                bw.writeInt(bs.getSize());
                bw.writeShort(bs.getCount());
                bs.getData().forEach(bv -> writeBinValue(bv.getType(), bv, bw, true));
                break;
            }
            case OPTIONAL_DATA:
            {
                BINData bd = (BINData) datapoint;
                bw.writeByte((byte) bd.getType().value);
                bw.writeByte(bd.getCount());
                bd.getData().forEach(bv -> writeBinValue(bd.getType(), bv, bw, false));
                break;
            }
            case PAIR:
            {
                BINMap bm = (BINMap) datapoint;
                bw.writeByte((byte) bm.getType1().value);
                bw.writeByte((byte) bm.getType2().value);
                bw.writeInt(bm.getSize());
                bw.writeInt(bm.getCount());
                bm.getData().forEach(bv -> {
                    writeBinValue(bm.getType1(), bv.getFirst(), bw, false);
                    writeBinValue(bm.getType2(), bv.getSecond(), bw, false);
                });
                break;
            }
        }
    }
    
    
    public String toJson()
    {
        if (json == null)
        {
            JsonWriterWrapper jw = new JsonWriterWrapper();
            try
            {
                Map<String, List<JsonElement>> content = new HashMap<>();
                for (BINEntry entry : entries)
                {
                    jw.beginObject();
                    entryToJson(entry, jw);
                    jw.endObject();
                    
                    List<JsonElement> enContent = content.getOrDefault(entry.getType(), new ArrayList<>());
                    enContent.add(UtilHandler.getJsonParser().parse(jw.toString()));
                    content.put(entry.getType(), enContent);
                    
                    jw.clear();
                }
                
                JsonArray arr = new JsonArray();
                for (String link : this.linkedFiles)
                {
                    arr.add(link);
                }
                
                JsonObject entryJson = UtilHandler.getJsonParser().parse(UtilHandler.getGson().toJson(content)).getAsJsonObject();
                entryJson.add("linkedBinFiles", arr);
                
                json = UtilHandler.getGson().toJson(entryJson);
                return json;
                
            } catch (IOException e)
            {
                e.printStackTrace();
                System.out.println(jw.toString());
            }
        }
        
        return json;
    }
    
    private void entryToJson(BINEntry entry, JsonWriterWrapper jw) throws IOException
    {
        jw.name(entry.getHash());
        jw.beginObject();
        for (BINValue value : entry.getValues())
        {
            jw.name(value.getHash());
            printType(value.getHash(), value.getType(), value.getValue(), jw);
        }
        jw.endObject();
    }
    
    // hash is here for debugging purposes
    @SuppressWarnings("unused")
    private void printType(String hash, BINValueType type, Object data, JsonWriterWrapper jw) throws IOException
    {
        switch (type)
        {
            case STRING:
            {
                printString(data.toString(), jw);
                break;
            }
            case CONTAINER:
            case CONTAINER2:
            {
                printContainer((BINContainer) data, jw);
                break;
            }
            case STRUCTURE:
            case EMBEDDED:
            {
                printStruct((BINStruct) data, jw);
                break;
            }
            
            case OPTIONAL_DATA:
            {
                printData((BINData) data, jw);
                break;
            }
            case PAIR:
            {
                printMap((BINMap) data, jw);
                break;
            }
            case LINK_OFFSET:
            {
                jw.value("LINK_OFFSET: " + HashHandler.getBinHashes().getOrDefault(data.toString(), data.toString()));
                break;
            }
            case WAD_LINK:
            {
                jw.value("WAD_LINK: " + HashHandler.getWADHashes().getOrDefault(data.toString(), data.toString()));
                break;
            }
            case STRING_HASH:
            {
                String other    = data.toString();
                String testHash = HashHandler.getBinHashes().getOrDefault(other, other);
                String output   = (other.equalsIgnoreCase(testHash)) ? "STRING_HASH: " : "";
                output += testHash;
                jw.value(output);
                break;
            }
            default:
            {
                jw.jsonValue(data.toString());
                break;
            }
        }
    }
    
    private void printString(String o, JsonWriterWrapper jw) throws IOException
    {
        // do we need to do these replacements? assuming .value() wraps it propperly
        String val = o;
        
        // JSON does not allow \ in the files, so we need to escape it to \\
        val = val.replace("\\", "\\\\");
        
        // JSON does not allow strings to start with ", so we escape them
        val = val.replace("\"", "\\\"");
        
        jw.value(val);
    }
    
    private void printMap(BINMap value, JsonWriterWrapper jw) throws IOException
    {
        jw.beginObject();
        for (Object o : value.getData())
        {
            Vector2<?, ?> obj = (Vector2<?, ?>) o;
            
            JsonWriterWrapper temp = new JsonWriterWrapper();
            printType("", value.getType1(), obj.getFirst(), temp);
            String val1 = temp.toString();
            // val1 should always be a string-like object, so remove quotes if any
            val1 = val1.replace("\"", "");
            
            temp.clear();
            printType("", value.getType2(), obj.getSecond(), temp);
            String val2 = temp.toString();
            
            jw.name(val1).jsonValue(val2);
        }
        jw.endObject();
    }
    
    private void printData(BINData value, JsonWriterWrapper jw) throws IOException
    {
        jw.beginArray();
        for (Object o : value.getData())
        {
            if (value.getType() == BINValueType.STRING)
            {
                printString(o.toString(), jw);
            } else
            {
                jw.jsonValue(o.toString());
            }
        }
        jw.endArray();
    }
    
    private void printStruct(BINStruct value, JsonWriterWrapper jw) throws IOException
    {
        jw.beginObject();
        jw.name(value.getHash());
        jw.beginObject();
        for (BINValue other : value.getData())
        {
            jw.name(other.getHash());
            printType(other.getHash(), other.getType(), other.getValue(), jw);
        }
        jw.endObject();
        jw.endObject();
    }
    
    private void printContainer(BINContainer value, JsonWriterWrapper jw) throws IOException
    {
        jw.beginArray();
        for (Object o : value.getData())
        {
            if (o instanceof BINValue)
            {
                BINValue other = (BINValue) o;
                printType(other.getHash(), other.getType(), other.getValue(), jw);
            } else if (o instanceof BINStruct)
            {
                printStruct((BINStruct) o, jw);
            } else if (value.getType() == BINValueType.STRING)
            {
                printString(o.toString(), jw);
            } else if (value.getType() == BINValueType.STRING_HASH)
            {
                String other  = o.toString();
                String hash   = HashHandler.getBinHashes().getOrDefault(other, other);
                String output = (other.equalsIgnoreCase(hash)) ? "STRING_HASH: " : "";
                output += hash;
                jw.value(output);
            } else if (value.getType() == BINValueType.LINK_OFFSET)
            {
                String other = (String) o;
                jw.value("LINK_OFFSET: " + HashHandler.getBinHashes().getOrDefault(other, other));
            } else
            {
                jw.jsonValue(o.toString());
            }
        }
        jw.endArray();
    }
}

package no.stelar7.cdragon.types.bin.data;

import com.google.gson.stream.JsonWriter;
import lombok.Data;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.ByteWriter;
import no.stelar7.cdragon.util.types.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Data
public class BINFile
{
    private BINHeader      header;
    private List<BINEntry> entries = new ArrayList<>();
    
    private String json;
    
    public void write(Path output)
    {
        try (ByteWriter bw = new ByteWriter())
        {
            bw.writeString("PROP");
            bw.writeInt(2);
            bw.writeInt(this.header.getLinkedFileCount());
            for (String s : this.header.getLinkedFiles())
            {
                bw.writeStringWithLength(s);
            }
            bw.writeInt(this.header.getEntryCount());
            for (Integer i : this.header.getEntryTypes())
            {
                bw.writeInt(i);
            }
            
            this.entries.forEach(e -> {
                bw.writeInt(e.getLenght());
                bw.writeInt(HashHandler.getBinKeyForHash(e.getHash()));
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
            bw.writeInt(HashHandler.getBinKeyForHash(t.getHash()));
            bw.writeByte((byte) type.value);
        }
        switch (type)
        {
            case BOOLEAN:
            {
                if (v instanceof BINValue)
                {
                    bw.writeBoolean((boolean) ((BINValue) v).getValue());
                } else
                {
                    bw.writeBoolean((boolean) v);
                }
                break;
            }
            case BYTE:
            case SIGNED_BYTE:
            case UNKNOWN_BYTE:
            {
                if (v instanceof BINValue)
                {
                    bw.writeByte((byte) ((BINValue) v).getValue());
                } else
                {
                    bw.writeByte((byte) v);
                }
                break;
            }
            case SHORT:
            case SIGNED_SHORT:
            {
                if (v instanceof BINValue)
                {
                    bw.writeShort((short) ((BINValue) v).getValue());
                } else
                {
                    bw.writeShort((short) v);
                }
                break;
            }
            case INT:
            case SIGNED_INT:
            case LINK_OFFSET:
            {
                if (v instanceof BINValue)
                {
                    bw.writeInt((Integer) ((BINValue) v).getValue());
                } else
                {
                    bw.writeInt((Integer) v);
                }
                break;
            }
            case LONG:
            case SIGNED_LONG:
            {
                if (v instanceof BINValue)
                {
                    bw.writeLong((Long) ((BINValue) v).getValue());
                } else
                {
                    bw.writeLong((Long) v);
                }
                break;
            }
            case FLOAT:
            {
                if (v instanceof BINValue)
                {
                    bw.writeFloat((Float) ((BINValue) v).getValue());
                } else
                {
                    bw.writeFloat((Float) v);
                }
                break;
            }
            case V2_FLOAT:
            {
                if (v instanceof BINValue)
                {
                    bw.writeVec2F((Vector2f) ((BINValue) v).getValue());
                } else
                {
                    bw.writeVec2F((Vector2f) v);
                }
                break;
            }
            case V3_FLOAT:
            {
                if (v instanceof BINValue)
                {
                    bw.writeVec3F((Vector3f) ((BINValue) v).getValue());
                } else
                {
                    bw.writeVec3F((Vector3f) v);
                }
                break;
            }
            case V3_SHORT:
            {
                if (v instanceof BINValue)
                {
                    bw.writeVec3S((Vector3s) ((BINValue) v).getValue());
                } else
                {
                    bw.writeVec3S((Vector3s) v);
                }
                break;
            }
            case V4_FLOAT:
            {
                if (v instanceof BINValue)
                {
                    bw.writeVec4F((Vector4f) ((BINValue) v).getValue());
                } else
                {
                    bw.writeVec4F((Vector4f) v);
                }
                break;
            }
            case M4X4_FLOAT:
            {
                if (v instanceof BINValue)
                {
                    bw.writeFloat4x4((Matrix4f) ((BINValue) v).getValue());
                } else
                {
                    bw.writeFloat4x4((Matrix4f) v);
                }
                break;
            }
            case RGBA_BYTE:
            {
                if (v instanceof BINValue)
                {
                    bw.writeVec4B((Vector4b) ((BINValue) v).getValue());
                } else
                {
                    bw.writeVec4B((Vector4b) v);
                }
                break;
            }
            case STRING:
            {
                if (v instanceof BINValue)
                {
                    bw.writeStringWithLength((String) ((BINValue) v).getValue());
                } else
                {
                    bw.writeStringWithLength((String) v);
                }
                break;
            }
            case STRING_HASH:
            {
                bw.writeInt(HashHandler.getBinKeyForHash(String.valueOf(v instanceof BINValue ? ((BINValue) v).getValue() : v)));
                break;
            }
            case CONTAINER:
            {
                BINContainer bc = v instanceof BINContainer ? (BINContainer) v : (BINContainer) (((BINValue) v).getValue());
                bw.writeByte((byte) bc.getType().value);
                bw.writeInt(bc.getSize());
                bw.writeInt(bc.getCount());
                bc.getData().forEach(bv -> writeBinValue(bc.getType(), bv, bw, false));
                break;
            }
            case STRUCTURE:
            case EMBEDDED:
            {
                BINStruct bs = v instanceof BINStruct ? (BINStruct) v : (BINStruct) (((BINValue) v).getValue());
                bw.writeInt(HashHandler.getBinKeyForHash(bs.getHash()));
                bw.writeInt(bs.getEntry());
                bw.writeShort(bs.getCount());
                bs.getData().forEach(bv -> writeBinValue(bv.getType(), bv, bw, true));
                break;
            }
            case OPTIONAL_DATA:
            {
                BINData bd = v instanceof BINData ? (BINData) v : (BINData) (((BINValue) v).getValue());
                bw.writeByte((byte) bd.getType().value);
                bw.writeByte(bd.getCount());
                bd.getData().forEach(bv -> writeBinValue(bd.getType(), bv, bw, false));
                break;
            }
            case PAIR:
            {
                BINMap bm = v instanceof BINMap ? (BINMap) v : (BINMap) (((BINValue) v).getValue());
                bw.writeByte((byte) bm.getType1().value);
                bw.writeByte((byte) bm.getType2().value);
                bw.writeInt(bm.getSize());
                bw.writeInt(bm.getCount());
                bm.getData().forEach(bv -> {
                    writeBinValue(bm.getType1(), bv.getX(), bw, false);
                    writeBinValue(bm.getType2(), bv.getY(), bw, false);
                });
                break;
            }
        }
    }
    
    public String toJson()
    {
        if (json == null)
        {
            try
            {
                StringWriter sw = new StringWriter();
                JsonWriter   jw = new JsonWriter(new BufferedWriter(sw));
                jw.beginObject();
                
                for (int i = 0; i < entries.size(); i++)
                {
                    BINEntry entry  = entries.get(i);
                    String   prefix = HashHandler.getBINHash(header.getEntryTypes().get(i));
                    jw.name(prefix);
                    jw.beginObject();
                    printEntry(entry, jw);
                    jw.endObject();
                }
                
                jw.endObject();
                jw.flush();
                
                json = UtilHandler.mergeTopKeysToArray(sw.toString());
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        
        return json;
    }
    
    private void printEntry(BINEntry entry, JsonWriter jw) throws IOException
    {
        jw.name(entry.getHash());
        jw.beginObject();
        for (BINValue value : entry.getValues())
        {
            printValue(value, jw);
        }
        jw.endObject();
    }
    
    private void printValue(BINValue value, JsonWriter jw) throws IOException
    {
        jw.name(value.getHash());
        printType(value.getHash(), value.getType(), value.getValue(), jw);
    }
    
    // hash is here for debugging purposes
    @SuppressWarnings("unused")
    private void printType(String hash, BINValueType type, Object data, JsonWriter jw) throws IOException
    {
        switch (type)
        {
            case STRING:
            {
                printString(data.toString(), jw);
                break;
            }
            case CONTAINER:
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
                jw.value("LINK_OFFSET: " + data.toString());
                break;
            }
            default:
            {
                jw.jsonValue(data.toString());
                break;
            }
        }
    }
    
    private void printString(String o, JsonWriter jw) throws IOException
    {
        // do we need to do these replacements? assuming .value() wraps it propperly
        
        String val = o;
        
        // JSON does not allow \ in the files, so we need to escape it to \\
        val = val.replace("\\", "\\\\");
        
        // JSON does not allow strings to start with ", so we escape them
        val = val.replace("\"", "\\\"");
        
        jw.value(val);
    }
    
    private void printMap(BINMap value, JsonWriter jw) throws IOException
    {
        jw.beginObject();
        for (Object o : value.getData())
        {
            Vector2<?, ?> obj = (Vector2<?, ?>) o;
            
            StringWriter sw   = new StringWriter();
            JsonWriter   temp = new JsonWriter(new BufferedWriter(sw));
            printType("", value.getType1(), obj.getX(), temp);
            temp.flush();
            String val1 = sw.toString();
            
            sw = new StringWriter();
            temp = new JsonWriter(new BufferedWriter(sw));
            printType("", value.getType2(), obj.getY(), temp);
            temp.flush();
            String val2 = sw.toString();
            
            jw.name(val1);
            jw.jsonValue(val2);
        }
        jw.endObject();
    }
    
    private void printData(BINData value, JsonWriter jw) throws IOException
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
    
    private void printStruct(BINStruct value, JsonWriter jw) throws IOException
    {
        jw.beginObject();
        jw.name(value.getHash());
        jw.beginObject();
        for (Object o : value.getData())
        {
            printValue((BINValue) o, jw);
        }
        jw.endObject();
        jw.endObject();
    }
    
    private void printContainer(BINContainer value, JsonWriter jw) throws IOException
    {
        jw.beginArray();
        for (Object o : value.getData())
        {
            if (o instanceof BINValue)
            {
                printType(((BINValue) o).getHash(), ((BINValue) o).getType(), ((BINValue) o).getValue(), jw);
            } else if (o instanceof BINStruct)
            {
                printStruct((BINStruct) o, jw);
            } else if (value.getType() == BINValueType.STRING)
            {
                printString(o.toString(), jw);
            } else if (value.getType() == BINValueType.STRING_HASH)
            {
                jw.value(HashHandler.getBINHash((Integer) o));
            } else
            {
                jw.jsonValue(o.toString());
            }
        }
        jw.endArray();
    }
}

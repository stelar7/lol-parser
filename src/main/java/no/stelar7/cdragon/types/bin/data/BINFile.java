package no.stelar7.cdragon.types.bin.data;

import com.google.gson.stream.JsonWriter;
import lombok.Data;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.Vector2;

import java.io.*;
import java.util.*;

@Data
public class BINFile
{
    private BINHeader      header;
    private List<BINEntry> entries = new ArrayList<>();
    
    private String json;
    
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
        printType(value.getHash(), BINValueType.valueOf(value.getType()), value.getValue(), jw);
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
            printType("", BINValueType.valueOf(value.getType1()), obj.getX(), temp);
            temp.flush();
            String val1 = sw.toString();
            
            sw = new StringWriter();
            temp = new JsonWriter(new BufferedWriter(sw));
            printType("", BINValueType.valueOf(value.getType2()), obj.getY(), temp);
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
            if (value.getType() == BINValueType.STRING.value)
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
                printType(((BINValue) o).getHash(), BINValueType.valueOf(((BINValue) o).getType()), ((BINValue) o).getValue(), jw);
            } else if (o instanceof BINStruct)
            {
                printStruct((BINStruct) o, jw);
            } else if (value.getType() == BINValueType.STRING.value)
            {
                printString(o.toString(), jw);
            } else if (value.getType() == BINValueType.STRING_HASH.value)
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

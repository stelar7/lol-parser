package no.stelar7.cdragon.types.bin.data;

import lombok.Data;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.Vector2;

import java.util.*;

@Data
public class BINFile
{
    private BINHeader header;
    private List<BINEntry> entries = new ArrayList<>();
    
    private String json;
    
    public String toJson()
    {
        if (json == null)
        {
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < entries.size(); i++)
            {
                BINEntry entry  = entries.get(i);
                String   prefix = HashHandler.getBINHash(header.getEntryTypes().get(i));
                sb.append("\"").append(prefix).append("\":{");
                printEntry(entry, sb);
                sb.append("}");
                sb.append(",");
            }
            removeTrailingComma(sb);
            sb.append("}");
            json = UtilHandler.mergeTopKeysToArray(sb.toString());
        }
        
        return json;
    }
    
    private void printEntry(BINEntry entry, StringBuilder sb)
    {
        sb.append("\"").append(entry.getHash()).append("\":{");
        
        for (BINValue value : entry.getValues())
        {
            printValue(value, sb);
        }
        removeTrailingComma(sb);
        sb.append("}");
    }
    
    private void printValue(BINValue value, StringBuilder sb)
    {
        sb.append("\"").append(value.getHash()).append("\":");
        
        printType(value.getHash(), BINValueType.valueOf(value.getType()), value.getValue(), sb);
        
        sb.append(",");
    }
    
    // hash is here for debugging purposes
    @SuppressWarnings("unused")
    private void printType(String hash, BINValueType type, Object data, StringBuilder sb)
    {
        switch (type)
        {
            case STRING:
            {
                printString(data.toString(), sb);
                break;
            }
            case CONTAINER:
            {
                printContainer((BINContainer) data, sb);
                break;
            }
            case STRUCTURE:
            case EMBEDDED:
            {
                printStruct((BINStruct) data, sb);
                break;
            }
            
            case OPTIONAL_DATA:
            {
                printData((BINData) data, sb);
                break;
            }
            case PAIR:
            {
                printMap((BINMap) data, sb);
                break;
            }
            default:
                sb.append(data.toString());
        }
    }
    
    private void printString(String o, StringBuilder sb)
    {
        String val = o;
        
        // JSON does not allow \ in the files, so we need to escape it to \\
        val = val.replace("\\", "\\\\");
        
        // JSON does not allow strings to start with ", so we escape them
        val = val.replace("\"", "\\\"");
        
        sb.append("\"").append(val).append("\"");
    }
    
    private void printMap(BINMap value, StringBuilder sb)
    {
        sb.append("{");
        for (Object o : value.getData())
        {
            Vector2<?, ?> obj = (Vector2<?, ?>) o;
            
            StringBuilder temp = new StringBuilder();
            printType("", BINValueType.valueOf(value.getType1()), obj.getX(), temp);
            String val1 = temp.toString();
            
            temp = new StringBuilder();
            printType("", BINValueType.valueOf(value.getType2()), obj.getY(), temp);
            String val2 = temp.toString();
            
            if (val1.startsWith("\""))
            {
                sb.append(val1);
            } else
            {
                sb.append("\"").append(val1).append("\"");
            }
            
            sb.append(":").append(val2).append(",");
        }
        removeTrailingComma(sb);
        sb.append("}");
    }
    
    private void printData(BINData value, StringBuilder sb)
    {
        sb.append("[");
        for (Object o : value.getData())
        {
            if (value.getType() == BINValueType.STRING.value)
            {
                printString(o.toString(), sb);
            } else
            {
                sb.append(o.toString()).append(",");
            }
        }
        removeTrailingComma(sb);
        sb.append("]");
    }
    
    private void printStruct(BINStruct value, StringBuilder sb)
    {
        sb.append("{");
        sb.append("\"").append(value.getHash()).append("\":{");
        for (Object o : value.getData())
        {
            printValue((BINValue) o, sb);
        }
        removeTrailingComma(sb);
        sb.append("}}");
    }
    
    private void printContainer(BINContainer value, StringBuilder sb)
    {
        sb.append("[");
        for (Object o : value.getData())
        {
            if (o instanceof BINValue)
            {
                printType(((BINValue) o).getHash(), BINValueType.valueOf(((BINValue) o).getType()), ((BINValue) o).getValue(), sb);
            } else if (o instanceof BINStruct)
            {
                printStruct((BINStruct) o, sb);
            } else if (value.getType() == BINValueType.STRING.value)
            {
                printString(o.toString(), sb);
            } else if (o instanceof Integer)
            {
                sb.append(HashHandler.getBINHash((Integer) o));
            } else
            {
                sb.append(o);
            }
            sb.append(",");
        }
        removeTrailingComma(sb);
        sb.append("]");
    }
    
    private void removeTrailingComma(StringBuilder sb)
    {
        sb.reverse();
        while (sb.charAt(0) == ',')
        {
            sb.deleteCharAt(0);
        }
        sb.reverse();
    }
}

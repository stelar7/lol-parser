package no.stelar7.cdragon.types.bin.data;

import lombok.Data;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.types.Vector2;

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
                String   prefix = UtilHandler.getBINHash(header.getEntryTypes().get(i));
                sb.append("\"").append(prefix).append("\":{");
                printEntry(entry, sb);
                sb.append("}");
                sb.append(",");
            }
            removeTrailingComma(sb);
            sb.append("}");
            json = sb.toString().replace("\\\"", "\"");
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
        
        printType(value.getHash(), value.getType(), value.getValue(), sb);
        
        sb.append(",");
    }
    
    // hash is here for debugging purposes
    @SuppressWarnings("unused")
    private void printType(String hash, byte type, Object data, StringBuilder sb)
    {
        switch (type)
        {
            case 16:
            {
                sb.append("\"").append(data.toString()).append("\"");
                break;
            }
            case 18:
            {
                printContainer((BINContainer) data, sb);
                break;
            }
            case 19:
            case 20:
            {
                printStruct((BINStruct) data, sb);
                break;
            }
            
            case 22:
            {
                printData((BINData) data, sb);
                break;
            }
            case 23:
            {
                printMap((BINMap) data, sb);
                break;
            }
            default:
                sb.append(data.toString());
        }
    }
    
    private void printMap(BINMap value, StringBuilder sb)
    {
        sb.append("{");
        for (Object o : value.getData())
        {
            Vector2<?, ?> obj = (Vector2<?, ?>) o;
            
            StringBuilder temp = new StringBuilder();
            printType("", value.getType1(), obj.getX(), temp);
            String val1 = temp.toString();
            
            temp = new StringBuilder();
            printType("", value.getType2(), obj.getY(), temp);
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
            if (value.getType() == 16)
            {
                sb.append("\"").append(o.toString()).append("\"").append(",");
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
                printType(((BINValue) o).getHash(), ((BINValue) o).getType(), ((BINValue) o).getValue(), sb);
            } else if (o instanceof BINStruct)
            {
                printStruct((BINStruct) o, sb);
            } else if (value.getType() == 16)
            {
                sb.append("\"").append(o.toString()).append("\"");
            } else if (o instanceof Integer)
            {
                sb.append(UtilHandler.getBINHash((Integer) o));
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

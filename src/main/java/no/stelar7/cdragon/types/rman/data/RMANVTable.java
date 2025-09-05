package no.stelar7.cdragon.types.rman.data;

import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.util.*;
import java.util.Map.Entry;

public class RMANVTable
{
    public static Map<String, Object> parseVTable(RandomAccessReader raf, Map<String, String> tableDefinition)
    {
        int startPos = (int) raf.pos();
        
        int vTableOffset = startPos - raf.readInt();
        raf.seek(vTableOffset);
        
        short       vtableSize         = raf.readShort();
        short       vtableEntryCount   = raf.readShort();
        List<Short> vTableEntryOffsets = raf.readShorts(vtableEntryCount);
        
        Map<String, Object>         data         = new HashMap<>();
        List<Entry<String, String>> tableEntries = tableDefinition.entrySet().stream().toList();
        for (int i = 0; i < tableEntries.size(); i++)
        {
            Entry<String, String> entry  = tableEntries.get(i);
            short                 offset = vTableEntryOffsets.get(i);
            
            if (offset == 0)
            {
                switch (entry.getValue())
                {
                    case "0" ->
                    {
                        // Do nothing
                    }
                    case "2" -> data.put(entry.getKey(), (short) 0);
                    case "4" -> data.put(entry.getKey(), 0);
                    case "8" -> data.put(entry.getKey(), 0L);
                    case "offset4" -> data.put(entry.getKey(), 0);
                    case "list8" -> data.put(entry.getKey(), List.of());
                    case "string" -> data.put(entry.getKey(), "");
                    default -> throw new RuntimeException("Unhandled read size: " + entry.getKey() + ": " + entry.getValue());
                }
                
                continue;
            }
            
            int offsetPos = startPos + offset;
            raf.seek(offsetPos);
            
            switch (entry.getValue())
            {
                case "0" ->
                {
                    // Do nothing
                }
                case "2" -> data.put(entry.getKey(), raf.readShort());
                case "4" -> data.put(entry.getKey(), raf.readInt());
                case "8" -> data.put(entry.getKey(), raf.readLong());
                case "offset4" -> data.put(entry.getKey(), offsetPos + raf.readInt());
                case "list8" ->
                {
                    int listOffset = raf.readInt();
                    raf.seek(offsetPos + listOffset);
                    List<Long> values = raf.readLongs(raf.readInt());
                    data.put(entry.getKey(), values);
                }
                case "string" ->
                {
                    int stringOffset = raf.readInt();
                    raf.seek(offsetPos + stringOffset);
                    data.put(entry.getKey(), raf.readString(raf.readInt()));
                }
                default -> throw new RuntimeException("Unhandled read size: " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        return data;
    }
    
    public static Map<String, String> getBundleFields()
    {
        return new LinkedHashMap<>()
        {
            {
                put("bundle_id", "8");
                put("chunks_offset", "offset4");
            }
        };
    }
    
    public static Map<String, String> getChunkFields()
    {
        return new LinkedHashMap<>()
        {
            {
                put("chunk_id", "8");
                put("compressed_size", "4");
                put("uncompressed_size", "4");
            }
        };
    }
    
    public static Map<String, String> getDirectoryFields()
    {
        return new LinkedHashMap<>()
        {
            {
                put("directory_id", "8");
                put("parent_id", "8");
                put("name", "string");
            }
        };
    }
    
    public static Map<String, String> getLanguageFields()
    {
        return new LinkedHashMap<>()
        {
            {
                put("language_id", "4");
                put("name", "string");
            }
        };
    }
    
    public static Map<String, String> getFileFields()
    {
        return new LinkedHashMap<>()
        {
            {
                put("file_id", "8");
                put("directory_id", "8");
                put("file_size", "4");
                put("name", "string");
                put("language_id", "8");
                put("unknown", "0");
                put("unknown1", "0");
                put("chunks", "list8");
                put("unknown2", "0");
                put("symlink", "string");
                put("unknown3", "0");
                put("unknown4", "0");
                put("unknown5", "0");
            }
        };
    }
    
    
}

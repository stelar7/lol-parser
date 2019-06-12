package no.stelar7.cdragon.util.hashguessing;

import com.google.gson.reflect.TypeToken;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.writers.JsonWriterWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;

public class HashFile
{
    
    private Path   file;
    private Path   backup;
    public  String printFormat;
    public  String writeFormat;
    
    public HashFile(Path file, Path backup)
    {
        this.file = file;
        this.backup = backup;
        this.printFormat = "%s: %s%n";
        this.writeFormat = "%s: %s%n";
    }
    
    private Map<String, String> hashes;
    
    public Map<String, String> load(boolean force)
    {
        try
        {
            if (force || hashes == null)
            {
                if (!Files.exists(file))
                {
                    hashes = UtilHandler.getGson().fromJson(Files.readString(this.backup), new TypeToken<Map<String, String>>() {}.getType());
                } else
                {
                    hashes = UtilHandler.getGson().fromJson(Files.readString(this.file), new TypeToken<Map<String, String>>() {}.getType());
                    if (hashes == null && Files.exists(backup))
                    {
                        hashes = UtilHandler.getGson().fromJson(Files.readString(this.backup), new TypeToken<Map<String, String>>() {}.getType());
                    }
                }
                
                if (hashes == null)
                {
                    hashes = new HashMap<>();
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return hashes;
    }
    
    public void save(Map<String, String> known)
    {
        try
        {
            List<Entry<String, String>> entries = new ArrayList<>(known.entrySet());
            entries.sort((a, b) -> a.getValue().compareToIgnoreCase(b.getValue()));
            
            Files.write(this.file, "{\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            for (Entry<String, String> e : entries)
            {
                Files.write(this.file, String.format(this.writeFormat, e.getKey(), e.getValue()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            }
            Files.write(this.file, "}".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void saveAsJson(Map<String, String> known)
    {
        try
        {
            NaturalOrderComparator      cmp     = new NaturalOrderComparator();
            List<Entry<String, String>> entries = new ArrayList<>(known.entrySet());
            entries.sort((a, b) -> cmp.compare(a.getValue(), b.getValue()));
            
            JsonWriterWrapper jw = new JsonWriterWrapper();
            
            jw.beginObject();
            for (Entry<String, String> e : entries)
            {
                jw.name(e.getKey()).value(e.getValue());
            }
            jw.endObject();
            
            Files.write(this.file, jw.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

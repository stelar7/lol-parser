package no.stelar7.cdragon.util.hashguessing;

import com.google.gson.reflect.TypeToken;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.JsonHandler;
import no.stelar7.cdragon.util.writers.JsonWriterWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;

public class HashFile
{
    
    protected Path   file;
    protected Path   backup;
    public    String printFormat;
    public    String writeFormat;
    
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
                    hashes = JsonHandler.getGson().fromJson(Files.readString(this.backup), new TypeToken<Map<String, String>>() {}.getType());
                } else
                {
                    hashes = JsonHandler.getGson().fromJson(Files.readString(this.file), new TypeToken<Map<String, String>>() {}.getType());
                    if (hashes == null && Files.exists(backup))
                    {
                        hashes = JsonHandler.getGson().fromJson(Files.readString(this.backup), new TypeToken<Map<String, String>>() {}.getType());
                    }
                }
                
                if (hashes == null)
                {
                    hashes = new HashMap<>();
                }
            }
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        
        return hashes;
    }
    
    public void save(Map<String, String> known)
    {
        try
        {
            List<Entry<String, String>> entries = new ArrayList<>(known.entrySet());
            entries.sort((a, b) -> a.getValue().compareToIgnoreCase(b.getValue()));
            
            StringBuilder sb = new StringBuilder("{\n");
            for (Entry<String, String> e : entries)
            {
                sb.append(String.format(this.writeFormat, e.getKey(), e.getValue()));
            }
            sb.append("}");
            Files.writeString(this.file, sb);
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
    
    public void saveAsJson(Map<String, String> known, Path outputFile)
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
            
            Files.write(outputFile, jw.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
    
    public void saveToBackupAsJson(Map<String, String> known)
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
            
            Files.write(this.backup, jw.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}

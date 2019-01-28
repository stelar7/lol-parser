package no.stelar7.cdragon.util.hashguessing;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;

public class HashFile
{
    
    private Map<String, String> hashes;
    private Path                file;
    private String              format;
    
    public HashFile(Path file)
    {
        this.file = file;
        this.format = "%s %s%n";
    }
    
    public Map<String, String> load(boolean force)
    {
        try
        {
            if (force || hashes == null)
            {
                hashes = new HashMap<>();
                Files.readAllLines(this.file).stream().map(l -> l.split(" ")).forEach(s -> hashes.put(s[0], s[1]));
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return hashes;
    }
    
    public void save()
    {
        try
        {
            List<Entry<String, String>> entries = new ArrayList<>(hashes.entrySet());
            entries.sort((a, b) -> a.getValue().compareToIgnoreCase(b.getValue()));
            
            Files.deleteIfExists(this.file);
            StandardOpenOption[] params = {StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE};
            for (Entry<String, String> e : entries)
            {
                Files.write(this.file, String.format(this.format, e.getKey(), e.getValue()).getBytes(StandardCharsets.UTF_8), params);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

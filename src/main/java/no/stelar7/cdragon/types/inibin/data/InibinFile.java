package no.stelar7.cdragon.types.inibin.data;

import no.stelar7.cdragon.interfaces.Extractable;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.types.Vector2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class InibinFile implements Extractable
{
    private InibinHeader        header;
    private Map<String, Object> keys;
    
    public InibinHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(InibinHeader header)
    {
        this.header = header;
    }
    
    public Map<String, Object> getKeys()
    {
        return keys;
    }
    
    public void setKeys(Map<String, Object> keys)
    {
        this.keys = keys;
    }
    
    public void extract(Path path)
    {
        try
        {
            List<Vector2<String, String>> values = new ArrayList<>();
            
            keys.forEach((k, v) -> values.add(new Vector2<>(InibinHash.getHash(k), InibinHash.getTransformed(k, v))));
            values.sort(new NaturalOrderComparator());
            
            StringBuilder sb = new StringBuilder();
            values.forEach(p -> sb.append(String.format("%s = %s%n", p.getFirst(), p.getSecond())));
            
            Files.createDirectories(path.getParent());
            Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

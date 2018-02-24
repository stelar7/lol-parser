package no.stelar7.cdragon.types.inibin.data;

import lombok.Data;
import no.stelar7.cdragon.interfaces.Extractable;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.readers.types.Vector2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Data
public class InibinFile implements Extractable
{
    private InibinHeader        header;
    private Map<String, Object> keys;
    
    public void extract(Path path)
    {
        try
        {
            List<Vector2<String, String>> values = new ArrayList<>();
            
            keys.forEach((k, v) -> values.add(new Vector2<>(InibinHash.getHash(k), InibinHash.getTransformed(k, v))));
            values.sort(new NaturalOrderComparator());
            
            StringBuilder sb = new StringBuilder();
            values.forEach(p -> sb.append(String.format("%s = %s%n", p.getX(), p.getY())));
            
            Files.createDirectories(path.getParent());
            Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

package no.stelar7.cdragon.types.inibin.data;

import javafx.util.Pair;
import lombok.*;
import no.stelar7.cdragon.util.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Data
public class InibinFile
{
    private InibinHeader        header;
    private Map<String, Object> keys;
    
    @Setter(value = AccessLevel.NONE)
    @Getter(value = AccessLevel.NONE)
    private final Path path;
    
    public InibinFile(Path pathToInibin)
    {
        this.path = pathToInibin;
    }
    
    public void extractFile(Path extractPath)
    {
        try
        {
            List<Pair<String, String>> values = new ArrayList<>();
            
            keys.forEach((k, v) -> values.add(new Pair<>(InibinHash.getHash(k), InibinHash.getTransformed(k, v))));
            values.sort(new NaturalOrderComparator());
            
            StringBuilder sb = new StringBuilder();
            values.forEach(p -> sb.append(String.format("%s = %s%n", p.getKey(), p.getValue())));
            
            Files.createDirectories(extractPath);
            Files.write(extractPath.resolve(UtilHandler.pathToFilename(path) + ".ini"), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

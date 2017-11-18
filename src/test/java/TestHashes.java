package no.stelar7.cdragon.wad;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.util.Pair;
import no.stelar7.cdragon.util.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class TestHashes
{
    
    private final List<String> exts = Arrays.asList("json", "txt", "png", "jpg", "jpeg", "webm", "ogg", "dds");
    
    
    private final Map<String, Integer[]> folderData = new HashMap<String, Integer[]>()
    {{
        final int iconMax = 10000;
        put("profile-icons", new Integer[]{iconMax});
        
        final int championMax = 600;
        put("champions", new Integer[]{championMax});
        put("champion-sfx-audios", new Integer[]{championMax});
        put("champion-icons", new Integer[]{championMax});
        put("champion-choose-vo", new Integer[]{championMax});
        put("champion-ban-vo", new Integer[]{championMax});
        
        final int skinMax = 25;
        put("champion-tiles", new Integer[]{championMax, skinMax});
        put("champion-splashes", new Integer[]{championMax, skinMax});
        put("champion-chroma-images", new Integer[]{championMax, skinMax});
    }};
    
    
    @Test
    public void testAllHashes() throws IOException, InterruptedException
    {
        List<String>    hashes  = getUnknownHashes(Paths.get("C:\\Users\\Steffen\\Downloads\\unknown"));
        ExecutorService service = Executors.newFixedThreadPool(8);
        
        for (String ext : exts)
        {
            service.submit(() -> folderData.forEach((k, v) -> generateHashList(k, v, ext, hashes)));
        }
        
        service.shutdown();
        service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        
        folderData.keySet().forEach(s -> combineFolderJSON(s, exts));
        combineJSON(folderData.keySet());
    }
    
    private void combineJSON(Set<String> folders)
    {
        try
        {
            List<Pair<String, String>> ml = new ArrayList<>();
            
            for (String folder : folders)
            {
                Path p = Paths.get(folder + ".json");
                if (!Files.exists(p))
                {
                    continue;
                }
                
                StringBuilder sb    = new StringBuilder();
                List<String>  lines = Files.readAllLines(p);
                lines.forEach(sb::append);
                
                ((Map<String, String>) new Gson().fromJson(sb.toString(), new TypeToken<Map<String, String>>() {}.getType())).forEach((k, v) -> ml.add(new Pair<>(k, v)));
                Files.deleteIfExists(p);
            }
            
            if (ml.isEmpty())
            {
                return;
            }
            
            ml.sort(Comparator.comparing(Pair::getValue, new NaturalOrderComparator()));
            
            StringBuilder sb = new StringBuilder("{\n");
            for (Pair<String, String> pair : ml)
            {
                sb.append("\t\"").append(pair.getKey()).append("\": \"").append(pair.getValue()).append("\",\n");
            }
            sb.reverse().delete(0, 2).reverse().append("\n}");
            
            Files.write(Paths.get("combined.json"), sb.toString().getBytes(StandardCharsets.UTF_8));
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void combineFolderJSON(String folder, List<String> exts)
    {
        try
        {
            List<Pair<String, String>> ml = new ArrayList<>();
            
            for (String ext : exts)
            {
                Path p = Paths.get(folder + "." + ext + ".json");
                if (!Files.exists(p))
                {
                    continue;
                }
                
                StringBuilder sb    = new StringBuilder();
                List<String>  lines = Files.readAllLines(p);
                lines.forEach(sb::append);
                
                ((Map<String, String>) new Gson().fromJson(sb.toString(), new TypeToken<Map<String, String>>() {}.getType())).forEach((k, v) -> ml.add(new Pair<>(k, v)));
                Files.deleteIfExists(p);
            }
            
            if (ml.isEmpty())
            {
                return;
            }
            
            ml.sort(Comparator.comparing(Pair::getValue, new NaturalOrderComparator()));
            
            StringBuilder sb = new StringBuilder("{\n");
            for (Pair<String, String> pair : ml)
            {
                sb.append("\t\"").append(pair.getKey()).append("\": \"").append(pair.getValue()).append("\",\n");
            }
            sb.reverse().delete(0, 2).reverse().append("\n}");
            
            Files.write(Paths.get(folder + ".json"), sb.toString().getBytes(StandardCharsets.UTF_8));
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void generateHashList(String folderName, Integer[] depths, String fileType, List<String> unknownHashes)
    {
        String pathPrefix = "plugins/rcp-be-lol-game-data/global/default/v1/" + folderName + "/";
        Path   p          = Paths.get(folderName + "." + fileType + ".json");
        
        StringBuilder sb = new StringBuilder("{\n");
        
        if (depths.length == 1)
        {
            doLoop(depths[0], pathPrefix + "%s." + fileType, unknownHashes, sb);
        } else
        {
            doNestedLoop(depths[0], depths[1], pathPrefix + "%1$d/%1$d%2$03d." + fileType, unknownHashes, sb);
        }
        sb.reverse().delete(0, 2).reverse();
        
        if (sb.length() > 0)
        {
            sb.append("\n}");
            writeFile(p, sb.toString());
        }
    }
    
    private void doLoop(int max, String format, List<String> hashes, StringBuilder sb)
    {
        for (int i = 0; i < max; i++)
        {
            String value = String.format(format, i);
            String hash  = UtilHandler.getHash(value);
            
            if (hashes.contains(hash))
            {
                sb.append("\t\"").append(hash).append("\": \"").append(value).append("\",\n");
            }
        }
    }
    
    private void doNestedLoop(int outerMax, int innerMax, String format, List<String> hashes, StringBuilder sb)
    {
        for (int i = 0; i < outerMax; i++)
        {
            for (int j = 0; j < innerMax; j++)
            {
                String value = String.format(format, i, j);
                String hash  = UtilHandler.getHash(value);
                
                if (hashes.contains(hash))
                {
                    sb.append("\t\"").append(hash).append("\": \"").append(value).append("\",\n");
                }
            }
        }
    }
    
    
    private List<String> getUnknownHashes(Path savedPath) throws IOException
    {
        return Files.readAllLines(Paths.get("unknown.json"));
    }
    
    
    private void writeFile(Path file, String data)
    {
        try
        {
            Files.write(file, data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

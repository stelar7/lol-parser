
import com.google.gson.*;
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
    
    private final List<String> exts   = Arrays.asList("json", "txt", "png", "jpg", "jpeg", "webm", "ogg", "dds");
    private final String       pre    = "plugins/rcp-be-lol-game-data/global/default/";
    private final List<String> hashes = getUnknownHashes();
    
    
    final         int                    iconMax     = 10000;
    final         int                    championMax = 600;
    final         int                    skinMax     = 25;
    private final Map<String, Integer[]> folderData  = new HashMap<String, Integer[]>()
    {{
        put("profile-icons", new Integer[]{iconMax});
        
        put("champions", new Integer[]{championMax});
        put("champion-sfx-audios", new Integer[]{championMax});
        put("champion-icons", new Integer[]{championMax});
        put("champion-choose-vo", new Integer[]{championMax});
        put("champion-ban-vo", new Integer[]{championMax});
        put("summoner-backdrops", new Integer[]{championMax});
        
        put("champion-tiles", new Integer[]{championMax, skinMax});
        put("champion-splashes", new Integer[]{championMax, skinMax});
        put("champion-chroma-images", new Integer[]{championMax, skinMax});
    }};
    
    @Test
    public void testAllHashes() throws IOException, InterruptedException
    {
        ExecutorService service = Executors.newFixedThreadPool(8);
        
        Path file  = Paths.get("C:/Users/Steffen/Downloads/plugins/rcp-be-lol-game-data/global/default/v1/");
        Path file2 = Paths.get("C:/Users/Steffen/Downloads/plugins/rcp-be-lol-game-data/global/default/v1/champions");
        
        findIconPathInJsonArrayFile(file, "perkstyles.json");
        findIconPathInJsonArrayFile(file, "perks.json");
        findIconPathInJsonArrayFile(file, "items.json");
        findIconPathInJsonArrayFile(file, "summoner-spells.json");
        findIconPathInJsonArrayFile(file, "profile-icons.json");
        
        parseWardSkins(file, "ward-skins.json");
        parseMasteries(file, "summoner-masteries.json");
        parseEmotes(file, "summoner-emotes.json");
        parseBanners(file, "summoner-banners.json");
        
        for (int i = -1; i < championMax; i++)
        {
            findInChampionFile(file2, i + ".json");
        }
        
        for (String ext : exts)
        {
            service.submit(() -> folderData.forEach((k, v) -> generateHashList(k, v, ext, hashes)));
        }
        
        service.shutdown();
        service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        
        
        folderData.keySet().forEach(s -> combineFolderJSON(s, exts));
        
        
        folderData.put("perkstyles", new Integer[]{1});
        folderData.put("perks", new Integer[]{1});
        folderData.put("items", new Integer[]{1});
        folderData.put("summoner-spells", new Integer[]{1});
        folderData.put("profile-icons", new Integer[]{1});
        folderData.put("summoner-masteries", new Integer[]{1});
        folderData.put("ward-skins", new Integer[]{1});
        folderData.put("summoner-emotes", new Integer[]{1});
        folderData.put("summoner-banners", new Integer[]{1});
        
        
        for (int i = 0; i < championMax; i++)
        {
            folderData.put(String.valueOf(i), new Integer[]{1});
        }
        
        combineJSON(folderData.keySet());
    }
    
    private void parseBanners(Path filepath, String filename) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonObject    elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonObject();
        StringBuilder data = new StringBuilder("{");
        
        JsonArray bflags = elem.getAsJsonArray("BannerFlags");
        for (JsonElement element : bflags)
        {
            JsonObject el = element.getAsJsonObject();
            getElementAndCheckHash(el, "inventoryIcon", data);
            getElementAndCheckHash(el, "profileIcon", data);
        }
        
        JsonArray bframes = elem.getAsJsonArray("BannerFrames");
        for (JsonElement element : bframes)
        {
            JsonObject el = element.getAsJsonObject();
            
            getElementAndCheckHash(el, "inventoryIcon", data);
            
            if (el.has("profileIcon"))
            {
                String wip = el.get("profileIcon").getAsString().toLowerCase(Locale.ENGLISH);
                wip = wip.substring(wip.lastIndexOf("assets"));
                hashAndAddToSB(data, pre + wip);
            }
        }
        finalizeFileReading(filename, data);
    }
    
    private void finalizeFileReading(String filename, StringBuilder data) throws IOException
    {
        data.reverse().delete(0, 2).reverse().append("\n}");
        if (data.toString().length() < 10)
        {
            return;
        }
        Files.write(Paths.get(filename), data.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    private void parseEmotes(Path filepath, String filename) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonArray     elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonArray();
        StringBuilder data = new StringBuilder("{");
        
        for (JsonElement element : elem)
        {
            JsonObject el = element.getAsJsonObject();
            getElementAndCheckHash(el, "inventoryIcon", data);
        }
        
        finalizeFileReading(filename, data);
    }
    
    private void parseMasteries(Path filepath, String filename) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonObject    elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonObject().getAsJsonObject("data");
        StringBuilder data = new StringBuilder("{");
        
        for (String key : elem.keySet())
        {
            JsonObject el = elem.getAsJsonObject(key);
            getElementAndCheckHash(el, "iconPath", data);
        }
        finalizeFileReading(filename, data);
    }
    
    private void getElementAndCheckHash(JsonObject el, String path, StringBuilder data)
    {
        String wip = el.get(path).getAsString().toLowerCase(Locale.ENGLISH);
        if (wip.contains("/content/"))
        {
            wip = wip.substring(wip.lastIndexOf("content"));
        } else if (wip.contains("/v1/"))
        {
            wip = wip.substring(wip.lastIndexOf("v1"));
        } else if (wip.contains("/data/"))
        {
            wip = wip.substring(wip.lastIndexOf("data"));
        } else if (wip.contains("/assets/"))
        {
            wip = wip.substring(wip.lastIndexOf("assets"));
        } else
        {
            System.err.println("WTF??");
            System.err.println(wip);
            System.err.println(pre + wip);
        }
        
        hashAndAddToSB(data, pre + wip);
    }
    
    private void parseWardSkins(Path filepath, String filename) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonArray     elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonArray();
        StringBuilder data = new StringBuilder("{");
        
        for (JsonElement element : elem)
        {
            JsonObject el = element.getAsJsonObject();
            
            getElementAndCheckHash(el, "wardImagePath", data);
            getElementAndCheckHash(el, "wardShadowImagePath", data);
        }
        finalizeFileReading(filename, data);
    }
    
    private void findInChampionFile(Path filepath, String filename) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonObject    elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonObject();
        StringBuilder data = new StringBuilder("{");
        
        String passive = elem.getAsJsonObject("passive").get("abilityIconPath").getAsString().toLowerCase(Locale.ENGLISH);
        
        if (!passive.isEmpty())
        {
            getElementAndCheckHash(elem.getAsJsonObject("passive"), "abilityIconPath", data);
        }
        
        JsonArray arr = elem.getAsJsonArray("spells");
        
        for (JsonElement element : arr)
        {
            JsonObject current = element.getAsJsonObject();
            getElementAndCheckHash(current, "abilityIconPath", data);
        }
        
        arr = elem.getAsJsonArray("recommendedItemDefaults");
        
        for (JsonElement element : arr)
        {
            String value = element.getAsString().toLowerCase(Locale.ENGLISH);
            value = value.substring(value.indexOf("data"));
            String hashMe = pre + value;
            
            hashAndAddToSB(data, hashMe);
        }
        
        
        arr = elem.getAsJsonArray("skins");
        
        for (JsonElement element : arr)
        {
            JsonObject ob = element.getAsJsonObject();
            
            getElementAndCheckHash(ob, "splashPath", data);
            getElementAndCheckHash(ob, "uncenteredSplashPath", data);
            getElementAndCheckHash(ob, "tilePath", data);
            getElementAndCheckHash(ob, "loadScreenPath", data);
            
            if (ob.has("chromas"))
            {
                JsonArray chrom = ob.getAsJsonArray("chromas");
                for (JsonElement ch : chrom)
                {
                    String cp = ob.get("chromaPath").getAsString().toLowerCase(Locale.ENGLISH);
                    cp = cp.substring(cp.lastIndexOf("v1"));
                    hashAndAddToSB(data, pre + cp);
                }
            }
        }
        
        finalizeFileReading(filename, data);
    }
    
    private void hashAndAddToSB(StringBuilder sb, String hashMe)
    {
        String hash = UtilHandler.getHash(hashMe);
        
        if (hashes.contains(hash))
        {
            sb.append("\t\"").append(hash).append("\": \"").append(hashMe).append("\",\n");
            hashes.remove(hash);
        }
    }
    
    
    private void findIconPathInJsonArrayFile(Path filepath, String filename) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonElement elem = new JsonParser().parse(UtilHandler.readAsString(path));
        JsonArray   arr  = elem.getAsJsonArray();
        
        StringBuilder data = new StringBuilder("{");
        
        for (JsonElement element : arr)
        {
            JsonObject ob = element.getAsJsonObject();
            getElementAndCheckHash(ob, "iconPath", data);
        }
        
        finalizeFileReading(filename, data);
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
                
                
                ((Map<String, String>) new Gson().fromJson(UtilHandler.readAsString(p), new TypeToken<Map<String, String>>() {}.getType())).forEach((k, v) -> ml.add(new Pair<>(k, v)));
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
                
                ((Map<String, String>) new Gson().fromJson(UtilHandler.readAsString(p), new TypeToken<Map<String, String>>() {}.getType())).forEach((k, v) -> ml.add(new Pair<>(k, v)));
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
            doNestedLoop(depths[0], depths[1], pathPrefix + "%1$s/%2$s%3$03d." + fileType, unknownHashes, sb);
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
        for (int i = -1; i < max; i++)
        {
            String value = String.format(format, i);
            String hash  = UtilHandler.getHash(value);
            
            if (hashes.contains(hash))
            {
                sb.append("\t\"").append(hash).append("\": \"").append(value).append("\",\n");
                hashes.remove(hash);
            }
        }
    }
    
    private void doNestedLoop(int outerMax, int innerMax, String format, List<String> hashes, StringBuilder sb)
    {
        for (int i = -1; i < outerMax; i++)
        {
            for (int j = -1; j < innerMax; j++)
            {
                String value;
                if (j == 0)
                {
                    value = format.replace("%3$03d", "");
                    value = String.format(value, i, "metadata");
                } else
                {
                    value = String.format(format, i, i, j);
                }
                
                String hash = UtilHandler.getHash(value);
                
                if (hashes.contains(hash))
                {
                    sb.append("\t\"").append(hash).append("\": \"").append(value).append("\",\n");
                    hashes.remove(hash);
                }
            }
        }
    }
    
    
    private List<String> getUnknownHashes()
    {
        try
        {
            return Files.readAllLines(Paths.get("unknown.json"));
        } catch (IOException e)
        {
            e.printStackTrace();
            return Collections.emptyList();
        }
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

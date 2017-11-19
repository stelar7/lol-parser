
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
    
    private final List<String> exts = Arrays.asList("json", "txt", "png", "jpg", "jpeg", "webm", "ogg", "dds");
    List<String> hashes = getUnknownHashes();
    
    
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
        
        String pre = "plugins/rcp-be-lol-game-data/global/default/";
        
        List<String>  lines = Files.readAllLines(path);
        StringBuilder sb    = new StringBuilder();
        lines.forEach(sb::append);
        JsonObject    elem = new JsonParser().parse(sb.toString()).getAsJsonObject();
        StringBuilder data = new StringBuilder("{");
        
        JsonArray bflags = elem.getAsJsonArray("BannerFlags");
        for (JsonElement element : bflags)
        {
            JsonObject el = element.getAsJsonObject();
            
            String wip = el.get("inventoryIcon").getAsString().toLowerCase(Locale.ENGLISH);
            wip = wip.substring(wip.lastIndexOf("assets"));
            hashAndAddToSB(data, pre + wip);
            
            wip = el.get("profileIcon").getAsString().toLowerCase(Locale.ENGLISH);
            wip = wip.substring(wip.lastIndexOf("assets"));
            hashAndAddToSB(data, pre + wip);
        }
        
        JsonArray bframes = elem.getAsJsonArray("BannerFrames");
        for (JsonElement element : bframes)
        {
            JsonObject el = element.getAsJsonObject();
            
            String wip = el.get("inventoryIcon").getAsString().toLowerCase(Locale.ENGLISH);
            wip = wip.substring(wip.lastIndexOf("assets"));
            hashAndAddToSB(data, pre + wip);
            
            if (el.has("profileIcon"))
            {
                wip = el.get("profileIcon").getAsString().toLowerCase(Locale.ENGLISH);
                wip = wip.substring(wip.lastIndexOf("assets"));
                hashAndAddToSB(data, pre + wip);
            }
        }
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
        
        String pre = "plugins/rcp-be-lol-game-data/global/default/";
        
        List<String>  lines = Files.readAllLines(path);
        StringBuilder sb    = new StringBuilder();
        lines.forEach(sb::append);
        JsonArray     elem = new JsonParser().parse(sb.toString()).getAsJsonArray();
        StringBuilder data = new StringBuilder("{");
        
        for (JsonElement element : elem)
        {
            JsonObject el = element.getAsJsonObject();
            
            String wip = el.get("inventoryIcon").getAsString().toLowerCase(Locale.ENGLISH);
            wip = wip.substring(wip.lastIndexOf("assets"));
            hashAndAddToSB(data, pre + wip);
        }
        data.reverse().delete(0, 2).reverse().append("\n}");
        
        if (data.toString().length() < 10)
        {
            return;
        }
        
        Files.write(Paths.get(filename), data.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    private void parseMasteries(Path filepath, String filename) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        String pre = "plugins/rcp-be-lol-game-data/global/default/";
        
        List<String>  lines = Files.readAllLines(path);
        StringBuilder sb    = new StringBuilder();
        lines.forEach(sb::append);
        JsonObject    elem = new JsonParser().parse(sb.toString()).getAsJsonObject().getAsJsonObject("data");
        StringBuilder data = new StringBuilder("{");
        
        for (String key : elem.keySet())
        {
            JsonObject el = elem.getAsJsonObject(key);
            
            String wip = el.get("iconPath").getAsString().toLowerCase(Locale.ENGLISH);
            wip = wip.substring(wip.lastIndexOf("content"));
            hashAndAddToSB(data, pre + wip);
        }
        data.reverse().delete(0, 2).reverse().append("\n}");
        
        if (data.toString().length() < 10)
        {
            return;
        }
        
        Files.write(Paths.get(filename), data.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    private void parseWardSkins(Path filepath, String filename) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        String pre = "plugins/rcp-be-lol-game-data/global/default/";
        
        List<String>  lines = Files.readAllLines(path);
        StringBuilder sb    = new StringBuilder();
        lines.forEach(sb::append);
        JsonArray     elem = new JsonParser().parse(sb.toString()).getAsJsonArray();
        StringBuilder data = new StringBuilder("{");
        
        for (JsonElement element : elem)
        {
            JsonObject el = element.getAsJsonObject();
            
            String wip = el.get("wardImagePath").getAsString().toLowerCase(Locale.ENGLISH);
            wip = wip.substring(wip.lastIndexOf("content"));
            hashAndAddToSB(data, pre + wip);
            
            wip = el.get("wardShadowImagePath").getAsString().toLowerCase(Locale.ENGLISH);
            wip = wip.substring(wip.lastIndexOf("content"));
            hashAndAddToSB(data, pre + wip);
        }
        data.reverse().delete(0, 2).reverse().append("\n}");
        
        if (data.toString().length() < 10)
        {
            return;
        }
        
        Files.write(Paths.get(filename), data.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    private void findInChampionFile(Path filepath, String filename) throws IOException
    {
        String pre = "plugins/rcp-be-lol-game-data/global/default/";
        
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        List<String>  lines = Files.readAllLines(path);
        StringBuilder sb    = new StringBuilder();
        lines.forEach(sb::append);
        JsonObject    elem = new JsonParser().parse(sb.toString()).getAsJsonObject();
        StringBuilder data = new StringBuilder("{");
        
        String passive = elem.getAsJsonObject("passive").get("abilityIconPath").getAsString().toLowerCase(Locale.ENGLISH);
        
        if (!passive.isEmpty())
        {
            passive = passive.substring(passive.lastIndexOf("v1"));
            String hashMe = pre + passive;
            
            hashAndAddToSB(data, hashMe);
        }
        
        JsonArray arr = elem.getAsJsonArray("spells");
        
        for (JsonElement element : arr)
        {
            JsonObject current = element.getAsJsonObject();
            String     value   = current.getAsJsonObject().get("abilityIconPath").getAsString().toLowerCase(Locale.ENGLISH);
            
            value = value.substring(value.lastIndexOf("v1"));
            String hashMe = pre + value;
            
            hashAndAddToSB(data, hashMe);
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
            
            String splash = ob.get("splashPath").getAsString().toLowerCase(Locale.ENGLISH);
            splash = splash.substring(splash.lastIndexOf("v1"));
            hashAndAddToSB(data, pre + splash);
            
            String usplash = ob.get("uncenteredSplashPath").getAsString().toLowerCase(Locale.ENGLISH);
            usplash = usplash.substring(usplash.lastIndexOf("v1"));
            hashAndAddToSB(data, pre + usplash);
            
            String tile = ob.get("tilePath").getAsString().toLowerCase(Locale.ENGLISH);
            tile = tile.substring(tile.lastIndexOf("v1"));
            hashAndAddToSB(data, pre + tile);
            
            String load = ob.get("loadScreenPath").getAsString().toLowerCase(Locale.ENGLISH);
            if (load.contains("/data/"))
            {
                load = load.substring(load.lastIndexOf("data"));
            } else
            {
                load = load.substring(load.lastIndexOf("assets"));
            }
            hashAndAddToSB(data, pre + load);
            
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
        
        
        data.reverse().delete(0, 2).reverse().append("\n}");
        
        if (data.toString().length() < 10)
        {
            return;
        }
        
        Files.write(Paths.get(filename), data.toString().getBytes(StandardCharsets.UTF_8));
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
        String pre = "plugins/rcp-be-lol-game-data/global/default/";
        
        StringBuilder sb    = new StringBuilder();
        List<String>  lines = Files.readAllLines(filepath.resolve(filename));
        lines.forEach(sb::append);
        JsonElement elem = new JsonParser().parse(sb.toString());
        JsonArray   arr  = elem.getAsJsonArray();
        
        StringBuilder data = new StringBuilder("{");
        
        for (JsonElement element : arr)
        {
            String value = element.getAsJsonObject().get("iconPath").getAsString().toLowerCase(Locale.ENGLISH);
            
            if (value.contains("v1"))
            {
                value = value.substring(value.lastIndexOf("v1"));
            } else
            {
                value = value.substring(value.lastIndexOf("data"));
            }
            
            String hashMe = pre + value;
            hashAndAddToSB(data, hashMe);
        }
        data.reverse().delete(0, 2).reverse().append("\n}");
        
        if (data.toString().length() < 10)
        {
            return;
        }
        
        Files.write(Paths.get(filename), data.toString().getBytes(StandardCharsets.UTF_8));
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

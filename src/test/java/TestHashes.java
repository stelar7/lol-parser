import com.google.common.collect.Sets;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import javafx.util.Pair;
import no.stelar7.cdragon.util.*;
import no.stelar7.cdragon.wad.WADParser;
import no.stelar7.cdragon.wad.data.WADFile;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

public class TestHashes
{
    
    private final List<String> exts        = Arrays.asList("json", "txt", "png", "jpg", "jpeg", "webm", "ogg", "dds");
    private final String       pre         = "plugins/rcp-be-lol-game-data/global/default/";
    private final Path         outerFolder = Paths.get("tmp");
    
    private final List<String> filenames = Arrays.asList(
            "v1/championperkstylemap.json",
            "v1/champion-summary.json",
            "v1/hovertips.json",
            "v1/items.json",
            "v1/loot.json",
            "v1/map-assets/map-assets.json",
            "v1/maps.json",
            "v1/perks.json",
            "v1/perkstyles.json",
            "v1/profile-icons.json",
            "v1/queues.json",
            "v1/runes.json",
            "v1/skins.json",
            "v1/summoner-banners.json",
            "v1/summoner-emotes.json",
            "v1/summoner-masteries.json",
            "v1/summoner-spells.json",
            "v1/ward-skins.json"
                                                        );
    
    
    final int skinMax     = 50;
    final int championMax = 7500;
    final int iconMax     = 100000;
    
    private final Map<String, Integer[]> folderData = new HashMap<String, Integer[]>()
    {{
        put("profile-icons", new Integer[]{iconMax});
        
        put("champions", new Integer[]{championMax});
        put("champion-sfx-audios", new Integer[]{championMax});
        put("champion-icons", new Integer[]{championMax});
        put("champion-choose-vo", new Integer[]{championMax});
        put("champion-ban-vo", new Integer[]{championMax});
        put("summoner-backdrops", new Integer[]{iconMax});
        
        put("champion-tiles", new Integer[]{championMax, skinMax});
        put("champion-splashes", new Integer[]{championMax, skinMax});
        put("champion-chroma-images", new Integer[]{championMax, skinMax});
    }};
    
    private Path         currentInnerFolder;
    private List<String> hashes;
    
    private void runDirectory(Path dir) throws IOException, InterruptedException
    {
        Path file  = dir.resolve("plugins/rcp-be-lol-game-data/global/default/v1/");
        Path file2 = dir.resolve("plugins/rcp-be-lol-game-data/global/default/v1/champions");
        
        ExecutorService service     = Executors.newFixedThreadPool(8);
        Path            innerFolder = outerFolder.resolve(dir.getFileName());
        currentInnerFolder = innerFolder;
        hashes = getUnknownHashes(dir);
        
        if (!Files.exists(innerFolder))
        {
            Files.createDirectories(innerFolder);
        }
        
        System.out.println("Parsing default file locations");
        StringBuilder data = new StringBuilder("{\n");
        for (String filename : filenames)
        {
            hashAndAddToSB(data, pre + filename);
        }
        finalizeFileReading("files.json", data);
        
        
        System.out.println("Parsing hextech");
        StringBuilder data2 = new StringBuilder("{\n");
        
        for (String attempt : parseHextechFile())
        {
            hashAndAddToSB(data2, pre + "v1/hextech-images/" + attempt + ".png");
            hashAndAddToSB(data2, pre + "v1/rarity-gem-icons/" + attempt + ".png");
        }
        
        for (int i = -1; i < championMax; i++)
        {
            hashAndAddToSB(data2, pre + "v1/hextech-images/chest_" + i + ".png");
            hashAndAddToSB(data2, pre + "v1/hextech-images/chest_" + i + "_open.png");
            hashAndAddToSB(data2, pre + "v1/hextech-images/lootbundle_" + i + ".png");
            hashAndAddToSB(data2, pre + "v1/hextech-images/lootbundle_" + i + "_open.png");
            hashAndAddToSB(data2, pre + "v1/hextech-images/loottable_chest_" + i + ".png");
            hashAndAddToSB(data2, pre + "v1/hextech-images/loottable_chest_" + i + "_open.png");
            hashAndAddToSB(data2, pre + "v1/hextech-images/rarity-gem-icons/" + i + ".png");
            hashAndAddToSB(data2, pre + "v1/hextech-images/loottable_chest_generic_" + i + ".png");
            hashAndAddToSB(data2, pre + "v1/hextech-images/loottable_chest_generic_" + i + "_open.png");
            hashAndAddToSB(data2, pre + "v1/hextech-images/loottable_chest_champion_mastery_" + i + ".png");
            hashAndAddToSB(data2, pre + "v1/hextech-images/loottable_chest_champion_mastery_" + i + "_open.png");
            hashAndAddToSB(data2, pre + "v1/hextech-images/lootbundle_icon_cosmetic_" + i + ".png");
            hashAndAddToSB(data2, pre + "v1/hextech-images/lootbundle_icon_cosmetic_" + i + "_open.png");
            
            for (int j = -1; j < skinMax; j++)
            {
                hashAndAddToSB(data2, pre + "v1/hextech-images/loottable_chest_" + i + "_" + j + ".png");
                hashAndAddToSB(data2, pre + "v1/hextech-images/loottable_chest_" + i + "_" + j + "_open.png");
                
                String skinid = String.format("%d%03d", i, j);
                hashAndAddToSB(data2, pre + "v1/hextech-images/champion_skin_" + skinid + ".png ");
                hashAndAddToSB(data2, pre + "v1/hextech-images/champion_skin_rental_" + skinid + ".png ");
            }
        }
        
        // constants..
        hashAndAddToSB(data2, pre + "v1/rarity-gem-icons/epic.png");
        hashAndAddToSB(data2, pre + "v1/rarity-gem-icons/legendary.png");
        hashAndAddToSB(data2, pre + "v1/rarity-gem-icons/mythic.png");
        hashAndAddToSB(data2, pre + "v1/rarity-gem-icons/ultimate.png");
        
        hashAndAddToSB(data2, pre + "v1/hextech-images/hextech-images/chest.png");
        hashAndAddToSB(data2, pre + "v1/hextech-images/hextech-images/chest_champion_mastery.png");
        hashAndAddToSB(data2, pre + "v1/hextech-images/hextech-images/chest_key_bundle.png");
        hashAndAddToSB(data2, pre + "v1/hextech-images/hextech-images/chest_mystery_champion_shard.png");
        hashAndAddToSB(data2, pre + "v1/hextech-images/hextech-images/chest_promotion.png");
        finalizeFileReading("hextech.json", data2);
        
        
        System.out.println("Parsing icon files");
        findIconPathInJsonArrayFile(file, "perkstyles.json");
        findIconPathInJsonArrayFile(file, "perks.json");
        findIconPathInJsonArrayFile(file, "items.json");
        findIconPathInJsonArrayFile(file, "summoner-spells.json");
        findIconPathInJsonArrayFile(file, "profile-icons.json");
        
        parseWardSkins(file, "ward-skins.json");
        parseMasteries(file, "summoner-masteries.json");
        parseEmotes(file, "summoner-emotes.json");
        parseBanners(file, "summoner-banners.json");
        parseMapAssets(file, "map-assets/map-assets.json");
        
        System.out.println("Parsing champion files");
        for (int i = -1; i < championMax; i++)
        {
            findInChampionFile(file2, i + ".json");
        }
        
        System.out.println("Parsing data from unknown files");
        for (String ext : exts)
        {
            service.submit(() -> folderData.forEach((k, v) -> generateHashList(k, v, ext, hashes)));
        }
        
        service.shutdown();
        service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        
        
        folderData.put("perkstyles", new Integer[]{1});
        folderData.put("perks", new Integer[]{1});
        folderData.put("items", new Integer[]{1});
        folderData.put("summoner-spells", new Integer[]{1});
        folderData.put("profile-icons", new Integer[]{1});
        folderData.put("summoner-masteries", new Integer[]{1});
        folderData.put("ward-skins", new Integer[]{1});
        folderData.put("summoner-emotes", new Integer[]{1});
        folderData.put("summoner-banners", new Integer[]{1});
        folderData.put("map-assets", new Integer[]{1});
        folderData.put("files", new Integer[]{1});
        folderData.put("hextech", new Integer[]{1});
        
        
        for (int i = -1; i < championMax; i++)
        {
            folderData.put(String.valueOf(i), new Integer[]{1});
        }
        
        System.out.println("Merging files");
        combineAndDeleteTemp();
    }
    
    @Test
    public void testAllHashes() throws IOException, InterruptedException
    {
        Files.walkFileTree(Paths.get(System.getProperty("user.home"), "Downloads"), new SimpleFileVisitor<Path>()
        {
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                try
                {
                    if (dir.equals(Paths.get(System.getProperty("user.home"), "Downloads")))
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    runDirectory(dir);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                return FileVisitResult.SKIP_SUBTREE;
            }
            
        });
    }
    
    private List<String> parseHextechFile()
    {
        
        // check the lol-loot plugin for more... (4c0ce4a49dbc214c)
        
        WADParser parser      = new WADParser();
        String    pluginName  = "rcp-fe-lol-loot";
        Path      extractPath = Paths.get(System.getProperty("user.home"), "Downloads");
        
        try
        {
            WADFile parsed = parser.parseLatest(pluginName, extractPath);
            parsed.extractFiles(pluginName, null, extractPath);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        
        Path                possibleTech = Paths.get(System.getProperty("user.home"), "Downloads\\rcp-fe-lol-loot\\unknown", "4c0ce4a49dbc214c.json");
        Map<String, String> data         = new Gson().fromJson(UtilHandler.readAsString(possibleTech), new TypeToken<Map<String, String>>() {}.getType());
        return transmute(data.keySet());
    }
    
    private List<String> transmute(Set<String> strings)
    {
        List<String> all = new ArrayList<>();
        for (String key : strings)
        {
            String[]    keyArray = key.split("_");
            Set<String> keySet   = new HashSet<>();
            keySet.addAll(Arrays.asList(keyArray));
            Set<Set<String>> powers = Sets.powerSet(keySet);
            
            for (Set<String> power : powers)
            {
                StringJoiner sb = new StringJoiner("_");
                for (String p : power)
                {
                    if (p.isEmpty())
                    {
                        continue;
                    }
                    
                    sb.add(p.toLowerCase(Locale.ENGLISH));
                }
                all.add(sb.toString());
            }
        }
        return all;
    }
    
    private void combineAndDeleteTemp() throws IOException
    {
        List<Pair<String, String>> foundHashes = new ArrayList<>();
        
        Files.walkFileTree(currentInnerFolder, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                ((Map<String, String>) new Gson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>() {}.getType())).forEach((k, v) -> foundHashes.add(new Pair<>(k, v)));
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }
        });
        
        try
        {
            foundHashes.sort(Comparator.comparing(Pair::getValue, new NaturalOrderComparator()));
            
            StringBuilder sb = new StringBuilder("{\n");
            for (Pair<String, String> pair : foundHashes)
            {
                sb.append("\t\"").append(pair.getKey()).append("\": \"").append(pair.getValue()).append("\",\n");
            }
            sb.reverse().delete(0, 2).reverse().append("\n}");
            
            if (sb.toString().length() > 10)
            {
                Files.createDirectories(currentInnerFolder);
                Files.write(currentInnerFolder.resolve("combined.json"), sb.toString().getBytes(StandardCharsets.UTF_8));
                System.out.println("New hashes found!!");
            } else
            {
                Files.deleteIfExists(currentInnerFolder.resolve("combined.json"));
                System.out.println("No new hashes found");
            }
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void parseMapAssets(Path filepath, String filename)
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonObject    elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonObject();
        StringBuilder data = new StringBuilder("{\n");
        
        for (String key : elem.keySet())
        {
            JsonArray array = elem.getAsJsonArray(key);
            
            for (JsonElement element : array)
            {
                JsonObject obj = element.getAsJsonObject().getAsJsonObject("assets");
                
                getElementAndCheckHash(obj, "champ-select-flyout-background", data);
                getElementAndCheckHash(obj, "champ-select-planning-intro", data);
                getElementAndCheckHash(obj, "game-select-icon-default", data);
                getElementAndCheckHash(obj, "game-select-icon-disabled", data);
                getElementAndCheckHash(obj, "game-select-icon-hover", data);
                getElementAndCheckHash(obj, "icon-defeat", data);
                getElementAndCheckHash(obj, "icon-empty", data);
                getElementAndCheckHash(obj, "icon-hover", data);
                getElementAndCheckHash(obj, "icon-leaver", data);
                getElementAndCheckHash(obj, "icon-victory", data);
                getElementAndCheckHash(obj, "parties-background", data);
                getElementAndCheckHash(obj, "social-icon-leaver", data);
                getElementAndCheckHash(obj, "social-icon-victory", data);
                getElementAndCheckHash(obj, "game-select-icon-active", data);
                getElementAndCheckHash(obj, "ready-check-background", data);
                getElementAndCheckHash(obj, "map-north", data);
                getElementAndCheckHash(obj, "map-south", data);
                getElementAndCheckHash(obj, "gameflow-background", data);
                getElementAndCheckHash(obj, "notification-background", data);
                getElementAndCheckHash(obj, "notification-icon", data);
                getElementAndCheckHash(obj, "champ-select-background-sound", data);
                getElementAndCheckHash(obj, "gameselect-button-hover-sound", data);
                getElementAndCheckHash(obj, "music-inqueue-loop-sound", data);
                getElementAndCheckHash(obj, "postgame-ambience-loop-sound", data);
                getElementAndCheckHash(obj, "sfx-ambience-pregame-loop-sound", data);
                getElementAndCheckHash(obj, "ready-check-background-sound", data);
                getElementAndCheckHash(obj, "game-select-icon-active-video", data);
                getElementAndCheckHash(obj, "game-select-icon-intro-video", data);
                getElementAndCheckHash(obj, "icon-defeat-video", data);
                getElementAndCheckHash(obj, "icon-victory-video", data);
            }
        }
        
        finalizeFileReading("map-assets.json", data);
    }
    
    private void parseBanners(Path filepath, String filename)
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonObject    elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonObject();
        StringBuilder data = new StringBuilder("{\n");
        
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
                getElementAndCheckHash(el, "profileIcon", data);
            }
        }
        finalizeFileReading(filename, data);
    }
    
    private void finalizeFileReading(String filename, StringBuilder data)
    {
        try
        {
            data.reverse().delete(0, 2).reverse().append("\n}");
            if (data.toString().length() < 10)
            {
                return;
            }
            
            if (!Files.exists(currentInnerFolder))
            {
                Files.createDirectories(currentInnerFolder);
            }
            
            Files.write(currentInnerFolder.resolve(filename), data.toString().getBytes(StandardCharsets.UTF_8));
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void parseEmotes(Path filepath, String filename)
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonArray     elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonArray();
        StringBuilder data = new StringBuilder("{\n");
        
        for (JsonElement element : elem)
        {
            JsonObject el = element.getAsJsonObject();
            getElementAndCheckHash(el, "inventoryIcon", data);
        }
        
        finalizeFileReading(filename, data);
    }
    
    private void parseMasteries(Path filepath, String filename)
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonObject    elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonObject().getAsJsonObject("data");
        StringBuilder data = new StringBuilder("{\n");
        
        for (String key : elem.keySet())
        {
            JsonObject el = elem.getAsJsonObject(key);
            getElementAndCheckHash(el, "iconPath", data);
        }
        finalizeFileReading(filename, data);
    }
    
    private void getElementAndCheckHash(JsonObject el, String path, StringBuilder data)
    {
        if (!el.has(path))
        {
            return;
        }
        
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
    
    private void parseWardSkins(Path filepath, String filename)
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonArray     elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonArray();
        StringBuilder data = new StringBuilder("{\n");
        
        for (JsonElement element : elem)
        {
            JsonObject el = element.getAsJsonObject();
            
            getElementAndCheckHash(el, "wardImagePath", data);
            getElementAndCheckHash(el, "wardShadowImagePath", data);
        }
        finalizeFileReading(filename, data);
    }
    
    private void findInChampionFile(Path filepath, String filename)
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonObject    elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonObject();
        StringBuilder data = new StringBuilder("{\n");
        
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
            getElementAndCheckHash(ob, "splashVideoPath", data);
            
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
    
    private void findIconPathInJsonArrayFile(Path filepath, String filename)
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonElement elem = new JsonParser().parse(UtilHandler.readAsString(path));
        JsonArray   arr  = elem.getAsJsonArray();
        
        StringBuilder data = new StringBuilder("{\n");
        
        for (JsonElement element : arr)
        {
            JsonObject ob = element.getAsJsonObject();
            getElementAndCheckHash(ob, "iconPath", data);
        }
        
        finalizeFileReading(filename, data);
    }
    
    private void generateHashList(String folderName, Integer[] depths, String fileType, List<String> unknownHashes)
    {
        String pathPrefix = pre + "v1/" + folderName + "/";
        
        StringBuilder sb = new StringBuilder("{\n");
        
        if (depths.length == 1)
        {
            doLoop(depths[0], pathPrefix + "%s." + fileType, unknownHashes, sb);
        } else
        {
            doNestedLoop(depths[0], depths[1], pathPrefix + "%1$s/%2$s%3$03d." + fileType, unknownHashes, sb);
        }
        
        finalizeFileReading(folderName + "." + fileType + ".json", sb);
    }
    
    private void doLoop(int max, String format, List<String> hashes, StringBuilder sb)
    {
        for (int i = -1; i < max; i++)
        {
            String value = String.format(format, i);
            hashAndAddToSB(sb, value);
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
                
                hashAndAddToSB(sb, value);
            }
        }
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
    
    
    private List<String> getUnknownHashes(Path dir)
    {
        try
        {
            return Files.readAllLines(dir.resolve("unknown.json"));
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

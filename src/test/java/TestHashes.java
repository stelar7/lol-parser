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

public class TestHashes
{
    
    private final List<String> exts   = Arrays.asList("json", "txt", "png", "jpg", "jpeg", "webm", "ogg", "dds");
    private final String       prePre = "plugins/rcp-be-lol-game-data/";
    
    private final List<String> preRegion = Arrays.asList(
            "global",
            "br",
            "cn",
            "eune",
            "eun",
            "euw",
            "garena",
            "garena2",
            "garena3",
            "id",
            "jp",
            "kr",
            "la",
            "la1",
            "la2",
            "lan",
            "las",
            "my",
            "na",
            "oc",
            "oc1",
            "oce",
            "pbe",
            "ph",
            "ru",
            "sea",
            "sg",
            "tencent",
            "tr",
            "th",
            "tw",
            "vn"
                                                        );
    
    private final List<String> preLang = Arrays.asList(
            "default",
            "cs_cz",
            "de_de",
            "el_gr",
            "en_au",
            "en_gb",
            "en_ph",
            "en_sg",
            "en_us",
            "es_ar",
            "es_es",
            "es_mx",
            "fr_fr",
            "hu_hu",
            "id_id",
            "it_it",
            "ja_jp",
            "ko_kr",
            "ms_my",
            "pl_pl",
            "pt_br",
            "ro_ro",
            "ru_ru",
            "th_th",
            "tr_tr",
            "vn_vn",
            "zh_cn",
            "zh_my",
            "zh_tw"
                                                      );
    
    
    private final Path outerFolder = Paths.get("tmp_hashes");
    
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
            "v1/summoner-trophies.json",
            "v1/ward-skins.json"
                                                        );
    
    
    final int skinMax     = 50;
    final int championMax = 750;
    final int iconMax     = 10000;
    
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
    
    private void runDirectory(Path dir) throws IOException
    {
        Path innerFolder = outerFolder.resolve(dir.getFileName());
        currentInnerFolder = innerFolder;
        hashes = getUnknownHashes(dir);
        
        if (hashes.isEmpty())
        {
            return;
        }
        
        if (!Files.exists(innerFolder))
        {
            Files.createDirectories(innerFolder);
        }
        
        StringBuilder filenameBuilder = new StringBuilder("{\n");
        StringBuilder hextechBuilder  = new StringBuilder("{\n");
        List<String>  hextechValues   = parseHextechFile();
        
        for (String reg : preRegion)
        {
            for (String lan : preLang)
            {
                String pre   = prePre + reg + "/" + lan + "/";
                Path   file  = dir.resolve(pre + "v1");
                Path   file2 = dir.resolve(pre + "v1/champions");
                
                System.out.println("Parsing " + pre);
                
                System.out.println("Parsing filenames");
                for (String filename : filenames)
                {
                    hashAndAddToSB(filenameBuilder, pre + filename);
                }
                
                System.out.println("Parsing champions");
                for (int i = -1; i < championMax; i++)
                {
                    findInChampionFile(file2, i + ".json");
                }
                
                System.out.println("Parsing hextech");
                doHextechParse(hextechBuilder, hextechValues, pre);
                
                System.out.println("Parsing icons");
                parseIcons(file);
                
                for (final String exten : exts)
                {
                    folderData.forEach((folderName, depths) -> generateHashList(pre, folderName, depths, exten));
                }
            }
        }
        
        finalizeFileReading("files.json", filenameBuilder);
        finalizeFileReading("hextech.json", hextechBuilder);
        
        
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
    
    private void parseIcons(Path file)
    {
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
    }
    
    private void doHextechParse(StringBuilder data2, List<String> hextechValues, String pre)
    {
        for (String attempt : hextechValues)
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
                hashAndAddToSB(data2, pre + "v1/hextech-images/champion_skin_" + skinid + ".png");
                hashAndAddToSB(data2, pre + "v1/hextech-images/champion_skin_rental_" + skinid + ".png");
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
    }
    
    
    private void combineAndDeleteNestedTemp() throws IOException
    {
        List<Pair<String, String>> foundHashes = new ArrayList<>();
        
        if (!Files.exists(outerFolder))
        {
            return;
        }
        
        System.out.println("Combining hashes");
        
        Files.walkFileTree(outerFolder, new SimpleFileVisitor<Path>()
        {
            
            @Override
            @SuppressWarnings(value = "unchecked")
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                ((Map<String, String>) new Gson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>()
                {
                }.getType())).forEach((k, v) -> {
                    Pair<String, String> data = new Pair<>(k, v);
                    if (!foundHashes.contains(data))
                    {
                        foundHashes.add(new Pair<>(k, v));
                    }
                });
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
                Files.write(Paths.get("combined.json"), sb.toString().getBytes(StandardCharsets.UTF_8));
                System.out.println("New hashes found!!");
            } else
            {
                Files.deleteIfExists(Paths.get("combined.json"));
                System.out.println("No new hashes found");
            }
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private List<String> parseHextechFile()
    {
        
        // check the lol-loot plugin for more... (4c0ce4a49dbc214c)
        
        WADParser parser      = new WADParser();
        String    pluginName  = "rcp-fe-lol-loot";
        Path      extractPath = Paths.get(System.getProperty("user.home"), "Downloads");
        
        if (!Files.exists(extractPath.resolve(pluginName)))
        {
            try
            {
                WADFile parsed = parser.parseLatest(pluginName, extractPath);
                parsed.extractFiles(pluginName, null, extractPath);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        
        Path possibleTech = Paths.get(System.getProperty("user.home"), "Downloads\\rcp-fe-lol-loot\\plugins\\rcp-fe-lol-loot\\global\\default", "trans.json");
        Map<String, String> data = new Gson().fromJson(UtilHandler.readAsString(possibleTech), new TypeToken<Map<String, String>>()
        {
        }.getType());
        return transmute(data.keySet());
    }
    
    private List<String> transmute(Set<String> strings)
    {
        List<String> all = new ArrayList<>();
        for (String key : strings)
        {
            String[]         keyArray = key.split("_");
            Set<String>      keySet   = new HashSet<>(Arrays.asList(keyArray));
            Set<Set<String>> powers   = Sets.powerSet(keySet);
            
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
                ((Map<String, String>) new Gson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>()
                {
                }.getType())).forEach((k, v) -> {
                    Pair<String, String> data = new Pair<>(k, v);
                    if (!foundHashes.contains(data))
                    {
                        foundHashes.add(new Pair<>(k, v));
                    }
                });
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
            if (elem.get(key).isJsonArray())
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
            } else
            {
                JsonObject obj = elem.getAsJsonObject(key).getAsJsonObject("assets");
                
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
            
            Files.write(currentInnerFolder.resolve(filename), data.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
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
            System.err.println("WTF?? no WIP?");
            System.err.println(wip);
            for (String reg : preRegion)
            {
                for (String lan : preLang)
                {
                    String pre = prePre + reg + "/" + lan + "/";
                    System.err.println(pre + wip);
                }
            }
        }
        
        for (String reg : preRegion)
        {
            for (String lan : preLang)
            {
                String pre = prePre + reg + "/" + lan + "/";
                hashAndAddToSB(data, pre + wip);
            }
        }
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
        
        if (elem.has("passive"))
        {
            getElementAndCheckHash(elem.getAsJsonObject("passive"), "abilityIconPath", data);
        }
        
        if (elem.has("spells"))
        {
            JsonArray arr = elem.getAsJsonArray("spells");
            for (JsonElement element : arr)
            {
                JsonObject current = element.getAsJsonObject();
                getElementAndCheckHash(current, "abilityIconPath", data);
            }
        }
        
        if (elem.has("recommendedItemDefaults"))
        {
            JsonArray arr = elem.getAsJsonArray("recommendedItemDefaults");
            for (JsonElement element : arr)
            {
                String value = element.getAsString().toLowerCase(Locale.ENGLISH);
                value = value.substring(value.indexOf("data"));
                for (String reg : preRegion)
                {
                    for (String lan : preLang)
                    {
                        String pre    = prePre + reg + "/" + lan + "/";
                        String hashMe = pre + value;
                        hashAndAddToSB(data, hashMe);
                    }
                }
            }
        }
        
        
        if (elem.has("skins"))
        {
            JsonArray arr = elem.getAsJsonArray("skins");
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
                        for (String reg : preRegion)
                        {
                            for (String lan : preLang)
                            {
                                String pre = prePre + reg + "/" + lan + "/";
                                hashAndAddToSB(data, pre + cp);
                            }
                        }
                    }
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
        if (elem.isJsonNull())
        {
            return;
        }
        JsonArray arr = elem.getAsJsonArray();
        
        StringBuilder data = new StringBuilder("{\n");
        
        for (JsonElement element : arr)
        {
            JsonObject ob = element.getAsJsonObject();
            getElementAndCheckHash(ob, "iconPath", data);
        }
        
        finalizeFileReading(filename, data);
    }
    
    private void generateHashList(String pre, String folderName, Integer[] depths, String fileType)
    {
        StringBuilder sb = new StringBuilder("{\n");
        
        String pathPrefix = pre + "v1/" + folderName + "/";
        if (depths.length == 1)
        {
            doLoop(depths[0], pathPrefix + "%s." + fileType, sb);
        } else
        {
            doNestedLoop(depths[0], depths[1], pathPrefix + "%1$s/%2$s%3$03d." + fileType, sb);
        }
        
        finalizeFileReading(folderName + "." + fileType + ".json", sb);
    }
    
    
    private void doLoop(int max, String format, StringBuilder sb)
    {
        for (int i = -1; i < max; i++)
        {
            String value = String.format(format, i);
            hashAndAddToSB(sb, value);
        }
    }
    
    private void doNestedLoop(int outerMax, int innerMax, String format, StringBuilder sb)
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
        String hash = UtilHandler.getHash(hashMe.trim());
        
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
    
    @Test
    public void testAllHashes() throws IOException
    {
        Files.walkFileTree(Paths.get(System.getProperty("user.home"), "Downloads"), new SimpleFileVisitor<Path>()
        {
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                if (dir.equals(Paths.get(System.getProperty("user.home"), "Downloads")))
                {
                    return FileVisitResult.CONTINUE;
                }
                
                if (dir.toAbsolutePath().toString().contains("lol-loot"))
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                System.out.println(dir.toAbsolutePath().toString());
                runDirectory(dir);
                
                return FileVisitResult.SKIP_SUBTREE;
            }
        });
        
        combineAndDeleteNestedTemp();
    }
    
    @Test
    public void testAllLangKnownPaths() throws IOException
    {
        Files.walkFileTree(Paths.get("hashes"), new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                String filename = file.getFileName().toString().substring(0, file.getFileName().toString().lastIndexOf('.'));
                
                final List<String>               foundHashes = new ArrayList<>();
                final List<Pair<String, String>> knownHashes = new ArrayList<>();
                
                ((Map<String, String>) new Gson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>()
                {
                }.getType())).forEach((k, v) -> {
                    Pair<String, String> data = new Pair<>(k, v);
                    if (!knownHashes.contains(data))
                    {
                        knownHashes.add(data);
                    }
                });
                
                ((Map<String, String>) new Gson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>()
                {
                }.getType())).forEach((k, v) -> {
                    String insert = v.substring(("plugins/" + filename + "/").length());
                    insert = insert.substring(insert.indexOf('/') + 1);
                    insert = insert.substring(insert.indexOf('/') + 1);
                    if (!foundHashes.contains(insert))
                    {
                        foundHashes.add(insert);
                    }
                });
                
                for (String reg : preRegion)
                {
                    System.out.println(reg);
                    for (String lan : preLang)
                    {
                        System.out.println(lan);
                        
                        String pre = "plugins/" + filename + "/" + reg + "/" + lan + "/";
                        for (String end : foundHashes)
                        {
                            String hashMe = pre + end;
                            String hash   = UtilHandler.getHash(hashMe.trim());
                            
                            Pair<String, String> data = new Pair<>(hash, hashMe);
                            if (!knownHashes.contains(data))
                            {
                                knownHashes.add(data);
                            }
                        }
                    }
                }
                
                knownHashes.sort(Comparator.comparing(Pair::getValue, new NaturalOrderComparator()));
                StringBuilder sb = new StringBuilder("{\n");
                for (Pair<String, String> pair : knownHashes)
                {
                    sb.append("\t\"").append(pair.getKey()).append("\": \"").append(pair.getValue()).append("\",\n");
                }
                sb.reverse().delete(0, 2).reverse().append("\n}");
                
                Files.createDirectories(Paths.get("hashes", "fixed"));
                Files.write(Paths.get("hashes", "fixed", file.getFileName().toString()), sb.toString().getBytes(StandardCharsets.UTF_8));
                
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    @Test
    public void testSortAllHashes() throws IOException
    {
        
        Files.walkFileTree(Paths.get("hashes"), new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                System.out.println(file.toAbsolutePath().toString());
                
                final List<Pair<String, String>> foundHashes = new ArrayList<>();
                ((Map<String, String>) new Gson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>()
                {
                }.getType())).forEach((k, v) -> {
                    Pair<String, String> data = new Pair<>(k, v);
                    if (!foundHashes.contains(data))
                    {
                        foundHashes.add(new Pair<>(k, v));
                    }
                });
                
                foundHashes.sort(Comparator.comparing(Pair::getValue, new NaturalOrderComparator()));
                
                StringBuilder sb = new StringBuilder("{\n");
                for (Pair<String, String> pair : foundHashes)
                {
                    sb.append("\t\"").append(pair.getKey()).append("\": \"").append(pair.getValue()).append("\",\n");
                }
                sb.reverse().delete(0, 2).reverse().append("\n}");
                
                Files.createDirectories(Paths.get("hashes", "fixed"));
                Files.write(Paths.get("hashes", "fixed", file.getFileName().toString()), sb.toString().getBytes(StandardCharsets.UTF_8));
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    @Test
    public void testJoinSplitClientHashes() throws IOException
    {
        Path file         = Paths.get(System.getProperty("user.home"), "Downloads", "morehash.json");
        Path newHashStore = Paths.get(System.getProperty("user.home"), "Downloads", "newhash");
        
        List<Pair<String, String>> foundHashes = new ArrayList<>();
        
        Map<String, StringBuilder> pluginData = new HashMap<>();
        
        Files.walkFileTree(Paths.get("hashes"), new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
            {
                ((Map<String, String>) new Gson().fromJson(UtilHandler.readAsString(path), new TypeToken<Map<String, String>>()
                {
                }.getType())).forEach((k, v) -> {
                    Pair<String, String> data = new Pair<>(k, v);
                    if (!foundHashes.contains(data))
                    {
                        foundHashes.add(data);
                    }
                });
                return FileVisitResult.CONTINUE;
            }
        });
        
        ((List<String>) new Gson().fromJson(UtilHandler.readAsString(file), new TypeToken<List<String>>()
        {
        }.getType())).forEach((v) -> {
            Pair<String, String> data = new Pair<>(UtilHandler.getHash(v), v);
            if (!foundHashes.contains(data))
            {
                foundHashes.add(data);
            }
        });
        
        foundHashes.sort(Comparator.comparing(Pair::getValue, new NaturalOrderComparator()));
        for (Pair<String, String> pair : foundHashes)
        {
            String builder = pair.getValue().substring("plugins/".length());
            if (builder.indexOf('/') < 0)
            {
                System.out.println(pair.getValue());
            }
            
            builder = builder.substring(0, builder.indexOf('/'));
            
            StringBuilder sb = pluginData.computeIfAbsent(builder, (k) -> new StringBuilder("{\n"));
            sb.append("\t\"").append(pair.getKey()).append("\": \"").append(pair.getValue()).append("\",\n");
        }
        pluginData.forEach((k, sb) -> {
            sb.reverse().delete(0, 2).reverse().append("\n}");
            try
            {
                Files.createDirectories(newHashStore);
                Files.write(newHashStore.resolve(k + ".json"), sb.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }
    
    @Test
    public void testMakePure() throws IOException
    {
        Path                       newHashStore = Paths.get(System.getProperty("user.home"), "Downloads", "newhash");
        List<Pair<String, String>> foundHashes  = new ArrayList<>();
        Map<String, StringBuilder> pluginData   = new HashMap<>();
        
        Files.walkFileTree(Paths.get("hashes"), new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                ((Map<String, String>) new Gson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>()
                {
                }.getType())).forEach((k, v) -> {
                    Pair<String, String> data = new Pair<>(k, v);
                    if (!foundHashes.contains(data))
                    {
                        foundHashes.add(new Pair<>(k, v));
                    }
                });
                return FileVisitResult.CONTINUE;
            }
        });
        
        foundHashes.sort(Comparator.comparing(Pair::getValue, new NaturalOrderComparator()));
        for (Pair<String, String> pair : foundHashes)
        {
            String[] temp    = pair.getValue().substring("plugins/".length()).split("/");
            String   builder = temp[0];
            
            if (temp.length == 2)
            {
                StringBuilder sb = pluginData.computeIfAbsent(builder, (k) -> new StringBuilder("{\n"));
                sb.append("\t\"").append(pair.getKey()).append("\": \"").append(pair.getValue()).append("\",\n");
                continue;
            }
            
            String region = temp[1];
            String lang   = temp[2];
            
            if (!region.equalsIgnoreCase("global"))
            {
                continue;
            }
            
            if (!lang.equalsIgnoreCase("default"))
            {
                continue;
            }
            
            StringBuilder sb = pluginData.computeIfAbsent(builder, (k) -> new StringBuilder("{\n"));
            sb.append("\t\"").append(pair.getKey()).append("\": \"").append(pair.getValue()).append("\",\n");
        }
        pluginData.forEach((k, sb) -> {
            sb.reverse().delete(0, 2).reverse().append("\n}");
            try
            {
                Files.createDirectories(newHashStore);
                Files.write(newHashStore.resolve(k + ".json"), sb.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
        
    }
    
    @Test
    public void testJoinSplitHashes() throws IOException
    {
        Path file         = Paths.get(System.getProperty("user.home"), "Downloads", "morehash.json");
        Path newHashStore = Paths.get(System.getProperty("user.home"), "Downloads", "newhash");
        
        List<Pair<String, String>> foundHashes = new ArrayList<>();
        
        Map<String, StringBuilder> pluginData = new HashMap<>();
        
        Files.walkFileTree(Paths.get("hashes"), new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                ((Map<String, String>) new Gson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>()
                {
                }.getType())).forEach((k, v) -> {
                    Pair<String, String> data = new Pair<>(k, v);
                    if (!foundHashes.contains(data))
                    {
                        foundHashes.add(new Pair<>(k, v));
                    }
                });
                return FileVisitResult.CONTINUE;
            }
        });
        
        ((Map<String, String>) new Gson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>()
        {
        }.getType())).forEach((k, v) -> {
            Pair<String, String> data = new Pair<>(k, v);
            if (!foundHashes.contains(data))
            {
                foundHashes.add(new Pair<>(k, v));
            }
        });
        
        foundHashes.sort(Comparator.comparing(Pair::getValue, new NaturalOrderComparator()));
        for (Pair<String, String> pair : foundHashes)
        {
            String builder = pair.getValue().substring("plugins/".length());
            builder = builder.substring(0, builder.indexOf('/'));
            
            StringBuilder sb = pluginData.computeIfAbsent(builder, (k) -> new StringBuilder("{\n"));
            sb.append("\t\"").append(pair.getKey()).append("\": \"").append(pair.getValue()).append("\",\n");
        }
        pluginData.forEach((k, sb) -> {
            sb.reverse().delete(0, 2).reverse().append("\n}");
            try
            {
                Files.createDirectories(newHashStore);
                Files.write(newHashStore.resolve(k + ".json"), sb.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }
}

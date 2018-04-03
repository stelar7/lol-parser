package types.util;

import com.google.common.collect.Sets;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.Vector2;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.net.ftp.*;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class TestAll
{
    @Test
    public void testAll() throws IOException
    {
        downloadWAD();
        getHashes();
        deleteUnknownFolder();
        downloadWAD();
        //extractImages();
        //  uploadToFTP();
    }
    
    private void deleteUnknownFolder() throws IOException
    {
        Path parent = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data");
        parent.resolve("unknown.json").toFile().delete();
        Files.walkFileTree(parent.resolve("unknown"), new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                file.toFile().delete();
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            {
                dir.toFile().delete();
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    private void downloadWAD()
    {
        WADParser parser = new WADParser();
        
        String pluginName  = "rcp-be-lol-game-data";
        Path   extractPath = UtilHandler.DOWNLOADS_FOLDER;
        
        WADFile parsed = parser.parseLatest(pluginName, extractPath, true);
        parsed.extractFiles(pluginName, null, extractPath);
    }
    
    private final List<String> exts   = Arrays.asList("json", "txt", "png", "jpg", "jpeg", "webm", "ogg", "dds");
    private final String       prePre = "plugins/rcp-be-lol-game-data/";
    
    private final List<String> preRegion = Collections.singletonList("global");
    private final List<String> preLang   = Collections.singletonList("default");
    
    
    private final Path tmp_hashes = Paths.get("tmp_hashes");
    
    private final List<String> filenames = Arrays.asList(
            "v1/champion-summary.json",
            "v1/championperkstylemap.json",
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
            "v1/settingstopersist.json",
            "v1/skinlines.json",
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
    
    private final Map<String, Integer[]> folderData = new HashMap<>()
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
        Path innerFolder = tmp_hashes.resolve(dir.getFileName());
        currentInnerFolder = innerFolder;
        hashes = getUnknownHashes();
        
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
        StringBuilder lootBuilder     = new StringBuilder("{\n");
        StringBuilder clashBuilder    = new StringBuilder("{\n");
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
                
                System.out.println("Parsing loot");
                doLootParse(lootBuilder, hextechValues, pre);
                
                System.out.println("Parsing icons");
                parseIcons(file);
                
                System.out.println("Parsing clash voices");
                parseClash(pre, clashBuilder);
                
                
                for (final String exten : exts)
                {
                    folderData.forEach((folderName, depths) -> generateHashList(pre, folderName, depths, exten));
                }
            }
        }
        
        finalizeFileReading("files.json", filenameBuilder);
        finalizeFileReading("hextech.json", hextechBuilder);
        finalizeFileReading("hexloot.json", lootBuilder);
        finalizeFileReading("clash.json", clashBuilder);
        
        
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
        folderData.put("hexloot", new Integer[]{1});
        
        
        for (int i = -1; i < championMax; i++)
        {
            folderData.put(String.valueOf(i), new Integer[]{1});
        }
        
        System.out.println("Merging files");
        
        combineAndDeleteTemp();
    }
    
    private void parseClash(String pre, StringBuilder sb)
    {
        Path                loadPath = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\unknown\\9d3e847588265a68.json");
        Map<String, String> data     = UtilHandler.getGson().fromJson(UtilHandler.readAsString(loadPath), new TypeToken<Map<String, String>>() {}.getType());
        
        for (Entry<String, String> en : data.entrySet())
        {
            String line = en.getValue();
            String wip  = line.substring(line.lastIndexOf("content"));
            hashAndAddToSB(sb, pre + wip);
        }
    }
    
    private void parseIcons(Path file)
    {
        findIconPathInJsonArrayFile(file, "perkstyles.json");
        findIconPathInJsonArrayFile(file, "perks.json");
        findIconPathInJsonArrayFile(file, "items.json");
        findIconPathInJsonArrayFile(file, "summoner-spells.json");
        findIconPathInJsonArrayFile(file, "profile-icons.json");
        
        parseLootFile(file, "loot.json");
        parseWardSkins(file, "ward-skins.json");
        parseMasteries(file, "summoner-masteries.json");
        parseEmotes(file, "summoner-emotes.json");
        parseBanners(file, "summoner-banners.json");
        parseMapAssets(file, "map-assets/map-assets.json");
    }
    
    private void parseLootFile(Path filepath, String filename)
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        String     jsonData = UtilHandler.readAsString(path);
        JsonObject element  = new JsonParser().parse(jsonData).getAsJsonObject();
        
        JsonArray     elem  = element.getAsJsonArray("LootItems");
        JsonArray     elem2 = element.getAsJsonArray("LootRecipes");
        JsonArray     elem3 = element.getAsJsonArray("LootTables");
        StringBuilder data  = new StringBuilder("{\n");
        
        for (JsonElement val : elem)
        {
            JsonObject el = val.getAsJsonObject();
            getElementAndCheckHash(el, "image", data);
        }
        
        for (JsonElement val : elem3)
        {
            JsonObject el = val.getAsJsonObject();
            getElementAndCheckHash(el, "image", data);
        }
        
        for (JsonElement val : elem2)
        {
            JsonObject el = val.getAsJsonObject();
            getElementAndCheckHash(el, "imagePath", data);
        }
        
        finalizeFileReading(filename, data);
        
    }
    
    private void doLootParse(StringBuilder data2, List<String> hextechValues, String pre)
    {
        for (String attempt : hextechValues)
        {
            hashAndAddToSB(data2, pre + "assets/loot/" + attempt + ".png");
        }
        
        for (int i = -1; i < championMax; i++)
        {
            hashAndAddToSB(data2, pre + "assets/loot/chest_" + i + ".png");
            hashAndAddToSB(data2, pre + "assets/loot/chest_" + i + "_open.png");
            hashAndAddToSB(data2, pre + "assets/loot/lootbundle_" + i + ".png");
            hashAndAddToSB(data2, pre + "assets/loot/lootbundle_" + i + "_open.png");
            hashAndAddToSB(data2, pre + "assets/loot/loottable_chest_" + i + ".png");
            hashAndAddToSB(data2, pre + "assets/loot/loottable_chest_" + i + "_open.png");
            hashAndAddToSB(data2, pre + "assets/loot/rarity-gem-icons/" + i + ".png");
            hashAndAddToSB(data2, pre + "assets/loot/loottable_chest_generic_" + i + ".png");
            hashAndAddToSB(data2, pre + "assets/loot/loottable_chest_generic_" + i + "_open.png");
            hashAndAddToSB(data2, pre + "assets/loot/loottable_chest_champion_mastery_" + i + ".png");
            hashAndAddToSB(data2, pre + "assets/loot/loottable_chest_champion_mastery_" + i + "_open.png");
            hashAndAddToSB(data2, pre + "assets/loot/lootbundle_icon_cosmetic_" + i + ".png");
            hashAndAddToSB(data2, pre + "assets/loot/lootbundle_icon_cosmetic_" + i + "_open.png");
            
            for (int j = -1; j < skinMax; j++)
            {
                hashAndAddToSB(data2, pre + "assets/loot/loottable_chest_" + i + "_" + j + ".png");
                hashAndAddToSB(data2, pre + "assets/loot/loottable_chest_" + i + "_" + j + "_open.png");
                
                String skinid = String.format("%d%03d", i, j);
                hashAndAddToSB(data2, pre + "assets/loot/champion_skin_" + skinid + ".png");
                hashAndAddToSB(data2, pre + "assets/loot/champion_skin_rental_" + skinid + ".png");
            }
        }
        
        // constants..
        hashAndAddToSB(data2, pre + "assets/loot/epic.png");
        hashAndAddToSB(data2, pre + "assets/loot/legendary.png");
        hashAndAddToSB(data2, pre + "assets/loot/mythic.png");
        hashAndAddToSB(data2, pre + "assets/loot/ultimate.png");
        
        hashAndAddToSB(data2, pre + "assets/loot/hextech-images/chest.png");
        hashAndAddToSB(data2, pre + "assets/loot/hextech-images/chest_champion_mastery.png");
        hashAndAddToSB(data2, pre + "assets/loot/hextech-images/chest_key_bundle.png");
        hashAndAddToSB(data2, pre + "assets/loot/hextech-images/chest_mystery_champion_shard.png");
        hashAndAddToSB(data2, pre + "assets/loot/hextech-images/chest_promotion.png");
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
        List<Vector2<String, String>> foundHashes = new ArrayList<>();
        
        if (!Files.exists(tmp_hashes))
        {
            return;
        }
        
        System.out.println("Combining hashes");
        
        Files.walkFileTree(tmp_hashes, new SimpleFileVisitor<>()
        {
            
            @Override
            @SuppressWarnings(value = "unchecked")
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                ((Map<String, String>) UtilHandler.getGson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>()
                {
                }.getType())).forEach((k, v) -> {
                    if (!HashHandler.getWadHashes("rcp-be-lol-game-data").containsKey(k))
                    {
                        Vector2<String, String> data = new Vector2<>(k, v);
                        if (!foundHashes.contains(data))
                        {
                            foundHashes.add(new Vector2<>(k, v));
                        }
                    }
                });
                return FileVisitResult.CONTINUE;
            }
        });
        
        try
        {
            foundHashes.sort(Comparator.comparing(Vector2::getY, new NaturalOrderComparator()));
            
            StringBuilder sb = new StringBuilder("{\n");
            for (Vector2<String, String> pair : foundHashes)
            {
                sb.append("\t\"").append(pair.getX()).append("\": \"").append(pair.getY()).append("\",\n");
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
        
        
        Files.walkFileTree(tmp_hashes, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                file.toFile().delete();
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            {
                dir.toFile().delete();
                return FileVisitResult.CONTINUE;
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
            WADFile parsed = parser.parseLatest(pluginName, extractPath, false);
            parsed.extractFiles(pluginName, null, extractPath);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        
        Path possibleTech = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-fe-lol-loot\\plugins\\rcp-fe-lol-loot\\global\\default\\trans.json");
        Map<String, String> data = UtilHandler.getGson().fromJson(UtilHandler.readAsString(possibleTech), new TypeToken<Map<String, String>>()
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
        List<Vector2<String, String>> foundHashes = new ArrayList<>();
        
        Files.walkFileTree(currentInnerFolder, new SimpleFileVisitor<>()
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
                ((Map<String, String>) UtilHandler.getGson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>()
                {
                }.getType())).forEach((k, v) -> {
                    Vector2<String, String> data = new Vector2<>(k, v);
                    if (!foundHashes.contains(data))
                    {
                        foundHashes.add(new Vector2<>(k, v));
                    }
                });
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }
        });
        
        try
        {
            foundHashes.sort(Comparator.comparing(Vector2::getY, new NaturalOrderComparator()));
            
            StringBuilder sb = new StringBuilder("{\n");
            for (Vector2<String, String> pair : foundHashes)
            {
                sb.append("\t\"").append(pair.getX()).append("\": \"").append(pair.getY()).append("\",\n");
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
        
        if (wip.isEmpty())
        {
            return;
        }
        
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
                
                String id = ob.get("id").getAsString();
                id = id.substring(3);
                int    skinid = Integer.parseInt(id);
                String alias  = elem.get("alias").getAsString();
                
                String loadScreen = "assets/characters/" + alias + "/skins/";
                if (skinid > 0)
                {
                    loadScreen += "skin" + id + "/";
                    hashAndAddToSB(data, loadScreen + alias + "loadscreen_" + skinid + ".dds");
                    String basePathL = loadScreen + alias + "_" + "skin" + id;
                    hashAndAddToSB(data, basePathL + ".skl");
                    hashAndAddToSB(data, basePathL + ".skn");
                    hashAndAddToSB(data, basePathL + "tx_cm.dds");
                }
                
                String minimapCircle = "assets/characters/" + alias + "/hud/" + alias + "_circle";
                if (skinid > 0)
                {
                    minimapCircle += "_" + skinid;
                }
                minimapCircle += ".dds";
                
                hashAndAddToSB(data, loadScreen);
                hashAndAddToSB(data, minimapCircle);
                
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
        String[] cases = new String[]{
                hashMe.trim(), hashMe.toLowerCase(Locale.ENGLISH).trim()
        };
        
        for (String hVal : cases)
        {
            String hash      = HashHandler.computeXXHash64(hVal);
            String knownHash = HashHandler.getWadHashes("rcp-be-lol-game-data").get(hash);
            
            if (knownHash != null)
            {
                continue;
            }
            
            if (hashes.contains(hash))
            {
                if (sb.toString().contains(hash))
                {
                    continue;
                }
                
                sb.append("\t\"").append(hash).append("\": \"").append(hVal).append("\",\n");
                hashes.remove(hash);
            }
        }
    }
    
    
    private List<String> getUnknownHashes()
    {
        List<String> unknowns = new ArrayList<>();
        try
        {
            Files.walkFileTree(UtilHandler.DOWNLOADS_FOLDER, new SimpleFileVisitor<>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    if (file.getFileName().toString().equals("unknown.json"))
                    {
                        unknowns.addAll(Files.readAllLines(file));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return unknowns;
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
    
    private Map<String, String> loadAllHashes() throws IOException
    {
        final Map<String, String> knownHashes = new HashMap<>();
        
        Files.walkFileTree(HashHandler.WAD_HASH_STORE, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                String filename = file.getFileName().toString().substring(0, file.getFileName().toString().lastIndexOf('.'));
                
                ((Map<String, String>) UtilHandler.getGson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>() {}.getType()))
                        .forEach(knownHashes::putIfAbsent);
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        return knownHashes;
    }
    
    public void testUnsplit() throws IOException
    {
        Path loadPath = Paths.get("combined.json");
        
        if (!Files.exists(loadPath))
        {
            return;
        }
        
        List<String> lines = Files.readAllLines(loadPath).stream().filter(x -> !x.equalsIgnoreCase("{") && !x.equalsIgnoreCase("}")).collect(Collectors.toList());
        
        Set<String> changedPlugins = new HashSet<>();
        for (String u : lines)
        {
            String[] parts  = u.split("\": \"");
            String   first  = parts[0].replaceAll("[\"]", "").trim();
            String   second = parts[1].replaceAll("[\",]", "").trim();
            
            String       pluginPre = second.substring("plugins/".length());
            final String plugin    = pluginPre.substring(0, pluginPre.indexOf('/'));
            
            
            Map<String, String> hashes = HashHandler.getWadHashes(plugin);
            hashes.computeIfAbsent(first, (key) -> {
                changedPlugins.add(plugin);
                return second;
            });
        }
        
        
        for (String plugin : changedPlugins)
        {
            System.out.println("Found new hashes for: " + plugin);
            List<Vector2<String, String>> foundHashes = new ArrayList<>();
            
            HashHandler.getWadHashes(plugin).forEach((k, v) -> {
                Vector2<String, String> data = new Vector2<>(k, v);
                if (!foundHashes.contains(data))
                {
                    foundHashes.add(data);
                }
            });
            
            foundHashes.sort(Comparator.comparing(Vector2::getY, new NaturalOrderComparator()));
            
            StringBuilder sb = new StringBuilder("{\n");
            for (Vector2<String, String> pair : foundHashes)
            {
                sb.append("\t\"").append(pair.getX()).append("\": \"").append(pair.getY()).append("\",\n");
            }
            sb.reverse().delete(0, 2).reverse().append("\n}");
            Files.write(HashHandler.WAD_HASH_STORE.resolve(plugin + ".json"), sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        
        Files.deleteIfExists(loadPath);
    }
    
    public void getHashes() throws IOException
    {
        runDirectory(UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data"));
        combineAndDeleteNestedTemp();
        testUnsplit();
    }
    
    private final List<String> img_exts   = Arrays.asList("json", "txt", "png", "jpg", "jpeg", "webm", "ogg", "dds");
    private final String       img_pre    = "plugins/rcp-be-lol-game-data/global/default/";
    private final Path         img_folder = Paths.get("tmp_gzip");
    
    
    final int img_skinMax     = 50;
    final int img_championMax = 700;
    final int img_iconMax     = 2000;
    
    private final Map<String, Integer> img_folderData = new HashMap<>()
    {{
        put("champion-sfx-audios", img_championMax);
        put("champion-icons", img_championMax);
        put("champion-choose-vo", img_championMax);
        put("champion-ban-vo", img_championMax);
        put("summoner-backdrops", img_iconMax);
    }};
    
    private final Map<String, Integer[]> img_folderData2 = new HashMap<>()
    {{
        put("champion-tiles", new Integer[]{img_championMax, img_skinMax});
    }};
    
    public void extractImages() throws IOException
    {
        Path file  = Paths.get(System.getProperty("user.home"), "Downloads/rcp-be-lol-game-data/plugins/rcp-be-lol-game-data/global/default/v1/");
        Path file2 = Paths.get(System.getProperty("user.home"), "Downloads/rcp-be-lol-game-data/plugins/rcp-be-lol-game-data/global/default/v1/champions");
        
        if (!Files.exists(img_folder))
        {
            Files.createDirectories(img_folder);
        }
        
        
        System.out.println("Parsing icon files");
        img_findIconPathInJsonArrayFile(file, "perkstyles.json");
        img_findIconPathInJsonArrayFile(file, "perks.json");
        img_findIconPathInJsonArrayFile(file, "items.json");
        img_findIconPathInJsonArrayFile(file, "summoner-spells.json");
        img_findIconPathInJsonArrayFile(file, "profile-icons.json");
        
        img_parseWardSkins(file, "ward-skins.json");
        img_parseMasteries(file, "summoner-masteries.json");
        img_parseEmotes(file, "summoner-emotes.json");
        img_parseBanners(file, "summoner-banners.json");
        img_parseMapAssets(file, "map-assets/map-assets.json");
        
        System.out.println("Parsing champion files");
        for (int i = -1; i < img_championMax; i++)
        {
            img_findInChampionFile(file2, i + ".json");
        }
        
        System.out.println("Parsing data from unknown files");
        for (String ext : img_exts)
        {
            img_folderData.forEach((k, v) -> img_generateHashList(k, v, ext));
            img_folderData2.forEach((k, v) -> img_generateHashListNested(k, v, ext));
        }
        
        img_combineAndDeleteTemp();
        
        System.out.println("Copying files");
        img_copyFilesToFolders();
        
        System.out.println("Creating zip");
        img_createTARGZ();
    }
    
    private void img_createTARGZ() throws IOException
    {
        Path base         = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\pretty");
        Path outputFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\pretty\\zipped-folders");
        Files.createDirectories(outputFolder);
        
        Files.walkFileTree(base, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                if (dir.equals(UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\pretty")))
                {
                    return FileVisitResult.CONTINUE;
                }
                if (dir.equals(UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\pretty\\zipped-folders")))
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                
                img_compressFiles(Files.list(dir).collect(Collectors.toList()), outputFolder.resolve(dir.getFileName() + ".tar.gz"));
                
                return FileVisitResult.SKIP_SUBTREE;
            }
        });
    }
    
    private void img_compressFiles(List<Path> files, Path output)
    {
        try (FileOutputStream fos = new FileOutputStream(output.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             GZIPOutputStream gos = new GZIPOutputStream(bos);
             TarArchiveOutputStream tos = new TarArchiveOutputStream(gos))
        {
            tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            
            for (Path file : files)
            {
                img_addToTar(tos, file, ".");
            }
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void img_addToTar(TarArchiveOutputStream tos, Path file, String dir) throws IOException
    {
        tos.putArchiveEntry(new TarArchiveEntry(file.toFile(), dir + "/" + file.getFileName().toString()));
        if (Files.isDirectory(file))
        {
            tos.closeArchiveEntry();
            for (Path child : Files.list(file).collect(Collectors.toList()))
            {
                img_addToTar(tos, child, dir + "/" + file.getFileName().toString());
            }
        } else
        {
            try (FileInputStream fis = new FileInputStream(file.toFile());
                 BufferedInputStream bis = new BufferedInputStream(fis))
            {
                IOUtils.copy(bis, tos);
                tos.closeArchiveEntry();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    private void img_copyFilesToFolders()
    {
        Path                inputFile = Paths.get("filenames.json");
        Map<String, String> files     = UtilHandler.getGson().fromJson(UtilHandler.readAsString(inputFile), new TypeToken<Map<String, String>>() {}.getType());
        Path                baseTo    = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\pretty");
        
        try
        {
            Files.createDirectories(baseTo);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        files.forEach((k, v) -> {
            Path from = Paths.get(v);
            Path to   = baseTo.resolve(k);
            
            
            try
            {
                Files.createDirectories(to.getParent());
                
                if (Files.exists(to))
                {
                    if (Files.size(from) != Files.size(to))
                    {
                        Files.delete(to);
                    }
                }
                
                Files.copy(from, to);
            } catch (FileAlreadyExistsException | NoSuchFileException ex)
            {
                // we dont care if the file is missing/already there
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
        
    }
    
    private List<String> img_parseHextechFile()
    {
        
        // check the lol-loot plugin for more... (4c0ce4a49dbc214c)
        WADParser parser      = new WADParser();
        String    pluginName  = "rcp-fe-lol-loot";
        Path      extractPath = Paths.get(System.getProperty("user.home"), "Downloads");
        
        try
        {
            WADFile parsed = parser.parseLatest(pluginName, extractPath, false);
            parsed.extractFiles(pluginName, null, extractPath);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        
        Path                possibleTech = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-fe-lol-loot\\unknown\\4c0ce4a49dbc214c.json");
        Map<String, String> data         = UtilHandler.getGson().fromJson(UtilHandler.readAsString(possibleTech), new TypeToken<Map<String, String>>() {}.getType());
        return img_transmute(data.keySet());
    }
    
    private List<String> img_transmute(Set<String> strings)
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
    
    private void img_extractData(StringBuilder data, JsonObject extMe, String path, String outFormat)
    {
        String[] elemen = img_getElement(extMe, path);
        if (elemen == null)
        {
            return;
        }
        
        String preHash  = elemen[0];
        String postHash = String.format(outFormat, path, elemen[1]);
        img_addToSB(data, preHash, postHash);
    }
    
    private void img_parseMapAssets(Path filepath, String filename)
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
                JsonObject par    = element.getAsJsonObject();
                JsonObject assets = par.getAsJsonObject("assets");
                String     mode   = par.get("gameMode").getAsString();
                
                if (!par.get("gameMutator").getAsString().isEmpty())
                {
                    mode = mode + "/" + par.get("gameMutator").getAsString();
                }
                
                String outFormat = "map-assets/" + key + "/" + mode + "/%s%s";
                
                img_extractData(data, assets, "champ-select-flyout-background", outFormat);
                img_extractData(data, assets, "champ-select-planning-intro", outFormat);
                img_extractData(data, assets, "game-select-icon-default", outFormat);
                img_extractData(data, assets, "game-select-icon-disabled", outFormat);
                img_extractData(data, assets, "game-select-icon-hover", outFormat);
                img_extractData(data, assets, "icon-defeat", outFormat);
                img_extractData(data, assets, "icon-empty", outFormat);
                img_extractData(data, assets, "icon-hover", outFormat);
                img_extractData(data, assets, "icon-leaver", outFormat);
                img_extractData(data, assets, "icon-victory", outFormat);
                img_extractData(data, assets, "parties-background", outFormat);
                img_extractData(data, assets, "social-icon-leaver", outFormat);
                img_extractData(data, assets, "social-icon-victory", outFormat);
                img_extractData(data, assets, "game-select-icon-active", outFormat);
                img_extractData(data, assets, "ready-check-background", outFormat);
                img_extractData(data, assets, "map-north", outFormat);
                img_extractData(data, assets, "map-south", outFormat);
                img_extractData(data, assets, "gameflow-background", outFormat);
                img_extractData(data, assets, "notification-background", outFormat);
                img_extractData(data, assets, "notification-icon", outFormat);
                img_extractData(data, assets, "champ-select-background-sound", outFormat);
                img_extractData(data, assets, "gameselect-button-hover-sound", outFormat);
                img_extractData(data, assets, "music-inqueue-loop-sound", outFormat);
                img_extractData(data, assets, "postgame-ambience-loop-sound", outFormat);
                img_extractData(data, assets, "sfx-ambience-pregame-loop-sound", outFormat);
                img_extractData(data, assets, "ready-check-background-sound", outFormat);
                img_extractData(data, assets, "game-select-icon-active-video", outFormat);
                img_extractData(data, assets, "game-select-icon-intro-video", outFormat);
                img_extractData(data, assets, "icon-defeat-video", outFormat);
                img_extractData(data, assets, "icon-victory-video", outFormat);
            }
        }
        
        img_finalizeFileReading("map-assets.json", data);
    }
    
    private void img_parseBanners(Path filepath, String filename)
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
            JsonObject el    = element.getAsJsonObject();
            String     level = el.get("level").getAsString();
            String     theme = el.get("theme").getAsString();
            
            String preHash  = img_getElement(el, "inventoryIcon")[0];
            String postHash = "banners/inventory/" + theme + "-" + level + ".png";
            img_addToSB(data, preHash, postHash);
            
            String preHash2  = img_getElement(el, "profileIcon")[0];
            String postHash2 = "banners/profile/" + theme + "-" + level + ".png";
            img_addToSB(data, preHash2, postHash2);
        }
        
        JsonArray bframes = elem.getAsJsonArray("BannerFrames");
        for (JsonElement element : bframes)
        {
            JsonObject el   = element.getAsJsonObject();
            String     name = el.get("level").getAsString();
            
            String preHash  = img_getElement(el, "inventoryIcon")[0];
            String postHash = "banners/frames/inventory/" + name + ".png";
            img_addToSB(data, preHash, postHash);
            
            if (el.has("profileIcon"))
            {
                String preHash2  = img_getElement(el, "profileIcon")[0];
                String postHash2 = "banners/frames/profile/" + name + ".png";
                img_addToSB(data, preHash2, postHash2);
            }
        }
        img_finalizeFileReading(filename, data);
    }
    
    private void img_finalizeFileReading(String filename, StringBuilder data)
    {
        try
        {
            data.reverse().delete(0, 2).reverse().append("\n}");
            if (data.toString().length() < 10)
            {
                return;
            }
            
            if (!Files.exists(img_folder))
            {
                Files.createDirectories(img_folder);
            }
            
            Files.write(img_folder.resolve(filename), data.toString().getBytes(StandardCharsets.UTF_8));
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void img_parseEmotes(Path filepath, String filename)
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
            int        id = el.get("id").getAsInt();
            
            if (id < 10)
            {
                continue;
            }
            
            String preHash  = img_getElement(el, "inventoryIcon")[0];
            String postHash = "emotes/" + id + ".png";
            img_addToSB(data, preHash, postHash);
        }
        
        img_finalizeFileReading(filename, data);
    }
    
    private void img_parseMasteries(Path filepath, String filename)
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
            img_getElement(el, "iconPath");
        }
        img_finalizeFileReading(filename, data);
    }
    
    private String[] img_getElement(JsonObject el, String path)
    {
        if (!el.has(path))
        {
            return null;
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
            System.err.println(img_pre + wip);
        }
        
        if (wip.lastIndexOf('.') < 0)
        {
            return null;
        }
        
        return new String[]{img_pre + wip, wip.substring(wip.lastIndexOf('.'))};
    }
    
    private void img_parseWardSkins(Path filepath, String filename)
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
            int        id = el.get("id").getAsInt();
            
            String preHash  = img_getElement(el, "wardImagePath")[0];
            String postHash = "wards/" + id + ".png";
            img_addToSB(data, preHash, postHash);
            
            String preHash2  = img_getElement(el, "wardShadowImagePath")[0];
            String postHash2 = "wards/shadow/" + id + ".png";
            img_addToSB(data, preHash2, postHash2);
            
        }
        img_finalizeFileReading(filename, data);
    }
    
    private void img_findInChampionFile(Path filepath, String filename)
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonObject    elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonObject();
        StringBuilder data = new StringBuilder("{\n");
        int           id   = elem.get("id").getAsInt();
        
        String passive = elem.getAsJsonObject("passive").get("abilityIconPath").getAsString().toLowerCase(Locale.ENGLISH);
        
        
        if (!passive.isEmpty())
        {
            String[] elemen = img_getElement(elem.getAsJsonObject("passive"), "abilityIconPath");
            if (elemen == null)
            {
                return;
            }
            
            String preHash  = elemen[0];
            String postHash = "abilities/" + id + "/passive.png";
            img_addToSB(data, preHash, postHash);
        }
        
        JsonArray arr = elem.getAsJsonArray("spells");
        
        for (JsonElement element : arr)
        {
            JsonObject current = element.getAsJsonObject();
            String     key     = current.get("spellKey").getAsString();
            String[]   elemen  = img_getElement(current, "abilityIconPath");
            if (elemen == null)
            {
                return;
            }
            
            String preHash  = elemen[0];
            String postHash = "abilities/" + id + "/" + key + ".png ";
            img_addToSB(data, preHash, postHash);
        }
        
        
        arr = elem.getAsJsonArray("skins");
        
        for (JsonElement element : arr)
        {
            JsonObject ob   = element.getAsJsonObject();
            int        skin = Integer.parseInt(ob.get("id").getAsString().substring(String.valueOf(id).length()));
            
            String[] elemen = img_getElement(ob, "splashPath");
            if (elemen == null)
            {
                return;
            }
            
            String preHash  = elemen[0];
            String postHash = "splash-art/" + id + "/" + skin + ".png ";
            img_addToSB(data, preHash, postHash);
            
            elemen = img_getElement(ob, "uncenteredSplashPath");
            if (elemen == null)
            {
                return;
            }
            
            preHash = elemen[0];
            postHash = "uncentered-splash-art/" + id + "/" + skin + ".png ";
            img_addToSB(data, preHash, postHash);
            
            elemen = img_getElement(ob, "tilePath");
            if (elemen == null)
            {
                return;
            }
            
            preHash = elemen[0];
            postHash = "tile/" + id + "/" + skin + ".png ";
            img_addToSB(data, preHash, postHash);
            
            elemen = img_getElement(ob, "loadScreenPath");
            if (elemen == null)
            {
                return;
            }
            
            preHash = elemen[0];
            postHash = "loading-screen/" + id + "/" + skin + ".png ";
            img_addToSB(data, preHash, postHash);
            
            
            if (ob.has("chromas"))
            {
                elemen = img_getElement(ob, "chromaPath");
                if (elemen == null)
                {
                    return;
                }
                
                preHash = elemen[0];
                postHash = "chroma/" + id + "/" + skin + ".png ";
                img_addToSB(data, preHash, postHash);
                
                JsonArray chrom = ob.getAsJsonArray("chromas");
                for (JsonElement ch : chrom)
                {
                    skin = Integer.parseInt(ch.getAsJsonObject().get("id").getAsString().substring(String.valueOf(id).length()));
                    
                    elemen = img_getElement(ch.getAsJsonObject(), "chromaPath");
                    if (elemen == null)
                    {
                        return;
                    }
                    
                    preHash = elemen[0];
                    postHash = "chroma/" + id + "/" + skin + ".png ";
                    img_addToSB(data, preHash, postHash);
                }
            }
        }
        
        img_finalizeFileReading(filename, data);
    }
    
    private void img_findIconPathInJsonArrayFile(Path filepath, String filename)
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
            JsonObject ob     = element.getAsJsonObject();
            String[]   elemen = img_getElement(ob, "iconPath");
            if (elemen == null)
            {
                return;
            }
            
            String preHash  = elemen[0];
            String postHash = filename.substring(0, filename.lastIndexOf(".json")) + "/" + ob.get("id") + elemen[1];
            img_addToSB(data, preHash, postHash);
        }
        
        img_finalizeFileReading(filename, data);
    }
    
    private void img_generateHashList(String folderName, Integer depths, String fileType)
    {
        String pathPrefix = img_pre + "v1/" + folderName + "/";
        
        StringBuilder sb = new StringBuilder("{\n");
        for (int i = -1; i < depths; i++)
        {
            String value  = String.format(pathPrefix + "%s." + fileType, i);
            String pretty = String.format(folderName + "/%s." + fileType, i);
            img_addToSB(sb, value, pretty);
        }
        img_finalizeFileReading(folderName + "." + fileType + ".json", sb);
    }
    
    private void img_generateHashListNested(String folderName, Integer[] depths, String fileType)
    {
        String        pathPrefix = img_pre + "v1/" + folderName + "/";
        StringBuilder sb         = new StringBuilder("{\n");
        String        format     = pathPrefix + "%1$s/%2$s%3$03d." + fileType;
        
        for (int i = -1; i < depths[0]; i++)
        {
            for (int j = -1; j < depths[1]; j++)
            {
                String value;
                if (j > 0)
                {
                    value = String.format(format, i, i, j);
                    String pretty = String.format(folderName + "/%1$s/%2$d." + fileType, i, j);
                    img_addToSB(sb, value, pretty);
                }
                
            }
        }
        
        img_finalizeFileReading(folderName + "." + fileType + ".json", sb);
    }
    
    private void img_addToSB(StringBuilder sb, String hashMe, String realPath)
    {
        Path path = Paths.get(System.getProperty("user.home"), "Downloads/rcp-be-lol-game-data/").resolve(hashMe.trim());
        if (Files.exists(path))
        {
            sb.append("\t\"").append(realPath.trim()).append("\": \"").append(path.toString().replace("\\", "/").trim()).append("\",\n");
        }
    }
    
    
    private void img_writeFile(Path file, String data)
    {
        try
        {
            Files.write(file, data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void img_combineAndDeleteTemp() throws IOException
    {
        List<Vector2<String, String>> foundHashes = new ArrayList<>();
        
        Files.walkFileTree(img_folder, new SimpleFileVisitor<>()
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
                ((Map<String, String>) UtilHandler.getGson().fromJson(UtilHandler.readAsString(file), new TypeToken<Map<String, String>>() {}.getType())).forEach((k, v) -> {
                    Vector2<String, String> data = new Vector2<>(k, v);
                    if (!foundHashes.contains(data))
                    {
                        foundHashes.add(new Vector2<>(k, v));
                    }
                });
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }
        });
        
        try
        {
            foundHashes.sort(Comparator.comparing(Vector2::getY, new NaturalOrderComparator()));
            
            StringBuilder sb = new StringBuilder("{\n");
            for (Vector2<String, String> pair : foundHashes)
            {
                sb.append("\t\"").append(pair.getX()).append("\": \"").append(pair.getY()).append("\",\n");
            }
            sb.reverse().delete(0, 2).reverse().append("\n}");
            
            Path filenames = Paths.get("filenames.json");
            Files.write(filenames, sb.toString().getBytes(StandardCharsets.UTF_8));
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void uploadToFTP() throws IOException
    {
        List<String> prefix = new ArrayList<>(Arrays.asList("www", "cdragon", "latest"));
        
        FTPClient client = new FTPClient();
        client.connect("ftp.domeneshop.no");
        client.login(SecretFile.USERNAME, SecretFile.PASSWORD);
        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.changeWorkingDirectory(prefix.stream().reduce("", (o, n) -> o + "/" + n));
        
        Path files = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\pretty\\");
        Files.walkFileTree(files, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                if (UtilHandler.pathToFolderName(dir).equalsIgnoreCase("pretty"))
                {
                    return FileVisitResult.CONTINUE;
                }
                
                String folder = UtilHandler.pathToFolderName(dir);
                
                prefix.add(folder);
                client.makeDirectory(folder);
                client.changeWorkingDirectory(prefix.stream().reduce("", (o, n) -> o + "/" + n));
                
                System.out.println("Uploading to " + client.printWorkingDirectory());
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                prefix.remove(UtilHandler.pathToFolderName(dir));
                client.changeWorkingDirectory(prefix.stream().reduce("", (o, n) -> o + "/" + n));
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                try (FileInputStream is = new FileInputStream(file.toFile()))
                {
                    client.storeFile(file.getFileName().toString(), is);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        
        client.logout();
        client.disconnect();
        
    }
}

package types.util;

import com.google.common.collect.Sets;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.types.Vector2;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class TestStoreAsRealName
{
    
    private final List<String> exts   = Arrays.asList("json", "txt", "png", "jpg", "jpeg", "webm", "ogg", "dds");
    private final String       pre    = "plugins/rcp-be-lol-game-data/global/default/";
    private final Path         folder = Paths.get("tmp_gzip");
    
    
    final int skinMax     = 50;
    final int championMax = 700;
    final int iconMax     = 2000;
    
    private final Map<String, Integer> folderData = new HashMap<>()
    {{
        put("champion-sfx-audios", championMax);
        put("champion-icons", championMax);
        put("champion-choose-vo", championMax);
        put("champion-ban-vo", championMax);
        put("summoner-backdrops", iconMax);
    }};
    
    private final Map<String, Integer[]> folderData2 = new HashMap<>()
    {{
        put("champion-tiles", new Integer[]{championMax, skinMax});
    }};
    
    @Test
    public void testAllImages() throws IOException
    {
        Path file  = Paths.get(System.getProperty("user.home"), "Downloads/rcp-be-lol-game-data/plugins/rcp-be-lol-game-data/global/default/v1/");
        Path file2 = Paths.get(System.getProperty("user.home"), "Downloads/rcp-be-lol-game-data/plugins/rcp-be-lol-game-data/global/default/v1/champions");
        
        if (!Files.exists(folder))
        {
            Files.createDirectories(folder);
        }
        
        
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
            folderData.forEach((k, v) -> generateHashList(k, v, ext));
            folderData2.forEach((k, v) -> generateHashListNested(k, v, ext));
        }
        
        combineAndDeleteTemp();
        System.out.println("Copying files");
        copyFilesToFolders();
        System.out.println("Creating zip");
        createTARGZ();
    }
    
    private void createTARGZ() throws IOException
    {
        Path base         = Paths.get(System.getProperty("user.home"), "Downloads\\rcp-be-lol-game-data\\pretty");
        Path outputFolder = Paths.get(System.getProperty("user.home"), "Downloads\\rcp-be-lol-game-data\\pretty\\zipped-folders");
        Files.createDirectories(outputFolder);
        
        Files.walkFileTree(base, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                if (dir.equals(Paths.get(System.getProperty("user.home"), "Downloads", "rcp-be-lol-game-data", "pretty")))
                {
                    return FileVisitResult.CONTINUE;
                }
                if (dir.equals(Paths.get(System.getProperty("user.home"), "Downloads", "rcp-be-lol-game-data", "pretty", "zipped-folders")))
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                
                compressFiles(Files.list(dir).collect(Collectors.toList()), outputFolder.resolve(dir.getFileName() + ".tar.gz"));
                
                return FileVisitResult.SKIP_SUBTREE;
            }
        });
    }
    
    private void compressFiles(List<Path> files, Path output)
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
                addToTar(tos, file, ".");
            }
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void addToTar(TarArchiveOutputStream tos, Path file, String dir) throws IOException
    {
        tos.putArchiveEntry(new TarArchiveEntry(file.toFile(), dir + "/" + file.getFileName().toString()));
        if (Files.isDirectory(file))
        {
            tos.closeArchiveEntry();
            for (Path child : Files.list(file).collect(Collectors.toList()))
            {
                addToTar(tos, child, dir + "/" + file.getFileName().toString());
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
    
    private void copyFilesToFolders()
    {
        Path                inputFile = Paths.get("filenames.json");
        Map<String, String> files     = UtilHandler.getGson().fromJson(UtilHandler.readAsString(inputFile), new TypeToken<Map<String, String>>() {}.getType());
        Path                baseTo    = Paths.get(System.getProperty("user.home"), "Downloads\\rcp-be-lol-game-data\\pretty");
        
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
        Map<String, String> data         = UtilHandler.getGson().fromJson(UtilHandler.readAsString(possibleTech), new TypeToken<Map<String, String>>() {}.getType());
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
    
    private void extractData(StringBuilder data, JsonObject extMe, String path, String outFormat)
    {
        String[] elemen = getElement(extMe, path);
        if (elemen == null)
        {
            return;
        }
        
        String preHash  = elemen[0];
        String postHash = String.format(outFormat, path, elemen[1]);
        addToSB(data, preHash, postHash);
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
                JsonObject par    = element.getAsJsonObject();
                JsonObject assets = par.getAsJsonObject("assets");
                String     mode   = par.get("gameMode").getAsString();
                
                if (!par.get("gameMutator").getAsString().isEmpty())
                {
                    mode = mode + "/" + par.get("gameMutator").getAsString();
                }
                
                String outFormat = "map-assets/" + key + "/" + mode + "/%s%s";
                
                extractData(data, assets, "champ-select-flyout-background", outFormat);
                extractData(data, assets, "champ-select-planning-intro", outFormat);
                extractData(data, assets, "game-select-icon-default", outFormat);
                extractData(data, assets, "game-select-icon-disabled", outFormat);
                extractData(data, assets, "game-select-icon-hover", outFormat);
                extractData(data, assets, "icon-defeat", outFormat);
                extractData(data, assets, "icon-empty", outFormat);
                extractData(data, assets, "icon-hover", outFormat);
                extractData(data, assets, "icon-leaver", outFormat);
                extractData(data, assets, "icon-victory", outFormat);
                extractData(data, assets, "parties-background", outFormat);
                extractData(data, assets, "social-icon-leaver", outFormat);
                extractData(data, assets, "social-icon-victory", outFormat);
                extractData(data, assets, "game-select-icon-active", outFormat);
                extractData(data, assets, "ready-check-background", outFormat);
                extractData(data, assets, "map-north", outFormat);
                extractData(data, assets, "map-south", outFormat);
                extractData(data, assets, "gameflow-background", outFormat);
                extractData(data, assets, "notification-background", outFormat);
                extractData(data, assets, "notification-icon", outFormat);
                extractData(data, assets, "champ-select-background-sound", outFormat);
                extractData(data, assets, "gameselect-button-hover-sound", outFormat);
                extractData(data, assets, "music-inqueue-loop-sound", outFormat);
                extractData(data, assets, "postgame-ambience-loop-sound", outFormat);
                extractData(data, assets, "sfx-ambience-pregame-loop-sound", outFormat);
                extractData(data, assets, "ready-check-background-sound", outFormat);
                extractData(data, assets, "game-select-icon-active-video", outFormat);
                extractData(data, assets, "game-select-icon-intro-video", outFormat);
                extractData(data, assets, "icon-defeat-video", outFormat);
                extractData(data, assets, "icon-victory-video", outFormat);
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
            int        id = el.get("level").getAsInt();
            
            String preHash  = getElement(el, "inventoryIcon")[0];
            String postHash = "banners/inventory/" + id + ".png";
            addToSB(data, preHash, postHash);
            
            String preHash2  = getElement(el, "profileIcon")[0];
            String postHash2 = "banners/profile/" + id + ".png";
            addToSB(data, preHash2, postHash2);
        }
        
        JsonArray bframes = elem.getAsJsonArray("BannerFrames");
        for (JsonElement element : bframes)
        {
            JsonObject el = element.getAsJsonObject();
            int        id = el.get("level").getAsInt();
            
            String preHash  = getElement(el, "inventoryIcon")[0];
            String postHash = "banners/frames/inventory/" + id + ".png";
            addToSB(data, preHash, postHash);
            
            if (el.has("profileIcon"))
            {
                String preHash2  = getElement(el, "profileIcon")[0];
                String postHash2 = "banners/frames/profile/" + id + ".png";
                addToSB(data, preHash2, postHash2);
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
            
            if (!Files.exists(folder))
            {
                Files.createDirectories(folder);
            }
            
            Files.write(folder.resolve(filename), data.toString().getBytes(StandardCharsets.UTF_8));
            
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
            int        id = el.get("id").getAsInt();
            
            if (id < 10)
            {
                continue;
            }
            
            String preHash  = getElement(el, "inventoryIcon")[0];
            String postHash = "emotes/" + id + ".png";
            addToSB(data, preHash, postHash);
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
            getElement(el, "iconPath");
        }
        finalizeFileReading(filename, data);
    }
    
    private String[] getElement(JsonObject el, String path)
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
            System.err.println(pre + wip);
        }
        
        if (wip.lastIndexOf('.') < 0)
        {
            return null;
        }
        
        return new String[]{pre + wip, wip.substring(wip.lastIndexOf('.'))};
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
            int        id = el.get("id").getAsInt();
            
            String preHash  = getElement(el, "wardImagePath")[0];
            String postHash = "wards/" + id + ".png";
            addToSB(data, preHash, postHash);
            
            String preHash2  = getElement(el, "wardShadowImagePath")[0];
            String postHash2 = "wards/shadow/" + id + ".png";
            addToSB(data, preHash2, postHash2);
            
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
        int           id   = elem.get("id").getAsInt();
        
        String passive = elem.getAsJsonObject("passive").get("abilityIconPath").getAsString().toLowerCase(Locale.ENGLISH);
        
        
        if (!passive.isEmpty())
        {
            String[] elemen = getElement(elem.getAsJsonObject("passive"), "abilityIconPath");
            if (elemen == null)
            {
                return;
            }
            
            String preHash  = elemen[0];
            String postHash = "abilities/" + id + "/passive.png";
            addToSB(data, preHash, postHash);
        }
        
        JsonArray arr = elem.getAsJsonArray("spells");
        
        for (JsonElement element : arr)
        {
            JsonObject current = element.getAsJsonObject();
            String     key     = current.get("spellKey").getAsString();
            String[]   elemen  = getElement(current, "abilityIconPath");
            if (elemen == null)
            {
                return;
            }
            
            String preHash  = elemen[0];
            String postHash = "abilities/" + id + "/" + key + ".png ";
            addToSB(data, preHash, postHash);
        }
        
        
        arr = elem.getAsJsonArray("skins");
        
        for (JsonElement element : arr)
        {
            JsonObject ob   = element.getAsJsonObject();
            int        skin = Integer.parseInt(ob.get("id").getAsString().substring(String.valueOf(id).length()));
            
            String[] elemen = getElement(ob, "splashPath");
            if (elemen == null)
            {
                return;
            }
            
            String preHash  = elemen[0];
            String postHash = "splash-art/" + id + "/" + skin + ".png ";
            addToSB(data, preHash, postHash);
            
            elemen = getElement(ob, "uncenteredSplashPath");
            if (elemen == null)
            {
                return;
            }
            
            preHash = elemen[0];
            postHash = "uncentered-splash-art/" + id + "/" + skin + ".png ";
            addToSB(data, preHash, postHash);
            
            elemen = getElement(ob, "tilePath");
            if (elemen == null)
            {
                return;
            }
            
            preHash = elemen[0];
            postHash = "tile/" + id + "/" + skin + ".png ";
            addToSB(data, preHash, postHash);
            
            elemen = getElement(ob, "loadScreenPath");
            if (elemen == null)
            {
                return;
            }
            
            preHash = elemen[0];
            postHash = "loading-screen/" + id + "/" + skin + ".png ";
            addToSB(data, preHash, postHash);
            
            
            if (ob.has("chromas"))
            {
                elemen = getElement(ob, "chromaPath");
                if (elemen == null)
                {
                    return;
                }
                
                preHash = elemen[0];
                postHash = "chroma/" + id + "/" + skin + ".png ";
                addToSB(data, preHash, postHash);
                
                JsonArray chrom = ob.getAsJsonArray("chromas");
                for (JsonElement ch : chrom)
                {
                    skin = Integer.parseInt(ch.getAsJsonObject().get("id").getAsString().substring(String.valueOf(id).length()));
                    
                    elemen = getElement(ch.getAsJsonObject(), "chromaPath");
                    if (elemen == null)
                    {
                        return;
                    }
                    
                    preHash = elemen[0];
                    postHash = "chroma/" + id + "/" + skin + ".png ";
                    addToSB(data, preHash, postHash);
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
            JsonObject ob     = element.getAsJsonObject();
            String[]   elemen = getElement(ob, "iconPath");
            if (elemen == null)
            {
                return;
            }
            
            String preHash  = elemen[0];
            String postHash = filename.substring(0, filename.lastIndexOf(".json")) + "/" + ob.get("id") + elemen[1];
            addToSB(data, preHash, postHash);
        }
        
        finalizeFileReading(filename, data);
    }
    
    private void generateHashList(String folderName, Integer depths, String fileType)
    {
        String pathPrefix = pre + "v1/" + folderName + "/";
        
        StringBuilder sb = new StringBuilder("{\n");
        for (int i = -1; i < depths; i++)
        {
            String value  = String.format(pathPrefix + "%s." + fileType, i);
            String pretty = String.format(folderName + "/%s." + fileType, i);
            addToSB(sb, value, pretty);
        }
        finalizeFileReading(folderName + "." + fileType + ".json", sb);
    }
    
    private void generateHashListNested(String folderName, Integer[] depths, String fileType)
    {
        String        pathPrefix = pre + "v1/" + folderName + "/";
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
                    addToSB(sb, value, pretty);
                }
                
            }
        }
        
        finalizeFileReading(folderName + "." + fileType + ".json", sb);
    }
    
    private void addToSB(StringBuilder sb, String hashMe, String realPath)
    {
        Path path = Paths.get(System.getProperty("user.home"), "Downloads/rcp-be-lol-game-data/").resolve(hashMe.trim());
        if (Files.exists(path))
        {
            sb.append("\t\"").append(realPath.trim()).append("\": \"").append(path.toString().replace("\\", "/").trim()).append("\",\n");
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
    
    private void combineAndDeleteTemp() throws IOException
    {
        List<Vector2<String, String>> foundHashes = new ArrayList<>();
        
        Files.walkFileTree(folder, new SimpleFileVisitor<>()
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
}

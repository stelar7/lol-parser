package types.util;

import com.google.common.collect.Sets;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.writers.JsonWriterWrapper;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class TestExtractImages
{
    @Test
    public void testAll() throws IOException
    {
        downloadWAD();
        extractImages();
    }
    
    private void downloadWAD()
    {
        WADParser parser = new WADParser();
        
        String pluginName  = "rcp-be-lol-game-data";
        Path   extractPath = UtilHandler.CDRAGON_FOLDER.resolve("images_out");
        
        WADFile parsed = parser.parseLatest(pluginName, extractPath, true);
        parsed.extractFiles(extractPath, pluginName, false);
    }
    
    private final List<String> img_exts = Arrays.asList("json", "txt", "png", "jpg", "jpeg", "webm", "ogg", "dds");
    private final String       img_pre  = "plugins/rcp-be-lol-game-data/global/default/";
    
    
    final int img_skinMax     = 50;
    final int img_championMax = 1000;
    final int img_iconMax     = 5000;
    
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
        Path file  = UtilHandler.CDRAGON_FOLDER.resolve("images_out").resolve("plugins/rcp-be-lol-game-data/global/default/v1/");
        Path file2 = UtilHandler.CDRAGON_FOLDER.resolve("images_out").resolve("plugins/rcp-be-lol-game-data/global/default/v1/champions");
        
        JsonWriterWrapper data = new JsonWriterWrapper();
        data.beginObject();
        
        System.out.println("Parsing perk styles");
        img_findIconPathInJsonArrayFile(file, "perkstyles.json", data);
        System.out.println("Parsing perks");
        img_findIconPathInJsonArrayFile(file, "perks.json", data);
        System.out.println("Parsing items");
        img_findIconPathInJsonArrayFile(file, "items.json", data);
        System.out.println("Parsing summoners");
        img_findIconPathInJsonArrayFile(file, "summoner-spells.json", data);
        System.out.println("Parsing profile icons");
        img_findIconPathInJsonArrayFile(file, "profile-icons.json", data);
        
        System.out.println("Parsing ward skins");
        img_parseWardSkins(file, "ward-skins.json", data);
        System.out.println("Parsing masteries");
        img_parseMasteries(file, "summoner-masteries.json", data);
        System.out.println("Parsing emotes");
        img_parseEmotes(file, "summoner-emotes.json", data);
        System.out.println("Parsing banner");
        img_parseBanners(file, "summoner-banners.json", data);
        System.out.println("Parsing map assets");
        img_parseMapAssets(file, "map-assets/map-assets.json", data);
        
        for (int i = -1; i < img_championMax; i++)
        {
            System.out.println("Parsing champion id: " + i);
            img_findInChampionFile(file2, i + ".json", data);
        }
        
        System.out.println("Parsing data from unknown files");
        for (String ext : img_exts)
        {
            System.out.println("Parsing extension: " + ext);
            img_folderData.forEach((k, v) -> img_generateHashList(k, v, ext, data));
            img_folderData2.forEach((k, v) -> img_generateHashListNested(k, v, ext, data));
        }
        data.endObject();
        
        Files.write(UtilHandler.CDRAGON_FOLDER.resolve("images_out").resolve("filenames.json"), data.toString().getBytes(StandardCharsets.UTF_8));
        
        System.out.println("Copying files");
        img_copyFilesToFolders();
        
        System.out.println("Creating zip");
        img_createTARGZ();
    }
    
    private void img_createTARGZ() throws IOException
    {
        Path base         = UtilHandler.CDRAGON_FOLDER.resolve("images_out").resolve("pretty");
        Path outputFolder = UtilHandler.CDRAGON_FOLDER.resolve("images_out").resolve("pretty\\zipped-folders");
        Files.createDirectories(outputFolder);
        
        Files.walkFileTree(base, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                if (dir.equals(base))
                {
                    return FileVisitResult.CONTINUE;
                }
                if (dir.equals(outputFolder))
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
        Path                inputFile = UtilHandler.CDRAGON_FOLDER.resolve("images_out").resolve("filenames.json");
        Map<String, String> files     = UtilHandler.getGson().fromJson(UtilHandler.readAsString(inputFile), new TypeToken<Map<String, String>>() {}.getType());
        Path                baseTo    = UtilHandler.CDRAGON_FOLDER.resolve("images_out").resolve("pretty");
        
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
        Path      extractPath = UtilHandler.CDRAGON_FOLDER.resolve("images_out");
        
        try
        {
            WADFile parsed = parser.parseLatest(pluginName, extractPath, false);
            parsed.extractFiles(extractPath, pluginName, false);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        
        Path                possibleTech = UtilHandler.CDRAGON_FOLDER.resolve("images_out").resolve("rcp-fe-lol-loot\\unknown\\4c0ce4a49dbc214c.json");
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
    
    private void img_extractData(JsonWriterWrapper data, JsonObject extMe, String path, String outFormat) throws IOException
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
    
    private void img_parseMapAssets(Path filepath, String filename, JsonWriterWrapper data) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonObject elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonObject();
        
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
    }
    
    // dirty workaround for invalid levels...
    Map<String, Integer> val = new HashMap<>();
    
    private void img_parseBanners(Path filepath, String filename, JsonWriterWrapper data) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonObject elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonObject();
        
        JsonArray bflags = elem.getAsJsonArray("BannerFlags");
        for (JsonElement element : bflags)
        {
            JsonObject el    = element.getAsJsonObject();
            int        level = Integer.parseInt(el.get("level").getAsString());
            String     theme = el.get("theme").getAsString();
            
            if (val.containsKey(theme))
            {
                if (level == 1)
                {
                    level = val.get(theme) + 1;
                    val.put(theme, level);
                }
            }
            
            String preHash  = img_getElement(el, "inventoryIcon")[0];
            String postHash = "banners/inventory/" + theme + "-" + level + ".png";
            img_addToSB(data, preHash, postHash);
            
            String preHash2  = img_getElement(el, "profileIcon")[0];
            String postHash2 = "banners/profile/" + theme + "-" + level + ".png";
            img_addToSB(data, preHash2, postHash2);
            
            // dirty workaround for invalid levels...
            val.putIfAbsent(theme, level);
            
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
    }
    
    private void img_parseEmotes(Path filepath, String filename, JsonWriterWrapper data) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonArray elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonArray();
        
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
    }
    
    private void img_parseMasteries(Path filepath, String filename, JsonWriterWrapper data) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonObject elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonObject().getAsJsonObject("data");
        
        for (String key : elem.keySet())
        {
            JsonObject el       = elem.getAsJsonObject(key);
            String[]   dat      = img_getElement(el, "iconPath");
            String     pre      = dat[0];
            String     postHash = "masteries/" + dat[1] + ".png";
            img_addToSB(data, pre, postHash);
        }
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
    
    private void img_parseWardSkins(Path filepath, String filename, JsonWriterWrapper data) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonArray elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonArray();
        
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
    }
    
    private void img_findInChampionFile(Path filepath, String filename, JsonWriterWrapper data) throws IOException
    {
        Path path = filepath.resolve(filename);
        if (!Files.exists(path))
        {
            return;
        }
        
        JsonObject elem = new JsonParser().parse(UtilHandler.readAsString(path)).getAsJsonObject();
        int        id   = elem.get("id").getAsInt();
        
        String passive = elem.getAsJsonObject("passive").get("abilityIconPath").getAsString().toLowerCase(Locale.ENGLISH);
        
        
        if (!passive.isEmpty())
        {
            String[] elemen = img_getElement(elem.getAsJsonObject("passive"), "abilityIconPath");
            if (elemen != null)
            {
                String preHash  = elemen[0];
                String postHash = "abilities/" + id + "/passive.png";
                img_addToSB(data, preHash, postHash);
            }
        }
        
        JsonArray arr = elem.getAsJsonArray("spells");
        
        for (JsonElement element : arr)
        {
            JsonObject current = element.getAsJsonObject();
            String     key     = current.get("spellKey").getAsString();
            String[]   elemen  = img_getElement(current, "abilityIconPath");
            if (elemen != null)
            {
                String preHash  = elemen[0];
                String postHash = "abilities/" + id + "/" + key + ".png ";
                img_addToSB(data, preHash, postHash);
            }
        }
        
        
        arr = elem.getAsJsonArray("skins");
        
        for (JsonElement element : arr)
        {
            JsonObject ob   = element.getAsJsonObject();
            int        skin = Integer.parseInt(ob.get("id").getAsString().substring(String.valueOf(id).length()));
            String     preHash;
            String     postHash;
            
            
            String[] elemen = img_getElement(ob, "splashPath");
            if (elemen != null)
            {
                preHash = elemen[0];
                postHash = "splash-art/" + id + "/" + skin + ".png ";
                img_addToSB(data, preHash, postHash);
            }
            
            elemen = img_getElement(ob, "uncenteredSplashPath");
            if (elemen != null)
            {
                preHash = elemen[0];
                postHash = "uncentered-splash-art/" + id + "/" + skin + ".png ";
                img_addToSB(data, preHash, postHash);
            }
            
            elemen = img_getElement(ob, "tilePath");
            if (elemen != null)
            {
                preHash = elemen[0];
                postHash = "tile/" + id + "/" + skin + ".png ";
                img_addToSB(data, preHash, postHash);
            }
            
            elemen = img_getElement(ob, "loadScreenPath");
            if (elemen != null)
            {
                preHash = elemen[0];
                postHash = "loading-screen/" + id + "/" + skin + ".png ";
                img_addToSB(data, preHash, postHash);
            }
            
            elemen = img_getElement(ob, "loadScreenVintagePath");
            if (elemen != null)
            {
                preHash = elemen[0];
                postHash = "loading-screen-vintage/" + id + "/" + skin + ".png ";
                img_addToSB(data, preHash, postHash);
            }
            
            
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
                    if (elemen != null)
                    {
                        preHash = elemen[0];
                        postHash = "chroma/" + id + "/" + skin + ".png ";
                        img_addToSB(data, preHash, postHash);
                    }
                }
            }
        }
        
    }
    
    private void img_findIconPathInJsonArrayFile(Path filepath, String filename, JsonWriterWrapper data) throws IOException
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
        
        if (filename.equals("perkstyles.json"))
        {
            JsonArray arr = elem.getAsJsonObject().getAsJsonArray("styles");
            
            for (JsonElement element : arr)
            {
                JsonObject ob     = element.getAsJsonObject();
                String[]   elemen = img_getElement(ob, "iconPath");
                if (elemen != null)
                {
                    String preHash  = elemen[0];
                    String postHash = filename.substring(0, filename.lastIndexOf(".json")) + "/" + ob.get("id") + elemen[1];
                    img_addToSB(data, preHash, postHash);
                }
                
                JsonObject assetMap = ob.getAsJsonObject("assetMap");
                for (String key : assetMap.keySet())
                {
                    elemen = img_getElement(assetMap, key);
                    if (elemen != null)
                    {
                        String preHash  = elemen[0];
                        String postHash = filename.substring(0, filename.lastIndexOf(".json")) + "/" + ob.get("id") + elemen[1];
                        img_addToSB(data, preHash, postHash);
                    }
                }
            }
        } else
        {
            JsonArray arr = elem.getAsJsonArray();
            
            for (JsonElement element : arr)
            {
                JsonObject ob     = element.getAsJsonObject();
                String[]   elemen = img_getElement(ob, "iconPath");
                if (elemen != null)
                {
                    String preHash  = elemen[0];
                    String postHash = filename.substring(0, filename.lastIndexOf(".json")) + "/" + ob.get("id") + elemen[1];
                    img_addToSB(data, preHash, postHash);
                }
            }
        }
    }
    
    private void img_generateHashList(String folderName, Integer depths, String fileType, JsonWriterWrapper sb)
    {
        String pathPrefix = img_pre + "v1/" + folderName + "/";
        
        for (int i = -1; i < depths; i++)
        {
            try
            {
                String value  = String.format(pathPrefix + "%s." + fileType, i);
                String pretty = String.format(folderName + "/%s." + fileType, i);
                img_addToSB(sb, value, pretty);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    private void img_generateHashListNested(String folderName, Integer[] depths, String fileType, JsonWriterWrapper sb)
    {
        String pathPrefix = img_pre + "v1/" + folderName + "/";
        String format     = pathPrefix + "%1$s/%2$s%3$03d." + fileType;
        
        for (int i = -1; i < depths[0]; i++)
        {
            for (int j = -1; j < depths[1]; j++)
            {
                String value;
                if (j > 0)
                {
                    try
                    {
                        value = String.format(format, i, i, j);
                        String pretty = String.format(folderName + "/%1$s/%2$d." + fileType, i, j);
                        img_addToSB(sb, value, pretty);
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    private void img_addToSB(JsonWriterWrapper sb, String hashMe, String realPath) throws IOException
    {
        Path path = UtilHandler.CDRAGON_FOLDER.resolve("images_out").resolve(hashMe.trim());
        if (Files.exists(path))
        {
            if (!sb.toString().contains(realPath.trim()))
            {
                sb.name(realPath.trim()).value(path.toString().replace("\\", "/").trim());
            }
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
    
}
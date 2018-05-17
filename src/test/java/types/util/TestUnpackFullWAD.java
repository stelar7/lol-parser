package types.util;

import com.google.gson.*;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.BINFile;
import no.stelar7.cdragon.types.bnk.BNKParser;
import no.stelar7.cdragon.types.bnk.data.*;
import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.ogg.OGGParser;
import no.stelar7.cdragon.types.ogg.data.*;
import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.SKNFile;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.types.wem.WEMParser;
import no.stelar7.cdragon.types.wem.data.WEMFile;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.*;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class TestUnpackFullWAD
{
    WADParser wadParser = new WADParser();
    DDSParser ddsParser = new DDSParser();
    BINParser binParser = new BINParser();
    BNKParser bnkParser = new BNKParser();
    OGGParser oggParser = new OGGParser();
    WEMParser wemParser = new WEMParser();
    SKNParser sknParser = new SKNParser();
    
    @Test
    public void testUnpack() throws IOException
    {
        Path wadFolder    = UtilHandler.DOWNLOADS_FOLDER.resolve("Pyke");
        Path outputFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("PykeParsed");
        
        Files.createDirectories(wadFolder);
        Files.createDirectories(outputFolder);
        
        WADFile wadFile = wadParser.parse(UtilHandler.DOWNLOADS_FOLDER.resolve("Pyke.wad.client"));
        wadFile.extractFiles("Champions", "Pyke.wad.client", wadFolder);
        
        List<Path> handled = new ArrayList<>();
        
        Files.walkFileTree(wadFolder, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                String filename = UtilHandler.pathToFilename(file);
                String ending   = UtilHandler.getEnding(file);
                
                switch (ending)
                {
                    case "dds":
                    {
                        BufferedImage image = ddsParser.parse(file);
                        ImageIO.write(image, "png", outputFolder.resolve(filename + ".png").toFile());
                        handled.add(file);
                        break;
                    }
                    case "bin":
                    {
                        BINFile binFile = binParser.parse(file);
                        Files.write(outputFolder.resolve(filename + ".json"), binFile.toJson().getBytes(StandardCharsets.UTF_8));
                        handled.add(file);
                        break;
                    }
                    case "bnk":
                    {
                        BNKFile bnkFile = bnkParser.parse(file);
                        if (bnkFile.getData() != null)
                        {
                            for (BNKDATAWEMFile bnkdatawemFile : bnkFile.getData().getWemFiles())
                            {
                                WEMFile wemFile = wemParser.parse(bnkdatawemFile.getData());
                                if (wemFile.getData() == null)
                                {
                                    continue;
                                }
                                
                                OGGStream oggStream = oggParser.parse(wemFile.getData());
                                Files.write(outputFolder.resolve(filename + ".ogg"), oggStream.getData().toByteArray());
                            }
                        }
                        handled.add(file);
                        break;
                    }
                    case "skn":
                    {
                        SKNFile skn = sknParser.parse(file);
                        Files.write(outputFolder.resolve(filename + ".obj"), skn.toOBJ().getBytes(StandardCharsets.UTF_8));
                        handled.add(file);
                        break;
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        handled.forEach(path -> path.toFile().delete());
        
    }
    
    @Test
    public void checkForChampionHash() throws IOException
    {
        Path              outputFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("temp");
        JsonWriterWrapper jsonWriter   = new JsonWriterWrapper();
        jsonWriter.beginObject();
        
        Files.walkFileTree(outputFolder, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (UtilHandler.getEnding(file).equalsIgnoreCase("json"))
                {
                    System.out.println(file.toString());
                    JsonObject e = UtilHandler.getJsonParser().parse(UtilHandler.readAsString(file)).getAsJsonObject();
                    parseRecursive(e);
                }
                return FileVisitResult.CONTINUE;
            }
            
            private void parseRecursive(JsonObject e) throws IOException
            {
                for (String key : e.keySet())
                {
                    JsonElement elem = e.get(key);
                    if (elem.isJsonObject())
                    {
                        parseRecursive(elem.getAsJsonObject());
                    } else if (elem.isJsonPrimitive())
                    {
                        String primitive = elem.getAsString().toLowerCase(Locale.ENGLISH);
                        if (primitive.contains("assets"))
                        {
                            String hash      = HashHandler.computeXXHash64(primitive);
                            String knownHash = HashHandler.getWadHashes("champions").get(hash);
                            
                            if (knownHash != null)
                            {
                                continue;
                            }
                            
                            if (jsonWriter.toString().contains(hash))
                            {
                                continue;
                            }
                            
                            jsonWriter.name(hash).value(primitive);
                        }
                    }
                }
            }
        });
        jsonWriter.endObject();
        Files.write(Paths.get("combined.json"), jsonWriter.toString().getBytes(StandardCharsets.UTF_8));
        unsplit();
        
    }
    
    public void unsplit() throws IOException
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
            
            String   pluginPre = second.substring("plugins/".length());
            String[] plugin    = {pluginPre.substring(0, pluginPre.indexOf('/'))};
            
            if (second.startsWith("assets"))
            {
                plugin[0] = "champions";
            }
            
            Map<String, String> hashes = HashHandler.getWadHashes(plugin[0]);
            hashes.computeIfAbsent(first, (key) -> {
                changedPlugins.add(plugin[0]);
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
            
            System.out.println("Sorting hashes");
            foundHashes.sort(Comparator.comparing(Vector2::getSecond, new NaturalOrderComparator()));
            
            System.out.println("Writing hashes");
            JsonWriterWrapper jsonWriter = new JsonWriterWrapper();
            jsonWriter.beginObject();
            for (Vector2<String, String> pair : foundHashes)
            {
                jsonWriter.name(pair.getFirst()).value(pair.getSecond());
            }
            jsonWriter.endObject();
            Files.write(HashHandler.WAD_HASH_STORE.resolve(plugin + ".json"), jsonWriter.toString().getBytes(StandardCharsets.UTF_8));
        }
        
        Files.deleteIfExists(loadPath);
    }
}
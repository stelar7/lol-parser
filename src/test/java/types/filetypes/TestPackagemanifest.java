package types.filetypes;

import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.packagemanifest.PackagemanifestParser;
import no.stelar7.cdragon.types.packagemanifest.data.*;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.*;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class TestPackagemanifest
{
    
    @Test
    public void testDownloadAll()
    {
        String       data        = "http://l3cdn.riotgames.com/releases/pbe/projects/lol_game_client/releases/releaselisting_PBE";
        List<String> files       = WebHandler.readWeb(data);
        Path         extractPath = UtilHandler.DOWNLOADS_FOLDER.resolve("pman");
        
        files.removeAll(Arrays.asList(extractPath.toFile().list()));
        String url = "http://l3cdn.riotgames.com/releases/pbe/projects/lol_game_client/releases/%s/packages/files/packagemanifest";
        for (String version : files)
        {
            String download = String.format(url, version);
            WebHandler.downloadFile(extractPath.resolve(version), download);
        }
    }
    
    @Test
    public void testFindFiles() throws IOException
    {
        List<String>        versions = WebHandler.readWeb("http://l3cdn.riotgames.com/releases/live/projects/lol_game_client/releases/releaselisting_EUW");
        ByteArray           data     = WebHandler.readBytes(String.format("http://l3cdn.riotgames.com/releases/live/projects/lol_game_client/releases/%s/packages/files/packagemanifest", versions.get(0)));
        PackagemanifestFile file     = new PackagemanifestParser().parse(data);
        List<String> files = file.getFiles()
                                 .stream()
                                 .map(PackagemanifestLine::getFilePath)
                                 .map(p -> "http://l3cdn.riotgames.com/releases/live" + p)
                                 .filter(p -> p.toLowerCase().contains("inhib"))
                                 .collect(Collectors.toList());
        
        Path folder = UtilHandler.DOWNLOADS_FOLDER.resolve("icons");
        files.forEach(f -> WebHandler.downloadFile(folder.resolve(UtilHandler.getFilename(f)), f));
        
        Files.walkFileTree(folder, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            {
                if (dir.toString().contains("parsed"))
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (file.toString().toLowerCase().contains(".dds"))
                {
                    DDSParser     parser     = new DDSParser();
                    BufferedImage image      = parser.parseCompressed(file);
                    Path          outputPath = file.resolveSibling("parsed/" + UtilHandler.pathToFilename(file) + ".png");
                    Files.createDirectories(outputPath.getParent());
                    ImageIO.write(image, "png", outputPath.toFile());
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
    }
    
    @Test
    public void testFindHashes() throws IOException
    {
        PackagemanifestParser parser      = new PackagemanifestParser();
        List<String>          names       = new ArrayList<>();
        Path                  extractPath = UtilHandler.DOWNLOADS_FOLDER.resolve("pman");
        
        Files.walkFileTree(extractPath, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
            {
                System.out.println("Parsing: " + path.toString());
                
                PackagemanifestFile file = parser.parse(path);
                for (PackagemanifestLine line : file.getFiles())
                {
                    String subbed = line.getFilePath().substring(line.getFilePath().indexOf("files/") + 5).toLowerCase(Locale.ENGLISH);
                    if (subbed.startsWith("/data/"))
                    {
                        subbed = subbed.substring("/data/".length());
                        subbed = "assets/" + subbed;
                    }
                    
                    if (subbed.startsWith("/levels/"))
                    {
                        subbed = "assets" + subbed;
                    }
                    
                    if (subbed.startsWith("/"))
                    {
                        subbed = subbed.substring(1);
                    }
                    
                    String lineInner = subbed.replace(".compressed", "");
                    hashAndAdd(lineInner);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        write();
    }
    
    private void hashAndAdd(String hashMe)
    {
        String[] cases = new String[]{
                hashMe.trim(), hashMe.toLowerCase(Locale.ENGLISH).trim()
        };
        
        for (String hVal : cases)
        {
            String hash      = HashHandler.computeXXHash64(hVal);
            String knownHash = HashHandler.getWadHashes("champions").get(hash);
            
            if (knownHash != null)
            {
                continue;
            }
            
            Vector2<String, String> data = new Vector2<>(hash, hVal);
            
            if (!foundHashes.contains(data))
            {
                foundHashes.add(new Vector2<>(hash, hVal));
            }
        }
    }
    
    List<Vector2<String, String>> foundHashes = new ArrayList<>();
    
    private void write() throws IOException
    {
        System.out.println("Loading remaining hashes");
        HashHandler.getWadHashes("champions").forEach((k, v) -> {
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
        Files.write(HashHandler.WAD_HASH_STORE.resolve("champions" + ".json"), jsonWriter.toString().getBytes(StandardCharsets.UTF_8));
    }
}

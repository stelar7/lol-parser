package types;

import no.stelar7.cdragon.types.packagemanifest.PackagemanifestParser;
import no.stelar7.cdragon.types.packagemanifest.data.*;
import no.stelar7.cdragon.util.handlers.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class TestPackagemanifest
{
    
    @Test
    public void testDownloadAll()
    {
        String       data        = "http://l3cdn.riotgames.com/releases/live/projects/lol_game_client/releases/releaselisting_EUW";
        List<String> files       = UtilHandler.readWeb(data);
        Path         extractPath = UtilHandler.DOWNLOADS_FOLDER.resolve("pman");
        
        files.removeAll(Arrays.asList(extractPath.toFile().list()));
        String url = "http://l3cdn.riotgames.com/releases/live/projects/lol_game_client/releases/%s/packages/files/packagemanifest";
        for (String version : files)
        {
            String download = String.format(url, version);
            UtilHandler.downloadFile(extractPath.resolve(version), download);
        }
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
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
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
                    names.add(lineInner);
                }
                
                StringBuilder sb = new StringBuilder();
                for (String name : names)
                {
                    String hash = HashHandler.computeXXHash64(name);
                    sb.append("\"").append(hash).append("\": \"").append(name).append("\",\n");
                }
                
                Files.write(extractPath.resolve("hashes.json"), sb.toString().getBytes(StandardCharsets.UTF_8));
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        
    }
}

import com.google.gson.*;
import no.stelar7.cdragon.types.packagemanifest.*;
import no.stelar7.cdragon.types.packagemanifest.data.PackagemanifestLine;
import no.stelar7.cdragon.util.*;
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
        Path extractPath = Paths.get(System.getProperty("user.home"), "Downloads", "pman");
        
        String url = "http://l3cdn.riotgames.com/releases/live/projects/lol_game_client/releases/%s/packages/files/packagemanifest";
        for (int i = UtilHandler.getLongFromIP("0.0.2.0"); i > 0; i--)
        {
            String version  = UtilHandler.getIPFromLong(i);
            String download = String.format(url, version);
            UtilHandler.downloadEfficient(extractPath.resolve(version), download);
        }
    }
    
    @Test
    public void testDownloadMissing() throws IOException
    {
        PackagemanifestParser parser      = new PackagemanifestParser();
        Path                  file        = Paths.get(System.getProperty("user.home"), "Downloads", "releaselisting_EUW");
        Path                  extractPath = Paths.get(System.getProperty("user.home"), "Downloads", "pman");
        List<String>          files       = Files.readAllLines(file);
        
        files.removeAll(Arrays.asList(extractPath.toFile().list()));
        String url = "http://l3cdn.riotgames.com/releases/live/projects/lol_game_client_en_gb/releases/%s/packages/files/packagemanifest";
        for (String version : files)
        {
            String download = String.format(url, version);
            UtilHandler.downloadEfficient(extractPath.resolve(version), download);
        }
    }
    
    @Test
    public void testFindLatestInibin() throws IOException
    {
        PackagemanifestParser parser      = new PackagemanifestParser();
        Map<String, String>   data        = new HashMap<>();
        List<Path>            filelist    = new ArrayList<>();
        List<String>          finalList   = new ArrayList<>();
        List<String>          unknown     = new ArrayList<>();
        Path                  summaryPath = Paths.get(System.getProperty("user.home"), "Downloads\\temp\\rcp-be-lol-game-data\\rcp-be-lol-game-data\\plugins\\rcp-be-lol-game-data\\global\\default\\v1\\champion-summary.json");
        JsonElement           summaryData = new JsonParser().parse(new String(Files.readAllBytes(summaryPath), StandardCharsets.UTF_8));
        
        Path extractPath  = Paths.get(System.getProperty("user.home"), "Downloads", "pman");
        Path extractPath2 = Paths.get(System.getProperty("user.home"), "Downloads", "pman_inibin");
        
        Files.walkFileTree(extractPath, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                filelist.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        
        filelist.sort(new NaturalOrderComparator().reversed());
        
        for (Path path : filelist)
        {
            System.out.println("Parsing: " + path.toString());
            
            PackagemanifestFile file = parser.parse(path);
            for (PackagemanifestLine line : file.getFiles())
            {
                data.putIfAbsent(line.getFilePath().substring(line.getFilePath().indexOf("files")), line.getFilePath());
            }
        }
        
        
        String path = "files/DATA/Characters/%s/%s.inibin.compressed";
        String url  = "http://l3cdn.riotgames.com/releases/live%s";
        for (JsonElement element : summaryData.getAsJsonArray())
        {
            JsonObject datum = element.getAsJsonObject();
            String     alias = datum.get("alias").getAsString();
            String     furl  = String.format(url, data.get(String.format(path, alias, alias)));
            
            if (furl.toLowerCase().contains("null"))
            {
                unknown.add(alias);
            }
            
            UtilHandler.downloadEfficient(extractPath2.resolve(alias + ".inibin.compressed"), furl);
        }
        
        System.out.println();
        for (String s : unknown)
        {
            System.out.println(s);
        }
        
        System.out.println();
        for (String key : data.keySet())
        {
            for (String unk : unknown)
            {
                if (key.toLowerCase(Locale.ENGLISH).contains(unk.toLowerCase(Locale.ENGLISH)))
                {
                    System.out.println(key);
                }
            }
        }
    }
}

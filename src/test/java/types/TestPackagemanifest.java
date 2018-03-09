package types;

import no.stelar7.cdragon.types.packagemanifest.PackagemanifestParser;
import no.stelar7.cdragon.types.packagemanifest.data.*;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.io.IOException;
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
    public void testFindLatest() throws IOException
    {
        PackagemanifestParser parser      = new PackagemanifestParser();
        Map<String, String>   data        = new HashMap<>();
        List<Path>            filelist    = new ArrayList<>();
        List<String>          finalList   = new ArrayList<>();
        List<String>          unknown     = new ArrayList<>();
        Path                  summaryPath = UtilHandler.DOWNLOADS_FOLDER.resolve("temp\\rcp-be-lol-game-data\\rcp-be-lol-game-data\\plugins\\rcp-be-lol-game-data\\global\\default\\v1\\champion-summary.json");
        
        Path extractPath  = UtilHandler.DOWNLOADS_FOLDER.resolve("pman");
        Path extractPath2 = UtilHandler.DOWNLOADS_FOLDER.resolve("pman_out");
        
        Files.walkFileTree(extractPath, new SimpleFileVisitor<>()
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
        
        String inibinPath = "files/DATA/Characters/%s/%s.inibin.compressed";
        String binPath    = "files/DATA/Characters/%s/%s.bin.compressed";
        String url        = "http://l3cdn.riotgames.com/releases/live%s";
        
        System.out.println();
        unknown.sort(new NaturalOrderComparator());
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

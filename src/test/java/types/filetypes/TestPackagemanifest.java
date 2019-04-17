package types.filetypes;

import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.packagemanifest.PackagemanifestParser;
import no.stelar7.cdragon.types.packagemanifest.data.*;
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
    public void testFindFiles() throws IOException
    {
        List<String>        versions = WebHandler.readWeb("http://l3cdn.riotgames.com/releases/live/projects/lol_game_client/releases/releaselisting_EUW");
        ByteArray           data     = WebHandler.readBytes(String.format("http://l3cdn.riotgames.com/releases/live/projects/lol_game_client/releases/%s/packages/files/packagemanifest", versions.get(0)));
        PackagemanifestFile file     = new PackagemanifestParser().parse(data);
        List<String> files = file.getFiles()
                                 .stream()
                                 .map(PackagemanifestLine::getFilePath)
                                 .map(p -> "http://l3cdn.riotgames.com/releases/live" + p)
                                 //.filter(p -> p.toLowerCase().contains("inhib"))
                                 .collect(Collectors.toList());
        
        Files.write(UtilHandler.CDRAGON_FOLDER.resolve("all.log"), String.join("\n", files).getBytes(StandardCharsets.UTF_8));
        
        Path folder = UtilHandler.CDRAGON_FOLDER.resolve("icons");
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
}

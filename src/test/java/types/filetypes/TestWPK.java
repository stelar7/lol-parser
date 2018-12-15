package types.filetypes;

import no.stelar7.cdragon.types.wem.data.WEMFile;
import no.stelar7.cdragon.types.wpk.WPKParser;
import no.stelar7.cdragon.types.wpk.data.WPKFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestWPK
{
    @Test
    public void testWPK()
    {
        WPKParser parser = new WPKParser();
        
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\aatrox_base_vo_audio.wpk");
        System.out.println("Parsing: " + file.toString());
        
        WPKFile data = parser.parse(file);
        
        for (WEMFile wemFile : data.getWEMFiles())
        {
            System.out.println(wemFile.getFilename() + (wemFile.getData() == null ? " - HAS JUNK CHUNK!" : ""));
        }
    }
    
    @Test
    public void getFilenames() throws IOException
    {
        WPKParser    parser = new WPKParser();
        Path         from   = UtilHandler.DOWNLOADS_FOLDER.resolve("pbe");
        List<String> names  = new ArrayList<>();
        Files.walkFileTree(from, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (file.toString().endsWith(".wpk"))
                {
                    WPKFile data = parser.parse(file);
                    names.addAll(data.getWEMFiles().stream().map(WEMFile::getFilename).collect(Collectors.toList()));
                    
                }
                return FileVisitResult.CONTINUE;
            }
        });
        Files.write(from.resolveSibling("wemfilenames"), UtilHandler.getGson().toJson(names).getBytes(StandardCharsets.UTF_8));
    }
}

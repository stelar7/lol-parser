package types.filetypes;

import no.stelar7.cdragon.types.ogg.OGGParser;
import no.stelar7.cdragon.types.ogg.data.OGGStream;
import no.stelar7.cdragon.types.wem.WEMParser;
import no.stelar7.cdragon.types.wem.data.WEMFile;
import no.stelar7.cdragon.types.wpk.WPKParser;
import no.stelar7.cdragon.types.wpk.data.WPKFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class TestOGG
{
    
    @Test
    public void testOGG() throws IOException
    {
        WPKParser wpkParser = new WPKParser();
        OGGParser parser    = new OGGParser();
        WEMParser wemparser = new WEMParser();
        
        
        Path    wpkfile = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\aatrox_base_vo_audio.wpk");
        WPKFile wpk     = wpkParser.parse(wpkfile);
        
        /*
        this extracts all the files, then parses
        
        wpk.extract(wpkfile.getParent());
        Files.walkFileTree(UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test"), new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (file.toString().endsWith("wem"))
                {
                    WEMFile   wem  = wemparser.parse(file);
                    OGGStream data = parser.parse(wem.getData());
                    
                    Files.write(file.resolveSibling(UtilHandler.pathToFilename(file) + ".ogg"), data.getData().toByteArray());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        */
        
        for (WEMFile wemFile : wpk.getWEMFiles())
        {
            WEMFile   wem = wemparser.parse(new ByteArray(wemFile.getData().getDataBytes()));
            OGGStream ogg = parser.parse(wem.getData());
            Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\ogg\\" + wemFile.getFilename() + ".ogg"), ogg.getData().toByteArray());
        }
        
    }
}

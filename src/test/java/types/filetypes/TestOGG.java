package types.filetypes;

import no.stelar7.cdragon.types.ogg.OGGParser;
import no.stelar7.cdragon.types.ogg.data.OGGStream;
import no.stelar7.cdragon.types.wem.WEMParser;
import no.stelar7.cdragon.types.wem.data.WEMFile;
import no.stelar7.cdragon.types.wpk.WPKParser;
import no.stelar7.cdragon.types.wpk.data.WPKFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;

public class TestOGG
{
    
    @Test
    public void testOGG() throws IOException
    {
        WPKParser wpkParser = new WPKParser();
        OGGParser parser    = new OGGParser();
        WEMParser wemparser = new WEMParser();
        
        
        Path wpkfile = UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\15646bae0aecf5be.wpk");
        Path file    = UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\24635440.wem");
        
        WPKFile wpk = wpkParser.parse(wpkfile);
        wpk.extract(wpkfile.getParent());
        
        WEMFile   wem  = wemparser.parse(file);
        OGGStream data = parser.parse(wem.getData());
        
        Files.write(file.resolveSibling("24635440.ogg"), data.getData().toByteArray());
        
        System.out.println();
    }
}

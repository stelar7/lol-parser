package types.filetypes;

import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.SKNFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class TestSKN
{
    
    @Test
    public void testSKN()
    {
        SKNParser parser = new SKNParser();
        
        Path    path = UtilHandler.DOWNLOADS_FOLDER.resolve("temp\\Champions\\assets\\characters\\aatrox\\skins\\base");
        SKNFile skn  = parser.parse(path.resolve("aatrox.skn"));
        
        System.out.println();
    }
}

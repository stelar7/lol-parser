package types;

import no.stelar7.cdragon.types.wem.WEMParser;
import no.stelar7.cdragon.types.wem.data.WEMFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.nio.file.Path;

public class TestWEM
{
    @Test
    public void testWEM()
    {
        WEMParser parser = new WEMParser();
        
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\24635440.wem");
        System.out.println("Parsing: " + file.toString());
        
        WEMFile data = parser.parse(file);
        System.out.println();
    }
    
}

package types.filetypes;

import no.stelar7.cdragon.types.anm.ANMParser;
import no.stelar7.cdragon.types.anm.data.ANMFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.nio.file.Path;

public class TestANM
{
    
    @Test
    public void testANM()
    {
        ANMParser parser = new ANMParser();
        
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\Aatrox_Attack1.anm");
        System.out.println("Parsing: " + file.toString());
    
        ANMFile data = parser.parse(file);
        System.out.println();
    }
}

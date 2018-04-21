package types.filetypes;

import no.stelar7.cdragon.types.skl.SKLParser;
import no.stelar7.cdragon.types.skl.data.SKLFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.nio.file.Path;

public class TestSKL
{
    
    @Test
    public void testSKL()
    {
        SKLParser parser = new SKLParser();
        
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\cc1796644bc53b73.skl");
        System.out.println("Parsing: " + file.toString());
    
        SKLFile data = parser.parse(file);
        System.out.println();
    }
}

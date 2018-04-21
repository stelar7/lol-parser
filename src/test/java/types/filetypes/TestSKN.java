package types.filetypes;

import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.SKNFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.nio.file.Path;

public class TestSKN
{
    
    @Test
    public void testSKL()
    {
        SKNParser parser = new SKNParser();
        
        Path path = UtilHandler.DOWNLOADS_FOLDER.resolve("lolmodelviewer\\SampleModels\\filearchives\\0.0.0.48\\DATA\\Characters\\TeemoMushroom\\SuperTrap.skn");
        
        SKNFile data = parser.parse(path);
        System.out.println();
    }
}

package types;

import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.SKNFile;
import org.junit.Test;

import java.nio.file.*;

public class TestSKN
{
    
    @Test
    public void testSKL()
    {
        SKNParser parser = new SKNParser();
        
        Path path = Paths.get(System.getProperty("user.home"), "Downloads\\lolmodelviewer\\SampleModels\\filearchives\\0.0.0.48\\DATA\\Characters\\TeemoMushroom\\SuperTrap.skn");
        
        SKNFile data = parser.parse(path);
        System.out.println();
    }
}

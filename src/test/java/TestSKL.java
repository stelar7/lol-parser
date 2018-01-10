import no.stelar7.cdragon.types.skl.SKLParser;
import no.stelar7.cdragon.types.skl.data.SKLFile;
import org.junit.Test;

import java.nio.file.*;

public class TestSKL
{
    
    @Test
    public void testSKL()
    {
        SKLParser parser = new SKLParser();
        
        Path file = Paths.get(System.getProperty("user.home"), "Downloads\\parser_test", "cc1796644bc53b73.skl");
        System.out.println("Parsing: " + file.toString());
    
        SKLFile data = parser.parse(file);
        System.out.println();
    }
}

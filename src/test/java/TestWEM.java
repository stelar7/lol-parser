import no.stelar7.cdragon.types.wem.WEMParser;
import no.stelar7.cdragon.types.wem.data.WEMFile;
import org.junit.Test;

import java.nio.file.*;

public class TestWEM
{
    @Test
    public void testWEM()
    {
        WEMParser parser = new WEMParser();
        
        Path file = Paths.get(System.getProperty("user.home"), "Downloads\\parser_test", "24635440.wem");
        System.out.println("Parsing: " + file.toString());
        
        WEMFile data = parser.parse(file);
        System.out.println();
    }
  
}

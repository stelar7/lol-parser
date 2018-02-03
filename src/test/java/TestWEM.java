import no.stelar7.cdragon.types.ogg.OGGParser;
import no.stelar7.cdragon.types.ogg.data.OGGStream;
import no.stelar7.cdragon.types.wem.WEMParser;
import no.stelar7.cdragon.types.wem.data.WEMFile;
import org.junit.Test;

import java.io.IOException;
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
    
    
    @Test
    public void testOGG() throws IOException
    {
        OGGParser parser    = new OGGParser();
        WEMParser wemparser = new WEMParser();
        
        Path file = Paths.get(System.getProperty("user.home"), "Downloads\\parser_test", "24635440.wem");
        System.out.println("Parsing: " + file.toString());
        
        WEMFile wem = wemparser.parse(file);
        OGGStream data = parser.parse(wem.getData());
        
        Files.write(file.resolveSibling("24635440.ogg"),data.getData().toByteArray());
        
        System.out.println();
    }
}

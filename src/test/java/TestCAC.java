import no.stelar7.cdragon.types.cac.CACParser;
import no.stelar7.cdragon.types.cac.data.CACFile;
import org.junit.Test;

import java.nio.file.*;

public class TestCAC
{
    
    
    @Test
    public void testCAC()
    {
        CACParser parser = new CACParser();
        
        Path file = Paths.get(System.getProperty("user.home"), "Downloads", "Yasuo_Base.cac");
        System.out.println("Parsing: " + file.toString());
        
        CACFile data = parser.parse(file);
    }
}

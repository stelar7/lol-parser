package types;

import no.stelar7.cdragon.types.anm.ANMParser;
import no.stelar7.cdragon.types.anm.data.ANMFile;
import org.junit.Test;

import java.nio.file.*;

public class TestANM
{
    
    @Test
    public void testANM()
    {
        ANMParser parser = new ANMParser();
        
        Path file = Paths.get(System.getProperty("user.home"), "Downloads\\parser_test", "Aatrox_Attack1.anm");
        System.out.println("Parsing: " + file.toString());
    
        ANMFile data = parser.parse(file);
        System.out.println();
    }
}

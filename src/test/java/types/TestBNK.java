package types;

import no.stelar7.cdragon.types.bnk.BNKParser;
import no.stelar7.cdragon.types.bnk.data.BNKFile;
import org.junit.Test;

import java.nio.file.*;

public class TestBNK
{
    
    @Test
    public void testBNK()
    {
        BNKParser parser = new BNKParser();
        
        Path file = Paths.get(System.getProperty("user.home"), "Downloads\\parser_test", "zoe_base_sfx_audio.bnk");
        System.out.println("Parsing: " + file.toString());
        
        BNKFile data = parser.parse(file);
        System.out.println();
    }
}

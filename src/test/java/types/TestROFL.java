package types;

import no.stelar7.cdragon.types.rofl.ROFLParser;
import no.stelar7.cdragon.types.rofl.data.ROFLFile;
import org.junit.Test;

import java.nio.file.*;

public class TestROFL
{
    
    @Test
    public void testROFL()
    {
        Path path = Paths.get(System.getProperty("user.home"), "Downloads", "parser_test", "EUW1-3541345046.rofl");
        
        ROFLParser parser = new ROFLParser();
        ROFLFile   file   = parser.parse(path);
    }
}

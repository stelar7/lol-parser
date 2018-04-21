package types.filetypes;

import no.stelar7.cdragon.types.rofl.ROFLParser;
import no.stelar7.cdragon.types.rofl.data.ROFLFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.nio.file.Path;

public class TestROFL
{
    
    @Test
    public void testROFL()
    {
        Path path = UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\EUW1-3541345046.rofl");
        
        ROFLParser parser = new ROFLParser();
        ROFLFile   file   = parser.parse(path);
    }
}

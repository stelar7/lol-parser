package types;

import no.stelar7.cdragon.types.cac.CACParser;
import no.stelar7.cdragon.types.cac.data.CACFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.nio.file.Path;

public class TestCAC
{
    
    
    @Test
    public void testCAC()
    {
        CACParser parser = new CACParser();
        
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("Yasuo_Base.cac");
        System.out.println("Parsing: " + file.toString());
        
        CACFile data = parser.parse(file);
    }
}

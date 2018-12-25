package types.filetypes;

import no.stelar7.cdragon.types.mgeo.MGEOParser;
import no.stelar7.cdragon.types.mgeo.data.MGEOFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.nio.file.Path;

public class TestMGEO
{
    @Test
    public void testMGEO()
    {
        MGEOParser parser = new MGEOParser();
        
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\9a61cbe95992f8ce.mapgeo");
        System.out.println("Parsing: " + file.toString());
    
        MGEOFile data = parser.parse(file);
        System.out.println();
    }
    
}

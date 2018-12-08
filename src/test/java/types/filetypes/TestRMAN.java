package types.filetypes;

import no.stelar7.cdragon.types.rman.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.nio.file.Path;

public class TestRMAN
{
    @Test
    public void testRMAN()
    {
        RMANParser parser = new RMANParser();
        
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\DC9F6F78A04934D6.manifest");
        System.out.println("Parsing: " + file.toString());
        
        RMANFile data = parser.parse(file);
        System.out.println();
    }
}

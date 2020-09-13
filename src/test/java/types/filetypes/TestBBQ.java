package types.filetypes;

import no.stelar7.cdragon.types.bbq.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;

public class TestBBQ
{
    BBQParser parser = new BBQParser();
    
    @Test
    public void testBBQ() throws IOException
    {
        Path file = UtilHandler.CDRAGON_FOLDER.resolve("C:\\Users\\Steffen\\Desktop\\unitypack\\UnityPack-master\\00-79-29-4a-ee-19-7f-ff-6b-50-58-60-a1-16-f6-93.bbq");
        Files.createDirectories(file.resolveSibling("generated"));
        BBQFile data = parser.parse(file);
        
        for (BBQAsset entry : data.getEntries())
        {
            System.out.println();
        }
    }
}

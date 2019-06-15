package types.filetypes;

import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.SKNFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class TestSKN
{
    
    @Test
    public void testSKN() throws IOException
    {
        SKNParser parser = new SKNParser();
        
        Path    path = UtilHandler.CDRAGON_FOLDER.resolve("../");
        SKNFile skn  = parser.parse(path.resolve("temp.skn"));
        
        for (int i = 0; i < skn.getMaterials().size(); i++)
        {
            Files.write(path.resolve("temp" + i + ".obj"), skn.toOBJ(skn.getMaterials().get(i)).getBytes(StandardCharsets.UTF_8));
        }
        
        System.out.println();
    }
}

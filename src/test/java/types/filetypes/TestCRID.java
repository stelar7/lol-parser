package types.filetypes;

import no.stelar7.cdragon.types.crid.CRIDParser;
import no.stelar7.cdragon.types.crid.data.CRIDFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.Pair;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class TestCRID
{
    
    @Test
    public void testCRID() throws IOException
    {
        CRIDParser p    = new CRIDParser();
        CRIDFile   crid = p.parse(UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon/9fbb7f50baf65f23.crid"));
        
        for (Pair<String, byte[]> pair : crid.getStreamData())
        {
            String filename = pair.getA();
            byte[] data     = pair.getB();
            
            Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve(filename), data);
        }
    }
}

package types.filetypes;

import no.stelar7.cdragon.types.bnk.BNKParser;
import no.stelar7.cdragon.types.bnk.data.*;
import no.stelar7.cdragon.types.ogg.OGGParser;
import no.stelar7.cdragon.types.ogg.data.OGGStream;
import no.stelar7.cdragon.types.wem.WEMParser;
import no.stelar7.cdragon.types.wem.data.WEMFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;

import static no.stelar7.cdragon.util.handlers.UtilHandler.*;

public class TestBNK
{
    
    @Test
    public void testBNK() throws IOException
    {
        BNKParser parser = new BNKParser();
        
        Path file = DOWNLOADS_FOLDER.resolve("Pyke\\Champions\\unknown\\9e2a3b17be356ffc.bnk");
        System.out.println("Parsing: " + file.toString());
        
        BNKFile data = parser.parse(file);
        int     i    = 0;
        for (BNKDATAWEMFile bnkdatawemFile : data.getData().getWemFiles())
        {
            Files.write(DOWNLOADS_FOLDER.resolve("test.wem"), bnkdatawemFile.getData());
            WEMFile wemFile = new WEMParser().parse(bnkdatawemFile.getData());
            if (wemFile.getData() != null)
            {
                OGGStream oggStream = new OGGParser().parse(wemFile.getData());
                Files.write(DOWNLOADS_FOLDER.resolve("9e2a3b17be356ffc" + i++ + ".ogg"), oggStream.getData().toByteArray());
            }
        }
    }
}

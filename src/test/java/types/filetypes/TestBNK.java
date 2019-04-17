package types.filetypes;

import no.stelar7.cdragon.types.bnk.BNKParser;
import no.stelar7.cdragon.types.bnk.data.*;
import no.stelar7.cdragon.types.ogg.OGGParser;
import no.stelar7.cdragon.types.ogg.data.OGGStream;
import no.stelar7.cdragon.types.wem.WEMParser;
import no.stelar7.cdragon.types.wem.data.WEMFile;
import no.stelar7.cdragon.util.types.ByteArray;
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
        
        Path file = CDRAGON_FOLDER.resolve("Ashe\\levels\\unknown\\e42848c953b2e155.bnk");
        System.out.println("Parsing: " + file.toString());
        
        BNKFile data = parser.parse(file);
        int     i    = 0;
        for (BNKDATAWEMFile bnkdatawemFile : data.getData().getWemFiles())
        {
            WEMFile wemFile = new WEMParser().parse(new ByteArray(bnkdatawemFile.getData()));
            if (wemFile.getData() != null)
            {
                OGGStream oggStream = new OGGParser().parse(wemFile.getData());
                Files.write(CDRAGON_FOLDER.resolve("9e2a3b17be356ffc" + i++ + ".ogg"), oggStream.getData().toByteArray());
            }
        }
    }
}

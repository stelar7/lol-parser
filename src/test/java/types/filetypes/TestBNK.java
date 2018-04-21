package types.filetypes;

import no.stelar7.cdragon.types.bnk.BNKParser;
import no.stelar7.cdragon.types.bnk.data.BNKFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.nio.file.Path;

public class TestBNK
{
    
    @Test
    public void testBNK()
    {
        BNKParser parser = new BNKParser();
        
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\zoe_base_sfx_audio.bnk");
        System.out.println("Parsing: " + file.toString());
        
        BNKFile data = parser.parse(file);
        System.out.println();
    }
}

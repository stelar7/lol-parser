package types.filetypes;

import no.stelar7.cdragon.types.rst.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.jupiter.api.Test;

import java.nio.file.*;

public class TestRST
{
    @Test
    public void testRSTv2()
    {
        Path      file   = UtilHandler.CDRAGON_FOLDER.resolve("cdragon/rst/v2_fontconfig_en_us.txt");
        RSTParser parser = new RSTParser();
        RSTFile   output = parser.parse(file);
        System.out.println();
    }
    
    @Test
    public void testRSTv4()
    {
        Path      file   = UtilHandler.CDRAGON_FOLDER.resolve("cdragon/rst/v4_fontconfig_en_us.txt");
        RSTParser parser = new RSTParser();
        RSTFile   output = parser.parse(file);
        String    generatedtip_spell_anniee_description = output.getFromHash("generatedtip_spell_anniee_description");
        System.out.println();
    }
}

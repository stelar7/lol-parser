package types.filetypes;

import no.stelar7.cdragon.types.rbun.*;
import no.stelar7.cdragon.util.handlers.*;
import org.junit.Test;

import java.nio.file.Path;

public class TestRBUN
{
    
    @Test
    public void testRBUN()
    {
        RBUNParser parser = new RBUNParser();
        Path       path   = UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\FD00F82B0CACEEF5.bundle");
        RBUNFile   data   = parser.parse(path);
        System.out.println();
    }
}

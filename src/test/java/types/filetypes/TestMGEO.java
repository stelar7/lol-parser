package types.filetypes;

import no.stelar7.cdragon.types.mgeo.MGEOParser;
import no.stelar7.cdragon.types.mgeo.data.MGEOFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class TestMGEO
{
    @Test
    public void testMGEO()
    {
        MGEOParser parser = new MGEOParser();
        Path       file   = UtilHandler.CDRAGON_FOLDER.resolve("pbe\\data\\maps\\mapgeometry\\map22\\base.mapgeo");
        MGEOFile   data   = parser.parse(file);
        System.out.println();
    }
    
}

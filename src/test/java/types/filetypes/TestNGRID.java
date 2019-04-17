package types.filetypes;

import no.stelar7.cdragon.types.ngrid.NGridParser;
import no.stelar7.cdragon.types.ngrid.data.NGridFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.nio.file.Path;

public class TestNGRID
{
    @Test
    public void testLocal()
    {
        Path        path   = UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\aipath.aimesh_ngrid");
        NGridParser parser = new NGridParser();
        NGridFile   parsed = parser.parse(path);
        System.out.println();
    }
}

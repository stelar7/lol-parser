package types.filetypes;

import no.stelar7.cdragon.types.crid.CRIDParser;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

public class TestCRID
{
    
    @Test
    public void testCRID()
    {
        CRIDParser p = new CRIDParser();
        p.parse(UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon/9fbb7f50baf65f23.crid"));
    }
}

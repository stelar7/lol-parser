package types.filetypes;

import no.stelar7.cdragon.types.bbq.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;

public class TestBBQ
{
    BBQParser parser = new BBQParser();
    
    @Test
    public void testBBQ() throws IOException
    {
        Path file = UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\bbq\\jinxemote.bbq");
        Files.createDirectories(file.resolveSibling("generated"));
        BBQFile data = parser.parse(file);
        for (BBQBundleEntry entry : data.getEntries())
        {
            data.export(file, entry, file.resolveSibling("generated\\" + entry.getName()));
        }
    }
}

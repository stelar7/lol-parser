package types.filetypes;

import no.stelar7.cdragon.types.atlas.AtlasParser;
import no.stelar7.cdragon.types.atlas.data.AtlasFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class TestAtlas
{
    AtlasParser parser = new AtlasParser();
    
    @Test
    public void testAtlas()
    {
        Path      file = UtilHandler.CDRAGON_FOLDER.resolve("pbe\\clientstates\\gameplay\\ux\\tft\\tftmatchupbanner.unknown");
        AtlasFile data = parser.parse(file);
        System.out.println();
    }
}

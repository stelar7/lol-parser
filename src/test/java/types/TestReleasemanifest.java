package types;

import no.stelar7.cdragon.types.releasemanifest.ReleasemanifestParser;
import no.stelar7.cdragon.types.releasemanifest.data.ReleasemanifestDirectory;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.nio.file.Path;

public class TestReleasemanifest
{
    
    @Test
    public void testReleasemanifest()
    {
        ReleasemanifestParser parser = new ReleasemanifestParser();
        
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("releasemanifest (1)");
        System.out.println("Parsing: " + file.toString());
        
        ReleasemanifestDirectory parsed = parser.parse(file);
        parsed.printLines("http://l3cdn.riotgames.com/releases/pbe/projects/lol_game_client/releases/0.0.0.1/files", ".compressed");
        System.out.println();
    }
}

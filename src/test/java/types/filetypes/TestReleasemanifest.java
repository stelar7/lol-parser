package types.filetypes;

import no.stelar7.cdragon.types.releasemanifest.ReleasemanifestParser;
import no.stelar7.cdragon.types.releasemanifest.data.ReleasemanifestDirectory;
import no.stelar7.cdragon.util.handlers.WebHandler;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestReleasemanifest
{
    
    
    @Test
    public void testReleasemanifest() 
    {
        ReleasemanifestParser parser = new ReleasemanifestParser();
        
        List<String>             versions = WebHandler.readWeb("http://l3cdn.riotgames.com/releases/pbe/projects/lol_game_client/releases/releaselisting_PBE");
        ByteArray                data     = WebHandler.readBytes(String.format("http://l3cdn.riotgames.com/releases/pbe/projects/lol_game_client/releases/%s/releasemanifest", versions.get(0)));
        ReleasemanifestDirectory parsed   = parser.parse(data);
    }
}

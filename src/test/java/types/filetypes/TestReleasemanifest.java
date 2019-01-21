package types.filetypes;

import no.stelar7.cdragon.types.releasemanifest.ReleasemanifestParser;
import no.stelar7.cdragon.types.releasemanifest.data.ReleasemanifestDirectory;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.*;
import no.stelar7.cdragon.util.types.math.Vector2;
import no.stelar7.cdragon.util.writers.JsonWriterWrapper;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

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

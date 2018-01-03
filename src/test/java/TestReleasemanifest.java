import no.stelar7.cdragon.types.releasemanifest.ReleasemanifestParser;
import no.stelar7.cdragon.types.releasemanifest.data.*;
import org.junit.Test;

import java.nio.file.*;

public class TestReleasemanifest
{
    
    @Test
    public void testReleasemanifest()
    {
        ReleasemanifestParser parser = new ReleasemanifestParser();
        
        Path file = Paths.get(System.getProperty("user.home"), "Downloads", "releasemanifest");
        
        System.out.println("Parsing: " + file.toString());
        ReleasemanifestDirectory parsed = parser.parse(file);
        System.out.println();
    }
}

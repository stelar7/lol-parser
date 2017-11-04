import no.stelar7.cdragon.wad.WADParser;
import no.stelar7.cdragon.wad.data.WADFile;
import org.junit.Test;

import java.nio.file.Paths;

public class WADTest
{
    @Test
    public void testWAD() throws Exception
    {
        WADParser parser = new WADParser();
        //WADFile file = parser.parse(Paths.get("C:\\Users\\Steffen\\Downloads\\default-assets.wad"));
        WADFile file = parser.parseLatest(Paths.get("C:\\Users\\Steffen\\Downloads\\default-assets.wad.compressed"));
        
        file.extractFiles(Paths.get("C:\\Users\\Steffen\\Downloads\\"));
    }
}

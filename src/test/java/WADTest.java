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
        WADFile file  = parser.parse(Paths.get("C:\\Riot Games\\League of Legends\\RADS\\projects\\league_client\\managedfiles\\0.0.0.104\\Plugins\\rcp-fe-l10n\\assets.wad"));
        
        file.extractFiles(Paths.get("C:\\Riot Games\\League of Legends\\RADS\\projects\\league_client\\managedfiles\\0.0.0.104\\Plugins\\rcp-fe-l10n"));
    }
}
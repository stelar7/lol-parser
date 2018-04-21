package types.filetypes;

import no.stelar7.cdragon.types.releasemanifest.ReleasemanifestParser;
import no.stelar7.cdragon.types.releasemanifest.data.ReleasemanifestDirectory;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

public class TestReleasemanifest
{
    
    @Test
    public void testReleasemanifest() throws IOException
    {
        ReleasemanifestParser parser = new ReleasemanifestParser();
        
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\releasemanifest");
        System.out.println("Parsing: " + file.toString());
        
        ReleasemanifestDirectory parsed = parser.parse(file);
        List<String>             lines  = parsed.printLines("http://l3cdn.riotgames.com/releases/pbe/projects/lol_game_client/releases/0.0.0.1/files", ".compressed");
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(UtilHandler.DOWNLOADS_FOLDER.resolve("relmnf.log").toFile(), true));
        for (String line : lines)
        {
            bw.write(line);
            bw.newLine();
        }
        bw.flush();
    }
}

package types.filetypes;

import no.stelar7.cdragon.types.lockfile.LockfileParser;
import no.stelar7.cdragon.types.lockfile.data.Lockfile;
import org.junit.Test;

import java.nio.file.*;

public class TestLockfile
{
    
    @Test
    public void testLockfile()
    {
        LockfileParser parser = new LockfileParser();
        
        Path file = Paths.get("C:\\Riot Games\\League of Legends\\lockfile");
        System.out.println("Parsing: " + file.toString());
        Lockfile parsed = parser.parse(file);
    }
}

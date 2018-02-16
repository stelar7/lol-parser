package types;

import no.stelar7.cdragon.types.lockfile.LockfileParser;
import no.stelar7.cdragon.types.lockfile.data.Lockfile;
import no.stelar7.cdragon.util.readers.LCUSocketReader;
import org.junit.Test;

import java.nio.file.*;

public class TestWebSocket
{
    @Test
    public void testWebSocket() throws InterruptedException
    {
        LockfileParser parser = new LockfileParser();
        Path           file   = Paths.get("C:\\Riot Games\\League of Legends\\lockfile");
        Lockfile       parsed = parser.parse(file);
        
        Thread th = new Thread(() -> {
            LCUSocketReader reader = new LCUSocketReader(parsed);
            reader.connect();
            reader.subscribe("OnJsonApiEvent", System.out::println);
        });
        th.start();
        Thread.sleep(20000);
    }
}

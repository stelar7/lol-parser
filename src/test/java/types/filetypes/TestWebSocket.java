package types.filetypes;

import com.google.gson.*;
import no.stelar7.cdragon.types.lockfile.LockfileParser;
import no.stelar7.cdragon.types.lockfile.data.Lockfile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.LCUSocketReader;
import org.junit.Test;
import org.slf4j.helpers.Util;

import java.io.*;
import java.nio.file.*;

public class TestWebSocket
{
    @Test
    public void testWebSocket() throws InterruptedException, IOException
    {
        String    data   = String.join("", Files.readAllLines(UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon/events.json")));
        JsonArray events = new JsonParser().parse(data).getAsJsonObject().get("events").getAsJsonArray();
        
        Thread th = new Thread(() -> {
            LCUSocketReader reader = new LCUSocketReader(UtilHandler.getLCUConnectionData());
            reader.connect();
            for (JsonElement event : events)
            {
                String eventName = event.getAsString();
                if (eventName.equals("OnJsonApiEvent"))
                {
                    // this event triggers on every event, so we dont want to listen to it
                    continue;
                }
                
                reader.subscribe(eventName, System.out::println);
            }
        });
        th.start();
        Thread.sleep(Long.MAX_VALUE);
    }
}

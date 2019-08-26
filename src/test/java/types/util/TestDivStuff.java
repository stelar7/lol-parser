package types.util;

import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class TestDivStuff
{
    @Test
    public void testHashFromManifests()
    {
        //RandomAccessReader r           = new RandomAccessReader(UtilHandler.DOWNLOADS_FOLDER.resolve("pbe/unknown/final/9292a9c26c1abd6b.unknown"));
        RandomAccessReader r           = new RandomAccessReader(UtilHandler.CDRAGON_FOLDER.resolve("pbe/unknown/final/1DE85B3040E21426.unknown"));
        
        
        int                stringCount = r.readInt();
        List<String>       lines       = new ArrayList<>();
        while (r.remaining() > 0)
        {
            int    length = r.readInt();
            String data   = r.readString(length);
            lines.add(data);
        }
        
        for (String line : lines)
        {
            Long   hashNum = HashHandler.computeXXHash64AsLong(line.toLowerCase());
            String hash    = HashHandler.toHex(hashNum, 16);
            if (!HashHandler.getWADHashes().containsKey(hash))
            {
                System.out.println("found new hash!");
                System.out.println(line);
                System.out.println(hash);
            }
        }
    }
    
    
    @Test
    public void testBinHashSingle()
    {
        String toHash = "tfttraitdata";
        String output = HashHandler.toHex(HashHandler.computeBINHash(toHash), 8);
        System.out.println(output);
    }
    
    @Test
    public void testBinHashFromFile() throws IOException
    {
        List<String> unknowns = Files.readAllLines(UtilHandler.CDRAGON_FOLDER.resolve("binHashUnknown.txt"));
        Path         output   = UtilHandler.CDRAGON_FOLDER.resolve("newhash.json");
        
        Map<String, String> hashed = new HashSet<>(Files.readAllLines(UtilHandler.CDRAGON_FOLDER.resolve("wordsToTest.txt")))
                .stream()
                .map(l -> l.substring(9))
                .filter(k -> !HashHandler.getBINHash(k).equalsIgnoreCase(k))
                .collect(Collectors.toMap(k -> k, HashHandler::getBINHash));
        
        hashed.forEach((key, value) -> {
            if (unknowns.contains(value))
            {
                String formatted = String.format("\"%s\":\"%s\",%n", value, key);
                try
                {
                    Files.write(output, formatted.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }
    
    
    @Test
    public void readProcessMemory()
    {
        String handleName = "League of Legends.exe";
        //String handleName = "notepad++.exe";
        MemoryHandler.readProcessMemory(handleName);
    }
}

package types.util;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class TestDivStuff
{
    @Test
    public void testStuff()
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
        String toHash = "TFT_UnitBuffImperial";
        String output = HashHandler.getBINHash(toHash);
        System.out.println(output);
        
        List<String> possible = Arrays.asList("1AF7CAAC",
                                              "0C239C49", "20F51FA6", "28905FBE", "2A9ED9A9", "2E2194DA", "2E77F7A4",
                                              "4BFE6252", "4DEAA8BF", "4EDC8BF9", "4F3CAFAC", "517AA65D", "5377F06E",
                                              "549BC8C7", "59A200C0", "6128AB3E", "6C4915DF", "6F9A9D3E", "7FAF979E",
                                              "97A1B425", "9C2D43C2", "AC4E0877", "AED73844", "B1DA655E", "FC8430DA");
        
        System.out.println(possible.contains(output));
    }
    
    @Test
    public void testBinHashBrute()
    {
        /*
         * 1AF7CAAC = trait container
         *
         * 0C239C49 = robot
         * 20F51FA6 = glacial
         * 28905FBE = knight
         * 2A9ED9A9 = noble
         * 2E2194DA = exile
         * 2E77F7A4 = elementalist
         * 4BFE6252 = demon
         * 4DEAA8BF = shapeshifter
         * 4EDC8BF9 = ninja
         * 4F3CAFAC = dragon
         * 517AA65D = void
         * 5377F06E = sorcerer
         * 549BC8C7 = wild
         * 59A200C0 = guardian
         * 6128AB3E = brawler
         * 6C4915DF = minion
         * 6F9A9D3E = ranger
         * 7FAF979E = imperial
         * 97A1B425 = blademaster
         * 9C2D43C2 = pirate
         * AC4E0877 = gunslinger
         * AED73844 = phantom
         * B1DA655E = assassin
         * FC8430DA = yordle
         */
        
        List<String> possible = Arrays.asList("1AF7CAAC",
                                              "0C239C49", "20F51FA6", "28905FBE", "2A9ED9A9", "2E2194DA", "2E77F7A4",
                                              "4BFE6252", "4DEAA8BF", "4EDC8BF9", "4F3CAFAC", "517AA65D", "5377F06E",
                                              "549BC8C7", "59A200C0", "6128AB3E", "6C4915DF", "6F9A9D3E", "7FAF979E",
                                              "97A1B425", "9C2D43C2", "AC4E0877", "AED73844", "B1DA655E", "FC8430DA");
        
        List<Long> asLong = possible.stream().map(s -> "0x" + s).map(Long::decode).collect(Collectors.toList());
        HashHandler.bruteForceHash(HashHandler::computeBINHash, asLong);
    }
    
    @Test
    public void listTraitHashes()
    {
        List<Path> readMe = UtilHandler.getFilesMatchingPredicate(UtilHandler.CDRAGON_FOLDER.resolve("pbe"), UtilHandler.IS_JSON_PREDICATE);
        
        Set<String> unique = new HashSet<>();
        
        readMe.stream()
              .map(UtilHandler::readAsString)
              .filter(s -> s.contains("1AF7CAAC"))
              .map(s -> UtilHandler.getJsonParser().parse(s))
              .map(JsonElement::getAsJsonObject)
              .forEach(e -> {
                  String     data   = "{" + e.toString().substring(e.toString().indexOf("1AF7CAAC") - 1);
                  JsonReader reader = new JsonReader(new StringReader(data));
                  reader.setLenient(true);
            
                  JsonArray bankUnits = UtilHandler.getJsonParser().parse(reader).getAsJsonObject().getAsJsonArray("1AF7CAAC");
                  for (JsonElement path : bankUnits)
                  {
                      String real = path.getAsString().toUpperCase();
                      unique.add(real);
                  }
              });
        
        unique.forEach(System.out::println);
    }
}

package types.util;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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
        String toHash = "PercentArmorPen";
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
    public void buildTFTDataFile() throws IOException
    {
        JsonArray champData = new JsonArray();
        JsonArray traitData = new JsonArray();
        
        Path traitFile       = UtilHandler.CDRAGON_FOLDER.resolve("pbe\\unknown\\Shipping\\22E2CF785BAEAC7E.bin");
        Path fontConfig      = UtilHandler.CDRAGON_FOLDER.resolve("pbe\\data\\menu\\fontconfig_en_us.txt");
        Path champFileParent = UtilHandler.CDRAGON_FOLDER.resolve("pbe\\data\\characters");
        
        String champFileTraitContainer = "1AF7CAAC";
        String champContainerKey       = "E52B8F5D";
        String costModifier            = "1F01E303";
        String champAbilityName        = "87A69A5E";
        String champSplash             = "8BEE2972";
        String champAbilityDesc        = "BC4F18B3";
        String champAbilityIcon        = "63F1FC69";
        String displayName             = "C3143D66";
        
        Map<String, String> descs = Files.readAllLines(fontConfig)
                                         .stream()
                                         .filter(s -> s.startsWith("tr "))
                                         .map(s -> s.substring(s.indexOf(" ") + 1))
                                         .collect(Collectors.toMap(s -> {
                                             String part = s.split("=")[0];
                                             part = part.substring(part.indexOf("\"") + 1);
                                             part = part.substring(0, part.indexOf("\""));
                                             return part;
                                         }, s -> {
                                             String part = Arrays.stream(s.split("=")).skip(1).collect(Collectors.joining("="));
                                             part = part.substring(part.indexOf("\"") + 1);
                                             part = part.substring(0, part.lastIndexOf("\""));
                                             return part;
                                         }));
        
        
        BINParser parser = new BINParser();
        
        String traitContainerKey       = "6F870247";
        String traitDescription        = "765F18DA";
        String traitIcon               = "8BEE2972";
        String traitEffectContainter   = "9E0B0655";
        String innerEffectContainer    = "C130C1E5";
        String traitMinUnits           = "749EAB2B";
        String traitEffectVarContainer = "C13D6D31";
        String traitEffectVar          = "62FF42F4";
        
        Map<String, String> traitLookup = new HashMap<>();
        
        JsonElement shipping = UtilHandler.getJsonParser().parse(parser.parse(traitFile).toJson());
        JsonArray   traits   = shipping.getAsJsonObject().getAsJsonArray(traitContainerKey);
        for (JsonElement traitContainer : traits)
        {
            String     container = traitContainer.getAsJsonObject().keySet().toArray(String[]::new)[0];
            JsonObject trait     = traitContainer.getAsJsonObject().getAsJsonObject(container);
            
            String mName = trait.get("mName").getAsString();
            
            if (mName.contains("Template"))
            {
                continue;
            }
            
            traitLookup.put(container, descs.getOrDefault(trait.get(displayName).getAsString(), trait.get(displayName).getAsString()));
            
            JsonObject o = new JsonObject();
            o.add("name", new JsonPrimitive(descs.getOrDefault(trait.get(displayName).getAsString(), trait.get(displayName).getAsString())));
            o.add("desc", new JsonPrimitive(descs.getOrDefault(trait.get(traitDescription).getAsString(), trait.get(traitDescription).getAsString())));
            o.add("icon", new JsonPrimitive(trait.get(traitIcon).getAsString()));
            
            JsonArray effects = new JsonArray();
            
            JsonArray effectJson = trait.getAsJsonArray(traitEffectContainter);
            for (JsonElement effect : effectJson)
            {
                JsonObject adder = new JsonObject();
                JsonObject inner = effect.getAsJsonObject().getAsJsonObject(innerEffectContainer);
                
                JsonArray varAdded     = new JsonArray();
                JsonArray varContainer = inner.has(traitEffectVarContainer) ? inner.getAsJsonArray(traitEffectVarContainer) : new JsonArray();
                for (JsonElement vars : varContainer)
                {
                    JsonObject var     = vars.getAsJsonObject().getAsJsonObject(traitEffectVar);
                    Long       name    = var.get("name").getAsLong();
                    String     hashKey = HashHandler.toHex(name, 8, 8);
                    
                    JsonPrimitive realName = new JsonPrimitive(HashHandler.getBinHashes().getOrDefault(hashKey, hashKey));
                    JsonElement   value    = var.get("Value");
                    
                    JsonObject temp = new JsonObject();
                    temp.add("name", realName);
                    temp.add("value", value);
                    varAdded.add(temp);
                }
                
                adder.add("minUnits", new JsonPrimitive(inner.get(traitMinUnits).getAsInt()));
                adder.add("vars", varAdded);
                
                effects.add(adder);
            }
            
            o.add("effects", effects);
            traitData.add(o);
        }
        
        
        JsonArray champs = shipping.getAsJsonObject().getAsJsonArray(champContainerKey);
        for (JsonElement champContainer : champs)
        {
            String     container = champContainer.getAsJsonObject().keySet().toArray(String[]::new)[0];
            JsonObject champ     = champContainer.getAsJsonObject().getAsJsonObject(container);
            
            String mName = champ.get("mName").getAsString();
            
            if (!mName.startsWith("TFT_") || mName.equals("TFT_Template"))
            {
                continue;
            }
            
            JsonObject o = new JsonObject();
            o.add("name", new JsonPrimitive(descs.getOrDefault(champ.get(displayName).getAsString(), champ.get(displayName).getAsString())));
            o.add("cost", new JsonPrimitive(1 + (champ.has(costModifier) ? champ.get(costModifier).getAsInt() : 0)));
            o.add("splash", new JsonPrimitive(descs.getOrDefault(champ.get(champSplash).getAsString(), champ.get(champSplash).getAsString())));
            o.add("abilityName", new JsonPrimitive(descs.getOrDefault(champ.get(champAbilityName).getAsString(), champ.get(champAbilityName).getAsString())));
            o.add("abilityDesc", new JsonPrimitive(descs.getOrDefault(champ.get(champAbilityDesc).getAsString(), champ.get(champAbilityDesc).getAsString())));
            o.add("abilityIcon", new JsonPrimitive(descs.getOrDefault(champ.get(champAbilityIcon).getAsString(), champ.get(champAbilityIcon).getAsString())));
            
            String traitContainer = "1AF7CAAC";
            Path   selfBin        = champFileParent.resolve(mName).resolve(mName + ".bin");
            String data           = parser.parse(selfBin).toJson();
            data = data.substring(data.indexOf(traitContainer) - 10);
            
            JsonReader reader = new JsonReader(new StringReader(data));
            reader.setLenient(true);
            JsonObject elem       = UtilHandler.getJsonParser().parse(reader).getAsJsonObject();
            JsonArray  traitDatas = elem.getAsJsonArray(traitContainer);
            
            JsonArray addme = new JsonArray();
            for (JsonElement traitDatum : traitDatas)
            {
                addme.add(new JsonPrimitive(traitLookup.get(traitDatum.getAsString())));
            }
            o.add("traits", addme);
            
            champData.add(o);
        }
        
        
        JsonObject obj = new JsonObject();
        obj.add("champions", champData);
        obj.add("traits", traitData);
        String data = UtilHandler.getGson().toJson(UtilHandler.getJsonParser().parse(obj.toString()));
        
        Files.write(UtilHandler.CDRAGON_FOLDER.resolve("TFT.json"), data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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

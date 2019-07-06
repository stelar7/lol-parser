package types.util;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
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
    public void buildTFTDataFile() throws IOException
    {
        JsonArray champData = new JsonArray();
        JsonArray traitData = new JsonArray();
        JsonArray itemData  = new JsonArray();
        
        Path traitFile       = UtilHandler.CDRAGON_FOLDER.resolve("pbe\\data\\maps\\shipping\\map22\\map22.bin");
        Path fontConfig      = UtilHandler.CDRAGON_FOLDER.resolve("pbe\\data\\menu\\fontconfig_en_us.txt");
        Path champFileParent = UtilHandler.CDRAGON_FOLDER.resolve("pbe\\data\\characters");
        
        Function<Map<String, String>, Function<JsonObject, Function<String, String>>> getFromMapOrDefault = desc -> obj -> key -> desc.getOrDefault(obj.get(key).getAsString(), obj.get(key).getAsString());
        
        Function<JsonElement, String>      getFirstChildKey     = obj -> obj.getAsJsonObject().keySet().toArray(String[]::new)[0];
        Function<JsonElement, JsonElement> getFirstChildElement = obj -> obj.getAsJsonObject().get(getFirstChildKey.apply(obj));
        Function<JsonElement, JsonObject>  getFirstChildObject  = obj -> getFirstChildElement.apply(obj).getAsJsonObject();
        Function<JsonElement, JsonArray>   getFirstChildArray   = obj -> getFirstChildElement.apply(obj).getAsJsonArray();
        
        BiFunction<JsonObject, String, JsonArray>     getKeyOrDefaultArray   = (obj, key) -> obj.has(key) ? obj.getAsJsonArray(key) : new JsonArray();
        BiFunction<JsonObject, String, JsonPrimitive> getKeyOrDefaultInt     = (obj, key) -> obj.has(key) ? obj.get(key).getAsJsonPrimitive() : new JsonPrimitive(0);
        BiFunction<JsonObject, String, JsonPrimitive> getKeyOrDefaultStringP = (obj, key) -> obj.has(key) ? obj.get(key).getAsJsonPrimitive() : new JsonPrimitive("P");
        
        BiFunction<String, String, JsonObject> createJsonObject = (data, key) -> {
            String     realData = "{" + data.substring(data.indexOf(key) - 1);
            JsonReader reader   = new JsonReader(new StringReader(realData));
            reader.setLenient(true);
            return UtilHandler.getJsonParser().parse(reader).getAsJsonObject();
        };
        
        
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
        
        String displayName = "C3143D66";
        
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
            String     hashKey = getFirstChildKey.apply(traitContainer);
            JsonObject trait   = getFirstChildObject.apply(traitContainer);
            
            String mName = trait.get("mName").getAsString();
            
            if (mName.contains("Template"))
            {
                continue;
            }
            
            traitLookup.put(hashKey, getFromMapOrDefault.apply(descs).apply(trait).apply(displayName));
            
            JsonObject o = new JsonObject();
            o.add("name", new JsonPrimitive(getFromMapOrDefault.apply(descs).apply(trait).apply(displayName)));
            o.add("desc", new JsonPrimitive(getFromMapOrDefault.apply(descs).apply(trait).apply(traitDescription)));
            o.add("icon", new JsonPrimitive(trait.get(traitIcon).getAsString()));
            
            JsonArray effects = new JsonArray();
            
            JsonArray effectJson = trait.getAsJsonArray(traitEffectContainter);
            for (JsonElement effect : effectJson)
            {
                JsonObject adder = new JsonObject();
                JsonObject inner = effect.getAsJsonObject().getAsJsonObject(innerEffectContainer);
                
                JsonArray varAdded     = new JsonArray();
                JsonArray varContainer = getKeyOrDefaultArray.apply(inner, traitEffectVarContainer);
                for (JsonElement vars : varContainer)
                {
                    JsonObject var        = vars.getAsJsonObject().getAsJsonObject(traitEffectVar);
                    Long       name       = var.get("name").getAsLong();
                    String     hashedName = HashHandler.toHex(name, 8, 8);
                    
                    JsonPrimitive realName = new JsonPrimitive(HashHandler.getBinHashes().getOrDefault(hashedName, hashedName));
                    JsonElement   value    = var.get("value");
                    
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
        
        String champFileTraitContainer = "1AF7CAAC";
        String champContainerKey       = "E52B8F5D";
        String costModifier            = "1F01E303";
        String champAbilityName        = "87A69A5E";
        String champSplash             = "8BEE2972";
        String champAbilityDesc        = "BC4F18B3";
        String champAbilityIcon        = "63F1FC69";
        
        JsonArray champs = shipping.getAsJsonObject().getAsJsonArray(champContainerKey);
        for (JsonElement champContainer : champs)
        {
            JsonObject champ = getFirstChildObject.apply(champContainer);
            String     mName = champ.get("mName").getAsString();
            
            if (!mName.startsWith("TFT_") || mName.equals("TFT_Template"))
            {
                continue;
            }
            
            JsonObject o         = new JsonObject();
            JsonObject abilities = new JsonObject();
            
            String     realName    = mName.substring(4);
            Path       selfRealBin = champFileParent.resolve(realName).resolve(realName + ".bin");
            String     realData    = parser.parse(selfRealBin).toJson();
            JsonObject realChamp   = createJsonObject.apply(realData, "characterToolData");
            
            o.add("id", realChamp.getAsJsonObject("characterToolData").getAsJsonObject("characterToolData").get("championId"));
            o.add("name", new JsonPrimitive(getFromMapOrDefault.apply(descs).apply(champ).apply(displayName)));
            o.add("cost", new JsonPrimitive(1 + (champ.has(costModifier) ? champ.get(costModifier).getAsInt() : 0)));
            o.add("splash", new JsonPrimitive(getFromMapOrDefault.apply(descs).apply(champ).apply(champSplash)));
            
            abilities.add("name", new JsonPrimitive(getFromMapOrDefault.apply(descs).apply(champ).apply(champAbilityName)));
            abilities.add("desc", new JsonPrimitive(getFromMapOrDefault.apply(descs).apply(champ).apply(champAbilityDesc)));
            abilities.add("icon", new JsonPrimitive(getFromMapOrDefault.apply(descs).apply(champ).apply(champAbilityIcon)));
            
            
            String champDataContainer = "304496F1";
            String spellDataContainer = "5E7E5A06";
            String traitContainer     = "mLinkedTraits";
            
            String initialSpellValue = "mBaseP";
            String nextSpellValue    = "mFormula";
            
            Path   selfBin = champFileParent.resolve(mName).resolve(mName + ".bin");
            String data    = parser.parse(selfBin).toJson();
            
            JsonObject elem               = createJsonObject.apply(data, champDataContainer);
            JsonObject champContainerData = getFirstChildObject.apply(elem);
            JsonObject champItem          = getFirstChildObject.apply(champContainerData);
            
            JsonArray traitDatas = champItem.getAsJsonArray(traitContainer);
            JsonArray traitArray = new JsonArray();
            for (JsonElement traitDatum : traitDatas)
            {
                traitArray.add(new JsonPrimitive(traitLookup.get(traitDatum.getAsString())));
            }
            
            JsonObject manaContainer = getFirstChildObject.apply(champItem.get("PrimaryAbilityResource"));
            
            JsonObject stats = new JsonObject();
            stats.add("hp", champItem.get("baseHP"));
            stats.add("mana", manaContainer.has("arBase") ? manaContainer.get("arBase").getAsJsonPrimitive() : new JsonPrimitive(100));
            stats.add("damage", champItem.get("BaseDamage"));
            stats.add("armor", champItem.get("baseArmor"));
            stats.add("magicResist", champItem.get("baseSpellBlock"));
            stats.add("critMultiplier", champItem.get("critDamageMultiplier"));
            stats.add("attackSpeed", champItem.get("AttackSpeed"));
            stats.add("range", new JsonPrimitive(champItem.get("attackRange").getAsInt() / 180));
            
            String spellName = champItem.getAsJsonArray("spellNames").get(0).getAsString();
            if (spellName.contains("/"))
            {
                spellName = spellName.substring(spellName.indexOf('/') + 1);
            }
            
            elem = createJsonObject.apply(data, spellDataContainer);
            JsonArray elems = getFirstChildArray.apply(elem);
            
            JsonArray abilityVars = new JsonArray();
            for (JsonElement elemm : elems)
            {
                JsonObject spellContainerData = getFirstChildObject.apply(elemm);
                if (spellContainerData.get("mScriptName").getAsString().equals(spellName))
                {
                    JsonObject spellData = getFirstChildObject.apply(spellContainerData.getAsJsonObject("mSpell"));
                    JsonArray  variables = getKeyOrDefaultArray.apply(spellData, "mDataValues");
                    for (JsonElement variable : variables)
                    {
                        JsonObject entry = getFirstChildObject.apply(variable);
                        
                        JsonObject currentVar = new JsonObject();
                        currentVar.add("key", entry.get("mName"));
                        currentVar.add("initialValue", getKeyOrDefaultInt.apply(entry, initialSpellValue));
                        currentVar.add("nextValue", getKeyOrDefaultStringP.apply(entry, nextSpellValue));
                        abilityVars.add(currentVar);
                    }
                    break;
                }
            }
            abilities.add("variables", abilityVars);
            
            
            o.add("stats", stats);
            o.add("traits", traitArray);
            o.add("ability", abilities);
            champData.add(o);
        }
        
        String itemContainerKey       = "D186C31A";
        String itemFromKey            = "mComposition";
        String itemEffectContainer    = "C13D6D31";
        String itemDescription        = "765F18DA";
        String itemEffectVarContainer = "62FF42F4";
        String itemIcon               = "8BEE2972";
        
        Map<String, Integer> itemLookup = new HashMap<>();
        
        JsonArray items = shipping.getAsJsonObject().getAsJsonArray(itemContainerKey);
        for (JsonElement itemContainer : items)
        {
            String     hashKey = getFirstChildKey.apply(itemContainer);
            JsonObject item    = getFirstChildObject.apply(itemContainer);
            
            String mName = item.get("mName").getAsString();
            
            if (mName.contains("Template") || mName.equals("TFT_Item_Null"))
            {
                continue;
            }
            
            itemLookup.put(hashKey, item.get("mID").getAsInt());
            
            JsonObject o = new JsonObject();
            o.add("id", item.get("mID"));
            o.add("name", new JsonPrimitive(getFromMapOrDefault.apply(descs).apply(item).apply(displayName)));
            o.add("desc", new JsonPrimitive(getFromMapOrDefault.apply(descs).apply(item).apply(itemDescription)));
            o.add("icon", new JsonPrimitive(item.get(itemIcon).getAsString()));
            o.add("from", getKeyOrDefaultArray.apply(item, itemFromKey));
            
            JsonArray effects    = new JsonArray();
            JsonArray effectJson = getKeyOrDefaultArray.apply(item, itemEffectContainer);
            for (JsonElement effect : effectJson)
            {
                JsonObject inner = effect.getAsJsonObject().getAsJsonObject(itemEffectVarContainer);
                
                Long          name       = inner.get("name").getAsLong();
                String        hashedName = HashHandler.toHex(name, 8, 8);
                JsonPrimitive realName   = new JsonPrimitive(HashHandler.getBinHashes().getOrDefault(hashedName, hashedName));
                JsonElement   value      = inner.get("value");
                
                JsonObject temp = new JsonObject();
                temp.add("name", realName);
                temp.add("value", value);
                effects.add(temp);
            }
            
            o.add("effects", effects);
            itemData.add(o);
        }
        
        for (JsonElement it : itemData)
        {
            JsonArray from    = it.getAsJsonObject().getAsJsonArray("from");
            JsonArray newFrom = new JsonArray();
            for (JsonElement element : from)
            {
                newFrom.add(itemLookup.get(element.getAsString()));
            }
            it.getAsJsonObject().add("from", newFrom);
        }
        
        
        // sort items by id
        NaturalOrderComparator noc   = new NaturalOrderComparator();
        List<JsonElement>      elems = new ArrayList<>();
        for (JsonElement datum : itemData)
        {
            elems.add(datum);
        }
        elems.sort(Comparator.comparingInt(a -> a.getAsJsonObject().get("id").getAsInt()));
        itemData = new JsonArray();
        elems.forEach(itemData::add);
        
        JsonObject obj = new JsonObject();
        obj.add("champions", champData);
        obj.add("traits", traitData);
        obj.add("items", itemData);
        String data = UtilHandler.getGson().toJson(UtilHandler.getJsonParser().parse(obj.toString()));
        
        Files.write(UtilHandler.CDRAGON_FOLDER.resolve("TFT.json"), data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    @Test
    public void testBinHashBrute() throws IOException
    {
        Path binhash = UtilHandler.CDRAGON_FOLDER.resolve("binHashUnknown.txt");
        
        List<String> possible = Files.readAllLines(binhash);
        List<Long>   asLong   = possible.stream().map(s -> "0x" + s).map(Long::decode).collect(Collectors.toList());
        HashHandler.bruteForceHash(HashHandler::computeBINHash, asLong);
    }
    
    @Test
    public void testBinHashBruteWords() throws IOException
    {
        Path         binhash  = UtilHandler.CDRAGON_FOLDER.resolve("binHashUnknown.txt");
        List<String> possible = Files.readAllLines(binhash);
        List<Long>   asLong   = possible.stream().map(s -> "0x" + s).map(Long::decode).collect(Collectors.toList());
        
        List<String> words = new ArrayList<>(UtilHandler.dictionary);
        words.add(0, "m");
        
        HashHandler.bruteForceHash(HashHandler::computeBINHash, asLong, words, "bruteforceWords.txt", false);
    }
    
    @Test
    public void testBinHashFromDesc() throws IOException
    {
        Path fontConfig = UtilHandler.CDRAGON_FOLDER.resolve("pbe\\data\\menu\\fontconfig_en_us.txt");
        
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
        
        Path         binhash  = UtilHandler.CDRAGON_FOLDER.resolve("binHashUnknown.txt");
        List<String> possible = Files.readAllLines(binhash);
        
        descs.values().forEach(v -> {
            String[] parts = v.split("@");
            for (int i = 1; i < parts.length; i += 2)
            {
                String toHash = parts[i];
                
                if (toHash.contains("*"))
                {
                    toHash = toHash.substring(0, toHash.indexOf('*'));
                }
                
                String hashList = HashHandler.getBINHash(toHash);
                String output   = HashHandler.toHex(HashHandler.computeBINHash(toHash), 8);
                if (!toHash.equalsIgnoreCase(hashList))
                {
                    if (possible.contains(output))
                    {
                        String formatted = String.format("\"%s\":\"%s\",", output, toHash);
                        System.out.println(formatted);
                    }
                }
            }
        });
        
    }
    
    @Test
    public void testBinHashSingle()
    {
        String toHash = "mRecTranslator ";
        String output = HashHandler.toHex(HashHandler.computeBINHash(toHash), 8);
        System.out.println(output);
    }
    
    @Test
    public void testBinHashFromFile() throws IOException
    {
        List<String> unknowns = Files.readAllLines(UtilHandler.CDRAGON_FOLDER.resolve("binHashUnknown.txt"));
        
        Path        binhash  = UtilHandler.CDRAGON_FOLDER.resolve("bruteforceWords.txt");
        Set<String> possible = new HashSet<>(Files.readAllLines(binhash));
        Map<String, String> hashed = possible.stream()
                                             .filter(k -> !HashHandler.getBINHash(k).equalsIgnoreCase(k))
                                             .collect(Collectors.toMap(k -> k, HashHandler::getBINHash));
        
        hashed.forEach((key, value) -> {
            if (unknowns.contains(value))
            {
                String formatted = String.format("\"%s\":\"%s\",", value, key);
                System.out.println(formatted);
            }
        });
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
    
    @Test
    public void readProcessMemory()
    {
        String handleName = "League of Legends.exe";
        //String handleName = "notepad++.exe";
        MemoryHandler.readProcessMemory(handleName);
    }
}

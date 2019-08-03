package types.util;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.types.wad.data.content.WADContentHeaderV1;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.Pair;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;
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
    
    /**
     * Only has the data from your local install, meaning not all languages etc..
     */
    @Test
    public void extractTFTData() throws IOException
    {
        Path leagueInstallFolder = Paths.get("C:\\Riot Games\\League of Legends");
        Path outputFolder        = Paths.get("C:\\Users\\Steffen\\Desktop\\tftdata");
        
        WADParser parser    = new WADParser();
        Pattern   filenames = Pattern.compile("data/characters/(.*)/\\1\\.bin");
        
        Files.walkFileTree(leagueInstallFolder, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                String pathName = UtilHandler.pathToFilename(file);
                
                if (file.toString().endsWith("Maps\\Shipping\\Map22.wad.client"))
                {
                    System.out.println("Extracting TFT constants from: " + file.toString());
                    
                    WADFile map = parser.parse(file);
                    for (WADContentHeaderV1 header : map.getContentHeaders())
                    {
                        String filename = HashHandler.getWadHash(header.getPathHash());
                        if (filename.contains("map22.bin") || filenames.matcher(filename).find())
                        {
                            map.saveFile(header, outputFolder, pathName);
                        }
                    }
                }
                
                if (file.toString().contains("Localized\\Global."))
                {
                    System.out.println("Extracting locales from: " + file.toString());
                    
                    WADFile map = parser.parse(file);
                    for (WADContentHeaderV1 header : map.getContentHeaders())
                    {
                        String filename = HashHandler.getWadHash(header.getPathHash());
                        if (filename.contains("fontconfig"))
                        {
                            map.saveFile(header, outputFolder, UtilHandler.pathToFilename(file));
                        }
                    }
                }
                
                if (file.toString().contains("Champions\\"))
                {
                    if (pathName.contains("_"))
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    System.out.println("Extracting champion constants from: " + file.toString());
                    
                    WADFile map = parser.parse(file);
                    for (WADContentHeaderV1 header : map.getContentHeaders())
                    {
                        String filename = HashHandler.getWadHash(header.getPathHash());
                        if (filenames.matcher(filename).find())
                        {
                            map.saveFile(header, outputFolder, pathName);
                        }
                    }
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        Files.walkFileTree(outputFolder, new SimpleFileVisitor<>()
        {
            BINParser parser = new BINParser();
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (file.toString().endsWith(".bin"))
                {
                    String json = parser.parse(file).toJson();
                    Files.write(file.resolveSibling(UtilHandler.pathToFilename(file) + ".json"), json.getBytes(StandardCharsets.UTF_8));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
    }
    
    @Test
    public void buildTFTDataFiles() throws IOException
    {
        Path inputFolder = Paths.get("D:\\pbe");
        //Path inputFolder  = Paths.get("C:\\Users\\Steffen\\Desktop\\tftdata");
        Path outputFolder = Paths.get("C:\\Users\\Steffen\\Desktop\\tftdata");
        
        Path traitFile       = inputFolder.resolve("data\\maps\\shipping\\map22\\map22.bin");
        Path fontConfig      = inputFolder.resolve("data\\menu");
        Path champFileParent = inputFolder.resolve("data\\characters");
        
        
        Function<JsonElement, String>      getFirstChildKey     = obj -> obj.getAsJsonObject().keySet().toArray(String[]::new)[0];
        Function<JsonElement, JsonElement> getFirstChildElement = obj -> obj.getAsJsonObject().get(getFirstChildKey.apply(obj));
        Function<JsonElement, JsonObject>  getFirstChildObject  = obj -> getFirstChildElement.apply(obj).getAsJsonObject();
        Function<JsonElement, JsonArray>   getFirstChildArray   = obj -> getFirstChildElement.apply(obj).getAsJsonArray();
        
        Function<String, Boolean> isFloat = obj -> {
            try
            {
                Float.parseFloat(obj);
                return true;
            } catch (Exception e)
            {
                return false;
            }
        };
        
        
        BiFunction<JsonObject, String, JsonArray>     getKeyOrDefaultArray   = (obj, key) -> obj.has(key) ? obj.getAsJsonArray(key) : new JsonArray();
        BiFunction<JsonObject, String, JsonPrimitive> getKeyOrDefaultInt     = (obj, key) -> obj.has(key) ? obj.get(key).getAsJsonPrimitive() : new JsonPrimitive(0);
        BiFunction<JsonObject, String, JsonPrimitive> getKeyOrDefaultStringP = (obj, key) -> obj.has(key) ? obj.get(key).getAsJsonPrimitive() : new JsonPrimitive("P");
        
        BiFunction<String, String, JsonPrimitive> adjustBasedOnFormula = (formula, P) -> {
            
            // assume that all formulas are in the format "X+Y"
            
            String compute = formula.replace("P", P);
            if (!compute.contains("+"))
            {
                if (isFloat.apply(compute))
                {
                    return new JsonPrimitive(Float.parseFloat(compute));
                }
                
                return new JsonPrimitive(compute);
            }
            
            String[] parts = compute.split("\\+");
            float    A     = Float.parseFloat(parts[0]);
            float    B     = Float.parseFloat(parts[1]);
            return new JsonPrimitive(A + B);
        };
        
        BiFunction<String, String, JsonObject> createJsonObject = (data, key) -> {
            String     realData = "{" + data.substring(data.indexOf(key) - 1);
            JsonReader reader   = new JsonReader(new StringReader(realData));
            reader.setLenient(true);
            return UtilHandler.getJsonParser().parse(reader).getAsJsonObject();
        };
        
        Map<String, Map<String, String>> descs = new HashMap<>();
        
        Files.walk(fontConfig).filter(p -> p.toString().contains("fontconfig")).forEach(p -> {
            
            try
            {
                Map<String, String> desc = Files.readAllLines(p)
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
                
                descs.put(UtilHandler.pathToFilename(p).substring("fontconfig_".length()), desc);
                
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
        
        
        Map<Integer, JsonObject> champData = new TreeMap<>();
        Map<Integer, JsonObject> itemData  = new TreeMap<>();
        List<JsonObject>         traitData = new ArrayList<>();
        
        
        BINParser parser = new BINParser();
        
        String displayName = "C3143D66";
        
        String traitContainerKey       = "6F870247";
        String traitDescription        = "765F18DA";
        String traitIcon               = "mIconPath";
        String traitEffectContainter   = "mTraitsSets";
        String innerEffectContainer    = "C130C1E5";
        String traitMinUnits           = "mMinUnits";
        String traitMaxUnits           = "mMaxUnits";
        String traitEffectVarContainer = "EffectAmounts";
        String traitEffectVar          = "TftEffectAmount";
        
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
            
            traitLookup.put(hashKey, mName);
            
            JsonObject o = new JsonObject();
            o.add("name", trait.get(displayName));
            o.add("desc", trait.get(traitDescription));
            o.add("icon", trait.get(traitIcon));
            
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
                    JsonObject var  = vars.getAsJsonObject().getAsJsonObject(traitEffectVar);
                    String     name = var.get("name").getAsString();
                    if (name.startsWith("STRING_HASH: "))
                    {
                        name = name.substring("STRING_HASH: ".length());
                    }
                    JsonElement value = var.get("value");
                    
                    JsonObject temp = new JsonObject();
                    temp.add("name", new JsonPrimitive(name));
                    temp.add("value", value);
                    varAdded.add(temp);
                }
                
                adder.add("minUnits", new JsonPrimitive(inner.get(traitMinUnits).getAsInt()));
                adder.add("maxUnits", new JsonPrimitive(inner.getAsJsonArray(traitMaxUnits).get(0).getAsInt()));
                adder.add("vars", varAdded);
                
                effects.add(adder);
            }
            
            o.add("effects", effects);
            traitData.add(o);
        }
        
        String champFileTraitContainer = "mLinkedTraits";
        String champContainerKey       = "E52B8F5D";
        String costModifier            = "mRarity";
        String champAbilityName        = "87A69A5E";
        String champSplash             = "mIconPath";
        String champAbilityDesc        = "BC4F18B3";
        String champAbilityIcon        = "mPortraitIconPath";
        
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
            String     championId  = realChamp.getAsJsonObject("characterToolData").getAsJsonObject("characterToolData").get("championId").getAsString();
            
            o.add("name", champ.get(displayName));
            o.add("cost", new JsonPrimitive(1 + (champ.has(costModifier) ? champ.get(costModifier).getAsInt() : 0)));
            o.add("splash", champ.get(champSplash));
            
            abilities.add("name", champ.get(champAbilityName));
            abilities.add("desc", champ.get(champAbilityDesc));
            abilities.add("icon", champ.get(champAbilityIcon));
            
            
            String champDataContainer = "304496F1";
            String spellDataContainer = "SpellObject";
            String traitContainer     = "mLinkedTraits";
            
            String initialSpellValue = "mBaseP";
            Path   selfBin           = champFileParent.resolve(mName).resolve(mName + ".bin");
            String data              = parser.parse(selfBin).toJson();
            
            JsonObject elem               = createJsonObject.apply(data, champDataContainer);
            JsonObject champContainerData = getFirstChildObject.apply(elem);
            JsonObject champItem          = getFirstChildObject.apply(champContainerData);
            
            JsonArray traitDatas = champItem.getAsJsonArray(traitContainer);
            JsonArray traitArray = new JsonArray();
            for (JsonElement traitDatum : traitDatas)
            {
                String hash = traitDatum.getAsString().substring("LINK_OFFSET: ".length());
                traitArray.add(new JsonPrimitive(traitLookup.get(hash)));
            }
            
            JsonObject manaContainer = getFirstChildObject.apply(champItem.get("PrimaryAbilityResource"));
            
            JsonObject stats = new JsonObject();
            stats.add("hp", champItem.get("baseHP"));
            stats.add("hpScaleFactor", new JsonPrimitive(1.8f));
            stats.add("mana", manaContainer.has("arBase") ? manaContainer.get("arBase").getAsJsonPrimitive() : new JsonPrimitive(100));
            stats.add("initalMana", champItem.has("57C662CB") ? champItem.get("57C662CB") : new JsonPrimitive(0));
            stats.add("damage", champItem.get("BaseDamage"));
            stats.add("damageScaleFactor", new JsonPrimitive(1.25f));
            stats.add("armor", champItem.get("baseArmor"));
            stats.add("magicResist", champItem.get("baseSpellBlock"));
            stats.add("critMultiplier", champItem.get("critDamageMultiplier"));
            stats.add("critChance", new JsonPrimitive(0.25f));
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
                    JsonObject spellData       = getFirstChildObject.apply(spellContainerData.getAsJsonObject("mSpell"));
                    JsonArray  spellDataValues = getKeyOrDefaultArray.apply(spellData, "mDataValues");
                    for (JsonElement variable : spellDataValues)
                    {
                        JsonObject spellDataEntry = variable.getAsJsonObject().getAsJsonObject("SpellDataValue");
                        
                        JsonObject currentVar = new JsonObject();
                        currentVar.add("key", spellDataEntry.get("mName"));
                        currentVar.add("values", spellDataEntry.getAsJsonArray("mValues"));
                        abilityVars.add(currentVar);
                    }
                    break;
                }
            }
            abilities.add("variables", abilityVars);
            
            
            o.add("stats", stats);
            o.add("traits", traitArray);
            o.add("ability", abilities);
            champData.put(Integer.valueOf(championId), o);
        }
        
        String itemContainerKey       = "D186C31A";
        String itemFromKey            = "mComposition";
        String itemEffectContainer    = "EffectAmounts";
        String itemDescription        = "765F18DA";
        String itemEffectVarContainer = "TftEffectAmount";
        String itemIcon               = "mIconPath";
        
        Map<String, String> itemLookup = new HashMap<>();
        
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
            
            String mId = item.get("mID").getAsString();
            itemLookup.put(hashKey, mId);
            
            JsonObject o = new JsonObject();
            o.add("name", item.get(displayName));
            o.add("desc", item.get(itemDescription));
            o.add("icon", item.get(itemIcon));
            
            JsonArray fromOther = getKeyOrDefaultArray.apply(item, itemFromKey);
            JsonArray fromReal  = new JsonArray();
            fromOther.forEach(f -> fromReal.add(f.getAsString().substring("LINK_OFFSET: ".length())));
            o.add("from", fromReal);
            
            JsonArray effects    = new JsonArray();
            JsonArray effectJson = getKeyOrDefaultArray.apply(item, itemEffectContainer);
            for (JsonElement effect : effectJson)
            {
                JsonObject inner = effect.getAsJsonObject().getAsJsonObject(itemEffectVarContainer);
                
                String name = inner.get("name").getAsString();
                if (name.startsWith("STRING_HASH: "))
                {
                    name = name.substring("STRING_HASH: ".length());
                }
                JsonElement value = inner.get("value");
                
                JsonObject temp = new JsonObject();
                temp.add("name", new JsonPrimitive(name));
                temp.add("value", value);
                effects.add(temp);
            }
            
            o.add("effects", effects);
            itemData.put(Integer.valueOf(mId), o);
        }
        
        for (Integer key : itemData.keySet())
        {
            JsonObject fromObject = itemData.get(key).getAsJsonObject();
            JsonArray  from       = fromObject.getAsJsonArray("from");
            JsonArray  newFrom    = new JsonArray();
            for (JsonElement element : from)
            {
                newFrom.add(itemLookup.get(element.getAsString()));
            }
            fromObject.add("from", newFrom);
        }
        
        JsonObject obj = new JsonObject();
        obj.add("champions", UtilHandler.getGson().toJsonTree(champData));
        obj.add("traits", UtilHandler.getGson().toJsonTree(traitData));
        obj.add("items", UtilHandler.getGson().toJsonTree(itemData));
        String data = UtilHandler.getGson().toJson(UtilHandler.getJsonParser().parse(obj.toString()));
        
        try
        {
            Files.createDirectories(outputFolder.resolve("TFT"));
            Files.write(outputFolder.resolve("TFT").resolve("template_TFT.json"), data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            descs.forEach((lang, vals) -> {
                try
                {
                    System.out.println("Generating files for " + lang);
                    
                    final String[] alteredData = {data};
                    vals.forEach((k, v) -> alteredData[0] = alteredData[0].replace(k, v));
                    Files.write(outputFolder.resolve("TFT").resolve(lang + "_TFT.json"), alteredData[0].getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
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
        
        Map<String, Map<String, String>> descs = new HashMap<>();
        
        Files.walk(fontConfig).filter(p -> p.toString().contains("fontconfig")).forEach(p -> {
            try
            {
                Map<String, String> desc = Files.readAllLines(p)
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
                
                descs.put(UtilHandler.pathToFilename(p).substring("fontconfig_".length()), desc);
                
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
        
        Path                      binhash  = UtilHandler.CDRAGON_FOLDER.resolve("binHashUnknown.txt");
        List<String>              possible = Files.readAllLines(binhash);
        Set<Pair<String, String>> saveme   = new HashSet<>();
        descs.values().forEach(d -> d.values().forEach(v -> {
            String[] parts = v.split("@");
            for (int i = 1; i < parts.length; i += 2)
            {
                String toHash = parts[i];
                
                if (toHash.contains("*"))
                {
                    toHash = toHash.substring(0, toHash.indexOf('*'));
                }
                
                String possibleHash = HashHandler.getBINHash(toHash);
                String hexHash      = HashHandler.toHex(HashHandler.computeBINHash(toHash), 8);
                if (hexHash.equalsIgnoreCase(possibleHash))
                {
                    if (possible.contains(hexHash))
                    {
                        String formatted = String.format("\"%s\":\"%s\",", hexHash, toHash);
                        saveme.add(new Pair<>(hexHash, toHash));
                    }
                }
            }
        }));
        saveme.forEach(a -> System.out.println(a.toJson()));
        
    }
    
    @Test
    public void testBinHashSingle()
    {
        String toHash = "APValue";
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

package types.util;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.types.wad.data.content.WADContentHeaderV1;
import no.stelar7.cdragon.util.handlers.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestTFTData
{
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
        // only set this to true if you have exported the images aswell!
        boolean exportImages = true;
        
        Path inputFolder = Paths.get("D:\\pbe");
        //Path inputFolder  = Paths.get("C:\\Users\\Steffen\\Desktop\\tftdata");
        Path outputFolder = Paths.get("C:\\Users\\Steffen\\Desktop\\tftdata");
        
        Path traitFile       = inputFolder.resolve("data\\maps\\shipping\\map22\\map22.bin");
        Path fontConfig      = inputFolder.resolve("data\\menu");
        Path champFileParent = inputFolder.resolve("data\\characters");
        
        Map<String, Map<String, String>> descs         = new HashMap<>();
        Function<String, String>         replaceDDSPNG = input -> UtilHandler.replaceEnding(input, "dds", "png");
        
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
        
        
        Map<Integer, Map<String, Object>> champData = new TreeMap<>();
        Map<Integer, Map<String, Object>> itemData  = new TreeMap<>();
        List<Map<String, Object>>         traitData = new ArrayList<>();
        
        
        BINParser parser = new BINParser();
        
        Map<String, String> traitLookup = new LinkedHashMap<>();
        
        BINFile        map22  = parser.parse(traitFile);
        List<BINEntry> traits = map22.getByType("TftTraitData");
        for (BINEntry trait : traits)
        {
            String mName = (String) trait.getIfPresent("mName").getValue();
            
            if (mName.contains("Template"))
            {
                continue;
            }
            
            traitLookup.put(trait.getHash(), mName);
            
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("name", trait.getIfPresent("C3143D66").getValue());
            o.put("desc", trait.getIfPresent("765F18DA").getValue());
            o.put("icon", replaceDDSPNG.apply((String) trait.getIfPresent("mIconPath").getValue()));
            
            List<Map<String, Object>> effects = new ArrayList<>();
            
            List<Object> traitEffectContainer = ((BINContainer) trait.getIfPresent("mTraitsSets").getValue()).getData();
            for (Object effectObj : traitEffectContainer)
            {
                BINStruct effect = (BINStruct) effectObj;
                
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("minUnits", effect.getIfPresent("mMinUnits").getValue());
                data.put("maxUnits", ((BINData) effect.getIfPresent("mMaxUnits").getValue()).getData().get(0));
                
                
                List<Map<String, Object>> vars          = new ArrayList<>();
                Optional<BINValue>        effectVarsObj = effect.get("EffectAmounts");
                if (effectVarsObj.isPresent())
                {
                    List<Object> effectVars = ((BINContainer) effect.getIfPresent("EffectAmounts").getValue()).getData();
                    for (Object var : effectVars)
                    {
                        BINStruct           effectVar = (BINStruct) var;
                        Map<String, Object> inner     = new LinkedHashMap<>();
                        
                        for (BINValue value : ((BINStruct) var).getData())
                        {
                            if (value.getType() == BINValueType.STRING_HASH)
                            {
                                String key = (String) value.getValue();
                                inner.put(value.getHash(), HashHandler.getBinHashes().getOrDefault(key, key));
                            } else
                            {
                                inner.put(value.getHash(), value.getValue());
                            }
                        }
                        
                        vars.add(inner);
                    }
                }
                
                data.put("vars", vars);
                effects.add(data);
            }
            
            o.put("effects", effects);
            traitData.add(o);
        }
        
        
        List<BINEntry> champs = map22.getByType("TftShopData");
        for (BINEntry champ : champs)
        {
            String mName = (String) champ.getIfPresent("mName").getValue();
            
            if (!mName.startsWith("TFT_") || mName.equals("TFT_Template"))
            {
                continue;
            }
            
            Map<String, Object> champion  = new LinkedHashMap<>();
            Map<String, Object> abilities = new LinkedHashMap<>();
            
            String  realName    = mName.substring(4);
            Path    selfRealBin = champFileParent.resolve(realName).resolve(realName + ".bin");
            BINFile realData    = parser.parse(selfRealBin);
            int     id          = (int) ((BINStruct) realData.getByType("CharacterRecord").get(0).getIfPresent("characterToolData").getValue()).getIfPresent("championId").getValue();
            
            champion.put("name", champ.getIfPresent("C3143D66").getValue());
            champion.put("id", id);
            champion.put("cost", 1 + champ.get("mRarity").map(BINValue::getValue).map(a -> (byte) a).orElse((byte) 0));
            champion.put("splash", replaceDDSPNG.apply((String) champ.getIfPresent("mIconPath").getValue()));
            
            abilities.put("name", champ.getIfPresent("87A69A5E").getValue());
            abilities.put("desc", champ.getIfPresent("BC4F18B3").getValue());
            abilities.put("icon", replaceDDSPNG.apply((String) champ.getIfPresent("mPortraitIconPath").getValue()));
            
            
            String  initialSpellValue = "mBaseP";
            Path    selfBin           = champFileParent.resolve(mName).resolve(mName + ".bin");
            BINFile data              = parser.parse(selfBin);
            
            BINEntry champItem = data.getByType("TFTCharacterRecord").get(0);
            
            List<String> traitArray  = new ArrayList<>();
            List<Object> champTraits = ((BINContainer) champItem.getIfPresent("mLinkedTraits").getValue()).getData();
            for (Object traitObj : champTraits)
            {
                BINStruct trait = (BINStruct) traitObj;
                String    key   = (String) trait.getIfPresent("053A1F33").getValue();
                traitArray.add(traitLookup.get(key));
            }
            
            BINStruct manaContainer = (BINStruct) champItem.getIfPresent("PrimaryAbilityResource").getValue();
            
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("hp", champItem.getIfPresent("baseHP").getValue());
            stats.put("hpScaleFactor", 1.8f);
            stats.put("mana", manaContainer.get("arBase").map(a -> (float) a.getValue()).orElse(100f));
            stats.put("initalMana", champItem.get("mInitialMana").map(a -> (float) a.getValue()).orElse(0f));
            stats.put("damage", champItem.getIfPresent("BaseDamage").getValue());
            stats.put("damageScaleFactor", 1.25f);
            stats.put("armor", champItem.getIfPresent("baseArmor").getValue());
            stats.put("magicResist", champItem.getIfPresent("baseSpellBlock").getValue());
            stats.put("critMultiplier", champItem.getIfPresent("critDamageMultiplier").getValue());
            stats.put("critChance", champItem.getIfPresent("BaseCritChance").getValue());
            stats.put("attackSpeed", champItem.getIfPresent("AttackSpeed").getValue());
            stats.put("range", champItem.get("attackRange").map(a -> Math.floor((float) a.getValue() / 180f)).get());
            
            String spellName = (String) ((BINContainer) champItem.getIfPresent("spellNames").getValue()).getData().get(0);
            if (spellName.contains("/"))
            {
                spellName = spellName.substring(spellName.indexOf('/') + 1);
            }
            
            String finalSpellName = spellName;
            
            List<Map<String, Object>> abilityVars = new ArrayList<>();
            List<BINEntry>            elems       = data.getByType("SpellObject");
            for (BINEntry elem : elems)
            {
                if (elem.getIfPresent("mScriptName").getValue().equals(spellName))
                {
                    BINStruct    spell      = (BINStruct) elem.getIfPresent("mSpell").getValue();
                    List<Object> dataValues = ((BINContainer) spell.getIfPresent("mDataValues").getValue()).getData();
                    for (Object variableObj : dataValues)
                    {
                        BINStruct variable = (BINStruct) variableObj;
                        
                        
                        Map<String, Object> currentVar = new LinkedHashMap<>();
                        currentVar.put("key", variable.getIfPresent("mName").getValue());
                        
                        Optional<BINValue> valuesContainer = variable.get("mValues");
                        if (valuesContainer.isPresent())
                        {
                            BINContainer values = (BINContainer) variable.getIfPresent("mValues").getValue();
                            currentVar.put("values", values.getData());
                        }
                        
                        abilityVars.add(currentVar);
                    }
                    break;
                }
            }
            abilities.put("variables", abilityVars);
            
            
            champion.put("stats", stats);
            champion.put("traits", traitArray);
            champion.put("ability", abilities);
            champData.put((Integer) champion.get("id"), champion);
            
        }
        
        Map<String, Object> itemLookup = new LinkedHashMap<>();
        
        List<BINEntry> items = map22.getByType("TftItemData");
        for (BINEntry item : items)
        {
            String mName = (String) item.getIfPresent("mName").getValue();
            
            if (mName.contains("Template") || mName.equals("TFT_Item_Null"))
            {
                continue;
            }
            
            int mId = (int) item.getIfPresent("mID").getValue();
            itemLookup.put(item.getHash(), mId);
            
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("name", item.getIfPresent("C3143D66").getValue());
            o.put("desc", item.getIfPresent("765F18DA").getValue());
            o.put("icon", replaceDDSPNG.apply((String) item.getIfPresent("mIconPath").getValue()));
            
            Optional<BINValue> fromCont = item.get("mComposition");
            o.put("from", fromCont.map(a -> ((BINContainer) a.getValue()).getData()).orElse(new ArrayList<>()));
            
            
            List<Map<String, Object>> effects = new ArrayList<>();
            
            Optional<BINValue> effectCont = item.get("EffectAmounts");
            if (effectCont.isPresent())
            {
                List<Object> effectContainer = ((BINContainer) effectCont.get().getValue()).getData();
                for (Object effectObj : effectContainer)
                {
                    BINStruct effect = (BINStruct) effectObj;
                    
                    BINValue nameVal = effect.getIfPresent("name");
                    String   nameKey = (String) nameVal.getValue();
                    String   name    = nameKey;
                    if (nameVal.getType() == BINValueType.STRING_HASH)
                    {
                        name = HashHandler.getBinHashes().getOrDefault(nameKey, nameKey);
                    }
                    
                    Map<String, Object> temp = new LinkedHashMap<>();
                    temp.put("name", name);
                    temp.put("value", effect.get("value").map(BINValue::getValue).orElse(null));
                    effects.add(temp);
                }
            }
            
            o.put("effects", effects);
            itemData.put(mId, o);
        }
        
        for (Integer key : itemData.keySet())
        {
            Map<String, Object> fromObject = itemData.get(key);
            List<String>        from       = (List<String>) fromObject.get("from");
            List<Integer>       newFrom    = new ArrayList<>();
            for (String element : from)
            {
                newFrom.add((int) itemLookup.get(element));
            }
            
            fromObject.put("from", newFrom);
        }
        
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("champions", champData);
        obj.put("traits", traitData);
        obj.put("items", itemData);
        String data = UtilHandler.getGson().toJson(obj);
        
        
        if (exportImages)
        {
            champData.forEach((k, v) -> {
                String splashPath  = (String) v.get("splash");
                Path   splash      = inputFolder.resolve(splashPath);
                String abilityPath = (String) ((Map<String, Object>) v.get("ability")).get("icon");
                Path   ability     = inputFolder.resolve(abilityPath);
                
                try
                {
                    Files.createDirectories(outputFolder.resolve(splashPath).getParent());
                    Files.createDirectories(outputFolder.resolve(abilityPath).getParent());
                    Files.copy(splash, outputFolder.resolve(splashPath), StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(ability, outputFolder.resolve(abilityPath), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                
            });
            
            traitData.forEach((v) -> {
                String iconPath = (String) v.get("icon");
                Path   icon     = inputFolder.resolve(iconPath);
                
                try
                {
                    Files.createDirectories(outputFolder.resolve(iconPath).getParent());
                    Files.copy(icon, outputFolder.resolve(iconPath), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
            
            itemData.forEach((k, v) -> {
                String iconPath = (String) v.get("icon");
                Path   icon     = inputFolder.resolve(iconPath);
                
                try
                {
                    Files.createDirectories(outputFolder.resolve(iconPath).getParent());
                    Files.copy(icon, outputFolder.resolve(iconPath), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
        }
        
        Files.createDirectories(outputFolder.resolve("TFT"));
        Files.write(outputFolder.resolve("TFT").resolve("template_TFT.json"), data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        descs.keySet()
             .stream()
             .sorted()
             .forEach(lang ->
                      {
                          Map<String, String> vals = descs.get(lang);
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
    }
}

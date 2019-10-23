package types.util;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.types.wad.data.content.WADContentHeaderV1;
import no.stelar7.cdragon.util.handlers.*;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;
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
    
    static class TFTSetInfo
    {
        private String       setName;
        private List<String> characters;
        
        private Map<Integer, Map<String, Object>> champData = new TreeMap<>();
        private List<Map<String, Object>>         traitData = new ArrayList<>();
        
        public TFTSetInfo(String setName, List<String> characters)
        {
            this.setName = setName;
            this.characters = characters;
        }
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
        
        BINParser parser = new BINParser();
        BINFile   map22  = parser.parse(traitFile);
        
        Map<Integer, TFTSetInfo> sets = new HashMap<>();
        
        Map<String, String> characterOffsetLookup = new HashMap<>();
        for (BINEntry character : map22.getByType("character"))
        {
            characterOffsetLookup.put(character.getHash(), character.getValue("name", "name"));
        }
        
        List<BINEntry> TFTSetList = map22.getByType("438850FF");
        for (BINEntry entry : TFTSetList)
        {
            String characterListOffsetId = (String) ((BINContainer) entry.getIfPresent("CharacterLists").getValue()).getData().get(0);
            BINMap setInfo               = (BINMap) entry.getIfPresent("D2538E5A").getValue();
            int    setNumber             = (Integer) ((BINStruct) setInfo.getIfPresent("SetNumber")).getData().get(0).getValue();
            String setName               = (String) ((BINStruct) setInfo.getIfPresent("SetName")).getData().get(0).getValue();
            
            List<String> characters = new ArrayList<>();
            for (BINEntry characterList : map22.getByType("MapCharacterList"))
            {
                if (!characterList.getHash().equalsIgnoreCase(characterListOffsetId))
                {
                    continue;
                }
                
                BINValue     cVal = characterList.getIfPresent("Characters");
                BINContainer cont = (BINContainer) cVal.getValue();
                for (Object l : cont.getData())
                {
                    String unhashedKey = HashHandler.getBinHashes().getOrDefault(String.valueOf(l), String.valueOf(l));
                    characters.add(characterOffsetLookup.get(unhashedKey));
                }
            }
            
            sets.put(setNumber, new TFTSetInfo(setName, characters));
        }
        
        Map<String, Map<String, Object>> traitLookup = new LinkedHashMap<>();
        
        List<BINEntry> traits = map22.getByType("TftTraitData");
        for (BINEntry trait : traits)
        {
            String mName = (String) trait.getIfPresent("mName").getValue();
            
            if (mName.contains("Template"))
            {
                continue;
            }
            
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("name", trait.getIfPresent("C3143D66").getValue());
            o.put("desc", trait.get("765F18DA").map(BINValue::getValue).orElse("No description key found"));
            o.put("icon", trait.get("mIconPath").map(BINValue::getValue).orElse("No image key found.dds"));
            
            List<Map<String, Object>> effects              = new ArrayList<>();
            List<Object>              traitEffectContainer = ((BINContainer) trait.getIfPresent("mTraitsSets").getValue()).getData();
            for (Object effectObj : traitEffectContainer)
            {
                BINStruct effect = (BINStruct) effectObj;
                
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("minUnits", effect.getIfPresent("mMinUnits").getValue());
                
                List<Object> mMaxUnitsList = effect.get("mMaxUnits").map(m -> ((BINData) m.getValue()).getData()).orElse(new ArrayList<>());
                if (mMaxUnitsList.size() > 0)
                {
                    data.put("maxUnits", mMaxUnitsList.get(0));
                } else
                {
                    data.put("maxUnits", 25000);
                }
                
                
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
            traitLookup.put(trait.getHash(), o);
        }
        
        
        List<BINEntry> champs = map22.getByType("TftShopData");
        for (BINEntry champ : champs)
        {
            String       mName   = (String) champ.getIfPresent("mName").getValue();
            List<String> badKeys = Arrays.asList("TFT_Template", "Sold", "SellAction", "GetXPAction", "RerollAction", "LockAction");
            if (badKeys.contains(mName))
            {
                continue;
            }
            
            Map<String, Object> champion  = new LinkedHashMap<>();
            Map<String, Object> abilities = new LinkedHashMap<>();
            
            String realName    = mName.substring(mName.lastIndexOf("_") + 1);
            Path   closestPath = findClosestSubstring(champFileParent, realName);
            
            BINFile realData = parser.parse(closestPath);
            int     id       = (int) ((BINStruct) realData.getByType("CharacterRecord").get(0).getIfPresent("characterToolData").getValue()).getIfPresent("championId").getValue();
            
            champion.put("name", champ.getIfPresent("C3143D66").getValue());
            champion.put("id", id);
            champion.put("cost", 1 + champ.get("mRarity").map(BINValue::getValue).map(a -> (byte) a).orElse((byte) 0));
            champion.put("splash", champ.getIfPresent("mIconPath").getValue());
            
            abilities.put("name", champ.get("87A69A5E").map(BINValue::getValue).orElse("No ability name key present"));
            abilities.put("desc", champ.get("BC4F18B3").map(BINValue::getValue).orElse("No ability description key present"));
            abilities.put("icon", champ.get("mPortraitIconPath").map(BINValue::getValue).orElse("No ability icon key present.png"));
            
            Path selfBin = champFileParent.resolve(mName).resolve(mName + ".bin");
            if (!Files.exists(selfBin))
            {
                continue;
            }
            
            BINFile data = parser.parse(selfBin);
            
            BINEntry champItem = data.getByType("TFTCharacterRecord").get(0);
            
            List<String> traitArray  = new ArrayList<>();
            List<Object> champTraits = ((BINContainer) champItem.getIfPresent("mLinkedTraits").getValue()).getData();
            for (Object traitObj : champTraits)
            {
                if (traitObj instanceof BINStruct)
                {
                    BINStruct trait = (BINStruct) traitObj;
                    String    key   = (String) trait.getIfPresent("053A1F33").getValue();
                    traitArray.add(key);
                } else
                {
                    traitArray.add(String.valueOf(traitObj));
                }
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
            
            for (Entry<Integer, TFTSetInfo> setEntry : sets.entrySet())
            {
                if (setEntry.getValue().characters.contains(mName))
                {
                    setEntry.getValue().champData.put(id, champion);
                }
            }
        }
        
        Map<String, Object>               itemLookup = new LinkedHashMap<>();
        Map<Integer, Map<String, Object>> itemData   = new TreeMap<>();
        List<BINEntry>                    items      = map22.getByType("TftItemData");
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
            o.put("icon", item.getIfPresent("mIconPath").getValue());
            
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
        obj.put("items", itemData);
        
        Map<Integer, Map<String, Object>> setMap = new HashMap<>();
        for (Entry<Integer, TFTSetInfo> setInfoEntry : sets.entrySet())
        {
            Map<String, Object> inf = new HashMap<>();
            inf.put("name", setInfoEntry.getValue().setName);
            
            Set<Map<String, Object>> setTraitData = new HashSet<>();
            for (Map<String, Object> champInfo : setInfoEntry.getValue().champData.values())
            {
                List<String> traitHashes   = (List<String>) champInfo.get("traits");
                List<String> renamedHashes = new ArrayList<>();
                for (String traitHash : traitHashes)
                {
                    setTraitData.add(traitLookup.get(traitHash));
                    String traitName = (String) traitLookup.get(traitHash).get("name");
                    renamedHashes.add(traitName);
                }
                champInfo.put("traits", renamedHashes);
                
            }
            inf.put("traits", setTraitData);
            
            Map<Integer, Map<String, Object>> champData = setInfoEntry.getValue().champData;
            inf.put("champions", setInfoEntry.getValue().champData);
            
            setMap.put(setInfoEntry.getKey(), inf);
        }
        
        obj.put("sets", setMap);
        String data = UtilHandler.getGson().toJson(obj);
        
        if (exportImages)
        {
            DDSParser d = new DDSParser();
            setMap.forEach((setId, setData) -> {
                Map<Integer, Map<String, Object>> champInfo = (Map<Integer, Map<String, Object>>) setData.get("champions");
                champInfo.forEach((k, v) -> {
                    String splashPath  = (String) v.get("splash");
                    Path   splash      = inputFolder.resolve(splashPath);
                    String abilityPath = (String) ((Map<String, Object>) v.get("ability")).get("icon");
                    Path   ability     = inputFolder.resolve(abilityPath);
                    
                    if (!Files.exists(splash))
                    {
                        splash = Paths.get(UtilHandler.replaceEnding(splash.toString(), "dds", "png"));
                    }
                    
                    if (!Files.exists(ability))
                    {
                        ability = Paths.get(UtilHandler.replaceEnding(ability.toString(), "dds", "png"));
                    }
                    
                    try
                    {
                        Files.createDirectories(outputFolder.resolve(splashPath).getParent());
                        if (splash.toString().endsWith(".dds"))
                        {
                            splashPath = UtilHandler.replaceEnding(splashPath, "dds", "png");
                            BufferedImage img = d.parse(splash);
                            ImageIO.write(img, "png", outputFolder.resolve(splashPath).toFile());
                        } else
                        {
                            Files.copy(splash, outputFolder.resolve(splashPath), StandardCopyOption.REPLACE_EXISTING);
                        }
                        
                        Files.createDirectories(outputFolder.resolve(abilityPath).getParent());
                        if (ability.toString().endsWith(".dds"))
                        {
                            abilityPath = UtilHandler.replaceEnding(abilityPath, "dds", "png");
                            BufferedImage img = d.parse(ability);
                            ImageIO.write(img, "png", outputFolder.resolve(abilityPath).toFile());
                        } else
                        {
                            Files.copy(ability, outputFolder.resolve(abilityPath), StandardCopyOption.REPLACE_EXISTING);
                        }
                        
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                });
                
                Set<Map<String, Object>> traitInfo = (Set<Map<String, Object>>) setData.get("traits");
                traitInfo.forEach((v) -> {
                    String iconPath = (String) v.get("icon");
                    Path   icon     = inputFolder.resolve(iconPath);
                    if (!Files.exists(icon))
                    {
                        icon = Paths.get(UtilHandler.replaceEnding(icon.toString(), "dds", "png"));
                    }
                    
                    try
                    {
                        Files.createDirectories(outputFolder.resolve(iconPath).getParent());
                        if (icon.toString().endsWith(".dds"))
                        {
                            iconPath = UtilHandler.replaceEnding(iconPath, "dds", "png");
                            BufferedImage img = d.parse(icon);
                            ImageIO.write(img, "png", outputFolder.resolve(iconPath).toFile());
                        } else
                        {
                            Files.copy(icon, outputFolder.resolve(iconPath), StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                });
            });
            
            itemData.forEach((k, v) -> {
                String iconPath = (String) v.get("icon");
                Path   icon     = inputFolder.resolve(iconPath);
                
                if (!Files.exists(icon))
                {
                    icon = Paths.get(UtilHandler.replaceEnding(icon.toString(), "dds", "png"));
                }
                
                try
                {
                    Files.createDirectories(outputFolder.resolve(iconPath).getParent());
                    if (icon.toString().endsWith(".dds"))
                    {
                        iconPath = UtilHandler.replaceEnding(iconPath, "dds", "png");
                        BufferedImage img = d.parse(icon);
                        ImageIO.write(img, "png", outputFolder.resolve(iconPath).toFile());
                    } else
                    {
                        Files.copy(icon, outputFolder.resolve(iconPath), StandardCopyOption.REPLACE_EXISTING);
                    }
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
    
    private Path findClosestSubstring(Path champFileParent, String realName)
    {
        String test = realName;
        
        while (test.length() > 0)
        {
            Path toTest = champFileParent.resolve(test).resolve(test + ".bin");
            if (Files.exists(toTest))
            {
                return toTest;
            }
            
            test = test.substring(0, test.length() - 1);
        }
        
        return null;
    }
}

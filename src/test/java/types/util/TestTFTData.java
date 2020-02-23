package types.util;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.rst.*;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.types.wad.data.content.WADContentHeaderV1;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.ByteArray;
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
        
        private List<Map<String, Object>> champData = new ArrayList<>();
        private List<Map<String, Object>> traitData = new ArrayList<>();
        
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
        
        Map<String, TranslationConfig> descs = parseTranslationMaps(fontConfig);
        
        BINParser           parser       = new BINParser();
        BINFile             map22        = parser.parse(traitFile);
        Map<String, Object> outputObject = new LinkedHashMap<>();
        
        Map<String, String>              characterOffsetLookup = parseCharacterOffsetLookup(map22);
        Map<Integer, TFTSetInfo>         setData               = parseSetInfo(map22, characterOffsetLookup);
        Map<String, Map<String, Object>> traitData             = parseTraitInfo(map22);
        parseChampionInfo(champFileParent, parser, map22, setData);
        
        Map<Integer, Map<String, Object>> outputSetMap = generateSetMap(setData, traitData, outputObject);
        Map<Integer, Map<String, Object>> itemData     = parseItemInfo(map22, outputObject);
        
        Map<String, TFTRound> roundMap     = parseRoundInfo(map22);
        Map<String, TFTStage> stageMap     = parseStageMap(map22);
        List<String>          stageOffsets = parseStageList(map22);
        
        outputObject.put("rounds", roundMap);
        outputObject.put("stages", stageMap);
        outputObject.put("stageOrder", stageOffsets);
        
        if (exportImages)
        {
            exportImages(inputFolder, outputFolder, outputSetMap, itemData);
        }
        
        String template = UtilHandler.getGson().toJson(outputObject);
        
        Files.createDirectories(outputFolder.resolve("TFT"));
        Files.write(outputFolder.resolve("TFT").resolve("template_TFT.json"), template.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        descs.keySet()
             .stream()
             .sorted()
             .forEach(lang ->
                      {
                              /*
                          try
                          {
                              todo
                              TranslationConfig vals = descs.get(lang);
                              System.out.println("Generating files for " + lang);
                
                              Map<String, Object> output = new HashMap<>(outputObject);
                              ((Map<Integer, Map<String, Object>>) output.get("sets")).forEach((k, v) -> {
                                  ((List<Map<String, Object>>) v.get("traits")).forEach(e -> {
                                      e.put("name", vals.get((String) e.get("name")));
                                      e.put("desc", vals.get((String) e.get("desc")));
                                  });
                    
                                  ((List<Map<String, Object>>) v.get("champions")).forEach(e -> {
                                      e.put("name", vals.get((String) e.get("name")));
                        
                                      List<String> clone = new ArrayList<>();
                                      ((List<String>) e.get("traits")).forEach(e2 -> clone.add(vals.get((String) e2)));
                                      e.put("traits", clone);
                        
                                      Map<String, Object> abs = (Map<String, Object>) e.get("ability");
                                      abs.forEach((k2, v2) -> {
                                          abs.put("name", vals.get((String) abs.get("name")));
                                          abs.put("desc", vals.get((String) abs.get("desc")));
                                      });
                                  });
                              });
                
                              ((Map<Integer, Map<String, Object>>) output.get("items")).forEach((k, v) -> {
                                  v.put("name", vals.get((String) v.get("name")));
                                  v.put("desc", vals.get((String) v.get("desc")));
                              });
                              System.out.println();
                
                              String alteredData = UtilHandler.getGson().toJson(output);
                              Files.write(outputFolder.resolve("TFT").resolve(lang + "_TFT.json"), alteredData.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                          } catch (IOException e)
                          {
                              e.printStackTrace();
                          }
                               */
                      });
    }
    
    private List<String> parseStageList(BINFile map22)
    {
        List<String> stages = new ArrayList<>();
        
        BINContainer stageOrderOffsetList = (BINContainer) map22.getByType("01D7548E").get(0).getIfPresent("4FF2F38F").getValue();
        stageOrderOffsetList.getData().stream().map(a -> (String) a).forEach(stages::add);
        
        return stages;
    }
    
    
    static class TFTStage
    {
        String       name;
        List<String> rounds = new ArrayList<>();
    }
    
    private Map<String, TFTStage> parseStageMap(BINFile map22)
    {
        Map<String, TFTStage> stages = new HashMap<>();
        
        map22.getByType("F737DEF9").forEach(e -> {
            TFTStage stage = new TFTStage();
            stage.name = (String) e.getIfPresent("mName").getValue();
            
            BINContainer entries = (BINContainer) e.getIfPresent("20ABF669").getValue();
            entries.getData().stream().map(a -> (String) a).forEach(a -> stage.rounds.add(a));
            stages.put(e.getHash(), stage);
        });
        
        return stages;
    }
    
    static class TFTRound
    {
        String             name;
        Map<String, Float> phases = new HashMap<>();
        String             roundType;
    }
    
    private Map<String, TFTRound> parseRoundInfo(BINFile map22)
    {
        Map<String, TFTRound> roundInfo = new HashMap<>();
        
        Map<String, String> phaseNames = new HashMap<>()
        {{
            put("0B0AE6F9", "TFT_phase_title_shopping");
            put("A35DB49D", "TFT_phase_title_arrival");
            put("FD870D28", "TFT_phase_title_overtime");
            put("F84A6A84", "TFT_phase_title_departure");
        }};
        
        map22.getByType("F4A6BB27").forEach(e -> {
            TFTRound round = new TFTRound();
            round.name = (String) e.getIfPresent("mName").getValue();
            
            
            phaseNames.keySet().forEach(p -> {
                BINStruct internal = (BINStruct) e.getIfPresent(p).getValue();
                if (internal.get("mEnabled").map(BINValue::getValue).map(a -> (boolean) a).orElse(false))
                {
                    String name     = (String) internal.get("C5E20B46").map(BINValue::getValue).orElse(phaseNames.get(p));
                    float  duration = (float) internal.getIfPresent("85F7D4C8").getValue();
                    round.phases.put(name, duration);
                }
            });
            
            BINMap typeMap = (BINMap) e.getIfPresent("78F60753").getValue();
            round.roundType = (String) typeMap.get("RoundType").map(a -> (BINStruct) a).map(a -> a.getIfPresent("mValue")).get().getValue();
            
            roundInfo.put(e.getHash(), round);
        });
        
        return roundInfo;
    }
    
    private void exportImages(Path inputFolder, Path outputFolder, Map<Integer, Map<String, Object>> outputSetMap, Map<Integer, Map<String, Object>> itemData)
    {
        DDSParser ddsParser = new DDSParser();
        
        outputSetMap.forEach((setId, setInfoData) -> {
            
            List<Map<String, Object>> champInfo = (List<Map<String, Object>>) setInfoData.get("champions");
            champInfo.forEach((v) -> {
                String splashPath = (String) v.get("splash");
                Path   splash     = inputFolder.resolve(splashPath);
                splash = renameIfNotExists(splash);
                exportAndRenameIfNeeded(outputFolder, ddsParser, splashPath, splash);
                
                String abilityPath = (String) ((Map<String, Object>) v.get("ability")).get("icon");
                Path   ability     = inputFolder.resolve(abilityPath);
                ability = renameIfNotExists(ability);
                exportAndRenameIfNeeded(outputFolder, ddsParser, abilityPath, ability);
            });
            
            List<Map<String, Object>> traitInfo = (List<Map<String, Object>>) setInfoData.get("traits");
            traitInfo.forEach((v) -> {
                String iconPath = (String) v.get("icon");
                Path   icon     = inputFolder.resolve(iconPath);
                
                icon = renameIfNotExists(icon);
                exportAndRenameIfNeeded(outputFolder, ddsParser, iconPath, icon);
            });
            
        });
        
        itemData.forEach((k, v) -> {
            String iconPath = (String) v.get("icon");
            Path   icon     = inputFolder.resolve(iconPath);
            
            icon = renameIfNotExists(icon);
            exportAndRenameIfNeeded(outputFolder, ddsParser, iconPath, icon);
        });
    }
    
    private Path renameIfNotExists(Path path)
    {
        if (!Files.exists(path))
        {
            path = Paths.get(UtilHandler.replaceEnding(path.toString(), "dds", "png"));
        }
        return path;
    }
    
    private void exportAndRenameIfNeeded(Path outputFolder, DDSParser ddsParser, String pathString, Path realPath)
    {
        try
        {
            Files.createDirectories(outputFolder.resolve(pathString).getParent());
            if (realPath.toString().endsWith(".dds"))
            {
                pathString = UtilHandler.replaceEnding(pathString, "dds", "png");
                BufferedImage img = ddsParser.parse(realPath);
                ImageIO.write(img, "png", outputFolder.resolve(pathString).toFile());
            } else
            {
                Files.copy(realPath, outputFolder.resolve(pathString), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private Map<Integer, Map<String, Object>> generateSetMap(Map<Integer, TFTSetInfo> setData, Map<String, Map<String, Object>> traitData, Map<String, Object> obj)
    {
        Map<Integer, Map<String, Object>> setMap = new HashMap<>();
        for (Entry<Integer, TFTSetInfo> setInfoEntry : setData.entrySet())
        {
            Map<String, Object> inf = new HashMap<>();
            inf.put("name", setInfoEntry.getValue().setName);
            
            Set<Map<String, Object>> setTraitData = new HashSet<>();
            for (Map<String, Object> champInfo : setInfoEntry.getValue().champData)
            {
                List<String> traitHashes   = (List<String>) champInfo.get("traits");
                List<String> renamedHashes = new ArrayList<>();
                for (String traitHash : traitHashes)
                {
                    setTraitData.add(traitData.get(traitHash));
                    String traitName = (String) traitData.get(traitHash).get("name");
                    renamedHashes.add(traitName);
                }
                champInfo.put("traits", renamedHashes);
            }
            
            List<Map<String, Object>> champions = setInfoEntry.getValue().champData;
            champions.sort(Comparator.comparing(a -> (int) a.get("id")));
            
            List<Map<String, Object>> traits = new ArrayList<>(setTraitData);
            traits.sort(Comparator.comparing(a -> (String) a.get("name")));
            
            inf.put("traits", traits);
            inf.put("champions", champions);
            setMap.put(setInfoEntry.getKey(), inf);
        }
        
        obj.put("sets", setMap);
        return setMap;
    }
    
    private Map<Integer, Map<String, Object>> parseItemInfo(BINFile map22, Map<String, Object> outputObject)
    {
        Map<String, Object>               itemLookup = new LinkedHashMap<>();
        Map<Integer, Map<String, Object>> itemData   = new TreeMap<>();
        List<BINEntry>                    items      = map22.getByType("TftItemData");
        for (BINEntry item : items)
        {
            String mName = (String) item.getIfPresent("mName").getValue();
            
            if (mName.contains("Template") || mName.equals("TFT_Item_Null"))
            {
                System.out.println("Skipping item: " + mName);
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
            
            
            Map<String, Object> effects    = new HashMap<>();
            Optional<BINValue>  effectCont = item.get("EffectAmounts");
            if (effectCont.isPresent())
            {
                List<Object> effectVars = ((BINContainer) effectCont.get().getValue()).getData();
                for (Object var : effectVars)
                {
                    BINStruct effectVar = (BINStruct) var;
                    if (effectVar.getData().size() == 0)
                    {
                        continue;
                    }
                    
                    String nameHash     = (String) effectVar.get("name").map(BINValue::getValue).orElse("null");
                    String unhashedName = HashHandler.getBinHashes().getOrDefault(nameHash, nameHash);
                    
                    Object value = effectVar.get("value").map(BINValue::getValue).orElse("null");
                    effects.put(unhashedName, value);
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
        
        outputObject.put("items", itemData);
        return itemData;
    }
    
    private void parseChampionInfo(Path champFileParent, BINParser parser, BINFile map22, Map<Integer, TFTSetInfo> sets)
    {
        List<BINEntry> champs = map22.getByType("TftShopData");
        for (BINEntry champ : champs)
        {
            String       mName   = (String) champ.getIfPresent("mName").getValue();
            List<String> badKeys = Arrays.asList("TFT_Template", "Sold", "SellAction", "GetXPAction", "RerollAction", "LockAction");
            if (badKeys.contains(mName))
            {
                System.out.println("Skipping shop item: " + mName);
                continue;
            }
            
            Map<String, Object> champion  = new LinkedHashMap<>();
            Map<String, Object> abilities = new LinkedHashMap<>();
            
            String realName    = mName.substring(mName.lastIndexOf("_") + 1);
            Path   closestPath = findClosestSubstring(champFileParent, realName);
            
            BINFile realData = parser.parse(closestPath);
            int     id       = (int) ((BINStruct) realData.getByType("CharacterRecord").get(0).getIfPresent("characterToolData").getValue()).getIfPresent("championId").getValue();
            
            champion.put("API_name", mName);
            champion.put("name", champ.getIfPresent("C3143D66").getValue());
            champion.put("id", id);
            
            int rarity    = champ.get("mRarity").map(BINValue::getValue).map(a -> ((byte) a) + 1).orElse(1);
            int increment = (int) Math.floor(rarity / 6f); // dirty hack for lux being a 7 cost
            champion.put("cost", rarity + increment);
            champion.put("splash", champ.getIfPresent("mIconPath").getValue());
            
            abilities.put("name", champ.get("87A69A5E").map(BINValue::getValue).orElse("No ability name key present"));
            abilities.put("desc", champ.get("BC4F18B3").map(BINValue::getValue).orElse("No ability description key present"));
            abilities.put("icon", champ.get("mPortraitIconPath").map(BINValue::getValue).orElse("No ability icon key present.png"));
            
            Path selfBin = champFileParent.resolve(mName).resolve(mName + ".bin");
            if (!Files.exists(selfBin))
            {
                System.out.println("Unable to find bin file for: " + selfBin);
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
            stats.put("hp", champItem.get("baseHP").map(a -> (float) a.getValue()).orElse(0f));
            stats.put("hpScaleFactor", 1.8f);
            stats.put("mana", manaContainer.get("arBase").map(a -> (float) a.getValue()).orElse(100f));
            stats.put("initalMana", champItem.get("mInitialMana").map(a -> (float) a.getValue()).orElse(0f));
            stats.put("damage", champItem.get("BaseDamage").map(a -> (float) a.getValue()).orElse(0f));
            stats.put("damageScaleFactor", 1.25f);
            stats.put("armor", champItem.get("baseArmor").map(a -> (float) a.getValue()).orElse(0f));
            stats.put("magicResist", champItem.get("baseSpellBlock").map(a -> (float) a.getValue()).orElse(0f));
            stats.put("critMultiplier", champItem.get("critDamageMultiplier").map(a -> (float) a.getValue()).orElse(0f));
            stats.put("critChance", champItem.get("BaseCritChance").map(a -> (float) a.getValue()).orElse(0.25f));
            stats.put("attackSpeed", champItem.get("AttackSpeed").map(a -> (float) a.getValue()).orElse(0f));
            stats.put("range", champItem.get("attackRange").map(a -> Math.floor((float) a.getValue() / 180f)).get());
            
            String spellName = (String) ((BINContainer) champItem.getIfPresent("spellNames").getValue()).getData().get(0);
            if (spellName.contains("/"))
            {
                spellName = spellName.substring(spellName.indexOf('/') + 1);
            }
            
            String finalSpellName = spellName;
            
            Map<String, Object> abilityVars = new LinkedHashMap<>();
            List<BINEntry>      elems       = data.getByType("SpellObject");
            for (BINEntry elem : elems)
            {
                if (elem.getIfPresent("mScriptName").getValue().equals(spellName))
                {
                    BINStruct    spell      = (BINStruct) elem.getIfPresent("mSpell").getValue();
                    List<Object> dataValues = ((BINContainer) spell.getIfPresent("mDataValues").getValue()).getData();
                    for (Object variableObj : dataValues)
                    {
                        BINStruct    variable = (BINStruct) variableObj;
                        String       key      = (String) variable.getIfPresent("mName").getValue();
                        List<Object> values   = variable.get("mValues").map(a -> (BINContainer) a.getValue()).map(BINContainer::getData).orElse(null);
                        abilityVars.put(key, values);
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
                if (setEntry.getValue().characters.contains(mName.toLowerCase()))
                {
                    setEntry.getValue().champData.add(champion);
                }
            }
        }
    }
    
    private Map<String, Map<String, Object>> parseTraitInfo(BINFile map22)
    {
        Map<String, Map<String, Object>> traitLookup = new LinkedHashMap<>();
        
        List<BINEntry> traits = map22.getByType("TftTraitData");
        for (BINEntry trait : traits)
        {
            String mName = (String) trait.getIfPresent("mName").getValue();
            
            if (mName.contains("Template"))
            {
                System.out.println("Skipping trait: " + mName);
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
                
                
                Map<String, Object> inner         = new HashMap<>();
                Optional<BINValue>  effectVarsObj = effect.get("EffectAmounts");
                if (effectVarsObj.isPresent())
                {
                    List<Object> effectVars = ((BINContainer) effect.getIfPresent("EffectAmounts").getValue()).getData();
                    for (Object var : effectVars)
                    {
                        BINStruct effectVar = (BINStruct) var;
                        if (effectVar.getData().size() == 0)
                        {
                            continue;
                        }
                        
                        String nameHash     = (String) effectVar.get("name").map(BINValue::getValue).orElse("null");
                        String unhashedName = HashHandler.getBinHashes().getOrDefault(nameHash, nameHash);
                        
                        Object value = effectVar.get("value").map(BINValue::getValue).orElse("null");
                        inner.put(unhashedName, value);
                    }
                }
                
                data.put("variables", inner);
                effects.add(data);
            }
            
            o.put("effects", effects);
            traitLookup.put(trait.getHash(), o);
        }
        
        return traitLookup;
    }
    
    private Map<Integer, TFTSetInfo> parseSetInfo(BINFile map22, Map<String, String> characterOffsetLookup)
    {
        Map<Integer, TFTSetInfo> sets       = new LinkedHashMap<>();
        List<BINEntry>           TFTSetList = map22.getByType("438850FF");
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
                    characters.add(characterOffsetLookup.get(unhashedKey).toLowerCase());
                }
            }
            
            sets.put(setNumber, new TFTSetInfo(setName, characters));
        }
        return sets;
    }
    
    private Map<String, String> parseCharacterOffsetLookup(BINFile map22)
    {
        Map<String, String> characterOffsetLookup = new HashMap<>();
        for (BINEntry character : map22.getByType("character"))
        {
            characterOffsetLookup.put(character.getHash(), character.getValue("name", "name"));
        }
        return characterOffsetLookup;
    }
    
    static class TranslationConfig
    {
        RSTFile             file;
        Map<String, String> rawData;
        
        public String get(String key)
        {
            return file != null ? file.getFromHash(key) : rawData.getOrDefault(key, key);
        }
        
    }
    
    private Map<String, TranslationConfig> parseTranslationMaps(Path fontConfig) throws IOException
    {
        Map<String, TranslationConfig> descs  = new HashMap<>();
        RSTParser                      parser = new RSTParser();
        Files.walk(fontConfig).filter(p -> p.toString().contains("fontconfig")).forEach(p -> {
            try
            {
                TranslationConfig config = new TranslationConfig();
                
                ByteArray data = ByteArray.fromFile(p);
                if (FileTypeHandler.isProbableRST(data))
                {
                    RSTFile f = parser.parse(p);
                    config.file = f;
                } else
                {
                    config.rawData = Files.readAllLines(p)
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
                }
                
                descs.put(UtilHandler.pathToFilename(p).substring("fontconfig_".length()), config);
                
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
        return descs;
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

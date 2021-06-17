package types.util;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.util.types.math.Vector2;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.IntStream;

public class TestParseChampionSpells
{
    @Test
    public void exportChampionAbilities() throws IOException
    {
        Path       base  = Paths.get("D:\\pbe\\data\\characters");
        List<Path> files = new ArrayList<>();
        
        Files.walkFileTree(base, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                if (!file.toString().endsWith(".bin"))
                {
                    return FileVisitResult.CONTINUE;
                }
                
                files.add(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            {
                if (!dir.equals(base) && !dir.getParent().equals(base))
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        BINParser parser = new BINParser();
        for (Path file : files)
        {
            System.out.println(file);
            BINFile        parsed       = parser.parse(file);
            List<BINEntry> spellObjects = parsed.getByType("SpellObject");
            for (BINEntry spellObject : spellObjects)
            {
                Optional<BINValue> maybeName = spellObject.get("mScriptName");
                if (maybeName.isEmpty())
                {
                    continue;
                }
                
                Optional<BINValue> maybeSpell = spellObject.get("mSpell");
                if (maybeSpell.isEmpty())
                {
                    continue;
                }
                
                String scriptName = (String) maybeName.get().getValue();
                System.out.println(scriptName);
                
                BINStruct spell = (BINStruct) maybeSpell.get().getValue();
                
                Optional<BINValue> calculations = spell.get("mspellcalculations");
                if (calculations.isEmpty())
                {
                    continue;
                }
                
                BINMap values = ((BINMap) calculations.get().getValue());
                for (Vector2<Object, Object> entry : values.getData())
                {
                    String key = (String) entry.getFirst();
                    System.out.println("Spell " + key + ":");
                    
                    BINStruct value = (BINStruct) entry.getSecond();
                    if (value.getHash().equalsIgnoreCase("gamecalculation"))
                    {
                        List<BINValue> parts = value.getData();
                        for (BINValue part : parts)
                        {
                            if (part.getHash().equalsIgnoreCase("mDisplayAsPercent"))
                            {
                                System.out.println("%");
                            }
                            
                            if (part.getType() != BINValueType.CONTAINER)
                            {
                                continue;
                            }
                            
                            BINContainer self = (BINContainer) part.getValue();
                            
                            for (Object partObject : self.getData())
                            {
                                System.out.println("+");
                                BINStruct partEntry = (BINStruct) partObject;
                                calculateFromType(spell, partEntry);
                            }
                        }
                    } else if (value.getHash().equalsIgnoreCase("gamecalculationmodified"))
                    {
                        Optional<BINValue> multiplier = value.get("mMultiplier");
                        if (multiplier.isEmpty())
                        {
                            throw new RuntimeException("Missing value!");
                        }
                        
                        Optional<BINValue> modifiedValue = value.get("mmodifiedgamecalculation");
                        if (modifiedValue.isEmpty())
                        {
                            throw new RuntimeException("Missing value!");
                        }
                        
                        System.out.println(modifiedValue.get().getValue());
                        System.out.println("*");
                        
                        BINStruct multi = (BINStruct) multiplier.get().getValue();
                        calculateFromType(spell, multi);
                    }
                }
            }
        }
    }
    
    private void calculateFromType(BINStruct spell, BINStruct partEntry)
    {
        switch (partEntry.getHash())
        {
            case "NamedDataValueCalculationPart":
            case "StatByNamedDataValueCalculationPart":
            {
                calculateStatByNamedVariable(spell, partEntry);
                break;
            }
            case "numbercalculationpart":
            {
                calculateByRawNumber(partEntry);
                break;
            }
            case "ByCharLevelInterpolationCalculationPart":
            {
                calculateByCharacterLevel(partEntry);
                break;
            }
            case "EffectValueCalculationPart":
            {
                calculateByEffect(spell, partEntry);
                break;
            }
            case "StatByCoefficientCalculationPart":
            {
                calculateByCoefficient(partEntry);
                break;
            }
            
            case "bycharlevelbreakpointscalculationpart":
            {
                calculateByCharacterLevelBreakpoint(partEntry);
                break;
            }
            
            case "statbysubpartcalculationpart":
            {
                calculateBySubpart(spell, partEntry);
                break;
            }
            case "BuffCounterByNamedDataValueCalculationPart":
            {
                calculateByDataValueBuff(spell, partEntry);
                break;
            }
            
            case "productofsubpartscalculationpart":
            {
                calculateByProductOfSubparts(spell, partEntry);
                break;
            }
            
            case "cooldownmultipliercalculationpart":
            {
                calculateByCooldownMultiplier();
                break;
            }
            
            case "sumofsubpartscalculationpart":
            {
                calculateBySumOfSubparts(spell, partEntry);
                break;
            }
            
            case "AbilityResourceByCoefficientCalculationPart":
            {
                calculateByCoefficient(partEntry);
                break;
            }
            
            case "bycharlevelformulacalculationpart":
            {
                calculateByCharacterLevelFormula(partEntry);
                break;
            }
            
            case "BuffCounterByCoefficientCalculationPart":
            {
                calculateByCoefficientBuff(partEntry);
                break;
            }
            
            case "subpartscaledproportionaltostat":
            {
                calculateBySubpartScaledByStat(spell, partEntry);
                break;
            }
            
            case "05ABDFAB":
            {
                calculateByWierdPatheonStat(spell, partEntry);
                break;
            }
            
            default:
                throw new RuntimeException("unknown calculation: " + partEntry.getHash());
        }
    }
    
    private void calculateByWierdPatheonStat(BINStruct spell, BINStruct partEntry)
    {
        calculateStatByNamedVariable(spell, partEntry);
        
        Optional<BINValue> maybeScaling = partEntry.get("BFE6AD01");
        if (maybeScaling.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        System.out.println("*");
        System.out.println(maybeScaling.get().getValue());
        System.out.println("%");
        
        int mStat = partEntry.getOrDefault("mStat", (byte) 0);
        if (!statTypes.containsKey(mStat))
        {
            throw new RuntimeException("Missing value! stat key: " + mStat);
        }
        
        System.out.println(statTypes.get(mStat));
    }
    
    private void calculateBySubpartScaledByStat(BINStruct spell, BINStruct partEntry)
    {
        System.out.println("(");
        calculateBySubpart(spell, partEntry);
        System.out.println(")");
        
        Optional<BINValue> maybeRatio = partEntry.get("mratio");
        if (maybeRatio.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        float ratio = (float) maybeRatio.get().getValue();
        
        System.out.println("*");
        System.out.println(ratio);
        
        int mStat = partEntry.getOrDefault("mStat", (byte) 0);
        if (!statTypes.containsKey(mStat))
        {
            throw new RuntimeException("Missing value! stat key: " + mStat);
        }
        
        System.out.println(statTypes.get(mStat));
    }
    
    private void calculateByCoefficientBuff(BINStruct partEntry)
    {
        Optional<BINValue> maybeValue = partEntry.get("mCoefficient");
        if (maybeValue.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        float coefficient = (float) maybeValue.get().getValue();
        
        Optional<BINValue> maybeBuff = partEntry.get("mBuffName");
        if (maybeBuff.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        String buff = (String) maybeBuff.get().getValue();
        
        System.out.println(coefficient + " * (stacks of " + buff + ")");
    }
    
    private void calculateByCharacterLevelFormula(BINStruct partEntry)
    {
        Optional<BINValue> maybeValues = partEntry.get("mValues");
        if (maybeValues.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        BINContainer valueContainer = (BINContainer) maybeValues.get().getValue();
        
        System.out.println(valueContainer.getData());
    }
    
    private void calculateBySumOfSubparts(BINStruct spell, BINStruct partEntry)
    {
        Optional<BINValue> maybeParts = partEntry.get("mSubParts");
        
        if (maybeParts.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        BINContainer parts = (BINContainer) maybeParts.get().getValue();
        
        System.out.println("(");
        for (Object partObject : parts.getData())
        {
            System.out.println("+");
            BINStruct part = (BINStruct) partObject;
            calculateFromType(spell, part);
        }
        
        System.out.println(")");
    }
    
    private void calculateByCooldownMultiplier()
    {
        System.out.println("1 (cooldown)");
    }
    
    private void calculateByProductOfSubparts(BINStruct spell, BINStruct partEntry)
    {
        Optional<BINValue> maybePart1 = partEntry.get("mPart1");
        Optional<BINValue> maybePart2 = partEntry.get("mPart2");
        
        if (maybePart1.isEmpty() || maybePart2.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        BINStruct part1 = (BINStruct) maybePart1.get().getValue();
        BINStruct part2 = (BINStruct) maybePart2.get().getValue();
        
        
        System.out.println("(");
        calculateFromType(spell, part1);
        System.out.println("*");
        calculateFromType(spell, part2);
        System.out.println(")");
    }
    
    private void calculateByDataValueBuff(BINStruct spell, BINStruct partEntry)
    {
        Optional<BINValue> maybeValue = partEntry.get("mDataValue");
        if (maybeValue.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        String nameKey    = (String) maybeValue.get().getValue();
        Object spellEntry = getSpellDataValueByKey(spell, nameKey);
        if (spellEntry == null)
        {
            throw new RuntimeException("Missing value!");
        }
        
        BINStruct          dataValue = (BINStruct) spellEntry;
        Optional<BINValue> maybeBase = dataValue.get("mBaseP");
        if (maybeBase.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        float perStack = (float) maybeBase.get().getValue();
        
        Optional<BINValue> maybeBuff = partEntry.get("mBuffName");
        if (maybeBuff.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        String buff = (String) maybeBuff.get().getValue();
        
        System.out.println(perStack + " per stack of " + buff);
    }
    
    private void calculateBySubpart(BINStruct spell, BINStruct partEntry)
    {
        Optional<BINValue> maybeSubPart = partEntry.get("mSubPart");
        if (maybeSubPart.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        BINStruct subPart = (BINStruct) maybeSubPart.get().getValue();
        
        calculateFromType(spell, subPart);
    }
    
    static final Map<Integer, String> statTypes = new HashMap<>()
    {{
        put(0, "AP");
        put(1, "Bonus Armor");
        put(2, "Bonus AD");
        put(3, "Bonus Attack Speed");
        put(5, "Bonus MR");
        put(6, "Bonus Move Speed");
        put(7, "Crit Chance(7)");
        put(8, "Crit Chance(8)");
        put(9, "UNKNOWN (9)");
        put(11, "Max HP");
        put(12, "UNKNOWN (12)");
        put(26, "Lethality");
    }};
    
    private void calculateByCharacterLevelBreakpoint(BINStruct partEntry)
    {
        Optional<BINValue> startMaybe = partEntry.get("mLevel1Value");
        if (startMaybe.isEmpty())
        {
            System.out.println("0");
            return;
            //throw new RuntimeException("Missing value!");
        }
        
        float start = (float) startMaybe.get().getValue();
        
        Optional<BINValue> maybeIgnorebreakpoints = partEntry.get("02DEB550");
        if (maybeIgnorebreakpoints.isPresent())
        {
            float perLevel = (float) maybeIgnorebreakpoints.get().getValue();
            
            double[] results = IntStream.rangeClosed(0, 17)
                                        .mapToDouble(i -> start + perLevel * i)
                                        .map(d -> Math.round(d * 10000.0) / 10000.0)
                                        .toArray();
            System.out.println(Arrays.toString(results));
            return;
        }
        
        List<Float>        datas            = new ArrayList<>();
        Optional<BINValue> breakpointsMaybe = partEntry.get("mBreakpoints");
        if (breakpointsMaybe.isEmpty())
        {
            Optional<BINValue> maybePerLevel = partEntry.get("02DEB550");
            if (maybePerLevel.isEmpty())
            {
                while (datas.size() < 18)
                {
                    datas.add(start);
                }
                
                System.out.println(datas);
                return;
            }
            
            float perLevel = (float) maybePerLevel.get().getValue();
            
            double[] results = IntStream.rangeClosed(0, 17)
                                        .mapToDouble(i -> start + perLevel * i)
                                        .map(d -> Math.round(d * 10000.0) / 10000.0)
                                        .toArray();
            
            System.out.println(Arrays.toString(results));
            return;
        }
        
        // assume they are sorted
        BINContainer breakpoints = (BINContainer) breakpointsMaybe.get().getValue();
        float        value       = start;
        float        increment   = 0;
        for (Object breakpointObject : breakpoints.getData())
        {
            BINStruct          breakpoint = (BINStruct) breakpointObject;
            Optional<BINValue> maybeLevel = breakpoint.get("mLevel");
            int                level      = 2;
            if (maybeLevel.isPresent())
            {
                level = (int) maybeLevel.get().getValue();
            } else
            {
                increment = parseIncrement(increment, breakpoint);
            }
            
            while (datas.size() < level - 1)
            {
                value = value + increment;
                datas.add(value);
            }
            
            increment = parseIncrement(increment, breakpoint);
        }
        
        while (datas.size() < 18)
        {
            value = value + increment;
            datas.add(value);
        }
        
        System.out.println(datas);
    }
    
    private float parseIncrement(float increment, BINStruct breakpoint)
    {
        Optional<BINValue> maybeNext = breakpoint.get("D5FD07ED");
        if (maybeNext.isEmpty())
        {
            maybeNext = breakpoint.get("57FDC438");
            if (maybeNext.isEmpty())
            {
                return 0;
            }
        }
        
        if (maybeNext.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        return (float) maybeNext.get().getValue();
    }
    
    private void calculateByCoefficient(BINStruct partEntry)
    {
        Optional<BINValue> coef = partEntry.get("mCoefficient");
        if (coef.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        if (coef.get().getType() != BINValueType.FLOAT)
        {
            throw new RuntimeException("Missing value!");
        }
        
        float value = (float) coef.get().getValue();
        
        int mStat = partEntry.getOrDefault("mStat", (byte) 0);
        if (!statTypes.containsKey(mStat))
        {
            throw new RuntimeException("Missing value! stat key: " + mStat);
        }
        
        double useValue = Math.round(value * 10000.0) / 100.0;
        
        System.out.println(useValue + "% " + statTypes.get(mStat));
    }
    
    private void calculateByEffect(BINStruct spell, BINStruct partEntry)
    {
        Optional<BINValue> maybeValue = partEntry.get("meffectindex");
        if (maybeValue.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        int index = ((Integer) maybeValue.get().getValue()) - 1;
        
        Optional<BINValue> mEffectValues = spell.get("mEffectAmount");
        if (mEffectValues.isEmpty())
        {
            // this path is sketchy
            int dataValueIndex = index + 1;
            
            Optional<BINValue> mDataValues = spell.get("mDataValues");
            if (mDataValues.isEmpty())
            {
                throw new RuntimeException("Missing value!");
            }
            
            BINContainer spellDataContainer = (BINContainer) mDataValues.get().getValue();
            if (spellDataContainer.getData().size() < dataValueIndex)
            {
                throw new RuntimeException("Missing value!");
            }
            
            BINStruct          value        = (BINStruct) spellDataContainer.getData().get(dataValueIndex);
            Optional<BINValue> resultsMaybe = value.get("mValues");
            if (resultsMaybe.isEmpty())
            {
                System.out.println("0");
                return;
            }
            
            BINContainer results = (BINContainer) resultsMaybe.get().getValue();
            System.out.println(results.getData());
            return;
        }
        
        BINContainer       effectContainer      = (BINContainer) mEffectValues.get().getValue();
        BINStruct          effectValueContainer = (BINStruct) effectContainer.getData().get(index);
        Optional<BINValue> maybeValues          = effectValueContainer.get("value");
        if (maybeValues.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        BINContainer value = (BINContainer) maybeValues.get().getValue();
        System.out.println(value.getData());
    }
    
    private void calculateByCharacterLevel(BINStruct partEntry)
    {
        Optional<BINValue> mStartValue = partEntry.get("mStartValue");
        Optional<BINValue> mEndValue   = partEntry.get("mEndValue");
        
        float start = 0;
        float end   = 0;
        
        if (mEndValue.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        if (mEndValue.get().getType() != BINValueType.FLOAT)
        {
            throw new RuntimeException("Missing value!");
        }
        
        if (mStartValue.isPresent())
        {
            if (mStartValue.get().getType() != BINValueType.FLOAT)
            {
                throw new RuntimeException("Missing value!");
            }
            
            start = (float) mStartValue.get().getValue();
        }
        
        end = (float) mEndValue.get().getValue();
        float perLevel = end - start;
        
        float finalStart = start;
        double[] results = IntStream.rangeClosed(0, 17)
                                    .mapToDouble(i -> finalStart + perLevel / 17 * i)
                                    .map(d -> Math.round(d * 10000.0) / 10000.0)
                                    .toArray();
        System.out.println(Arrays.toString(results));
    }
    
    private void calculateByRawNumber(BINStruct partEntry)
    {
        if (partEntry.getData().size() == 0)
        {
            System.out.println(0);
            return;
        }
        
        if (partEntry.getData().size() > 1)
        {
            throw new RuntimeException("Missing value!");
        }
        
        BINValue value = partEntry.getData().get(0);
        
        if (value.getType() == BINValueType.FLOAT)
        {
            System.out.println((float) value.getValue());
            return;
        }
        
        throw new RuntimeException("Missing value!");
    }
    
    private Object getSpellDataValueByKey(BINStruct spell, String key)
    {
        Optional<BINValue> mDataValues = spell.get("mDataValues");
        if (mDataValues.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        BINContainer spellDataContainer = (BINContainer) mDataValues.get().getValue();
        for (Object dataValue : spellDataContainer.getData())
        {
            BINStruct          value    = (BINStruct) dataValue;
            Optional<BINValue> dataName = value.get("mName");
            if (dataName.isEmpty())
            {
                throw new RuntimeException("Missing value!");
            }
            
            String testKey = (String) dataName.get().getValue();
            
            if (!testKey.equalsIgnoreCase(key))
            {
                continue;
            }
            
            return dataValue;
        }
        
        return null;
    }
    
    private void calculateStatByNamedVariable(BINStruct spell, BINStruct partEntry)
    {
        Optional<BINValue> maybeValue = partEntry.get("mDataValue");
        if (maybeValue.isEmpty())
        {
            throw new RuntimeException("Missing value!");
        }
        
        String nameKey    = (String) maybeValue.get().getValue();
        Object spellEntry = getSpellDataValueByKey(spell, nameKey);
        if (spellEntry == null)
        {
            throw new RuntimeException("Missing value! no bin hash for key " + nameKey);
        }
        
        BINStruct          value        = (BINStruct) spellEntry;
        Optional<BINValue> resultsMaybe = value.get("mValues");
        if (resultsMaybe.isEmpty())
        {
            System.out.println(nameKey + ": 0");
            return;
        }
        
        BINContainer results = (BINContainer) resultsMaybe.get().getValue();
        System.out.println(nameKey + ": " + results.getData());
    }
}

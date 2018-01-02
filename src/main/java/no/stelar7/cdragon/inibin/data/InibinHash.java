package no.stelar7.cdragon.inibin.data;

import java.util.*;
import java.util.function.Function;

public final class InibinHash
{
    private static Map<String, String>                   hashes     = new HashMap<>();
    private static Map<String, Function<Object, Object>> transforms = new HashMap<>();
    
    private InibinHash()
    {
        // hide public constructor
    }
    
    public static String getHash(String hash)
    {
        return hashes.getOrDefault(hash, hash);
    }
    
    public static String getTransformed(String key, Object hash)
    {
        return transforms.getOrDefault(key, i -> i).apply(hash).toString();
    }
    
    private static final Function<Object, Object> percent          = v -> ((v instanceof Integer) ? (Integer.valueOf((String) v) * 10) : (Float.valueOf((String) v) * 100f));
    private static final Function<Object, Object> attckspeed       = v -> (0.625f / (1.0f + Float.valueOf((String) v)));
    private static final Function<Object, Object> attckspeedPerLvl = v -> ((Number) v).floatValue() * 0.01f;
    private static final Function<Object, Object> regenPer5        = v -> ((Number) v).floatValue() * 5;
    
    
    static
    {
        // base stats
        hashes.put(Integer.toUnsignedString(1880118880), "champion.stats.damage");
        hashes.put(Integer.toUnsignedString(742042233), "champion.stats.hp");
        hashes.put(Integer.toUnsignedString(742370228), "champion.stats.mana");
        hashes.put(Integer.toUnsignedString(1081768566), "champion.stats.runspeed");
        hashes.put(Integer.toUnsignedString(-1695914273), "champion.stats.armor");
        hashes.put(Integer.toUnsignedString(1395891205), "champion.stats.magicarmor");
        hashes.put(Integer.toUnsignedString(-166675978), "champion.stats.hpreg");
        hashes.put(Integer.toUnsignedString(619143803), "champion.stats.mpreg");
        hashes.put(Integer.toUnsignedString(-2103674057), "champion.stats.aspeed");
        
        // stats gained per level
        hashes.put(Integer.toUnsignedString(1139868982), "champion.levelup.damage");
        hashes.put(Integer.toUnsignedString(-988146097), "champion.levelup.hp");
        hashes.put(Integer.toUnsignedString(1003217290), "champion.levelup.mana");
        hashes.put(Integer.toUnsignedString(1608827366), "champion.levelup.armor");
        hashes.put(Integer.toUnsignedString(-194100563), "champion.levelup.magicarmor");
        hashes.put(Integer.toUnsignedString(-1232864324), "champion.levelup.hpreg");
        hashes.put(Integer.toUnsignedString(1248483905), "champion.levelup.mpreg");
        hashes.put(Integer.toUnsignedString(770205030), "champion.levelup.aspeed");
        
        // skills
        hashes.put(Integer.toUnsignedString(404599689), "champion.skills.1");
        hashes.put(Integer.toUnsignedString(404599690), "champion.skills.2");
        hashes.put(Integer.toUnsignedString(404599691), "champion.skills.3");
        hashes.put(Integer.toUnsignedString(404599692), "champion.skills.4");
        hashes.put(Integer.toUnsignedString(-893169035), "champion.skills.passive.info");
        hashes.put(Integer.toUnsignedString(743602011), "champion.skills.passive.desc");
        hashes.put(Integer.toUnsignedString(-484483517), "champion.skills.passive.icon");
        
        // champion info
        hashes.put(Integer.toUnsignedString(-148652351), "champion.info.description");
        hashes.put(Integer.toUnsignedString(1387461685), "champion.info.range");
        hashes.put(Integer.toUnsignedString(-148652351), "champion.info.tags");
        hashes.put(Integer.toUnsignedString(-51751813), "champion.info.lore");
        hashes.put(Integer.toUnsignedString(82690155), "champion.info.name");
        hashes.put(Integer.toUnsignedString(-547924932), "champion.info.desc");
        hashes.put(Integer.toUnsignedString(70667385), "champion.info.tips_as");
        hashes.put(Integer.toUnsignedString(70667386), "champion.info.tips_against");
        hashes.put(Integer.toUnsignedString(-547924932), "champion.info.title");
        
        // attackspeed
        transforms.put(Integer.toUnsignedString(-2103674057), attckspeed);
        transforms.put(Integer.toUnsignedString(770205030), attckspeedPerLvl);
        
        // mp and hp regen
        transforms.put(Integer.toUnsignedString(-166675978), regenPer5);
        transforms.put(Integer.toUnsignedString(-1232864324), regenPer5);
        transforms.put(Integer.toUnsignedString(619143803), regenPer5);
        transforms.put(Integer.toUnsignedString(1248483905), regenPer5);
        
        
        hashes.put(Integer.toUnsignedString(1829373218), "ability.tags");
        hashes.put(Integer.toUnsignedString(-963665088), "ability.levelup");
        hashes.put(Integer.toUnsignedString(-2007242095), "ability.buff_tooltip");
        hashes.put(Integer.toUnsignedString(-1549183761), "ability.range_ult");
        hashes.put(Integer.toUnsignedString(1805538005), "ability.name");
        hashes.put(Integer.toUnsignedString(-1203593619), "ability.internalName");
        hashes.put(Integer.toUnsignedString(-863113692), "ability.desc");
        hashes.put(Integer.toUnsignedString(-1660048132), "ability.tooltip");
        hashes.put(Integer.toUnsignedString(2059614685), "ability.img");
        hashes.put(Integer.toUnsignedString(-1764096472), "ability.range");
        
        hashes.put(Integer.toUnsignedString(-523242843), "ability.cost.level1");
        hashes.put(Integer.toUnsignedString(-523242842), "ability.cost.level2");
        hashes.put(Integer.toUnsignedString(-523242841), "ability.cost.level3");
        hashes.put(Integer.toUnsignedString(-523242840), "ability.cost.level4");
        hashes.put(Integer.toUnsignedString(-523242839), "ability.cost.level5");
        
        // @EffectXAmount@
        hashes.put(Integer.toUnsignedString(466816973), "ability.effect1.level1");
        hashes.put(Integer.toUnsignedString(-235012530), "ability.effect1.level2");
        hashes.put(Integer.toUnsignedString(-936842033), "ability.effect1.level3");
        hashes.put(Integer.toUnsignedString(-1638671536), "ability.effect1.level4");
        hashes.put(Integer.toUnsignedString(1954466257), "ability.effect1.level5");
        
        hashes.put(Integer.toUnsignedString(-396677938), "ability.effect2.level1");
        hashes.put(Integer.toUnsignedString(-1098507441), "ability.effect2.level2");
        hashes.put(Integer.toUnsignedString(-1800336944), "ability.effect2.level3");
        hashes.put(Integer.toUnsignedString(1792800849), "ability.effect2.level4");
        hashes.put(Integer.toUnsignedString(1090971346), "ability.effect2.level5");
        
        hashes.put(Integer.toUnsignedString(-1260172849), "ability.effect3.level1");
        hashes.put(Integer.toUnsignedString(-1962002352), "ability.effect3.level2");
        hashes.put(Integer.toUnsignedString(1631135441), "ability.effect3.level3");
        hashes.put(Integer.toUnsignedString(929305938), "ability.effect3.level4");
        hashes.put(Integer.toUnsignedString(227476435), "ability.effect3.level5");
        
        hashes.put(Integer.toUnsignedString(-2123667760), "ability.effect4.level1");
        hashes.put(Integer.toUnsignedString(1469470033), "ability.effect4.level2");
        hashes.put(Integer.toUnsignedString(767640530), "ability.effect4.level3");
        hashes.put(Integer.toUnsignedString(65811027), "ability.effect4.level4");
        hashes.put(Integer.toUnsignedString(-636018476), "ability.effect4.level5");
        
        hashes.put(Integer.toUnsignedString(1307804625), "ability.effect5.level1");
        hashes.put(Integer.toUnsignedString(605975122), "ability.effect5.level2");
        hashes.put(Integer.toUnsignedString(-95854381), "ability.effect5.level3");
        hashes.put(Integer.toUnsignedString(-797683884), "ability.effect5.level4");
        hashes.put(Integer.toUnsignedString(-1499513387), "ability.effect5.level5");
        
        hashes.put(Integer.toUnsignedString(-1665665330), "ability.cooldown.level1");
        hashes.put(Integer.toUnsignedString(-1665665329), "ability.cooldown.level2");
        hashes.put(Integer.toUnsignedString(-1665665328), "ability.cooldown.level3");
        hashes.put(Integer.toUnsignedString(-1665665327), "ability.cooldown.level4");
        hashes.put(Integer.toUnsignedString(-1665665326), "ability.cooldown.level5");
        
        // @CharAbilityPower@ and @CharBonusPhysical@
        hashes.put(Integer.toUnsignedString(-797683884), "ability.scale1");
        hashes.put(Integer.toUnsignedString(-797683884), "ability.scale2");
        
        
        // @CharAbilityPower@ and @CharBonusPhysical@
        transforms.put(Integer.toUnsignedString(-797683884), percent);
        transforms.put(Integer.toUnsignedString(-797683884), percent);
        
    }
    
    
}

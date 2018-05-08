package types.util;

import com.google.gson.*;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.Vector2;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class TestFlipHashKeys
{
    Path                        toHash = Paths.get("src\\main\\java\\no\\stelar7\\cdragon").resolve("hashes.json");
    List<Vector2<Long, String>> values = new ArrayList<>();
    
    @Test
    public void testFlips() throws IOException
    {
        String     data = new String(Files.readAllBytes(toHash));
        JsonObject elem = new JsonParser().parse(data).getAsJsonObject();
        
        printKeys(elem, "");
        
        values.sort(Comparator.comparing(Vector2::getSecond, new NaturalOrderComparator()));
        
        StringBuilder sb = new StringBuilder("{\n");
        for (Vector2<Long, String> pair : values)
        {
            sb.append("\t\"").append(pair.getFirst()).append("\": \"").append(pair.getSecond()).append("\",\n");
        }
        sb.reverse().delete(0, 2).reverse().append("\n}");
        Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\binhash.json"), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    private void printKeys(JsonObject elem, String s)
    {
        for (String s1 : elem.keySet())
        {
            String next = s + "." + s1;
            if (s.isEmpty())
            {
                next = s1;
            }
            if (elem.get(s1).isJsonObject())
            {
                printKeys(elem.getAsJsonObject(s1), next);
            } else
            {
                //String data = String.format("\"%s\":\"%s\",%n", elem.get(s1), next);
                values.add(new Vector2<>(elem.get(s1).getAsLong(), next));
            }
        }
    }
    
    @Test
    public void testOldHash() throws IOException
    {
        
        Map<String, String> hashes = new HashMap<>();
        
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
        
        
        List<Vector2<String, String>> vals = new ArrayList<>();
        
        hashes.forEach((k, v) -> vals.add(new Vector2<>(k, v)));
        
        vals.sort(Comparator.comparing(Vector2::getSecond, new NaturalOrderComparator()));
        
        StringBuilder sb = new StringBuilder("{\n");
        for (Vector2<String, String> pair : vals)
        {
            sb.append("\t\"").append(pair.getFirst()).append("\": \"").append(pair.getSecond()).append("\",\n");
        }
        sb.reverse().delete(0, 2).reverse().append("\n}");
        Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\binhash2.json"), sb.toString().getBytes(StandardCharsets.UTF_8));
        
    }
}

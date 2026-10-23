package offline;

import no.stelar7.cdragon.util.handlers.HashHandler;
import no.stelar7.cdragon.util.hashguessing.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The hash guessing patterns, on fabricated paths.
 * The guessers get a small HashFile in a temp folder, so the real hash lists (and the network) are never used.
 */
public class HashGuessingTest
{
    @BeforeAll
    public static void offlineHashes()
    {
        OfflineHashes.useEmpty();
    }

    private Path dir;

    @BeforeEach
    public void setup() throws IOException
    {
        dir = Files.createTempDirectory("cdragon-hashguess");
    }

    @AfterEach
    public void cleanup() throws IOException
    {
        try (Stream<Path> files = Files.walk(dir))
        {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    private static String hex(String path)
    {
        return HashHandler.toHex(HashHandler.computeXXHash64AsLong(path.toLowerCase(Locale.ENGLISH)), 16);
    }

    /**
     * A hash file (in the temp folder) containing the given paths
     */
    private HashFile hashFile(String name, Collection<String> paths) throws IOException
    {
        StringBuilder json = new StringBuilder("{");
        for (String path : paths)
        {
            json.append(json.length() == 1 ? "" : ",").append('"').append(hex(path)).append("\":\"").append(path).append('"');
        }
        json.append("}");

        Path file = dir.resolve(name + ".json");
        Files.writeString(file, json);
        return new HashFile(file, dir.resolve(name + ".backup.json"));
    }

    private Set<String> found(HashGuesser guesser) throws IOException
    {
        Path out = dir.resolve("out-" + UUID.randomUUID() + ".json");
        guesser.saveAsJson(out);
        String json = Files.readString(out);
        Set<String> values = new HashSet<>();
        no.stelar7.cdragon.util.handlers.JsonHandler.getGson().fromJson(json, Map.class).values().forEach(v -> values.add((String) v));
        return values;
    }

    @Test
    public void testGamePatterns() throws IOException
    {
        List<String> knownPaths = List.of(
                "assets/sounds/wwise2016/vo/en_us/characters/ahri/skins/base/ahri_base_vo_audio.wpk",
                "data/menu/en_us/main.stringtable",
                "data/final/champions/ahri.en_us.wad.client",
                "assets/characters/ahri/skins/skin12/ahri_skin12_tx_cm.dds",
                "data/characters/ahri/skins/skin3.bin",
                "assets/characters/ahri/skins/base/ahri.skn",
                "assets/maps/props/rock.scb",
                "assets/sounds/wwise2016/sfx/characters/ahri/skins/base/ahri_base_sfx_events.bnk",
                "assets/ux/icons/icon_7.tex",
                "assets/ux/icons/0123456789abcdef.tex",
                "data/shaders/hlsl/foo.ps_2_0.dx11_100"
                                         );

        List<String> targets = List.of(
                "assets/sounds/wwise2016/vo/ko_kr/characters/ahri/skins/base/ahri_base_vo_audio.wpk",
                "data/menu/zh_cn/main.stringtable",
                "data/final/champions/ahri.de_de.wad.client",
                "assets/characters/ahri/skins/skin77/ahri_skin77_tx_cm.dds",
                // skin number, then sibling extension
                "assets/characters/ahri/skins/skin77/ahri_skin77_tx_cm.tex",
                "data/characters/ahri/skins/skin42.bin",
                "assets/characters/ahri/skins/skin12/ahri_skin12_tx_cm.tex",
                "assets/characters/ahri/skins/base/ahri.skl",
                "assets/maps/props/rock.sco",
                "assets/sounds/wwise2016/sfx/characters/ahri/skins/base/ahri_base_sfx_audio.bnk",
                "assets/sounds/wwise2016/sfx/characters/ahri/skins/base/ahri_base_sfx_audio.wpk",
                "assets/sounds/wwise2016/vo/en_us/characters/ahri/skins/base/ahri_base_vo_events.bnk",
                "assets/ux/icons/icon_55.tex",
                "assets/ux/icons/icon_2.tex"
                                      );

        // content hashed names and shaders are skipped by substituteNumbers
        List<String> filtered = List.of("assets/ux/icons/0123456789abcdef1.tex", "data/shaders/hlsl/foo.ps_2_0.dx11_5");

        Set<String> unknown = new HashSet<>();
        targets.forEach(t -> unknown.add(hex(t)));
        filtered.forEach(t -> unknown.add(hex(t)));
        // a known hash in the unknown list is ignored
        unknown.add(hex(knownPaths.get(0)));

        GameHashGuesser g = new GameHashGuesser(hashFile("game", knownPaths), unknown);
        assertFalse(g.didFindNewHash());

        g.substituteSiblingExtensions();
        g.substituteLanguages();
        g.substituteSkinNumbers(99);
        g.substituteSiblingExtensions();
        g.substituteNumbers(99, Optional.empty());

        assertTrue(g.didFindNewHash());
        Set<String> found = found(g);
        for (String t : targets)
        {
            assertTrue(found.contains(t), "found " + t);
        }
        for (String f : filtered)
        {
            assertFalse(found.contains(f), "filtered " + f);
        }
        assertEquals(knownPaths.size() + targets.size(), found.size());
    }

    @Test
    public void testLanguageTemplatesKeepOccurrencesInSync() throws IOException
    {
        String known  = "assets/sounds/vo/en_us/x_en_us.bnk";
        String synced = "assets/sounds/vo/ko_kr/x_ko_kr.bnk";
        String mixed  = "assets/sounds/vo/ko_kr/x_en_us.bnk";

        GameHashGuesser g = new GameHashGuesser(hashFile("lang", List.of(known)), Set.of(hex(synced), hex(mixed)));
        g.substituteLanguages();

        Set<String> found = found(g);
        assertTrue(found.contains(synced));
        assertFalse(found.contains(mixed), "every occurrence of the locale is replaced together");
    }

    @Test
    public void testCheckVariants() throws IOException
    {
        List<String> targets = List.of("plugins/p/global/default/2x_a.png", "plugins/p/global/default/4x_a.png", "data/scripts/x.luabin", "data/scripts/x.preload");
        Set<String>  unknown = new HashSet<>();
        targets.forEach(t -> unknown.add(hex(t)));

        LCUHashGuesser g = new LCUHashGuesser(hashFile("variants", List.of()), unknown);
        g.check("plugins/p/global/default/a.png");
        g.check("data/scripts/x.lua");

        assertEquals(new HashSet<>(targets), found(g));

        // pre-hashed CDTB list lines, the hash is trusted and the path is not rehashed
        LCUHashGuesser p = new LCUHashGuesser(hashFile("prehashed", List.of()), Set.of("00000000000000AB"));
        p.checkPrehashed("00000000000000ab Some/Path.txt");
        p.checkPrehashed("garbage");
        assertEquals(Set.of("some/path.txt"), found(p));
    }

    @Test
    public void testLcuPatterns() throws IOException
    {
        String base = "plugins/p/global/default/";
        List<String> targets = List.of(
                base + "foo-new-thing.png",  // substituteBasenameWords, 2 words
                base + "foo_new.png",        // two words with "_"
                base + "new_bar.png",        // doubleSubstitution
                base + "icon_042.png",       // substituteNumbers with 3 digits
                base + "bar_thing.png",      // addBasenameWord
                "plugins/q/global/default/y.png" // substituteExtensions
                                      );
        Set<String> unknown = new HashSet<>();
        targets.forEach(t -> unknown.add(hex(t)));

        List<String> knownPaths = List.of("plugins/q/global/default/y.json", "plugins/r/global/default/z.png");
        LCUHashGuesser g = new LCUHashGuesser(hashFile("lcu", knownPaths), unknown);

        Set<String> paths = Set.of(base + "foo-old-stuff.png", base + "icon_007.png", base + "bar.png");
        Set<String> words = Set.of("new", "thing", "foo");

        g.substituteBasenameWords(paths, words, 2);
        g.doubleSubstitution(paths, words);
        g.substituteNumbers(paths, 100, Optional.of(3));
        g.addBasenameWord(paths, words);
        g.substituteExtensions();
        // missing folder: does nothing
        g.grepWads(dir.resolve("does-not-exist"));

        Set<String> found = found(g);
        for (String t : targets)
        {
            assertTrue(found.contains(t), "found " + t);
        }
    }
}

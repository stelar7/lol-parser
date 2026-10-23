package types.util;

import no.stelar7.cdragon.util.ProgressBar;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.hashguessing.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@TestMethodOrder(OrderAnnotation.class)
public class TestCDTBHashGuessing
{
    private final Path dataPath = UtilHandler.CDRAGON_FOLDER.resolve("pbe");
    
    // not a @Test, the ordered tests below already run everything
    public void doTests() throws Exception
    {
        ProgressBar stages = new ProgressBar("Hash guessing", 4);
        stages.stage("subchunktoc hashes");
        doSubchunkTocTest();

        stages.stage("BIN hashes");
        doBINTest();
        stages.stage("GAME hashes");
        doGameTest();
        stages.stage("LCU hashes");
        doLCUTest();
        stages.finish();
    }
    
    @Test
    @Order(1)
    public void doSubchunkTocTest() throws IOException
    {
        System.out.println("Started guessing subchunk hashes");
        
        GameHashGuesser game = new GameHashGuesser(HashLists.unknownFromExportWAD(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        
        Path extracted = UtilHandler.CDRAGON_FOLDER.resolve("extractedFiles");
        if (!Files.isDirectory(extracted))
        {
            System.out.println("No extracted files at " + extracted + ", skipping subchunk guessing");
            return;
        }

        Set<String> languages = game.getLanguages();
        List<String> wads;
        try (Stream<Path> files = Files.find(extracted, 50, (path, attr) -> {
            String name = path.getFileName().toString().toLowerCase();
            return attr.isRegularFile() && (name.endsWith(".wad") || name.endsWith(".wad.client"));
        }))
        {
            wads = files.map(p -> extracted.relativize(p).toString())
                        .map(p -> p.replace("\\", "/").toLowerCase(Locale.ENGLISH))
                        .toList();
        }

        ProgressBar progress = new ProgressBar("Guessing subchunktoc hashes", wads.size());
        wads.forEach(p -> {
                 progress.step();
                 game.check(p + ".subchunktoc");
                 game.check(p.replace(".client", "") + ".subchunktoc");

                 // {name}.wad.client -> {name}.{locale}.wad.subchunktoc, also when the wad itself is already localized
                 String name = p.substring(0, p.indexOf(".wad"));
                 int    dot  = name.lastIndexOf('.');
                 if (dot > name.lastIndexOf('/') && languages.contains(name.substring(dot + 1)))
                 {
                     name = name.substring(0, dot);
                 }

                 for (String locale : languages)
                 {
                     game.check(name + "." + locale + ".wad.subchunktoc");
                     game.check(name + "." + locale + ".wad.client.subchunktoc");
                 }
             });
        progress.finish();

        game.saveToBackup();
    }
    
    @Test
    @Order(10)
    public void doBINTest()
    {
        Set<String> unknowns = new HashSet<>();
        try
        {
            Path hashBinPath = UtilHandler.CDRAGON_FOLDER.resolve("hashbins");
            Files.createDirectories(hashBinPath);
            Files.find(hashBinPath, 2, (path, attr) -> path.toString().contains("UnknownBinHash")).forEach(p -> {
                unknowns.addAll(HashLists.unknownFromExportBIN(p));
            });
        } catch (IOException e)
        {
            e.printStackTrace();
            System.exit(0);
        }
        
        System.out.println("Started guessing BIN hashes");
        BINHashGuesser guesser = new BINHashGuesser(unknowns, dataPath);
        ProgressBar    stages  = new ProgressBar("BIN hashes", 8);
        stages.stage("pullCDTB");
        guesser.pullCDTB();
        guesser.saveToBackup();
        //guesser.guessFromFile(UtilHandler.CDRAGON_FOLDER.resolve("sorted_real.txt"), "(.*)");
        stages.stage("guessFromPets");
        guesser.guessFromPets(dataPath);
        stages.stage("guessTFTItems");
        guesser.guessTFTItems();
        stages.stage("guessSpellNames");
        guesser.guessSpellNames();
        stages.stage("guessTFTParticles");
        guesser.guessTFTParticles();
        stages.stage("guessNewCharacters");
        guesser.guessNewCharacters();
        stages.stage("guessNewAnimations");
        guesser.guessNewAnimations();
        stages.stage("guessFromFontFiles");
        guesser.guessFromFontFiles();
        guesser.saveToBackup();
        stages.finish();
    }
    
    @Test
    @Order(20)
    public void doGameTest() throws InterruptedException
    {
        System.out.println("Started guessing GAME hashes");
        GameHashGuesser guesser = new GameHashGuesser(HashLists.unknownFromExportWAD(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        ProgressBar     stages  = new ProgressBar("GAME hashes", 13);
        stages.stage("pullCDTB");
        guesser.pullCDTB();
        guesser.saveToBackup();
        stages.stage("guessHardcoded");
        guesser.guessHardcoded();
        stages.stage("guessStringTableFiles");
        guesser.guessStringTableFiles();
        stages.stage("guessScripts");
        guesser.guessScripts(dataPath);
        stages.stage("guessShaderFiles");
        guesser.guessShaderFiles(dataPath);
        guesser.saveToBackup();
        stages.stage("guessAssetsBySearch");
        guesser.guessAssetsBySearch(dataPath);
        stages.stage("guessBinByLinkedFiles");
        guesser.guessBinByLinkedFiles(dataPath);
        guesser.saveToBackup();

        // cheap and exact, run these first so the substitutions below can build on what they find
        stages.stage("substituteSiblingExtensions");
        guesser.substituteSiblingExtensions();
        stages.stage("substituteLanguages");
        guesser.substituteLanguages();
        guesser.saveToBackup();
        stages.stage("substituteSkinNumbers");
        guesser.substituteSkinNumbers(99);
        stages.stage("substituteSiblingExtensions");
        guesser.substituteSiblingExtensions();
        guesser.saveToBackup();
        stages.stage("substituteNumbers");
        guesser.substituteNumbers(99, Optional.empty());
        guesser.saveToBackup();
        stages.finish();

        //guesser.guessByExistingWords();
        //guesser.saveToBackup();
    }
    
    @Test
    @Order(30)
    public void doLCUTest()
    {
        System.out.println("Started guessing LCU hashes");
        LCUHashGuesser guesser = new LCUHashGuesser(HashLists.unknownFromExportWAD(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        ProgressBar    stages  = new ProgressBar("LCU hashes", 13);
        stages.stage("pullCDTB");
        guesser.pullCDTB();
        guesser.saveToBackup();
        stages.stage("guessManifestFiles");
        guesser.guessManifestFiles();
        stages.stage("guessAssetsBySearch");
        guesser.guessAssetsBySearch(dataPath);
        stages.stage("guessSanitizerHashes");
        guesser.guessSanitizerHashes();
        stages.stage("guessPatterns");
        guesser.guessPatterns();
        guesser.saveToBackup();

        // needs the game hashes, so doGameTest runs first
        stages.stage("guessFromGameHashes");
        guesser.guessFromGameHashes();
        stages.stage("grepWads");
        guesser.grepWads(UtilHandler.CDRAGON_FOLDER.resolve("extractedFiles"));
        guesser.saveToBackup();

        stages.stage("substituteRegionLang");
        guesser.substituteRegionLang();
        stages.stage("substituteBasenames");
        guesser.substituteBasenames();
        stages.stage("substitutePlugins");
        guesser.substitutePlugins();
        stages.stage("substituteExtensions");
        guesser.substituteExtensions();
        guesser.saveToBackup();

        stages.stage("substituteBasenameWords");
        guesser.substituteBasenameWords(null, null, null, 1);
        stages.stage("addBasenameWord");
        guesser.addBasenameWord();
        guesser.saveToBackup();
        stages.finish();
        
        //guesser.guessByExistingWords();
        //guesser.saveToBackup();
    }
    
}

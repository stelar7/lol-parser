package types.util;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.hashguessing.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@TestMethodOrder(OrderAnnotation.class)
public class TestCDTBHashGuessing
{
    private final Path dataPath = UtilHandler.CDRAGON_FOLDER.resolve("pbe");
    
    // not a @Test, the ordered tests below already run everything
    public void doTests() throws Exception
    {
        doSubchunkTocTest();
        
        doBINTest();
        doGameTest();
        doLCUTest();
    }
    
    @Test
    @Order(1)
    public void doSubchunkTocTest() throws IOException
    {
        System.out.println("Started guessing subchunk hashes");
        
        GameHashGuesser game = new GameHashGuesser(HashGuesser.unknownFromExportWAD(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        
        Path extracted = UtilHandler.CDRAGON_FOLDER.resolve("extractedFiles");
        if (!Files.isDirectory(extracted))
        {
            System.out.println("No extracted files at " + extracted + ", skipping subchunk guessing");
            return;
        }

        Set<String> languages = game.getLanguages();
        Files.find(extracted, 50, (path, attr) -> {
                 String name = path.getFileName().toString().toLowerCase();
                 return attr.isRegularFile() && (name.endsWith(".wad") || name.endsWith(".wad.client"));
             })
             .map(p -> extracted.relativize(p).toString())
             .map(p -> p.replace("\\", "/").toLowerCase(Locale.ENGLISH))
             .forEach(p -> {
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
                unknowns.addAll(HashGuesser.unknownFromExportBIN(p));
            });
        } catch (IOException e)
        {
            e.printStackTrace();
            System.exit(0);
        }
        
        System.out.println("Started guessing BIN hashes");
        BINHashGuesser guesser = new BINHashGuesser(unknowns, dataPath);
        guesser.pullCDTB();
        guesser.saveToBackup();
        //guesser.guessFromFile(UtilHandler.CDRAGON_FOLDER.resolve("sorted_real.txt"), "(.*)");
        guesser.guessFromPets(dataPath);
        guesser.guessTFTItems();
        guesser.guessSpellNames();
        guesser.guessTFTParticles();
        guesser.guessNewCharacters();
        guesser.guessNewAnimations();
        guesser.guessFromFontFiles();
        guesser.saveToBackup();
    }
    
    @Test
    @Order(20)
    public void doGameTest() throws InterruptedException
    {
        System.out.println("Started guessing GAME hashes");
        GameHashGuesser guesser = new GameHashGuesser(HashGuesser.unknownFromExportWAD(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        guesser.pullCDTB();
        guesser.saveToBackup();
        guesser.guessHardcoded();
        guesser.guessStringTableFiles();
        guesser.guessScripts(dataPath);
        guesser.guessShaderFiles(dataPath);
        guesser.saveToBackup();
        guesser.guessAssetsBySearch(dataPath);
        guesser.guessBinByLinkedFiles(dataPath);
        guesser.saveToBackup();

        // cheap and exact, run these first so the substitutions below can build on what they find
        guesser.substituteSiblingExtensions();
        guesser.substituteLanguages();
        guesser.saveToBackup();
        guesser.substituteSkinNumbers(99);
        guesser.substituteSiblingExtensions();
        guesser.saveToBackup();
        guesser.substituteNumbers(99, Optional.empty());
        guesser.saveToBackup();

        //guesser.guessByExistingWords();
        //guesser.saveToBackup();
    }
    
    @Test
    @Order(30)
    public void doLCUTest()
    {
        System.out.println("Started guessing LCU hashes");
        LCUHashGuesser guesser = new LCUHashGuesser(HashGuesser.unknownFromExportWAD(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        guesser.pullCDTB();
        guesser.saveToBackup();
        guesser.guessManifestFiles();
        guesser.guessAssetsBySearch(dataPath);
        guesser.guessSanitizerHashes();
        guesser.guessPatterns();
        guesser.saveToBackup();

        // needs the game hashes, so doGameTest runs first
        guesser.guessFromGameHashes();
        guesser.grepWads(UtilHandler.CDRAGON_FOLDER.resolve("extractedFiles"));
        guesser.saveToBackup();

        guesser.substituteRegionLang();
        guesser.substituteBasenames();
        guesser.substitutePlugins();
        guesser.substituteExtensions();
        guesser.saveToBackup();
        
        guesser.substituteBasenameWords(null, null, null, 1);
        guesser.addBasenameWord();
        guesser.saveToBackup();
        
        //guesser.guessByExistingWords();
        //guesser.saveToBackup();
    }
    
}

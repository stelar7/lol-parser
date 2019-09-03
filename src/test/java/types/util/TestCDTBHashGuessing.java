package types.util;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.hashguessing.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.*;

import java.nio.file.Path;

@TestMethodOrder(OrderAnnotation.class)
public class TestCDTBHashGuessing
{
    private final Path dataPath = UtilHandler.CDRAGON_FOLDER.resolve("pbe");
    
    @Test
    @Order(1)
    public void doBINTest()
    {
        BINHashGuesser guesser = new BINHashGuesser(HashGuesser.unknownFromExportBIN(UtilHandler.CDRAGON_FOLDER.resolve("binHashUnknown.txt")), dataPath);
        guesser.pullCDTB();
        guesser.guessNewCharacters();
        guesser.guessNewAnimations();
        guesser.guessFromFontFiles();
        
        guesser.saveToBackup();
    }
    
    @Test
    @Order(10)
    public void doGameTest()
    {
        GameHashGuesser guesser = new GameHashGuesser(HashGuesser.unknownFromExportWAD(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        guesser.pullCDTB();
        guesser.guessAssetsBySearch(dataPath);
        guesser.guessBinByLinkedFiles(dataPath);
        
        guesser.saveToBackup();
    }
    
    @Test
    @Order(20)
    public void doLCUTest()
    {
        LCUHashGuesser guesser = new LCUHashGuesser(HashGuesser.unknownFromExportWAD(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        guesser.pullCDTB();
        guesser.guessAssetsBySearch(dataPath);
        guesser.substituteRegionLang();
        guesser.substitutePlugin();
        guesser.substituteBasenames();
        guesser.substituteBasenameWords(null, null, null, 1);
        guesser.addBasenameWord();
        
        guesser.saveToBackup();
    }
    
}

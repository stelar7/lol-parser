package types.util;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.hashguessing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.nio.file.Path;

@TestMethodOrder(OrderAnnotation.class)
public class TestCDTBHashGuessing
{
    private final Path dataPath = UtilHandler.CDRAGON_FOLDER.resolve("pbe");
    
    @Test
    @Order(1)
    public void doGameTest()
    {
        GameHashGuesser gguesser = new GameHashGuesser(HashGuesser.unknownFromExportWAD(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        gguesser.guessAssetsBySearch(dataPath);
        gguesser.guessBinByLinkedFiles(dataPath);
        //gguesser.save();
        
        gguesser.saveToBackup();
    }
    
    @Test
    @Order(2)
    public void doLCUTest()
    {
        LCUHashGuesser guesser = new LCUHashGuesser(HashGuesser.unknownFromExportWAD(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        guesser.substituteRegionLang();
        guesser.substitutePlugin();
        guesser.substituteBasenames();
        guesser.substituteBasenameWords(null, null, null, 1);
        guesser.addBasenameWord();
        //guesser.save();
        
        guesser.saveToBackup();
    }
    
    @Test
    @Order(3)
    public void doBINTest()
    {
        BINHashGuesser guesser = new BINHashGuesser(HashGuesser.unknownFromExportBIN(UtilHandler.CDRAGON_FOLDER.resolve("binHashUnknown.txt")), dataPath);
        //guesser.guessNewCharacters();
        guesser.guessNewAnimations();
        
        // guesser.saveAsJson();
        
        // guesser.saveToBackup();
    }
}

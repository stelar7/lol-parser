package types.util;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.hashguessing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.nio.file.Path;

@TestMethodOrder(OrderAnnotation.class)
public class TestCDTBHashGuessing
{
    @Test
    @Order(1)
    public void doGameTest()
    {
        Path dataPath = UtilHandler.CDRAGON_FOLDER.resolve("pbe");
        
        GameHashGuesser gguesser = new GameHashGuesser(HashGuesser.unknownFromExport(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        gguesser.guessAssetsBySearch(dataPath);
        gguesser.guessBinByLinkedFiles(dataPath);
        //gguesser.save();
        
        gguesser.saveToBackup();
    }
    
    @Test
    @Order(2)
    public void doLCUTest()
    {
        LCUHashGuesser guesser = new LCUHashGuesser(HashGuesser.unknownFromExport(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        guesser.substituteRegionLang();
        guesser.substitutePlugin();
        guesser.substituteBasenames();
        guesser.substituteBasenameWords(null, null, null, 1);
        guesser.addBasenameWord();
        //guesser.save();
        
        guesser.saveToBackup();
    }
}

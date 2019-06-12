package types.util;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.hashguessing.*;
import org.junit.Test;

public class TestCDTBHashGuessing
{
    @Test
    public void doTest()
    {
        LCUHashGuesser guesser = new LCUHashGuesser(HashGuesser.unknownFromExport(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        guesser.substituteRegionLang();
        guesser.save();
        guesser.substitutePlugin();
        guesser.save();
        guesser.substituteBasenames();
        guesser.save();
        guesser.substituteBasenameWords(null, null, null, 1);
        guesser.save();
        guesser.addBasenameWord();
        guesser.save();
        
        guesser.saveAsJson();
    }
    
    @Test
    public void doGameTest()
    {
        GameHashGuesser gguesser = new GameHashGuesser(HashGuesser.unknownFromExport(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        gguesser.guessVoiceLines(UtilHandler.CDRAGON_FOLDER.resolve("pbe"));
        gguesser.save();
        
        gguesser.saveAsJson();
    }
}

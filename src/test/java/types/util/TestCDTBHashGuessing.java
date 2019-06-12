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
        guesser.substitutePlugin();
        guesser.substituteBasenames();
        guesser.substituteBasenameWords(null, null, null, 1);
        guesser.addBasenameWord();
        //guesser.save();
        
        guesser.saveAsJson();
    }
    
    @Test
    public void doGameTest()
    {
        GameHashGuesser gguesser = new GameHashGuesser(HashGuesser.unknownFromExport(UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt")));
        gguesser.check("assets/Characters/PetGhosty/Skins/Skin12/Particles/PetGhosty_Skin12_Idle_Wisp.dds".toLowerCase());
        gguesser.guessBinByLinkedFiles(UtilHandler.CDRAGON_FOLDER.resolve("pbe"));
        gguesser.guessVoiceLines(UtilHandler.CDRAGON_FOLDER.resolve("pbe"));
        //gguesser.save();
        
        gguesser.saveAsJson();
    }
}

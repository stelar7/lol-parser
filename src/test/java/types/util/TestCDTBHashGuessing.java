package types.util;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.hashguessing.*;
import org.junit.Test;

public class TestCDTBHashGuessing
{
    @Test
    public void doTest()
    {
        LCUHashGuesser guesser = new LCUHashGuesser(HashGuesser.unknownFromExport(UtilHandler.DOWNLOADS_FOLDER.resolve("pbe")));
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
    }
}

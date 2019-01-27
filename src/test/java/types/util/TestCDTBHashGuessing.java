package types.util;

import no.stelar7.cdragon.util.hashguessing.*;
import org.junit.Test;

import java.nio.file.Paths;

public class TestCDTBHashGuessing
{
    @Test
    public void doTest()
    {
        LCUHashGuesser guesser = new LCUHashGuesser(HashGuesser.unknownFromExport(Paths.get("")));
        guesser.substituteRegionLang();
        guesser.substitutePlugin();
        guesser.substituteBasenames();
        guesser.substituteBasenameWords(null, null, null, 1);
        guesser.addBasenameWord();
        guesser.save();
    }
}

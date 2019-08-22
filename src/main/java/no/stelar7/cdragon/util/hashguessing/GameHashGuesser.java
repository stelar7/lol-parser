package no.stelar7.cdragon.util.hashguessing;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.util.handlers.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.*;
import java.util.stream.Stream;

public class GameHashGuesser extends HashGuesser
{
    
    public GameHashGuesser(Set<String> hashes)
    {
        super(HashGuesser.hashFileGAME, hashes);
        System.out.println("Started guessing GAME hashes");
    }
    
    public void guessBinByLinkedFiles(Path pbe)
    {
        System.out.println("Guessing bin files by linked file names");
        try
        {
            System.out.println("Parsing bin files...");
            BINParser parser = new BINParser();
            Files.walk(pbe)
                 .filter(UtilHandler.IS_BIN_PREDICATE)
                 .map(parser::parse)
                 .flatMap(b -> b.getLinkedFiles().stream())
                 .forEach(this::check);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    
    public void guessAssetsBySearch(Path pbe)
    {
        System.out.println("Guessing assets by searching strings");
        List<Path> readMe = UtilHandler.getFilesMatchingPredicate(pbe, UtilHandler.IS_JSON_PREDICATE);
        
        // need a better regex for this :thinking:
        Pattern p = Pattern.compile("(/.*?/).*?\\..*\"");
        
        readMe.stream()
              .map(UtilHandler::readAsString)
              .forEach(e -> {
                  Matcher m = p.matcher(e);
                  while (m.find())
                  {
                      int lastStart = 0;
                      for (int i = 0; i <= m.groupCount(); i++)
                      {
                          int start = m.start(i);
                          int end   = m.end(i) - 1;
                    
                          if (start == lastStart)
                          {
                              continue;
                          }
                    
                          lastStart = start;
                          while (e.charAt(start - 1) != '"')
                          {
                              start--;
                          }
                    
                          while (e.charAt(end) != '"')
                          {
                              end++;
                          }
                    
                          String toCheck = e.substring(start, end).toLowerCase();
                          this.check(toCheck);
                      }
                  }
              });
    }
    
    private final Predicate<String> isGameHash = s -> !s.startsWith("plugins/");
    
    public void pullCDTB()
    {
        System.out.println("Feching hashlists from CDTB");
        String hashA = "https://github.com/CommunityDragon/CDTB/raw/master/cdragontoolbox/hashes.game.txt";
        String hashB = "https://github.com/Morilli/CDTB/raw/new-hashes/cdragontoolbox/hashes.game.txt";
        
        Stream.of(WebHandler.readWeb(hashA).stream(), WebHandler.readWeb(hashB).stream())
              .flatMap(a -> a)
              .map(line -> line.substring(line.indexOf(' ') + 1))
              .filter(isGameHash)
              .forEach(this::check);
    }
    
    @Override
    public String generateHash(String val)
    {
        Long hashNum = HashHandler.computeXXHash64AsLong(val);
        return HashHandler.toHex(hashNum, 16);
    }
}

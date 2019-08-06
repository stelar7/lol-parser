package no.stelar7.cdragon.util.hashguessing;

import com.google.gson.JsonElement;
import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;

public class GameHashGuesser extends HashGuesser
{
    
    public GameHashGuesser(Set<String> hashes)
    {
        super(HashGuesser.hashFileGAME, hashes);
    }
    
    public void guessBinByLinkedFiles(Path pbe)
    {
        System.out.println("Guessing bin files by linked file names");
        List<Path> readMe = UtilHandler.getFilesMatchingPredicate(pbe, UtilHandler.IS_JSON_PREDICATE);
        
        readMe.stream()
              .map(UtilHandler::readAsString)
              .filter(s -> s.contains("linkedBinFiles"))
              .map(s -> UtilHandler.getJsonParser().parse(s))
              .map(JsonElement::getAsJsonObject)
              .forEach(e -> {
                  for (JsonElement link : e.getAsJsonArray("linkedBinFiles"))
                  {
                      String real = link.getAsString().toLowerCase();
                      this.check(real);
                  }
              });
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
}

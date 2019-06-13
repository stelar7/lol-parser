package no.stelar7.cdragon.util.hashguessing;

import com.google.gson.*;
import com.google.gson.stream.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class GameHashGuesser extends HashGuesser
{
    
    public GameHashGuesser(Set<String> hashes)
    {
        super(HashGuesser.hashFileGAME, hashes);
    }
    
    public void guessVoiceLines(Path pbe)
    {
        System.out.println("Guessing new voice lines");
        List<Path> readMe = UtilHandler.getFilesMatchingPredicate(pbe, UtilHandler.IS_JSON_PREDICATE);
        
        readMe.stream()
              .map(UtilHandler::readAsString)
              .filter(s -> s.contains("bankUnits"))
              .map(s -> UtilHandler.getJsonParser().parse(s))
              .map(JsonElement::getAsJsonObject)
              .forEach(e -> {
                  String     data   = "{" + e.toString().substring(e.toString().indexOf("bankUnits") - 1);
                  JsonReader reader = new JsonReader(new StringReader(data));
                  reader.setLenient(true);
            
                  JsonArray bankUnits = UtilHandler.getJsonParser().parse(reader).getAsJsonObject().getAsJsonArray("bankUnits");
                  for (JsonElement unit : bankUnits)
                  {
                      String    child = unit.getAsJsonObject().keySet().toArray(new String[0])[0];
                      JsonArray paths = unit.getAsJsonObject().getAsJsonObject(child).getAsJsonArray("bankPath");
                
                      for (JsonElement path : paths)
                      {
                          String real = path.getAsString().toLowerCase();
                          this.check(real);
                      }
                  }
              });
    }
    
    public void guessBinByLinkedFiles(Path pbe)
    {
        System.out.println("Guessing bin files by linked file names");
        List<Path> readMe = UtilHandler.getFilesMatchingPredicate(pbe, UtilHandler.IS_JSON_PREDICATE);
        
        readMe.stream()
              .map(UtilHandler::readAsString)
              .filter(s -> s.contains("linkedFiles"))
              .map(s -> UtilHandler.getJsonParser().parse(s))
              .map(JsonElement::getAsJsonObject)
              .forEach(e -> {
                  for (JsonElement link : e.getAsJsonArray("linkedFiles"))
                  {
                      String real = link.getAsString().toLowerCase();
                      this.check(real);
                  }
              });
    }
    
    
    public void guessAssetsBySearch(Path pbe)
    {
        System.out.println("Guessing assets by searching for assets");
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

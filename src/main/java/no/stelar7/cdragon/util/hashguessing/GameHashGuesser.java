package no.stelar7.cdragon.util.hashguessing;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class GameHashGuesser extends HashGuesser
{
    
    public GameHashGuesser(Set<String> hashes)
    {
        super(HashGuesser.hashFileGAME, hashes);
    }
    
    public void guessVoiceLines(Path pbe)
    {
        try
        {
            System.out.println("Guessing new voice lines");
            List<Path> readMe = new ArrayList<>();
            Files.walkFileTree(pbe, new SimpleFileVisitor<>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    if (Files.isDirectory(file))
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    if (!file.toString().endsWith(".json"))
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    String content = Files.readString(file);
                    
                    if (!content.contains("bankUnits"))
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    readMe.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
            
            
            readMe.stream().map(p -> {
                try
                {
                    return Files.readString(p);
                } catch (IOException e)
                {
                    e.printStackTrace();
                    return "";
                }
            }).map(s -> UtilHandler.getJsonParser().parse(s)).map(JsonElement::getAsJsonObject).forEach(e -> {
                JsonObject parent = e.getAsJsonObject("SkinCharacterDataProperties");
                if (parent != null)
                {
                    // its a skin
                    String firstKey = parent.keySet().toArray(new String[0])[0];
                    
                    JsonArray bankUnits = parent.getAsJsonObject(firstKey).getAsJsonObject("skinAudioProperties").getAsJsonObject("skinAudioProperties").getAsJsonArray("bankUnits");
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
                } else
                {
                    String     data   = e.toString().substring(e.toString().indexOf("bankUnits") - 2);
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
                }
            });
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

package no.stelar7.cdragon.util.handlers;

import com.google.gson.*;
import no.stelar7.cdragon.util.types.math.Vector2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Shared Gson instance and JSON helpers
 */
public final class JsonHandler
{
    private JsonHandler()
    {
        // Hide public constructor
    }
    
    private static Gson       gson;
    private static JsonParser parser;
    
    public static JsonParser getJsonParser()
    {
        if (parser == null)
        {
            parser = new JsonParser();
        }
        
        return parser;
    }
    
    public static Gson getGson()
    {
        if (gson == null)
        {
            gson = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();
        }
        
        return gson;
    }
    
    public static String mergeTopKeysToArray(String json)
    {
        Map<String, List<JsonElement>> data = new HashMap<>();
        json = json.replace("\n", "");
        
        for (int i = 1; i < json.length(); i++)
        {
            i += (json.indexOf('"') + 1);
            String key = json.substring(json.indexOf('"') + 1);
            i += (key.indexOf('"'));
            key = key.substring(0, key.indexOf('"'));
            i += 2;
            
            int           count = 0;
            StringBuilder sb    = new StringBuilder();
            
            while (true)
            {
                char at = json.charAt(i++);
                if (at == '{' || at == '[')
                {
                    count++;
                }
                
                if (at == '}' || at == ']')
                {
                    count--;
                }
                
                sb.append(at);
                
                if (count == 0)
                {
                    List<JsonElement> list = data.getOrDefault(key, new ArrayList<>());
                    list.add(getJsonParser().parse(sb.toString()));
                    data.put(key, list);
                    
                    sb.setLength(0);
                    json = json.substring(++i);
                    i = 0;
                    break;
                }
            }
        }
        
        Map<String, Object> returnData = new HashMap<>();
        data.forEach((k, v) -> {
            if (v.size() == 1)
            {
                returnData.put(k, v.get(0));
            } else
            {
                returnData.put(k, v);
            }
        });
        
        
        return getGson().toJson(returnData);
    }
    
    public static <X, Y> void pairPrintout(Path outputFile, List<Vector2<X, Y>> data)
    {
        StringBuilder sb = new StringBuilder("{\n");
        for (Vector2<X, Y> pair : data)
        {
            sb.append("\t\"").append(pair.getFirst()).append("\": \"").append(pair.getSecond()).append("\",\n");
        }
        sb.reverse().delete(0, 2).reverse().append("\n}");
        
        try
        {
            Files.createDirectories(outputFile.getParent());
            Files.write(outputFile, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public static byte[] makePrettyJson(byte[] jsonString)
    {
        StringBuilder sb = new StringBuilder(new String(jsonString, StandardCharsets.UTF_8));
        String        dataString;
        
        String rev = sb.reverse().toString().replaceAll("\n", "");
        if (rev.charAt(1) == ',')
        {
            dataString = new StringBuilder(rev).replace(1, 2, "").reverse().toString();
        } else
        {
            sb.reverse();
            dataString = sb.toString();
        }
        
        try
        {
            JsonElement obj    = JsonParser.parseString(dataString);
            String      pretty = getGson().toJson(obj);
            return pretty.getBytes(StandardCharsets.UTF_8);
        } catch (JsonSyntaxException e)
        {
            return jsonString;
        }
    }
}

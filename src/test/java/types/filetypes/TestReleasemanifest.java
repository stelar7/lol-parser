package types.filetypes;

import no.stelar7.cdragon.types.releasemanifest.ReleasemanifestParser;
import no.stelar7.cdragon.types.releasemanifest.data.ReleasemanifestDirectory;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.*;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class TestReleasemanifest
{
    
    @Test
    public void testReleasemanifest() throws IOException
    {
        ReleasemanifestParser parser = new ReleasemanifestParser();
        
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("releasemanifest (1)");
        System.out.println("Parsing: " + file.toString());
        
        ReleasemanifestDirectory parsed = parser.parse(file);
        List<String>             lines  = parsed.printLines("http://l3cdn.riotgames.com/releases/pbe/projects/lol_game_client/releases/0.0.0.1/files", ".compressed");
        
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(UtilHandler.DOWNLOADS_FOLDER.resolve("relmnf.log").toFile(), false));
        lines.stream()
             //.filter(l -> l.contains(".wad"))
             .sorted(new NaturalOrderComparator())
             .forEach(line -> {
                 try
                 {
                     bw.write(line);
                     bw.newLine();
                 } catch (IOException e)
                 {
                     e.printStackTrace();
                 }
             });
        bw.flush();
    }
    
    private void saveToHashlist(List<String> lines) throws IOException
    {
        Set<Vector2<String, String>> foundHasheSet = new HashSet<>();
        
        System.out.println("Loading hashes");
        HashHandler.getWadHashes("champions")
                   .entrySet()
                   .stream()
                   .map(e -> new Vector2<>(e.getKey(), e.getValue()))
                   .forEach(foundHasheSet::add);
        
        System.out.println("parsing manifest");
        lines.stream()
             .map(String::toLowerCase)
             .filter(l -> l.startsWith("/data/"))
             .map(l -> "assets" + l.substring(5))
             .map(l -> new Vector2<>(HashHandler.computeXXHash64(l), l))
             .forEach(foundHasheSet::add);
        
        List<Vector2<String, String>> foundHashes = new ArrayList<>(foundHasheSet);
        
        System.out.println("Sorting hashes");
        foundHashes.sort(Comparator.comparing(Vector2::getSecond, new NaturalOrderComparator()));
        
        System.out.println("Writing hashes");
        JsonWriterWrapper jsonWriter = new JsonWriterWrapper();
        jsonWriter.beginObject();
        for (Vector2<String, String> pair : foundHashes)
        {
            jsonWriter.name(pair.getFirst()).value(pair.getSecond());
        }
        jsonWriter.endObject();
        Files.write(HashHandler.WAD_HASH_STORE.resolve("champions" + ".json"), jsonWriter.toString().getBytes(StandardCharsets.UTF_8));
    }
}

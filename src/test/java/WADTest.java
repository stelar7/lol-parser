import net.jpountz.xxhash.*;
import no.stelar7.cdragon.wad.WADParser;
import no.stelar7.cdragon.wad.data.WADFile;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class WADTest
{
    @Test
    public void testWAD() throws Exception
    {
        WADParser parser = new WADParser();
        WADFile   file   = parser.parseLatest(Paths.get("C:\\Users\\Steffen\\Downloads"));
        
        file.extractFiles(Paths.get("C:\\Users\\Steffen\\Downloads"));
    }
    
    @Test
    public void testBuildHashList() throws IOException, InterruptedException
    {
        String pathPrefix = "plugins/rcp-be-lol-game-data/global/default/v1/champion-ability-icons/";
        Path   p          = Paths.get("champion-ability-icons.json");
        
        String result = "";
        
        Files.write(p, "{\n".getBytes(StandardCharsets.UTF_8));
        
        for (String ext : Arrays.asList(".json", ".txt", ".png", ".jpg", ".jpeg", ".webm", ".ogg", ".dds"))
        {
            System.out.println(pathPrefix + result + ext);
            String hash   = getHash(pathPrefix + result + ext);
            String data   = result + ext;
            Path   folder = Paths.get("C:\\Users\\Steffen\\Downloads\\unknown");
            
            if (Files.exists(folder.resolve(hash + ext)))
            {
                String fpa = pathPrefix.substring(7) + data;
                String js  = "\t\"" + hash + "\": \"" + fpa + "\",\n";
                Files.write(p, js.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            }
        }
        
        Files.write(p, "}".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
    }
    
//    private void perm(List<String> a, String result, int l, Path p, String pathPrefix) throws IOException
//    {
//        if (result.length() == l)
//        {
//
//            for (String ext : Arrays.asList(".json", ".txt", ".png", ".jpg", ".jpeg", ".webm", ".ogg", ".dds"))
//            {
//                String hash   = getHash(pathPrefix + result + ext);
//                String data   = result + ext;
//                Path   folder = Paths.get("C:\\Users\\Steffen\\Downloads\\unknown");
//
//                if (Files.exists(folder.resolve(hash + ext)))
//                {
//                    String fpa = pathPrefix.substring(7) + data;
//                    String js  = "\t\"" + hash + "\": \"" + fpa + "\",\n";
//                    Files.write(p, js.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
//                }
//            }
//
//            return;
//        }
//        for (int i = 0; i < a.size(); i++)
//        {
//            String nr = result + a.get(i);
//            perm(a, nr, l, p, pathPrefix);
//        }
//    }
    
    
    private String getHash(String text) throws IOException
    {
        XXHashFactory        factory = XXHashFactory.fastestInstance();
        byte[]               data    = text.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream in      = new ByteArrayInputStream(data);
        
        StreamingXXHash64 hash64 = factory.newStreamingHash64(0);
        byte[]            buf    = new byte[8];
        for (; ; )
        {
            int read = in.read(buf);
            if (read == -1)
            {
                break;
            }
            hash64.update(buf, 0, read);
        }
        return String.format("%016X", hash64.getValue()).toLowerCase(Locale.ENGLISH);
    }
}
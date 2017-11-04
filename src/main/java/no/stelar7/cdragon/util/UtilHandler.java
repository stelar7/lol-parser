package no.stelar7.cdragon.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public final class UtilHandler
{
    
    private UtilHandler()
    {
        // Hide public constructor
    }
    
    private static Map<String, String>           hashNames;
    private static Map<ByteArrayWrapper, String> magicNumbers;
    
    public static synchronized Map<String, String> getKnownFileHashes()
    {
        if (hashNames == null)
        {
            System.out.println("Loading known hashes");
            try
            {
                StringBuilder sb = new StringBuilder();
                
                try (InputStream is = new URL("https://raw.githubusercontent.com/CommunityDragon/RADS-CDragon/Python-Version/wad_parser/hashes.json").openConnection().getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                     Stream<String> stream = reader.lines())
                {
                    stream.forEach(sb::append);
                }
                
                hashNames = new Gson().fromJson(sb.toString(), new TypeToken<Map<String, String>>() {}.getType());
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        return hashNames;
    }
    
    public static synchronized Map<ByteArrayWrapper, String> getMagicNumbers()
    {
        if (magicNumbers == null)
        {
            System.out.println("Loading JMimeMagic unknown types");
            
            ByteArrayWrapper oggMagic  = new ByteArrayWrapper(new byte[]{79, 103, 103, 83});
            ByteArrayWrapper webmMagic = new ByteArrayWrapper(new byte[]{26, 69, -33, -93});
            ByteArrayWrapper ddsMagic  = new ByteArrayWrapper(new byte[]{68, 68, 83, 32});
            
            magicNumbers = new HashMap<>();
            magicNumbers.put(oggMagic, "ogg");
            magicNumbers.put(webmMagic, "webm");
            magicNumbers.put(ddsMagic, "dds");
            
        }
        
        return magicNumbers;
    }
    
    public static boolean tryDownloadVersion(Path output, String url, int min, int max) throws IOException
    {
        for (int i = max; i >= min; i--)
        {
            String finalUrl = String.format(url, i);
            
            HttpURLConnection con = (HttpURLConnection) new URL(finalUrl).openConnection();
            if (con.getResponseCode() == 200)
            {
                con.disconnect();
                
                System.out.println("Downloading file: " + finalUrl);
                ReadableByteChannel rbc = Channels.newChannel(new URL(finalUrl).openStream());
                FileOutputStream    fos = new FileOutputStream(output.toFile());
                
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                return true;
            }
        }
        return false;
    }
}

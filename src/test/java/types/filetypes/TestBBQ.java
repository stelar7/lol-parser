package types.filetypes;

import no.stelar7.cdragon.types.bbq.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class TestBBQ
{
    BBQParser parser = new BBQParser();
    
    @Test
    public void testBBQ() throws IOException
    {
        Path file = UtilHandler.CDRAGON_FOLDER.resolve("C:\\Users\\Steffen\\Desktop\\unitypack\\UnityPack-master\\alwaysloaded.bbq");
        Files.createDirectories(file.resolveSibling("generated"));
        BBQFile data = parser.parse(file);
        
        for (BBQAsset entry : data.getEntries())
        {
            for (BBQObjectInfo value : entry.getObjects().values())
            {
                if (value.getType().equals("Texture2D"))
                {
                    Map<String, Object> temp   = (Map<String, Object>) value.read();
                    BBQTextureFormat    format = BBQTextureFormat.getFromId((Integer) temp.get("m_TextureFormat"));
                    
                    Map<String, Object> streamData = (Map<String, Object>) temp.get("m_StreamData");
                    int                 offset     = (int) streamData.get("offset");
                    int                 size       = (int) streamData.get("size");
                    String              path       = (String) streamData.get("path");
                    path = path.substring(path.lastIndexOf('/') + 1);
                    
                    String             finalPath = path;
                    Optional<BBQAsset> first     = data.getEntries().stream().filter(e -> e.getName().equals(finalPath)).findFirst();
                    if (first.isEmpty())
                    {
                        System.out.println("Missing reference to asset: " + path);
                    }
                    
                    // TODO
                    BBQAsset source = first.get();
                    source.buf.seek(13527188 + offset);
                    byte[] dataBytes = source.buf.readBytes(size);
                    
                    System.out.println();
                }
                System.out.println(value.getType());
            }
        }
    }
}

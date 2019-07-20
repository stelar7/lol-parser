package types.filetypes;

import no.stelar7.cdragon.types.ktx.ktx.KTX11Parser;
import no.stelar7.cdragon.types.ktx.ktx.data.KTX11File;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;

public class TestKTX
{
    @Test
    public void testKTX() throws IOException
    {
        KTX11Parser parser = new KTX11Parser();
        
        Files.createDirectories(UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\ktx\\output"));
        Path file = UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\ktx\\test2.ktx");
        System.out.println("Parsing: " + file.toString());
        
        KTX11File data = parser.parse(file);
        for (int i = 0; i < data.getHeader().getNumberOfMipmapLevels(); i++)
        {
            byte[] encodedData = data.getMipMaps().getTextureData().get(i).getData();
            Files.write(UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\ktx\\output\\parsed_" + i + ".etc1"), encodedData);
            
            data.toImage(i, UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\ktx\\output\\parsed_bmp_" + i + ".bmp"));
        }
        
    }
}

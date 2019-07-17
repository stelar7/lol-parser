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
        
        Path file = UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\ktx\\test.ktx");
        System.out.println("Parsing: " + file.toString());
        
        KTX11File data = parser.parse(file);
        for (int i = 0; i < data.getHeader().getNumberOfMipmapLevels(); i++)
        {
            byte[] textureData = data.getMipMaps().getTextureData().get(i);
            Files.write(UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\ktx\\parsed_" + i + ".etc1"), textureData);
            data.toImage(i, UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\ktx\\parsed_bmp_" + i + ".bmp"));
        }
        
    }
}

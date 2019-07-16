package types.filetypes;

import no.stelar7.cdragon.types.ktx.ktx.KTX11Parser;
import no.stelar7.cdragon.types.ktx.ktx.data.KTX11File;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;

public class TestKTX
{
    @Test
    public void testKTX() throws IOException
    {
        KTX11Parser parser = new KTX11Parser();
        
        Path file = UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\ktx\\red_brambleback_background.dds");
        System.out.println("Parsing: " + file.toString());
        
        KTX11File data = parser.parse(file);
        for (int i = 0; i < data.getHeader().getNumberOfMipmapLevels(); i++)
        {
            Files.write(UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\ktx\\parsed_" + i + ".etc1"), data.getMipMaps().getTextureData().get(i));
            
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data.getMipMaps().getTextureData().get(i)));
            System.out.println();
        }
        
    }
}

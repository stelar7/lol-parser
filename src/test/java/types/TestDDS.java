package types;

import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;

public class TestDDS
{
    
    @Test
    public void testDDS()
    {
        try
        {
            DDSParser parser = new DDSParser();
            
            Path file   = UtilHandler.DOWNLOADS_FOLDER.resolve("MordekaiserLoadScreen_5.dds.compressed");
            Path output = file.resolveSibling(UtilHandler.pathToFilename(file) + ".png");
            System.out.println("Parsing: " + file.toString());
            
            BufferedImage image = parser.parseCompressed(file);
            ImageIO.write(image, "png", output.toFile());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

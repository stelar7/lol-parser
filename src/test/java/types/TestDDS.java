package types;

import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class TestDDS
{
    
    @Test
    public void testDDS()
    {
        try
        {
            DDSParser parser = new DDSParser();
            
            Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("nid");
            
            Files.walkFileTree(file, new SimpleFileVisitor<>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    BufferedImage img;
                    if (file.toString().endsWith(".dds.compressed"))
                    {
                        img = parser.parseCompressed(file);
                    } else if (file.toString().endsWith(".dds"))
                    {
                        img = parser.parse(file);
                    } else
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    ImageIO.write(img, "png", file.resolve("../png/" + UtilHandler.pathToFilename(file) + ".png").normalize().toFile());
                    return FileVisitResult.CONTINUE;
                }
            });
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

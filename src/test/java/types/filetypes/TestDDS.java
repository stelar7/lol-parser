package types.filetypes;

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
            
            Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("temp");
            
            Files.walkFileTree(file, new SimpleFileVisitor<>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    BufferedImage img = null;
                    if (file.toString().endsWith(".dds.compressed"))
                    {
                        img = parser.parseCompressed(file);
                    } else if (file.toString().endsWith(".dds"))
                    {
                        img = parser.parse(file);
                    }
                    
                    if (img == null)
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    Path output = UtilHandler.DOWNLOADS_FOLDER.resolve("png/" + UtilHandler.pathToFilename(file) + ".png");
                    Files.createDirectories(output.getParent());
                    
                    ImageIO.write(img, "png", output.toFile());
                    return FileVisitResult.CONTINUE;
                }
            });
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testSingle() throws IOException
    {
        DDSParser     parser = new DDSParser();
        Path          file   = UtilHandler.DOWNLOADS_FOLDER.resolve("Baron_Minimap_Icon.DDS.compressed");
        BufferedImage img    = parser.parseCompressed(file);
        ImageIO.write(img, "png", file.resolveSibling("Baron_Minimap_Icon.png").toFile());
    }
}

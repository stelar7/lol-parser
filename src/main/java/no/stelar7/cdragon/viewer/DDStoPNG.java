package no.stelar7.cdragon.viewer;

import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.util.handlers.UtilHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;

public class DDStoPNG
{
    public static void main(String[] args) throws IOException
    {
        DDSParser parser = new DDSParser();
        
        if (args.length != 2)
        {
            System.out.println("Needs 2 parameters! (skn file, and output folder)");
        }
        
        Path output = Paths.get(args[1]);
        if (!Files.isDirectory(output))
        {
            System.out.println("Output is not a folder");
        }
        
        Path          path = Paths.get(args[0]);
        BufferedImage img  = parser.parse(path);
        
        Path outputPath = output.resolve(UtilHandler.pathToFilename(path) + ".png");
        ImageIO.write(img, "png", outputPath.toFile());
        System.out.println("Saved file: " + outputPath.toAbsolutePath().toString());
    }
}

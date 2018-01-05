package no.stelar7.cdragon.types.dds;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class DDSParser
{
    public BufferedImage parse(Path path)
    {
        try
        {
            return ImageIO.read(path.toFile());
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}

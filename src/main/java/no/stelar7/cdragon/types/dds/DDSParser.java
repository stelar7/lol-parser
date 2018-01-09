package no.stelar7.cdragon.types.dds;


import no.stelar7.cdragon.util.CompressionHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;

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
    
    public BufferedImage parseCompressed(Path path)
    {
        try
        {
            byte[] dataBytes = CompressionHandler.uncompressDEFLATE(Files.readAllBytes(path));
            return ImageIO.read(new ByteArrayInputStream(dataBytes));
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}

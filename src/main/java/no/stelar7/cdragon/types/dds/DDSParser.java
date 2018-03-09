package no.stelar7.cdragon.types.dds;


import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.util.handlers.CompressionHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;

public class DDSParser implements Parseable<BufferedImage>
{
    @Override
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
    
    @Override
    public BufferedImage parse(byte[] data)
    {
        try
        {
            return ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public BufferedImage parse(RandomAccessReader raf)
    {
        return parse(raf.readRemaining());
    }
    
    public BufferedImage parseCompressed(Path path)
    {
        try
        {
            byte[] dataBytes = CompressionHandler.uncompress(Files.readAllBytes(path));
            return parse(dataBytes);
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}

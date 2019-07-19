package no.stelar7.cdragon.types.dds;


import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.ktx.ktx.KTX11Parser;
import no.stelar7.cdragon.util.handlers.CompressionHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

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
            return parse(new ByteArray(Files.readAllBytes(path)));
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    KTX11Parser parser = new KTX11Parser();
    
    @Override
    public BufferedImage parse(ByteArray data)
    {
        try
        {
            if (data.startsWith(new ByteArray(new byte[]{(byte) 0xAB, 0x4B, 0x54, 0x58, 0x20, 0x31, 0x31, (byte) 0xBB, 0x0D, 0x0A, 0x1A, 0x0A})))
            {
                data = new ByteArray(parser.parse(data).getMipMaps().getTextureData().get(0).getData());
            }
            
            return ImageIO.read(new ByteArrayInputStream(data.getData()));
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public BufferedImage parse(RandomAccessReader raf)
    {
        return parse(raf.readToByteArray());
    }
    
    public BufferedImage parseCompressed(Path path)
    {
        try
        {
            byte[] dataBytes = CompressionHandler.uncompress(Files.readAllBytes(path));
            return parse(new ByteArray(dataBytes));
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}

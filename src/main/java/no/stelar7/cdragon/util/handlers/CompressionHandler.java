package no.stelar7.cdragon.util.handlers;

import com.github.luben.zstd.Zstd;
import net.jpountz.lz4.*;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public final class CompressionHandler
{
    
    private CompressionHandler()
    {
        // Hide public constructor
    }
    
    public static byte[] uncompressLZ4(byte[] input, int outputSize)
    {
        LZ4Factory          factory      = LZ4Factory.fastestInstance();
        LZ4FastDecompressor decompressor = factory.fastDecompressor();
        
        return decompressor.decompress(input, outputSize);
    }
    
    public static byte[] uncompressLZMA(byte[] input)
    {
        try (LZMACompressorInputStream lzma = new LZMACompressorInputStream(new ByteArrayInputStream(input));
             ByteArrayOutputStream bos = new ByteArrayOutputStream())
        {
            lzma.transferTo(bos);
            return bos.toByteArray();
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void uncompressDEFLATE(Path inputPath, Path uncompressPath)
    {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(Files.readAllBytes(inputPath));
             InflaterInputStream in = new InflaterInputStream(bis)
        )
        {
            Files.createDirectories(uncompressPath.getParent());
            Files.copy(in, uncompressPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public static byte[] uncompressDEFLATE(byte[] input)
    {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(input);
             InflaterInputStream in = new InflaterInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()
        )
        {
            int    read;
            byte[] data = new byte[4096];
            
            while ((read = in.read(data, 0, data.length)) != -1)
            {
                bos.write(data, 0, read);
            }
            bos.flush();
            
            return bos.toByteArray();
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static byte[] uncompressGZIP(byte[] data)
    {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             GZIPInputStream input = new GZIPInputStream(bis);
             ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            byte[] buffer = new byte[1024];
            
            int length;
            while ((length = input.read(buffer)) != -1)
            {
                output.write(buffer, 0, length);
            }
            output.flush();
            return output.toByteArray();
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static byte[] uncompressZSTD(byte[] fileBytes, int originalSize)
    {
        try
        {
            return Zstd.decompress(fileBytes, originalSize);
        } catch (RuntimeException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static byte[] uncompress(byte[] bytes)
    {
        if (FileTypeHandler.isProbableGZIP(bytes))
        {
            return uncompressGZIP(bytes);
        }
        
        if (FileTypeHandler.isProbableZSTD(bytes))
        {
            return uncompressZSTD(bytes, bytes.length * 6);
        }
        
        if (FileTypeHandler.isProbableDEFLATE(bytes))
        {
            return uncompressDEFLATE(bytes);
        }
        
        return null;
    }
}
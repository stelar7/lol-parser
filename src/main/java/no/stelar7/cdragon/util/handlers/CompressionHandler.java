package no.stelar7.cdragon.util.handlers;

import com.github.luben.zstd.*;
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
    
    /**
     * Decompresses a classic .lzma stream (13 byte header: 5 bytes of properties + 8 byte uncompressed size)
     */
    public static byte[] uncompressLZMA(byte[] input)
    {
        try (LZMACompressorInputStream lzma = new LZMACompressorInputStream(new ByteArrayInputStream(input)))
        {
            return lzma.readAllBytes();
        } catch (IOException e)
        {
            throw new UncheckedIOException("Failed to decompress LZMA data", e);
        }
    }

    /**
     * Decompresses LZMA data that starts with the 5 property bytes (lc/lp/pb byte + little-endian dictionary size)
     * but has no size field, like the data in Unity asset bundles.
     * The uncompressed size has to be known up front.
     */
    public static byte[] uncompressLZMA(byte[] input, int uncompressedSize)
    {
        if (input.length < 5)
        {
            throw new IllegalArgumentException("LZMA data is too short to contain the properties header");
        }

        byte propsByte = input[0];
        int  dictSize  = (input[1] & 0xFF) | ((input[2] & 0xFF) << 8) | ((input[3] & 0xFF) << 16) | ((input[4] & 0xFF) << 24);

        try (org.tukaani.xz.LZMAInputStream lzma = new org.tukaani.xz.LZMAInputStream(new ByteArrayInputStream(input, 5, input.length - 5), uncompressedSize, propsByte, dictSize))
        {
            byte[] output = lzma.readNBytes(uncompressedSize);
            if (output.length != uncompressedSize)
            {
                throw new IOException("LZMA data ended after " + output.length + " of " + uncompressedSize + " bytes");
            }
            return output;
        } catch (IOException e)
        {
            throw new UncheckedIOException("Failed to decompress LZMA data", e);
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
    
    /**
     * Decompresses zstd data when the uncompressed size is not known.
     * Streams the data, so it also works for several concatenated frames and frames that do not store their size.
     */
    public static byte[] uncompressZSTD(byte[] fileBytes)
    {
        try (ZstdInputStream input = new ZstdInputStream(new ByteArrayInputStream(fileBytes)))
        {
            return input.readAllBytes();
        } catch (IOException e)
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
            return uncompressZSTD(bytes);
        }
        
        if (FileTypeHandler.isProbableDEFLATE(bytes))
        {
            return uncompressDEFLATE(bytes);
        }
        
        return null;
    }
}
package no.stelar7.cdragon.util;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public final class CompressionHandler
{
    
    private CompressionHandler()
    {
        // Hide public constructor
    }
    
    public static byte[] uncompressGZIP(byte[] data) throws IOException
    {
        return transferBuffer(new GZIPInputStream(new ByteArrayInputStream(data)));
    }
    
    public static void uncompressDEFLATE(byte[] data, Path uncompressPath) throws IOException
    {
        Files.copy(new InflaterInputStream(new ByteArrayInputStream(data)), uncompressPath, StandardCopyOption.REPLACE_EXISTING);
    }
    
    private static byte[] transferBuffer(FilterInputStream input) throws IOException
    {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            byte[] buffer = new byte[1024];
            
            int length;
            while ((length = input.read(buffer)) != -1)
            {
                output.write(buffer, 0, length);
            }
            output.flush();
            return output.toByteArray();
        }
    }
}
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
    
    
    public static void uncompressDEFLATE(Path inputPath, Path uncompressPath) throws IOException
    {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(Files.readAllBytes(inputPath));
             InflaterInputStream in = new InflaterInputStream(bis)
        )
        {
            Files.copy(in, uncompressPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    public static byte[] uncompressGZIP(byte[] data) throws IOException
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
        }
    }
}
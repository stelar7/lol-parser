package types.util;

import no.stelar7.cdragon.util.handlers.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class TestUncompress
{
    @Test
    public void testAll() throws IOException
    {
        Files.walkFileTree(Paths.get(System.getProperty("user.home"), "Downloads"), new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                if (file.toString().endsWith(".compressed"))
                {
                    CompressionHandler.uncompressDEFLATE(file, UtilHandler.DOWNLOADS_FOLDER.resolve("decompressed\\" + UtilHandler.pathToFilename(file)));
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
package types.filetypes;

import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.Test;

import java.nio.ByteOrder;
import java.nio.file.*;

public class TestTGA
{
    @Test
    public void testTGA()
    {
        Path      path = UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\8d60a44bb6212a0d.tga");
        ByteArray data = new ByteArray(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN).readRemaining());
        System.out.println(FileTypeHandler.isProbableTGA(data));
    }
}

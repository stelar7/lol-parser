package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;

public class BBQAsset
{
    public BBQAsset(BBQBundleEntry entry, byte[] input)
    {
        if (entry.getName().endsWith(".resource") || entry.getName().endsWith(".resS"))
        {
            return;
        }
        
        RandomAccessReader raf        = new RandomAccessReader(input, ByteOrder.BIG_ENDIAN);
        int                metaSize   = raf.readInt();
        int                fileSize   = raf.readInt();
        int                format     = raf.readInt();
        int                dataOffset = raf.readInt();
        
        System.out.println();
    }
}

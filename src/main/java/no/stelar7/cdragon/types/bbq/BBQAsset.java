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
        
        if (format >= 9) {
            int endian = raf.readInt();
            if(endian == 0) {
                raf.setEndian(ByteOrder.LITTLE_ENDIAN);
            }
        }
        
        if(format >= 7 && format <= 13) {
            boolean longObjectIds = raf.readInt() > 0;
        }
        
        int objectCount = raf.readInt();
        for (int i = 0; i < objectCount; i++)
        {
            if (format >= 14) {
                System.out.println("TODO: align to byte boundary?");
            }
        }
        
        
        System.out.println();
    }
}

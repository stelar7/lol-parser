package no.stelar7.cdragon.types.ogg.data;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.io.File;
import java.nio.ByteOrder;

public class CodebookLibrary
{
    
    private int   codebookCount;
    private int[] codebookOffsets;
    
    private byte[] codebookData;
    
    public CodebookLibrary()
    {
        String codebook     = getClass().getClassLoader().getResource("codebook/packed_codebooks_aoTuV_603.bin").getFile();
        File   codebookFile = new File(codebook);
        
        RandomAccessReader raf = RandomAccessReader.create(codebookFile, ByteOrder.LITTLE_ENDIAN);
        
        int streamSize = raf.remaining();
        
        raf.seek(streamSize - 4);
        int offsetOffset = raf.readInt();
        
        codebookCount = (streamSize - offsetOffset) / 4;
        codebookOffsets = new int[codebookCount];
        raf.seek(0);
        
        codebookData = raf.readBytes(offsetOffset);
        
        for (int i = 0; i < codebookCount; i++)
        {
            codebookOffsets[i] = raf.readInt();
        }
        
    }
    
    public void rebuild(int codeId, OGGStream stream)
    {
        byte[] codebook     = getCodebook(codeId);
        int    codebookSize = getCodebookSize(codeId);
        
        if (codebookSize == 0xFFFFFFFF || codebook == null)
        {
            throw new IllegalArgumentException("Invalid codebook index");
        }
        
        rebuild(new RandomAccessReader(codebook, ByteOrder.LITTLE_ENDIAN), codebookSize, stream);
        
    }
    
    
    public void rebuild(RandomAccessReader bitStream, int codebookSize, OGGStream ogg)
    {
        byte  dimensions = (byte) bitStream.readBits(4);
        short entries    = (short) bitStream.readBits(14);
        
        
        ogg.bitWrite(0x564342, 24);
        ogg.bitWrite(dimensions, 16);
        ogg.bitWrite(entries, 24);
        
        int orderedFlag = bitStream.readBits(1);
        ogg.writeBit(orderedFlag);
        
        if (orderedFlag == 1)
        {
            byte initLength = (byte) bitStream.readBits(5);
            ogg.bitWrite(initLength, 5);
            
            
            int currentEntry = 0;
            while (currentEntry < entries)
            {
                int bitCount = UtilHandler.iLog(entries - currentEntry);
                int number   = bitStream.readBits(bitCount);
                ogg.bitWrite(number, bitCount);
                currentEntry += number;
            }
            
            if (currentEntry > entries)
            {
                throw new IllegalArgumentException("Failed to rebuild codebook");
            }
        } else
        {
            byte codewordLengthLength = (byte) bitStream.readBits(3);
            byte sparse               = (byte) bitStream.readBits(1);
            ogg.writeBit(sparse);
            
            if (codewordLengthLength == 0 || codewordLengthLength > 5)
            {
                throw new IllegalArgumentException("Failed to rebuild codebook");
            }
            
            for (int i = 0; i < entries; i++)
            {
                boolean present = true;
                
                if (sparse == 1)
                {
                    byte sparsePresence = (byte) bitStream.readBits(1);
                    ogg.writeBit(sparsePresence);
                    
                    present = (sparsePresence == 1);
                }
                
                if (present)
                {
                    byte codewordLength = (byte) bitStream.readBits(codewordLengthLength);
                    ogg.bitWrite(codewordLength, 5);
                }
            }
        }
        
        byte lookupType = (byte) bitStream.readBits(1);
        ogg.bitWrite(lookupType, 4);
        if (lookupType == 1)
        {
            ogg.bitWrite(bitStream.readBits(32), 32);
            ogg.bitWrite(bitStream.readBits(32), 32);
            
            byte valueLength = (byte) bitStream.readBits(4);
            ogg.bitWrite(valueLength, 4);
            ogg.writeBit(bitStream.readBits(1));
            
            int quantValues = getQuantValues(entries, dimensions);
            for (int i = 0; i < quantValues; i++)
            {
                int value = bitStream.readBits(valueLength + 1);
                ogg.bitWrite(value, (byte) (valueLength + 1));
            }
        } else if (lookupType != 0)
        {
            throw new IllegalArgumentException("Failed to rebuild codebook");
        }
        
        if (codebookSize != 0 && (bitStream.getTotalBitsRead() / 8) + 1 != codebookSize)
        {
            throw new IllegalArgumentException("Failed to rebuild codebook");
        }
    }
    
    
    private int getQuantValues(int entries, int dimensions)
    {
        int bits   = UtilHandler.iLog(entries);
        int values = entries >> (bits - 1) * (dimensions - 1) / dimensions;
        
        while (true)
        {
            long acc  = 1;
            long acc1 = 1;
            
            for (int i = 0; i < dimensions; i++)
            {
                acc *= values;
                acc1 *= values + 1;
            }
            
            if (acc <= entries && acc1 > entries)
            {
                return values;
            } else
            {
                if (acc > entries)
                {
                    values--;
                } else
                {
                    values++;
                }
            }
        }
    }
    
    private int getCodebookSize(int index)
    {
        if (codebookData == null || codebookOffsets == null)
        {
            throw new IllegalArgumentException("Codebook not initialized");
        }
        
        if (index >= codebookCount - 1)
        {
            return 0xFFFFFFFF;
        }
        
        return codebookOffsets[index + 1] - codebookOffsets[index];
    }
    
    private byte[] getCodebook(int index)
    {
        if (codebookData == null || codebookOffsets == null)
        {
            throw new IllegalArgumentException("Codebook not initialized");
        }
        
        if (index >= codebookCount - 1)
        {
            return null;
        }
        
        byte[] returnData = new byte[getCodebookSize(index)];
        System.arraycopy(codebookData, codebookOffsets[index], returnData, 0, returnData.length);
        
        
        return returnData;
    }
    
    public void copy(RandomAccessReader bitStream, OGGStream ogg)
    {
        byte[] id = new byte[]{
                (byte) bitStream.readBits(8),
                (byte) bitStream.readBits(8),
                (byte) bitStream.readBits(8)
        };
        
        short dimensions = (short) bitStream.readBits(16);
        int   entries    = bitStream.readBits(24);
        
        if (id[0] != 0x42 || id[0] != 0x43 || id[0] != 0x56)
        {
            throw new IllegalArgumentException("Invalid codebook sync pattern");
        }
        
        ogg.bitWrite(id[0], Byte.SIZE);
        ogg.bitWrite(id[1], Byte.SIZE);
        ogg.bitWrite(id[2], Byte.SIZE);
        
        ogg.bitWrite(dimensions, Short.SIZE);
        ogg.bitWrite(entries, 24);
        
        byte orderedFlag = (byte) bitStream.readBits(1);
        ogg.writeBit(orderedFlag);
        if (orderedFlag == 1)
        {
            byte initLength = (byte) bitStream.readBits(5);
            ogg.bitWrite(initLength, 5);
            
            
            int currentEntry = 0;
            while (currentEntry < entries)
            {
                int bitCount = UtilHandler.iLog(entries - currentEntry);
                int number   = bitStream.readBits(bitCount);
                ogg.bitWrite(number, bitCount);
                currentEntry += number;
            }
            
            if (currentEntry > entries)
            {
                throw new IllegalArgumentException("Failed to rebuild codebook");
            }
        } else
        {
            byte sparse = (byte) bitStream.readBits(1);
            ogg.writeBit(sparse);
            
            for (int i = 0; i < entries; i++)
            {
                if (sparse == 1)
                {
                    byte sparsePresence = (byte) bitStream.readBits(1);
                    ogg.writeBit(sparsePresence);
                } else
                {
                    byte codewordLength = (byte) bitStream.readBits(5);
                    ogg.bitWrite(codewordLength, 5);
                }
            }
        }
        
        
        byte lookupType = (byte) bitStream.readBits(4);
        ogg.bitWrite(lookupType, 4);
        if (lookupType == 1)
        {
            ogg.bitWrite(bitStream.readBits(32), 32);
            ogg.bitWrite(bitStream.readBits(32), 32);
            
            byte valueLength = (byte) bitStream.readBits(4);
            ogg.bitWrite(valueLength, 4);
            ogg.writeBit(bitStream.readBits(1));
            
            int quantValues = getQuantValues(entries, dimensions);
            for (int i = 0; i < quantValues; i++)
            {
                int value = bitStream.readBits(valueLength + 1);
                ogg.bitWrite(value, (byte) (valueLength + 1));
            }
        } else
        {
            throw new IllegalArgumentException("Failed to rebuild codebook");
        }
    }
}

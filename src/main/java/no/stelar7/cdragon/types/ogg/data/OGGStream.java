package no.stelar7.cdragon.types.ogg.data;

import com.google.common.io.*;
import no.stelar7.cdragon.util.handlers.HashHandler;

import java.nio.*;
import java.util.Arrays;

public class OGGStream
{
    private static final int HEADER_SIZE   = 0x1B;
    private static final int SEGMENT_COUNT = 0xFF;
    private static final int SEGMENT_SIZE  = 0xFF;
    
    private byte bitBuffer;
    private int  bitsStored;
    
    private int payloadBytes;
    
    private boolean first = true;
    private boolean continued;
    
    private byte[] pageBuffer = new byte[HEADER_SIZE + SEGMENT_COUNT + (SEGMENT_SIZE * SEGMENT_COUNT)];
    
    private int granule;
    private int sequenceNumber;
    
    private ByteArrayDataOutput data = ByteStreams.newDataOutput();
    
    public OGGStream()
    {
        flushPage(false, false);
    }
    
    public static int getHeaderSize()
    {
        return HEADER_SIZE;
    }
    
    public static int getSegmentCount()
    {
        return SEGMENT_COUNT;
    }
    
    public static int getSegmentSize()
    {
        return SEGMENT_SIZE;
    }
    
    public byte getBitBuffer()
    {
        return bitBuffer;
    }
    
    public void setBitBuffer(byte bitBuffer)
    {
        this.bitBuffer = bitBuffer;
    }
    
    public int getBitsStored()
    {
        return bitsStored;
    }
    
    public void setBitsStored(int bitsStored)
    {
        this.bitsStored = bitsStored;
    }
    
    public int getPayloadBytes()
    {
        return payloadBytes;
    }
    
    public void setPayloadBytes(int payloadBytes)
    {
        this.payloadBytes = payloadBytes;
    }
    
    public boolean isFirst()
    {
        return first;
    }
    
    public void setFirst(boolean first)
    {
        this.first = first;
    }
    
    public boolean isContinued()
    {
        return continued;
    }
    
    public void setContinued(boolean continued)
    {
        this.continued = continued;
    }
    
    public byte[] getPageBuffer()
    {
        return pageBuffer;
    }
    
    public void setPageBuffer(byte[] pageBuffer)
    {
        this.pageBuffer = pageBuffer;
    }
    
    public int getGranule()
    {
        return granule;
    }
    
    public int getSequenceNumber()
    {
        return sequenceNumber;
    }
    
    public void setSequenceNumber(int sequenceNumber)
    {
        this.sequenceNumber = sequenceNumber;
    }
    
    public ByteArrayDataOutput getData()
    {
        return data;
    }
    
    public void setData(ByteArrayDataOutput data)
    {
        this.data = data;
    }
    
    public void flushPage(boolean nextContinued, boolean last)
    {
        if (payloadBytes != SEGMENT_COUNT * SEGMENT_SIZE)
        {
            flushBits();
        }
        
        if (payloadBytes != 0)
        {
            int segments = (payloadBytes + SEGMENT_SIZE) / SEGMENT_SIZE;
            if (segments == SEGMENT_COUNT + 1)
            {
                segments = SEGMENT_COUNT;
            }
            
            for (int i = 0; i < payloadBytes; i++)
            {
                pageBuffer[HEADER_SIZE + segments + i] = pageBuffer[HEADER_SIZE + SEGMENT_COUNT + i];
            }
            
            pageBuffer[0] = 'O';
            pageBuffer[1] = 'g';
            pageBuffer[2] = 'g';
            pageBuffer[3] = 'S';
            pageBuffer[4] = 0;
            pageBuffer[5] = (byte) ((continued ? 1 : 0) | (first ? 2 : 0) | (last ? 4 : 0));
            
            System.arraycopy(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(granule).array(), 0, pageBuffer, 6, 4);
            
            Arrays.fill(pageBuffer, 10, 14, (byte) ((granule == 0xFFFFFFFF) ? 0xFF : 0));
            
            pageBuffer[14] = 1;
            
            System.arraycopy(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sequenceNumber).array(), 0, pageBuffer, 18, 4);
            
            Arrays.fill(pageBuffer, 22, 26, (byte) 0);
            
            pageBuffer[26] = (byte) segments;
            
            for (int i = 0, bytesLeft = payloadBytes; i < segments; i++)
            {
                if (bytesLeft >= SEGMENT_SIZE)
                {
                    bytesLeft -= SEGMENT_SIZE;
                    pageBuffer[27 + i] = (byte) SEGMENT_SIZE;
                } else
                {
                    pageBuffer[27 + i] = (byte) bytesLeft;
                }
            }
            
            int crc = (int) HashHandler.computeCCITT32(pageBuffer, HEADER_SIZE + segments + payloadBytes);
            System.arraycopy(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(crc).array(), 0, pageBuffer, 22, 4);
            
            for (int i = 0; i < HEADER_SIZE + segments + payloadBytes; i++)
            {
                data.write(pageBuffer[i]);
            }
            
            sequenceNumber++;
            first = false;
            continued = nextContinued;
            payloadBytes = 0;
        }
    }
    
    private void flushBits()
    {
        if (bitsStored != 0)
        {
            if (payloadBytes == SEGMENT_SIZE * SEGMENT_COUNT)
            {
                throw new IllegalArgumentException("OGGPacket out of space");
            }
            
            pageBuffer[HEADER_SIZE + SEGMENT_COUNT + payloadBytes] = bitBuffer;
            payloadBytes++;
            
            bitBuffer = 0;
            bitsStored = 0;
        }
    }
    
    public void writeVorbisHeader(int i)
    {
        bitWrite(i, Byte.SIZE);
        bitWriteChars("vorbis");
    }
    
    public void bitWriteChars(String value)
    {
        for (int j = 0; j < value.length(); j++)
        {
            bitWrite(value.charAt(j), Byte.SIZE);
        }
    }
    
    public void bitWriteLengthAndString(String value)
    {
        bitWrite(value.length(), Integer.SIZE);
        for (int j = 0; j < value.length(); j++)
        {
            bitWrite(value.charAt(j), Byte.SIZE);
        }
    }
    
    public void bitWrite(int value, int size)
    {
        for (int i = 0; i < size; i++)
        {
            writeBit((value & (1 << i)) != 0 ? (byte) 1 : (byte) 0);
        }
    }
    
    public void writeBit(int bit)
    {
        if (bit == 1)
        {
            bitBuffer |= (byte) 1 << bitsStored;
        }
        
        bitsStored++;
        
        if (bitsStored == 8)
        {
            flushBits();
        }
    }
    
    public void setGranule(int granule)
    {
        this.granule = granule;
    }
}

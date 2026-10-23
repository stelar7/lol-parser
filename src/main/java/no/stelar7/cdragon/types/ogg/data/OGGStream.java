package no.stelar7.cdragon.types.ogg.data;

import com.google.common.io.*;
import no.stelar7.cdragon.util.readers.ParseException;

public class OGGStream
{
    private static final int HEADER_BYTES = 0x1B;
    private static final int MAX_SEGMENTS = 0xFF;
    private static final int SEGMENT_SIZE = 0xFF;
    
    private byte bitBuffer;
    private int  bitsStored;
    
    private int payloadBytes;
    
    private boolean first = true;
    private boolean continued;
    
    private byte[] pageBuffer = new byte[HEADER_BYTES + MAX_SEGMENTS + (SEGMENT_SIZE * MAX_SEGMENTS)];
    
    private int granule;
    private int sequenceNumber;
    
    private ByteArrayDataOutput data = ByteStreams.newDataOutput();
    
    public static int getHeaderBytes()
    {
        return HEADER_BYTES;
    }
    
    public static int getMaxSegments()
    {
        return MAX_SEGMENTS;
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
        if (payloadBytes != (SEGMENT_SIZE * MAX_SEGMENTS))
        {
            flushBits();
        }
        
        if (payloadBytes != 0)
        {
            // intentionally round up; a full page eschews the final 0 lacing value
            int segments = (payloadBytes + SEGMENT_SIZE) / SEGMENT_SIZE;
            if (segments == MAX_SEGMENTS + 1)
            {
                segments = MAX_SEGMENTS;
            }
            
            // move the payload back to sit right after the lacing values
            System.arraycopy(pageBuffer, HEADER_BYTES + MAX_SEGMENTS, pageBuffer, HEADER_BYTES + segments, payloadBytes);
            
            pageBuffer[0] = 'O';
            pageBuffer[1] = 'g';
            pageBuffer[2] = 'g';
            pageBuffer[3] = 'S';
            pageBuffer[4] = 0;
            // header type
            pageBuffer[5] = (byte) ((continued ? 1 : 0) | (first ? 2 : 0) | (last ? 4 : 0));
            
            // granule low
            writeIntLE(pageBuffer, 6, granule);
            // granule high
            writeIntLE(pageBuffer, 10, (granule == 0xFFFFFFFF) ? 0xFFFFFFFF : 0);
            
            // stream serial
            writeIntLE(pageBuffer, 14, 1);
            
            // page sequence
            writeIntLE(pageBuffer, 18, sequenceNumber);
            
            // clear checksum
            writeIntLE(pageBuffer, 22, 0);
            
            // segment count
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
            
            int pageSize = HEADER_BYTES + segments + payloadBytes;
            
            // write checksum
            writeIntLE(pageBuffer, 22, oggCRC(pageBuffer, pageSize));
            
            data.write(pageBuffer, 0, pageSize);
            
            sequenceNumber++;
            first = false;
            continued = nextContinued;
            payloadBytes = 0;
        }
    }
    
    private static void writeIntLE(byte[] buffer, int offset, int value)
    {
        buffer[offset] = (byte) value;
        buffer[offset + 1] = (byte) (value >>> 8);
        buffer[offset + 2] = (byte) (value >>> 16);
        buffer[offset + 3] = (byte) (value >>> 24);
    }
    
    private static final int[] CRC_TABLE = new int[256];
    
    static
    {
        for (int i = 0; i < 256; i++)
        {
            int r = i << 24;
            for (int j = 0; j < 8; j++)
            {
                r = ((r & 0x80000000) != 0) ? ((r << 1) ^ 0x04C11DB7) : (r << 1);
            }
            CRC_TABLE[i] = r;
        }
    }
    
    /**
     * The Ogg page checksum (CRC-32, polynomial 0x04C11DB7, initial value 0, no reflection, no final xor)
     */
    public static int oggCRC(byte[] buffer, int length)
    {
        int crc = 0;
        for (int i = 0; i < length; i++)
        {
            crc = (crc << 8) ^ CRC_TABLE[((crc >>> 24) ^ buffer[i]) & 0xFF];
        }
        return crc;
    }
    
    private void flushBits()
    {
        if (bitsStored != 0)
        {
            if (payloadBytes == SEGMENT_SIZE * MAX_SEGMENTS)
            {
                throw new ParseException("Ran out of space in an Ogg packet", -1);
            }
            
            int page = HEADER_BYTES + MAX_SEGMENTS + payloadBytes;
            
            pageBuffer[page] = bitBuffer;
            payloadBytes++;
            
            bitBuffer = 0;
            bitsStored = 0;
        }
    }
    
    /**
     * Writes whole bytes; takes a fast path when the bit buffer is byte aligned
     */
    public void writeBytes(byte[] src, int offset, int length)
    {
        if (bitsStored != 0)
        {
            for (int i = 0; i < length; i++)
            {
                bitWrite(src[offset + i], Byte.SIZE);
            }
            return;
        }
        
        if (payloadBytes + length > SEGMENT_SIZE * MAX_SEGMENTS)
        {
            throw new ParseException("Ran out of space in an Ogg packet", -1);
        }
        
        System.arraycopy(src, offset, pageBuffer, HEADER_BYTES + MAX_SEGMENTS + payloadBytes, length);
        payloadBytes += length;
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
            writeBit((value >>> i) & 1);
        }
    }
    
    public void writeBit(int bit)
    {
        if (bit != 0)
        {
            bitBuffer |= (byte) (1 << bitsStored);
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

package no.stelar7.cdragon.types.wem.data;

import lombok.Data;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.readers.types.ByteArray;

import java.nio.ByteOrder;
import java.util.*;

@Data
public class WEMData
{
    private int fmtChunkOffset  = 0xFFFFFFFF;
    private int cueChunkOffset  = 0xFFFFFFFF;
    private int listChunkOffset = 0xFFFFFFFF;
    private int smplChunkOffset = 0xFFFFFFFF;
    private int vorbChunkOffset = 0xFFFFFFFF;
    private int dataChunkOffset = 0xFFFFFFFF;
    
    private int fmtChunkSize  = 0xFFFFFFFF;
    private int cueChunkSize  = 0xFFFFFFFF;
    private int listChunkSize = 0xFFFFFFFF;
    private int smplChunkSize = 0xFFFFFFFF;
    private int vorbChunkSize = 0xFFFFFFFF;
    private int dataChunkSize = 0xFFFFFFFF;
    
    private int channelCount;
    private int sampleRate;
    private int bytesPerSecond;
    
    private int     cueCount;
    private int     loopCount;
    private int     loopStart;
    private int     loopEnd;
    private int     sampleCount;
    private boolean noGranule;
    private boolean modPackets;
    private int     setupPacketOffset;
    private int     firstAudioPacketOffset;
    private boolean headerTriadPresent;
    private boolean oldPacketHeaders;
    private int     uid;
    private int     blockSize0Pow;
    private int     blockSize1Pow;
    
    private byte[] dataBytes;
    
    
    public WEMData(byte[] data)
    {
        this.dataBytes = data;
        RandomAccessReader raf = new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN);
        
        String magic = raf.readString(4);
        if (!"RIFF".equals(magic))
        {
            throw new IllegalArgumentException("This is not a valid WEM file, or its an unsupported type");
        }
        int riffSize = raf.readInt() + 8;
        
        String waveMagic = raf.readString(4);
        if (!"WAVE".equals(waveMagic))
        {
            throw new IllegalArgumentException("Missing WAVE magic");
        }
        
        int chunkOffset = 12;
        while (chunkOffset < riffSize)
        {
            raf.seek(chunkOffset);
            
            String chunkName = raf.readString(4);
            int    chunkSize = raf.readInt();
            
            if ("fmt ".equals(chunkName))
            {
                fmtChunkOffset = chunkOffset + 8;
                fmtChunkSize = chunkSize;
            } else if ("cue ".equals(chunkName))
            {
                cueChunkOffset = chunkOffset + 8;
                cueChunkSize = chunkSize;
            } else if ("LIST".equals(chunkName))
            {
                listChunkOffset = chunkOffset + 8;
                listChunkSize = chunkSize;
            } else if ("smpl".equals(chunkName))
            {
                smplChunkOffset = chunkOffset + 8;
                smplChunkSize = chunkSize;
            } else if ("vorb".equals(chunkName))
            {
                vorbChunkOffset = chunkOffset + 8;
                vorbChunkSize = chunkSize;
            } else if ("data".equals(chunkName))
            {
                dataChunkOffset = chunkOffset + 8;
                dataChunkSize = chunkSize;
            }
            
            chunkOffset += chunkSize + 8;
        }
        
        if (chunkOffset > riffSize)
        {
            throw new IllegalArgumentException("There was an error reading the file");
        }
        
        if (fmtChunkOffset == 0xFFFFFFFF && dataChunkOffset == 0xFFFFFFFF)
        {
            throw new IllegalArgumentException("There was an error reading the file");
        }
        
        if ((vorbChunkOffset == 0xFFFFFFFF) && (fmtChunkSize != 0x42))
        {
            throw new IllegalArgumentException("There was an error reading the file");
        }
        if ((vorbChunkOffset != 0xFFFFFFFF) && (fmtChunkSize != 0x28) && (fmtChunkSize != 0x18) && (fmtChunkSize != 0x12))
        {
            throw new IllegalArgumentException("There was an error reading the file");
        }
        if ((vorbChunkOffset == 0xFFFFFFFF) && (fmtChunkSize == 0x42))
        {
            vorbChunkOffset = fmtChunkOffset + 0x18;
        }
        
        raf.seek(fmtChunkOffset);
        
        short codecId = raf.readShort();
        if (codecId != (short) 0xFFFF)
        {
            throw new IllegalArgumentException("Invalid codec");
        }
        
        channelCount = raf.readShort();
        sampleRate = raf.readInt();
        bytesPerSecond = raf.readInt();
        
        int bitsPerSample = raf.readInt();
        if (bitsPerSample != 0)
        {
            throw new IllegalArgumentException("Invalid BPS");
        }
        
        
        int extra = raf.readInt();
        if (extra != (fmtChunkSize - 0x12))
        {
            throw new IllegalArgumentException("Invalid extra size");
        }
        
        if (fmtChunkSize == 0x28)
        {
            ByteArray signature = new ByteArray(new byte[]{1, 0, 0, 0, 0, 0, 0x10, 0, (byte) 0x80, 0, 0, (byte) 0xAA, 0, 0x38, (byte) 0x9B, 0x71});
            ByteArray check     = new ByteArray(raf.readBytes(16));
            
            if (!signature.equals(check))
            {
                throw new IllegalArgumentException("Invalid buffer signature");
            }
        }
        
        if (cueChunkOffset != 0xFFFFFFFF)
        {
            raf.seek(cueChunkOffset);
            cueCount = raf.readInt();
        }
        
        if (smplChunkOffset != 0xFFFFFFFF)
        {
            raf.seek(smplChunkOffset);
            
            loopCount = raf.readInt();
            if (loopCount != 1)
            {
                throw new IllegalArgumentException("Invalid loop count");
            }
            
            raf.seek(smplChunkOffset + 0x2C);
            
            loopStart = raf.readInt();
            loopEnd = raf.readInt();
        }
        
        List<Integer> validChunks = Arrays.asList(0xFFFFFFFF, 0x28, 0x2A, 0x2C, 0x32, 0x34);
        if (!validChunks.contains(vorbChunkSize))
        {
            throw new IllegalArgumentException("Invalid vorb chunk size");
        }
        
        raf.seek(vorbChunkOffset);
        sampleCount = raf.readInt();
        
        if ((vorbChunkSize == 0xFFFFFFFF) || (vorbChunkSize == 0x2A))
        {
            noGranule = true;
            
            raf.seek(vorbChunkOffset + 4);
            int modSignal = raf.readInt();
            
            List<Integer> validSignals = Arrays.asList(0x4A, 0x4B, 0x69, 0x70);
            if (!validSignals.contains(modSignal))
            {
                modPackets = true;
            }
            
            raf.seek(vorbChunkOffset + 0x10);
        } else
        {
            raf.seek(vorbChunkOffset + 0x18);
        }
        
        // remove this?
        modPackets = false;
        
        setupPacketOffset = raf.readInt();
        firstAudioPacketOffset = raf.readInt();
        
        if ((vorbChunkSize == 0xFFFFFFFF) || (vorbChunkSize == 0x2A))
        {
            raf.seek(vorbChunkOffset + 0x24);
        }
        
        if ((vorbChunkSize == 0x32) || (vorbChunkSize == 0x34))
        {
            raf.seek(vorbChunkOffset + 0x2C);
        }
        
        if ((vorbChunkSize == 0x28) || (vorbChunkSize == 0x2C))
        {
            headerTriadPresent = true;
            oldPacketHeaders = true;
        }
        
        if ((vorbChunkSize == 0xFFFFFFFF) || (vorbChunkSize == 0x2A) || (vorbChunkSize == 0x32) || (vorbChunkSize == 0x34))
        {
            uid = raf.readInt();
            blockSize0Pow = raf.readByte();
            blockSize1Pow = raf.readByte();
        }
        
        if (loopCount != 0)
        {
            if (loopEnd == 0)
            {
                loopEnd = sampleCount;
            } else
            {
                loopEnd++;
            }
            
            if (loopStart >= sampleCount || loopEnd > sampleCount || loopStart > loopEnd)
            {
                throw new IllegalArgumentException("Invalid loop range");
            }
        }
    }
}

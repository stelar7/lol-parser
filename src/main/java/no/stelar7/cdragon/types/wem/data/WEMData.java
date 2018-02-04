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
    
    private boolean littleEndian = true;
    
    private byte[] dataBytes;
    
    
    public WEMData(byte[] data)
    {
        this.dataBytes = data;
        RandomAccessReader raf = new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN);
        
        String magic = raf.readString(4);
        if (!"RIFF".equals(magic))
        {
            if (!"RIFX".equals(magic))
            {
                throw new IllegalArgumentException("This is not a valid WEM file, or its an unsupported type");
            } else
            {
                throw new IllegalArgumentException("This file is in big endian, and that is not supported...");
            }
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
        
        if (fmtChunkOffset == -1 && dataChunkOffset == -1)
        {
            throw new IllegalArgumentException("Expected ftm, data chunks");
        }
        
        if ((vorbChunkOffset == -1) && (fmtChunkSize != 0x42))
        {
            throw new IllegalArgumentException("Expected 0x42 fmt if vorb is missing");
        }
        if ((vorbChunkOffset != -1) && (fmtChunkSize != 0x28) && (fmtChunkSize != 0x18) && (fmtChunkSize != 0x12))
        {
            throw new IllegalArgumentException("bad fmt size");
        }
        if ((vorbChunkOffset == -1) && (fmtChunkSize == 0x42))
        {
            vorbChunkOffset = fmtChunkOffset + 0x18;
        }
        
        raf.seek(fmtChunkOffset);
        
        short codecId = raf.readShort();
        if (codecId != (short) 0xFFFF)
        {
            throw new IllegalArgumentException("bad codec id");
        }
        
        channelCount = raf.readShort();
        sampleRate = raf.readInt();
        bytesPerSecond = raf.readInt();
        
        if (raf.readShort() != 0)
        {
            throw new IllegalArgumentException("bad block align");
        }
        
        if (raf.readShort() != 0)
        {
            throw new IllegalArgumentException("expected 0 bps");
        }
        
        int extra = raf.readShort();
        if (extra != (fmtChunkSize - 0x12))
        {
            throw new IllegalArgumentException("Invalid extra size");
        }
        
        if ((fmtChunkSize - 0x12) >= 2)
        {
            // ext_unk
            raf.readShort();
            if ((fmtChunkSize - 0x12) >= 6)
            {
                // subtype
                raf.readInt();
            }
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
        
        if (cueChunkOffset != -1)
        {
            raf.seek(cueChunkOffset);
            cueCount = raf.readInt();
        }
        
        if (smplChunkOffset != -1)
        {
            raf.seek(smplChunkOffset + 0x1C);
            
            loopCount = raf.readInt();
            if (loopCount != 1)
            {
                throw new IllegalArgumentException("expected 1 loop");
            }
            
            raf.seek(smplChunkOffset + 0x2C);
            
            loopStart = raf.readInt();
            loopEnd = raf.readInt();
        }
        
        List<Integer> validChunks = Arrays.asList(-1, 0x28, 0x2A, 0x2C, 0x32, 0x34);
        if (!validChunks.contains(vorbChunkSize))
        {
            throw new IllegalArgumentException("Invalid vorb chunk size");
        }
        
        raf.seek(vorbChunkOffset);
        sampleCount = raf.readInt();
        
        if ((vorbChunkSize == -1) || (vorbChunkSize == 0x2A))
        {
            noGranule = true;
            
            raf.seek(vorbChunkOffset + 0x4);
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
        
        setupPacketOffset = raf.readInt();
        firstAudioPacketOffset = raf.readInt();
        
        if ((vorbChunkSize == -1) || (vorbChunkSize == 0x2A))
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
        
        if ((vorbChunkSize == -1) || (vorbChunkSize == 0x2A) || (vorbChunkSize == 0x32) || (vorbChunkSize == 0x34))
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
    
    public void printInfo()
    {
        if (littleEndian)
        {
            System.out.print("RIFF WAVE");
        } else
        {
            System.out.print("RIFX WAVE");
        }
        
        System.out.print(" " + channelCount + " channel");
        if (channelCount != 1)
        {
            System.out.print("s");
        }
        
        System.out.println(" " + sampleRate + " Hz " + bytesPerSecond * 8 + " bps");
        System.out.println(sampleCount + " samples");
        
        if (loopCount != 0)
        {
            System.out.println("loop from " + loopStart + " to " + loopEnd);
        }
        
        if (oldPacketHeaders)
        {
            System.out.println("- 8 byte (old) packet headers");
        } else if (noGranule)
        {
            System.out.println("- 2 byte packet headers, no granule");
        } else
        {
            System.out.println("- 6 byte packet headers");
        }
        
        if (headerTriadPresent)
        {
            System.out.println("- Vorbis header triad present");
        }
        
        if (headerTriadPresent)
        {
            System.out.println("- full setup header");
        } else
        {
            System.out.println("- stripped setup header");
        }
        
        if (headerTriadPresent)
        {
            System.out.println("- inline codebooks");
        } else
        {
            System.out.println("- external codebooks");
        }
        
        if (modPackets)
        {
            System.out.println("- modified Vorbis packets");
        } else
        {
            System.out.println("- standard Vorbis packets");
        }
    }
}

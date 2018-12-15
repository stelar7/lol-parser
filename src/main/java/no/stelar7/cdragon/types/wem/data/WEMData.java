package no.stelar7.cdragon.types.wem.data;

import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.util.*;

public class WEMData
{
    private int fmtChunkOffset  = 0xFFFFFFFF;
    private int cueChunkOffset  = 0xFFFFFFFF;
    private int listChunkOffset = 0xFFFFFFFF;
    private int smplChunkOffset = 0xFFFFFFFF;
    private int vorbChunkOffset = 0xFFFFFFFF;
    private int dataChunkOffset = 0xFFFFFFFF;
    private int junkChunkOffset = 0xFFFFFFFF;
    private int akdChunkOffset  = 0xFFFFFFFF;
    
    
    private int fmtChunkSize  = 0xFFFFFFFF;
    private int cueChunkSize  = 0xFFFFFFFF;
    private int listChunkSize = 0xFFFFFFFF;
    private int smplChunkSize = 0xFFFFFFFF;
    private int vorbChunkSize = 0xFFFFFFFF;
    private int dataChunkSize = 0xFFFFFFFF;
    private int junkChunkSize = 0xFFFFFFFF;
    private int akdChunkSize  = 0xFFFFFFFF;
    
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
    
    public int getJunkChunkOffset()
    {
        return junkChunkOffset;
    }
    
    public void setJunkChunkOffset(int junkChunkOffset)
    {
        this.junkChunkOffset = junkChunkOffset;
    }
    
    public int getAkdChunkOffset()
    {
        return akdChunkOffset;
    }
    
    public void setAkdChunkOffset(int akdChunkOffset)
    {
        this.akdChunkOffset = akdChunkOffset;
    }
    
    public int getJunkChunkSize()
    {
        return junkChunkSize;
    }
    
    public void setJunkChunkSize(int junkChunkSize)
    {
        this.junkChunkSize = junkChunkSize;
    }
    
    public int getAkdChunkSize()
    {
        return akdChunkSize;
    }
    
    public void setAkdChunkSize(int akdChunkSize)
    {
        this.akdChunkSize = akdChunkSize;
    }
    
    public int getFmtChunkOffset()
    {
        return fmtChunkOffset;
    }
    
    public void setFmtChunkOffset(int fmtChunkOffset)
    {
        this.fmtChunkOffset = fmtChunkOffset;
    }
    
    public int getCueChunkOffset()
    {
        return cueChunkOffset;
    }
    
    public void setCueChunkOffset(int cueChunkOffset)
    {
        this.cueChunkOffset = cueChunkOffset;
    }
    
    public int getListChunkOffset()
    {
        return listChunkOffset;
    }
    
    public void setListChunkOffset(int listChunkOffset)
    {
        this.listChunkOffset = listChunkOffset;
    }
    
    public int getSmplChunkOffset()
    {
        return smplChunkOffset;
    }
    
    public void setSmplChunkOffset(int smplChunkOffset)
    {
        this.smplChunkOffset = smplChunkOffset;
    }
    
    public int getVorbChunkOffset()
    {
        return vorbChunkOffset;
    }
    
    public void setVorbChunkOffset(int vorbChunkOffset)
    {
        this.vorbChunkOffset = vorbChunkOffset;
    }
    
    public int getDataChunkOffset()
    {
        return dataChunkOffset;
    }
    
    public void setDataChunkOffset(int dataChunkOffset)
    {
        this.dataChunkOffset = dataChunkOffset;
    }
    
    public int getFmtChunkSize()
    {
        return fmtChunkSize;
    }
    
    public void setFmtChunkSize(int fmtChunkSize)
    {
        this.fmtChunkSize = fmtChunkSize;
    }
    
    public int getCueChunkSize()
    {
        return cueChunkSize;
    }
    
    public void setCueChunkSize(int cueChunkSize)
    {
        this.cueChunkSize = cueChunkSize;
    }
    
    public int getListChunkSize()
    {
        return listChunkSize;
    }
    
    public void setListChunkSize(int listChunkSize)
    {
        this.listChunkSize = listChunkSize;
    }
    
    public int getSmplChunkSize()
    {
        return smplChunkSize;
    }
    
    public void setSmplChunkSize(int smplChunkSize)
    {
        this.smplChunkSize = smplChunkSize;
    }
    
    public int getVorbChunkSize()
    {
        return vorbChunkSize;
    }
    
    public void setVorbChunkSize(int vorbChunkSize)
    {
        this.vorbChunkSize = vorbChunkSize;
    }
    
    public int getDataChunkSize()
    {
        return dataChunkSize;
    }
    
    public void setDataChunkSize(int dataChunkSize)
    {
        this.dataChunkSize = dataChunkSize;
    }
    
    public int getChannelCount()
    {
        return channelCount;
    }
    
    public void setChannelCount(int channelCount)
    {
        this.channelCount = channelCount;
    }
    
    public int getSampleRate()
    {
        return sampleRate;
    }
    
    public void setSampleRate(int sampleRate)
    {
        this.sampleRate = sampleRate;
    }
    
    public int getBytesPerSecond()
    {
        return bytesPerSecond;
    }
    
    public void setBytesPerSecond(int bytesPerSecond)
    {
        this.bytesPerSecond = bytesPerSecond;
    }
    
    public int getCueCount()
    {
        return cueCount;
    }
    
    public void setCueCount(int cueCount)
    {
        this.cueCount = cueCount;
    }
    
    public int getLoopCount()
    {
        return loopCount;
    }
    
    public void setLoopCount(int loopCount)
    {
        this.loopCount = loopCount;
    }
    
    public int getLoopStart()
    {
        return loopStart;
    }
    
    public void setLoopStart(int loopStart)
    {
        this.loopStart = loopStart;
    }
    
    public int getLoopEnd()
    {
        return loopEnd;
    }
    
    public void setLoopEnd(int loopEnd)
    {
        this.loopEnd = loopEnd;
    }
    
    public int getSampleCount()
    {
        return sampleCount;
    }
    
    public void setSampleCount(int sampleCount)
    {
        this.sampleCount = sampleCount;
    }
    
    public boolean isNoGranule()
    {
        return noGranule;
    }
    
    public void setNoGranule(boolean noGranule)
    {
        this.noGranule = noGranule;
    }
    
    public boolean isModPackets()
    {
        return modPackets;
    }
    
    public void setModPackets(boolean modPackets)
    {
        this.modPackets = modPackets;
    }
    
    public int getSetupPacketOffset()
    {
        return setupPacketOffset;
    }
    
    public void setSetupPacketOffset(int setupPacketOffset)
    {
        this.setupPacketOffset = setupPacketOffset;
    }
    
    public int getFirstAudioPacketOffset()
    {
        return firstAudioPacketOffset;
    }
    
    public void setFirstAudioPacketOffset(int firstAudioPacketOffset)
    {
        this.firstAudioPacketOffset = firstAudioPacketOffset;
    }
    
    public boolean isHeaderTriadPresent()
    {
        return headerTriadPresent;
    }
    
    public void setHeaderTriadPresent(boolean headerTriadPresent)
    {
        this.headerTriadPresent = headerTriadPresent;
    }
    
    public boolean isOldPacketHeaders()
    {
        return oldPacketHeaders;
    }
    
    public void setOldPacketHeaders(boolean oldPacketHeaders)
    {
        this.oldPacketHeaders = oldPacketHeaders;
    }
    
    public int getUid()
    {
        return uid;
    }
    
    public void setUid(int uid)
    {
        this.uid = uid;
    }
    
    public int getBlockSize0Pow()
    {
        return blockSize0Pow;
    }
    
    public void setBlockSize0Pow(int blockSize0Pow)
    {
        this.blockSize0Pow = blockSize0Pow;
    }
    
    public int getBlockSize1Pow()
    {
        return blockSize1Pow;
    }
    
    public void setBlockSize1Pow(int blockSize1Pow)
    {
        this.blockSize1Pow = blockSize1Pow;
    }
    
    public boolean isLittleEndian()
    {
        return littleEndian;
    }
    
    public void setLittleEndian(boolean littleEndian)
    {
        this.littleEndian = littleEndian;
    }
    
    public byte[] getDataBytes()
    {
        return dataBytes;
    }
    
    public void setDataBytes(byte[] dataBytes)
    {
        this.dataBytes = dataBytes;
    }
    
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
        int riffSize = raf.readInt();
        
        String waveMagic = raf.readString(4);
        if (!"WAVE".equals(waveMagic))
        {
            throw new IllegalArgumentException("Missing WAVE magic");
        }
        
        int chunkOffset = 12;
        int chunkEnd    = riffSize + 8;
        while (chunkOffset < chunkEnd)
        {
            raf.seek(chunkOffset);
            String chunkName = raf.readString(4);
            int    chunkSize = raf.readInt();
            
            switch (chunkName)
            {
                case "fmt ":
                    fmtChunkOffset = chunkOffset + 8;
                    fmtChunkSize = chunkSize;
                    break;
                case "cue ":
                    cueChunkOffset = chunkOffset + 8;
                    cueChunkSize = chunkSize;
                    break;
                case "LIST":
                    listChunkOffset = chunkOffset + 8;
                    listChunkSize = chunkSize;
                    break;
                case "smpl":
                    smplChunkOffset = chunkOffset + 8;
                    smplChunkSize = chunkSize;
                    break;
                case "vorb":
                    vorbChunkOffset = chunkOffset + 8;
                    vorbChunkSize = chunkSize;
                    break;
                case "data":
                    dataChunkOffset = chunkOffset + 8;
                    dataChunkSize = chunkSize;
                    break;
                case "JUNK":
                    junkChunkOffset = chunkOffset + 8;
                    junkChunkSize = chunkSize;
                case "akd ":
                {
                    akdChunkOffset = chunkOffset + 8;
                    akdChunkSize = chunkSize;
                }
                default:
                    throw new RuntimeException("Invalid chunk name: " + chunkName);
            }
            
            chunkOffset += chunkSize + 8;
        }
        
        if (chunkOffset > chunkEnd)
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
    
    private byte[] fixWEM(byte[] data)
    {
        RandomAccessReader raf = new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN);
        ByteWriter         bw  = new ByteWriter();
        bw.writeRAF(raf);
        byte[] ba = bw.toByteArray();
        
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
        
        int riffSize = raf.readInt();
        
        String waveMagic = raf.readString(4);
        if (!"WAVE".equals(waveMagic))
        {
            throw new IllegalArgumentException("Missing WAVE magic");
        }
        
        int startChunk  = 12;
        int endChunk    = riffSize + 8;
        int chunkOffset = startChunk;
        int channels    = -1;
        int block_size  = -1;
        
        while (chunkOffset < endChunk)
        {
            raf.seek(chunkOffset);
            String type = raf.readString(4);
            int    size = raf.readInt();
            
            if ((chunkOffset + 8 + size) < chunkOffset + 8 ||
                (chunkOffset + 8 + size) > endChunk)
            {
                System.out.println("chunk size out of range");
            }
            
            
            switch (type)
            {
                case "fmt ":
                    if (0xE > size)
                    {
                        System.out.println("chunk too small");
                    }
                    
                    raf.seek(chunkOffset + 8 + 0);
                    int codec = raf.readShort();
                    if (0x2 != codec)
                    {
                        System.out.println("invalid codec");
                    }
                    
                    ba[chunkOffset + 8 + 0] = 0x11;
                    
                    raf.seek(chunkOffset + 8 + 2);
                    channels = raf.readShort();
                    
                    raf.seek(chunkOffset + 8 + 0xC);
                    block_size = raf.readShort();
                    break;
                case "data":
                    if (channels == -1 || block_size == -1)
                    {
                        System.out.println("data before fmt");
                    }
                    reinterleave_ms_ima(raf, ba, chunkOffset + 8, size, channels, block_size);
                    break;
            }
            
            chunkOffset += 8 + size;
        }
        
        return ba;
    }
    
    private void reinterleave_ms_ima(RandomAccessReader raf, byte[] ba, int offset, int size, int channels, int block_size)
    {
        int bps = block_size / channels;
        if (size % block_size != 0)
        {
            System.out.println("blocks doesnt fit in size");
        }
        
        if (block_size % channels != 0)
        {
            System.out.println("blocks doesnt divide channels");
        }
        
        if (bps % 4 != 0)
        {
            System.out.println("expected 4 bytes per channel");
        }
        
        while (size > 0)
        {
            int writes = 0;
            
            raf.seek(offset);
            byte[] data = raf.readBytes(block_size);
            
            for (int i = 0; i < channels; i++)
            {
                ByteArray baw = new ByteArray(data);
                ba[offset + writes] = baw.copyOfRange(i * bps, 1).getData()[0];
                ba[offset + writes + 1] = baw.copyOfRange(i * bps + 1, 1).getData()[0];
                ba[offset + writes + 2] = baw.copyOfRange(i * bps + 2, 1).getData()[0];
                ba[offset + writes + 3] = baw.copyOfRange(i * bps + 3, 1).getData()[0];
                writes += 4;
            }
            
            for (int j = 0; j < bps - 4; j += 4)
            {
                for (int i = 0; i < channels; i++)
                {
                    ByteArray baw = new ByteArray(data);
                    ba[offset + writes] = baw.copyOfRange(i * bps + 4 + j + 0, 1).getData()[0];
                    ba[offset + writes + 1] = baw.copyOfRange(i * bps + 4 + j + 1, 1).getData()[0];
                    ba[offset + writes + 2] = baw.copyOfRange(i * bps + 4 + j + 2, 1).getData()[0];
                    ba[offset + writes + 3] = baw.copyOfRange(i * bps + 4 + j + 3, 1).getData()[0];
                    writes += 4;
                }
            }
            
            size -= block_size;
            offset += block_size;
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

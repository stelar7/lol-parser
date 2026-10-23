package no.stelar7.cdragon.types.wem.data;

import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.util.*;

/**
 * The header of a wem file, parsed by the constructor.
 * The fields are filled in while walking the chunks (so they are not final), there are no setters.
 */
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
    
    public int getAkdChunkOffset()
    {
        return akdChunkOffset;
    }
    
    public int getJunkChunkSize()
    {
        return junkChunkSize;
    }
    
    public int getAkdChunkSize()
    {
        return akdChunkSize;
    }
    
    public int getFmtChunkOffset()
    {
        return fmtChunkOffset;
    }
    
    public int getCueChunkOffset()
    {
        return cueChunkOffset;
    }
    
    public int getListChunkOffset()
    {
        return listChunkOffset;
    }
    
    public int getSmplChunkOffset()
    {
        return smplChunkOffset;
    }
    
    public int getVorbChunkOffset()
    {
        return vorbChunkOffset;
    }
    
    public int getDataChunkOffset()
    {
        return dataChunkOffset;
    }
    
    public int getFmtChunkSize()
    {
        return fmtChunkSize;
    }
    
    public int getCueChunkSize()
    {
        return cueChunkSize;
    }
    
    public int getListChunkSize()
    {
        return listChunkSize;
    }
    
    public int getSmplChunkSize()
    {
        return smplChunkSize;
    }
    
    public int getVorbChunkSize()
    {
        return vorbChunkSize;
    }
    
    public int getDataChunkSize()
    {
        return dataChunkSize;
    }
    
    public int getChannelCount()
    {
        return channelCount;
    }
    
    public int getSampleRate()
    {
        return sampleRate;
    }
    
    public int getBytesPerSecond()
    {
        return bytesPerSecond;
    }
    
    public int getCueCount()
    {
        return cueCount;
    }
    
    public int getLoopCount()
    {
        return loopCount;
    }
    
    public int getLoopStart()
    {
        return loopStart;
    }
    
    public int getLoopEnd()
    {
        return loopEnd;
    }
    
    public int getSampleCount()
    {
        return sampleCount;
    }
    
    public boolean isNoGranule()
    {
        return noGranule;
    }
    
    public boolean isModPackets()
    {
        return modPackets;
    }
    
    public int getSetupPacketOffset()
    {
        return setupPacketOffset;
    }
    
    public int getFirstAudioPacketOffset()
    {
        return firstAudioPacketOffset;
    }
    
    public boolean isHeaderTriadPresent()
    {
        return headerTriadPresent;
    }
    
    public boolean isOldPacketHeaders()
    {
        return oldPacketHeaders;
    }
    
    public int getUid()
    {
        return uid;
    }
    
    public int getBlockSize0Pow()
    {
        return blockSize0Pow;
    }
    
    public int getBlockSize1Pow()
    {
        return blockSize1Pow;
    }
    
    public boolean isLittleEndian()
    {
        return littleEndian;
    }
    
    public byte[] getDataBytes()
    {
        return dataBytes;
    }
    
    /**
     * Parses the RIFF/RIFX header of a Wwise vorbis wem.
     * Port of the Wwise_RIFF_Vorbis constructor from ww2ogg (wwriff.cpp)
     *
     * @throws ParseException if the file is not a supported wem
     */
    public WEMData(byte[] data)
    {
        this.dataBytes = data;
        try
        {
            parseHeader(data);
        } catch (IndexOutOfBoundsException e)
        {
            throw new ParseException("WEM header truncated", -1, e);
        }
    }

    private void parseHeader(byte[] data)
    {
        RandomAccessReader raf = new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN);

        if (data.length < 12)
        {
            throw new ParseException("File too small to be a WEM", 0);
        }

        String magic = raf.readString(4);
        if ("RIFX".equals(magic))
        {
            littleEndian = false;
            raf.setEndian(ByteOrder.BIG_ENDIAN);
        } else if (!"RIFF".equals(magic))
        {
            throw new ParseException("Missing RIFF magic", 0);
        }

        long riffSize = raf.readU32() + 8;
        if (riffSize > data.length)
        {
            throw new ParseException("RIFF truncated", 4);
        }

        String waveMagic = raf.readString(4);
        if (!"WAVE".equals(waveMagic))
        {
            throw new ParseException("Missing WAVE magic", 8);
        }

        long chunkOffset = 12;
        while (chunkOffset < riffSize)
        {
            if (chunkOffset + 8 > riffSize)
            {
                throw new ParseException("Chunk header truncated", chunkOffset);
            }

            raf.seek(chunkOffset);
            String chunkName = raf.readString(4);
            long   chunkSize = raf.readU32();
            int    offset    = (int) (chunkOffset + 8);
            int    size      = (int) Math.min(chunkSize, Integer.MAX_VALUE);

            switch (chunkName)
            {
                case "fmt " ->
                {
                    fmtChunkOffset = offset;
                    fmtChunkSize = size;
                }
                case "cue " ->
                {
                    cueChunkOffset = offset;
                    cueChunkSize = size;
                }
                case "LIST" ->
                {
                    listChunkOffset = offset;
                    listChunkSize = size;
                }
                case "smpl" ->
                {
                    smplChunkOffset = offset;
                    smplChunkSize = size;
                }
                case "vorb" ->
                {
                    vorbChunkOffset = offset;
                    vorbChunkSize = size;
                }
                case "data" ->
                {
                    dataChunkOffset = offset;
                    dataChunkSize = size;
                }
                case "JUNK" ->
                {
                    junkChunkOffset = offset;
                    junkChunkSize = size;
                }
                case "akd " ->
                {
                    akdChunkOffset = offset;
                    akdChunkSize = size;
                }
                default ->
                {
                    // unknown chunks (hash, meta, ...) are skipped, like ww2ogg does
                }
            }

            long next = chunkOffset + 8 + chunkSize;
            // RIFF chunks are padded to an even size with a 0 byte (chunk ids never start with 0)
            if ((chunkSize & 1) != 0 && next < riffSize && data[(int) next] == 0)
            {
                next++;
            }
            chunkOffset = next;
        }

        if (chunkOffset > riffSize)
        {
            throw new ParseException("Chunk truncated", chunkOffset);
        }

        // check that we have the chunks we're expecting
        if (fmtChunkOffset == -1 || dataChunkOffset == -1)
        {
            throw new ParseException("Expected fmt and data chunks", -1);
        }

        if ((vorbChunkOffset == -1) && (fmtChunkSize != 0x42))
        {
            throw new ParseException("Expected 0x42 fmt if vorb is missing", fmtChunkOffset);
        }
        if ((vorbChunkOffset != -1) && (fmtChunkSize != 0x28) && (fmtChunkSize != 0x18) && (fmtChunkSize != 0x12))
        {
            throw new ParseException("Bad fmt size", fmtChunkOffset);
        }
        if ((vorbChunkOffset == -1) && (fmtChunkSize == 0x42))
        {
            // fake it out
            vorbChunkOffset = fmtChunkOffset + 0x18;
        }

        raf.seek(fmtChunkOffset);

        int codecId = raf.readU16();
        if (codecId != 0xFFFF)
        {
            throw new ParseException("Bad codec id 0x" + Integer.toHexString(codecId) + ", only vorbis is supported", fmtChunkOffset);
        }

        channelCount = raf.readU16();
        sampleRate = raf.readInt();
        bytesPerSecond = raf.readInt();

        if (raf.readU16() != 0)
        {
            throw new ParseException("Bad block align", raf);
        }

        if (raf.readU16() != 0)
        {
            throw new ParseException("Expected 0 bps", raf);
        }

        int extra = raf.readU16();
        if (extra != (fmtChunkSize - 0x12))
        {
            throw new ParseException("Bad extra fmt length", raf);
        }

        if ((fmtChunkSize - 0x12) >= 2)
        {
            // ext_unk
            raf.readU16();
            if ((fmtChunkSize - 0x12) >= 6)
            {
                // subtype
                raf.readInt();
            }
        }

        if (fmtChunkSize == 0x28)
        {
            byte[] signature = new byte[]{1, 0, 0, 0, 0, 0, 0x10, 0, (byte) 0x80, 0, 0, (byte) 0xAA, 0, 0x38, (byte) 0x9B, 0x71};
            if (!Arrays.equals(signature, raf.readBytes(16)))
            {
                throw new ParseException("Expected signature in extra fmt", raf);
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
                throw new ParseException("Expected one loop", raf);
            }

            raf.seek(smplChunkOffset + 0x2C);

            loopStart = raf.readInt();
            loopEnd = raf.readInt();
        }

        if (!List.of(-1, 0x28, 0x2A, 0x2C, 0x32, 0x34).contains(vorbChunkSize))
        {
            throw new ParseException("Bad vorb size 0x" + Integer.toHexString(vorbChunkSize), vorbChunkOffset);
        }

        raf.seek(vorbChunkOffset);
        sampleCount = raf.readInt();

        if ((vorbChunkSize == -1) || (vorbChunkSize == 0x2A))
        {
            noGranule = true;

            raf.seek(vorbChunkOffset + 0x4);
            int modSignal = raf.readInt();

            // seems to be 0xD9 when mod packets should be set, also seen 0xCB, 0xBC, 0xB2
            if (modSignal != 0x4A && modSignal != 0x4B && modSignal != 0x69 && modSignal != 0x70)
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
            // ok to leave uid, blockSize0Pow and blockSize1Pow unset
            headerTriadPresent = true;
            oldPacketHeaders = true;
        } else
        {
            uid = raf.readInt();
            blockSize0Pow = raf.readU8();
            blockSize1Pow = raf.readU8();
        }

        // check/set loops now that we know total sample count
        if (loopCount != 0)
        {
            if (loopEnd == 0)
            {
                loopEnd = sampleCount;
            } else
            {
                loopEnd++;
            }

            if (Integer.compareUnsigned(loopStart, sampleCount) >= 0 || Integer.compareUnsigned(loopEnd, sampleCount) > 0 || Integer.compareUnsigned(loopStart, loopEnd) > 0)
            {
                throw new ParseException("Loops out of range", smplChunkOffset);
            }
        }

        if (setupPacketOffset < 0 || firstAudioPacketOffset < 0 || (long) dataChunkOffset + dataChunkSize > data.length)
        {
            throw new ParseException("Data chunk out of range", dataChunkOffset);
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

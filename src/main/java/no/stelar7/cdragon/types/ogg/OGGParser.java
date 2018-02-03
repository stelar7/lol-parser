package no.stelar7.cdragon.types.ogg;

import javafx.util.Pair;
import no.stelar7.cdragon.types.ogg.data.*;
import no.stelar7.cdragon.types.wem.data.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;

public class OGGParser
{
    
    public OGGStream parse(WEMData wem)
    {
        OGGStream          ogg       = new OGGStream();
        RandomAccessReader bitStream = new RandomAccessReader(wem.getDataBytes(), ByteOrder.LITTLE_ENDIAN);
        
        Boolean[] modeBlockFlag     = null;
        int       modeBits          = 0;
        boolean   previousBlockFlag = false;
        
        if (wem.isHeaderTriadPresent())
        {
            generateOGGHeaderTriad(ogg, bitStream, wem);
        } else
        {
            // should fullSetup be false?
            Pair<Integer, Boolean[]> data = generateOGGHeader(ogg, bitStream, wem, false, true);
            modeBits = data.getKey();
            modeBlockFlag = data.getValue();
        }
        
        int offset = wem.getDataChunkOffset() + wem.getFirstAudioPacketOffset();
        while (offset < wem.getDataChunkOffset() + wem.getDataChunkSize())
        {
            int size;
            int granule;
            int packetHeaderSize;
            int packetPayloadOffset;
            int nextOffset;
            
            if (wem.isOldPacketHeaders())
            {
                OGGPacket8 audioPacket = new OGGPacket8(bitStream, offset);
                packetHeaderSize = audioPacket.getHeaderSize();
                size = audioPacket.getSize();
                packetPayloadOffset = audioPacket.getOffset();
                granule = audioPacket.getGranule();
                nextOffset = audioPacket.nextOffset();
            } else
            {
                OGGPacket audioPacket = new OGGPacket(bitStream, offset, wem.isNoGranule());
                packetHeaderSize = audioPacket.getHeaderSize();
                size = audioPacket.getSize();
                packetPayloadOffset = audioPacket.getOffset();
                granule = audioPacket.getGranule();
                nextOffset = audioPacket.nextOffset();
            }
            
            if (offset + packetHeaderSize > wem.getDataChunkOffset() + wem.getDataChunkSize())
            {
                throw new IllegalArgumentException("Error generating vorbis packet");
            }
            
            offset = packetPayloadOffset;
            bitStream.seek(offset);
            
            ogg.setGranule((granule == 0xFFFFFFFF) ? 1 : granule);
            
            if (wem.isModPackets())
            {
                if (modeBlockFlag == null)
                {
                    throw new IllegalArgumentException("Error generating vorbis packet");
                }
                
                byte packetType = 0;
                ogg.writeBit(packetType);
                
                
                int modeNumber = bitStream.readBits(modeBits);
                ogg.bitWrite(modeNumber, Byte.SIZE);
                
                int remainder = bitStream.readBits(8 - modeBits);
                
                
                if (modeBlockFlag[modeNumber])
                {
                    bitStream.seek(nextOffset);
                    boolean nextBlockFlag = false;
                    
                    if (nextOffset + packetHeaderSize <= wem.getDataChunkOffset() + wem.getDataChunkSize())
                    {
                        OGGPacket audioPacket = new OGGPacket(bitStream, nextOffset, wem.isNoGranule());
                        int       nextSize    = audioPacket.getSize();
                        
                        if (nextSize != 0xFFFFFFFF)
                        {
                            bitStream.seek(audioPacket.getOffset());
                            int nextModeNumber = bitStream.readBits(modeBits);
                            
                            nextBlockFlag = modeBlockFlag[nextModeNumber];
                        }
                    }
                    
                    byte previousWindowType = previousBlockFlag ? (byte) 1 : (byte) 0;
                    ogg.writeBit(previousWindowType);
                    
                    byte nextWindowType = nextBlockFlag ? (byte) 1 : (byte) 0;
                    ogg.writeBit(nextWindowType);
                    
                    bitStream.seek(offset + 1);
                }
                
                previousBlockFlag = modeBlockFlag[modeNumber];
                ogg.bitWrite(remainder, 8 - modeBits);
            } else
            {
                int b = Byte.toUnsignedInt(bitStream.readByte());
                if (b < 0)
                {
                    throw new IllegalArgumentException("Error generating vorbis packet");
                }
                ogg.bitWrite(b, Byte.SIZE);
            }
            
            for (int i = 1; i < size; i++)
            {
                int b = Byte.toUnsignedInt(bitStream.readByte());
                if (b < 0)
                {
                    throw new IllegalArgumentException("Error generating vorbis packet");
                }
                ogg.bitWrite(b, Byte.SIZE);
            }
            
            offset = nextOffset;
            boolean finalPage = (offset == wem.getDataChunkOffset() + wem.getDataChunkSize());
            ogg.flushPage(false, finalPage);
        }
        
        if (offset > wem.getDataChunkOffset() + wem.getDataChunkSize())
        {
            throw new IllegalArgumentException("Error generating vorbis packet");
        }
        
        return ogg;
    }
    
    private void generateOGGHeaderTriad(OGGStream ogg, RandomAccessReader bitStream, WEMData wem)
    {
        int offset = wem.getDataChunkOffset() + wem.getSetupPacketOffset();
        
        OGGPacket8 infoPacket = new OGGPacket8(bitStream, offset);
        int        infoSize   = infoPacket.getSize();
        
        if (infoPacket.getGranule() != 0)
        {
            throw new IllegalArgumentException("Error generating vorbis packet");
        }
        
        bitStream.seek(infoPacket.getOffset());
        byte type = bitStream.readByte();
        if (type != 1)
        {
            throw new IllegalArgumentException("Error generating vorbis packet");
        }
        ogg.bitWrite(type, Byte.SIZE);
        
        for (int i = 0; i < infoSize; i++)
        {
            ogg.bitWrite(bitStream.readByte(), Byte.SIZE);
        }
        
        ogg.flushPage(false, false);
        offset = infoPacket.nextOffset();
        
        
        OGGPacket8 comment     = new OGGPacket8(bitStream, offset);
        int        commentSize = comment.getSize();
        if (comment.getGranule() != 0)
        {
            throw new IllegalArgumentException("Error generating vorbis packet");
        }
        bitStream.seek(comment.getOffset());
        type = bitStream.readByte();
        if (type != 3)
        {
            throw new IllegalArgumentException("Error generating vorbis packet");
        }
        ogg.bitWrite(type, Byte.SIZE);
        
        for (int i = 0; i < commentSize; i++)
        {
            ogg.bitWrite(bitStream.readByte(), Byte.SIZE);
        }
        
        ogg.flushPage(false, false);
        offset = comment.nextOffset();
        
        
        OGGPacket8 setup = new OGGPacket8(bitStream, offset);
        bitStream.seek(comment.getOffset());
        if (setup.getGranule() != 0)
        {
            throw new IllegalArgumentException("Error generating vorbis packet");
        }
        type = (byte) bitStream.readBits(8);
        if (type != 5)
        {
            throw new IllegalArgumentException("Error generating vorbis packet");
        }
        ogg.bitWrite(type, Byte.SIZE);
        
        for (int i = 0; i < 6; i++)
        {
            ogg.bitWrite(bitStream.readBits(8), Byte.SIZE);
        }
        
        byte codebookCount = (byte) bitStream.readBits(8);
        ogg.bitWrite(codebookCount, Byte.SIZE);
        codebookCount++;
        
        CodebookLibrary codebook = new CodebookLibrary();
        for (int i = 0; i < codebookCount; i++)
        {
            codebook.copy(bitStream, ogg);
        }
        
        while (bitStream.getBitsRead() < setup.getSize() * 8)
        {
            ogg.writeBit(bitStream.readBits(1));
        }
        
        ogg.flushPage(false, false);
        offset = setup.nextOffset();
        
        if (offset != wem.getDataChunkOffset() + wem.getFirstAudioPacketOffset())
        {
            throw new IllegalArgumentException("Error generating vorbis packet");
        }
    }
    
    private void generateIdentificationHeader(OGGStream ogg, WEMData wem)
    {
        ogg.writeVorbisHeader(1);
        
        // version - 32
        ogg.bitWrite(0, Integer.SIZE);
        
        // audio channels - 8
        ogg.bitWrite(wem.getChannelCount(), Byte.SIZE);
        
        // sample rate - 32
        ogg.bitWrite(wem.getSampleRate(), Integer.SIZE);
        
        // bitrate max - 32
        ogg.bitWrite(0, Integer.SIZE);
        
        // bitrate nominal - 32
        ogg.bitWrite(wem.getBytesPerSecond() * 8, Integer.SIZE);
        
        // bitrate min - 32
        ogg.bitWrite(0, Integer.SIZE);
        
        // blocksize 0 - 4
        ogg.bitWrite(wem.getBlockSize0Pow(), 4);
        
        // blocksize 1 - 4
        ogg.bitWrite(wem.getBlockSize1Pow(), 4);
        
        // framing flag
        ogg.writeBit(1);
        ogg.flushPage(false, false);
    }
    
    private void generateCommentHeader(OGGStream ogg, WEMData wem)
    {
//        String vendor = "Converted from WEM to OGG by lol-parser";
        String vendor = "converted from Audiokinetic Wwise by ww2ogg 0.19";
        ogg.writeVorbisHeader(3);
        
        // vendor length - 32
        ogg.bitWrite(vendor.length(), Integer.SIZE);
        
        // vendor string - utf8 vector
        ogg.bitWriteChars(vendor);
        
        
        // user-comment-list
        if (wem.getLoopCount() == 0)
        {
            ogg.bitWrite(0, Integer.SIZE);
        } else
        {
            String loopstart = "LoopStart=" + wem.getLoopStart();
            String loopend   = "LoopEnd=" + wem.getLoopEnd();
            
            ogg.bitWrite(2, Integer.SIZE);
            ogg.bitWriteLengthAndString(loopstart);
            ogg.bitWriteLengthAndString(loopend);
        }
        
        // framing bit - 1
        ogg.writeBit(1);
        
        ogg.flushPage(false, false);
    }
    
    
    private Pair<Integer, Boolean[]> generateSetupHeader(OGGStream ogg, WEMData wem, RandomAccessReader bitStream, boolean inlineCodebook, boolean fullSetup)
    {
        Boolean[] modeBlockFlag = null;
        int       modeBits      = -1;
        
        
        ogg.writeVorbisHeader(5);
        OGGPacket setupPacket = new OGGPacket(bitStream, wem.getDataChunkOffset() + wem.getSetupPacketOffset(), wem.isNoGranule());
        bitStream.seek(setupPacket.getOffset());
        
        if (setupPacket.getGranule() != 0)
        {
            throw new IllegalArgumentException("Error generating vorbis packet");
        }
        
        int codebookCount = bitStream.readBits(8);
        ogg.bitWrite(codebookCount, Byte.SIZE);
        codebookCount++;
        
        if (inlineCodebook)
        {
            CodebookLibrary codebook = new CodebookLibrary();
            for (int i = 0; i < codebookCount; i++)
            {
                if (fullSetup)
                {
                    codebook.copy(bitStream, ogg);
                } else
                {
                    codebook.rebuild(bitStream, 0, ogg);
                }
            }
        } else
        {
            CodebookLibrary codebook = new CodebookLibrary();
            for (int i = 0; i < codebookCount; i++)
            {
                int codeId = bitStream.readBits(10);
                codebook.rebuild(codeId, ogg);
            }
        }
        
        
        byte  timeCountLess1 = 0;
        short dummyValue     = 0;
        ogg.bitWrite(timeCountLess1, 6);
        ogg.bitWrite(dummyValue, Short.SIZE);
        
        if (fullSetup)
        {
            while (bitStream.getBitsRead() < setupPacket.getSize() * 8)
            {
                ogg.writeBit(bitStream.readBits(1));
            }
        } else
        {
            
            
            byte floorCount = (byte) bitStream.readBits(6);
            ogg.bitWrite(floorCount, 6);
            floorCount++;
            
            for (int i = 0; i < floorCount; i++)
            {
                short floorType = 1;
                ogg.bitWrite(floorType, Short.SIZE);
                
                byte floor1partitions = (byte) bitStream.readBits(5);
                ogg.bitWrite(floor1partitions, 5);
                
                int[] floor1PartitionClassList = new int[floor1partitions];
                int   maxClass                 = 0;
                for (int j = 0; j < floor1partitions; j++)
                {
                    byte floor1PartitionClass = (byte) bitStream.readBits(4);
                    ogg.bitWrite(floor1PartitionClass, 4);
                    
                    floor1PartitionClassList[j] = floor1PartitionClass;
                    if (floor1PartitionClass > maxClass)
                    {
                        maxClass = floor1PartitionClass;
                    }
                }
                
                int[] floor1ClassDimentionList = new int[maxClass + 1];
                for (int j = 0; j < maxClass; j++)
                {
                    byte classDimension = (byte) bitStream.readBits(3);
                    ogg.bitWrite(classDimension, 3);
                    
                    floor1ClassDimentionList[j] = classDimension + 1;
                    
                    byte classSubclass = (byte) bitStream.readBits(2);
                    ogg.bitWrite(classSubclass, 2);
                    
                    if (classSubclass != 0)
                    {
                        byte masterbook = (byte) bitStream.readBits(8);
                        ogg.bitWrite(masterbook, Byte.SIZE);
                        
                        if (maxClass >= codebookCount)
                        {
                            throw new IllegalArgumentException("Failed to generate vorbis packet");
                        }
                    }
                    
                    for (int k = 0; k < (1 << classSubclass); k++)
                    {
                        byte subclassBook = (byte) bitStream.readBits(8);
                        ogg.bitWrite(subclassBook, Byte.SIZE);
                        
                        if ((subclassBook - 1) >= 0 && (subclassBook - 1) >= codebookCount)
                        {
                            throw new IllegalArgumentException("Failed to generate vorbis packet");
                        }
                    }
                }
                
                byte floor1Multiplier = (byte) bitStream.readBits(2);
                ogg.bitWrite(floor1Multiplier, 2);
                
                byte rangeBits = (byte) bitStream.readBits(4);
                ogg.bitWrite(rangeBits, 4);
                
                for (int j = 0; j < floor1partitions; j++)
                {
                    int currentClass = floor1PartitionClassList[j];
                    for (int k = 0; k < floor1ClassDimentionList[currentClass]; k++)
                    {
                        ogg.bitWrite(bitStream.readBits(rangeBits), rangeBits);
                    }
                }
            }
            
            byte residueCount = (byte) bitStream.readBits(6);
            ogg.bitWrite(residueCount, 6);
            residueCount++;
            
            
            for (int i = 0; i < residueCount; i++)
            {
                byte residueType = (byte) bitStream.readBits(2);
                ogg.bitWrite(residueCount, Short.SIZE);
                
                if (residueType > 2)
                {
                    throw new IllegalArgumentException("Failed to generate vorbis packet");
                }
                
                int residueBegin           = bitStream.readBits(24);
                int residueEnd             = bitStream.readBits(24);
                int residuePartitionSize   = bitStream.readBits(24);
                int residueClassifications = bitStream.readBits(6);
                int residueClassbook       = bitStream.readBits(8);
                
                ogg.bitWrite(residueBegin, 24);
                ogg.bitWrite(residueEnd, 24);
                ogg.bitWrite(residuePartitionSize, 24);
                ogg.bitWrite(residueClassifications, 6);
                ogg.bitWrite(residueClassbook, 8);
                
                residueClassifications++;
                
                if (residueClassifications >= codebookCount)
                {
                    throw new IllegalArgumentException("Failed to generate vorbis packet");
                }
                
                int[] residueCascade = new int[residueClassifications];
                for (int j = 0; j < residueClassifications; j++)
                {
                    byte highBits = 0;
                    
                    byte lowBits = (byte) bitStream.readBits(3);
                    ogg.bitWrite(lowBits, 3);
                    
                    byte bitFlag = (byte) bitStream.readBits(1);
                    ogg.writeBit(bitFlag);
                    
                    if (bitFlag == 1)
                    {
                        highBits = (byte) bitStream.readBits(5);
                        ogg.bitWrite(highBits, 5);
                    }
                    
                    residueCascade[j] = (highBits * 8) + lowBits;
                }
                
                for (int j = 0; j < residueClassifications; j++)
                {
                    for (int k = 0; k < 8; k++)
                    {
                        if ((residueCascade[j] & (1 << k)) != 0)
                        {
                            byte residueBook = (byte) bitStream.readBits(8);
                            ogg.bitWrite(residueBook, Byte.SIZE);
                            
                            if (residueBook >= codebookCount)
                            {
                                throw new IllegalArgumentException("Failed to generate vorbis packet");
                            }
                        }
                    }
                }
            }
            
            byte mappingCount = (byte) bitStream.readBits(6);
            ogg.bitWrite(mappingCount, 6);
            mappingCount++;
            
            for (int i = 0; i < mappingCount; i++)
            {
                short mappingType = 0;
                ogg.bitWrite(mappingType, Short.SIZE);
                
                byte submapFlag = (byte) bitStream.readBits(1);
                ogg.writeBit(submapFlag);
                
                int submaps = 1;
                if (submapFlag == 1)
                {
                    byte submapLess = (byte) bitStream.readBits(4);
                    submaps = submapLess + 1;
                    ogg.bitWrite(submapLess, 4);
                }
                
                byte squarePolarFlags = (byte) bitStream.readBits(1);
                ogg.writeBit(squarePolarFlags);
                if (squarePolarFlags == 1)
                {
                    byte couplingSteps = (byte) bitStream.readBits(8);
                    ogg.bitWrite(couplingSteps, Byte.SIZE);
                    couplingSteps++;
                    
                    for (int j = 0; j < couplingSteps; j++)
                    {
                        int ilogChannels = UtilHandler.iLog(wem.getChannelCount() - 1);
                        int magnitude    = bitStream.readBits(ilogChannels);
                        int angle        = bitStream.readBits(ilogChannels);
                        
                        ogg.bitWrite(magnitude, ilogChannels);
                        ogg.bitWrite(angle, ilogChannels);
                        
                        if (angle == magnitude || magnitude >= wem.getChannelCount() || angle >= wem.getChannelCount())
                        {
                            throw new IllegalArgumentException("Failed to generate vorbis packet");
                        }
                    }
                }
                
                byte mappingReserved = (byte) bitStream.readBits(2);
                ogg.bitWrite(mappingReserved, 2);
                
                if (mappingReserved != 0)
                {
                    throw new IllegalArgumentException("Failed to generate vorbis packet");
                }
                
                if (submaps > 1)
                {
                    for (int j = 0; j < wem.getChannelCount(); j++)
                    {
                        byte mappingMux = (byte) bitStream.readBits(4);
                        ogg.bitWrite(mappingMux, 4);
                        
                        if (mappingMux >= submaps)
                        {
                            throw new IllegalArgumentException("Failed to generate vorbis packet");
                        }
                    }
                }
                
                for (int j = 0; j < submaps; j++)
                {
                    byte timeConfig = (byte) bitStream.readBits(8);
                    ogg.bitWrite(timeConfig, Byte.SIZE);
                    
                    byte floorNumber = (byte) bitStream.readBits(8);
                    ogg.bitWrite(floorNumber, Byte.SIZE);
                    
                    if (floorNumber >= floorCount)
                    {
                        throw new IllegalArgumentException("Failed to generate vorbis packet");
                    }
                    
                    byte residueNumber = (byte) bitStream.readBits(8);
                    ogg.bitWrite(residueNumber, Byte.SIZE);
                    
                    if (residueNumber >= residueCount)
                    {
                        throw new IllegalArgumentException("Failed to generate vorbis packet");
                    }
                }
            }
            
            byte modeCount = (byte) bitStream.readBits(6);
            ogg.bitWrite(modeCount, 6);
            modeCount++;
            
            modeBlockFlag = new Boolean[modeCount];
            modeBits = UtilHandler.iLog(modeCount - 1);
            
            for (int i = 0; i < modeCount; i++)
            {
                byte blockFlag = (byte) bitStream.readBits(1);
                ogg.bitWrite(blockFlag, Byte.SIZE);
                
                modeBlockFlag[i] = (blockFlag == 1);
                
                short windowType    = 0;
                short transformType = 0;
                ogg.bitWrite(windowType, Short.SIZE);
                ogg.bitWrite(transformType, Short.SIZE);
                
                byte mapping = (byte) bitStream.readBits(8);
                ogg.bitWrite(mapping, Byte.SIZE);
                
                if (mapping >= mappingCount)
                {
                    throw new IllegalArgumentException("Failed to generate vorbis packet");
                }
            }
            
            ogg.writeBit(1);
        }
        ogg.flushPage(false, false);
        
        if (((bitStream.getBitsRead() + 7) / 8) != setupPacket.getSize())
        {
            throw new IllegalArgumentException("Failed to generate vorbis packet");
        }
        
        if (setupPacket.nextOffset() != wem.getDataChunkOffset() + wem.getFirstAudioPacketOffset())
        {
            throw new IllegalArgumentException("Failed to generate vorbis packet");
        }
        
        return new Pair<>(modeBits, modeBlockFlag);
    }
    
    private Pair<Integer, Boolean[]> generateOGGHeader(OGGStream ogg, RandomAccessReader bitStream, WEMData wem, boolean inlineCodebook, boolean fullSetup)
    {
        generateIdentificationHeader(ogg, wem);
        generateCommentHeader(ogg, wem);
        return generateSetupHeader(ogg, wem, bitStream, inlineCodebook, fullSetup);
    }
    
}

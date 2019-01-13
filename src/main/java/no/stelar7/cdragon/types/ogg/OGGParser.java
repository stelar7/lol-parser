package no.stelar7.cdragon.types.ogg;

import no.stelar7.cdragon.types.ogg.data.*;
import no.stelar7.cdragon.types.wem.data.WEMData;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.math.Vector2;

import java.nio.ByteOrder;

public class OGGParser
{
    
    public OGGStream parse(WEMData wem)
    {
        //wem.printInfo();
        
        OGGStream          ogg       = new OGGStream();
        RandomAccessReader bitStream = new RandomAccessReader(wem.getDataBytes(), ByteOrder.LITTLE_ENDIAN);
        
        generateOGGFile(bitStream, ogg, wem);
        
        return ogg;
    }
    
    private void generateOGGFile(RandomAccessReader bitStream, OGGStream ogg, WEMData wem)
    {
        Boolean[] modeBlockFlag     = null;
        int       modeBits          = 0;
        boolean   previousBlockFlag = false;
        
        if (wem.isHeaderTriadPresent())
        {
            generateOGGHeaderTriad(ogg, bitStream, wem);
        } else
        {
            Vector2<Integer, Boolean[]> data = generateOGGHeader(ogg, bitStream, wem, false, false);
            modeBits = data.getFirst();
            modeBlockFlag = data.getSecond();
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
            
            ogg.setGranule((granule == -1) ? 1 : granule);
            
            if (wem.isModPackets())
            {
                if (modeBlockFlag == null)
                {
                    throw new IllegalArgumentException("Didnt load modeBlockFlag");
                }
                
                BitField packetType = new BitField(1, 0);
                packetType.write(ogg);
                
                RandomAccessReader ss = RandomAccessReader.create(bitStream);
                ss.seek(offset);
                
                BitField modeNumber = new BitField(modeBits);
                modeNumber.read(ss);
                modeNumber.write(ogg);
                
                BitField remainder = new BitField(8 - modeBits);
                remainder.read(ss);
                
                bitStream.seek(ss.pos());
                
                
                if (modeBlockFlag[modeNumber.getValue()])
                {
                    bitStream.seek(nextOffset);
                    boolean nextBlockFlag = false;
                    
                    if (nextOffset + packetHeaderSize <= wem.getDataChunkOffset() + wem.getDataChunkSize())
                    {
                        OGGPacket audioPacket = new OGGPacket(bitStream, nextOffset, wem.isNoGranule());
                        int       nextSize    = audioPacket.getSize();
                        
                        if (nextSize > 0)
                        {
                            bitStream.seek(audioPacket.getOffset());
                            BitField nextModeNumber = new BitField(modeBits);
                            nextModeNumber.read(bitStream);
                            
                            nextBlockFlag = modeBlockFlag[nextModeNumber.getValue()];
                        }
                    }
                    
                    BitField previousWindowType = new BitField(1, previousBlockFlag ? 1 : 0);
                    previousWindowType.write(ogg);
                    
                    BitField nextWindowType = new BitField(1, nextBlockFlag ? 1 : 0);
                    nextWindowType.write(ogg);
                    
                    bitStream.seek(offset + 1);
                }
                
                previousBlockFlag = modeBlockFlag[modeNumber.getValue()];
                remainder.write(ogg);
            } else
            {
                BitField b = new BitField(8);
                b.read(bitStream);
                if (b.getValue() < 0)
                {
                    throw new IllegalArgumentException("file truncated");
                }
                b.write(ogg);
            }
            
            for (int i = 1; i < size; i++)
            {
                BitField v = new BitField(8, bitStream.readByte());
                if ((v.getValue() & 0xFF) < 0)
                {
                    throw new IllegalArgumentException("file truncated");
                }
                v.write(ogg);
            }
            
            offset = nextOffset;
            boolean finalPage = (offset == wem.getDataChunkOffset() + wem.getDataChunkSize());
            ogg.flushPage(false, finalPage);
            
        }
        
        if (offset > wem.getDataChunkOffset() + wem.getDataChunkSize())
        {
            throw new IllegalArgumentException("page truncated");
        }
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
        
        while (bitStream.getTotalBitsRead() < setup.getSize() * 8)
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
        
        BitField version = new BitField(32, 0);
        version.write(ogg);
        
        BitField ch = new BitField(8, wem.getChannelCount());
        ch.write(ogg);
        
        BitField srate = new BitField(32, wem.getSampleRate());
        srate.write(ogg);
        
        BitField bitrateMax = new BitField(32, 0);
        bitrateMax.write(ogg);
        
        BitField bitrateNominal = new BitField(32, wem.getBytesPerSecond() * 8);
        bitrateNominal.write(ogg);
        
        BitField bitrateMin = new BitField(32, 0);
        bitrateMin.write(ogg);
        
        BitField blocksize0 = new BitField(4, wem.getBlockSize0Pow());
        blocksize0.write(ogg);
        
        BitField blocksize1 = new BitField(4, wem.getBlockSize1Pow());
        blocksize1.write(ogg);
        
        BitField framing = new BitField(1, 1);
        framing.write(ogg);
        
        ogg.flushPage(false, false);
    }
    
    private void generateCommentHeader(OGGStream ogg, WEMData wem)
    {
        ogg.writeVorbisHeader(3);
        
        String vendor = "converted from Audiokinetic Wwise by ww2ogg 0.24";
        
        BitField vendorSize = new BitField(32, vendor.length());
        vendorSize.write(ogg);
        
        ogg.bitWriteChars(vendor);
        
        
        if (wem.getLoopCount() == 0)
        {
            // user comment count
            BitField userCommentCount = new BitField(32, 0);
            userCommentCount.write(ogg);
        } else
        {
            BitField userCommentCount = new BitField(32, 2);
            userCommentCount.write(ogg);
            
            String loopstart = "LoopStart=" + wem.getLoopStart();
            String loopend   = "LoopEnd=" + wem.getLoopEnd();
            
            ogg.bitWriteLengthAndString(loopstart);
            ogg.bitWriteLengthAndString(loopend);
        }
        
        BitField framing = new BitField(1, 1);
        framing.write(ogg);
        
        ogg.flushPage(false, false);
    }
    
    
    private Vector2<Integer, Boolean[]> generateSetupHeader(OGGStream ogg, WEMData wem, RandomAccessReader bitStream, boolean inlineCodebook, boolean fullSetup)
    {
        Boolean[] modeBlockFlag = null;
        int       modeBits      = -1;
        
        // todo this is being generated wrongly now....??
        
        ogg.writeVorbisHeader(5);
        OGGPacket setupPacket = new OGGPacket(bitStream, wem.getDataChunkOffset() + wem.getSetupPacketOffset(), wem.isNoGranule());
        bitStream.seek(setupPacket.getOffset());
        
        if (setupPacket.getGranule() != 0)
        {
            throw new IllegalArgumentException("Setup packet granule != 0");
        }
        
        BitField codebookCountLess1 = new BitField(8);
        codebookCountLess1.read(bitStream);
        int codebookCount = codebookCountLess1.getValue() + 1;
        codebookCountLess1.write(ogg);
        
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
                BitField codebookId = new BitField(10);
                codebookId.read(bitStream);
                try
                {
                    codebook.rebuild(codebookId.getValue(), ogg);
                } catch (IllegalArgumentException e)
                {
                    if (codebookId.getValue() == 0x342)
                    {
                        BitField codebookIdentifier = new BitField(14);
                        codebookIdentifier.read(bitStream);
                        if (codebookIdentifier.getValue() == 0x1590)
                        {
                            throw new IllegalArgumentException("Invalid codebook 0x342, use fullSetup");
                        }
                    }
                }
            }
        }
        
        BitField timeCountLess1 = new BitField(6, 0);
        timeCountLess1.write(ogg);
        
        BitField dummyTimeValue = new BitField(16, 0);
        dummyTimeValue.write(ogg);
        
        if (fullSetup)
        {
            BitField bitly = new BitField(1);
            while (bitStream.getTotalBitsRead() < setupPacket.getSize() * 8)
            {
                bitly.read(bitStream);
                bitly.write(ogg);
            }
        } else
        {
            
            BitField floorCountLess1 = new BitField(6);
            floorCountLess1.read(bitStream);
            int floorCount = floorCountLess1.getValue() + 1;
            floorCountLess1.write(ogg);
            
            for (int i = 0; i < floorCount; i++)
            {
                BitField floorType = new BitField(16, 1);
                floorType.write(ogg);
                
                BitField floor1Partitions = new BitField(5);
                floor1Partitions.read(bitStream);
                floor1Partitions.write(ogg);
                
                int[] floor1PartitionClassList = new int[floor1Partitions.getValue()];
                
                int maxClass = 0;
                for (int j = 0; j < floor1Partitions.getValue(); j++)
                {
                    BitField floor1PartitionClass = new BitField(4);
                    floor1PartitionClass.read(bitStream);
                    floor1PartitionClass.write(ogg);
                    
                    floor1PartitionClassList[j] = floor1PartitionClass.getValue();
                    
                    if (floor1PartitionClass.getValue() > maxClass)
                    {
                        maxClass = floor1PartitionClass.getValue();
                    }
                }
                
                int[] floor1ClassDimentionList = new int[maxClass + 1];
                
                for (int j = 0; j <= maxClass; j++)
                {
                    BitField classDimensionsLess1 = new BitField(3);
                    classDimensionsLess1.read(bitStream);
                    classDimensionsLess1.write(ogg);
                    
                    floor1ClassDimentionList[j] = classDimensionsLess1.getValue() + 1;
                    
                    BitField classSubclasses = new BitField(2);
                    classSubclasses.read(bitStream);
                    classSubclasses.write(ogg);
                    
                    if (classSubclasses.getValue() != 0)
                    {
                        BitField masterbook = new BitField(8);
                        masterbook.read(bitStream);
                        masterbook.write(ogg);
                        
                        if (masterbook.getValue() >= codebookCount)
                        {
                            throw new IllegalArgumentException("Invalid floor1 masterbook");
                        }
                    }
                    
                    for (int k = 0; k < (1 << classSubclasses.getValue()); k++)
                    {
                        BitField subclassBookPlus1 = new BitField(8);
                        subclassBookPlus1.read(bitStream);
                        subclassBookPlus1.write(ogg);
                        int subclassBook = subclassBookPlus1.getValue() - 1;
                        
                        if (subclassBook >= 0 && subclassBook >= codebookCount)
                        {
                            throw new IllegalArgumentException("Invalid floor1 subclass book");
                        }
                    }
                }
                
                BitField floorMultiplierLess1 = new BitField(2);
                floorMultiplierLess1.read(bitStream);
                floorMultiplierLess1.write(ogg);
                
                BitField rangeBits = new BitField(4);
                rangeBits.read(bitStream);
                rangeBits.write(ogg);
                
                BitField x = new BitField(rangeBits.getValue());
                for (int j = 0; j < floor1Partitions.getValue(); j++)
                {
                    int currentClass = floor1PartitionClassList[j];
                    for (int k = 0; k < floor1ClassDimentionList[currentClass]; k++)
                    {
                        x.read(bitStream);
                        x.write(ogg);
                    }
                }
            }
            
            BitField residueCountLess1 = new BitField(6);
            residueCountLess1.read(bitStream);
            residueCountLess1.write(ogg);
            int residueCount = residueCountLess1.getValue() + 1;
            
            for (int i = 0; i < residueCount; i++)
            {
                BitField residueType = new BitField(2);
                residueType.read(bitStream);
                residueType.write(ogg, 16);
                
                if (residueType.getValue() > 2)
                {
                    throw new IllegalArgumentException("Invalid residue type");
                }
                
                BitField residueBegin                = new BitField(24);
                BitField residueEnd                  = new BitField(24);
                BitField residuePartitionSizeLess1   = new BitField(24);
                BitField residueClassificationsLess1 = new BitField(6);
                BitField residueClassbook            = new BitField(8);
                
                residueBegin.read(bitStream);
                residueEnd.read(bitStream);
                residuePartitionSizeLess1.read(bitStream);
                residueClassificationsLess1.read(bitStream);
                residueClassbook.read(bitStream);
                
                int residueClassifications = residueClassificationsLess1.getValue() + 1;
                
                residueBegin.write(ogg);
                residueEnd.write(ogg);
                residuePartitionSizeLess1.write(ogg);
                residueClassificationsLess1.write(ogg);
                residueClassbook.write(ogg);
                
                if (residueClassifications >= codebookCount)
                {
                    throw new IllegalArgumentException("Invalid residue classbook");
                }
                
                int[] residueCascade = new int[residueClassifications];
                
                BitField bitFlag  = new BitField(1);
                BitField highBits = new BitField(5, 0);
                BitField lowBits  = new BitField(3);
                for (int j = 0; j < residueClassifications; j++)
                {
                    lowBits.read(bitStream);
                    lowBits.write(ogg);
                    
                    bitFlag.read(bitStream);
                    bitFlag.write(ogg);
                    
                    if (bitFlag.getValue() == 1)
                    {
                        highBits = new BitField(5, 0);
                        highBits.read(bitStream);
                        highBits.write(ogg);
                    }
                    
                    residueCascade[j] = (highBits.getValue() * 8) + lowBits.getValue();
                }
                
                for (int j = 0; j < residueClassifications; j++)
                {
                    for (int k = 0; k < 8; k++)
                    {
                        if ((residueCascade[j] & (1 << k)) != 0)
                        {
                            BitField residueBook = new BitField(8);
                            residueBook.read(bitStream);
                            residueBook.write(ogg);
                            
                            if (residueBook.getValue() >= codebookCount)
                            {
                                throw new IllegalArgumentException("Invalid residue book");
                            }
                        }
                    }
                }
            }
            
            
            BitField mappingCountLess1 = new BitField(6);
            mappingCountLess1.read(bitStream);
            mappingCountLess1.write(ogg);
            
            int mappingCount = mappingCountLess1.getValue() + 1;
            
            for (int i = 0; i < mappingCount; i++)
            {
                BitField mappingType = new BitField(16, 0);
                mappingType.write(ogg);
                
                BitField submapsFlag = new BitField(1);
                submapsFlag.read(bitStream);
                submapsFlag.write(ogg);
                
                int submaps = 1;
                if (submapsFlag.getValue() == 1)
                {
                    BitField submapsLess1 = new BitField(4);
                    submapsLess1.read(bitStream);
                    submapsLess1.write(ogg);
                    
                    submaps = submapsLess1.getValue() + 1;
                }
                
                BitField squarePolarFlag = new BitField(1);
                squarePolarFlag.read(bitStream);
                squarePolarFlag.write(ogg);
                
                if (squarePolarFlag.getValue() == 1)
                {
                    
                    BitField couplingStepsLess1 = new BitField(8);
                    couplingStepsLess1.read(bitStream);
                    couplingStepsLess1.write(ogg);
                    
                    int couplingSteps = couplingStepsLess1.getValue() + 1;
                    
                    int      iLogChannels = UtilHandler.iLog(wem.getChannelCount() - 1);
                    BitField magnitude    = new BitField(iLogChannels);
                    BitField angle        = new BitField(iLogChannels);
                    for (int j = 0; j < couplingSteps; j++)
                    {
                        magnitude.read(bitStream);
                        angle.read(bitStream);
                        
                        magnitude.write(ogg);
                        angle.write(ogg);
                        
                        if (angle.getValue() == magnitude.getValue() || magnitude.getValue() >= wem.getChannelCount() || angle.getValue() >= wem.getChannelCount())
                        {
                            throw new IllegalArgumentException("Invalid coupling");
                        }
                    }
                }
                
                BitField mappingReserved = new BitField(2);
                mappingReserved.read(bitStream);
                mappingReserved.write(ogg);
                
                if (mappingReserved.getValue() != 0)
                {
                    throw new IllegalArgumentException("Mapping reserved fieldn nonzero");
                }
                
                if (submaps > 1)
                {
                    BitField mappingMux = new BitField(4);
                    for (int j = 0; j < wem.getChannelCount(); j++)
                    {
                        mappingMux.read(bitStream);
                        mappingMux.write(ogg);
                        
                        if (mappingMux.getValue() >= submaps)
                        {
                            throw new IllegalArgumentException("mapping_mux >= submaps");
                        }
                    }
                }
                
                
                BitField timeConfig    = new BitField(8);
                BitField floorNumber   = new BitField(8);
                BitField residueNumber = new BitField(8);
                for (int j = 0; j < submaps; j++)
                {
                    timeConfig.read(bitStream);
                    timeConfig.write(ogg);
                    
                    
                    floorNumber.read(bitStream);
                    floorNumber.write(ogg);
                    if (floorNumber.getValue() >= floorCount)
                    {
                        throw new IllegalArgumentException("Invalid floor mapping");
                    }
                    
                    residueNumber.read(bitStream);
                    residueNumber.write(ogg);
                    if (residueNumber.getValue() >= residueCount)
                    {
                        throw new IllegalArgumentException("Invalid residue mapping");
                    }
                }
            }
            
            BitField modeCountLess1 = new BitField(6);
            modeCountLess1.read(bitStream);
            modeCountLess1.write(ogg);
            int modeCount = modeCountLess1.getValue() + 1;
            
            modeBlockFlag = new Boolean[modeCount];
            modeBits = UtilHandler.iLog(modeCount - 1);
            
            BitField blockFlag     = new BitField(1);
            BitField windowType    = new BitField(16, 0);
            BitField transformType = new BitField(16, 0);
            BitField mapping       = new BitField(8);
            for (int i = 0; i < modeCount; i++)
            {
                
                blockFlag.read(bitStream);
                blockFlag.write(ogg);
                
                modeBlockFlag[i] = (blockFlag.getValue() != 0);
                
                windowType.write(ogg);
                transformType.write(ogg);
                
                mapping.read(bitStream);
                mapping.write(ogg);
                
                if (mapping.getValue() >= mappingCount)
                {
                    throw new IllegalArgumentException("Invalid mode mapping");
                }
            }
            
            BitField framing = new BitField(1, 1);
            framing.write(ogg);
        }
        
        ogg.flushPage(false, false);
        
        if (((bitStream.getTotalBitsRead() + 7) / 8) != setupPacket.getSize())
        {
            throw new IllegalArgumentException("Didnt read exact setup packet");
        }
        
        if (setupPacket.nextOffset() != wem.getDataChunkOffset() + wem.getFirstAudioPacketOffset())
        {
            throw new IllegalArgumentException("First audio packet doesnt follow setup packet");
        }
        
        return new Vector2<>(modeBits, modeBlockFlag);
    }
    
    private Vector2<Integer, Boolean[]> generateOGGHeader(OGGStream ogg, RandomAccessReader bitStream, WEMData wem, boolean inlineCodebook, boolean fullSetup)
    {
        generateIdentificationHeader(ogg, wem);
        generateCommentHeader(ogg, wem);
        return generateSetupHeader(ogg, wem, bitStream, inlineCodebook, fullSetup);
    }
    
}

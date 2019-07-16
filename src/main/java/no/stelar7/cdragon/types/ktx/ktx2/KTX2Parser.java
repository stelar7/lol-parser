package no.stelar7.cdragon.types.ktx.ktx2;


import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.ktx.ktx2.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class KTX2Parser implements Parseable<KTX2File>
{
    
    @Override
    public KTX2File parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public KTX2File parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public KTX2File parse(RandomAccessReader raf)
    {
        KTX2File data = new KTX2File();
        
        data.setHeader(parseHeader(raf));
        data.setIndecies(parseIndex(raf));
        data.setLevels(parseLevels(raf, data.getHeader().getLevelCount()));
        data.setDataFormatBlocks(parseDataFormatBlocks(raf, data.getIndecies().getDfdByteLength()));
        
        return data;
    }
    
    private List<KTX2FileDataFormatDescriptorBlock> parseDataFormatBlocks(RandomAccessReader raf, int dfdByteLength)
    {
        List<KTX2FileDataFormatDescriptorBlock> blocks = new ArrayList<>();
        
        int read = 0;
        do
        {
            KTX2FileDataFormatDescriptorBlock dfd = new KTX2FileDataFormatDescriptorBlock();
            dfd.setVendorId(raf.readShort());
            dfd.setDescriptorType(raf.readShort());
            dfd.setVersionNumber(raf.readShort());
            dfd.setDescriptorBlockSize(raf.readShort());
            dfd.setColorModel(raf.readByte());
            dfd.setColorPrimaries(raf.readByte());
            dfd.setTransferFunction(raf.readByte());
            dfd.setFlags(raf.readByte());
            dfd.setTexelBlockDimension0(raf.readByte());
            dfd.setTexelBlockDimension1(raf.readByte());
            dfd.setTexelBlockDimension2(raf.readByte());
            dfd.setTexelBlockDimension3(raf.readByte());
            dfd.setBytesPlane0(raf.readByte());
            dfd.setBytesPlane1(raf.readByte());
            dfd.setBytesPlane2(raf.readByte());
            dfd.setBytesPlane3(raf.readByte());
            dfd.setBytesPlane4(raf.readByte());
            dfd.setBytesPlane5(raf.readByte());
            dfd.setBytesPlane6(raf.readByte());
            dfd.setBytesPlane7(raf.readByte());
            
            int sampleCount = (dfd.getDescriptorBlockSize() - 24) / 16;
            for (int i = 0; i < sampleCount; i++)
            {
                KTX2FileDataFormatDescriptorBlockSample sample = new KTX2FileDataFormatDescriptorBlockSample();
                sample.setBitOffset(raf.readShort());
                sample.setBitLength(raf.readByte());
                sample.setChannelType(raf.readByte());
                sample.setSamplePosition0(raf.readByte());
                sample.setSamplePosition1(raf.readByte());
                sample.setSamplePosition2(raf.readByte());
                sample.setSamplePosition3(raf.readByte());
                sample.setSampleLower(raf.readInt());
                sample.setSampleUpper(raf.readInt());
                dfd.addSample(sample);
            }
            
            blocks.add(dfd);
            
            read += dfd.getDescriptorBlockSize();
        } while (read < dfdByteLength);
        
        return blocks;
        
    }
    
    private List<KTX2FileLevel> parseLevels(RandomAccessReader raf, int levelCount)
    {
        List<KTX2FileLevel> levels = new ArrayList<>();
        
        int count = Math.max(1, levelCount);
        for (int i = 0; i < count; i++)
        {
            KTX2FileLevel level = new KTX2FileLevel();
            level.setByteOffset(raf.readLong());
            level.setByteLength(raf.readLong());
            level.setUncompressedByteLength(raf.readLong());
            levels.add(level);
        }
        
        return levels;
    }
    
    private KTX2FileIndecies parseIndex(RandomAccessReader raf)
    {
        KTX2FileIndecies index = new KTX2FileIndecies();
        index.setDfdByteOffset(raf.readInt());
        index.setDfdByteLength(raf.readInt());
        index.setKvdByteOffset(raf.readInt());
        index.setKvdByteLength(raf.readInt());
        index.setSgdByteOffset(raf.readInt());
        index.setSgdByteLength(raf.readInt());
        return index;
    }
    
    private KTX2FileHeader parseHeader(RandomAccessReader raf)
    {
        KTX2FileHeader header = new KTX2FileHeader();
        header.setMagic(raf.readBytes(12));
        header.setFormat(raf.readInt());
        header.setTypeSize(raf.readInt());
        header.setPixelWidth(raf.readInt());
        header.setPixelHeight(raf.readInt());
        header.setArrayElementCount(raf.readInt());
        header.setFaceCount(raf.readInt());
        header.setLevelCount(raf.readInt());
        header.setSupercompressionScheme(raf.readInt());
        return header;
    }
}

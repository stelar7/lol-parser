package no.stelar7.cdragon.types.inibin;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.inibin.data.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiFunction;

public final class InibinParser implements Parseable<InibinFile>
{
    private static Map<BitSet, BiFunction<RandomAccessReader, Integer, Object>> maskBytes = new HashMap<>();
    
    public InibinParser()
    {
        // U32
        maskBytes.put(UtilHandler.longToBitSet(0b1), (reader, nan) -> reader.readInt());
        // F32
        maskBytes.put(UtilHandler.longToBitSet(0b10), (reader, nan) -> reader.readFloat());
        // U8 / 10
        maskBytes.put(UtilHandler.longToBitSet(0b100), (reader, nan) -> reader.readByte() / 10f);
        // U16
        maskBytes.put(UtilHandler.longToBitSet(0b1000), (reader, nan) -> reader.readShort());
        // U8
        maskBytes.put(UtilHandler.longToBitSet(0b10000), (reader, nan) -> reader.readByte());
        // bool
        maskBytes.put(UtilHandler.longToBitSet(0b100000), (reader, nan) -> takeBoolean(reader));
        // RGB
        maskBytes.put(UtilHandler.longToBitSet(0b1000000), (reader, nan) -> reader.readBytes(3));
        // Unknown
        maskBytes.put(UtilHandler.longToBitSet(0b10000000), (reader, nan) -> reader.readBytes(3 * Float.BYTES));
        // RGBA
        maskBytes.put(UtilHandler.longToBitSet(0b10000000000), (reader, nan) -> reader.readBytes(4));
        // Unknown (Rage values?)
        maskBytes.put(UtilHandler.longToBitSet(0b100000000000), (reader, nan) -> reader.readBytes(Float.BYTES * 3));
        // Strings
        maskBytes.put(UtilHandler.longToBitSet(0b1000000000000), this::takeString);
    }
    
    private String takeBoolean(RandomAccessReader raf)
    {
        if (index++ % 8 == 0)
        {
            data = raf.readByte();
        } else
        {
            data = data >> 1;
        }
        
        return String.valueOf((data & 0x1) > 0);
    }
    
    
    private String takeString(RandomAccessReader raf, int segmentKeyCount)
    {
        if (stringStart == -1)
        {
            stringStart = raf.pos() + segmentKeyCount * 2;
        }
        
        int offset = raf.readShort();
        return raf.readFromOffset(stringStart + offset);
    }
    
    private InibinFile         file;
    private RandomAccessReader raf;
    private int stringStart = -1;
    private int index       = 0;
    private int data        = 0;
    
    @Override
    public InibinFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public InibinFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public InibinFile parse(RandomAccessReader raf)
    {
        this.raf = raf;
        file = new InibinFile();
        
        stringStart = -1;
        index = 0;
        data = 0;
        
        file.setHeader(parseHeader());
        file.setKeys(parseKeys());
        return file;
    }
    
    
    public InibinFile parseCompressed(Path path)
    {
        try
        {
            byte[] dataBytes = CompressionHandler.uncompressDEFLATE(Files.readAllBytes(path));
            return parse(new RandomAccessReader(dataBytes, ByteOrder.LITTLE_ENDIAN));
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    
    private Map<String, Object> parseKeys()
    {
        Map<String, Object> keys = new HashMap<>();
        
        BitSet comparator = new BitSet(16);
        BitSet selfBits   = UtilHandler.longToBitSet(file.getHeader().getBitmask());
        
        for (int i = 0; i < selfBits.length(); i++)
        {
            comparator.set(i);
            comparator.and(selfBits);
            if (comparator.isEmpty())
            {
                continue;
            }
            
            BiFunction<RandomAccessReader, Integer, Object> function = maskBytes.get(comparator);
            if (function == null)
            {
                System.out.println("No comparator for bit no. " + comparator.toString());
            }
            
            List<Integer> segmentKeys = new ArrayList<>();
            int           count       = raf.readShort();
            for (int j = 0; j < count; j++)
            {
                segmentKeys.add(raf.readInt());
            }
            segmentKeys.forEach(key -> keys.put(Integer.toUnsignedString(key), function.apply(raf, segmentKeys.size())));
            stringStart = -1;
            
            comparator.set(i, false);
        }
        
        
        return keys;
    }
    
    private InibinHeader parseHeader()
    {
        InibinHeader header = new InibinHeader();
        
        header.setVersion(raf.readByte());
        header.setTableLength(raf.readShort());
        header.setBitmask(raf.readShort());
        
        if (header.getVersion() != 2)
        {
            throw new RuntimeException(String.format("Unknown inibin version (%d), only version 2 is supported", header.getVersion()));
        }
        
        return header;
    }
}

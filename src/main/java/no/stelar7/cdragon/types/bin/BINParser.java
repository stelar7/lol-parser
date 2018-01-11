package no.stelar7.cdragon.types.bin;

import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.util.UtilHandler;
import no.stelar7.cdragon.util.reader.RandomAccessReader;
import no.stelar7.cdragon.util.reader.types.Vector2;

import java.nio.ByteOrder;
import java.nio.file.Path;

public class BINParser
{
    
    public BINFile parse(Path path)
    {
        RandomAccessReader raf = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN);
        
        BINFile file = new BINFile();
        
        file.setHeader(parseHeader(raf));
        parseEntries(file, raf);
        
        return file;
    }
    
    private void parseEntries(BINFile file, RandomAccessReader raf)
    {
        for (int i = 0; i < file.getHeader().getEntryCount(); i++)
        {
            BINEntry entry = new BINEntry();
            
            int lengthCheck = raf.pos() + Integer.BYTES;
            entry.setLenght(raf.readInt());
            entry.setHash(UtilHandler.getBINHash(raf.readInt()));
            entry.setValueCount(raf.readShort());
            
            for (int j = 0; j < entry.getValueCount(); j++)
            {
                BINValue value = readValue(raf);
                entry.getValues().add(value);
            }
            
            file.getEntries().add(entry);
            
            if (lengthCheck + entry.getLenght() != raf.pos())
            {
                System.out.format("Wrong detail size from %s to %s, value %s, expected %s%n", lengthCheck, raf.pos(), raf.pos() - lengthCheck, entry.getLenght());
            }
        }
    }
    
    private BINValue readValue(RandomAccessReader raf)
    {
        BINValue value = new BINValue();
        
        value.setHash(UtilHandler.getBINHash(raf.readInt()));
        value.setType(raf.readByte());
        value.setValue(readByType(value.getType(), raf));
        
        return value;
    }
    
    private Object readByType(byte type, RandomAccessReader raf)
    {
        switch (type)
        {
            case 0:
                return raf.readVec3S();
            case 1:
                return raf.readBoolean();
            case 2:
                return raf.readByte();
            case 3:
                return raf.readByte();
            case 4:
                return raf.readShort();
            case 5:
                return raf.readShort();
            case 6:
                return raf.readInt();
            case 7:
                return raf.readInt();
            case 8:
                return raf.readLong();
            case 9:
                return raf.readLong();
            case 10:
                return raf.readFloat();
            case 11:
                return raf.readVec2F();
            case 12:
                return raf.readVec3F();
            case 13:
                return raf.readVec4F();
            case 14:
                return raf.readMatrix4x4();
            case 15:
                return raf.readVec4B();
            case 16:
                return raf.readString(raf.readShort());
            case 17:
                return raf.readInt();
            case 18:
            {
                BINContainer bc = new BINContainer();
                
                bc.setType(raf.readByte());
                bc.setSize(raf.readInt());
                bc.setCount(raf.readInt());
                for (int i = 0; i < bc.getCount(); i++)
                {
                    bc.getData().add(readByType(bc.getType(), raf));
                }
                
                return bc;
            }
            case 19:
            case 20:
            {
                BINStruct bs = new BINStruct();
                
                bs.setHash(raf.readInt());
                bs.setEntry(raf.readInt());
                bs.setCount(raf.readShort());
                for (int i = 0; i < bs.getCount(); i++)
                {
                    bs.getData().add(readValue(raf));
                }
                
                return bs;
            }
            case 21:
                return raf.readInt();
            case 22:
            {
                BINData bd = new BINData();
                
                bd.setType(raf.readByte());
                bd.setCount(raf.readByte());
                for (int i = 0; i < bd.getCount(); i++)
                {
                    bd.getData().add(readByType(bd.getType(), raf));
                }
                
                return bd;
            }
            case 23:
            {
                BINMap bm = new BINMap();
                bm.setType1(raf.readByte());
                bm.setType2(raf.readByte());
                bm.setSize(raf.readInt());
                bm.setCount(raf.readInt());
                for (int i = 0; i < bm.getCount(); i++)
                {
                    bm.getData().add(new Vector2<>(readByType(bm.getType1(), raf), readByType(bm.getType2(), raf)));
                }
                return bm;
            }
            case 24:
            {
                return raf.readByte();
            }
            default:
                System.out.println("Unknown type: " + type);
                return null;
        }
    }
    
    private BINHeader parseHeader(RandomAccessReader raf)
    {
        BINHeader header = new BINHeader();
        
        header.setMagic(raf.readString(4));
        header.setVersion(raf.readInt());
        header.setEntryCount(raf.readInt());
        
        for (int i = 0; i < header.getEntryCount(); i++)
        {
            header.getEntryTypes().add(raf.readInt());
        }
        
        return header;
    }
}

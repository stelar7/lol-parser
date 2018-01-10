package no.stelar7.cdragon.types.bin;

import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.util.reader.RandomAccessReader;
import no.stelar7.cdragon.util.reader.types.Vector2;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

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
            
            entry.setLenght(raf.readInt());
            entry.setHash(raf.readInt());
            entry.setValueCount(raf.readShort());
            
            for (int j = 0; j < entry.getValueCount(); j++)
            {
                BINValue value = readValue(raf);
                entry.getValues().add(value);
            }
            
            file.getEntries().add(entry);
        }
    }
    
    private BINValue readValue(RandomAccessReader raf)
    {
        BINValue value = new BINValue();
        
        value.setHash(raf.readInt());
        value.setType(raf.readByte());
        value.setValue(readByType(value.getType(), raf));
        
        return value;
    }
    
    private Object readByType(byte type, RandomAccessReader raf)
    {
        if (type == 0)
        {
            return raf.readVec3S();
        } else if (type == 1)
        {
            return raf.readBoolean();
        } else if (type == 3)
        {
            return raf.readByte();
        } else if (type == 5)
        {
            return raf.readShort();
        } else if (type == 6 || type == 7 || type == 17)
        {
            return raf.readInt();
        } else if (type == 9)
        {
            return raf.readVec2I();
        } else if (type == 10)
        {
            return raf.readFloat();
        } else if (type == 11)
        {
            return raf.readVec2F();
        } else if (type == 12)
        {
            return raf.readVec3F();
        } else if (type == 13)
        {
            return raf.readQuaternion();
        } else if (type == 15 || type == 21)
        {
            return raf.readVec4B();
        } else if (type == 16)
        {
            return raf.readString(raf.readShort());
        } else if (type == 18)
        {
            List<Object> datai = new ArrayList<>();
            byte         typei = raf.readByte();
            // int sizei
            raf.readInt();
            int counti = raf.readInt();
            for (int i = 0; i < counti; i++)
            {
                datai.add(readByType(typei, raf));
            }
            return datai;
        } else if (type == 19 || type == 20)
        {
            List<Object> datai = new ArrayList<>();
            // int hashi
            // int entryi
            raf.readInt();
            raf.readInt();
            short counti = raf.readShort();
            for (int i = 0; i < counti; i++)
            {
                datai.add(readValue(raf));
            }
            return datai;
        } else if (type == 22)
        {
            List<Object> datai  = new ArrayList<>();
            byte         typei  = raf.readByte();
            byte         counti = raf.readByte();
            for (int i = 0; i < counti; i++)
            {
                datai.add(readByType(typei, raf));
            }
            return datai;
        } else if (type == 23)
        {
            List<Object> datai  = new ArrayList<>();
            byte         typei  = raf.readByte();
            byte         typei2 = raf.readByte();
            // int sizei
            raf.readInt();
            int counti = raf.readInt();
            for (int i = 0; i < counti; i++)
            {
                datai.add(new Vector2<>(readByType(typei, raf), readByType(typei2, raf)));
            }
            return datai;
        } else
        {
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
        
        return header;
    }
}

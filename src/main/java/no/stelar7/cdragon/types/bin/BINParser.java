package no.stelar7.cdragon.types.bin;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.util.handlers.HashHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.*;
import no.stelar7.cdragon.util.types.math.Vector2;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("ALL")
public class BINParser implements Parseable<BINFile>
{
    public static Set<String> hashes = new HashSet<>();
    
    public BINFile parse(RandomAccessReader raf)
    {
        BINFile file = new BINFile();
        parseHeader(file, raf);
        if (file.getHeader() == null)
        {
            return null;
        }
        
        parseEntries(file, raf);
        return file;
    }
    
    @Override
    public BINFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public BINFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    private void parseEntries(BINFile file, RandomAccessReader raf)
    {
        for (int i = 0; i < file.getHeader().getEntryCount(); i++)
        {
            BINEntry entry = new BINEntry();
            
            int lengthCheck = raf.pos() + Integer.BYTES;
            entry.setType(HashHandler.getBINHash(file.getHeader().getEntryTypes().get(i)));
            entry.setLenght(raf.readInt());
            entry.setHash(HashHandler.getBINHash(raf.readInt()));
            entry.setValueCount(raf.readShort());
            hashes.add(entry.getHash());
            
            for (int j = 0; j < entry.getValueCount(); j++)
            {
                BINValue value = readValue(raf);
                entry.getValues().add(value);
            }
            
            file.getEntries().add(entry);
            
            if (lengthCheck + entry.getLenght() != raf.pos())
            {
                System.out.format("Wrong detail size from %s to %s, value %s, expected %s%n", lengthCheck, raf.pos(), raf.pos() - lengthCheck, entry.getLenght());
                break;
            }
        }
    }
    
    private BINValue readValue(RandomAccessReader raf)
    {
        BINValue value = new BINValue();
        
        value.setHash(HashHandler.getBINHash(raf.readInt()));
        value.setType(BINValueType.valueOf(raf.readByte()));
        value.setValue(readByType(value.getType(), raf));
        hashes.add(value.getHash());
        
        return value;
    }
    
    private Object readByType(BINValueType type, RandomAccessReader raf)
    {
        switch (type)
        {
            case V3_SHORT:
                return raf.readVec3S();
            case BOOLEAN:
                return raf.readBoolean();
            case SIGNED_BYTE:
                return raf.readByte();
            case BYTE:
                return raf.readByte();
            case SIGNED_SHORT:
                return raf.readShort();
            case SHORT:
                return raf.readShort();
            case SIGNED_INT:
                return raf.readInt();
            case INT:
                return raf.readInt();
            case SIGNED_LONG:
                return raf.readLong();
            case LONG:
                return raf.readLong();
            case FLOAT:
                return raf.readFloat();
            case V2_FLOAT:
                return raf.readVec2F();
            case V3_FLOAT:
                return raf.readVec3F();
            case V4_FLOAT:
                return raf.readVec4F();
            case M4X4_FLOAT:
                return raf.readMatrix4x4();
            case RGBA_BYTE:
                return raf.readVec4B();
            case STRING:
                return raf.readString(raf.readShort());
            case STRING_HASH:
                return raf.readInt();
            case CONTAINER:
                BINContainer bc = new BINContainer();
                
                bc.setType(BINValueType.valueOf(raf.readByte()));
                bc.setSize(raf.readInt());
                bc.setCount(raf.readInt());
                
                for (int i = 0; i < bc.getCount(); i++)
                {
                    bc.getData().add(readByType(bc.getType(), raf));
                }
                
                return bc;
            case STRUCTURE:
            case EMBEDDED:
                BINStruct bs = new BINStruct();
                
                bs.setHash(HashHandler.getBINHash(raf.readInt()));
                hashes.add(bs.getHash());
                
                if (bs.getHash().equalsIgnoreCase("00000000"))
                {
                    return bs;
                }
                
                bs.setSize(raf.readInt());
                bs.setCount(raf.readShort());
                for (int i = 0; i < bs.getCount(); i++)
                {
                    bs.getData().add(readValue(raf));
                }
                
                return bs;
            case LINK_OFFSET:
                String hash = HashHandler.toHex((long) raf.readInt(), 8);
                if (hash.length() > 8)
                {
                    hash = hash.substring(8);
                }
                
                return hash;
            case OPTIONAL_DATA:
                BINData bd = new BINData();
                
                bd.setType(BINValueType.valueOf(raf.readByte()));
                bd.setCount(raf.readByte());
                for (int i = 0; i < bd.getCount(); i++)
                {
                    bd.getData().add(readByType(bd.getType(), raf));
                }
                
                return bd;
            case PAIR:
                BINMap bm = new BINMap();
                
                bm.setType1(BINValueType.valueOf(raf.readByte()));
                bm.setType2(BINValueType.valueOf(raf.readByte()));
                bm.setSize(raf.readInt());
                bm.setCount(raf.readInt());
                
                for (int i = 0; i < bm.getCount(); i++)
                {
                    bm.getData().add(new Vector2<>(readByType(bm.getType1(), raf), readByType(bm.getType2(), raf)));
                }
                
                return bm;
            case BOOLEAN_FLAGS:
                return raf.readBoolean();
            default:
                throw new RuntimeException("Unknown type: " + type + " at location: " + (raf.pos() - 1));
        }
    }
    
    private void parseHeader(BINFile file, RandomAccessReader raf)
    {
        BINHeader header = new BINHeader();
        header.setMagic(raf.readString(4));
        
        if (!"PROP".equalsIgnoreCase(header.getMagic()))
        {
            file.setHeader(null);
            return;
        }
        
        header.setVersion(raf.readInt());
        
        if (header.getVersion() >= 2)
        {
            int fileCount = raf.readInt();
            for (int i = 0; i < fileCount; i++)
            {
                short  length = raf.readShort();
                String link   = raf.readString(length);
                file.getLinkedFiles().add(link);
            }
        }
        
        header.setEntryCount(raf.readInt());
        for (int i = 0; i < header.getEntryCount(); i++)
        {
            header.getEntryTypes().add(raf.readInt());
        }
        
        file.setHeader(header);
    }
}

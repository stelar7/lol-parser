package no.stelar7.cdragon.types.rst;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.*;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class RSTParser implements Parseable<RSTFile>
{
    
    @Override
    public RSTFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public RSTFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public RSTFile parse(RandomAccessReader raf)
    {
        RSTFile file = new RSTFile();
        
        String magic = raf.readString(3);
        if (!magic.equals("RST"))
        {
            return null;
        }
        
        int major = raf.readByte();
        file.setMagic(magic);
        file.setMajor(major);
        
        if (major <= 2)
        {
            int minor = raf.readByte();
            file.setMinor(minor);
            
            int    configLength = raf.readInt();
            String config       = raf.readString(configLength);
            
            file.setConfig(config);
        }
        
        if ((major != 2 && major != 3 && major != 4) || file.getMinor() > 1)
        {
            System.out.println("Invalid major/minor version");
            return null;
        }
        
        List<Pair<Integer, Long>> entries = new ArrayList<>();
        
        int entryCount = raf.readInt();
        int hashBits   = major > 3 ? 39 : 40;
        for (int i = 0; i < entryCount; i++)
        {
            long entryHash = raf.readLong();
            
            int  offset    = (int) (entryHash >>> hashBits);
            long valueHash = entryHash & ((1L << hashBits) - 1);
            
            entries.add(new Pair<>(offset, valueHash));
        }
        
        if (major <= 2)
        {
            int endByte = raf.readByte();
            if (endByte != file.getMinor())
            {
                System.out.println("End byte doesnt match minor");
            }
        }
        
        ByteArray remaining = new ByteArray(raf.readRemaining());
        entries.forEach(p -> {
            int  offset = p.getA();
            long hash   = p.getB();
            
            ByteArray data  = remaining.copyOfRange(offset, remaining.indexOf(0x00, offset + 1));
            String    value = new String(data.getDataRaw()).replace("\u0000", "");
            file.getEntries().put(hash, value);
        });
        
        return file;
    }
    
}

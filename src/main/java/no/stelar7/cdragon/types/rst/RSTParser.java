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
        int minor = raf.readByte();
        
        if (major != 2 || minor > 1)
        {
            System.out.println("Invalid major/minor version");
            return null;
        }
        
        file.setMagic(magic);
        file.setMajor(major);
        file.setMinor(minor);
        
        if (minor == 1)
        {
            int    configLength = raf.readInt();
            String config       = raf.readString(configLength);
            
            file.setConfig(config);
        }
        
        List<Pair<Integer, Long>> entries = new ArrayList<>();
        
        int entryCount = raf.readInt();
        for (int i = 0; i < entryCount; i++)
        {
            long hash = raf.readLong();
            entries.add(new Pair<>(Math.toIntExact(hash >>> 40), hash & 0xFFFFFFFFFFL));
        }
        
        int endByte = raf.readByte();
        if (endByte != minor)
        {
            System.out.println("End byte doesnt match minor");
        }
        
        ByteArray remaining = new ByteArray(raf.readRemaining());
        entries.stream()
               .sorted(Comparator.comparing(Pair::getB))
               .forEach(p -> {
                   int  offset = Math.toIntExact(p.getA());
                   long hash   = p.getB();
            
                   ByteArray data  = remaining.copyOfRange(offset, remaining.indexOf(0x00, offset + 1));
                   String    value = new String(data.getDataRaw());
                   file.getEntries().put(hash, value);
               });
        
        return file;
    }
    
}

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
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
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
        
        if (minor == 1)
        {
            int    configLength = raf.readInt();
            String config       = raf.readString(configLength);
        }
        
        List<Pair<Long, Long>> entries    = new ArrayList<>();
        int                    entryCount = raf.readInt();
        for (int i = 0; i < entryCount; i++)
        {
            long hash = raf.readLong();
            entries.add(new Pair<>(hash >>> 40, hash & 0xFFFFFFFFFFL));
        }
        
        int endByte = raf.readByte();
        if (endByte != minor)
        {
            System.out.println("End byte doesnt match minor");
        }
        
        String            remaining = raf.readAsString();
        Map<Long, String> result    = new HashMap<>();
        entries.forEach(p -> {
            int  offset = Math.toIntExact(p.getA());
            long hash   = p.getB();
            
            if (offset < remaining.length())
            {
                int    end   = remaining.indexOf('\0', offset);
                String value = remaining.substring(offset, end > 0 ? end : remaining.length());
                result.put(hash, value);
            }
        });
        
        return file;
    }
    
}

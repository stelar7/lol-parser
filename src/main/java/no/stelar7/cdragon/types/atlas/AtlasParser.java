package no.stelar7.cdragon.types.atlas;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.atlas.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class AtlasParser implements Parseable<AtlasFile>
{
    
    @Override
    public AtlasFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public AtlasFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public AtlasFile parse(RandomAccessReader raf)
    {
        try
        {
            Map<String, List<AtlasEntry>> data        = new HashMap<>();
            int                           bundleCount = raf.readInt();
            for (int i = 0; i < bundleCount; i++)
            {
                String           bundleName = raf.readIntString();
                List<AtlasEntry> entries    = parseEntries(raf);
                data.put(bundleName, entries);
            }
            
            AtlasFile file = new AtlasFile();
            file.setData(data);
            return file;
            
        } catch (Exception e)
        {
            return null;
        }
    }
    
    private List<AtlasEntry> parseEntries(RandomAccessReader raf)
    {
        int              bundleEntryCount = raf.readInt();
        List<AtlasEntry> entries          = new ArrayList<>();
        for (int j = 0; j < bundleEntryCount; j++)
        {
            AtlasEntry entry = new AtlasEntry();
            entry.setName(raf.readIntString());
            entry.setStartX(raf.readFloat());
            entry.setStartY(raf.readFloat());
            entry.setEndX(raf.readFloat());
            entry.setEndY(raf.readFloat());
            entry.setUnknown(raf.readFloat());
            entries.add(entry);
        }
        
        return entries;
    }
}

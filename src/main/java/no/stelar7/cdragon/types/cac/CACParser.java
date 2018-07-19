package no.stelar7.cdragon.types.cac;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.cac.data.CACFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class CACParser implements Parseable<CACFile>
{
    @Override
    public CACFile parse(Path path)
    {
        return UtilHandler.getGson().fromJson(UtilHandler.readAsString(path), CACFile.class);
    }
    
    @Override
    public CACFile parse(ByteArray data)
    {
        return UtilHandler.getGson().fromJson(new String(data.getData(), StandardCharsets.UTF_8), CACFile.class);
    }
    
    @Override
    public CACFile parse(RandomAccessReader raf)
    {
        return UtilHandler.getGson().fromJson(raf.readAsString(), CACFile.class);
    }
}

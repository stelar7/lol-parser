package no.stelar7.cdragon.types.cac;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.cac.data.CACFile;
import no.stelar7.cdragon.util.handlers.JsonHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

/**
 * CAC files are plain JSON
 */
public class CACParser implements Parseable<CACFile>
{
    @Override
    public CACFile parse(RandomAccessReader raf)
    {
        return JsonHandler.getGson().fromJson(raf.readAsString(), CACFile.class);
    }
}

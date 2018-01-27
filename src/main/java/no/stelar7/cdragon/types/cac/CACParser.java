package no.stelar7.cdragon.types.cac;

import no.stelar7.cdragon.types.cac.data.CACFile;
import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.nio.file.Path;

public class CACParser
{
    public CACFile parse(Path path)
    {
        return UtilHandler.getGson().fromJson(UtilHandler.readAsString(path), CACFile.class);
    }
}

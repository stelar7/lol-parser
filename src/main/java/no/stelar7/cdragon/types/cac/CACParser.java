package no.stelar7.cdragon.types.cac;

import no.stelar7.api.l4j8.basic.utils.Utils;
import no.stelar7.cdragon.types.cac.data.CACFile;
import no.stelar7.cdragon.util.UtilHandler;

import java.nio.file.Path;

public class CACParser
{
    public CACFile parse(Path path)
    {
        return Utils.getGson().fromJson(UtilHandler.readAsString(path), CACFile.class);
    }
}

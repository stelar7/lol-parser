package offline;

import no.stelar7.cdragon.util.handlers.HashHandler;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Keeps the offline tests away from the real hash tables.
 * <p>
 * HashHandler lazily loads the bin/wad/ini name tables from the (large) bundled json files, from the C:\cdragon stores,
 * or from GitHub when those are missing. Calling {@link #useEmpty()} first (from a @BeforeAll, before BINParser is
 * initialized, since its static block preloads the bin hashes) installs empty tables so nothing is read or downloaded.
 */
public final class OfflineHashes
{
    private OfflineHashes()
    {
    }

    public static synchronized void useEmpty()
    {
        try
        {
            for (String name : new String[]{"binHashNames", "wadHashNames", "iniHashNames"})
            {
                Field f = HashHandler.class.getDeclaredField(name);
                f.setAccessible(true);
                if (f.get(null) == null)
                {
                    f.set(null, new HashMap<>());
                }
            }
        } catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("HashHandler fields changed, update OfflineHashes", e);
        }
    }
}

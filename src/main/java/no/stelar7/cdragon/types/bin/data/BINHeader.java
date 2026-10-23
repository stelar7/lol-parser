package no.stelar7.cdragon.types.bin.data;


import java.util.*;

public final class BINHeader
{
    private final String        magic;
    private final int           version;
    private final int           entryCount;
    private final boolean       isPatch;
    private final List<Integer> entryTypes;
    private final int           patchVersion;
    private final int           patchUnknown;
    
    public BINHeader(String magic, int version, int entryCount, boolean isPatch, List<Integer> entryTypes, int patchVersion, int patchUnknown)
    {
        this.magic = magic;
        this.version = version;
        this.entryCount = entryCount;
        this.isPatch = isPatch;
        this.entryTypes = entryTypes;
        this.patchVersion = patchVersion;
        this.patchUnknown = patchUnknown;
    }
    
    public String getMagic()
    {
        return magic;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public boolean isPatch()
    {
        return isPatch;
    }
    
    public int getEntryCount()
    {
        return entryCount;
    }
    
    public List<Integer> getEntryTypes()
    {
        return entryTypes;
    }
    
    /**
     * The version from the PTCH header (only set for patch files)
     */
    public int getPatchVersion()
    {
        return patchVersion;
    }
    
    /**
     * The unknown u32 after the PTCH version (only set for patch files)
     */
    public int getPatchUnknown()
    {
        return patchUnknown;
    }
}

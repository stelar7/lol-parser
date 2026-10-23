package no.stelar7.cdragon.types.inibin.data;

public final class InibinHeader
{
    private final int version;
    private final int tableLength;
    private final int bitmask;
    
    public InibinHeader(int version, int tableLength, int bitmask)
    {
        this.version = version;
        this.tableLength = tableLength;
        this.bitmask = bitmask;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public int getTableLength()
    {
        return tableLength;
    }
    
    public int getBitmask()
    {
        return bitmask;
    }
}

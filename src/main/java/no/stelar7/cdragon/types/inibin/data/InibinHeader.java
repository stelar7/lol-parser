package no.stelar7.cdragon.types.inibin.data;

public class InibinHeader
{
    private int version;
    private int tableLength;
    private int bitmask;
    
    public int getVersion()
    {
        return version;
    }
    
    public void setVersion(int version)
    {
        this.version = version;
    }
    
    public int getTableLength()
    {
        return tableLength;
    }
    
    public void setTableLength(int tableLength)
    {
        this.tableLength = tableLength;
    }
    
    public int getBitmask()
    {
        return bitmask;
    }
    
    public void setBitmask(int bitmask)
    {
        this.bitmask = bitmask;
    }
}

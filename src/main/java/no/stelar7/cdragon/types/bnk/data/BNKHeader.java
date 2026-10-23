package no.stelar7.cdragon.types.bnk.data;

public class BNKHeader
{
    private final String section;
    private final int    length;
    private final int    dataStart;
    
    public BNKHeader(String section, int length, int dataStart)
    {
        this.section = section;
        this.length = length;
        this.dataStart = dataStart;
    }
    
    /**
     * Copies the section header, used by the parsed sections
     */
    protected BNKHeader(BNKHeader header)
    {
        this(header.getSection(), header.getLength(), header.getDataStart());
    }
    
    public String getSection()
    {
        return section;
    }
    
    public int getLength()
    {
        return length;
    }
    
    public int getDataStart()
    {
        return dataStart;
    }
}

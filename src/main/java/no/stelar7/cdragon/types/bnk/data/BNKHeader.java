package no.stelar7.cdragon.types.bnk.data;

public class BNKHeader
{
    private String section;
    private int    length;
    private int    dataStart;
    
    public String getSection()
    {
        return section;
    }
    
    public void setSection(String section)
    {
        this.section = section;
    }
    
    public int getLength()
    {
        return length;
    }
    
    public void setLength(int length)
    {
        this.length = length;
    }
    
    public int getDataStart()
    {
        return dataStart;
    }
    
    public void setDataStart(int dataStart)
    {
        this.dataStart = dataStart;
    }
}

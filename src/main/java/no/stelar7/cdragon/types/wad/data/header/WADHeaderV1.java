package no.stelar7.cdragon.types.wad.data.header;

public class WADHeaderV1 extends WADHeaderBase
{
    private int entryHeaderOffset;
    private int entryHeaderCellSize;
    
    public WADHeaderV1(WADHeaderBase base)
    {
        this.magic = base.magic;
        this.major = base.major;
        this.minor = base.minor;
        this.fileCount = base.fileCount;
    }
    
    public int getEntryHeaderOffset()
    {
        return entryHeaderOffset;
    }
    
    public void setEntryHeaderOffset(int entryHeaderOffset)
    {
        this.entryHeaderOffset = entryHeaderOffset;
    }
    
    public int getEntryHeaderCellSize()
    {
        return entryHeaderCellSize;
    }
    
    public void setEntryHeaderCellSize(int entryHeaderCellSize)
    {
        this.entryHeaderCellSize = entryHeaderCellSize;
    }
}
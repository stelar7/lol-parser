package no.stelar7.cdragon.wad.data.header;

import lombok.Data;

@Data
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
}
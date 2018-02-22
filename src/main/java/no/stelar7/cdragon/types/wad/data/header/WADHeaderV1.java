package no.stelar7.cdragon.types.wad.data.header;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
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
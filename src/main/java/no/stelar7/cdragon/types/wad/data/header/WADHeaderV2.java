package no.stelar7.cdragon.types.wad.data.header;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class WADHeaderV2 extends WADHeaderBase
{
    private int    ECDSALength;
    private byte[] ECDSA;
    private byte[] ECDSAPadding;
    private long   fileChecksum;
    private int    entryHeaderOffset;
    private int    entryHeaderCellSize;
    
    public WADHeaderV2(WADHeaderBase base)
    {
        this.magic = base.magic;
        this.major = base.major;
        this.minor = base.minor;
        this.fileCount = base.fileCount;
    }
}
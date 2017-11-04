package no.stelar7.cdragon.wad.data.header;

import lombok.Data;

@Data
public class WADHeaderV3 extends WADHeaderBase
{
    private byte[] ECDSA;
    private long   checksum;
    
    
    public WADHeaderV3(WADHeaderBase base)
    {
        this.magic = base.magic;
        this.major = base.major;
        this.minor = base.minor;
        this.fileCount = base.fileCount;
    }
}
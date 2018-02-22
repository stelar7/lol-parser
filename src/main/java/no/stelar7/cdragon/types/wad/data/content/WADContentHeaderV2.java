package no.stelar7.cdragon.types.wad.data.content;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WADContentHeaderV2 extends WADContentHeaderV1
{
    private boolean duplicate;
    private short   padding;
    private long    sha256;
    
    public WADContentHeaderV2(WADContentHeaderV1 header)
    {
        this.pathHash = header.pathHash;
        this.offset = header.offset;
        this.compressedFileSize = header.compressedFileSize;
        this.fileSize = header.fileSize;
        this.compressed = header.compressed;
    }
}

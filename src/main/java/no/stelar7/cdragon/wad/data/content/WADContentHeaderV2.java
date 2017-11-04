package no.stelar7.cdragon.wad.data.content;

import lombok.Data;

@Data
public class WADContentHeaderV2 extends WADContentHeaderV1
{
    private boolean duplicate;
    private int     unknown;
    private int     unknown2;
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

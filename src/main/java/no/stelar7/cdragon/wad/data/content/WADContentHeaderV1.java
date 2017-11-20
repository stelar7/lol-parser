package no.stelar7.cdragon.wad.data.content;

import lombok.Data;

@Data
public class WADContentHeaderV1
{
    protected long pathHash;
    protected int  offset;
    protected int  compressedFileSize;
    protected int  fileSize;
    //0 = uncompressed, 1 = gzip, 2 = reference(File is located in another wad doesnt say which... just the file name -.-), 3 = zstd
    protected byte compressed;
    
    public boolean isCompressed()
    {
        return compressed > 0;
    }
}

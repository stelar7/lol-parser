package no.stelar7.cdragon.types.wad.data.content;

import lombok.Data;

@Data
public class WADContentHeaderV1
{
    protected long pathHash;
    protected int  offset;
    protected int  compressedFileSize;
    protected int  fileSize;
    protected byte compressed;
    
    public boolean isCompressed()
    {
        return compressed > 0;
    }
}

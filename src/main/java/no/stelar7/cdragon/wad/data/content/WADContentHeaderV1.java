package no.stelar7.cdragon.wad.data.content;

import lombok.*;

@Data
public class WADContentHeaderV1
{
    protected long    pathHash;
    protected int     offset;
    protected int     compressedFileSize;
    protected int     fileSize;
    protected boolean compressed;
}

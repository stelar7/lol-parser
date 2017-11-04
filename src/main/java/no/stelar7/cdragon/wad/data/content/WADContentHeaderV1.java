package no.stelar7.cdragon.wad.data.content;

import lombok.*;

@Data
public class WADContentHeaderV1
{
    protected long    pathHash;
    protected long    offset;
    protected int     compressedFileSize;
    protected int     fileSize;
    protected boolean compressed;
}

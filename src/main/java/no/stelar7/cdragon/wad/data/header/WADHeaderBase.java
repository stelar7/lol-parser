package no.stelar7.cdragon.wad.data.header;

import lombok.Data;

@Data
public class WADHeaderBase
{
    protected String magic;
    protected int    major;
    protected int    minor;
    protected long   fileCount;
}

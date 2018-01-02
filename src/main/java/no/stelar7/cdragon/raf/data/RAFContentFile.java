package no.stelar7.cdragon.raf.data;

import lombok.Data;

@Data
public class RAFContentFile
{
    private final int hash;
    private final int offset;
    private final int size;
    private final int pathIndex;
}

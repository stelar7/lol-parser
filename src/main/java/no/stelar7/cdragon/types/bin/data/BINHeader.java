package no.stelar7.cdragon.types.bin.data;

import lombok.Data;

import java.util.*;

@Data
public class BINHeader
{
    private String magic;
    private int    version;
    private int    entryCount;
    private int    linkedFileCount;
    private List<Integer> entryTypes  = new ArrayList<>();
    private List<String>  linkedFiles = new ArrayList<>();
}

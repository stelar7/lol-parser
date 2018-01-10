package no.stelar7.cdragon.types.bin.data;

import lombok.Data;

import java.util.*;

@Data
public class BINEntry
{
    private int    lenght;
    private String hash;
    private short  valueCount;
    private List<BINValue> values = new ArrayList<>();
    
}

package no.stelar7.cdragon.types.bin.data;

import lombok.Data;

import java.util.List;

@Data
public class BINEntry
{
    private int            lenght;
    private int            hash;
    private short          valueCount;
    private List<BINValue> values;
    
}

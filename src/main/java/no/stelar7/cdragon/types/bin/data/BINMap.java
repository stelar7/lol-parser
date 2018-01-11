package no.stelar7.cdragon.types.bin.data;

import lombok.Data;

import java.util.*;

@Data
public class BINMap
{
    private byte type1;
    private byte type2;
    private int  size;
    private int  count;
    private List<Object> data = new ArrayList<>();
}

package no.stelar7.cdragon.types.bin.data;

import lombok.Data;

import java.util.*;

@Data
public class BINContainer
{
    private byte type;
    private int  size;
    private int  count;
    private List<Object> data = new ArrayList<>();
}

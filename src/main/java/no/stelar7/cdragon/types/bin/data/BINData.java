package no.stelar7.cdragon.types.bin.data;

import lombok.Data;

import java.util.*;

@Data
public class BINData
{
    private byte type;
    private byte count;
    private List<Object> data = new ArrayList<>();
}

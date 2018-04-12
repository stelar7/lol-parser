package no.stelar7.cdragon.types.bin.data;

import lombok.Data;

import java.util.*;

@Data
public class BINData
{
    private BINValueType type;
    private byte         count;
    private List<Object> data = new ArrayList<>();
}

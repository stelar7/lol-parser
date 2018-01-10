package no.stelar7.cdragon.types.bin.data;

import lombok.Data;

import java.util.*;

@Data
public class BINFile
{
    private BINHeader header;
    private List<BINEntry> entries = new ArrayList<>();
}

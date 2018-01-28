package no.stelar7.cdragon.types.bnk.data;

import lombok.Data;

@Data
public class BNKHeader
{
    private String section;
    private int    length;
}

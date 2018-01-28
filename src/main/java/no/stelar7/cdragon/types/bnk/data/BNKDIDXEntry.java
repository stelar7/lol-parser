package no.stelar7.cdragon.types.bnk.data;

import lombok.Data;

@Data
public class BNKDIDXEntry
{
    private int fileId;
    private int fileOffset;
    private int fileSize;
}

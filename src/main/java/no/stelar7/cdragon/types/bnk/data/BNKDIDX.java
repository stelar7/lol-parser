package no.stelar7.cdragon.types.bnk.data;


import java.util.*;

public final class BNKDIDX extends BNKHeader
{
    public BNKDIDX(BNKHeader header, List<BNKDIDXEntry> entries)
    {
        super(header);
        this.entries = entries;
    }
    
    private final List<BNKDIDXEntry> entries;
    
    public List<BNKDIDXEntry> getEntries()
    {
        return entries;
    }
}

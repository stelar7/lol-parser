package no.stelar7.cdragon.types.bnk.data;


import java.util.*;

public class BNKDIDX extends BNKHeader
{
    public BNKDIDX(BNKHeader header)
    {
        setDataStart(header.getDataStart());
        setSection(header.getSection());
        setLength(header.getLength());
    }
    
    private List<BNKDIDXEntry> entries = new ArrayList<>();
    
    public List<BNKDIDXEntry> getEntries()
    {
        return entries;
    }
    
    public void setEntries(List<BNKDIDXEntry> entries)
    {
        this.entries = entries;
    }
}

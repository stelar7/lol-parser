package no.stelar7.cdragon.types.bnk.data;

import no.stelar7.cdragon.types.bnk.BNKHIRCObject;

import java.util.*;

public final class BNKHIRC extends BNKHeader
{
    public BNKHIRC(BNKHeader header, List<BNKHIRCObject> hircData)
    {
        super(header);
        this.hircData = hircData;
    }
    
    private final List<BNKHIRCObject> hircData;
    
    public List<BNKHIRCObject> getHircData()
    {
        return hircData;
    }
}

package no.stelar7.cdragon.types.bnk.data;

import no.stelar7.cdragon.types.bnk.BNKHIRCObject;

import java.util.*;

public class BNKHIRC extends BNKHeader
{
    public BNKHIRC(BNKHeader header)
    {
        setDataStart(header.getDataStart());
        setSection(header.getSection());
        setLength(header.getLength());
    }
    
    List<BNKHIRCObject> hircData = new ArrayList<>();
    
    public List<BNKHIRCObject> getHircData()
    {
        return hircData;
    }
    
    public void setHircData(List<BNKHIRCObject> hircData)
    {
        this.hircData = hircData;
    }
}

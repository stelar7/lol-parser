package no.stelar7.cdragon.types.bnk.data;

import lombok.*;

import java.util.*;

@Data
@ToString(callSuper = true)
public class BNKDIDX extends BNKHeader
{
    public BNKDIDX(BNKHeader header)
    {
        setSection(header.getSection());
        setLength(header.getLength());
    }
    
    private List<BNKDIDXEntry> entries = new ArrayList<>();
    
}

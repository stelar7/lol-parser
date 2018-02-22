package no.stelar7.cdragon.types.bnk.data;

import lombok.*;

import java.util.*;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BNKDIDX extends BNKHeader
{
    public BNKDIDX(BNKHeader header)
    {
        setDataStart(header.getDataStart());
        setSection(header.getSection());
        setLength(header.getLength());
    }
    
    private List<BNKDIDXEntry> entries = new ArrayList<>();
    
}

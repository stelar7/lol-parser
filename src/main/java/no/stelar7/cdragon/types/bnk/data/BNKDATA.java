package no.stelar7.cdragon.types.bnk.data;

import lombok.*;

import java.util.*;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BNKDATA extends BNKHeader
{
    public BNKDATA(BNKHeader header)
    {
        setDataStart(header.getDataStart());
        setSection(header.getSection());
        setLength(header.getLength());
    }
    
    private List<BNKDATAWEMFile> wemFiles = new ArrayList<>();
    
}

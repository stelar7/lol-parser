package no.stelar7.cdragon.types.bnk.data;

import lombok.*;

import java.util.*;

@Data
@ToString(callSuper = true)
public class BNKDATA extends BNKHeader
{
    public BNKDATA(BNKHeader header)
    {
        setSection(header.getSection());
        setLength(header.getLength());
    }
    
    private List<BNKDATAWEMFile> wemFiles = new ArrayList<>();
    
}

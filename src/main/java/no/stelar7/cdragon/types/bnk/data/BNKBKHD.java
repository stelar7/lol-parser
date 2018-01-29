package no.stelar7.cdragon.types.bnk.data;

import lombok.*;

@Data
@ToString(callSuper = true)
public class BNKBKHD extends BNKHeader
{
    public BNKBKHD(BNKHeader header)
    {
        setDataStart(header.getDataStart());
        setSection(header.getSection());
        setLength(header.getLength());
    }
    
    private int bankVersion;
    private int bankId;
    private int languageId;
    private int feedback;
}

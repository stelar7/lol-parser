package no.stelar7.cdragon.types.bnk.data;

import lombok.Data;

@Data
public class BNKFile
{
    private BNKBKHD bankHeader;
    private BNKDIDX dataIndex;
    private BNKDATA data;
//    private BNKENVS environments;
//    private BNKFXPR effectsProduction;
//    private BNKHIRC objectHierarchy;
//    private BNKSTID bankIds;
//    private BNKSTMG settings;
}

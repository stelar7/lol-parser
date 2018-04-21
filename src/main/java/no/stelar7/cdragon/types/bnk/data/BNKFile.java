package no.stelar7.cdragon.types.bnk.data;

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
    
    
    public BNKBKHD getBankHeader()
    {
        return bankHeader;
    }
    
    public void setBankHeader(BNKBKHD bankHeader)
    {
        this.bankHeader = bankHeader;
    }
    
    public BNKDIDX getDataIndex()
    {
        return dataIndex;
    }
    
    public void setDataIndex(BNKDIDX dataIndex)
    {
        this.dataIndex = dataIndex;
    }
    
    public BNKDATA getData()
    {
        return data;
    }
    
    public void setData(BNKDATA data)
    {
        this.data = data;
    }
}

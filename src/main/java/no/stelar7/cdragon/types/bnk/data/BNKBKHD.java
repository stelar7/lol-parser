package no.stelar7.cdragon.types.bnk.data;

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
    
    public int getBankVersion()
    {
        return bankVersion;
    }
    
    public void setBankVersion(int bankVersion)
    {
        this.bankVersion = bankVersion;
    }
    
    public int getBankId()
    {
        return bankId;
    }
    
    public void setBankId(int bankId)
    {
        this.bankId = bankId;
    }
    
    public int getLanguageId()
    {
        return languageId;
    }
    
    public void setLanguageId(int languageId)
    {
        this.languageId = languageId;
    }
    
    public int getFeedback()
    {
        return feedback;
    }
    
    public void setFeedback(int feedback)
    {
        this.feedback = feedback;
    }
}

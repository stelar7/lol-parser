package no.stelar7.cdragon.types.bnk.data;

public final class BNKBKHD extends BNKHeader
{
    /**
     * @param languageId 0 if the section is too small to have it
     * @param feedback   0 if the section is too small to have it
     */
    public BNKBKHD(BNKHeader header, int bankVersion, int bankId, int languageId, int feedback)
    {
        super(header);
        this.bankVersion = bankVersion;
        this.bankId = bankId;
        this.languageId = languageId;
        this.feedback = feedback;
    }
    
    private final int bankVersion;
    private final int bankId;
    private final int languageId;
    private final int feedback;
    
    public int getBankVersion()
    {
        return bankVersion;
    }
    
    public int getBankId()
    {
        return bankId;
    }
    
    public int getLanguageId()
    {
        return languageId;
    }
    
    public int getFeedback()
    {
        return feedback;
    }
}

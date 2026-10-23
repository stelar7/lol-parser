package no.stelar7.cdragon.types.bnk.data;

import java.util.*;

/**
 * String mapping chunk; maps bank ids to bank names
 */
public final class BNKSTID extends BNKHeader
{
    public BNKSTID(BNKHeader header, int stringType, Map<Integer, String> bankNames)
    {
        super(header);
        this.stringType = stringType;
        this.bankNames = bankNames;
    }

    private final int                  stringType;
    private final Map<Integer, String> bankNames;

    public int getStringType()
    {
        return stringType;
    }

    public Map<Integer, String> getBankNames()
    {
        return bankNames;
    }
}

package no.stelar7.cdragon.types.bnk.data;

import java.util.*;

public class BNKFile
{
    private BNKBKHD bankHeader;
    private BNKDIDX dataIndex;
    private BNKDATA data;
    private BNKHIRC objectHierarchy;
    private BNKSTID bankIds;

    // ENVS, FXPR, STMG, INIT, PLAT and unknown sections
    private List<BNKRawSection> otherSections = new ArrayList<>();

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

    public BNKHIRC getObjectHierarchy()
    {
        return objectHierarchy;
    }

    public void setObjectHierarchy(BNKHIRC objectHierarchy)
    {
        this.objectHierarchy = objectHierarchy;
    }

    public BNKSTID getBankIds()
    {
        return bankIds;
    }

    public void setBankIds(BNKSTID bankIds)
    {
        this.bankIds = bankIds;
    }

    public List<BNKRawSection> getOtherSections()
    {
        return otherSections;
    }

    public void setOtherSections(List<BNKRawSection> otherSections)
    {
        this.otherSections = otherSections;
    }
}

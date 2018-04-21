package no.stelar7.cdragon.types.bnk.data;


import java.util.*;

public class BNKDATA extends BNKHeader
{
    public BNKDATA(BNKHeader header)
    {
        setDataStart(header.getDataStart());
        setSection(header.getSection());
        setLength(header.getLength());
    }
    
    private List<BNKDATAWEMFile> wemFiles = new ArrayList<>();
    
    public List<BNKDATAWEMFile> getWemFiles()
    {
        return wemFiles;
    }
    
    public void setWemFiles(List<BNKDATAWEMFile> wemFiles)
    {
        this.wemFiles = wemFiles;
    }
}

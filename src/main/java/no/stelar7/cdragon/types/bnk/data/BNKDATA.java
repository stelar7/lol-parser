package no.stelar7.cdragon.types.bnk.data;


import java.util.*;

public final class BNKDATA extends BNKHeader
{
    public BNKDATA(BNKHeader header, List<BNKDATAWEMFile> wemFiles)
    {
        super(header);
        this.wemFiles = wemFiles;
    }
    
    private final List<BNKDATAWEMFile> wemFiles;
    
    public List<BNKDATAWEMFile> getWemFiles()
    {
        return wemFiles;
    }
}

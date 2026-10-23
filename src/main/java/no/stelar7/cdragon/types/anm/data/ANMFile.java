package no.stelar7.cdragon.types.anm.data;

import no.stelar7.cdragon.types.anm.data.versioned.*;

public final class ANMFile
{
    private final ANMHeader       header;
    private final ANMDataVersion1 version1;
    private final ANMDataVersion3 version3;
    private final ANMDataVersion4 version4;
    private final ANMDataVersion5 version5;
    
    public ANMFile(ANMHeader header, ANMDataVersion1 version1, ANMDataVersion3 version3, ANMDataVersion4 version4, ANMDataVersion5 version5)
    {
        this.header = header;
        this.version1 = version1;
        this.version3 = version3;
        this.version4 = version4;
        this.version5 = version5;
    }
    
    public ANMHeader getHeader()
    {
        return header;
    }
    
    public ANMDataVersion1 getVersion1()
    {
        return version1;
    }
    
    public ANMDataVersion3 getVersion3()
    {
        return version3;
    }
    
    public ANMDataVersion4 getVersion4()
    {
        return version4;
    }
    
    public ANMDataVersion5 getVersion5()
    {
        return version5;
    }
}

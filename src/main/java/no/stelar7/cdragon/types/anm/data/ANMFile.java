package no.stelar7.cdragon.types.anm.data;

import no.stelar7.cdragon.types.anm.data.versioned.*;

public class ANMFile
{
    private ANMHeader       header;
    private ANMDataVersion1 version1;
    private ANMDataVersion3 version3;
    private ANMDataVersion4 version4;
    private ANMDataVersion5 version5;
    
    public ANMHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(ANMHeader header)
    {
        this.header = header;
    }
    
    public ANMDataVersion1 getVersion1()
    {
        return version1;
    }
    
    public void setVersion1(ANMDataVersion1 version1)
    {
        this.version1 = version1;
    }
    
    public ANMDataVersion3 getVersion3()
    {
        return version3;
    }
    
    public void setVersion3(ANMDataVersion3 version3)
    {
        this.version3 = version3;
    }
    
    public ANMDataVersion4 getVersion4()
    {
        return version4;
    }
    
    public void setVersion4(ANMDataVersion4 version4)
    {
        this.version4 = version4;
    }
    
    public ANMDataVersion5 getVersion5()
    {
        return version5;
    }
    
    public void setVersion5(ANMDataVersion5 version5)
    {
        this.version5 = version5;
    }
}

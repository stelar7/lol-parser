package no.stelar7.cdragon.types.skl.data;

import no.stelar7.cdragon.types.skl.data.versioned.data.*;

public class SKLFile
{
    private SKLHeader header;
    private SKLDataV0 dataV0;
    private SKLDataV1 dataV1;
    private SKLDataV2 dataV2;
    
    public SKLDataV0 getDataV0()
    {
        return dataV0;
    }
    
    public void setDataV0(SKLDataV0 dataV0)
    {
        this.dataV0 = dataV0;
    }
    
    public SKLDataV1 getDataV1()
    {
        return dataV1;
    }
    
    public void setDataV1(SKLDataV1 dataV1)
    {
        this.dataV1 = dataV1;
    }
    
    public SKLDataV2 getDataV2()
    {
        return dataV2;
    }
    
    public void setDataV2(SKLDataV2 dataV2)
    {
        this.dataV2 = dataV2;
    }
    
    public SKLHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(SKLHeader header)
    {
        this.header = header;
    }
}

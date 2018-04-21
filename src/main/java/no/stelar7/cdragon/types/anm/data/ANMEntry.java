package no.stelar7.cdragon.types.anm.data;

import no.stelar7.cdragon.util.types.Vector3s;

public class ANMEntry
{
    private short    compressedTime;
    private byte     hashId;
    private byte     dataType;
    private Vector3s compressedData;
    
    public short getCompressedTime()
    {
        return compressedTime;
    }
    
    public void setCompressedTime(short compressedTime)
    {
        this.compressedTime = compressedTime;
    }
    
    public byte getHashId()
    {
        return hashId;
    }
    
    public void setHashId(byte hashId)
    {
        this.hashId = hashId;
    }
    
    public byte getDataType()
    {
        return dataType;
    }
    
    public void setDataType(byte dataType)
    {
        this.dataType = dataType;
    }
    
    public Vector3s getCompressedData()
    {
        return compressedData;
    }
    
    public void setCompressedData(Vector3s compressedData)
    {
        this.compressedData = compressedData;
    }
}

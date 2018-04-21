package no.stelar7.cdragon.types.bnk.data;

public class BNKDATAWEMFile
{
    private int    fileId;
    private byte[] data;
    
    public int getFileId()
    {
        return fileId;
    }
    
    public void setFileId(int fileId)
    {
        this.fileId = fileId;
    }
    
    public byte[] getData()
    {
        return data;
    }
    
    public void setData(byte[] data)
    {
        this.data = data;
    }
}

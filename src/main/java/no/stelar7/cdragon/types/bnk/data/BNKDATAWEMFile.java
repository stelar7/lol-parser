package no.stelar7.cdragon.types.bnk.data;

public final class BNKDATAWEMFile
{
    private final int    fileId;
    private final byte[] data;
    
    public BNKDATAWEMFile(int fileId, byte[] data)
    {
        this.fileId = fileId;
        this.data = data;
    }
    
    public int getFileId()
    {
        return fileId;
    }
    
    public byte[] getData()
    {
        return data;
    }
}

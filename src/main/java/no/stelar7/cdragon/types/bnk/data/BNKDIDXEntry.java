package no.stelar7.cdragon.types.bnk.data;

public class BNKDIDXEntry
{
    private int fileId;
    private int fileOffset;
    private int fileSize;
    
    public int getFileId()
    {
        return fileId;
    }
    
    public void setFileId(int fileId)
    {
        this.fileId = fileId;
    }
    
    public int getFileOffset()
    {
        return fileOffset;
    }
    
    public void setFileOffset(int fileOffset)
    {
        this.fileOffset = fileOffset;
    }
    
    public int getFileSize()
    {
        return fileSize;
    }
    
    public void setFileSize(int fileSize)
    {
        this.fileSize = fileSize;
    }
}

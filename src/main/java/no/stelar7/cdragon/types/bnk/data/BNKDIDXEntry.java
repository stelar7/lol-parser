package no.stelar7.cdragon.types.bnk.data;

public final class BNKDIDXEntry
{
    private final int fileId;
    private final int fileOffset;
    private final int fileSize;
    
    public BNKDIDXEntry(int fileId, int fileOffset, int fileSize)
    {
        this.fileId = fileId;
        this.fileOffset = fileOffset;
        this.fileSize = fileSize;
    }
    
    public int getFileId()
    {
        return fileId;
    }
    
    public int getFileOffset()
    {
        return fileOffset;
    }
    
    public int getFileSize()
    {
        return fileSize;
    }
}

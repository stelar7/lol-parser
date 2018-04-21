package no.stelar7.cdragon.types.rofl.data;

public class ROFLHeader
{
    private String       magic;
    private byte[]       signature;
    private short        headerSize;
    private int          fileSize;
    private int          metadataOffset;
    private int          metadataLength;
    private int          payloadHeaderOffset;
    private int          payloadHeaderLength;
    private int          payloadOffset;
    private ROFLMetadata metadata;
    
    public String getMagic()
    {
        return magic;
    }
    
    public void setMagic(String magic)
    {
        this.magic = magic;
    }
    
    public byte[] getSignature()
    {
        return signature;
    }
    
    public void setSignature(byte[] signature)
    {
        this.signature = signature;
    }
    
    public short getHeaderSize()
    {
        return headerSize;
    }
    
    public void setHeaderSize(short headerSize)
    {
        this.headerSize = headerSize;
    }
    
    public int getFileSize()
    {
        return fileSize;
    }
    
    public void setFileSize(int fileSize)
    {
        this.fileSize = fileSize;
    }
    
    public int getMetadataOffset()
    {
        return metadataOffset;
    }
    
    public void setMetadataOffset(int metadataOffset)
    {
        this.metadataOffset = metadataOffset;
    }
    
    public int getMetadataLength()
    {
        return metadataLength;
    }
    
    public void setMetadataLength(int metadataLength)
    {
        this.metadataLength = metadataLength;
    }
    
    public int getPayloadHeaderOffset()
    {
        return payloadHeaderOffset;
    }
    
    public void setPayloadHeaderOffset(int payloadHeaderOffset)
    {
        this.payloadHeaderOffset = payloadHeaderOffset;
    }
    
    public int getPayloadHeaderLength()
    {
        return payloadHeaderLength;
    }
    
    public void setPayloadHeaderLength(int payloadHeaderLength)
    {
        this.payloadHeaderLength = payloadHeaderLength;
    }
    
    public int getPayloadOffset()
    {
        return payloadOffset;
    }
    
    public void setPayloadOffset(int payloadOffset)
    {
        this.payloadOffset = payloadOffset;
    }
    
    public ROFLMetadata getMetadata()
    {
        return metadata;
    }
    
    public void setMetadata(ROFLMetadata metadata)
    {
        this.metadata = metadata;
    }
}

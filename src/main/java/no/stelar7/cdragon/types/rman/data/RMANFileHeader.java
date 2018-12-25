package no.stelar7.cdragon.types.rman.data;

import java.util.Objects;

public class RMANFileHeader
{
    private String magic;
    private byte   major;
    private byte   minor;
    private byte   unknown;
    private byte   signatureType;
    private int    offset;
    private int    length;
    private long   manifestId;
    private int    decompressedLength;
    
    public String getMagic()
    {
        return magic;
    }
    
    public void setMagic(String magic)
    {
        this.magic = magic;
    }
    
    public byte getMajor()
    {
        return major;
    }
    
    public void setMajor(byte major)
    {
        this.major = major;
    }
    
    public byte getMinor()
    {
        return minor;
    }
    
    public void setMinor(byte minor)
    {
        this.minor = minor;
    }
    
    public byte getUnknown()
    {
        return unknown;
    }
    
    public void setUnknown(byte unknown)
    {
        this.unknown = unknown;
    }
    
    public byte getSignatureType()
    {
        return signatureType;
    }
    
    public void setSignatureType(byte signatureType)
    {
        this.signatureType = signatureType;
    }
    
    public long getManifestId()
    {
        return manifestId;
    }
    
    public void setManifestId(long manifestId)
    {
        this.manifestId = manifestId;
    }
    
    public int getDecompressedLength()
    {
        return decompressedLength;
    }
    
    public void setDecompressedLength(int decompressedLength)
    {
        this.decompressedLength = decompressedLength;
    }
    
    public int getOffset()
    {
        return offset;
    }
    
    public void setOffset(int offset)
    {
        this.offset = offset;
    }
    
    public int getLength()
    {
        return length;
    }
    
    public void setLength(int length)
    {
        this.length = length;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        RMANFileHeader that = (RMANFileHeader) o;
        return major == that.major &&
               minor == that.minor &&
               unknown == that.unknown &&
               signatureType == that.signatureType &&
               offset == that.offset &&
               length == that.length &&
               manifestId == that.manifestId &&
               decompressedLength == that.decompressedLength &&
               Objects.equals(magic, that.magic);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(magic, major, minor, unknown, signatureType, offset, length, manifestId, decompressedLength);
    }
    
    @Override
    public String toString()
    {
        return "RMANFileHeader{" +
               "magic='" + magic + '\'' +
               ", major=" + major +
               ", minor=" + minor +
               ", unknown=" + unknown +
               ", signatureType=" + signatureType +
               ", offset=" + offset +
               ", length=" + length +
               ", manifestId=" + manifestId +
               ", decompressedLength=" + decompressedLength +
               '}';
    }
}

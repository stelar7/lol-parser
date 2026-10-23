package no.stelar7.cdragon.types.rman.data;

import java.util.Objects;

public final class RMANFileHeader
{
    private final String magic;
    private final byte   major;
    private final byte   minor;
    private final byte   unknown;
    private final byte   signatureType;
    private final int    offset;
    private final int    length;
    private final long   manifestId;
    private final int    decompressedLength;
    
    public RMANFileHeader(String magic, byte major, byte minor, byte unknown, byte signatureType, int offset, int length, long manifestId, int decompressedLength)
    {
        this.magic = magic;
        this.major = major;
        this.minor = minor;
        this.unknown = unknown;
        this.signatureType = signatureType;
        this.offset = offset;
        this.length = length;
        this.manifestId = manifestId;
        this.decompressedLength = decompressedLength;
    }
    
    public String getMagic()
    {
        return magic;
    }
    
    public byte getMajor()
    {
        return major;
    }
    
    public byte getMinor()
    {
        return minor;
    }
    
    public byte getUnknown()
    {
        return unknown;
    }
    
    public byte getSignatureType()
    {
        return signatureType;
    }
    
    public long getManifestId()
    {
        return manifestId;
    }
    
    public int getDecompressedLength()
    {
        return decompressedLength;
    }
    
    public int getOffset()
    {
        return offset;
    }
    
    public int getLength()
    {
        return length;
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

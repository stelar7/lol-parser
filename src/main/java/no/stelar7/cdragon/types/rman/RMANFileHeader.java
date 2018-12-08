package no.stelar7.cdragon.types.rman;

import java.util.Objects;

public class RMANFileHeader
{
    private String magic;
    private byte   major;
    private byte   minor;
    private byte   unknown;
    private byte   signed;
    private int    offset;
    private int    length;
    
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
    
    public byte getSigned()
    {
        return signed;
    }
    
    public void setSigned(byte signed)
    {
        this.signed = signed;
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
               signed == that.signed &&
               offset == that.offset &&
               length == that.length &&
               Objects.equals(magic, that.magic);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(magic, major, minor, unknown, signed, offset, length);
    }
    
    @Override
    public String toString()
    {
        return String.format("RMANFileHeader{magic='%s', major=%s, minor=%s, unknown=%s, signed=%s, offset=%d, length=%d}", magic, major, minor, unknown, signed, offset, length);
    }
}

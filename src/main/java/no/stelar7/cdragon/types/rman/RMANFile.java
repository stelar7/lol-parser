package no.stelar7.cdragon.types.rman;

import java.util.*;

public class RMANFile
{
    private RMANFileHeader header;
    private byte[]         content;
    private byte[]         remainder;
    
    public RMANFileHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(RMANFileHeader header)
    {
        this.header = header;
    }
    
    public byte[] getContent()
    {
        return content;
    }
    
    public void setContent(byte[] zstd)
    {
        this.content = zstd;
    }
    
    public byte[] getRemainder()
    {
        return remainder;
    }
    
    public void setRemainder(byte[] remainder)
    {
        this.remainder = remainder;
    }
    
    @Override
    public String toString()
    {
        return "RMANFile{" + "header=" + header + '}';
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
        RMANFile rmanFile = (RMANFile) o;
        return Objects.equals(header, rmanFile.header) &&
               Arrays.equals(content, rmanFile.content) &&
               Arrays.equals(remainder, rmanFile.remainder);
    }
    
    @Override
    public int hashCode()
    {
        int result = Objects.hash(header);
        result = 31 * result + Arrays.hashCode(content);
        result = 31 * result + Arrays.hashCode(remainder);
        return result;
    }
}

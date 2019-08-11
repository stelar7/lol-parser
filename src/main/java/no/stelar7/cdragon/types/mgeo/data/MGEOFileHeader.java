package no.stelar7.cdragon.types.mgeo.data;

import java.util.Objects;

public class MGEOFileHeader
{
    private String magic;
    private int    version;
    
    public String getMagic()
    {
        return magic;
    }
    
    public void setMagic(String magic)
    {
        this.magic = magic;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public void setVersion(int version)
    {
        this.version = version;
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
        MGEOFileHeader that = (MGEOFileHeader) o;
        return version == that.version &&
               Objects.equals(magic, that.magic);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(magic, version);
    }
    
    @Override
    public String toString()
    {
        return "MGEOFileHeader{" +
               "magic='" + magic + '\'' +
               ", version=" + version +
               '}';
    }
}

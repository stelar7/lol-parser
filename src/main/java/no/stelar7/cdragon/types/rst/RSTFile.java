package no.stelar7.cdragon.types.rst;

import no.stelar7.cdragon.util.handlers.HashHandler;

import java.util.*;

public class RSTFile
{
    private String            magic;
    private int               major;
    private int               minor;
    private String            config;
    private Map<Long, String> entries = new HashMap<>();
    
    public String getMagic()
    {
        return magic;
    }
    
    public void setMagic(String magic)
    {
        this.magic = magic;
    }
    
    public int getMajor()
    {
        return major;
    }
    
    public void setMajor(int major)
    {
        this.major = major;
    }
    
    public int getMinor()
    {
        return minor;
    }
    
    public void setMinor(int minor)
    {
        this.minor = minor;
    }
    
    public String getConfig()
    {
        return config;
    }
    
    public void setConfig(String config)
    {
        this.config = config;
    }
    
    public Map<Long, String> getEntries()
    {
        return entries;
    }
    
    public String getFromHash(String hash)
    {
        return entries.getOrDefault(HashHandler.computeXXHash64AsLong(hash) & 0xFFFFFFFFFFL, hash);
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
        RSTFile rstFile = (RSTFile) o;
        return major == rstFile.major &&
               minor == rstFile.minor &&
               Objects.equals(magic, rstFile.magic) &&
               Objects.equals(config, rstFile.config) &&
               Objects.equals(entries, rstFile.entries);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(magic, major, minor, config, entries);
    }
    
    @Override
    public String toString()
    {
        return "RSTFile{" +
               "magic='" + magic + '\'' +
               ", major=" + major +
               ", minor=" + minor +
               ", config='" + config + '\'' +
               ", entries=" + entries +
               '}';
    }
}

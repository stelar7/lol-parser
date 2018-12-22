package no.stelar7.cdragon.types.rbun;

import java.util.*;

public class RBUNFile
{
    private String              magic;
    private int                 version;
    private int                 chunkCount;
    private String              bundleId;
    private List<RBUNChunkInfo> chunks;
    
    
    public int getMetadataSize()
    {
        return 4 + 4 + 4 + 8 + (chunkCount * (8 + 4 + 4));
    }
    
    public String getMagic()
    {
        return magic;
    }
    
    public void setMagic(String magic)
    {
        this.magic = magic;
    }
    
    public List<RBUNChunkInfo> getChunks()
    {
        return chunks;
    }
    
    public void setChunks(List<RBUNChunkInfo> chunks)
    {
        this.chunks = chunks;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public void setVersion(int version)
    {
        this.version = version;
    }
    
    public int getChunkCount()
    {
        return chunkCount;
    }
    
    public void setChunkCount(int chunkCount)
    {
        this.chunkCount = chunkCount;
    }
    
    public String getBundleId()
    {
        return bundleId;
    }
    
    public void setBundleId(String bundleId)
    {
        this.bundleId = bundleId;
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
        RBUNFile rbunFile = (RBUNFile) o;
        return version == rbunFile.version &&
               chunkCount == rbunFile.chunkCount &&
               Objects.equals(magic, rbunFile.magic) &&
               Objects.equals(bundleId, rbunFile.bundleId) &&
               Objects.equals(chunks, rbunFile.chunks);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(magic, version, chunkCount, bundleId, chunks);
    }
    
    @Override
    public String toString()
    {
        return "RBUNFile{" +
               "magic='" + magic + '\'' +
               ", version=" + version +
               ", chunkCount=" + chunkCount +
               ", bundleId='" + bundleId + '\'' +
               ", chunks=" + chunks +
               '}';
    }
}

package no.stelar7.cdragon.types.bbq;

import java.util.Objects;

public class BBQHeader
{
    
    private String signature;
    private int    version;
    private String playerVersion;
    private String fsVersion;
    private long   totalFileSize;
    private int    metadataCompressedSize;
    private int    metadataUncompressedSize;
    private int    flags;
    private int    headerSize;
    
    // 0 = none, 1 = LZMA, 2/3 = LZ4/LZ4HC
    private int     compressionMode;
    private boolean hasEntryInfo;
    private boolean isMetadataAtEnd;
    
    public String getSignature()
    {
        return signature;
    }
    
    public void setSignature(String signature)
    {
        this.signature = signature;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public void setVersion(int version)
    {
        this.version = version;
    }
    
    public String getPlayerVersion()
    {
        return playerVersion;
    }
    
    public void setPlayerVersion(String playerVersion)
    {
        this.playerVersion = playerVersion;
    }
    
    public String getFsVersion()
    {
        return fsVersion;
    }
    
    public void setFsVersion(String fsVersion)
    {
        this.fsVersion = fsVersion;
    }
    
    public long getTotalFileSize()
    {
        return totalFileSize;
    }
    
    public void setTotalFileSize(long totalFileSize)
    {
        this.totalFileSize = totalFileSize;
    }
    
    public int getMetadataCompressedSize()
    {
        return metadataCompressedSize;
    }
    
    public void setMetadataCompressedSize(int metadataCompressedSize)
    {
        this.metadataCompressedSize = metadataCompressedSize;
    }
    
    public int getMetadataUncompressedSize()
    {
        return metadataUncompressedSize;
    }
    
    public void setMetadataUncompressedSize(int metadataUncompressedSize)
    {
        this.metadataUncompressedSize = metadataUncompressedSize;
    }
    
    public int getFlags()
    {
        return flags;
    }
    
    public void setFlags(int flags)
    {
        this.flags = flags;
    }
    
    public int getCompressionMode()
    {
        return compressionMode;
    }
    
    public void setCompressionMode(int compressionMode)
    {
        this.compressionMode = compressionMode;
    }
    
    public boolean isHasEntryInfo()
    {
        return hasEntryInfo;
    }
    
    public void setHasEntryInfo(boolean hasEntryInfo)
    {
        this.hasEntryInfo = hasEntryInfo;
    }
    
    public boolean isMetadataAtEnd()
    {
        return isMetadataAtEnd;
    }
    
    public void setMetadataAtEnd(boolean metadataAtEnd)
    {
        isMetadataAtEnd = metadataAtEnd;
    }
    
    public int getHeaderSize()
    {
        return headerSize;
    }
    
    public void setHeaderSize(int headerSize)
    {
        this.headerSize = headerSize;
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
        BBQHeader bbqHeader = (BBQHeader) o;
        return signature.equals(bbqHeader.signature) &&
               version == bbqHeader.version &&
               totalFileSize == bbqHeader.totalFileSize &&
               metadataCompressedSize == bbqHeader.metadataCompressedSize &&
               metadataUncompressedSize == bbqHeader.metadataUncompressedSize &&
               flags == bbqHeader.flags &&
               compressionMode == bbqHeader.compressionMode &&
               hasEntryInfo == bbqHeader.hasEntryInfo &&
               isMetadataAtEnd == bbqHeader.isMetadataAtEnd &&
               Objects.equals(playerVersion, bbqHeader.playerVersion) &&
               Objects.equals(fsVersion, bbqHeader.fsVersion);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(signature, version, playerVersion, fsVersion, totalFileSize, metadataCompressedSize, metadataUncompressedSize, flags, compressionMode, hasEntryInfo, isMetadataAtEnd);
    }
    
    @Override
    public String toString()
    {
        return "BBQHeader{" +
               "signature=" + signature +
               ", version=" + version +
               ", playerVersion='" + playerVersion + '\'' +
               ", fsVersion='" + fsVersion + '\'' +
               ", totalFileSize=" + totalFileSize +
               ", metadataCompressedSize=" + metadataCompressedSize +
               ", metadataUncompressedSize=" + metadataUncompressedSize +
               ", flags=" + flags +
               ", compressionMode=" + compressionMode +
               ", hasDirectoryInfo=" + hasEntryInfo +
               ", isDirectoryAtEnd=" + isMetadataAtEnd +
               '}';
    }
}

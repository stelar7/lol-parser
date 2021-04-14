package no.stelar7.cdragon.types.bbq;

import java.util.Objects;

public class BBQHeader
{
    
    private String signature;
    private int    formatVersion;
    // https://github.com/mafaca/UtinyRipper/blob/8ca850c8fe3ac25659008aa6e80e0694675280d0/uTinyRipperCore/Parser/Files/BundleFile/BundleGeneration.cs
    private String unityVersion;
    private String generatorVersion;
    private long   totalFileSize;
    private int    metadataCompressedSize;
    private int    metadataUncompressedSize;
    private int    flags;
    private int    headerSize;
    
    private BBQCompressionType compressionMode;
    private boolean            blockAndDirCombined;
    private boolean            isMetadataAtEnd;
    
    public String getSignature()
    {
        return signature;
    }
    
    public void setSignature(String signature)
    {
        this.signature = signature;
    }
    
    public int getFormatVersion()
    {
        return formatVersion;
    }
    
    public void setFormatVersion(int formatVersion)
    {
        this.formatVersion = formatVersion;
    }
    
    public String getUnityVersion()
    {
        return unityVersion;
    }
    
    public void setUnityVersion(String unityVersion)
    {
        this.unityVersion = unityVersion;
    }
    
    public String getGeneratorVersion()
    {
        return generatorVersion;
    }
    
    public void setGeneratorVersion(String generatorVersion)
    {
        this.generatorVersion = generatorVersion;
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
    
    public BBQCompressionType getCompressionMode()
    {
        return compressionMode;
    }
    
    public void setCompressionMode(BBQCompressionType compressionMode)
    {
        this.compressionMode = compressionMode;
    }
    
    public boolean isBlockAndDirCombined()
    {
        return blockAndDirCombined;
    }
    
    public void setBlockAndDirCombined(boolean blockAndDirCombined)
    {
        this.blockAndDirCombined = blockAndDirCombined;
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
    
    public boolean isUnityFS()
    {
        return this.signature.equals("UnityFS");
    }
    
    public boolean isRAW()
    {
        return this.signature.equals("UnityRaw");
    }
    
    public boolean isWEB()
    {
        return this.signature.equals("UnityWeb");
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
               formatVersion == bbqHeader.formatVersion &&
               totalFileSize == bbqHeader.totalFileSize &&
               metadataCompressedSize == bbqHeader.metadataCompressedSize &&
               metadataUncompressedSize == bbqHeader.metadataUncompressedSize &&
               flags == bbqHeader.flags &&
               compressionMode == bbqHeader.compressionMode &&
               blockAndDirCombined == bbqHeader.blockAndDirCombined &&
               isMetadataAtEnd == bbqHeader.isMetadataAtEnd &&
               Objects.equals(unityVersion, bbqHeader.unityVersion) &&
               Objects.equals(generatorVersion, bbqHeader.generatorVersion);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(signature, formatVersion, unityVersion, generatorVersion, totalFileSize, metadataCompressedSize, metadataUncompressedSize, flags, compressionMode, blockAndDirCombined, isMetadataAtEnd);
    }
    
    @Override
    public String toString()
    {
        return "BBQHeader{" +
               "signature='" + signature + '\'' +
               ", formatVersion=" + formatVersion +
               ", unityVersion='" + unityVersion + '\'' +
               ", generatorVersion='" + generatorVersion + '\'' +
               ", totalFileSize=" + totalFileSize +
               ", metadataCompressedSize=" + metadataCompressedSize +
               ", metadataUncompressedSize=" + metadataUncompressedSize +
               ", flags=" + flags +
               ", headerSize=" + headerSize +
               ", compressionMode=" + compressionMode +
               ", hasEntryInfo=" + blockAndDirCombined +
               ", isMetadataAtEnd=" + isMetadataAtEnd +
               '}';
    }
}

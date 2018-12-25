package no.stelar7.cdragon.types.rman.data;

import java.util.Objects;

public class RMANFileBodyHeader
{
    private int offsetTableOffset;
    private int bundleListOffset;
    private int languageListOffset;
    private int fileListOffset;
    private int folderListOffset;
    private int keyHeaderOffset;
    private int unknownOffset;
    
    public int getOffsetTableOffset()
    {
        return offsetTableOffset;
    }
    
    public void setOffsetTableOffset(int offsetTableOffset)
    {
        this.offsetTableOffset = offsetTableOffset;
    }
    
    public int getBundleListOffset()
    {
        return bundleListOffset;
    }
    
    public void setBundleListOffset(int bundleListOffset)
    {
        this.bundleListOffset = bundleListOffset;
    }
    
    public int getLanguageListOffset()
    {
        return languageListOffset;
    }
    
    public void setLanguageListOffset(int languageListOffset)
    {
        this.languageListOffset = languageListOffset;
    }
    
    public int getFileListOffset()
    {
        return fileListOffset;
    }
    
    public void setFileListOffset(int fileListOffset)
    {
        this.fileListOffset = fileListOffset;
    }
    
    public int getFolderListOffset()
    {
        return folderListOffset;
    }
    
    public void setFolderListOffset(int folderListOffset)
    {
        this.folderListOffset = folderListOffset;
    }
    
    public int getKeyHeaderOffset()
    {
        return keyHeaderOffset;
    }
    
    public void setKeyHeaderOffset(int keyHeaderOffset)
    {
        this.keyHeaderOffset = keyHeaderOffset;
    }
    
    public int getUnknownOffset()
    {
        return unknownOffset;
    }
    
    public void setUnknownOffset(int unknownOffset)
    {
        this.unknownOffset = unknownOffset;
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
        RMANFileBodyHeader that = (RMANFileBodyHeader) o;
        return offsetTableOffset == that.offsetTableOffset &&
               bundleListOffset == that.bundleListOffset &&
               languageListOffset == that.languageListOffset &&
               fileListOffset == that.fileListOffset &&
               folderListOffset == that.folderListOffset &&
               keyHeaderOffset == that.keyHeaderOffset &&
               unknownOffset == that.unknownOffset;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(offsetTableOffset, bundleListOffset, languageListOffset, fileListOffset, folderListOffset, keyHeaderOffset, unknownOffset);
    }
    
    @Override
    public String toString()
    {
        return "RMANFileBodyHeader{" +
               "offsetTableOffset=" + offsetTableOffset +
               ", bundleListOffset=" + bundleListOffset +
               ", languageListOffset=" + languageListOffset +
               ", fileListOffset=" + fileListOffset +
               ", folderListOffset=" + folderListOffset +
               ", keyHeaderOffset=" + keyHeaderOffset +
               ", unknownOffset=" + unknownOffset +
               '}';
    }
}

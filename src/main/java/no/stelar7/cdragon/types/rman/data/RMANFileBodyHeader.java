package no.stelar7.cdragon.types.rman.data;

import java.util.Objects;

public final class RMANFileBodyHeader
{
    private final int offsetTableOffset;
    private final int bundleListOffset;
    private final int languageListOffset;
    private final int fileListOffset;
    private final int folderListOffset;
    private final int keyHeaderOffset;
    private final int unknownOffset;
    
    public RMANFileBodyHeader(int offsetTableOffset, int bundleListOffset, int languageListOffset, int fileListOffset, int folderListOffset, int keyHeaderOffset, int unknownOffset)
    {
        this.offsetTableOffset = offsetTableOffset;
        this.bundleListOffset = bundleListOffset;
        this.languageListOffset = languageListOffset;
        this.fileListOffset = fileListOffset;
        this.folderListOffset = folderListOffset;
        this.keyHeaderOffset = keyHeaderOffset;
        this.unknownOffset = unknownOffset;
    }
    
    public int getOffsetTableOffset()
    {
        return offsetTableOffset;
    }
    
    public int getBundleListOffset()
    {
        return bundleListOffset;
    }
    
    public int getLanguageListOffset()
    {
        return languageListOffset;
    }
    
    public int getFileListOffset()
    {
        return fileListOffset;
    }
    
    public int getFolderListOffset()
    {
        return folderListOffset;
    }
    
    public int getKeyHeaderOffset()
    {
        return keyHeaderOffset;
    }
    
    public int getUnknownOffset()
    {
        return unknownOffset;
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

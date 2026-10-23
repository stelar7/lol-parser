package no.stelar7.cdragon.types.rman.data;

import java.util.*;

public final class RMANFileBodyFile
{
    private final int        filetypeFlag;
    private final String     name;
    private final int        structSize;
    private final String     symlink;
    private final long       fileId;
    private final long       directoryId;
    private final long       fileSize;
    private final int        permissions;
    private final int        languageId;
    private final int        unknown2;
    private final int        unknown3;
    private final long       languageFlags;
    private final List<Long> chunkIds;
    
    public RMANFileBodyFile(String name, String symlink, long fileId, long directoryId, long fileSize, int permissions, long languageFlags, List<Long> chunkIds)
    {
        // never read from the file
        this.filetypeFlag = 0;
        this.name = name;
        // never read from the file
        this.structSize = 0;
        this.symlink = symlink;
        this.fileId = fileId;
        this.directoryId = directoryId;
        this.fileSize = fileSize;
        this.permissions = permissions;
        // never read from the file
        this.languageId = 0;
        // never read from the file
        this.unknown2 = 0;
        // never read from the file
        this.unknown3 = 0;
        this.languageFlags = languageFlags;
        this.chunkIds = chunkIds;
    }
    
    public String getFullFilepath(RMANFile manifest)
    {
        StringBuilder output = new StringBuilder(getName());
        
        if (getDirectoryId() == 0)
        {
            return output.toString();
        }
        
        RMANFileBodyDirectory parentDir = manifest.getDirectory(getDirectoryId());
        if (parentDir != null)
        {
            String parent = parentDir.getFullPath(manifest);
            output.insert(0, parent + "/");
            
            return output.toString();
        }
        
        System.out.println("Invalid directory id found!");
        return "";
    }
    
    public int getFiletypeFlag()
    {
        return filetypeFlag;
    }
    
    public String getName()
    {
        return name;
    }
    
    public int getStructSize()
    {
        return structSize;
    }
    
    public String getSymlink()
    {
        return symlink;
    }
    
    public long getFileId()
    {
        return fileId;
    }
    
    public long getDirectoryId()
    {
        return directoryId;
    }
    
    public long getFileSize()
    {
        return fileSize;
    }
    
    public int getPermissions()
    {
        return permissions;
    }
    
    public int getLanguageId()
    {
        return languageId;
    }
    
    public int getUnknown2()
    {
        return unknown2;
    }
    
    public int getUnknown3()
    {
        return unknown3;
    }
    
    /**
     * Bitmask of the languages this file belongs to; bit i set means language id i + 1
     */
    public long getLanguageFlags()
    {
        return languageFlags;
    }
    
    public List<Long> getChunkIds()
    {
        return chunkIds;
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
        RMANFileBodyFile that = (RMANFileBodyFile) o;
        return filetypeFlag == that.filetypeFlag &&
               structSize == that.structSize &&
               fileId == that.fileId &&
               directoryId == that.directoryId &&
               fileSize == that.fileSize &&
               permissions == that.permissions &&
               languageId == that.languageId &&
               languageFlags == that.languageFlags &&
               unknown2 == that.unknown2 &&
               unknown3 == that.unknown3 &&
               Objects.equals(name, that.name) &&
               Objects.equals(symlink, that.symlink) &&
               Objects.equals(chunkIds, that.chunkIds);
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    /*
    @Override
    public int hashCode()
    {
        return Objects.hash(offset, offsetTableOffset, customNameOffset, filetypeFlag, nameOffset, name, structSize, symlinkOffset, symlink, fileId, directoryId, fileSize, permissions, languageId, unknown2, unknown3, isSingleChunk, chunkIds);
    }
    
    @Override
    public String toString()
    {
        return "RMANFileBodyFile{" +
               "offset=" + offset +
               ", offsetTableOffset=" + offsetTableOffset +
               ", unknown1=" + unknown1 +
               ", nameOffset=" + nameOffset +
               ", name='" + name + '\'' +
               ", structSize=" + structSize +
               ", symlinkOffset=" + symlinkOffset +
               ", symlink='" + symlink + '\'' +
               ", fileId=" + fileId +
               ", directoryId=" + directoryId +
               ", fileSize=" + fileSize +
               ", permissions=" + permissions +
               ", languageId=" + languageId +
               ", unknown2=" + unknown2 +
               ", unknown3=" + unknown3 +
               ", chunkIds=" + chunkIds +
               '}';
    }
    */
}

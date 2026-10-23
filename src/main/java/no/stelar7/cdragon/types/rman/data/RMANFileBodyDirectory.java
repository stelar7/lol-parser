package no.stelar7.cdragon.types.rman.data;

import java.util.*;

public final class RMANFileBodyDirectory
{
    private final String name;
    private final long   directoryId;
    private final long   parentId;
    
    private String fullPath;
    
    public RMANFileBodyDirectory(String name, long directoryId, long parentId)
    {
        this.name = name;
        this.directoryId = directoryId;
        this.parentId = parentId;
    }
    
    /**
     * Resolves the full path using the directory lookup map of the manifest
     */
    public String getFullPath(RMANFile manifest)
    {
        if (fullPath != null)
        {
            return fullPath;
        }
        
        StringBuilder         output  = new StringBuilder(getName());
        Set<Long>             visited = new HashSet<>();
        RMANFileBodyDirectory parent  = manifest.getDirectory(getParentId());
        while (parent != null && parent.directoryId != 0 && visited.add(parent.directoryId))
        {
            output.insert(0, parent.getName() + "/");
            parent = manifest.getDirectory(parent.getParentId());
        }
        
        fullPath = output.toString();
        return fullPath;
    }
    
    public String getFullPath(List<RMANFileBodyDirectory> folders)
    {
        if (fullPath != null)
        {
            return fullPath;
        }
        
        StringBuilder                   output      = new StringBuilder(getName());
        Optional<RMANFileBodyDirectory> maybeParent = folders.stream().filter(d -> d.getDirectoryId() == getParentId()).findAny();
        while (maybeParent.isPresent())
        {
            RMANFileBodyDirectory parent = maybeParent.get();
            if (parent.directoryId == 0)
            {
                break;
            }
            
            output.insert(0, parent.getName() + "/");
            maybeParent = folders.stream().filter(d -> d.getDirectoryId() == parent.getParentId()).findAny();
        }
        
        fullPath = output.toString();
        
        return getFullPath(folders);
    }
    
    public String getName()
    {
        return name;
    }
    
    public long getDirectoryId()
    {
        return directoryId;
    }
    
    public long getParentId()
    {
        return parentId;
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
        RMANFileBodyDirectory that = (RMANFileBodyDirectory) o;
        return directoryId == that.directoryId &&
               parentId == that.parentId &&
               Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(name, directoryId, parentId);
    }
    
    @Override
    public String toString()
    {
        return "RMANFileBodyDirectory{" +
               "name='" + name + '\'' +
               ", directoryId=" + directoryId +
               ", parentId=" + parentId +
               '}';
    }
}

package no.stelar7.cdragon.types.rman.data;

import java.util.*;

public class RMANFileBodyDirectory
{
    private String name;
    private long   directoryId;
    private long   parentId;
    
    private String fullPath;
    
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
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public long getDirectoryId()
    {
        return directoryId;
    }
    
    public void setDirectoryId(long directoryId)
    {
        this.directoryId = directoryId;
    }
    
    public long getParentId()
    {
        return parentId;
    }
    
    public void setParentId(long parentId)
    {
        this.parentId = parentId;
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

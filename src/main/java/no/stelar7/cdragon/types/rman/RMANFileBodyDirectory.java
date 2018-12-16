package no.stelar7.cdragon.types.rman;

import java.util.Objects;

public class RMANFileBodyDirectory
{
    
    private int    offset;
    private int    offsetTableOffset;
    private short  directoryIdOffset;
    private short  parentIdOffset;
    private int    nameOffset;
    private String name;
    private long   directoryId;
    private long   parentId;
    
    public int getOffset()
    {
        return offset;
    }
    
    public void setOffset(int offset)
    {
        this.offset = offset;
    }
    
    public int getOffsetTableOffset()
    {
        return offsetTableOffset;
    }
    
    public void setOffsetTableOffset(int offsetTableOffset)
    {
        this.offsetTableOffset = offsetTableOffset;
    }
    
    public short getDirectoryIdOffset()
    {
        return directoryIdOffset;
    }
    
    public void setDirectoryIdOffset(short directoryIdOffset)
    {
        this.directoryIdOffset = directoryIdOffset;
    }
    
    public short getParentIdOffset()
    {
        return parentIdOffset;
    }
    
    public void setParentIdOffset(short parentIdOffset)
    {
        this.parentIdOffset = parentIdOffset;
    }
    
    public int getNameOffset()
    {
        return nameOffset;
    }
    
    public void setNameOffset(int nameOffset)
    {
        this.nameOffset = nameOffset;
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
        return offset == that.offset &&
               offsetTableOffset == that.offsetTableOffset &&
               directoryIdOffset == that.directoryIdOffset &&
               parentIdOffset == that.parentIdOffset &&
               nameOffset == that.nameOffset &&
               directoryId == that.directoryId &&
               parentId == that.parentId &&
               Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(offset, offsetTableOffset, directoryIdOffset, parentIdOffset, nameOffset, name, directoryId, parentId);
    }
    
    @Override
    public String toString()
    {
        return "RMANFileBodyDirectory{" +
               "offset=" + offset +
               ", offsetTableOffset=" + offsetTableOffset +
               ", directoryIdOffset=" + directoryIdOffset +
               ", parentIdOffset=" + parentIdOffset +
               ", nameOffset=" + nameOffset +
               ", name='" + name + '\'' +
               ", directoryId=" + directoryId +
               ", parentId=" + parentId +
               '}';
    }
}

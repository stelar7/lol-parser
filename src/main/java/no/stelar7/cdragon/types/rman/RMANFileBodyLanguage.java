package no.stelar7.cdragon.types.rman;

import java.util.Objects;

public class RMANFileBodyLanguage
{
    private int    offset;
    private int    offsetTableOffset;
    private int    id;
    private int    nameOffset;
    private String name;
    
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
    
    public int getId()
    {
        return id;
    }
    
    public void setId(int id)
    {
        this.id = id;
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
        RMANFileBodyLanguage that = (RMANFileBodyLanguage) o;
        return offset == that.offset &&
               offsetTableOffset == that.offsetTableOffset &&
               id == that.id &&
               nameOffset == that.nameOffset &&
               Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(offset, offsetTableOffset, id, nameOffset, name);
    }
    
    @Override
    public String toString()
    {
        return "RMANFileBodyLanguage{" +
               "offset=" + offset +
               ", offsetTableOffset=" + offsetTableOffset +
               ", id=" + id +
               ", nameOffset=" + nameOffset +
               ", name='" + name + '\'' +
               '}';
    }
}

package no.stelar7.cdragon.types.rman.data;

import java.util.Objects;

public class RMANFileBodyLanguage
{
    private int    id;
    private String name;
    
    public int getId()
    {
        return id;
    }
    
    public void setId(int id)
    {
        this.id = id;
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
        return id == that.id &&
               Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(id, name);
    }
    
    @Override
    public String toString()
    {
        return "RMANFileBodyLanguage{" +
               "id=" + id +
               ", name='" + name + '\'' +
               '}';
    }
}

package no.stelar7.cdragon.types.crid.data.utf;


import java.util.*;

public class CRIDUTFTableColumn
{
    int          typeFlags;
    String       columnName;
    int          constantOffset;
    List<Object> values = new ArrayList<>();
    
    public int getTypeFlags()
    {
        return typeFlags;
    }
    
    public void setTypeFlags(int typeFlags)
    {
        this.typeFlags = typeFlags;
    }
    
    public String getColumnName()
    {
        return columnName;
    }
    
    public void setColumnName(String columnName)
    {
        this.columnName = columnName;
    }
    
    public int getConstantOffset()
    {
        return constantOffset;
    }
    
    public void setConstantOffset(int constantOffset)
    {
        this.constantOffset = constantOffset;
    }
    
    public List<Object> getValues()
    {
        return values;
    }
    
    public void setValues(List<Object> values)
    {
        this.values = values;
    }
    
    @Override
    public String toString()
    {
        return "CRIDUTFTableColumn{" +
               "type=" + Integer.toBinaryString(typeFlags) +
               ", columnName='" + columnName + '\'' +
               ", constantOffset=" + constantOffset +
               ", values=" + values +
               '}';
    }
}

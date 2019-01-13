package no.stelar7.cdragon.types.crid.data.utf;

import java.util.*;

public class CRIDUTFTableColumn
{
    int          type;
    String       columnName;
    int          constantOffset;
    List<Object> values = new ArrayList<>();
    
    public int getType()
    {
        return type;
    }
    
    public void setType(int type)
    {
        this.type = type;
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
               "type=" + type +
               ", columnName='" + columnName + '\'' +
               ", constantOffset=" + constantOffset +
               ", values=" + values +
               '}';
    }
}

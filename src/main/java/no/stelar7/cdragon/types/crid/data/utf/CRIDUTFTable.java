package no.stelar7.cdragon.types.crid.data.utf;

import java.util.List;

public class CRIDUTFTable
{
    int                      tableOffset;
    int                      tableSize;
    int                      schemaOffset;
    int                      rowOffset;
    int                      stringTableOffset;
    int                      dataOffset;
    String                   stringTable;
    String                   tableName;
    short                    columns;
    short                    rowWidth;
    int                      rows;
    List<CRIDUTFTableColumn> schema;
    
    public int getTableOffset()
    {
        return tableOffset;
    }
    
    public void setTableOffset(int tableOffset)
    {
        this.tableOffset = tableOffset;
    }
    
    public int getTableSize()
    {
        return tableSize;
    }
    
    public void setTableSize(int tableSize)
    {
        this.tableSize = tableSize;
    }
    
    public int getSchemaOffset()
    {
        return schemaOffset;
    }
    
    public void setSchemaOffset(int schemaOffset)
    {
        this.schemaOffset = schemaOffset;
    }
    
    public int getRowOffset()
    {
        return rowOffset;
    }
    
    public void setRowOffset(int rowOffset)
    {
        this.rowOffset = rowOffset;
    }
    
    public int getStringTableOffset()
    {
        return stringTableOffset;
    }
    
    public void setStringTableOffset(int stringTableOffset)
    {
        this.stringTableOffset = stringTableOffset;
    }
    
    public int getDataOffset()
    {
        return dataOffset;
    }
    
    public void setDataOffset(int dataOffset)
    {
        this.dataOffset = dataOffset;
    }
    
    public String getStringTable()
    {
        return stringTable;
    }
    
    public void setStringTable(String stringTable)
    {
        this.stringTable = stringTable;
    }
    
    public String getTableName()
    {
        return tableName;
    }
    
    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }
    
    public short getColumns()
    {
        return columns;
    }
    
    public void setColumns(short columns)
    {
        this.columns = columns;
    }
    
    public short getRowWidth()
    {
        return rowWidth;
    }
    
    public void setRowWidth(short rowWidth)
    {
        this.rowWidth = rowWidth;
    }
    
    public int getRows()
    {
        return rows;
    }
    
    public void setRows(int rows)
    {
        this.rows = rows;
    }
    
    public List<CRIDUTFTableColumn> getSchema()
    {
        return schema;
    }
    
    public void setSchema(List<CRIDUTFTableColumn> schema)
    {
        this.schema = schema;
    }
    
    @Override
    public String toString()
    {
        return "CRIDUTFTable{" +
               "tableOffset=" + tableOffset +
               ", tableSize=" + tableSize +
               ", schemaOffset=" + schemaOffset +
               ", rowOffset=" + rowOffset +
               ", stringTableOffset=" + stringTableOffset +
               ", dataOffset=" + dataOffset +
               ", stringTable='" + stringTable + '\'' +
               ", tableName='" + tableName + '\'' +
               ", columns=" + columns +
               ", rowWidth=" + rowWidth +
               ", rows=" + rows +
               ", schema=" + schema +
               '}';
    }
    
    public Object query(String columnName, int row)
    {
        for (CRIDUTFTableColumn column : getSchema())
        {
            if (column.getColumnName().equals(columnName))
            {
                return column.getValues().get(row);
            }
        }
        
        return null;
    }
}

package no.stelar7.cdragon.types.wem.data;

public class WEMFile
{
    private int     dataOffset;
    private int     dataLength;
    private String  filename;
    private WEMData data;
    
    public int getDataOffset()
    {
        return dataOffset;
    }
    
    public void setDataOffset(int dataOffset)
    {
        this.dataOffset = dataOffset;
    }
    
    public int getDataLength()
    {
        return dataLength;
    }
    
    public void setDataLength(int dataLength)
    {
        this.dataLength = dataLength;
    }
    
    public String getFilename()
    {
        return filename;
    }
    
    public void setFilename(String filename)
    {
        this.filename = filename;
    }
    
    public WEMData getData()
    {
        return data;
    }
    
    public void setData(WEMData data)
    {
        this.data = data;
    }
    
    @Override
    public String toString()
    {
        return "WEMFile{" +
               "filename='" + filename + '\'' +
               ", data=" + data +
               '}';
    }
}

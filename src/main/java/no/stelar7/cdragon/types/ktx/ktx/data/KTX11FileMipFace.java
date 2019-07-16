package no.stelar7.cdragon.types.ktx.ktx.data;

import java.util.Arrays;

public class KTX11FileMipFace
{
    private byte[] data;
    private byte[] padding;
    
    
    public byte[] getPadding()
    {
        return padding;
    }
    
    public void setPadding(byte[] padding)
    {
        this.padding = padding;
    }
    
    public byte[] getData()
    {
        return data;
    }
    
    public void setData(byte[] data)
    {
        this.data = data;
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
        KTX11FileMipFace that = (KTX11FileMipFace) o;
        return Arrays.equals(data, that.data) &&
               Arrays.equals(padding, that.padding);
    }
    
    @Override
    public int hashCode()
    {
        int result = Arrays.hashCode(data);
        result = 31 * result + Arrays.hashCode(padding);
        return result;
    }
    
    @Override
    public String toString()
    {
        return "KTX11FileMipFace{" +
               "data=" + Arrays.toString(data) +
               ", padding=" + Arrays.toString(padding) +
               '}';
    }
}

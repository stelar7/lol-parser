package no.stelar7.cdragon.types.ktx.ktx.data;

import java.util.*;

public class KTX11FileKeyValuePair
{
    private int    keyValueByteSize;
    private byte[] keyValue;
    private byte[] padding;
    
    public int getKeyValueByteSize()
    {
        return keyValueByteSize;
    }
    
    public void setKeyValueByteSize(int keyValueByteSize)
    {
        this.keyValueByteSize = keyValueByteSize;
    }
    
    public byte[] getKeyValue()
    {
        return keyValue;
    }
    
    public void setKeyValue(byte[] keyValue)
    {
        this.keyValue = keyValue;
    }
    
    public byte[] getPadding()
    {
        return padding;
    }
    
    public void setPadding(byte[] padding)
    {
        this.padding = padding;
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
        KTX11FileKeyValuePair that = (KTX11FileKeyValuePair) o;
        return keyValueByteSize == that.keyValueByteSize &&
               Arrays.equals(keyValue, that.keyValue) &&
               Arrays.equals(padding, that.padding);
    }
    
    @Override
    public int hashCode()
    {
        int result = Objects.hash(keyValueByteSize);
        result = 31 * result + Arrays.hashCode(keyValue);
        result = 31 * result + Arrays.hashCode(padding);
        return result;
    }
    
    @Override
    public String toString()
    {
        return "KTX11FileKeyValuePair{" +
               "keyValueByteSize=" + keyValueByteSize +
               ", keyValue=" + Arrays.toString(keyValue) +
               ", padding=" + Arrays.toString(padding) +
               '}';
    }
}

package no.stelar7.cdragon.types.ktx.ktx.data;

import java.util.*;

public class KTX11FileMipMap
{
    int                  imageSize;
    Map<Integer, byte[]> textureData = new HashMap<>();
    
    public int getImageSize()
    {
        return imageSize;
    }
    
    public void setImageSize(int imageSize)
    {
        this.imageSize = imageSize;
    }
    
    public Map<Integer, byte[]> getTextureData()
    {
        return textureData;
    }
    
    public void setTextureData(int level, byte[] textureData)
    {
        this.textureData.put(level, textureData);
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
        KTX11FileMipMap that = (KTX11FileMipMap) o;
        return imageSize == that.imageSize &&
               textureData.equals(that.textureData);
    }
    
    @Override
    public int hashCode()
    {
        int result = Objects.hash(imageSize);
        result = 31 * result + Objects.hash(textureData);
        return result;
    }
    
    @Override
    public String toString()
    {
        return "KTX11FileMipMap{" +
               "imageSize=" + imageSize +
               ", textureData=" + textureData +
               '}';
    }
}

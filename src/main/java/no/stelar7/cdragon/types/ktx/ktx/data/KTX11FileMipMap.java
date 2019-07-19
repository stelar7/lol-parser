package no.stelar7.cdragon.types.ktx.ktx.data;

import java.util.*;

public class KTX11FileMipMap
{
    int                                  imageSize;
    Map<Integer, KTX11FileMipMapTexture> textureData = new HashMap<>();
    
    public int getImageSize()
    {
        return imageSize;
    }
    
    public void setImageSize(int imageSize)
    {
        this.imageSize = imageSize;
    }
    
    public Map<Integer, KTX11FileMipMapTexture> getTextureData()
    {
        return textureData;
    }
    
    public void setTextureData(int level, KTX11FileMipMapTexture texture)
    {
        this.textureData.put(level, texture);
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

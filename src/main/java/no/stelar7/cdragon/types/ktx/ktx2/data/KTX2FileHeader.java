package no.stelar7.cdragon.types.ktx.ktx2.data;

import java.util.*;

public class KTX2FileHeader
{
    private byte[] magic;
    private int    format;
    private int    typeSize;
    private int    pixelWidth;
    private int    pixelHeight;
    private int    pixelDepth;
    private int    arrayElementCount;
    private int    faceCount;
    private int    levelCount;
    private int    supercompressionScheme;
    
    public void setMagic(byte[] magic)
    {
        this.magic = magic;
    }
    
    public void setFormat(int format)
    {
        this.format = format;
    }
    
    public int getTypeSize()
    {
        return typeSize;
    }
    
    public void setTypeSize(int typeSize)
    {
        this.typeSize = typeSize;
    }
    
    public int getPixelWidth()
    {
        return pixelWidth;
    }
    
    public void setPixelWidth(int pixelWidth)
    {
        this.pixelWidth = pixelWidth;
    }
    
    public int getPixelHeight()
    {
        return pixelHeight;
    }
    
    public void setPixelHeight(int pixelHeight)
    {
        this.pixelHeight = pixelHeight;
    }
    
    public int getPixelDepth()
    {
        return pixelDepth;
    }
    
    public void setPixelDepth(int pixelDepth)
    {
        this.pixelDepth = pixelDepth;
    }
    
    public int getArrayElementCount()
    {
        return arrayElementCount;
    }
    
    public void setArrayElementCount(int arrayElementCount)
    {
        this.arrayElementCount = arrayElementCount;
    }
    
    public int getFaceCount()
    {
        return faceCount;
    }
    
    public void setFaceCount(int faceCount)
    {
        this.faceCount = faceCount;
    }
    
    public int getLevelCount()
    {
        return levelCount;
    }
    
    public void setLevelCount(int levelCount)
    {
        this.levelCount = levelCount;
    }
    
    public int getSupercompressionScheme()
    {
        return supercompressionScheme;
    }
    
    public void setSupercompressionScheme(int supercompressionScheme)
    {
        this.supercompressionScheme = supercompressionScheme;
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
        KTX2FileHeader that = (KTX2FileHeader) o;
        return format == that.format &&
               typeSize == that.typeSize &&
               pixelWidth == that.pixelWidth &&
               pixelHeight == that.pixelHeight &&
               pixelDepth == that.pixelDepth &&
               arrayElementCount == that.arrayElementCount &&
               faceCount == that.faceCount &&
               levelCount == that.levelCount &&
               supercompressionScheme == that.supercompressionScheme &&
               Arrays.equals(magic, that.magic);
    }
    
    @Override
    public int hashCode()
    {
        int result = Objects.hash(format, typeSize, pixelWidth, pixelHeight, pixelDepth, arrayElementCount, faceCount, levelCount, supercompressionScheme);
        result = 31 * result + Arrays.hashCode(magic);
        return result;
    }
    
    @Override
    public String toString()
    {
        return "KTXFileHeader{" +
               "identifier=" + Arrays.toString(magic) +
               ", vkFormat=" + format +
               ", typeSize=" + typeSize +
               ", pixelWidth=" + pixelWidth +
               ", pixelHeight=" + pixelHeight +
               ", pixelDepth=" + pixelDepth +
               ", arrayElementCount=" + arrayElementCount +
               ", faceCount=" + faceCount +
               ", levelCount=" + levelCount +
               ", supercompressionScheme=" + supercompressionScheme +
               '}';
    }
}

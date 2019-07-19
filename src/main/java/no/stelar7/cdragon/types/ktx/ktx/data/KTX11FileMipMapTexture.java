package no.stelar7.cdragon.types.ktx.ktx.data;

import java.util.*;

public class KTX11FileMipMapTexture
{
    private TextureFormat format;
    private byte[]        data;
    private int           width;
    private int           height;
    private int           widthInBlocks;
    private int           heightInBlocks;
    
    public TextureFormat getFormat()
    {
        return format;
    }
    
    public void setFormat(TextureFormat format)
    {
        this.format = format;
    }
    
    public byte[] getData()
    {
        return data;
    }
    
    public void setData(byte[] data)
    {
        this.data = data;
    }
    
    public int getWidth()
    {
        return width;
    }
    
    public void setWidth(int width)
    {
        this.width = width;
    }
    
    public int getHeight()
    {
        return height;
    }
    
    public void setHeight(int height)
    {
        this.height = height;
    }
    
    public int getWidthInBlocks()
    {
        return widthInBlocks;
    }
    
    public void setWidthInBlocks(int widthInBlocks)
    {
        this.widthInBlocks = widthInBlocks;
    }
    
    public int getHeightInBlocks()
    {
        return heightInBlocks;
    }
    
    public void setHeightInBlocks(int heightInBlocks)
    {
        this.heightInBlocks = heightInBlocks;
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
        KTX11FileMipMapTexture that = (KTX11FileMipMapTexture) o;
        return width == that.width &&
               height == that.height &&
               widthInBlocks == that.widthInBlocks &&
               heightInBlocks == that.heightInBlocks &&
               format == that.format &&
               Arrays.equals(data, that.data);
    }
    
    @Override
    public int hashCode()
    {
        int result = Objects.hash(format, width, height, widthInBlocks, heightInBlocks);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
    
    @Override
    public String toString()
    {
        return "KTX11FileMipMapTexture{" +
               "format=" + format +
               ", data=" + Arrays.toString(data) +
               ", width=" + width +
               ", height=" + height +
               ", widthInBlocks=" + widthInBlocks +
               ", heightInBlocks=" + heightInBlocks +
               '}';
    }
}

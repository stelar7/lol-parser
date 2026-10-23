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
    private int           depth          = 1;
    private int           faces          = 1;
    private int           arrayElements  = 1;
    private int           rowPitch;
    private int           imageByteSize;
    
    /**
     * The z slice count of this level (1 for 2D textures)
     */
    public int getDepth()
    {
        return depth;
    }
    
    public void setDepth(int depth)
    {
        this.depth = depth;
    }
    
    public int getFaces()
    {
        return faces;
    }
    
    public void setFaces(int faces)
    {
        this.faces = faces;
    }
    
    public int getArrayElements()
    {
        return arrayElements;
    }
    
    public void setArrayElements(int arrayElements)
    {
        this.arrayElements = arrayElements;
    }
    
    /**
     * Bytes per row (including KTX's 4 byte row alignment) for uncompressed formats, 0 for block compressed formats
     */
    public int getRowPitch()
    {
        return rowPitch;
    }
    
    public void setRowPitch(int rowPitch)
    {
        this.rowPitch = rowPitch;
    }
    
    /**
     * The size of one 2D image (one z slice of one face of one array element) in {@link #getData()}
     */
    public int getImageByteSize()
    {
        return imageByteSize;
    }
    
    public void setImageByteSize(int imageByteSize)
    {
        this.imageByteSize = imageByteSize;
    }
    
    /**
     * Returns one 2D image of this level. {@link #getData()} holds all images ordered by array element, then face, then z slice.
     */
    public byte[] getImage(int arrayElement, int face, int slice)
    {
        if (arrayElement < 0 || arrayElement >= arrayElements || face < 0 || face >= faces || slice < 0 || slice >= depth)
        {
            throw new IndexOutOfBoundsException("Image " + arrayElement + "/" + face + "/" + slice + " does not exist");
        }
        
        int index = (arrayElement * faces + face) * depth + slice;
        return Arrays.copyOfRange(data, index * imageByteSize, (index + 1) * imageByteSize);
    }
    
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

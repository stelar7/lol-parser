package no.stelar7.cdragon.types.ktx.ktx.data;

import java.util.*;

public class KTX11FileHeader
{
    
    private byte[]        identifier;
    private int           endianness;
    private boolean       reversedEndian;
    private int           glType;
    private int           glTypeSize;
    private int           glFormat;
    private int           glInternalFormat;
    private int           glBaseInternalFormat;
    private int           pixelWidth;
    private int           pixelHeight;
    private int           pixelDepth;
    private int           numberOfArrayElements;
    private int           numberOfFaces;
    private int           numberOfMipmapLevels;
    private int           bytesOfKeyValueData;
    private TextureFormat textureFormat;
    private int           bytesPerBlock;
    
    public int getBytesPerBlock()
    {
        return bytesPerBlock;
    }
    
    public void setBytesPerBlock(int bytesPerBlock)
    {
        this.bytesPerBlock = bytesPerBlock;
    }
    
    public boolean isReversedEndian()
    {
        return reversedEndian;
    }
    
    public TextureFormat getTextureFormat()
    {
        return textureFormat;
    }
    
    public void setTextureFormat(TextureFormat format)
    {
        this.textureFormat = format;
    }
    
    public void setReversedEndian(boolean reversedEndian)
    {
        this.reversedEndian = reversedEndian;
    }
    
    public byte[] getIdentifier()
    {
        return identifier;
    }
    
    public void setIdentifier(byte[] identifier)
    {
        this.identifier = identifier;
    }
    
    public int getEndianness()
    {
        return endianness;
    }
    
    public void setEndianness(int endianness)
    {
        this.endianness = endianness;
    }
    
    public int getGlType()
    {
        return glType;
    }
    
    public void setGlType(int glType)
    {
        this.glType = glType;
    }
    
    public int getGlTypeSize()
    {
        return glTypeSize;
    }
    
    public void setGlTypeSize(int glTypeSize)
    {
        this.glTypeSize = glTypeSize;
    }
    
    public int getGlFormat()
    {
        return glFormat;
    }
    
    public void setGlFormat(int glFormat)
    {
        this.glFormat = glFormat;
    }
    
    public int getGlInternalFormat()
    {
        return glInternalFormat;
    }
    
    public void setGlInternalFormat(int glInternalFormat)
    {
        this.glInternalFormat = glInternalFormat;
    }
    
    public int getGlBaseInternalFormat()
    {
        return glBaseInternalFormat;
    }
    
    public void setGlBaseInternalFormat(int glBaseInternalFormat)
    {
        this.glBaseInternalFormat = glBaseInternalFormat;
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
    
    public int getNumberOfArrayElements()
    {
        return numberOfArrayElements;
    }
    
    public void setNumberOfArrayElements(int numberOfArrayElements)
    {
        this.numberOfArrayElements = numberOfArrayElements;
    }
    
    public int getNumberOfFaces()
    {
        return numberOfFaces;
    }
    
    public void setNumberOfFaces(int numberOfFaces)
    {
        this.numberOfFaces = numberOfFaces;
    }
    
    public int getNumberOfMipmapLevels()
    {
        return numberOfMipmapLevels;
    }
    
    public void setNumberOfMipmapLevels(int numberOfMipmapLevels)
    {
        this.numberOfMipmapLevels = numberOfMipmapLevels;
    }
    
    public int getBytesOfKeyValueData()
    {
        return bytesOfKeyValueData;
    }
    
    public void setBytesOfKeyValueData(int bytesOfKeyValueData)
    {
        this.bytesOfKeyValueData = bytesOfKeyValueData;
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
        KTX11FileHeader ktx11File = (KTX11FileHeader) o;
        return endianness == ktx11File.endianness &&
               glType == ktx11File.glType &&
               glTypeSize == ktx11File.glTypeSize &&
               glFormat == ktx11File.glFormat &&
               glInternalFormat == ktx11File.glInternalFormat &&
               glBaseInternalFormat == ktx11File.glBaseInternalFormat &&
               pixelWidth == ktx11File.pixelWidth &&
               pixelHeight == ktx11File.pixelHeight &&
               pixelDepth == ktx11File.pixelDepth &&
               numberOfArrayElements == ktx11File.numberOfArrayElements &&
               numberOfFaces == ktx11File.numberOfFaces &&
               numberOfMipmapLevels == ktx11File.numberOfMipmapLevels &&
               bytesOfKeyValueData == ktx11File.bytesOfKeyValueData &&
               Arrays.equals(identifier, ktx11File.identifier);
    }
    
    @Override
    public int hashCode()
    {
        int result = Objects.hash(endianness, glType, glTypeSize, glFormat, glInternalFormat, glBaseInternalFormat, pixelWidth, pixelHeight, pixelDepth, numberOfArrayElements, numberOfFaces, numberOfMipmapLevels, bytesOfKeyValueData);
        result = 31 * result + Arrays.hashCode(identifier);
        return result;
    }
    
    @Override
    public String toString()
    {
        return "KTX11File{" +
               "identifier=" + Arrays.toString(identifier) +
               ", endianness=" + endianness +
               ", reversedEndian=" + isReversedEndian() +
               ", glType=" + glType +
               ", glTypeSize=" + glTypeSize +
               ", glFormat=" + glFormat +
               ", glInternalFormat=" + glInternalFormat +
               ", glBaseInternalFormat=" + glBaseInternalFormat +
               ", textureFormat=" + textureFormat +
               ", pixelWidth=" + pixelWidth +
               ", pixelHeight=" + pixelHeight +
               ", pixelDepth=" + pixelDepth +
               ", numberOfArrayElements=" + numberOfArrayElements +
               ", numberOfFaces=" + numberOfFaces +
               ", numberOfMipmapLevels=" + numberOfMipmapLevels +
               ", bytesOfKeyValueData=" + bytesOfKeyValueData +
               '}';
    }
}

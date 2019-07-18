package no.stelar7.cdragon.types.ktx.ktx.data;

import java.util.*;

public class KTX11FileHeader
{
    
    private byte[]  identifier;
    private int     endianness;
    private boolean reversedEndian;
    private int     glType;
    private int     glTypeSize;
    private int     glFormat;
    private String  glFormatString;
    private int     glInternalFormat;
    private String  glInternalFormatString;
    private int     glBaseInternalFormat;
    private int     pixelWidth;
    private int     pixelHeight;
    private int     pixelDepth;
    private int     numberOfArrayElements;
    private int     numberOfFaces;
    private int     numberOfMipmapLevels;
    private int     bytesOfKeyValueData;
    
    public boolean isReversedEndian()
    {
        return reversedEndian;
    }
    
    
    public void setReversedEndian(boolean reversedEndian)
    {
        this.reversedEndian = reversedEndian;
    }
    
    public String getGlFormatString()
    {
        return glFormatString;
    }
    
    public String getGlInternalFormatString()
    {
        return glInternalFormatString;
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
        if (glFormat == 0)
        {
            this.glFormatString = "COMPRESSED";
        } else
        {
            this.glFormatString = String.valueOf(glFormat);
        }
    }
    
    public int getGlInternalFormat()
    {
        return glInternalFormat;
    }
    
    public void setGlInternalFormat(int glInternalFormat)
    {
        this.glInternalFormat = glInternalFormat;
        switch (glInternalFormat)
        {
            case 0x8D64:
            {
                this.glInternalFormatString = "GL_ETC1_RGB8_OES";
                break;
            }
            case 0x9274:
            {
                this.glInternalFormatString = "GL_COMPRESSED_RGB8_ETC2";
                break;
            }
            default:
            {
                this.glInternalFormatString = String.valueOf(glInternalFormat);
            }
        }
        
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
               ", glFormat=" + this.glFormatString +
               ", glInternalFormat=" + this.glInternalFormatString +
               ", glBaseInternalFormat=" + glBaseInternalFormat +
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

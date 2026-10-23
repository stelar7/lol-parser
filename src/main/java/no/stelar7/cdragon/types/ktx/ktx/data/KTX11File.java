package no.stelar7.cdragon.types.ktx.ktx.data;

import no.stelar7.cdragon.types.dds.TextureDecoder;
import no.stelar7.cdragon.util.readers.ParseException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;

public class KTX11File
{

    private KTX11FileHeader             header;
    private List<KTX11FileKeyValuePair> keyValueData;
    private KTX11FileMipMap             mipMaps;

    public KTX11FileMipMap getMipMaps()
    {
        return mipMaps;
    }

    public void setMipMaps(KTX11FileMipMap mipMaps)
    {
        this.mipMaps = mipMaps;
    }

    public KTX11FileHeader getHeader()
    {
        return header;
    }

    public void setHeader(KTX11FileHeader header)
    {
        this.header = header;
    }

    public List<KTX11FileKeyValuePair> getKeyValueData()
    {
        return keyValueData;
    }

    public void setKeyValueData(List<KTX11FileKeyValuePair> keyValueData)
    {
        this.keyValueData = keyValueData;
    }

    /**
     * Maps the format of this file to a format the decoder supports, or null if it can not be decoded
     */
    public TextureDecoder.Format getDecoderFormat()
    {
        if (header.getGlFormat() == 0)
        {
            return header.getTextureFormat() == null ? null : header.getTextureFormat().getDecoderFormat();
        }

        if (header.getGlFormat() == 0x1908 && header.getGlType() == 0x1403)
        {
            return TextureDecoder.Format.RGBA16_UNORM;
        }
        if (header.getGlFormat() == 0x1908 && header.getGlType() == 0x140B)
        {
            return TextureDecoder.Format.RGBA16_FLOAT;
        }

        // otherwise only 8 bit unsigned normalized channels are supported
        if (header.getGlType() != 0x1401)
        {
            return null;
        }

        return switch (header.getGlFormat())
        {
            case 0x1908 -> TextureDecoder.Format.RGBA8;
            case 0x80E1 -> TextureDecoder.Format.BGRA8;
            case 0x1907 -> TextureDecoder.Format.RGB8;
            case 0x80E0 -> TextureDecoder.Format.BGR8;
            case 0x1903, 0x1906, 0x1909 -> TextureDecoder.Format.R8;
            case 0x8227 -> TextureDecoder.Format.RG8;
            default -> null;
        };
    }

    /**
     * Decodes the first image (array element 0, face 0, z slice 0) of a mip level
     *
     * @throws ParseException if the texture format can not be decoded
     */
    public BufferedImage toBufferedImage(int level)
    {
        TextureDecoder.Format format = getDecoderFormat();
        if (format == null)
        {
            String name = header.getTextureFormat() != null ? header.getTextureFormat().name() : ("glInternalFormat 0x" + Integer.toHexString(header.getGlInternalFormat()));
            throw new ParseException("Decoding KTX textures with format " + name + " is not supported", -1);
        }

        KTX11FileMipMapTexture tex = mipMaps.getTextureData().get(level);
        if (tex == null)
        {
            throw new IllegalArgumentException("Level " + level + " does not exist");
        }

        return TextureDecoder.decode(format, tex.getData(), 0, tex.getWidth(), tex.getHeight(), tex.getRowPitch());
    }

    /**
     * Decodes an ETC1 mip level to tightly packed RGB bytes
     */
    public ByteBuffer decodeETC1(int level)
    {
        if (this.header.getTextureFormat() != TextureFormat.TEXTURE_FORMAT_ETC1)
        {
            throw new ParseException("Texture is not ETC1, found: " + this.header.getTextureFormat(), -1);
        }

        BufferedImage image  = toBufferedImage(level);
        ByteBuffer    output = ByteBuffer.allocate(image.getWidth() * image.getHeight() * 3);
        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = 0; x < image.getWidth(); x++)
            {
                int rgb = image.getRGB(x, y);
                output.put((byte) (rgb >> 16));
                output.put((byte) (rgb >> 8));
                output.put((byte) rgb);
            }
        }
        output.flip();
        return output;
    }

    /**
     * Decodes a mip level and writes it as a .bmp file
     */
    public void toImage(int level, Path output)
    {
        BufferedImage decoded = toBufferedImage(level);

        // BMP can not store alpha
        BufferedImage rgb = new BufferedImage(decoded.getWidth(), decoded.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.getGraphics().drawImage(decoded, 0, 0, null);

        try
        {
            ImageIO.write(rgb, "bmp", output.toFile());
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}

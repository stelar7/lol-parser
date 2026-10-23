package no.stelar7.cdragon.types.dds;


import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.ktx.ktx.KTX11Parser;
import no.stelar7.cdragon.types.ktx.ktx2.KTX2Parser;
import no.stelar7.cdragon.util.handlers.CompressionHandler;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.ByteArray;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

/**
 * Decodes the first image (mip 0, face/layer 0) of a DDS, KTX 1.1 or KTX2 texture to a BufferedImage.
 * Other image formats are passed on to ImageIO.
 * <p>
 * Throws a {@link ParseException} for data that is corrupt or in a format that can not be decoded.
 */
public class DDSParser implements Parseable<BufferedImage>
{
    private static final byte[] DDS_MAGIC = "DDS ".getBytes(StandardCharsets.US_ASCII);

    private static final int DDPF_ALPHAPIXELS = 0x1;
    private static final int DDPF_ALPHA       = 0x2;
    private static final int DDPF_FOURCC      = 0x4;
    private static final int DDPF_RGB         = 0x40;
    private static final int DDPF_YUV         = 0x200;
    private static final int DDPF_LUMINANCE   = 0x20000;

    private final KTX11Parser ktxParser  = new KTX11Parser();
    private final KTX2Parser  ktx2Parser = new KTX2Parser();

    @Override
    public BufferedImage parse(Path path)
    {
        try
        {
            return parse(Files.readAllBytes(path));
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public BufferedImage parse(ByteArray data)
    {
        return parse(data.getDataRaw());
    }

    @Override
    public BufferedImage parse(RandomAccessReader raf)
    {
        return parse(raf.readRemaining());
    }

    public BufferedImage parse(byte[] data)
    {
        if (startsWith(data, KTX11Parser.MAGIC))
        {
            return ktxParser.parse(new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN)).toBufferedImage(0);
        }

        if (startsWith(data, KTX2Parser.MAGIC))
        {
            return ktx2Parser.parse(new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN)).toImage(0);
        }

        if (startsWith(data, DDS_MAGIC))
        {
            BufferedImage image = parseDDS(data);
            if (image != null)
            {
                return image;
            }
        }

        // formats we do not decode ourselves (DDS variants handled by the ImageIO plugin, png, jpg, ...)
        BufferedImage image;
        try
        {
            image = ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException | RuntimeException e)
        {
            throw new ParseException("Unable to decode image: " + e.getMessage(), -1, e);
        }

        if (image == null)
        {
            throw new ParseException("Unsupported image format", 0);
        }
        return image;
    }

    /**
     * Decodes the formats we support natively, returns null for formats that should be passed on to ImageIO
     */
    private BufferedImage parseDDS(byte[] data)
    {
        if (data.length < 128)
        {
            throw new ParseException("File is too small to be a DDS file", 0);
        }

        RandomAccessReader raf = new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN);
        raf.seek(4);

        int headerSize = raf.readInt();
        if (headerSize != 124)
        {
            throw new ParseException("Invalid DDS header size " + headerSize, 4);
        }

        raf.readInt(); // flags
        int height = raf.readInt();
        int width  = raf.readInt();
        raf.readInt(); // pitchOrLinearSize, not reliable
        raf.readInt(); // depth
        raf.readInt(); // mipMapCount
        raf.seek(76);

        int pfSize = raf.readInt();
        if (pfSize != 32)
        {
            throw new ParseException("Invalid DDS pixel format size " + pfSize, 76);
        }

        int    pfFlags  = raf.readInt();
        String fourCC   = raf.readString(4);
        int    bitCount = raf.readInt();
        long   rMask    = raf.readU32();
        long   gMask    = raf.readU32();
        long   bMask    = raf.readU32();
        long   aMask    = raf.readU32();

        if (width <= 0 || height <= 0)
        {
            throw new ParseException("Invalid DDS dimensions " + width + "x" + height, 12);
        }

        int dataOffset = 128;
        if ((pfFlags & DDPF_FOURCC) != 0)
        {
            TextureDecoder.Format format;
            if ("DX10".equals(fourCC))
            {
                if (data.length < 148)
                {
                    throw new ParseException("DDS file is too small for its DX10 header", 128);
                }
                raf.seek(128);
                int dxgiFormat = raf.readInt();
                dataOffset = 148;

                format = fromDxgiFormat(dxgiFormat);

                if (format == null)
                {
                    throw new ParseException("Unsupported DDS DXGI format " + dxgiFormat, 128);
                }
            } else
            {
                format = switch (fourCC)
                {
                    // DXT1 with the 3 color mode producing transparent black, like D3D does
                    case "DXT1" -> TextureDecoder.Format.BC1A;
                    case "DXT2", "DXT3" -> TextureDecoder.Format.BC2;
                    case "DXT4", "DXT5" -> TextureDecoder.Format.BC3;
                    case "ATI1", "BC4U" -> TextureDecoder.Format.BC4;
                    case "ATI2", "BC5U" -> TextureDecoder.Format.BC5;
                    default -> null;
                };

                if (format == null)
                {
                    // let the ImageIO plugin try
                    return null;
                }
            }

            return TextureDecoder.decode(format, data, dataOffset, width, height);
        }

        if ((pfFlags & (DDPF_RGB | DDPF_LUMINANCE | DDPF_ALPHA | DDPF_YUV)) != 0)
        {
            if ((pfFlags & DDPF_YUV) != 0)
            {
                throw new ParseException("Unsupported DDS YUV pixel format", 80);
            }

            boolean luminance = (pfFlags & DDPF_LUMINANCE) != 0;
            boolean alphaOnly = (pfFlags & (DDPF_RGB | DDPF_LUMINANCE)) == 0;
            if ((pfFlags & (DDPF_ALPHAPIXELS | DDPF_ALPHA)) == 0)
            {
                aMask = 0;
            }

            if (alphaOnly)
            {
                return TextureDecoder.decodeMasked(data, dataOffset, width, height, bitCount, 0, 0, 0, 0, aMask, false);
            }

            return TextureDecoder.decodeMasked(data, dataOffset, width, height, bitCount, 0, rMask, gMask, bMask, aMask, luminance);
        }

        throw new ParseException("Unsupported DDS pixel format flags 0x" + Integer.toHexString(pfFlags), 80);
    }

    /**
     * Maps a DXGI_FORMAT to a decoder format, or null if it is not supported
     */
    public static TextureDecoder.Format fromDxgiFormat(int dxgiFormat)
    {
        return switch (dxgiFormat)
        {
            case 10 -> TextureDecoder.Format.RGBA16_FLOAT;
            case 11 -> TextureDecoder.Format.RGBA16_UNORM;
            case 13 -> TextureDecoder.Format.RGBA16_SNORM;
            case 28, 29 -> TextureDecoder.Format.RGBA8;
            case 87, 91 -> TextureDecoder.Format.BGRA8;
            case 61 -> TextureDecoder.Format.R8;
            case 49 -> TextureDecoder.Format.RG8;
            case 71, 72 -> TextureDecoder.Format.BC1A;
            case 74, 75 -> TextureDecoder.Format.BC2;
            case 77, 78 -> TextureDecoder.Format.BC3;
            case 80 -> TextureDecoder.Format.BC4;
            case 83 -> TextureDecoder.Format.BC5;
            case 95 -> TextureDecoder.Format.BC6H_UF16;
            case 96 -> TextureDecoder.Format.BC6H_SF16;
            case 97, 98, 99 -> TextureDecoder.Format.BC7;
            default ->
            {
                // the (unofficial, removed from the public headers) DXGI_FORMAT_ASTC_*: typeless/unorm/srgb triples from 133, step 4
                int[][] astc = {{4, 4}, {5, 4}, {5, 5}, {6, 5}, {6, 6}, {8, 5}, {8, 6}, {8, 8}, {10, 5}, {10, 6}, {10, 8}, {10, 10}, {12, 10}, {12, 12}};
                int     i    = (dxgiFormat - 133) / 4;
                if (dxgiFormat >= 133 && i < astc.length && (dxgiFormat - 133) % 4 < 3)
                {
                    yield TextureDecoder.Format.astc(astc[i][0], astc[i][1]);
                }
                yield null;
            }
        };
    }

    private static boolean startsWith(byte[] data, byte[] prefix)
    {
        return data.length >= prefix.length && Arrays.equals(data, 0, prefix.length, prefix, 0, prefix.length);
    }

    public BufferedImage parseCompressed(Path path)
    {
        byte[] dataBytes;
        try
        {
            dataBytes = CompressionHandler.uncompress(Files.readAllBytes(path));
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        if (dataBytes == null)
        {
            throw new ParseException("Unable to decompress " + path, -1);
        }
        return parse(dataBytes);
    }
}

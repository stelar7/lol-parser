package no.stelar7.cdragon.types.tex;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.dds.TextureDecoder;
import no.stelar7.cdragon.util.readers.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * League's own texture container (.tex): a 12 byte header followed by DDS style pixel data.
 * <pre>
 * char[4] magic "TEX\0"
 * u16     width
 * u16     height
 * u8      unknown
 * u8      format
 * u8      unknown
 * bool    hasMipmaps
 * </pre>
 * When there are mipmaps they are stored smallest first, so the full size image is the last one in the file.
 * <p>
 * Layout and format ids follow CDTB's export.py (TexToPngConverter.tex_to_dds).
 */
public class TEXParser implements Parseable<BufferedImage>
{
    public static final int FORMAT_DXT1   = 0x0A;
    public static final int FORMAT_DXT5   = 0x0C;
    public static final int FORMAT_BC7    = 0x0D;
    public static final int FORMAT_BC5    = 0x0E;
    public static final int FORMAT_BGRA8  = 0x14;
    public static final int FORMAT_RGBA16 = 0x15;
    /**
     * One byte per pixel, decoded as grey (written to DDS as R8_UNORM).
     * Every file seen so far (patch 16.22, 64x64 without mipmaps) is all zeros, so whether it is red, alpha or luminance is unknown.
     */
    public static final int FORMAT_R8     = 0x17;

    private static final int HEADER_SIZE = 12;

    /**
     * How TEX format 0x15 (RGBA16) is interpreted, used both for decoding and for the DXGI format written by {@link #toDDS}.
     * <p>
     * CDTB writes it as DXGI_FORMAT 13 = R16G16B16A16_SNORM (10 is FLOAT, 11 UNORM, 12 UINT), so that is what is used here.
     * This mapping is <b>unverified against real game files</b>; if it turns out to be wrong, change it here
     * (to RGBA16_UNORM or RGBA16_FLOAT) and both the PNG and the DDS output follow.
     */
    public static final TextureDecoder.Format RGBA16_FORMAT = TextureDecoder.Format.RGBA16_SNORM;

    /**
     * Decodes the full size image
     */
    @Override
    public BufferedImage parse(RandomAccessReader raf)
    {
        byte[] data   = raf.readRemaining();
        Header header = readHeader(data);

        TextureDecoder.Format decoderFormat = switch (header.format())
        {
            // DXT1 always allows the 3 color mode with transparent black, the same as DDSParser decodes a DXT1 dds
            case FORMAT_DXT1 -> TextureDecoder.Format.BC1A;
            case FORMAT_DXT5 -> TextureDecoder.Format.BC3;
            case FORMAT_BC7 -> TextureDecoder.Format.BC7;
            case FORMAT_BC5 -> TextureDecoder.Format.BC5;
            case FORMAT_BGRA8 -> TextureDecoder.Format.BGRA8;
            case FORMAT_RGBA16 -> RGBA16_FORMAT;
            case FORMAT_R8 -> TextureDecoder.Format.R8;
            default -> throw new ParseException("TEX format 0x" + Integer.toHexString(header.format()) + " can not be decoded to an image, use toDDS instead", 9);
        };

        return TextureDecoder.decode(decoderFormat, data, largestMipOffset(data, header), header.width(), header.height());
    }

    private static int rgba16DxgiFormat()
    {
        return switch (RGBA16_FORMAT)
        {
            case RGBA16_FLOAT -> 10;
            case RGBA16_UNORM -> 11;
            case RGBA16_SNORM -> 13;
            default -> throw new IllegalStateException(RGBA16_FORMAT.name());
        };
    }

    /**
     * Converts a TEX file to a DDS file containing only the full size image, the same way CDTB does.
     * This works for every known format.
     */
    public static byte[] toDDS(byte[] data)
    {
        Header header = readHeader(data);

        ByteBuffer pixelFormat = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer dx10        = null;
        pixelFormat.putInt(32);
        switch (header.format())
        {
            case FORMAT_DXT1 -> pixelFormat.putInt(0x4).put("DXT1".getBytes(StandardCharsets.US_ASCII));
            case FORMAT_DXT5 -> pixelFormat.putInt(0x4).put("DXT5".getBytes(StandardCharsets.US_ASCII));
            case FORMAT_BC7, FORMAT_BC5, FORMAT_RGBA16, FORMAT_R8 ->
            {
                pixelFormat.putInt(0x4).put("DX10".getBytes(StandardCharsets.US_ASCII));
                int dxgiFormat = switch (header.format())
                {
                    case FORMAT_BC7 -> 99;
                    case FORMAT_BC5 -> 83; // BC5_UNORM
                    case FORMAT_R8 -> 61; // R8_UNORM
                    default -> rgba16DxgiFormat();
                };
                // dxgiFormat, dimension (2D), miscFlag, arraySize, miscFlags2
                dx10 = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN).putInt(dxgiFormat).putInt(3).putInt(0).putInt(1).putInt(1);
            }
            case FORMAT_BGRA8 -> pixelFormat.putInt(0x41).putInt(0).putInt(32).putInt(0x00FF0000).putInt(0x0000FF00).putInt(0x000000FF).putInt(0xFF000000);
            default -> throw new ParseException("Unsupported TEX format 0x" + Integer.toHexString(header.format()), 9);
        }

        int offset = header.hasMipmaps() ? largestMipOffset(data, header) : HEADER_SIZE;

        ByteBuffer dds = ByteBuffer.allocate(128 + (dx10 == null ? 0 : 20) + data.length - offset).order(ByteOrder.LITTLE_ENDIAN);
        dds.put("DDS ".getBytes(StandardCharsets.US_ASCII));
        dds.putInt(124);
        dds.putInt(0x1007); // caps | height | width | pixelformat
        dds.putInt(header.height());
        dds.putInt(header.width());
        dds.position(dds.position() + 56); // pitch, depth, mipmap count, reserved
        dds.put(pixelFormat.array());
        dds.putInt(0x1000); // texture
        dds.position(dds.position() + 16);
        if (dx10 != null)
        {
            dds.put(dx10.array());
        }
        dds.put(data, offset, data.length - offset);
        return dds.array();
    }

    public record Header(int width, int height, int format, boolean hasMipmaps) {}

    public static boolean isTEX(byte[] data)
    {
        return data.length >= HEADER_SIZE && data[0] == 'T' && data[1] == 'E' && data[2] == 'X' && data[3] == 0;
    }

    public static Header readHeader(byte[] data)
    {
        if (!isTEX(data))
        {
            throw new ParseException("Not a TEX file", 0);
        }

        RandomAccessReader raf = new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN);
        raf.seek(4);
        int width  = raf.readU16();
        int height = raf.readU16();
        raf.readByte();
        int format = raf.readU8();
        raf.readByte();
        boolean hasMipmaps = raf.readBoolean();
        return new Header(width, height, format, hasMipmaps);
    }

    /**
     * The full size image is the last mip in the file
     */
    private static int largestMipOffset(byte[] data, Header header)
    {
        if (!header.hasMipmaps())
        {
            return HEADER_SIZE;
        }

        // uncompressed formats are stored per pixel, the rest in 4x4 blocks
        int blockSize = (header.format() == FORMAT_BGRA8 || header.format() == FORMAT_RGBA16 || header.format() == FORMAT_R8) ? 1 : 4;
        int blockBytes = switch (header.format())
        {
            case FORMAT_DXT1 -> 8;
            case FORMAT_DXT5, FORMAT_BC7, FORMAT_BC5 -> 16;
            case FORMAT_BGRA8 -> 4;
            case FORMAT_RGBA16 -> 8;
            case FORMAT_R8 -> 1;
            default -> throw new ParseException("Unsupported TEX format 0x" + Integer.toHexString(header.format()), 9);
        };

        long size = (long) blockBytes * ((header.width() + blockSize - 1) / blockSize) * ((header.height() + blockSize - 1) / blockSize);
        if (size > data.length - HEADER_SIZE)
        {
            throw new ParseException("TEX data too short for a " + header.width() + "x" + header.height() + " image", data.length);
        }
        return (int) (data.length - size);
    }
}

package no.stelar7.cdragon.types.ktx.ktx;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.ktx.ktx.data.*;
import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.util.*;

/**
 * https://registry.khronos.org/KTX/specs/1.0/ktxspec.v1.html
 */
public class KTX11Parser implements Parseable<KTX11File>
{
    public static final byte[] MAGIC = {(byte) 0xAB, 0x4B, 0x54, 0x58, 0x20, 0x31, 0x31, (byte) 0xBB, 0x0D, 0x0A, 0x1A, 0x0A};

    private static final int ENDIAN_NATIVE   = 0x04030201;
    private static final int ENDIAN_REVERSED = 0x01020304;

    @Override
    public KTX11File parse(RandomAccessReader raf)
    {
        if (raf.size() < 64)
        {
            throw new ParseException("File is too small to be a KTX file", 0);
        }

        KTX11File data = new KTX11File();
        data.setHeader(parseHeader(raf));
        data.setKeyValueData(parseKeyValueData(raf, data.getHeader().getBytesOfKeyValueData()));
        data.setMipMaps(parseMipMaps(raf, data.getHeader()));
        return data;
    }

    private KTX11FileMipMap parseMipMaps(RandomAccessReader raf, KTX11FileHeader header)
    {
        int mipCount = Math.max(1, header.getNumberOfMipmapLevels());

        int     faces        = Math.max(1, header.getNumberOfFaces());
        int     layers       = Math.max(1, header.getNumberOfArrayElements());
        boolean nonArrayCube = header.getNumberOfFaces() == 6 && header.getNumberOfArrayElements() == 0;

        TextureFormat format     = header.getTextureFormat();
        boolean       compressed = header.getGlFormat() == 0;

        KTX11FileMipMap map = new KTX11FileMipMap();
        for (int level = 0; level < mipCount; level++)
        {
            int width  = Math.max(1, header.getPixelWidth() >> level);
            int height = Math.max(1, header.getPixelHeight() >> level);
            int depth  = Math.max(1, header.getPixelDepth() >> level);

            int  widthInBlocks;
            int  heightInBlocks;
            int  rowPitch = 0;
            long singleImage;
            if (compressed)
            {
                widthInBlocks = (width + format.getBlockWidth() - 1) / format.getBlockWidth();
                heightInBlocks = (height + format.getBlockHeight() - 1) / format.getBlockHeight();
                singleImage = (long) widthInBlocks * heightInBlocks * header.getBytesPerBlock();
            } else
            {
                // uncompressed rows are padded to GL_UNPACK_ALIGNMENT = 4
                widthInBlocks = width;
                heightInBlocks = height;
                rowPitch = (width * header.getBytesPerBlock() + 3) & ~3;
                singleImage = (long) rowPitch * height;
            }

            long levelSize    = singleImage * depth * faces * layers;
            long expectedSize = nonArrayCube ? singleImage * depth : levelSize;

            long sizeOffset = raf.pos();
            if (raf.remaining() < 4)
            {
                throw new ParseException("KTX file ends before mip level " + level, raf);
            }

            long imageSize = raf.readU32();
            if (imageSize != expectedSize)
            {
                throw new ParseException("KTX mip level " + level + " imageSize is " + imageSize + ", expected " + expectedSize, sizeOffset);
            }

            if (levelSize > Integer.MAX_VALUE)
            {
                throw new ParseException("KTX mip level " + level + " is too large (" + levelSize + " bytes)", sizeOffset);
            }

            byte[] levelData = new byte[(int) levelSize];
            if (nonArrayCube)
            {
                // each face is stored separately, followed by cubePadding
                for (int face = 0; face < faces; face++)
                {
                    readFully(raf, levelData, (int) (face * imageSize), (int) imageSize, level);
                    raf.align();
                }
            } else
            {
                readFully(raf, levelData, 0, (int) levelSize, level);
            }

            // mipPadding
            raf.align();

            if (header.isReversedEndian() && !compressed && (header.getGlTypeSize() == 2 || header.getGlTypeSize() == 4))
            {
                swapEndian(levelData, header.getGlTypeSize());
            }

            KTX11FileMipMapTexture tex = new KTX11FileMipMapTexture();
            tex.setFormat(format);
            tex.setWidth(width);
            tex.setHeight(height);
            tex.setDepth(depth);
            tex.setFaces(faces);
            tex.setArrayElements(layers);
            tex.setWidthInBlocks(widthInBlocks);
            tex.setHeightInBlocks(heightInBlocks);
            tex.setRowPitch(rowPitch);
            tex.setImageByteSize((int) singleImage);
            tex.setData(levelData);

            map.setImageSize((int) imageSize);
            map.setTextureData(level, tex);
        }

        return map;
    }

    private void readFully(RandomAccessReader raf, byte[] target, int offset, int length, int level)
    {
        if (raf.remaining() < length)
        {
            throw new ParseException("KTX mip level " + level + " data is truncated: need " + length + " bytes, " + raf.remaining() + " left", raf);
        }
        System.arraycopy(raf.readBytes(length), 0, target, offset, length);
    }

    private void swapEndian(byte[] data, int size)
    {
        for (int i = 0; i + size <= data.length; i += size)
        {
            for (int j = 0; j < size / 2; j++)
            {
                byte tmp = data[i + j];
                data[i + j] = data[i + size - 1 - j];
                data[i + size - 1 - j] = tmp;
            }
        }
    }

    private List<KTX11FileKeyValuePair> parseKeyValueData(RandomAccessReader raf, int bytesOfKeyValueData)
    {
        List<KTX11FileKeyValuePair> values = new ArrayList<>();

        long start = raf.pos();
        long end   = start + Integer.toUnsignedLong(bytesOfKeyValueData);
        if (end > raf.size())
        {
            throw new ParseException("KTX key/value data (" + Integer.toUnsignedLong(bytesOfKeyValueData) + " bytes) is outside the file", start);
        }

        while (raf.pos() < end)
        {
            long entryStart = raf.pos();
            long size       = raf.readU32();
            if (raf.pos() + size > end)
            {
                throw new ParseException("Invalid KTX key/value entry length " + size, entryStart);
            }

            KTX11FileKeyValuePair pair = new KTX11FileKeyValuePair();
            pair.setKeyValueByteSize((int) size);
            pair.setKeyValue(raf.readBytes(size));
            pair.setPadding(raf.readBytes(Math.min(end - raf.pos(), 3 - ((size + 3) % 4))));
            values.add(pair);
        }

        raf.seek(end);
        return values;
    }

    private KTX11FileHeader parseHeader(RandomAccessReader raf)
    {
        KTX11FileHeader header = new KTX11FileHeader();
        header.setIdentifier(raf.readBytes(12));
        if (!Arrays.equals(header.getIdentifier(), MAGIC))
        {
            throw new ParseException("Not a KTX 1.1 file (invalid identifier)", 0);
        }

        raf.setEndian(ByteOrder.LITTLE_ENDIAN);
        header.setEndianness(raf.readInt());

        if (header.getEndianness() == ENDIAN_REVERSED)
        {
            // the file was written on a big endian machine; everything after this is big endian
            header.setReversedEndian(true);
            raf.setEndian(ByteOrder.BIG_ENDIAN);
        } else if (header.getEndianness() != ENDIAN_NATIVE)
        {
            throw new ParseException("Invalid KTX endianness marker 0x" + Integer.toHexString(header.getEndianness()), 12);
        }

        header.setGlType(raf.readInt());
        header.setGlTypeSize(raf.readInt());
        header.setGlFormat(raf.readInt());
        header.setGlInternalFormat(raf.readInt());
        header.setGlBaseInternalFormat(raf.readInt());
        header.setPixelWidth(raf.readInt());
        header.setPixelHeight(raf.readInt());
        header.setPixelDepth(raf.readInt());
        header.setNumberOfArrayElements(raf.readInt());
        header.setNumberOfFaces(raf.readInt());
        header.setNumberOfMipmapLevels(raf.readInt());
        header.setBytesOfKeyValueData(raf.readInt());

        if (header.getPixelWidth() <= 0 || header.getPixelHeight() < 0 || header.getPixelDepth() < 0 || header.getNumberOfArrayElements() < 0)
        {
            throw new ParseException("Invalid KTX dimensions " + header.getPixelWidth() + "x" + header.getPixelHeight() + "x" + header.getPixelDepth(), 36);
        }

        if (header.getNumberOfFaces() != 1 && header.getNumberOfFaces() != 6)
        {
            throw new ParseException("Invalid KTX numberOfFaces " + header.getNumberOfFaces(), 52);
        }

        if (header.getNumberOfMipmapLevels() < 0 || header.getNumberOfMipmapLevels() > 32)
        {
            throw new ParseException("Invalid KTX numberOfMipmapLevels " + header.getNumberOfMipmapLevels(), 56);
        }

        if (header.getGlFormat() == 0)
        {
            TextureFormat format = TextureFormat.fromCompressedGlInternalFormat(header.getGlInternalFormat());
            if (format == null)
            {
                throw new ParseException("Unsupported compressed KTX glInternalFormat 0x" + Integer.toHexString(header.getGlInternalFormat()), 28);
            }
            header.setTextureFormat(format);
            header.setBytesPerBlock(format.getBlockBytes());
        } else
        {
            int pixelSize = getUncompressedPixelSize(header.getGlFormat(), header.getGlType());
            if (pixelSize <= 0)
            {
                throw new ParseException("Unsupported uncompressed KTX format: glFormat 0x" + Integer.toHexString(header.getGlFormat()) + ", glType 0x" + Integer.toHexString(header.getGlType()), 20);
            }

            // may be null for formats not in the table, the data is still read correctly
            header.setTextureFormat(TextureFormat.fromUncompressedGl(header.getGlInternalFormat(), header.getGlFormat(), header.getGlType()));
            header.setBytesPerBlock(pixelSize);
        }

        return header;
    }

    /**
     * Bytes per pixel of an uncompressed GL format/type pair, or -1 if it is unknown
     */
    private int getUncompressedPixelSize(int glFormat, int glType)
    {
        switch (glType)
        {
            // packed 16 bit types
            case 0x8033, 0x8034, 0x8363, 0x8364, 0x8365, 0x8366:
                return 2;
            // packed 32 bit types
            case 0x8035, 0x8036, 0x8367, 0x8368, 0x8C3B, 0x8C3E, 0x84FA:
                return 4;
            default:
                break;
        }

        int componentSize = switch (glType)
        {
            case 0x1400, 0x1401 -> 1; // BYTE, UNSIGNED_BYTE
            case 0x1402, 0x1403, 0x140B, 0x8D61 -> 2; // SHORT, UNSIGNED_SHORT, HALF_FLOAT, HALF_FLOAT_OES
            case 0x1404, 0x1405, 0x1406 -> 4; // INT, UNSIGNED_INT, FLOAT
            default -> -1;
        };

        int components = switch (glFormat)
        {
            case 0x1901, 0x1902, 0x1903, 0x1904, 0x1905, 0x1906, 0x1909, 0x8D94 -> 1; // STENCIL, DEPTH, RED, GREEN, BLUE, ALPHA, LUMINANCE, RED_INTEGER
            case 0x8227, 0x8228, 0x190A, 0x84F9 -> 2; // RG, RG_INTEGER, LUMINANCE_ALPHA, DEPTH_STENCIL
            case 0x1907, 0x80E0, 0x8D98, 0x8D9A -> 3; // RGB, BGR, RGB_INTEGER, BGR_INTEGER
            case 0x1908, 0x80E1, 0x8D99, 0x8D9B -> 4; // RGBA, BGRA, RGBA_INTEGER, BGRA_INTEGER
            default -> -1;
        };

        if (componentSize < 0 || components < 0)
        {
            return -1;
        }

        return componentSize * components;
    }
}

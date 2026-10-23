package no.stelar7.cdragon.types.ktx.ktx2.data;

import com.github.luben.zstd.Zstd;
import no.stelar7.cdragon.types.dds.TextureDecoder;
import no.stelar7.cdragon.util.readers.ParseException;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

public class KTX2File
{
    public static final int SUPERCOMPRESSION_NONE    = 0;
    public static final int SUPERCOMPRESSION_BASISLZ = 1;
    public static final int SUPERCOMPRESSION_ZSTD    = 2;
    public static final int SUPERCOMPRESSION_ZLIB    = 3;

    KTX2FileHeader                          header;
    KTX2FileIndecies                        indecies;
    List<KTX2FileLevel>                     levels;
    int                                     dfdTotalSize;
    List<KTX2FileDataFormatDescriptorBlock> dataFormatBlocks;
    Map<String, byte[]>                     keyValueData = new LinkedHashMap<>();
    byte[]                                  supercompressionGlobalData;

    public KTX2FileHeader getHeader()
    {
        return header;
    }

    public void setHeader(KTX2FileHeader header)
    {
        this.header = header;
    }

    public KTX2FileIndecies getIndecies()
    {
        return indecies;
    }

    public void setIndecies(KTX2FileIndecies indecies)
    {
        this.indecies = indecies;
    }

    public List<KTX2FileLevel> getLevels()
    {
        return levels;
    }

    public void setLevels(List<KTX2FileLevel> levels)
    {
        this.levels = levels;
    }

    public int getDfdTotalSize()
    {
        return dfdTotalSize;
    }

    public void setDfdTotalSize(int dfdTotalSize)
    {
        this.dfdTotalSize = dfdTotalSize;
    }

    public List<KTX2FileDataFormatDescriptorBlock> getDataFormatBlocks()
    {
        return dataFormatBlocks;
    }

    public void setDataFormatBlocks(List<KTX2FileDataFormatDescriptorBlock> dataFormatBlocks)
    {
        this.dataFormatBlocks = dataFormatBlocks;
    }

    /**
     * Key/value metadata, in file order. Values are the raw bytes (string values usually include their terminating 0x00)
     */
    public Map<String, byte[]> getKeyValueData()
    {
        return keyValueData;
    }

    public void setKeyValueData(Map<String, byte[]> keyValueData)
    {
        this.keyValueData = keyValueData;
    }

    /**
     * Returns a key/value entry as a string (without a trailing 0x00), or null if the key is not present
     */
    public String getKeyValueString(String key)
    {
        byte[] value = keyValueData.get(key);
        if (value == null)
        {
            return null;
        }

        int end = value.length;
        while (end > 0 && value[end - 1] == 0)
        {
            end--;
        }
        return new String(value, 0, end, StandardCharsets.UTF_8);
    }

    /**
     * Supercompression global data (used by BasisLZ), or an empty array if the file has none
     */
    public byte[] getSupercompressionGlobalData()
    {
        return supercompressionGlobalData;
    }

    public void setSupercompressionGlobalData(byte[] supercompressionGlobalData)
    {
        this.supercompressionGlobalData = supercompressionGlobalData;
    }

    public int getLevelWidth(int level)
    {
        return Math.max(1, header.getPixelWidth() >> level);
    }

    public int getLevelHeight(int level)
    {
        return Math.max(1, header.getPixelHeight() >> level);
    }

    public int getLevelDepth(int level)
    {
        return Math.max(1, header.getPixelDepth() >> level);
    }

    /**
     * Returns the data for a mip level with the zstd/zlib supercompression removed.
     * Images are ordered by array layer, then face, then z slice.
     * <p>
     * BasisLZ data can not be decompressed without transcoding, so it is returned as stored.
     */
    public byte[] getLevelData(int level)
    {
        if (level < 0 || level >= levels.size())
        {
            throw new IllegalArgumentException("Level " + level + " does not exist, file has " + levels.size() + " levels");
        }

        KTX2FileLevel entry = levels.get(level);
        byte[]        data  = entry.getData();

        switch (header.getSupercompressionScheme())
        {
            case SUPERCOMPRESSION_NONE:
            case SUPERCOMPRESSION_BASISLZ:
                return data;
            case SUPERCOMPRESSION_ZSTD:
            {
                byte[] output;
                try
                {
                    output = Zstd.decompress(data, Math.toIntExact(entry.getUncompressedByteLength()));
                } catch (RuntimeException e)
                {
                    throw new ParseException("Failed to zstd decompress KTX2 level " + level, entry.getByteOffset(), e);
                }

                if (output.length != entry.getUncompressedByteLength())
                {
                    throw new ParseException("KTX2 level " + level + " decompressed to " + output.length + " bytes, expected " + entry.getUncompressedByteLength(), entry.getByteOffset());
                }
                return output;
            }
            case SUPERCOMPRESSION_ZLIB:
            {
                Inflater inflater = new Inflater();
                try
                {
                    inflater.setInput(data);
                    ByteArrayOutputStream out    = new ByteArrayOutputStream((int) Math.min(Integer.MAX_VALUE, entry.getUncompressedByteLength()));
                    byte[]                buffer = new byte[8192];
                    while (!inflater.finished())
                    {
                        int read = inflater.inflate(buffer);
                        if (read == 0 && (inflater.needsInput() || inflater.needsDictionary()))
                        {
                            throw new ParseException("Truncated zlib data in KTX2 level " + level, entry.getByteOffset());
                        }
                        out.write(buffer, 0, read);
                    }

                    if (out.size() != entry.getUncompressedByteLength())
                    {
                        throw new ParseException("KTX2 level " + level + " decompressed to " + out.size() + " bytes, expected " + entry.getUncompressedByteLength(), entry.getByteOffset());
                    }
                    return out.toByteArray();
                } catch (DataFormatException e)
                {
                    throw new ParseException("Failed to zlib decompress KTX2 level " + level, entry.getByteOffset(), e);
                } finally
                {
                    inflater.end();
                }
            }
            default:
                throw new ParseException("Unsupported KTX2 supercompression scheme " + header.getSupercompressionScheme(), entry.getByteOffset());
        }
    }

    public static final int KHR_DF_MODEL_ETC1S = 163;
    public static final int KHR_DF_MODEL_UASTC = 166;

    /**
     * The color model of the basic data format descriptor block, or -1 if there is none
     */
    public int getColorModel()
    {
        if (dataFormatBlocks != null)
        {
            for (KTX2FileDataFormatDescriptorBlock block : dataFormatBlocks)
            {
                if (block.getVendorId() == 0 && block.getDescriptorType() == 0)
                {
                    return block.getColorModel() & 0xFF;
                }
            }
        }
        return -1;
    }

    /**
     * Maps the VkFormat of this file (or the DFD color model, for UASTC) to a format the decoder supports,
     * or null if it is not supported
     */
    public TextureDecoder.Format getDecoderFormat()
    {
        int vkFormat = header.getFormat();
        if (vkFormat >= 157 && vkFormat <= 184)
        {
            // VK_FORMAT_ASTC_{4x4..12x12}_{UNORM,SRGB}_BLOCK
            int[][] sizes = {{4, 4}, {5, 4}, {5, 5}, {6, 5}, {6, 6}, {8, 5}, {8, 6}, {8, 8}, {10, 5}, {10, 6}, {10, 8}, {10, 10}, {12, 10}, {12, 12}};
            int[]   size  = sizes[(vkFormat - 157) / 2];
            return TextureDecoder.Format.astc(size[0], size[1]);
        }

        if (vkFormat == 0 && getColorModel() == KHR_DF_MODEL_UASTC)
        {
            return TextureDecoder.Format.UASTC_4x4;
        }

        return switch (vkFormat)
        {
            case 91 -> TextureDecoder.Format.RGBA16_UNORM;
            case 92 -> TextureDecoder.Format.RGBA16_SNORM;
            case 97 -> TextureDecoder.Format.RGBA16_FLOAT;
            case 143 -> TextureDecoder.Format.BC6H_UF16;
            case 144 -> TextureDecoder.Format.BC6H_SF16;
            case 145, 146 -> TextureDecoder.Format.BC7;
            case 147, 148 -> TextureDecoder.Format.ETC2_RGB;
            case 149, 150 -> TextureDecoder.Format.ETC2_RGB_A1;
            case 151, 152 -> TextureDecoder.Format.ETC2_RGBA;
            case 153 -> TextureDecoder.Format.EAC_R11;
            case 155 -> TextureDecoder.Format.EAC_RG11;
            case 9, 15 -> TextureDecoder.Format.R8;
            case 16, 22 -> TextureDecoder.Format.RG8;
            case 23, 29 -> TextureDecoder.Format.RGB8;
            case 30, 36 -> TextureDecoder.Format.BGR8;
            case 37, 43 -> TextureDecoder.Format.RGBA8;
            case 44, 50 -> TextureDecoder.Format.BGRA8;
            case 131, 132 -> TextureDecoder.Format.BC1;
            case 133, 134 -> TextureDecoder.Format.BC1A;
            case 135, 136 -> TextureDecoder.Format.BC2;
            case 137, 138 -> TextureDecoder.Format.BC3;
            case 139 -> TextureDecoder.Format.BC4;
            case 141 -> TextureDecoder.Format.BC5;
            default -> null;
        };
    }

    /**
     * Decodes the first image (array layer 0, face 0, z slice 0) of a mip level
     *
     * @throws ParseException if the texture format can not be decoded (BasisLZ/ETC1S, signed EAC/RGTC, 32 bit float, ...)
     */
    public BufferedImage toImage(int level)
    {
        if (header.getSupercompressionScheme() == SUPERCOMPRESSION_BASISLZ)
        {
            // ETC1S needs the global endpoint/selector codebooks and Huffman tables from the supercompression global data
            throw new ParseException("Decoding BasisLZ (ETC1S) supercompressed KTX2 textures is not supported", -1);
        }

        TextureDecoder.Format format = getDecoderFormat();
        if (format == null)
        {
            String name = header.getFormat() == 0 ? "VK_FORMAT_UNDEFINED (UASTC or DFD-only format)" : "VkFormat " + header.getFormat();
            throw new ParseException("Decoding KTX2 textures with " + name + " is not supported", -1);
        }

        byte[] data = getLevelData(level);
        return TextureDecoder.decode(format, data, 0, getLevelWidth(level), getLevelHeight(level));
    }

    @Override
    public String toString()
    {
        return "KTX2File{" +
               "header=" + header +
               ", indecies=" + indecies +
               ", levels=" + levels +
               ", dfdTotalSize=" + dfdTotalSize +
               ", dataFormatBlocks=" + dataFormatBlocks +
               ", keyValueData=" + keyValueData.keySet() +
               '}';
    }
}

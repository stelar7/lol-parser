package no.stelar7.cdragon.types.ktx.ktx.data;

import no.stelar7.cdragon.util.writers.ByteWriter;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;

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
    
    public void decompressETC(int level)
    {
        TextureFormat          outputFormat = TextureFormat.PIXEL_FORMAT_RGB8;
        KTX11FileMipMapTexture tex          = mipMaps.getTextureData().get(level);
        int                    size         = outputFormat.getPixelSize() * tex.getWidth() * tex.getHeight();
        ByteBuffer             input        = ByteBuffer.wrap(tex.getData());
        ByteBuffer             output       = ByteBuffer.allocate(size);
        
        if (!tex.getFormat().isCompressed())
        {
            convertPixels(input, tex.getWidth() * tex.getHeight(), tex.getFormat(), output, outputFormat);
        }
        
        ByteBuffer blockBuffer = ByteBuffer.allocate(256);
        int        pixelSize   = outputFormat.getPixelSize();
        int        dataOffset  = 0;
        for (int y = 0; y < tex.getHeightInBlocks(); y++)
        {
            int rows = 4;
            if ((y * 4) + 3 >= tex.getHeight())
            {
                rows = tex.getHeight() - (y * 4);
            }
            
            for (int x = 0; x < tex.getWidthInBlocks(); x++)
            {
                decompressBlock(input, dataOffset, tex.getFormat(), 0XFFFFFFFF, 0, blockBuffer, outputFormat);
                
                int blockSize = pixelSize * 16;
                int pixelPos  = y * 4 * tex.getWidth() * pixelSize + x * 4 * pixelSize;
                int columns   = 4;
                if ((x * 4) + 3 >= tex.getWidth())
                {
                    columns = tex.getWidth() - (x * 4);
                }
                
                for (int row = 0; row < rows; row++)
                {
                    byte[] buffer = new byte[columns * pixelSize];
                    input.get(buffer, row * 4 * pixelSize, buffer.length);
                    output.put(buffer, pixelPos + row * tex.getWidth() * pixelSize, buffer.length);
                }
                
                dataOffset += tex.getFormat().getCompressedBlockSize();
            }
            
        }
        
        
        System.out.println();
    }
    
    private void decompressBlock(ByteBuffer bitstring, int bitstringOffset, TextureFormat format, int modeMask, int flags, ByteBuffer pixelBuffer, TextureFormat pixelFormat)
    {
        ByteBuffer blockBuffer = ByteBuffer.allocate(256);
        byte[]     data        = ByteBuffer.allocate(8).putLong(bitstring.getLong(bitstringOffset)).array();
        decompressionFunctions.get(format.getCompressedFormat()).apply(data, modeMask, flags, blockBuffer);
        convertPixels(blockBuffer, 16, format, pixelBuffer, pixelFormat);
    }
    
    private void convertPixels(ByteBuffer input, int pixelCount, TextureFormat sourceFormat, ByteBuffer output, TextureFormat destFormat)
    {
        if (sourceFormat == destFormat)
        {
            return;
        }
        
        convertionFunctions.get(sourceFormat).get(destFormat).apply(input, pixelCount, output);
        System.out.println();
    }
    
    @FunctionalInterface
    interface TriFunction<A, B, C, R>
    {
        R apply(A a, B b, C c);
    }
    
    private static final Map<TextureFormat, Map<TextureFormat, TriFunction<ByteBuffer, Integer, ByteBuffer, Void>>> convertionFunctions = new HashMap<>()
    
    {
        {
            Map<TextureFormat, TriFunction<ByteBuffer, Integer, ByteBuffer, Void>> inner = new HashMap<>()
            {{
                put(TextureFormat.PIXEL_FORMAT_RGB8, (ByteBuffer input, Integer pixels, ByteBuffer output) -> {
                    for (int i = 0; i < pixels; i++)
                    {
                        // RGBA32 -> RGB24
                        int r = input.getInt() & 0xFF;
                        int g = (input.getInt() & 0xFF) >> 8;
                        int b = (input.getInt() & 0xFF) >> 16;
                        int a = (input.getInt() & 0xFF) >> 24;
                        
                        output.putInt(r);
                        output.putInt(g);
                        output.putInt(b);
                    }
                    
                    return null;
                });
            }};
            put(TextureFormat.TEXTURE_FORMAT_ETC2, inner);
        }
    };
    
    @FunctionalInterface
    interface QuadFunction<A, B, C, D, R>
    {
        R apply(A a, B b, C c, D d);
    }
    
    private static final Map<Integer, QuadFunction<byte[], Integer, Integer, ByteBuffer, Boolean>> decompressionFunctions = new HashMap<>()
    {
        {
            put(12, (byte[] bitstring, Integer modeMask, Integer flags, ByteBuffer pixelBuffer) -> {
                boolean differential_mode = (bitstring[3] & 2) != 0;
                if (differential_mode)
                {
                    if ((modeMask & 2) == 0)
                    {
                        return false;
                    }
                    
                } else if ((modeMask & 1) == 0)
                {
                    return false;
                }
                
                boolean isFlipped            = (bitstring[3] & 1) != 0;
                int[]   base_color_subblock1 = new int[3];
                int[]   base_color_subblock2 = new int[3];
                
                if (differential_mode)
                {
                    base_color_subblock1[0] = (bitstring[0] & 0xF8);
                    base_color_subblock1[0] |= ((base_color_subblock1[0] & 224) >> 5);
                    base_color_subblock1[1] = (bitstring[1] & 0xF8);
                    base_color_subblock1[1] |= (base_color_subblock1[1] & 224) >> 5;
                    base_color_subblock1[2] = (bitstring[2] & 0xF8);
                    base_color_subblock1[2] |= (base_color_subblock1[2] & 224) >> 5;
                    base_color_subblock2[0] = (bitstring[0] & 0xF8);
                    base_color_subblock2[0] += complement3bitshifted(bitstring[0] & 7);
                    if ((base_color_subblock2[0] & 0xFF07) > 0)
                    {
                        return false;
                    }
                    
                    base_color_subblock2[0] |= (base_color_subblock2[0] & 224) >> 5;
                    base_color_subblock2[1] = (bitstring[1] & 0xF8);
                    base_color_subblock2[1] += complement3bitshifted(bitstring[1] & 7);
                    if ((base_color_subblock2[1] & 0xFF07) > 0)
                    {
                        return false;
                    }
                    
                    base_color_subblock2[1] |= (base_color_subblock2[1] & 224) >> 5;
                    base_color_subblock2[2] = (bitstring[2] & 0xF8);
                    base_color_subblock2[2] += complement3bitshifted(bitstring[2] & 7);
                    if ((base_color_subblock2[2] & 0xFF07) > 0)
                    {
                        return false;
                    }
                    
                    base_color_subblock2[2] |= (base_color_subblock2[2] & 224) >> 5;
                } else
                {
                    base_color_subblock1[0] = (bitstring[0] & 0xF0);
                    base_color_subblock1[0] |= base_color_subblock1[0] >> 4;
                    base_color_subblock1[1] = (bitstring[1] & 0xF0);
                    base_color_subblock1[1] |= base_color_subblock1[1] >> 4;
                    base_color_subblock1[2] = (bitstring[2] & 0xF0);
                    base_color_subblock1[2] |= base_color_subblock1[2] >> 4;
                    base_color_subblock2[0] = (bitstring[0] & 0x0F);
                    base_color_subblock2[0] |= base_color_subblock2[0] << 4;
                    base_color_subblock2[1] = (bitstring[1] & 0x0F);
                    base_color_subblock2[1] |= base_color_subblock2[1] << 4;
                    base_color_subblock2[2] = (bitstring[2] & 0x0F);
                    base_color_subblock2[2] |= base_color_subblock2[2] << 4;
                }
                int table_codeword1 = (bitstring[3] & 224) >> 5;
                int table_codeword2 = (bitstring[3] & 28) >> 2;
                
                long indexA           = Integer.toUnsignedLong(Byte.toUnsignedInt(bitstring[4]) << 24);
                long indexB           = Integer.toUnsignedLong(Byte.toUnsignedInt(bitstring[5]) << 16);
                long indexC           = Integer.toUnsignedLong(Byte.toUnsignedInt(bitstring[6]) << 8);
                long indexD           = Integer.toUnsignedLong(Byte.toUnsignedInt(bitstring[7]) << 0);
                long pixel_index_word = indexA | indexB | indexC | indexD;
                
                if (!isFlipped)
                {
                    processPixelETC1(0, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(1, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(2, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(3, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(4, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(5, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(6, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(7, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(8, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(9, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(10, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(11, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(12, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(13, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(14, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(15, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                } else
                {
                    processPixelETC1(0, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(1, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(2, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(3, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(4, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(5, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(6, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(7, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(8, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(9, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(10, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(11, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(12, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(13, pixel_index_word, table_codeword1, base_color_subblock1, pixelBuffer);
                    processPixelETC1(14, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                    processPixelETC1(15, pixel_index_word, table_codeword2, base_color_subblock2, pixelBuffer);
                }
                
                return true;
            });
        }
        
        private void processPixelETC1(int i, long pixelIndexCode, int tableCode, int[] baseColorSubblock, ByteBuffer pixelBuffer)
        {
            int pixelIndex = (int) (((pixelIndexCode & (1 << i)) >> i) | ((pixelIndexCode & (0x10000 << i)) >> (16 + i - 1)));
            int modifier   = modifierTable[tableCode][pixelIndex];
            int r          = clampTable[baseColorSubblock[0] + modifier + 255];
            int g          = clampTable[baseColorSubblock[1] + modifier + 255];
            int b          = clampTable[baseColorSubblock[2] + modifier + 255];
            int index      = (i & 3) * 4 + ((i & 12) >> 2);
            int value      = pack32RGB8A0xFF(r, g, b);
            pixelBuffer.putInt(index, value);
        }
        
        private int pack32RGB8A0xFF(int r, int g, int b)
        {
            return pack32RGB8A(r, g, b, 0xff);
        }
        
        private int pack32RGB8A(int r, int g, int b, int a)
        {
            return (r | (g << 8) | (b << 16) | (a << 24));
        }
        
        int[] clampTable = new int[]{
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
                33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
                49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
                65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80,
                81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96,
                97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112,
                113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128,
                129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144,
                145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160,
                161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176,
                177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192,
                193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208,
                209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224,
                225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240,
                241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255
        };
        
        int[][] modifierTable = new int[][]{
                {2, 8, -2, -8},
                {5, 17, -5, -17},
                {9, 29, -9, -29},
                {13, 42, -13, -42},
                {18, 60, -18, -60},
                {24, 80, -24, -80},
                {33, 106, -33, -106},
                {47, 183, -47, -183}
        };
        
        private int complement3bitshifted(int i)
        {
            return new int[]{0, 8, 16, 24, -32, -24, -16, -8}[i];
        }
    };
    
    
    public void toImage(int level, Path output)
    {
        byte[] data = new byte[0];//UtilHandler.bytebufferToArray(unpackETC(level, false));
        
        ByteWriter bw             = new ByteWriter();
        int        fileHeaderSize = 14;
        int        dataHeaderSize = 40;
        int        headerSize     = fileHeaderSize + dataHeaderSize;
        
        int pixelSize = 3 * header.getPixelWidth() * header.getPixelHeight();
        int totalSize = headerSize + pixelSize;
        
        bw.writeString("BM");
        bw.writeInt(totalSize);
        bw.writeInt(0);
        bw.writeInt(headerSize);
        bw.writeInt(dataHeaderSize);
        bw.writeInt(header.getPixelWidth() >> level);
        bw.writeInt(header.getPixelHeight() >> level);
        bw.writeShort((short) 1);
        bw.writeShort((short) 24);
        bw.writeInt(0);
        bw.writeInt(pixelSize);
        bw.writeInt(0);
        bw.writeInt(0);
        bw.writeInt(0);
        bw.writeInt(0);
        
        for (int i = 0; i < data.length; i += 4)
        {
            byte r = data[i + 0];
            byte g = data[i + 1];
            byte b = data[i + 2];
            byte a = data[i + 3];
            
            bw.writeInt(b);
            bw.writeInt(g);
            bw.writeInt(r);
            bw.writeInt(a);
        }
        
        bw.save(output);
    }
    
}

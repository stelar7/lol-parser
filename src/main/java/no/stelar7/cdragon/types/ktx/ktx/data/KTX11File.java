package no.stelar7.cdragon.types.ktx.ktx.data;

import no.stelar7.cdragon.util.handlers.UtilHandler;
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
    
    private ByteBuffer unpackETC(int level, boolean alpha)
    {
        // assume no alpha
        ByteBuffer input  = ByteBuffer.wrap(mipMaps.getTextureData().get(level));
        ByteBuffer output = ByteBuffer.allocate(this.getHeader().getPixelWidth() * this.header.getPixelHeight());
        
        int width  = this.header.getPixelWidth() >> level;
        int height = this.header.getPixelHeight() >> level;
        
        for (int xImage = 0; xImage < width; xImage += 8)
        {
            for (int yImage = 0; yImage < height; yImage += 8)
            {
                for (int z = 0; z < 4; z++)
                {
                    if (input.remaining() < 8)
                    {
                        return output;
                    }
                    
                    int xStart = (z == 0 || z == 1 ? 0 : 4);
                    int yStart = (z == 0 || z == 2 ? 0 : 4);
                    
                    long read = input.getLong();
                    long swapped = ((read & 0x00000000000000FFL) << 56) |
                                   ((read & 0x000000000000FF00L) << 40) |
                                   ((read & 0x0000000000FF0000L) << 24) |
                                   ((read & 0x00000000FF000000L) << 8) |
                                   ((read & 0x000000FF00000000L) >> 8) |
                                   ((read & 0x0000FF0000000000L) >> 24) |
                                   ((read & 0x00FF000000000000L) >> 40) |
                                   ((read & 0xFF00000000000000L) >> 56);
                    byte[]      data     = ByteBuffer.allocate(8).putLong(swapped).array();
                    ColorQuad[] unpacked = unpackETCBlock(data, alpha);
                    
                    int indexA = 0;
                    int indexB = 0;
                    for (int x = xImage + xStart; x < xImage + xStart + 4; x++)
                    {
                        for (int y = yImage + yStart; y < yImage + yStart + 4; y++)
                        {
                            output.put((x * height) + y, unpacked[indexA].get(indexB));
                            indexB++;
                            if (indexB == 4)
                            {
                                indexA++;
                                indexB = 0;
                            }
                        }
                    }
                }
            }
        }
        
        return output;
    }
    
    public void toImage(int level, Path output)
    {
        byte[] data = UtilHandler.bytebufferToArray(unpackETC(level, false));
        
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
    
    class ColorQuad
    {
        private byte r;
        private byte g;
        private byte b;
        private byte a;
        
        public ColorQuad(int r, int g, int b, int a)
        {
            this.r = (byte) r;
            this.g = (byte) g;
            this.b = (byte) b;
            this.a = (byte) a;
        }
        
        public ColorQuad()
        {
        }
        
        public void setRGB(ColorQuad other)
        {
            this.r = other.r;
            this.g = other.g;
            this.b = other.b;
        }
        
        public byte get(int index)
        {
            switch (index)
            {
                case 0:
                    return r;
                case 1:
                    return g;
                case 2:
                    return b;
                case 3:
                    return a;
                default:
                    throw new RuntimeException("Index out of range");
            }
        }
        
        public void set(int r, int g, int b)
        {
            this.r = (byte) r;
            this.g = (byte) g;
            this.b = (byte) b;
        }
    }
    
    class ETCBlock
    {
        private byte[] data;
        
        public ETCBlock(long input)
        {
            this.data = new byte[8];
            for (int i = 7; i >= 0; i--)
            {
                data[i] = (byte) (input & 0xFF);
                input >>= 8;
            }
        }
        
        public ETCBlock(byte[] input)
        {
            this.data = input;
        }
        
        public boolean getDiffBit()
        {
            return (data[3] & 2) != 0;
        }
        
        public boolean getFlipBit()
        {
            return (data[3] & 1) != 0;
        }
        
        public int getIntenTable(int i)
        {
            int offset = i > 0 ? 2 : 5;
            return (data[3] >> offset) & 7;
        }
        
        public int getSelector(int x, int y)
        {
            int bit_index    = x * 4 + y;
            int byte_bit_ofs = bit_index & 7;
            int index        = 7 - (bit_index >> 3);
            int lsb          = (data[index + 0] >> byte_bit_ofs) & 1;
            int msb          = (data[index - 2] >> byte_bit_ofs) & 1;
            int val          = lsb | (msb << 1);
            
            switch (val)
            {
                case 0:
                    return 2;
                case 1:
                    return 3;
                case 2:
                    return 1;
                case 3:
                    return 0;
                default:
                    throw new RuntimeException("Invalid index");
            }
        }
        
        public short getBase5Color()
        {
            int r = getByteBits(59, 5);
            int g = getByteBits(51, 5);
            int b = getByteBits(43, 5);
            return (short) (b | (g << 5) | (r << 10));
        }
        
        public short getDelta3Color()
        {
            int r = getByteBits(56, 3);
            int g = getByteBits(48, 3);
            int b = getByteBits(40, 3);
            return (short) (b | (g << 3) | (r << 6));
        }
        
        public short getBase4(int i)
        {
            int r;
            int g;
            int b;
            
            if (i > 0)
            {
                r = getByteBits(56, 4);
                g = getByteBits(48, 4);
                b = getByteBits(40, 4);
            } else
            {
                r = getByteBits(60, 4);
                g = getByteBits(52, 4);
                b = getByteBits(44, 4);
            }
            
            return (short) (b | (g << 4) | (r << 8));
        }
        
        public byte getByteBits(int offset, int num)
        {
            int byte_ofs     = 7 - (offset >> 3);
            int byte_bit_ofs = offset & 7;
            return (byte) ((data[byte_ofs] >> byte_bit_ofs) & ((1 << num) - 1));
        }
        
        public void getDiffSubblockColor(ColorQuad[] dest, short base5, int tableIndex)
        {
            int[][] g_etc1_inten_tables =
                    {
                            {-8, -2, 2, 8}, {-17, -5, 5, 17}, {-29, -9, 9, 29}, {-42, -13, 13, 42},
                            {-60, -18, 18, 60}, {-80, -24, 24, 80}, {-106, -33, 33, 106}, {-183, -47, 47, 183}
                    };
            
            
            int[] intenTable = g_etc1_inten_tables[tableIndex];
            int[] rgb        = new int[3];
            
            unpack_color5(rgb, base5, true, 0);
            int ir = rgb[0];
            int ig = rgb[1];
            int ib = rgb[2];
            
            int y0 = intenTable[0];
            dest[0].set(ir + y0, ig + y0, ib + y0);
            
            int y1 = intenTable[1];
            dest[1].set(ir + y1, ig + y1, ib + y1);
            
            int y2 = intenTable[2];
            dest[2].set(ir + y2, ig + y2, ib + y2);
            
            int y3 = intenTable[3];
            dest[3].set(ir + y3, ig + y3, ib + y3);
        }
        
        private void unpack_color5(int[] rgb, short base5, boolean scaled, int alpha)
        {
            int b = base5 & 31;
            int g = (base5 >> 5) & 31;
            int r = (base5 >> 10) & 31;
            
            if (scaled)
            {
                b = (b << 3) | (b >> 2);
                g = (g << 3) | (g >> 2);
                r = (r << 3) | (r >> 2);
            }
            
            rgb[0] = r;
            rgb[1] = g;
            rgb[2] = b;
        }
        
        private void unpack_color5(int[] rgb, short base5, short delta3, boolean scaled, int alpha)
        {
            int[] drgb = new int[3];
            unpack_delta3(drgb, delta3);
            
            int b = (base5 & 31) + drgb[2];
            int g = ((base5 >> 5) & 31) + drgb[1];
            int r = ((base5 >> 10) & 31) + drgb[0];
            
            if ((r | g | b) > 31)
            {
                r = Math.max(0, Math.min(31, r));
                g = Math.max(0, Math.min(31, g));
                b = Math.max(0, Math.min(31, b));
            }
            
            if (scaled)
            {
                b = (b << 3) | (b >> 2);
                g = (g << 3) | (g >> 2);
                r = (r << 3) | (r >> 2);
            }
            
            rgb[0] = r;
            rgb[1] = g;
            rgb[2] = b;
        }
        
        
        private void unpack_color4(int[] rgb, short packed4, boolean scaled, int alpha)
        {
            int b = packed4 & 15;
            int g = (packed4 >> 4) & 15;
            int r = (packed4 >> 8) & 15;
            
            if (scaled)
            {
                b = (b << 4) | b;
                g = (g << 4) | g;
                r = (r << 4) | r;
            }
            
            rgb[0] = r;
            rgb[1] = g;
            rgb[2] = b;
        }
        
        private void unpack_delta3(int[] drgb, short delta3)
        {
            int r = (drgb[0] >> 6) & 7;
            int g = (drgb[1] >> 3) & 7;
            int b = drgb[2] & 7;
            
            if (r >= 4)
            {
                r -= 8;
            }
            
            if (g >= 4)
            {
                g -= 8;
            }
            
            if (b >= 4)
            {
                b -= 8;
            }
            
            drgb[0] = r;
            drgb[1] = g;
            drgb[2] = b;
        }
        
        public void getDiffSubblockColor(ColorQuad[] dest, short base5, short delta3, int tableIndex)
        {
            int[][] g_etc1_inten_tables =
                    {
                            {-8, -2, 2, 8}, {-17, -5, 5, 17}, {-29, -9, 9, 29}, {-42, -13, 13, 42},
                            {-60, -18, 18, 60}, {-80, -24, 24, 80}, {-106, -33, 33, 106}, {-183, -47, 47, 183}
                    };
            
            int[] intenTable = g_etc1_inten_tables[tableIndex];
            int[] rgb        = new int[3];
            unpack_color5(rgb, base5, delta3, true, 0);
            int ir = rgb[0];
            int ig = rgb[1];
            int ib = rgb[2];
            
            int y0 = intenTable[0];
            dest[0].set(ir + y0, ig + y0, ib + y0);
            
            int y1 = intenTable[1];
            dest[1].set(ir + y1, ig + y1, ib + y1);
            
            int y2 = intenTable[2];
            dest[2].set(ir + y2, ig + y2, ib + y2);
            
            int y3 = intenTable[3];
            dest[3].set(ir + y3, ig + y3, ib + y3);
        }
        
        public void getAbsSubblockColor(ColorQuad[] dest, short packed4, int tableIndex)
        {
            int[][] g_etc1_inten_tables =
                    {
                            {-8, -2, 2, 8}, {-17, -5, 5, 17}, {-29, -9, 9, 29}, {-42, -13, 13, 42},
                            {-60, -18, 18, 60}, {-80, -24, 24, 80}, {-106, -33, 33, 106}, {-183, -47, 47, 183}
                    };
            
            int[] intenTable = g_etc1_inten_tables[tableIndex];
            int[] rgb        = new int[3];
            unpack_color4(rgb, packed4, true, 0);
            
            int ir = rgb[0];
            int ig = rgb[1];
            int ib = rgb[2];
            
            int y0 = intenTable[0];
            dest[0].set(ir + y0, ig + y0, ib + y0);
            
            int y1 = intenTable[1];
            dest[1].set(ir + y1, ig + y1, ib + y1);
            
            int y2 = intenTable[2];
            dest[2].set(ir + y2, ig + y2, ib + y2);
            
            int y3 = intenTable[3];
            dest[3].set(ir + y3, ig + y3, ib + y3);
        }
    }
    
    ColorQuad[] unpackETCBlock(byte[] input, boolean alpha)
    {
        ColorQuad[] pDst = new ColorQuad[4 * 4];
        
        ETCBlock block = new ETCBlock(input);
        
        boolean diff_flag    = block.getDiffBit();
        boolean flip_flag    = block.getFlipBit();
        int     table_index0 = block.getIntenTable(0);
        int     table_index1 = block.getIntenTable(1);
        
        ColorQuad[] subblock_colors0 = new ColorQuad[4];
        ColorQuad[] subblock_colors1 = new ColorQuad[4];
        Arrays.fill(subblock_colors0, new ColorQuad());
        Arrays.fill(subblock_colors1, new ColorQuad());
        
        if (diff_flag)
        {
            short base5  = block.getBase5Color();
            short delta3 = block.getDelta3Color();
            block.getDiffSubblockColor(subblock_colors0, base5, table_index0);
            block.getDiffSubblockColor(subblock_colors1, base5, delta3, table_index1);
        } else
        {
            short base40 = block.getBase4(0);
            short base41 = block.getBase4(1);
            block.getAbsSubblockColor(subblock_colors0, base40, table_index0);
            block.getAbsSubblockColor(subblock_colors1, base41, table_index1);
        }
        
        int index = 0;
        if (alpha)
        {
            if (flip_flag)
            {
                for (int y = 0; y < 2; y++)
                {
                    pDst[index + 0].setRGB(subblock_colors0[block.getSelector(0, y)]);
                    pDst[index + 1].setRGB(subblock_colors0[block.getSelector(1, y)]);
                    pDst[index + 2].setRGB(subblock_colors0[block.getSelector(2, y)]);
                    pDst[index + 3].setRGB(subblock_colors0[block.getSelector(3, y)]);
                    index += 4;
                }
                
                for (int y = 2; y < 4; y++)
                {
                    pDst[index + 0].setRGB(subblock_colors1[block.getSelector(0, y)]);
                    pDst[index + 1].setRGB(subblock_colors1[block.getSelector(1, y)]);
                    pDst[index + 2].setRGB(subblock_colors1[block.getSelector(2, y)]);
                    pDst[index + 3].setRGB(subblock_colors1[block.getSelector(3, y)]);
                    index += 4;
                }
            } else
            {
                for (int y = 0; y < 4; y++)
                {
                    pDst[index + 0].setRGB(subblock_colors0[block.getSelector(0, y)]);
                    pDst[index + 1].setRGB(subblock_colors0[block.getSelector(1, y)]);
                    pDst[index + 2].setRGB(subblock_colors1[block.getSelector(2, y)]);
                    pDst[index + 3].setRGB(subblock_colors1[block.getSelector(3, y)]);
                    index += 4;
                }
            }
        } else
        {
            if (flip_flag)
            {
                // 0000
                // 0000
                // 1111
                // 1111
                for (int y = 0; y < 2; y++)
                {
                    pDst[index + 0] = subblock_colors0[block.getSelector(0, y)];
                    pDst[index + 1] = subblock_colors0[block.getSelector(1, y)];
                    pDst[index + 2] = subblock_colors0[block.getSelector(2, y)];
                    pDst[index + 3] = subblock_colors0[block.getSelector(3, y)];
                    index += 4;
                }
                
                for (int y = 2; y < 4; y++)
                {
                    pDst[index + 0] = subblock_colors1[block.getSelector(0, y)];
                    pDst[index + 1] = subblock_colors1[block.getSelector(1, y)];
                    pDst[index + 2] = subblock_colors1[block.getSelector(2, y)];
                    pDst[index + 3] = subblock_colors1[block.getSelector(3, y)];
                    index += 4;
                }
            } else
            {
                // 0011
                // 0011
                // 0011
                // 0011
                for (int y = 0; y < 4; y++)
                {
                    pDst[index + 0] = subblock_colors0[block.getSelector(0, y)];
                    pDst[index + 1] = subblock_colors0[block.getSelector(1, y)];
                    pDst[index + 2] = subblock_colors1[block.getSelector(2, y)];
                    pDst[index + 3] = subblock_colors1[block.getSelector(3, y)];
                    index += 4;
                }
            }
        }
        
        
        return pDst;
    }
    
}

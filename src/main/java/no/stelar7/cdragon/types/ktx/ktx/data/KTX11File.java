package no.stelar7.cdragon.types.ktx.ktx.data;

import java.nio.ByteBuffer;
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
    
    public void unpackETC(int level, boolean alpha)
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
                    byte[] data     = ByteBuffer.allocate(8).putLong(swapped).array();
                    byte[] unpacked = unpackETCBlock(data, alpha);
                    
                    for (int xPos = 0, x = xImage + xStart; x < xImage + xStart + 4; x++, xPos++)
                    {
                        for (int yPos = 0, y = yImage + yStart; y < yImage + yStart + 4; y++, yPos++)
                        {
                            output.put((x * height) + y, unpacked[(xPos * 4) + yPos]);
                        }
                    }
                }
            }
        }
    }
    
    // TODO
    byte[] unpackETCBlock(byte[] block, boolean alpha)
    {
        byte[]  pDst         = new byte[16];
        boolean diff_flag    = (block[3] & 2) != 0;
        boolean flip_flag    = (block[3] & 1) != 0;
        int     table_index0 = (block[3] >> 5) & 7;
        int     table_index1 = (block[3] >> 2) & 7;
     
        return block;
        /*
        
        // 4xRGBA
        byte[][] subblock_colors0 = new byte[4][4];
        byte[][] subblock_colors1 = new byte[4][4];
        
        if (diff_flag)
        {
             const uint16 base_color5 = block.get_base5_color();
            const uint16 delta_color3 = block.get_delta3_color();
            etc1_block::get_diff_subblock_colors (subblock_colors0, base_color5, table_index0);
            
            if (!etc1_block::get_diff_subblock_colors (subblock_colors1, base_color5, delta_color3, table_index1))
            success = false;
        } else
        {
         const uint16 base_color4_0 = block.get_base4_color(0);
            etc1_block::get_abs_subblock_colors (subblock_colors0, base_color4_0, table_index0);

         const uint16 base_color4_1 = block.get_base4_color(1);
            etc1_block::get_abs_subblock_colors (subblock_colors1, base_color4_1, table_index1);
        }
        
        if (preserve_alpha)
        {
            if (flip_flag)
            {
                for (uint y = 0; y < 2; y++)
                {
                    pDst[0].set_rgb(subblock_colors0[block.get_selector(0, y)]);
                    pDst[1].set_rgb(subblock_colors0[block.get_selector(1, y)]);
                    pDst[2].set_rgb(subblock_colors0[block.get_selector(2, y)]);
                    pDst[3].set_rgb(subblock_colors0[block.get_selector(3, y)]);
                    pDst += 4;
                }
                
                for (uint y = 2; y < 4; y++)
                {
                    pDst[0].set_rgb(subblock_colors1[block.get_selector(0, y)]);
                    pDst[1].set_rgb(subblock_colors1[block.get_selector(1, y)]);
                    pDst[2].set_rgb(subblock_colors1[block.get_selector(2, y)]);
                    pDst[3].set_rgb(subblock_colors1[block.get_selector(3, y)]);
                    pDst += 4;
                }
            } else
            {
                for (uint y = 0; y < 4; y++)
                {
                    pDst[0].set_rgb(subblock_colors0[block.get_selector(0, y)]);
                    pDst[1].set_rgb(subblock_colors0[block.get_selector(1, y)]);
                    pDst[2].set_rgb(subblock_colors1[block.get_selector(2, y)]);
                    pDst[3].set_rgb(subblock_colors1[block.get_selector(3, y)]);
                    pDst += 4;
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
                for (uint y = 0; y < 2; y++)
                {
                    pDst[0] = subblock_colors0[block.get_selector(0, y)];
                    pDst[1] = subblock_colors0[block.get_selector(1, y)];
                    pDst[2] = subblock_colors0[block.get_selector(2, y)];
                    pDst[3] = subblock_colors0[block.get_selector(3, y)];
                    pDst += 4;
                }
                
                for (uint y = 2; y < 4; y++)
                {
                    pDst[0] = subblock_colors1[block.get_selector(0, y)];
                    pDst[1] = subblock_colors1[block.get_selector(1, y)];
                    pDst[2] = subblock_colors1[block.get_selector(2, y)];
                    pDst[3] = subblock_colors1[block.get_selector(3, y)];
                    pDst += 4;
                }
            } else
            {
                // 0011
                // 0011
                // 0011
                // 0011
                for (uint y = 0; y < 4; y++)
                {
                    pDst[0] = subblock_colors0[block.get_selector(0, y)];
                    pDst[1] = subblock_colors0[block.get_selector(1, y)];
                    pDst[2] = subblock_colors1[block.get_selector(2, y)];
                    pDst[3] = subblock_colors1[block.get_selector(3, y)];
                    pDst += 4;
                }
            }
        }
        
        return pDst;
         */
    }
    
}

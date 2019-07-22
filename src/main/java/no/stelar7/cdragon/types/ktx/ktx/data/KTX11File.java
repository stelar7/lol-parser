package no.stelar7.cdragon.types.ktx.ktx.data;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.writers.ByteWriter;

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
    
    
    public ByteBuffer decodeETC1(int level)
    {
        byte[]     textureData = this.getMipMaps().getTextureData().get(level).getData();
        ByteBuffer input       = ByteBuffer.wrap(textureData);
        
        int pixelWidth  = this.getHeader().getPixelWidth() >> level;
        int pixelHeight = this.getHeader().getPixelHeight() >> level;
        
        int        align  = 0;
        int        stride = ((pixelWidth * 3) + align) & ~align;
        int        size   = stride * pixelHeight;
        ByteBuffer output = ByteBuffer.allocate(size);
        
        int    pixelSize = 3;
        byte[] block     = new byte[48];
        
        int encodedWidth  = (pixelWidth + 3) & ~3;
        int encodedHeight = (pixelHeight + 3) & ~3;
        
        
        for (int y = 0; y < encodedHeight; y += 4)
        {
            int yOff = pixelHeight - y;
            int yEnd = yOff;
            if (yOff > 4)
            {
                yEnd = 4;
            }
            
            for (int x = 0; x < encodedWidth; x += 4)
            {
                int xOff = pixelWidth - x;
                int xEnd = xOff;
                if (xOff > 4)
                {
                    xEnd = 4;
                }
                
                byte[] inVal = new byte[8];
                input.get(inVal);
                decodeETC1Block(inVal, block);
                
                for (int cy = 0; cy < yEnd; cy++)
                {
                    int readBufferPos  = (cy * 4) * 3;
                    int writeBufferPos = pixelSize * x + stride * (y + cy);
                    
                    if (pixelSize == 3)
                    {
                        System.arraycopy(block, readBufferPos, output.array(), writeBufferPos, xEnd * 3);
                    } else
                    
                    {
                        for (int cx = 0; cx < xEnd; cx++)
                        {
                            byte r     = block[readBufferPos++];
                            byte g     = block[readBufferPos++];
                            byte b     = block[readBufferPos++];
                            int  pixel = ((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3);
                            
                            output.put(writeBufferPos++, (byte) pixel);
                            output.put(writeBufferPos++, (byte) ((byte) pixel >> 8));
                        }
                    }
                }
            }
        }
        return output;
    }
    
    private void decodeETC1Block(byte[] input, byte[] block)
    {
        int low  = (input[0] << 24) | (input[1] << 16) | (input[2] << 8) | input[3];
        int high = (input[4] << 24) | (input[5] << 16) | (input[6] << 8) | input[7];
        
        int r1;
        int r2;
        int g1;
        int g2;
        int b1;
        int b2;
        
        if ((high & 2) > 0)
        {
            // diff mode
            int rBase = high >> 27;
            int gBase = high >> 19;
            int bBase = high >> 11;
            
            r1 = convert5To8(rBase);
            r2 = convertDiff(rBase, high >> 24);
            g1 = convert5To8(gBase);
            g2 = convertDiff(gBase, high >> 16);
            b1 = convert5To8(bBase);
            b2 = convertDiff(bBase, high >> 8);
            
        } else
        {
            r1 = convert4To8(high >> 28);
            r2 = convert4To8(high >> 24);
            g1 = convert4To8(high >> 20);
            g2 = convert4To8(high >> 16);
            b1 = convert4To8(high >> 12);
            b2 = convert4To8(high >> 8);
        }
        
        int     tableIndexA = (7 & (high >> 5)) * 4;
        int     tableIndexB = (7 & (high >> 2)) * 4;
        boolean flipped     = (high & 1) != 0;
        
        decodeETC1SubBlock(block, r1, g1, b1, tableIndexA, low, false, flipped);
        decodeETC1SubBlock(block, r2, g2, b2, tableIndexB, low, true, flipped);
        
    }
    
    private void decodeETC1SubBlock(byte[] output, int r, int g, int b, int tableIndex, int low, boolean second, boolean flipped)
    {
        int baseX = (second && !flipped) ? 2 : 0;
        int baseY = (second && flipped) ? 2 : 0;
        
        for (int i = 0; i < 8; i++)
        {
            int x = baseX + (flipped ? (i >> 1) : (i >> 2));
            int y = baseY + (flipped ? (i & 1) : (i & 3));
            
            int k      = y + (x * 4);
            int offset = ((low >> k) & 1) | ((low >> (k + 15)) & 2);
            int delta  = modifierTable[tableIndex + offset];
            
            int writeOffset = 3 * (x + 4 * y);
            output[writeOffset++] = clamp(r + delta);
            output[writeOffset++] = clamp(g + delta);
            output[writeOffset++] = clamp(b + delta);
        }
    }
    
    private int[] modifierTable =
            {
                    2, 8, -2, -8,
                    5, 17, -5, -17,
                    9, 29, -9, -29,
                    13, 42, -13, -42,
                    18, 60, -18, -60,
                    24, 80, -24, -80,
                    33, 106, -33, -106,
                    47, 183, -47, -183
            };
    
    private int[] lookupTable =
            {
                    0, 1, 2, 3, -4, -3, -2, -1
            };
    
    private byte clamp(int x)
    {
        return (byte) (x >= 0 ? (x < 255 ? x : 255) : 0);
    }
    
    
    private int convert4To8(int b)
    {
        int c = b & 0xf;
        return (c << 4) | c;
    }
    
    private int convert5To8(int b)
    {
        int c = b & 0x1f;
        return (c << 3) | (c >> 2);
    }
    
    private int convert6To8(int b)
    {
        int c = b & 0x3f;
        return (c << 2) | (c >> 4);
    }
    
    private int divideBy255(int d)
    {
        return (d + 128 + (d >> 8)) >> 8;
    }
    
    private int convert8To4(int b)
    {
        //int c = b & 0xff;
        return divideBy255(b * 15);
    }
    
    private int convert8To5(int b)
    {
        //int c = b & 0xff;
        return divideBy255(b * 31);
    }
    
    private int convertDiff(int base, int diff)
    {
        return convert5To8((0x1f & base) + lookupTable[0x7 & diff]);
    }
    
    public void toImage(int level, Path output)
    {
        if (this.header.getTextureFormat() != TextureFormat.TEXTURE_FORMAT_ETC1)
        {
            throw new RuntimeException("Unable to transform from other types than ETC!, found: " + this.header.getTextureFormat());
        }
        
        byte[] data = UtilHandler.bytebufferToArray(decodeETC1(level));
        
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
        
        for (int i = 0; i < data.length; i += 3)
        {
            byte r = data[i + 0];
            byte g = data[i + 1];
            byte b = data[i + 2];
            
            bw.writeByte(r);
            bw.writeByte(g);
            bw.writeByte(b);
        }
        
        bw.save(output);
    }
}

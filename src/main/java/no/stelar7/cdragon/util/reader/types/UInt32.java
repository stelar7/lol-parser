package no.stelar7.cdragon.util.reader.types;

import lombok.Data;

@Data
public class UInt32
{
    private int low;
    private int high;
    
    public UInt32(int value)
    {
        this.high = value >>> 16;
        this.low = value & 0xFFFF;
    }
    
    public UInt32(int lowBits, int highBits)
    {
        this.high = highBits;
        this.low = lowBits;
    }
    
    public long toLong()
    {
        return (high * 65536L) + low;
    }
    
    public int toInt()
    {
        return (high * 65536) + low;
    }
    
    public UInt32 xor(UInt32 other)
    {
        return new UInt32(this.low ^ other.low, this.high ^ other.high);
    }
    
    public UInt32 multiply(UInt32 other)
    {
        int a16 = this.high;
        int a00 = this.low;
        int b16 = other.high;
        int b00 = other.low;
        int c00 = a00 * b00;
        
        int c16 = c00 >>> 16;
        c16 += a16 * b00;
        c16 &= 0xFFFF;
        c16 += a00 * b16;
        
        return new UInt32(c00 & 0xFFF, c16 & 0xFFFF);
    }
    
}

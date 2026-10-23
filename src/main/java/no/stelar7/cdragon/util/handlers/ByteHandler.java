package no.stelar7.cdragon.util.handlers;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Byte, bit and buffer helpers
 */
public final class ByteHandler
{
    private ByteHandler()
    {
        // Hide public constructor
    }
    
    public static void reverse(byte[] data)
    {
        for (int i = 0; i < data.length / 2; i++)
        {
            byte temp = data[i];
            data[i] = data[data.length - i - 1];
            data[data.length - i - 1] = temp;
        }
    }
    
    public static BitSet longToBitSet(long value)
    {
        BitSet bits  = new BitSet();
        int    index = 0;
        while (value != 0L)
        {
            if (value % 2L != 0)
            {
                bits.set(index);
            }
            ++index;
            value = value >>> 1;
        }
        return bits;
    }
    
    public static long bitsetToLong(BitSet bits)
    {
        long value = 0L;
        for (int i = 0; i < bits.length(); ++i)
        {
            value += bits.get(i) ? (1L << i) : 0L;
        }
        return value;
    }
    
    public static int getLongFromIP(String ipAddress)
    {
        long     result           = 0;
        String[] ipAddressInArray = ipAddress.split("\\.");
        
        for (int i = 3; i >= 0; i--)
        {
            long ip = Long.parseLong(ipAddressInArray[3 - i]);
            
            //left shifting 24,16,8,0 and bitwise OR
            //1. 192 << 24
            //1. 168 << 16
            //1. 1   << 8
            //1. 2   << 0
            result |= ip << (i * 8);
        }
        
        return (int) result;
    }
    
    public static String getIPFromLong(long ip)
    {
        return String.format("%d.%d.%d.%d", (ip >> 24) & 0xFF, (ip >> 16) & 0xFF, (ip >> 8) & 0xFF, ip & 0xFF);
    }
    
    
    public static byte[] bytebufferToArray(ByteBuffer buffer)
    {
        int oldPos = buffer.position();
        
        byte[] data = new byte[buffer.limit()];
        buffer.position(0);
        buffer.get(data);
        
        buffer.position(oldPos);
        return data;
    }
    
    public static void printBuffer(ByteBuffer data)
    {
        StringBuilder result = new StringBuilder();
        
        data.mark();
        
        while (data.remaining() > 0)
        {
            String hex = Integer.toHexString(Byte.toUnsignedInt(data.get())).toUpperCase(Locale.ENGLISH);
            if (hex.length() != 2)
            {
                hex = "0" + hex;
            }
            result.append(hex).append(" ");
        }
        
        data.reset();
        
        result.reverse().deleteCharAt(0).reverse();
        System.out.println(result);
    }
    
    public static String bufferToString(ByteBuffer data)
    {
        StringBuilder result = new StringBuilder("(");
        
        data.mark();
        
        while (data.remaining() > 0)
        {
            result.append(data.get()).append(", ");
        }
        
        result.reverse().deleteCharAt(0).deleteCharAt(0).reverse().append(")");
        data.reset();
        
        return result.toString();
    }
    
    public static byte[] byteArrayFromString(String data)
    {
        int    length     = data.length() / 4;
        byte[] returnData = new byte[length];
        
        for (int i = 0; i < length; i++)
        {
            String hex   = data.substring(i * 4, (i * 4) + 4);
            byte   value = Integer.decode(hex).byteValue();
            returnData[i] = value;
        }
        return returnData;
    }
    
    public static boolean isBitSet(int number, int bit)
    {
        return (number & (1 << bit)) > 0;
    }
    
    public static byte[] byteArrayFromSetBits(int number)
    {
        byte[]  returnData = new byte[31];
        boolean found      = false;
        int     i          = 0;
        for (int j = 31; j >= 0; j--)
        {
            boolean bitValue = isBitSet(number, j);
            found = found || bitValue;
            if (found)
            {
                returnData[i++] = (byte) (bitValue ? 1 : 0);
            }
        }
        
        byte[] realData = new byte[i];
        System.arraycopy(returnData, 0, realData, 0, i);
        
        return realData;
    }
}

package no.stelar7.cdragon.types.ktx.ktx2.data;

import java.util.Objects;

public class KTX2FileDataFormatDescriptorBlockSample
{
    private short bitOffset;
    private byte  bitLength;
    private byte  channelType;
    private byte  samplePosition0;
    private byte  samplePosition1;
    private byte  samplePosition2;
    private byte  samplePosition3;
    private int   sampleLower;
    private int   sampleUpper;
    
    public short getBitOffset()
    {
        return bitOffset;
    }
    
    public void setBitOffset(short bitOffset)
    {
        this.bitOffset = bitOffset;
    }
    
    public byte getBitLength()
    {
        return bitLength;
    }
    
    public void setBitLength(byte bitLength)
    {
        this.bitLength = bitLength;
    }
    
    public byte getChannelType()
    {
        return channelType;
    }
    
    public void setChannelType(byte channelType)
    {
        this.channelType = channelType;
    }
    
    public byte getSamplePosition0()
    {
        return samplePosition0;
    }
    
    public void setSamplePosition0(byte samplePosition0)
    {
        this.samplePosition0 = samplePosition0;
    }
    
    public byte getSamplePosition1()
    {
        return samplePosition1;
    }
    
    public void setSamplePosition1(byte samplePosition1)
    {
        this.samplePosition1 = samplePosition1;
    }
    
    public byte getSamplePosition2()
    {
        return samplePosition2;
    }
    
    public void setSamplePosition2(byte samplePosition2)
    {
        this.samplePosition2 = samplePosition2;
    }
    
    public byte getSamplePosition3()
    {
        return samplePosition3;
    }
    
    public void setSamplePosition3(byte samplePosition3)
    {
        this.samplePosition3 = samplePosition3;
    }
    
    public int getSampleLower()
    {
        return sampleLower;
    }
    
    public void setSampleLower(int sampleLower)
    {
        this.sampleLower = sampleLower;
    }
    
    public int getSampleUpper()
    {
        return sampleUpper;
    }
    
    public void setSampleUpper(int sampleUpper)
    {
        this.sampleUpper = sampleUpper;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        KTX2FileDataFormatDescriptorBlockSample that = (KTX2FileDataFormatDescriptorBlockSample) o;
        return bitOffset == that.bitOffset &&
               bitLength == that.bitLength &&
               channelType == that.channelType &&
               samplePosition0 == that.samplePosition0 &&
               samplePosition1 == that.samplePosition1 &&
               samplePosition2 == that.samplePosition2 &&
               samplePosition3 == that.samplePosition3 &&
               sampleLower == that.sampleLower &&
               sampleUpper == that.sampleUpper;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(bitOffset, bitLength, channelType, samplePosition0, samplePosition1, samplePosition2, samplePosition3, sampleLower, sampleUpper);
    }
    
    @Override
    public String toString()
    {
        return "KTX2FileDataFormatDescriptorBlockSample{" +
               "bitOffset=" + bitOffset +
               ", bitLength=" + bitLength +
               ", channelType=" + channelType +
               ", samplePosition0=" + samplePosition0 +
               ", samplePosition1=" + samplePosition1 +
               ", samplePosition2=" + samplePosition2 +
               ", samplePosition3=" + samplePosition3 +
               ", sampleLower=" + sampleLower +
               ", sampleUpper=" + sampleUpper +
               '}';
    }
}

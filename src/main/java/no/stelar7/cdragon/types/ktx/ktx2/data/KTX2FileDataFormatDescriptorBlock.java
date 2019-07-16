package no.stelar7.cdragon.types.ktx.ktx2.data;

import java.util.*;

public class KTX2FileDataFormatDescriptorBlock
{
    
    private short                                        vendorId;
    private short                                        descriptorType;
    private short                                        versionNumber;
    private short                                        descriptorBlockSize;
    private byte                                         colorModel;
    private byte                                         colorPrimaries;
    private byte                                         transferFunction;
    private byte                                         flags;
    private byte                                         texelBlockDimension0;
    private byte                                         texelBlockDimension1;
    private byte                                         texelBlockDimension2;
    private byte                                         texelBlockDimension3;
    private byte                                         bytesPlane0;
    private byte                                          bytesPlane1;
    private byte                                          bytesPlane2;
    private byte                                          bytesPlane3;
    private byte                                          bytesPlane4;
    private byte                                          bytesPlane5;
    private byte                                          bytesPlane6;
    private byte                                          bytesPlane7;
    private List<KTX2FileDataFormatDescriptorBlockSample> samples;
    
    public short getVendorId()
    {
        return vendorId;
    }
    
    public void setVendorId(short vendorId)
    {
        this.vendorId = vendorId;
    }
    
    public short getDescriptorType()
    {
        return descriptorType;
    }
    
    public void setDescriptorType(short descriptorType)
    {
        this.descriptorType = descriptorType;
    }
    
    public short getVersionNumber()
    {
        return versionNumber;
    }
    
    public void setVersionNumber(short versionNumber)
    {
        this.versionNumber = versionNumber;
    }
    
    public short getDescriptorBlockSize()
    {
        return descriptorBlockSize;
    }
    
    public void setDescriptorBlockSize(short descriptorBlockSize)
    {
        this.descriptorBlockSize = descriptorBlockSize;
    }
    
    public byte getColorModel()
    {
        return colorModel;
    }
    
    public void setColorModel(byte colorModel)
    {
        this.colorModel = colorModel;
    }
    
    public byte getColorPrimaries()
    {
        return colorPrimaries;
    }
    
    public void setColorPrimaries(byte colorPrimaries)
    {
        this.colorPrimaries = colorPrimaries;
    }
    
    public byte getTransferFunction()
    {
        return transferFunction;
    }
    
    public void setTransferFunction(byte transferFunction)
    {
        this.transferFunction = transferFunction;
    }
    
    public byte getFlags()
    {
        return flags;
    }
    
    public void setFlags(byte flags)
    {
        this.flags = flags;
    }
    
    public byte getTexelBlockDimension0()
    {
        return texelBlockDimension0;
    }
    
    public void setTexelBlockDimension0(byte texelBlockDimension0)
    {
        this.texelBlockDimension0 = texelBlockDimension0;
    }
    
    public byte getTexelBlockDimension1()
    {
        return texelBlockDimension1;
    }
    
    public void setTexelBlockDimension1(byte texelBlockDimension1)
    {
        this.texelBlockDimension1 = texelBlockDimension1;
    }
    
    public byte getTexelBlockDimension2()
    {
        return texelBlockDimension2;
    }
    
    public void setTexelBlockDimension2(byte texelBlockDimension2)
    {
        this.texelBlockDimension2 = texelBlockDimension2;
    }
    
    public byte getTexelBlockDimension3()
    {
        return texelBlockDimension3;
    }
    
    public void setTexelBlockDimension3(byte texelBlockDimension3)
    {
        this.texelBlockDimension3 = texelBlockDimension3;
    }
    
    public byte getBytesPlane0()
    {
        return bytesPlane0;
    }
    
    public void setBytesPlane0(byte bytesPlane0)
    {
        this.bytesPlane0 = bytesPlane0;
    }
    
    public byte getBytesPlane1()
    {
        return bytesPlane1;
    }
    
    public void setBytesPlane1(byte bytesPlane1)
    {
        this.bytesPlane1 = bytesPlane1;
    }
    
    public byte getBytesPlane2()
    {
        return bytesPlane2;
    }
    
    public void setBytesPlane2(byte bytesPlane2)
    {
        this.bytesPlane2 = bytesPlane2;
    }
    
    public byte getBytesPlane3()
    {
        return bytesPlane3;
    }
    
    public void setBytesPlane3(byte bytesPlane3)
    {
        this.bytesPlane3 = bytesPlane3;
    }
    
    public byte getBytesPlane4()
    {
        return bytesPlane4;
    }
    
    public void setBytesPlane4(byte bytesPlane4)
    {
        this.bytesPlane4 = bytesPlane4;
    }
    
    public byte getBytesPlane5()
    {
        return bytesPlane5;
    }
    
    public void setBytesPlane5(byte bytesPlane5)
    {
        this.bytesPlane5 = bytesPlane5;
    }
    
    public byte getBytesPlane6()
    {
        return bytesPlane6;
    }
    
    public void setBytesPlane6(byte bytesPlane6)
    {
        this.bytesPlane6 = bytesPlane6;
    }
    
    public byte getBytesPlane7()
    {
        return bytesPlane7;
    }
    
    public void setBytesPlane7(byte bytesPlane7)
    {
        this.bytesPlane7 = bytesPlane7;
    }
    
    public List<KTX2FileDataFormatDescriptorBlockSample> getSamples()
    {
        return samples;
    }
    
    public void setSamples(List<KTX2FileDataFormatDescriptorBlockSample> samples)
    {
        this.samples = samples;
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
        KTX2FileDataFormatDescriptorBlock that = (KTX2FileDataFormatDescriptorBlock) o;
        return vendorId == that.vendorId &&
               descriptorType == that.descriptorType &&
               versionNumber == that.versionNumber &&
               descriptorBlockSize == that.descriptorBlockSize &&
               colorModel == that.colorModel &&
               colorPrimaries == that.colorPrimaries &&
               transferFunction == that.transferFunction &&
               flags == that.flags &&
               texelBlockDimension0 == that.texelBlockDimension0 &&
               texelBlockDimension1 == that.texelBlockDimension1 &&
               texelBlockDimension2 == that.texelBlockDimension2 &&
               texelBlockDimension3 == that.texelBlockDimension3 &&
               bytesPlane0 == that.bytesPlane0 &&
               bytesPlane1 == that.bytesPlane1 &&
               bytesPlane2 == that.bytesPlane2 &&
               bytesPlane3 == that.bytesPlane3 &&
               bytesPlane4 == that.bytesPlane4 &&
               bytesPlane5 == that.bytesPlane5 &&
               bytesPlane6 == that.bytesPlane6 &&
               bytesPlane7 == that.bytesPlane7 &&
               Objects.equals(samples, that.samples);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(vendorId, descriptorType, versionNumber, descriptorBlockSize, colorModel, colorPrimaries, transferFunction, flags, texelBlockDimension0, texelBlockDimension1, texelBlockDimension2, texelBlockDimension3, bytesPlane0, bytesPlane1, bytesPlane2, bytesPlane3, bytesPlane4, bytesPlane5, bytesPlane6, bytesPlane7, samples);
    }
    
    @Override
    public String toString()
    {
        return "KTX2FileDataFormatDescriptorBlock{" +
               "vendorId=" + vendorId +
               ", descriptorType=" + descriptorType +
               ", versionNumber=" + versionNumber +
               ", descriptorBlockSize=" + descriptorBlockSize +
               ", colorModel=" + colorModel +
               ", colorPrimaries=" + colorPrimaries +
               ", transferFunction=" + transferFunction +
               ", flags=" + flags +
               ", texelBlockDimension0=" + texelBlockDimension0 +
               ", texelBlockDimension1=" + texelBlockDimension1 +
               ", texelBlockDimension2=" + texelBlockDimension2 +
               ", texelBlockDimension3=" + texelBlockDimension3 +
               ", bytesPlane0=" + bytesPlane0 +
               ", bytesPlane1=" + bytesPlane1 +
               ", bytesPlane2=" + bytesPlane2 +
               ", bytesPlane3=" + bytesPlane3 +
               ", bytesPlane4=" + bytesPlane4 +
               ", bytesPlane5=" + bytesPlane5 +
               ", bytesPlane6=" + bytesPlane6 +
               ", bytesPlane7=" + bytesPlane7 +
               ", samples=" + samples +
               '}';
    }
    
    public void addSample(KTX2FileDataFormatDescriptorBlockSample sample)
    {
        samples.add(sample);
    }
}

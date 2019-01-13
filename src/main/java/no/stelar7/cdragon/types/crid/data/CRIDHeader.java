package no.stelar7.cdragon.types.crid.data;

public class CRIDHeader
{
    private String magic;
    private int    blockSize;
    private short  headerSize;
    private short  footerSize;
    private int    payloadSize;
    private int    blockType;
    private int    granule;
    private int    samples;
    private int    unk;
    private int    unk2;
    
    public String getMagic()
    {
        return magic;
    }
    
    public void setMagic(String magic)
    {
        this.magic = magic;
    }
    
    public int getBlockSize()
    {
        return blockSize;
    }
    
    public void setBlockSize(int blockSize)
    {
        this.blockSize = blockSize;
    }
    
    public short getHeaderSize()
    {
        return headerSize;
    }
    
    public void setHeaderSize(short headerSize)
    {
        this.headerSize = headerSize;
    }
    
    public short getFooterSize()
    {
        return footerSize;
    }
    
    public void setFooterSize(short footerSize)
    {
        this.footerSize = footerSize;
    }
    
    public int getPayloadSize()
    {
        return payloadSize;
    }
    
    public void setPayloadSize(int payloadSize)
    {
        this.payloadSize = payloadSize;
    }
    
    public int getBlockType()
    {
        return blockType;
    }
    
    public void setBlockType(int blockType)
    {
        this.blockType = blockType;
    }
    
    public int getGranule()
    {
        return granule;
    }
    
    public void setGranule(int granule)
    {
        this.granule = granule;
    }
    
    public int getSamples()
    {
        return samples;
    }
    
    public void setSamples(int samples)
    {
        this.samples = samples;
    }
    
    public int getUnk()
    {
        return unk;
    }
    
    public void setUnk(int unk)
    {
        this.unk = unk;
    }
    
    public int getUnk2()
    {
        return unk2;
    }
    
    public void setUnk2(int unk2)
    {
        this.unk2 = unk2;
    }
    
    @Override
    public String toString()
    {
        return "CRIDHeader{" +
               "magic='" + magic + '\'' +
               ", blockSize=" + blockSize +
               ", headerSize=" + headerSize +
               ", footerSize=" + footerSize +
               ", payloadSize=" + payloadSize +
               ", blockType=" + blockType +
               ", granule=" + granule +
               ", samples=" + samples +
               ", unk=" + unk +
               ", unk2=" + unk2 +
               '}';
    }
}

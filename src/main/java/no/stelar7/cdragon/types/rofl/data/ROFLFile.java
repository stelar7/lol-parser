package no.stelar7.cdragon.types.rofl.data;


import java.util.List;

public class ROFLFile
{
    private ROFLHeader             header;
    private ROFLPayloadHeader      payloadHeader;
    private List<ROFLPayloadEntry> entries;
    
    public ROFLHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(ROFLHeader header)
    {
        this.header = header;
    }
    
    public ROFLPayloadHeader getPayloadHeader()
    {
        return payloadHeader;
    }
    
    public void setPayloadHeader(ROFLPayloadHeader payloadHeader)
    {
        this.payloadHeader = payloadHeader;
    }
    
    public List<ROFLPayloadEntry> getEntries()
    {
        return entries;
    }
    
    public void setEntries(List<ROFLPayloadEntry> entries)
    {
        this.entries = entries;
    }
}

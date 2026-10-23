package no.stelar7.cdragon.types.rofl.data;


import java.util.*;

public class ROFLFile
{
    private ROFLHeader             header;
    private ROFLPayloadHeader      payloadHeader;
    private List<ROFLPayloadEntry> entries;
    private List<ROFL2Chunk>       chunks = new ArrayList<>();
    private byte[]                 signature;
    private String                 payloadError;
    
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

    /**
     * The payload chunks of a ROFL2 file (empty for ROFL v1 files, which use {@link #getEntries()})
     */
    public List<ROFL2Chunk> getChunks()
    {
        return chunks;
    }

    public void setChunks(List<ROFL2Chunk> chunks)
    {
        this.chunks = chunks;
    }

    /**
     * The 256 byte signature between the ROFL2 payload and the metadata (null for ROFL v1, where it is in the header)
     */
    public byte[] getSignature()
    {
        return signature;
    }

    public void setSignature(byte[] signature)
    {
        this.signature = signature;
    }

    /**
     * Null if the whole ROFL2 payload could be split into chunks, else why it stopped ({@link #getChunks()} holds the chunks before that)
     */
    public String getPayloadError()
    {
        return payloadError;
    }

    public void setPayloadError(String payloadError)
    {
        this.payloadError = payloadError;
    }
}

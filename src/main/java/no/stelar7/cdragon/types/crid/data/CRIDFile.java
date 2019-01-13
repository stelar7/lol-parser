package no.stelar7.cdragon.types.crid.data;

import no.stelar7.cdragon.types.crid.data.utf.CRIDUTFTable;
import no.stelar7.cdragon.util.types.Pair;

import java.util.List;

public class CRIDFile
{
    CRIDHeader                 header;
    CRIDUTFTable               metadata;
    List<CRIDStreamInfo>       streams;
    List<Pair<String, byte[]>> streamData;
    
    public List<Pair<String, byte[]>> getStreamData()
    {
        return streamData;
    }
    
    public void setStreamData(List<Pair<String, byte[]>> streamData)
    {
        this.streamData = streamData;
    }
    
    public CRIDHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(CRIDHeader header)
    {
        this.header = header;
    }
    
    public CRIDUTFTable getMetadata()
    {
        return metadata;
    }
    
    public void setMetadata(CRIDUTFTable metadata)
    {
        this.metadata = metadata;
    }
    
    public List<CRIDStreamInfo> getStreams()
    {
        return streams;
    }
    
    public void setStreams(List<CRIDStreamInfo> streams)
    {
        this.streams = streams;
    }
    
    @Override
    public String toString()
    {
        return "CRIDFile{" +
               "header=" + header +
               ", metadata=" + metadata +
               ", streams=" + streams +
               ", streamData=" + streamData +
               '}';
    }
}

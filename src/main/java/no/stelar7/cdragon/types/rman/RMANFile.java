package no.stelar7.cdragon.types.rman;

import java.util.*;

public class RMANFile
{
    private RMANFileHeader header;
    private RMANFileBody   body;
    private byte[]         compressedBody;
    private byte[]         signature;
    
    private Map<String, RMANFileBodyBundleChunkInfo> chunksById = null;
    
    public void buildChunkMap()
    {
        chunksById = new HashMap<>();
        for (RMANFileBodyBundle bundle : getBody().getBundles())
        {
            int currentIndex = 0;
            for (RMANFileBodyBundleChunk chunk : bundle.getChunks())
            {
                RMANFileBodyBundleChunkInfo chunkInfo = new RMANFileBodyBundleChunkInfo(bundle.getBundleId(), chunk.getChunkId(), currentIndex, chunk.getCompressedSize());
                chunksById.put(chunk.getChunkId(), chunkInfo);
                
                currentIndex += chunk.getCompressedSize();
            }
        }
    }
    
    public RMANFileHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(RMANFileHeader header)
    {
        this.header = header;
    }
    
    public byte[] getCompressedBody()
    {
        return compressedBody;
    }
    
    public void setCompressedBody(byte[] compressedBody)
    {
        this.compressedBody = compressedBody;
    }
    
    public byte[] getSignature()
    {
        return signature;
    }
    
    public void setSignature(byte[] signature)
    {
        this.signature = signature;
    }
    
    public RMANFileBody getBody()
    {
        return body;
    }
    
    public void setBody(RMANFileBody body)
    {
        this.body = body;
    }
    
    public Map<String, RMANFileBodyBundleChunkInfo> getChunkMap()
    {
        return chunksById;
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
        RMANFile rmanFile = (RMANFile) o;
        return Objects.equals(header, rmanFile.header) &&
               Arrays.equals(compressedBody, rmanFile.compressedBody) &&
               Arrays.equals(signature, rmanFile.signature) &&
               Objects.equals(body, rmanFile.body);
    }
    
    @Override
    public int hashCode()
    {
        int result = Objects.hash(header, body);
        result = 31 * result + Arrays.hashCode(compressedBody);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }
    
    @Override
    public String toString()
    {
        return "RMANFile{" +
               "header=" + header +
               ", compressedBody=" + Arrays.toString(compressedBody) +
               ", signature=" + Arrays.toString(signature) +
               ", body=" + body +
               '}';
    }
}

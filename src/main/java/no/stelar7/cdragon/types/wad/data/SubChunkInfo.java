package no.stelar7.cdragon.types.wad.data;

public class SubChunkInfo
{
    int  uncompressed;
    int  compressed;
    long hash;
    
    public SubChunkInfo(int uncompressed, int compressed, long hash)
    {
        this.uncompressed = uncompressed;
        this.compressed = compressed;
        this.hash = hash;
    }
}

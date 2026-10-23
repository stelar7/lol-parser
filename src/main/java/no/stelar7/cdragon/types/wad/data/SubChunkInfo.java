package no.stelar7.cdragon.types.wad.data;

/**
 * One entry of a .subchunktoc file; sizes are unsigned 32 bit values, hash is the xxh3_64 of the compressed data
 */
public class SubChunkInfo
{
    long uncompressed;
    long compressed;
    long hash;

    public SubChunkInfo(long uncompressed, long compressed, long hash)
    {
        this.uncompressed = uncompressed;
        this.compressed = compressed;
        this.hash = hash;
    }

    public long getUncompressed()
    {
        return uncompressed;
    }

    public long getCompressed()
    {
        return compressed;
    }

    public long getHash()
    {
        return hash;
    }
}

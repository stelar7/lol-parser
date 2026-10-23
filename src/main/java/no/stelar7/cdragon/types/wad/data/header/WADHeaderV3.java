package no.stelar7.cdragon.types.wad.data.header;

/**
 * WAD 3.x header (3.0 - 3.4)
 */
public final class WADHeaderV3 extends WADHeaderBase
{
    private final byte[] ECDSA;
    private final long   checksum;

    public WADHeaderV3(String magic, int major, int minor, byte[] ECDSA, long checksum, long fileCount)
    {
        super(magic, major, minor, fileCount);
        this.ECDSA = ECDSA;
        this.checksum = checksum;
    }

    public byte[] getECDSA()
    {
        return ECDSA.clone();
    }

    public long getChecksum()
    {
        return checksum;
    }
}

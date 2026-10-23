package no.stelar7.cdragon.types.wad.data.header;

/**
 * WAD 2.x header
 */
public final class WADHeaderV2 extends WADHeaderBase
{
    private final int    ECDSALength;
    private final byte[] ECDSA;
    private final byte[] ECDSAPadding;
    private final long   fileChecksum;
    private final int    entryHeaderOffset;
    private final int    entryHeaderCellSize;

    public WADHeaderV2(String magic, int major, int minor, int ECDSALength, byte[] ECDSA, byte[] ECDSAPadding, long fileChecksum, int entryHeaderOffset, int entryHeaderCellSize, long fileCount)
    {
        super(magic, major, minor, fileCount);
        this.ECDSALength = ECDSALength;
        this.ECDSA = ECDSA;
        this.ECDSAPadding = ECDSAPadding;
        this.fileChecksum = fileChecksum;
        this.entryHeaderOffset = entryHeaderOffset;
        this.entryHeaderCellSize = entryHeaderCellSize;
    }

    public int getECDSALength()
    {
        return ECDSALength;
    }

    public byte[] getECDSA()
    {
        return ECDSA.clone();
    }

    public byte[] getECDSAPadding()
    {
        return ECDSAPadding.clone();
    }

    public long getFileChecksum()
    {
        return fileChecksum;
    }

    public int getEntryHeaderOffset()
    {
        return entryHeaderOffset;
    }

    public int getEntryHeaderCellSize()
    {
        return entryHeaderCellSize;
    }
}

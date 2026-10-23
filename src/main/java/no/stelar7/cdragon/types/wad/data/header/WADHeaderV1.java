package no.stelar7.cdragon.types.wad.data.header;

/**
 * WAD 1.x header
 */
public final class WADHeaderV1 extends WADHeaderBase
{
    private final int entryHeaderOffset;
    private final int entryHeaderCellSize;

    public WADHeaderV1(String magic, int major, int minor, int entryHeaderOffset, int entryHeaderCellSize, long fileCount)
    {
        super(magic, major, minor, fileCount);
        this.entryHeaderOffset = entryHeaderOffset;
        this.entryHeaderCellSize = entryHeaderCellSize;
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

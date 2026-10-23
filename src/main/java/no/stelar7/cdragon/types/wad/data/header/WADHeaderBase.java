package no.stelar7.cdragon.types.wad.data.header;

/**
 * The fields shared by every WAD header version. Immutable; the concrete type tells the version
 * ({@link WADHeaderV1} for 1.x, {@link WADHeaderV2} for 2.x, {@link WADHeaderV3} for 3.x).
 */
public abstract sealed class WADHeaderBase permits WADHeaderV1, WADHeaderV2, WADHeaderV3
{
    protected final String magic;
    protected final int    major;
    protected final int    minor;
    protected final long   fileCount;

    protected WADHeaderBase(String magic, int major, int minor, long fileCount)
    {
        this.magic = magic;
        this.major = major;
        this.minor = minor;
        this.fileCount = fileCount;
    }

    public String getMagic()
    {
        return magic;
    }

    public int getMajor()
    {
        return major;
    }

    public int getMinor()
    {
        return minor;
    }

    public long getFileCount()
    {
        return fileCount;
    }
}

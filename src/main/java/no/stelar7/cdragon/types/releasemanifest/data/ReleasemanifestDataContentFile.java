package no.stelar7.cdragon.types.releasemanifest.data;

public final class ReleasemanifestDataContentFile
{
    private final int    nameIndex;
    private final int    version;
    private final String hash;
    private final int    flags;
    private final int    size;
    private final int    compressedSize;
    private final int    unknown;
    private final short  type;
    private final short  padding;
    
    public ReleasemanifestDataContentFile(int nameIndex, int version, String hash, int flags, int size, int compressedSize, int unknown, short type, short padding)
    {
        this.nameIndex = nameIndex;
        this.version = version;
        this.hash = hash;
        this.flags = flags;
        this.size = size;
        this.compressedSize = compressedSize;
        this.unknown = unknown;
        this.type = type;
        this.padding = padding;
    }
    
    // Flags:
    // 0x01 :  Managedfiles dir (?)
    // 0x02 :  Archived/Filearchives dir (?)
    // 0x04 :  (?) #
    // 0x10 :  Compressed
    // lol_air_client: all 0
    // lol_air_client_config_euw: all 0
    // lol_launcher: all & 4
    // lol_game_client: all & 4
    
    public int getNameIndex()
    {
        return nameIndex;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public String getHash()
    {
        return hash;
    }
    
    public int getFlags()
    {
        return flags;
    }
    
    public int getSize()
    {
        return size;
    }
    
    public int getCompressedSize()
    {
        return compressedSize;
    }
    
    public int getUnknown()
    {
        return unknown;
    }
    
    public short getType()
    {
        return type;
    }
    
    public short getPadding()
    {
        return padding;
    }
}

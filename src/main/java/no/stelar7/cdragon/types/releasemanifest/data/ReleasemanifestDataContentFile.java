package no.stelar7.cdragon.types.releasemanifest.data;

public class ReleasemanifestDataContentFile
{
    private int    nameIndex;
    private int    version;
    private String hash;
    private int    flags;
    private int    size;
    private int    compressedSize;
    private int    unknown;
    private short  type;
    private short  padding;
    
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
    
    public void setNameIndex(int nameIndex)
    {
        this.nameIndex = nameIndex;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public void setVersion(int version)
    {
        this.version = version;
    }
    
    public String getHash()
    {
        return hash;
    }
    
    public void setHash(String hash)
    {
        this.hash = hash;
    }
    
    public int getFlags()
    {
        return flags;
    }
    
    public void setFlags(int flags)
    {
        this.flags = flags;
    }
    
    public int getSize()
    {
        return size;
    }
    
    public void setSize(int size)
    {
        this.size = size;
    }
    
    public int getCompressedSize()
    {
        return compressedSize;
    }
    
    public void setCompressedSize(int compressedSize)
    {
        this.compressedSize = compressedSize;
    }
    
    public int getUnknown()
    {
        return unknown;
    }
    
    public void setUnknown(int unknown)
    {
        this.unknown = unknown;
    }
    
    public short getType()
    {
        return type;
    }
    
    public void setType(short type)
    {
        this.type = type;
    }
    
    public short getPadding()
    {
        return padding;
    }
    
    public void setPadding(short padding)
    {
        this.padding = padding;
    }
}

package no.stelar7.cdragon.types.crid;

public class BlockType
{
    private PacketType type;
    private int        size;
    
    public BlockType(PacketType type, int size)
    {
        this.type = type;
        this.size = size;
    }
    
    public PacketType getType()
    {
        return type;
    }
    
    public int getSize()
    {
        return size;
    }
}

package no.stelar7.cdragon.types.crid.data;

public class BlockType
{
    private final String     blockKey;
    private final PacketType type;
    private final int        size;
    
    public BlockType(String blockKey, PacketType type, int size)
    {
        this.blockKey = blockKey;
        this.type = type;
        this.size = size;
    }
    
    public String getBlockKey()
    {
        return blockKey;
    }
    
    public PacketType getType()
    {
        return type;
    }
    
    public int getSize()
    {
        return size;
    }
    
    @Override
    public String toString()
    {
        return "BlockType{" +
               "blockKey='" + blockKey + '\'' +
               ", type=" + type +
               ", size=" + size +
               '}';
    }
}

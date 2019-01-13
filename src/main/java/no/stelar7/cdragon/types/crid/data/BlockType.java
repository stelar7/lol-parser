package no.stelar7.cdragon.types.crid.data;

public class BlockType
{
    private String     blockKey;
    private PacketType type;
    private int        size;
    
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

package no.stelar7.cdragon.types.mgeo.data;

/**
 * A texture override, either for the whole map (sampler definition) or per mesh
 */
public class MGEOTextureOverride
{
    private int    index;
    private String texture;
    
    public int getIndex()
    {
        return index;
    }
    
    public void setIndex(int index)
    {
        this.index = index;
    }
    
    public String getTexture()
    {
        return texture;
    }
    
    public void setTexture(String texture)
    {
        this.texture = texture;
    }
    
    public MGEOTextureOverride()
    {
    }
    
    public MGEOTextureOverride(int index, String texture)
    {
        this.index = index;
        this.texture = texture;
    }
}

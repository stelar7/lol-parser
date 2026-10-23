package no.stelar7.cdragon.types.mgeo.data;

import no.stelar7.cdragon.util.types.math.Vector2f;

/**
 * A texture channel (baked light, stationary light, baked paint)
 */
public class MGEOChannel
{
    private String   texture;
    private Vector2f scale;
    private Vector2f bias;
    
    public String getTexture()
    {
        return texture;
    }
    
    public void setTexture(String texture)
    {
        this.texture = texture;
    }
    
    public Vector2f getScale()
    {
        return scale;
    }
    
    public void setScale(Vector2f scale)
    {
        this.scale = scale;
    }
    
    public Vector2f getBias()
    {
        return bias;
    }
    
    public void setBias(Vector2f bias)
    {
        this.bias = bias;
    }
}

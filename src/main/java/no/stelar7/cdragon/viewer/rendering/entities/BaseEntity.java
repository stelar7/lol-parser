package no.stelar7.cdragon.viewer.rendering.entities;

import no.stelar7.cdragon.util.types.math.Vector3f;
import no.stelar7.cdragon.viewer.rendering.models.Model;
import org.joml.Quaternionf;

public class BaseEntity
{
    private Model       model;
    private Vector3f    position;
    private Vector3f    scale;
    private Quaternionf rotation;
    
    public BaseEntity(Model model, Vector3f position, Quaternionf rotation, Vector3f scale)
    {
        this.model = model;
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }
    
    public BaseEntity(Model model)
    {
        this(model, new Vector3f(), new Quaternionf(), new Vector3f(1));
    }
    
    public Model getModel()
    {
        return model;
    }
    
    public void setModel(Model model)
    {
        this.model = model;
    }
    
    public Vector3f getPosition()
    {
        return position;
    }
    
    public void setPosition(Vector3f position)
    {
        this.position = position;
    }
    
    public Quaternionf getRotation()
    {
        return rotation;
    }
    
    public void setRotation(Quaternionf rotation)
    {
        this.rotation = rotation;
    }
    
    public Vector3f getScale()
    {
        return scale;
    }
    
    public void setScale(Vector3f scale)
    {
        this.scale = scale;
    }
}

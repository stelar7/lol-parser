package no.stelar7.cdragon.viewer.rendering.entities;

import no.stelar7.cdragon.util.types.math.Vector3f;
import no.stelar7.cdragon.viewer.rendering.models.Model;
import org.joml.Quaternionf;

public class BaseEntity
{
    private Model       model;
    private Vector3f    position;
    private Quaternionf rotation;
    
    public BaseEntity(Model model, Vector3f position, Quaternionf rotation)
    {
        this.model = model;
        this.position = position;
        this.rotation = rotation;
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
}

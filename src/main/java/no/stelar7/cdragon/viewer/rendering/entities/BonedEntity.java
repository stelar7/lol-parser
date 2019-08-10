package no.stelar7.cdragon.viewer.rendering.entities;

import no.stelar7.cdragon.types.skl.data.ReadableBone;
import no.stelar7.cdragon.util.types.math.Vector3f;
import no.stelar7.cdragon.viewer.rendering.models.Model;
import org.joml.Quaternionf;

import java.util.*;

public class BonedEntity extends BaseEntity
{
    
    private List<ReadableBone> bones;
    
    public BonedEntity(List<ReadableBone> bones, Model model)
    {
        super(model, new Vector3f(), new Quaternionf(), new Vector3f(1));
    }
    
    public List<ReadableBone> getBones()
    {
        return bones;
    }
    
    public void setBones(List<ReadableBone> bones)
    {
        this.bones = bones;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        BonedEntity that = (BonedEntity) o;
        return Objects.equals(bones, that.bones);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(bones);
    }
}

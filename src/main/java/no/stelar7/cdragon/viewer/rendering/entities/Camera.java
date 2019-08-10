package no.stelar7.cdragon.viewer.rendering.entities;

import no.stelar7.cdragon.util.types.math.Vector3f;
import org.joml.*;

import java.util.Objects;

public class Camera extends BaseEntity
{
    
    private float fov;
    private float width;
    private float height;
    private float near;
    private float far;
    
    public Camera(float fov, float width, float height, float near, float far)
    {
        super(null, new Vector3f(), new Quaternionf());
        this.fov = fov;
        this.width = width;
        this.height = height;
        this.near = near;
        this.far = far;
    }
    
    public float getFOV()
    {
        return fov;
    }
    
    public void setFOV(float fov)
    {
        this.fov = fov;
    }
    
    public float getWidth()
    {
        return width;
    }
    
    public void setWidth(float width)
    {
        this.width = width;
    }
    
    public float getHeight()
    {
        return height;
    }
    
    public void setHeight(float height)
    {
        this.height = height;
    }
    
    public float getNear()
    {
        return near;
    }
    
    public void setNear(float near)
    {
        this.near = near;
    }
    
    public float getFar()
    {
        return far;
    }
    
    public void setFar(float far)
    {
        this.far = far;
    }
    
    public float getPerspective()
    {
        return width / height;
    }
    
    public Matrix4f getPerspectiveMatrix()
    {
        return new Matrix4f().setPerspective(fov, getPerspective(), near, far);
    }
    
    public Matrix4f getOrthographicMatrix()
    {
        return new Matrix4f().setOrtho(0, width, 0, height, near, far, false);
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
        Camera camera = (Camera) o;
        return Float.compare(camera.fov, fov) == 0 &&
               Float.compare(camera.width, width) == 0 &&
               Float.compare(camera.height, height) == 0 &&
               Float.compare(camera.near, near) == 0 &&
               Float.compare(camera.far, far) == 0;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(fov, width, height, near, far);
    }
    
}

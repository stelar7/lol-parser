package no.stelar7.cdragon.util.types.math;

import org.joml.*;

public class Matrix4x3f extends org.joml.Matrix4x3f
{
    
    public Vector3f decomposePosition()
    {
        Vector3f ret = new Vector3f();
        this.getTranslation(ret);
        return ret;
    }
    
    public Vector3f decomposeScale()
    {
        Vector3f ret = new Vector3f();
        this.getScale(ret);
        return ret;
    }
    
    public Vector4f decomposeRotation()
    {
        Vector4f ret = new Vector4f();
        
        AxisAngle4f ang = new AxisAngle4f();
        this.getRotation(ang);
        Quaternionf temp = new Quaternionf(ang);
        
        ret.x = temp.x;
        ret.y = temp.y;
        ret.z = temp.z;
        ret.w = temp.w;
        
        return ret;
    }
    
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("[");
        sb.append("[").append(m00());
        sb.append(", ").append(m01());
        sb.append(", ").append(m02());
        sb.append("],[ ").append(m10());
        sb.append(", ").append(m11());
        sb.append(", ").append(m12());
        sb.append("],[ ").append(m20());
        sb.append(", ").append(m21());
        sb.append(", ").append(m22());
        sb.append("],[ ").append(m30());
        sb.append(", ").append(m31());
        sb.append(", ").append(m32());
        sb.append("]]");
        return sb.toString();
    }
}

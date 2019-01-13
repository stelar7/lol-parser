package no.stelar7.cdragon.util.types.math;

public class Matrix4f extends org.joml.Matrix4f
{
    
    public Matrix4f()
    {
        super();
    }
    
    public Matrix4f(Matrix4x3f mat)
    {
        this.m00(mat.m00());
        this.m01(mat.m01());
        this.m02(mat.m02());
        this.m03(0);
        
        this.m10(mat.m10());
        this.m11(mat.m11());
        this.m12(mat.m12());
        this.m13(0);
        
        this.m20(mat.m20());
        this.m21(mat.m21());
        this.m22(mat.m22());
        this.m23(0);
        
        this.m30(mat.m30());
        this.m31(mat.m31());
        this.m32(mat.m32());
        this.m33(1);
    }
    
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("[");
        sb.append("[").append(m00());
        sb.append(", ").append(m01());
        sb.append(", ").append(m02());
        sb.append(", ").append(m03());
        sb.append("],[ ").append(m10());
        sb.append(", ").append(m11());
        sb.append(", ").append(m12());
        sb.append(", ").append(m13());
        sb.append("],[ ").append(m20());
        sb.append(", ").append(m21());
        sb.append(", ").append(m22());
        sb.append(", ").append(m23());
        sb.append("],[ ").append(m30());
        sb.append(", ").append(m31());
        sb.append(", ").append(m32());
        sb.append(", ").append(m33());
        sb.append("]]");
        return sb.toString();
    }
    
}

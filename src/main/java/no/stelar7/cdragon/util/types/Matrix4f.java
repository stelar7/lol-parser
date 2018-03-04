package no.stelar7.cdragon.util.types;

public class Matrix4f
{
    
    public float m00, m01, m02, m03;
    public float m10, m11, m12, m13;
    public float m20, m21, m22, m23;
    public float m30, m31, m32, m33;
    
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("[");
        sb.append("[").append(m00);
        sb.append(", ").append(m01);
        sb.append(", ").append(m02);
        sb.append(", ").append(m03);
        sb.append("],[ ").append(m10);
        sb.append(", ").append(m11);
        sb.append(", ").append(m12);
        sb.append(", ").append(m13);
        sb.append("],[ ").append(m20);
        sb.append(", ").append(m21);
        sb.append(", ").append(m22);
        sb.append(", ").append(m23);
        sb.append("],[ ").append(m30);
        sb.append(", ").append(m31);
        sb.append(", ").append(m32);
        sb.append(", ").append(m33);
        sb.append("]]");
        return sb.toString();
    }
    
}

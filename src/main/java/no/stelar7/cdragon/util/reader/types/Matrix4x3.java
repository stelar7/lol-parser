package no.stelar7.cdragon.util.reader.types;

import lombok.Data;

@Data
public class Matrix4x3<T>
{
    private T m00;
    private T m01;
    private T m02;
    
    private T m10;
    private T m11;
    private T m12;
    
    private T m20;
    private T m21;
    private T m22;
    
    private T m30;
    private T m31;
    private T m32;
}

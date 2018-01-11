package no.stelar7.cdragon.util.reader.types;

import lombok.*;

@Data
public class Vector4<T>
{
    private T x;
    private T y;
    private T z;
    private T w;
    
    public String toString()
    {
        return String.format("{\"x\":%s,\"y\":%s,\"z\":%s,\"w\":%s}", x, y, z, w);
    }
}


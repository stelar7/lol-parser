package no.stelar7.cdragon.util.reader.types;

import lombok.*;

@Data
public class Vector3<T>
{
    private T x;
    private T y;
    private T z;
    
    public String toString()
    {
        return String.format("{\"x\":%s,\"y\":%s,\"z\":%s}", x, y, z);
    }
}

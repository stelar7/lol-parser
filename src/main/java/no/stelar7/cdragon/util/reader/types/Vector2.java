package no.stelar7.cdragon.util.reader.types;

import lombok.*;

@Data
public class Vector2<T>
{
    private T x;
    private T y;
    
    public Vector2()
    {
    }
    
    public Vector2(T o, T o1)
    {
        this.x = o;
        this.y = o1;
    }
}

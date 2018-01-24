package no.stelar7.cdragon.util.reader.types;

import lombok.Data;

@Data
public class Vector2<T>
{
    private T x;
    private T y;
    
    public Vector2()
    {
    }
    
    public Vector2(T x, T y)
    {
        this.x = x;
        this.y = y;
    }
}

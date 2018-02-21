package no.stelar7.cdragon.util.readers.types;

import lombok.Data;

@Data
public class Vector2<X, Y>
{
    private X x;
    private Y y;
    
    public Vector2()
    {
    }
    
    public Vector2(X x, Y y)
    {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public String toString()
    {
        return String.format("{\"x\":%s, \"y\":%s}", x, y);
    }
}

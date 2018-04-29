package no.stelar7.cdragon.util.types;

import java.util.*;

public class Vector2<X, Y>
{
    private X x;
    private Y y;
    
    public Vector2()
    {
    }
    
    public Vector2(Map.Entry<X, Y> entry)
    {
        this.x = entry.getKey();
        this.y = entry.getValue();
    }
    
    public Vector2(X x, Y y)
    {
        this.x = x;
        this.y = y;
    }
    
    public X getX()
    {
        return x;
    }
    
    public Y getY()
    {
        return y;
    }
    
    @Override
    public String toString()
    {
        return String.format("{\"x\":%s, \"y\":%s}", x, y);
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
        Vector2<?, ?> vector2 = (Vector2<?, ?>) o;
        return Objects.equals(x, vector2.x) &&
               Objects.equals(y, vector2.y);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(x, y);
    }
}

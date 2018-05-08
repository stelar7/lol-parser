package no.stelar7.cdragon.util.types;

import java.util.*;

public class Vector2<X, Y>
{
    private X first;
    private Y second;
    
    public Vector2()
    {
    }
    
    public Vector2(Map.Entry<X, Y> entry)
    {
        this.first = entry.getKey();
        this.second = entry.getValue();
    }
    
    public Vector2(X x, Y y)
    {
        this.first = x;
        this.second = y;
    }
    
    public X getFirst()
    {
        return first;
    }
    
    public Y getSecond()
    {
        return second;
    }
    
    @Override
    public String toString()
    {
        return String.format("{\"x\":%s, \"y\":%s}", first, second);
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
        return Objects.equals(first, vector2.first) &&
               Objects.equals(second, vector2.second);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(first, second);
    }
}

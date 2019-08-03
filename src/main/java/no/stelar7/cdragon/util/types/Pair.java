package no.stelar7.cdragon.util.types;

import java.util.Objects;

public class Pair<A, B>
{
    A a;
    B b;
    
    public Pair(A a, B b)
    {
        this.a = a;
        this.b = b;
    }
    
    public A getA()
    {
        return a;
    }
    
    public void setA(A a)
    {
        this.a = a;
    }
    
    public B getB()
    {
        return b;
    }
    
    public void setB(B b)
    {
        this.b = b;
    }
    
    public String toJson()
    {
        return "\"" + a.toString() + "\": \"" + b.toString() + "\",";
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
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(a, pair.a) &&
               Objects.equals(b, pair.b);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(a, b);
    }
    
    @Override
    public String toString()
    {
        return "Pair{" +
               "a=" + a +
               ", b=" + b +
               '}';
    }
}

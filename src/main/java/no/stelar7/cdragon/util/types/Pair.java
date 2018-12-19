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
        Pair<?, ?> triplet = (Pair<?, ?>) o;
        return Objects.equals(a, triplet.a) &&
               Objects.equals(b, triplet.b);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(a, b);
    }
    
    @Override
    public String toString()
    {
        return "Triplet{" +
               "a=" + a +
               ", b=" + b +
               '}';
    }
}

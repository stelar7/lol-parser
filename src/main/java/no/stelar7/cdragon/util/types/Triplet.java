package no.stelar7.cdragon.util.types;

import java.util.Objects;

public class Triplet<A, B, C>
{
    A a;
    B b;
    C c;
    
    public Triplet(A a, B b, C c)
    {
        this.a = a;
        this.b = b;
        this.c = c;
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
    
    public C getC()
    {
        return c;
    }
    
    public void setC(C c)
    {
        this.c = c;
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
        Triplet<?, ?, ?> triplet = (Triplet<?, ?, ?>) o;
        return Objects.equals(a, triplet.a) &&
               Objects.equals(b, triplet.b) &&
               Objects.equals(c, triplet.c);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(a, b, c);
    }
    
    @Override
    public String toString()
    {
        return "Triplet{" +
               "a=" + a +
               ", b=" + b +
               ", c=" + c +
               '}';
    }
}

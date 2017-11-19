package no.stelar7.cdragon.util;

import java.io.Serializable;
import java.util.Comparator;

public class NaturalOrderComparator implements Comparator<Object>, Serializable
{
    private static final long serialVersionUID = -1;
    
    static char charAt(final String s, final int i)
    {
        if (i >= s.length())
        {
            return 0;
        } else
        {
            return Character.toLowerCase(s.charAt(i));
        }
    }
    
    @Override
    public int compare(final Object o1, final Object o2)
    {
        final String a = o1.toString();
        final String b = o2.toString();
    
        int  ia = 0;
        int  ib = 0;
        int  nza;
        int  nzb;
        char ca;
        char cb;
        int  result;
        
        while (true)
        {
            nza = nzb = 0;
            
            ca = NaturalOrderComparator.charAt(a, ia);
            cb = NaturalOrderComparator.charAt(b, ib);
            
            if (Character.isDigit(ca) && Character.isDigit(cb))
            {
                if ((result = this.compareRight(a.substring(ia), b.substring(ib))) != 0)
                {
                    return result;
                }
            }
            
            if ((ca == 0) && (cb == 0))
            {
                return nza - nzb;
            }
            
            if (ca < cb)
            {
                return -1;
            } else if (ca > cb)
            {
                return +1;
            }
            
            ++ia;
            ++ib;
        }
    }
    
    int compareRight(final String a, final String b)
    {
        int bias = 0;
        int ia   = 0;
        int ib   = 0;
        
        for (; ; ia++, ib++)
        {
            final char ca = NaturalOrderComparator.charAt(a, ia);
            final char cb = NaturalOrderComparator.charAt(b, ib);
    
            if ((!Character.isDigit(ca) && !Character.isDigit(cb)) || ((ca == 0) && (cb == 0)))
            {
                return bias;
            } else if (!Character.isDigit(ca))
            {
                return -1;
            } else if (!Character.isDigit(cb))
            {
                return +1;
            } else if (ca < cb)
            {
                if (bias == 0)
                {
                    bias = -1;
                }
            } else if (ca > cb)
            {
                if (bias == 0)
                {
                    bias = +1;
                }
            }
        }
    }
}
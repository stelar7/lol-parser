package no.stelar7.cdragon.util.types;

import java.awt.color.ColorSpace;

public class Color extends java.awt.Color
{
    public Color(int r, int g, int b)
    {
        super(r, g, b);
    }
    
    public Color(int r, int g, int b, int a)
    {
        super(r, g, b, a);
    }
    
    public Color(int rgb)
    {
        super(rgb);
    }
    
    public Color(int rgba, boolean hasalpha)
    {
        super(rgba, hasalpha);
    }
    
    public Color(float r, float g, float b)
    {
        super(r, g, b);
    }
    
    public Color(float r, float g, float b, float a)
    {
        super(r, g, b, a);
    }
    
    public Color(ColorSpace cspace, float[] components, float alpha)
    {
        super(cspace, components, alpha);
    }
    
    public Color multiply(float f)
    {
        return new Color((int) (this.getRed() * f), (int) (this.getBlue() * f), (int) (this.getGreen() * f));
    }
    
}

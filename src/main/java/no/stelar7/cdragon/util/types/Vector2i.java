package no.stelar7.cdragon.util.types;

public class Vector2i extends org.joml.Vector2i
{
    @Override
    public String toString()
    {
        return String.format("{\"x\":%d, \"y\":%d}", x, y);
    }
}

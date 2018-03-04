package no.stelar7.cdragon.util.types;

public class Vector2f extends org.joml.Vector2f
{
    @Override
    public String toString()
    {
        return String.format("{\"x\":%s, \"y\":%s}", x, y);
    }
}

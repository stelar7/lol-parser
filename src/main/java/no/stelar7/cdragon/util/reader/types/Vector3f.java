package no.stelar7.cdragon.util.reader.types;

public class Vector3f extends org.joml.Vector3f
{
    @Override
    public String toString()
    {
        return String.format("{\"x\":%s, \"y\":%s, \"z\":%s}", x, y, z);
    }
}

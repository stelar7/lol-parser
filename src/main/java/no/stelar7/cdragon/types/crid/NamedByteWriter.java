package no.stelar7.cdragon.types.crid;

public class NamedByteWriter extends no.stelar7.cdragon.util.readers.ByteWriter
{
    String name;
    
    public NamedByteWriter(String name)
    {
        this.name = name;
    }
}

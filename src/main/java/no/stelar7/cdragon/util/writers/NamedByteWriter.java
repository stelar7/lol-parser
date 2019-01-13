package no.stelar7.cdragon.util.writers;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.io.IOException;
import java.nio.file.Files;

public class NamedByteWriter extends ByteWriter implements AutoCloseable
{
    String name;
    
    public NamedByteWriter(String name)
    {
        this.name = name;
    }
    
    public String getName()
    {
        return name;
    }
    
    public void save()
    {
        try
        {
            Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve("_" + name), toByteArray());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

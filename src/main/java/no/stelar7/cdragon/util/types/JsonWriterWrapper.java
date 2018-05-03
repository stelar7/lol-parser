package no.stelar7.cdragon.util.types;

import com.google.gson.stream.JsonWriter;

import java.io.*;

public class JsonWriterWrapper
{
    private final StringWriter sw;
    private final JsonWriter   jw;
    
    public JsonWriterWrapper()
    {
        sw = new StringWriter();
        jw = new JsonWriter(new BufferedWriter(sw));
        jw.setIndent("    ");
    }
    
    public JsonWriter getJsonWriter()
    {
        return jw;
    }
    
    @Override
    public String toString()
    {
        try
        {
            jw.flush();
            return sw.toString();
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}

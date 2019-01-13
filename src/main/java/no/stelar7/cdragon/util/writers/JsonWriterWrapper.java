package no.stelar7.cdragon.util.writers;

import com.google.gson.stream.JsonWriter;

import java.io.*;

public class JsonWriterWrapper
{
    private StringWriter sw;
    private JsonWriter   jw;
    
    public JsonWriterWrapper()
    {
        sw = new StringWriter();
        jw = new JsonWriter(new BufferedWriter(sw));
        jw.setIndent("    ");
    }
    
    public JsonWriter beginObject() throws IOException
    {
        return jw.beginObject();
    }
    
    public JsonWriter endObject() throws IOException
    {
        return jw.endObject();
    }
    
    public JsonWriter beginArray() throws IOException
    {
        return jw.beginArray();
    }
    
    public JsonWriter endArray() throws IOException
    {
        return jw.endArray();
    }
    
    public JsonWriter name(String s) throws IOException
    {
        return jw.name(s);
    }
    
    public JsonWriter value(String s) throws IOException
    {
        return jw.value(s);
    }
    
    public JsonWriter jsonValue(String s) throws IOException
    {
        return jw.jsonValue(s);
    }
    
    public JsonWriter clear()
    {
        sw = new StringWriter();
        jw = new JsonWriter(new BufferedWriter(sw));
        jw.setIndent("    ");
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

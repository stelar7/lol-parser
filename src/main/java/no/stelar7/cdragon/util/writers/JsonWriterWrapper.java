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
    
    public JsonWriter beginObject()
    {
        try
        {
            return jw.beginObject();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public JsonWriter endObject()
    {
        try
        {
            return jw.endObject();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public JsonWriter beginArray()
    {
        try
        {
            return jw.beginArray();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    
        return null;
    }
    
    public JsonWriter endArray()
    {
        try
        {
            return jw.endArray();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public JsonWriter name(String s)
    {
        try
        {
            return jw.name(s);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    
        return null;
    }
    
    public JsonWriter value(String s)
    {
        try
        {
            return jw.value(s);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    
        return null;
    }
    
    public JsonWriter value(long s)
    {
        try
        {
            return jw.value(s);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return null;
    }
    
    
    public JsonWriter value(int s)
    {
        try
        {
            return jw.value(s);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public JsonWriter jsonValue(String s)
    {
        try
        {
            return jw.jsonValue(s);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    
        return null;
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
    
    public void close()
    {
        try
        {
            jw.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

package no.stelar7.cdragon.util.readers;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import com.neovisionaries.ws.client.*;
import no.stelar7.cdragon.types.lockfile.data.Lockfile;
import no.stelar7.cdragon.util.SimpleSSLContext;
import no.stelar7.cdragon.util.types.Pair;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;

public class LCUSocketReader
{
    
    private WebSocket                           socket;
    private Lockfile                            lockfile;
    private Map<String, List<Consumer<String>>> handlers = new HashMap<>();
    
    
    public LCUSocketReader(Pair<String, String> params)
    {
        setupConnection(params.getA(), params.getB());
    }
    
    public LCUSocketReader(Lockfile lock)
    {
        setupConnection(lock.getPassword(), String.valueOf(lock.getPort()));
    }
    
    private void setupConnection(String password, String port)
    {
        try
        {
            String           url     = String.format("wss://localhost:%s/", port);
            WebSocketFactory factory = new WebSocketFactory();
            factory.setSSLContext(SimpleSSLContext.getInstance("TLS"));
            factory.setVerifyHostname(false);
            
            socket = factory.createSocket(url);
            socket.setUserInfo("riot", password);
            socket.addListener(new WebSocketAdapter()
            {
                @Override
                public void onTextMessage(WebSocket websocket, String text)
                {
                    JsonElement elem = new JsonParser().parse(text);
                    JsonArray   data = elem.getAsJsonArray();
                    
                    String id      = data.get(0).toString();
                    String event   = data.get(1).getAsString();
                    String content = data.get(2).getAsJsonObject().toString();
                    
                    List<Consumer<String>> consumers = handlers.get(event);
                    try
                    {
                        StringWriter holder = new StringWriter();
                        JsonWriter   jw     = new JsonWriter(holder);
                        jw.beginObject();
                        jw.name(event).jsonValue(content);
                        jw.endObject();
                        jw.flush();
                        jw.close();
                        
                        consumers.forEach(c -> c.accept(holder.toString()));
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException | NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }
    
    
    public void connect()
    {
        try
        {
            socket.connect();
            System.out.println("Connected!");
        } catch (WebSocketException e)
        {
            e.printStackTrace();
        }
    }
    
    public void disconnect()
    {
        socket.disconnect();
    }
    
    private void sendMessage(int eventCode, String data)
    {
        socket.sendText(String.format("[%s, \"%s\"]", eventCode, data));
    }
    
    public void subscribe(String event, Consumer<String> consumer)
    {
        sendMessage(5, event);
        handlers.get(event).add(consumer);
        System.out.println("Subscribed to " + event);
    }
    
    
    public void unsubscribe(String event)
    {
        sendMessage(6, event);
        handlers.remove(event);
        System.out.println("Unsubscribed to " + event);
    }
}
    

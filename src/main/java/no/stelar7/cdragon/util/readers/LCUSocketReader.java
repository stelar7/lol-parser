package no.stelar7.cdragon.util.readers;

import com.google.gson.*;
import com.neovisionaries.ws.client.*;
import lombok.*;
import no.stelar7.cdragon.types.lockfile.data.Lockfile;
import no.stelar7.cdragon.util.SimpleSSLContext;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;

@Data
@Getter(AccessLevel.NONE)
@Setter(AccessLevel.NONE)
public class LCUSocketReader
{
    
    private WebSocket socket;
    private Lockfile  lockfile;
    private Map<String, Consumer<String>> handlers = new HashMap<>();
    
    public LCUSocketReader(Lockfile lock)
    {
        try
        {
            String           url     = String.format("wss://localhost:%s/", lock.getPort());
            WebSocketFactory factory = new WebSocketFactory();
            factory.setSSLContext(SimpleSSLContext.getInstance("TLS"));
            factory.setVerifyHostname(false);
            
            socket = factory.createSocket(url);
            socket.setUserInfo("riot", lock.getPassword());
            socket.addListener(new WebSocketAdapter()
            {
                @Override
                public void onTextMessage(WebSocket websocket, String text)
                {
                    JsonElement elem = new JsonParser().parse(text);
                    JsonArray   data = elem.getAsJsonArray();
                    handlers.get(data.get(1).getAsString()).accept(data.get(2).getAsJsonObject().toString());
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
        handlers.put(event, consumer);
    }
    
    
    public void unsubscribe(String event)
    {
        sendMessage(6, event);
        handlers.remove(event);
    }
}
    

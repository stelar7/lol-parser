package types.web;

import no.stelar7.api.l4j8.basic.cache.CacheProvider;
import no.stelar7.api.l4j8.basic.cache.impl.*;
import no.stelar7.api.l4j8.basic.calling.DataCall;
import no.stelar7.api.l4j8.basic.constants.api.*;
import no.stelar7.api.l4j8.pojo.staticdata.champion.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.*;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class ChampionTest
{
    
    static Collection<StaticChampion> statics;
    
    @BeforeClass
    public static void setup()
    {
        
        DataCall.setLogLevel(LogLevel.INFO);
        DataCall.setCacheProvider(new TieredCacheProvider(
                                          new MemoryCacheProvider(CacheProvider.TTL_INFINITY),
                                          new FileSystemCacheProvider(CacheProvider.LOCATION_DEFAULT, CacheProvider.TTL_INFINITY))
                                 );
        
        statics = UtilHandler.getL4J8().getDDragonAPI().getChampions(null, null).values();
        
    }
    
    
    @Test
    public void testSquare()
    {
        String url = "http://localhost:8080/latest/champion/%s/square";
        
        
        for (StaticChampion champion : statics)
        {
            for (Skin skin : champion.getSkins())
            {
                callUrl(url, String.valueOf(champion.getId()), String.valueOf(skin.getNum()));
                callUrl(url, champion.getKey(), String.valueOf(skin.getNum()));
                callUrl(url, champion.getName(), String.valueOf(skin.getNum()));
            }
        }
        
    }
    
    @Test
    public void testSplash()
    {
        String url = "http://localhost:8080/latest/champion/%s/splash/%s";
        
        for (StaticChampion champion : statics)
        {
            callUrl(url, String.valueOf(champion.getId()), "");
            callUrl(url, champion.getKey(), "");
            callUrl(url, champion.getName(), "");
            
            for (Skin skin : champion.getSkins())
            {
                callUrl(url, String.valueOf(champion.getId()), String.valueOf(skin.getNum()));
                callUrl(url, champion.getKey(), String.valueOf(skin.getNum()));
                callUrl(url, champion.getName(), String.valueOf(skin.getNum()));
            }
        }
    }
    
    @Test
    public void testUncentered()
    {
        String url = "http://localhost:8080/latest/champion/%s/uncentered/%s";
        
        for (StaticChampion champion : statics)
        {
            callUrl(url, String.valueOf(champion.getId()), "");
            callUrl(url, champion.getKey(), "");
            callUrl(url, champion.getName(), "");
            
            for (Skin skin : champion.getSkins())
            {
                callUrl(url, String.valueOf(champion.getId()), String.valueOf(skin.getNum()));
                callUrl(url, champion.getKey(), String.valueOf(skin.getNum()));
                callUrl(url, champion.getName(), String.valueOf(skin.getNum()));
            }
        }
    }
    
    @Test
    public void testTile()
    {
        String url = "http://localhost:8080/latest/champion/%s/tile/%s";
        
        for (StaticChampion champion : statics)
        {
            callUrl(url, String.valueOf(champion.getId()), "");
            callUrl(url, champion.getKey(), "");
            callUrl(url, champion.getName(), "");
            
            for (Skin skin : champion.getSkins())
            {
                callUrl(url, String.valueOf(champion.getId()), String.valueOf(skin.getNum()));
                callUrl(url, champion.getKey(), String.valueOf(skin.getNum()));
                callUrl(url, champion.getName(), String.valueOf(skin.getNum()));
            }
        }
    }
    
    @Test
    public void testPortrait()
    {
        String url = "http://localhost:8080/latest/champion/%s/portrait/%s";
        
        for (StaticChampion champion : statics)
        {
            callUrl(url, String.valueOf(champion.getId()), "");
            callUrl(url, champion.getKey(), "");
            callUrl(url, champion.getName(), "");
            
            for (Skin skin : champion.getSkins())
            {
                callUrl(url, String.valueOf(champion.getId()), String.valueOf(skin.getNum()));
                callUrl(url, champion.getKey(), String.valueOf(skin.getNum()));
                callUrl(url, champion.getName(), String.valueOf(skin.getNum()));
            }
        }
    }
    
    private void callUrl(String url, String name, String num)
    {
        try
        {
            String            fixed = String.format(url, name, num);
            HttpURLConnection con   = (HttpURLConnection) new URL(fixed).openConnection();
            if (con.getResponseCode() == 404)
            {
                System.out.println(fixed);
            }
            con.disconnect();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

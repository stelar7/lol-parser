package no.stelar7.cdragon.web;

import no.stelar7.api.l4j8.basic.APICredentials;
import no.stelar7.api.l4j8.basic.cache.CacheProvider;
import no.stelar7.api.l4j8.basic.cache.impl.*;
import no.stelar7.api.l4j8.basic.calling.DataCall;
import no.stelar7.api.l4j8.basic.constants.api.LogLevel;
import no.stelar7.api.l4j8.impl.L4J8;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class SpringBootSetup
{
    public static void main(String[] args)
    {
        SpringApplication.run(SpringBootSetup.class, args);
    }
    
    @Bean
    public L4J8 getL4J8()
    {
        L4J8 api = new L4J8(new APICredentials(SecretFile.API_KEY, SecretFile.API_KEY));
        
        DataCall.setLogLevel(LogLevel.INFO);
        DataCall.setCacheProvider(new TieredCacheProvider(
                                          new MemoryCacheProvider(CacheProvider.TTL_INFINITY),
                                          new FileSystemCacheProvider(CacheProvider.LOCATION_DEFAULT, CacheProvider.TTL_INFINITY))
                                 );
        
        return api;
    }
}

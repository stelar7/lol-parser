package no.stelar7.cdragon.web;

import no.stelar7.api.l4j8.basic.APICredentials;
import no.stelar7.api.l4j8.basic.cache.CacheProvider;
import no.stelar7.api.l4j8.basic.cache.impl.*;
import no.stelar7.api.l4j8.basic.calling.DataCall;
import no.stelar7.api.l4j8.basic.constants.api.LogLevel;
import no.stelar7.api.l4j8.impl.L4J8;
import no.stelar7.cdragon.util.handlers.UtilHandler;
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
        return UtilHandler.getL4J8();
    }
}

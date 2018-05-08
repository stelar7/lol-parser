package no.stelar7.cdragon.web.services;

import no.stelar7.api.l4j8.basic.constants.api.Platform;
import no.stelar7.api.l4j8.impl.L4J8;
import no.stelar7.api.l4j8.pojo.staticdata.champion.StaticChampion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LeagueApiService
{
    @Autowired
    L4J8 api;
    
    public Optional<StaticChampion> getStaticChampionFromKey(String key)
    {
        return api.getStaticAPI().getChampions(Platform.EUW1, null, null, null).values().stream().filter(c -> c.getKey().equalsIgnoreCase(key)).findAny();
    }
    
    public Optional<StaticChampion> getStaticChampionFromName(String key)
    {
        return api.getStaticAPI().getChampions(Platform.EUW1, null, null, null).values().stream().filter(c -> c.getName().equalsIgnoreCase(key)).findAny();
    }
    
    public Optional<StaticChampion> getStaticChampionFromId(String key)
    {
        try
        {
            return api.getStaticAPI().getChampions(Platform.EUW1, null, null, null).values().stream().filter(c -> c.getId() == Integer.parseInt(key)).findAny();
        } catch (NumberFormatException e)
        {
            return Optional.empty();
        }
    }
    
    public Optional<StaticChampion> getStaticChampion(String key)
    {
        return getStaticChampionFromId(key)
                .or(() -> getStaticChampionFromName(key)
                        .or(() -> getStaticChampionFromKey(key)));
    }
}

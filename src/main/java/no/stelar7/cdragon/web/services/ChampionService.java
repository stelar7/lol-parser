package no.stelar7.cdragon.web.services;

import no.stelar7.api.l4j8.pojo.staticdata.champion.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.util.Optional;

@Service
public class ChampionService
{
    
    @Autowired
    LeagueApiService api;
    
    public Optional<Path> findChampionSquare(String key)
    {
        Path SQUARE_PATH = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\plugins\\rcp-be-lol-game-data\\global\\default\\v1\\champion-icons");
        
        if (key.equalsIgnoreCase("-1"))
        {
            return Optional.of(SQUARE_PATH.resolve("-1.png"));
        }
        
        Optional<StaticChampion> champ = api.getStaticChampion(key);
        return champ.map(staticChampion -> SQUARE_PATH.resolve(staticChampion.getId() + ".png"));
        
    }
    
    public Optional<Path> findChampionSplash(String key, @Nullable String skin)
    {
        Path SPLASH_PATH = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\plugins\\rcp-be-lol-game-data\\global\\default\\v1\\champion-splashes");
        
        Optional<StaticChampion> champ = api.getStaticChampion(key);
        if (champ.isPresent())
        {
            Path INNER_SPLASH_PATH = SPLASH_PATH.resolve(String.valueOf(champ.get().getId()));
            
            if (skin == null)
            {
                return champ.map(staticChampion -> INNER_SPLASH_PATH.resolve(staticChampion.getId() + "000.jpg"));
            } else
            {
                Optional<Skin> skinData = champ.get()
                                               .getSkins()
                                               .stream()
                                               .filter(s -> String.valueOf(s.getNum()).equalsIgnoreCase(skin))
                                               .findAny();
                
                return skinData.map(data -> INNER_SPLASH_PATH.resolve(data.getId() + ".jpg"));
            }
        }
        
        return Optional.empty();
    }
    
    public Optional<Path> findUncenteredChampionSplash(String key, @Nullable String skin)
    {
        Path SPLASH_PATH = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\plugins\\rcp-be-lol-game-data\\global\\default\\v1\\champion-splashes\\uncentered");
        
        Optional<StaticChampion> champ = api.getStaticChampion(key);
        if (champ.isPresent())
        {
            Path INNER_SPLASH_PATH = SPLASH_PATH.resolve(String.valueOf(champ.get().getId()));
            
            if (skin == null)
            {
                return champ.map(staticChampion -> INNER_SPLASH_PATH.resolve(staticChampion.getId() + "000.jpg"));
            } else
            {
                Optional<Skin> skinData = champ.get()
                                               .getSkins()
                                               .stream()
                                               .filter(s -> String.valueOf(s.getNum()).equalsIgnoreCase(skin))
                                               .findAny();
                
                return skinData.map(data -> INNER_SPLASH_PATH.resolve(data.getId() + ".jpg"));
            }
        }
        
        return Optional.empty();
    }
    
    public Optional<Path> findChampionTile(String key, @Nullable String skin)
    {
        Path TILE_PATH = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\plugins\\rcp-be-lol-game-data\\global\\default\\v1\\champion-tiles");
        
        Optional<StaticChampion> champ = api.getStaticChampion(key);
        if (champ.isPresent())
        {
            Path INNER_TILE_PATH = TILE_PATH.resolve(String.valueOf(champ.get().getId()));
            if (skin == null)
            {
                return champ.map(staticChampion -> INNER_TILE_PATH.resolve(staticChampion.getId() + "000.jpg"));
            } else
            {
                Optional<Skin> skinData = champ.get()
                                               .getSkins()
                                               .stream()
                                               .filter(s -> String.valueOf(s.getNum()).equalsIgnoreCase(skin))
                                               .findAny();
                
                return skinData.map(data -> INNER_TILE_PATH.resolve(data.getId() + ".jpg"));
                
            }
        }
        
        return Optional.empty();
    }
    
    
    public Optional<Path> findChampionPortrait(String key, @Nullable String skin)
    {
        Path PORTRAIT_PATH = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\plugins\\rcp-be-lol-game-data\\global\\default\\assets\\characters");
        
        Optional<StaticChampion> champ = api.getStaticChampion(key);
        if (champ.isPresent())
        {
            String champKey            = champ.get().getKey().toLowerCase();
            Path   INNER_PORTRAIT_PATH = PORTRAIT_PATH.resolve(champKey).resolve("skins");
            if (skin == null)
            {
                return Optional.of(INNER_PORTRAIT_PATH.resolve("base\\" + champKey + "loadscreen.jpg"));
            } else
            {
                Optional<Skin> skinData = champ.get()
                                               .getSkins()
                                               .stream()
                                               .filter(s -> String.valueOf(s.getNum()).equalsIgnoreCase(skin))
                                               .findAny();
                
                String skinKey = skin.length() == 2 ? skin : "0" + skin;
                return Optional.of(INNER_PORTRAIT_PATH.resolve("skin" + skinKey + "\\" + champKey + "loadscreen_" + skin + ".jpg"));
            }
        }
        
        return Optional.empty();
    }
}

package no.stelar7.cdragon.types.rofl.data;


import java.util.List;

public class ROFLMetadata
{
    private long                    gameLength;
    private String                  gameVersion;
    private int                     lastGameChunkId;
    private int                     lastKeyframeId;
    private List<ROFLMetadataStats> statsJson;
    
    public long getGameLength()
    {
        return gameLength;
    }
    
    public void setGameLength(long gameLength)
    {
        this.gameLength = gameLength;
    }
    
    public String getGameVersion()
    {
        return gameVersion;
    }
    
    public void setGameVersion(String gameVersion)
    {
        this.gameVersion = gameVersion;
    }
    
    public int getLastGameChunkId()
    {
        return lastGameChunkId;
    }
    
    public void setLastGameChunkId(int lastGameChunkId)
    {
        this.lastGameChunkId = lastGameChunkId;
    }
    
    public int getLastKeyframeId()
    {
        return lastKeyframeId;
    }
    
    public void setLastKeyframeId(int lastKeyframeId)
    {
        this.lastKeyframeId = lastKeyframeId;
    }
    
    public List<ROFLMetadataStats> getStatsJson()
    {
        return statsJson;
    }
    
    public void setStatsJson(List<ROFLMetadataStats> statsJson)
    {
        this.statsJson = statsJson;
    }
}

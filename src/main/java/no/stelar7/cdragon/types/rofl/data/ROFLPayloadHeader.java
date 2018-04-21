package no.stelar7.cdragon.types.rofl.data;

public class ROFLPayloadHeader
{
    private long   gameId;
    private int    gameLength;
    private int    keyframeCount;
    private int    chunkCount;
    private int    endStartupChunkId;
    private int    startGameChunkId;
    private int    keyframeInterval;
    private short  encryptionKeyLength;
    private byte[] encryptionKey;
    
    public long getGameId()
    {
        return gameId;
    }
    
    public void setGameId(long gameId)
    {
        this.gameId = gameId;
    }
    
    public int getGameLength()
    {
        return gameLength;
    }
    
    public void setGameLength(int gameLength)
    {
        this.gameLength = gameLength;
    }
    
    public int getKeyframeCount()
    {
        return keyframeCount;
    }
    
    public void setKeyframeCount(int keyframeCount)
    {
        this.keyframeCount = keyframeCount;
    }
    
    public int getChunkCount()
    {
        return chunkCount;
    }
    
    public void setChunkCount(int chunkCount)
    {
        this.chunkCount = chunkCount;
    }
    
    public int getEndStartupChunkId()
    {
        return endStartupChunkId;
    }
    
    public void setEndStartupChunkId(int endStartupChunkId)
    {
        this.endStartupChunkId = endStartupChunkId;
    }
    
    public int getStartGameChunkId()
    {
        return startGameChunkId;
    }
    
    public void setStartGameChunkId(int startGameChunkId)
    {
        this.startGameChunkId = startGameChunkId;
    }
    
    public int getKeyframeInterval()
    {
        return keyframeInterval;
    }
    
    public void setKeyframeInterval(int keyframeInterval)
    {
        this.keyframeInterval = keyframeInterval;
    }
    
    public short getEncryptionKeyLength()
    {
        return encryptionKeyLength;
    }
    
    public void setEncryptionKeyLength(short encryptionKeyLength)
    {
        this.encryptionKeyLength = encryptionKeyLength;
    }
    
    public byte[] getEncryptionKey()
    {
        return encryptionKey;
    }
    
    public void setEncryptionKey(byte[] encryptionKey)
    {
        this.encryptionKey = encryptionKey;
    }
}

package no.stelar7.cdragon.types.rofl.data;

import lombok.Data;

@Data
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
}

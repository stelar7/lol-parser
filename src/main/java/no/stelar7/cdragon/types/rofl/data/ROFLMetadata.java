package no.stelar7.cdragon.types.rofl.data;

import lombok.Data;

import java.util.List;

@Data
public class ROFLMetadata
{
    private long                    gameLength;
    private String                  gameVersion;
    private int                     lastGameChunkId;
    private int                     lastKeyframeId;
    private List<ROFLMetadataStats> statsJson;
}

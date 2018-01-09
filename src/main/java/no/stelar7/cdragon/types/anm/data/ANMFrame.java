package no.stelar7.cdragon.types.anm.data;

import lombok.Data;

@Data
public class ANMFrame
{
    private int   boneHash;
    private short positionId;
    private short scaleId;
    private short rotationId;
    private short unknown;
}

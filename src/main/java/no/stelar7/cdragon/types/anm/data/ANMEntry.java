package no.stelar7.cdragon.types.anm.data;

import lombok.Data;
import no.stelar7.cdragon.util.types.Vector3s;

@Data
public class ANMEntry
{
    private short    compressedTime;
    private byte     hashId;
    private byte     dataType;
    private Vector3s compressedData;
}

package no.stelar7.cdragon.types.anm.data;

import lombok.Data;
import no.stelar7.cdragon.types.anm.data.versioned.*;

@Data
public class ANMFile
{
    private ANMHeader       header;
    private ANMDataVersion1 version1;
    private ANMDataVersion3 version3;
    private ANMDataVersion4 version4;
    private ANMDataVersion5 version5;
    
}

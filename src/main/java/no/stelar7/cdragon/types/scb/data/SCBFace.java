package no.stelar7.cdragon.types.scb.data;

import lombok.Data;
import org.joml.*;

@Data
public class SCBFace
{
    private Vector3i indecies;
    private String   material;
    
    private Vector3f U;
    private Vector3f V;
    
}

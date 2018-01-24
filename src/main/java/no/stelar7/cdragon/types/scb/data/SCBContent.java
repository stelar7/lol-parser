package no.stelar7.cdragon.types.scb.data;

import lombok.Data;
import no.stelar7.cdragon.util.reader.types.Vector3b;
import org.joml.*;

import java.util.*;

@Data
public class SCBContent
{
    private int vertexCount;
    private int faceCount;
    private int colored;
    
    private Vector3f center;
    private Vector3f extents;
    
    private List<Vector3f> vertices = new ArrayList<>();
    private List<Integer>  indecies = new ArrayList<>();
    private List<SCBFace>  faces    = new ArrayList<>();
    private List<Vector3b> colors   = new ArrayList<>();
}

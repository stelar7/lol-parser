package no.stelar7.cdragon.util.handlers;

import no.stelar7.cdragon.util.types.*;

import java.util.*;

public class ModelHandler
{
    public static Vector2<Vector3f, Vector3f> getScaleFactorMinMax(List<Vector3f> positions)
    {
        float minValue = Integer.MIN_VALUE;
        
        for (Vector3f pos : positions)
        {
            minValue = Math.max(minValue, Math.max(pos.x, Math.max(pos.y, pos.z)));
        }
        
        minValue /= 5;
        
        return new Vector2<>(new Vector3f(-minValue), new Vector3f(minValue));
    }
    
    public static List<Vector3f> getScaledVertices(List<Vector3f> positions)
    {
        Vector2<Vector3f, Vector3f> scaleFactor = getScaleFactorMinMax(positions);
        
        List<Vector3f> verts = new ArrayList<>();
        for (Vector3f pos : positions)
        {
            Vector3f vert = new Vector3f();
            vert.x = UtilHandler.scale(pos.x, scaleFactor.getFirst().x, scaleFactor.getSecond().x, -1, 1);
            vert.y = UtilHandler.scale(pos.y, scaleFactor.getFirst().y, scaleFactor.getSecond().y, -1, 1);
            vert.z = UtilHandler.scale(pos.z, scaleFactor.getFirst().z, scaleFactor.getSecond().z, -1, 1);
            verts.add(vert);
        }
        
        return verts;
    }
    
    public static Vector3f getMaxVertex(List<Vector3f> vertexPositions)
    {
        float minx = Integer.MIN_VALUE;
        float miny = Integer.MIN_VALUE;
        float minz = Integer.MIN_VALUE;
        
        for (Vector3f pos : vertexPositions)
        {
            minx = Math.max(minx, pos.x);
            miny = Math.max(miny, pos.y);
            minz = Math.max(minz, pos.z);
        }
        
        return new Vector3f(minx, miny, minz);
    }
    
    public static List<Integer> getIndeciesAsIntegerList(List<Short> indecies)
    {
        List<Integer> inds = new ArrayList<>();
        for (Short in : indecies)
        {
            inds.add(Integer.valueOf(in));
        }
        return inds;
    }
}

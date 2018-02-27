package no.stelar7.cdragon.viewer.rendering.models;

import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.types.Vector3f;
import no.stelar7.cdragon.viewer.rendering.buffers.*;

import java.nio.file.*;

import static org.lwjgl.opengl.GL15.*;

public class Mesh
{
    private static final int VERTEX_SIZE = 3;
    
    VAO vao;
    VBO vbo;
    VBO ibo;
    
    public int indexCount;
    
    public Mesh(float[] vertices, int[] indecies)
    {
        vao = new VAO();
        vbo = new VBO(GL_ARRAY_BUFFER);
        ibo = new VBO(GL_ELEMENT_ARRAY_BUFFER);
        
        setVertices(vertices);
        setIndecies(indecies);
    }
    
    public void setVertices(float[] vertices)
    {
        vao.bind();
        vbo.bind();
        vbo.setData(vertices);
        vao.enableAttribIndex(0);
        vao.setPointer(0, VERTEX_SIZE);
        vao.unbind();
        vbo.unbind();
    }
    
    public void setIndecies(int[] indecies)
    {
        ibo.bind();
        ibo.setData(indecies);
        indexCount = indecies.length;
        ibo.unbind();
    }
    
    public void bind()
    {
        vao.bind();
        ibo.bind();
    }
    
    public void unbind()
    {
        ibo.unbind();
        vao.unbind();
    }
    
    public static Mesh loadSKN()
    {
        SKNParser parser = new SKNParser();
        Path      path   = Paths.get(System.getProperty("user.home"), "Downloads\\lolmodelviewer\\SampleModels\\filearchives\\0.0.0.48\\DATA\\Characters\\Brand\\Brand_frostfire.skn");
        SKNFile   data   = parser.parse(path);
        
        float[] verts = new float[data.getVertexCount() * 3];
        
        float minx = Integer.MAX_VALUE;
        float maxx = Integer.MIN_VALUE;
        float miny = Integer.MAX_VALUE;
        float maxy = Integer.MIN_VALUE;
        float minz = Integer.MAX_VALUE;
        float maxz = Integer.MIN_VALUE;
        
        for (int i = 0; i < data.getVertexCount(); i++)
        {
            SKNData  v   = data.getVertices().get(i);
            Vector3f pos = v.getPosition();
            
            maxx = Math.max(maxx, pos.x);
            minx = Math.min(minx, pos.x);
            maxy = Math.max(maxy, pos.y);
            miny = Math.min(miny, pos.y);
            maxz = Math.max(maxz, pos.z);
            minz = Math.min(minz, pos.z);
        }
        
        for (int i = 0; i < data.getVertexCount(); i++)
        {
            SKNData  v   = data.getVertices().get(i);
            Vector3f pos = v.getPosition();
            
            verts[(i * 3) + 0] = UtilHandler.scale(pos.x, minx, maxx, -1, 1);
            verts[(i * 3) + 1] = UtilHandler.scale(pos.y, miny, maxy, -1, 1);
            verts[(i * 3) + 2] = UtilHandler.scale(pos.z, minz, maxz, -1, 1);
        }
        
        
        int[] inds = new int[data.getIndexCount()];
        for (int i = 0; i < data.getIndecies().size(); i++)
        {
            Short in = data.getIndecies().get(i);
            inds[i] = Integer.valueOf(in);
        }
        
        return new Mesh(verts, inds);
    }
    
    public int getIndexCount()
    {
        return indexCount;
    }
}

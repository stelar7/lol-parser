package no.stelar7.cdragon.viewer;

import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.skl.SKLParser;
import no.stelar7.cdragon.types.skl.data.*;
import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.math.Vector2;
import no.stelar7.cdragon.viewer.rendering.Renderer;
import no.stelar7.cdragon.viewer.rendering.models.*;
import no.stelar7.cdragon.viewer.rendering.shaders.*;
import org.joml.*;
import org.lwjgl.glfw.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.Math;
import java.nio.file.*;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;

public class SKNViewer extends Renderer
{
    public SKNViewer(int width, int height)
    {
        super(width, height);
    }
    
    public static void main(String[] args)
    {
        new SKNViewer(600, 600).start();
    }
    
    List<Vector2<String, Model>> models = new ArrayList<>();
    
    @Override
    public void initPostGL()
    {
        try
        {
            Files.deleteIfExists(Paths.get("C:\\Users\\Steffen\\Downloads").resolve("gl.log"));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        Path               path   = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon");
        SKNFile            skn    = new SKNParser().parse(path.resolve("illaoi_skin01.skn"));
        SKLFile            skl    = new SKLParser().parse(path.resolve("illaoi_skin01.skl"));
        List<ReadableBone> bones  = skl.toReadableBones();
        BufferedImage      texImg = new DDSParser().parse(path.resolve("illaoi_skin01_tx_cm.dds"));
        
        for (SKNMaterial submesh : skn.getMaterials())
        {
            Texture tex = new Texture(submesh, texImg);
            models.add(new Vector2<>(submesh.getName(), new Model(new Mesh(submesh), tex, skn.getDataForSubmesh(submesh))));
        }
        
        meshIndex = models.size() - 1;
        model = models.get(meshIndex).getSecond();
        
        Shader vert = new Shader("shaders/basic.vert");
        Shader frag = new Shader("shaders/basic.frag");
        
        prog = new Program();
        prog.attach(vert);
        prog.attach(frag);
        
        prog.bindVertLocation("position", 0);
        prog.bindVertLocation("uv", 1);
        
        prog.bindFragLocation("color", 0);
        
        prog.link();
    }
    
    float   x;
    float   z;
    float   time  = 10;
    boolean dirty = true;
    
    private void updateMVP()
    {
        if (!dirty)
        {
            return;
        }
        
        
        float perspective = (float) width / (float) height;
        float fov         = (float) Math.toRadians(65);
        
        Matrix4f projection = new Matrix4f().setPerspective(fov, perspective, 0.1f, 10000f);
        
        Matrix4f view = new Matrix4f().setLookAt(
                new Vector3f(x, 0.5f, z),
                new Vector3f(0, 0, 0),
                new Vector3f(0, 1f, 0));
        
        Matrix4f modelMat = new Matrix4f().translation(0, -2.5f, 0);//.scaling(2);
        
        Matrix4f mvp = projection.mul(view, new Matrix4f()).mul(modelMat, new Matrix4f());
        
        prog.bind();
        prog.setMatrix4f("mvp", mvp);
        prog.setInt("texImg", 0);
        dirty = false;
    }
    
    int   meshIndex;
    float distance = 10;
    
    @Override
    public void update()
    {
        time += .01f;
        
        x = (float) Math.sin(time) * distance;
        z = (float) Math.cos(time) * distance;
        
        dirty = true;
        updateMVP();
    }
    
    float last = 0;
    
    Model   model;
    Program prog;
    
    @Override
    public void render()
    {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        UtilHandler.logToFile("gl.log", "glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)");
        
        prog.bind();
        model.bind();
        
        glDrawElements(GL_TRIANGLES, model.getMesh().getIndexCount(), GL_UNSIGNED_INT, 0);
        UtilHandler.logToFile("gl.log", String.format("glDrawElements(GL_TRIANGLES, %s, GL_UNSIGNED_INT, 0)", model.getMesh().getIndexCount()));
    }
    
    @Override
    protected void keyCallback(long window, int key, int scanCode, int action, int mods)
    {
        if (action == GLFW.GLFW_RELEASE)
        {
            if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)
            {
                if (key == GLFW.GLFW_KEY_LEFT)
                {
                    int newIndex = meshIndex - 1;
                    meshIndex = (newIndex >= 0) ? newIndex : models.size() - 1;
                }
                if (key == GLFW.GLFW_KEY_RIGHT)
                {
                    meshIndex = (meshIndex + 1) % models.size();
                }
                Vector2<String, Model> data = models.get(meshIndex);
                System.out.println(data.getFirst());
                model = data.getSecond();
            }
            
            if (key == GLFW.GLFW_KEY_UP)
            {
                distance--;
            }
            if (key == GLFW.GLFW_KEY_DOWN)
            {
                distance++;
            }
        }
    }
}

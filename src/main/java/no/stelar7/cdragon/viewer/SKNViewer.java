package no.stelar7.cdragon.viewer;

import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.skl.SKLParser;
import no.stelar7.cdragon.types.skl.data.*;
import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.viewer.rendering.Renderer;
import no.stelar7.cdragon.viewer.rendering.models.*;
import no.stelar7.cdragon.viewer.rendering.shaders.*;
import org.joml.*;

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
    
    List<Model> models = new ArrayList<>();
    
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
        
        Path               path  = UtilHandler.DOWNLOADS_FOLDER.resolve("Pyke\\Champions\\assets\\characters\\pyke\\skins\\base");
        SKNFile            skn   = new SKNParser().parse(path.resolve("pyke_base.pyke.skn"));
        SKLFile            skl   = new SKLParser().parse(path.resolve("pyke_base.pyke.skl"));
        List<ReadableBone> bones = skl.toReadableBones();
        
        BufferedImage texImg = new DDSParser().parse(path.resolve("pyke_base_tx_cm.pyke.dds"));
        Texture       tex    = new Texture(skn, texImg);
        
        models.add(new Model(new Mesh(skn), tex));
        for (SKNMaterial submesh : skn.getMaterials())
        {
            models.add(new Model(new Mesh(submesh), tex));
        }
        model = models.get(0);
        
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
    float   time  = 0;
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
        
        Matrix4f model = new Matrix4f().scaling(2);
        
        Matrix4f mvp = projection.mul(view, new Matrix4f()).mul(model, new Matrix4f());
        
        prog.bind();
        prog.setMatrix4f("mvp", mvp);
        prog.setInt("texImg", 0);
        dirty = false;
    }
    
    int meshIndex;
    
    @Override
    public void update()
    {
        time += .01f;
        
        float distance = 6;
        
        x = (float) Math.sin(time) * distance;
        z = (float) Math.cos(time) * distance;
        
        dirty = true;
        
        if (time > 5)
        {
            meshIndex = (meshIndex + 1) % models.size();
            model = models.get(meshIndex);
            time = 0;
        }
        
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
        
        if (prog != Program.current)
        {
            Program.current = prog;
            prog.bind();
        }
        
        
        if (model != Model.current)
        {
            Model.current = model;
            model.bind();
        }
        
        glDrawElements(GL_TRIANGLES, model.getMesh().getIndexCount(), GL_UNSIGNED_INT, 0);
        UtilHandler.logToFile("gl.log", String.format("glDrawElements(GL_TRIANGLES, %s, GL_UNSIGNED_INT, 0)", model.getMesh().getIndexCount()));
    }
}

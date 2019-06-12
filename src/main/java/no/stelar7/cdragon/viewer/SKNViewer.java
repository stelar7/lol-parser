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
        
        Path               path   = UtilHandler.CDRAGON_FOLDER.resolve("cdragon");
        SKNFile            skn    = new SKNParser().parse(path.resolve("ghosty_tier1.companions.skn"));
        SKLFile            skl    = new SKLParser().parse(path.resolve("ghosty_tier1.companions.skl"));
        List<ReadableBone> bones  = skl.toReadableBones();
        BufferedImage      texImg = new DDSParser().parse(path.resolve("npc_pet_ghosty_fire_tx_cm.dds"));
        
        for (SKNMaterial submesh : skn.getMaterials())
        {
            Texture tex = new Texture(submesh, texImg);
            models.add(new Vector2<>(submesh.getName(), new Model(new Mesh(submesh), tex, skn.getDataForSubmesh(submesh))));
        }
        
        /*
        use this when bins support linked files!
        Path       binPath       = UtilHandler.CDRAGON_FOLDER.resolve("cdragon").resolve("ahri_skin_14.bin");
        Path       extractedPath = UtilHandler.CDRAGON_FOLDER.resolve("pbe");
        JsonObject bin           = new JsonParser().parse(new BINParser().parse(binPath).toJson()).getAsJsonObject();
        JsonObject skinContainer = bin.getAsJsonObject("SkinCharacterDataProperties");
        JsonObject skinData      = skinContainer.getAsJsonObject((String) skinContainer.keySet().toArray()[0]).getAsJsonObject();
        JsonObject meshContainer = skinData.getAsJsonObject("skinMeshProperties");
        JsonObject meshData      = meshContainer.getAsJsonObject((String) meshContainer.keySet().toArray()[0]).getAsJsonObject();
        
        SKNFile            skn        = new SKNParser().parse(extractedPath.resolve(meshData.get("simpleSkin").getAsString()));
        SKLFile            skl        = new SKLParser().parse(extractedPath.resolve(meshData.get("skeleton").getAsString()));
        BufferedImage      defaultTex = new DDSParser().parse(extractedPath.resolve(meshData.get("texture").getAsString()));
        List<ReadableBone> bones      = skl.toReadableBones();
        
        outer:
        for (SKNMaterial submesh : skn.getMaterials())
        {
            for (JsonElement override : meshData.getAsJsonArray("materialOverride"))
            {
                JsonObject obj = override.getAsJsonObject().get((String) override.getAsJsonObject().keySet().toArray()[0]).getAsJsonObject();
                if (submesh.getName().equals(obj.get("submesh").getAsString()))
                {
                    Texture tex = new Texture(submesh, new DDSParser().parse(extractedPath.resolve(obj.get("texture").getAsString())));
                    models.add(new Vector2<>(submesh.getName(), new Model(new Mesh(submesh), tex, skn.getDataForSubmesh(submesh))));
                    continue outer;
                }
            }
            
            models.add(new Vector2<>(submesh.getName(), new Model(new Mesh(submesh), defaultTex, skn.getDataForSubmesh(submesh))));
        }
        
        if (skn.getMaterials().size() != models.size())
        {
            System.out.println();
        }
        */
        
        meshIndex = 0;
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
    float   time  = -1;
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
                int index = meshIndex + ((key == GLFW.GLFW_KEY_LEFT) ? -1 : 1);
                index = (index >= 0) ? index : (models.size() - 1);
                index = (index >= models.size()) ? (index % models.size()) : index;
                
                Vector2<String, Model> data = models.get(index);
                if (model == data.getSecond())
                {
                    System.out.println("Only one model present!");
                    return;
                }
                
                System.out.println(data.getFirst());
                model = data.getSecond();
                meshIndex = index;
            }
            
            if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN)
            {
                distance += (key == GLFW.GLFW_KEY_UP ? 1 : -1);
            }
        }
    }
}

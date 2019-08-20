package no.stelar7.cdragon.viewer;

import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.skl.SKLParser;
import no.stelar7.cdragon.types.skl.data.SKLFile;
import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.Pair;
import no.stelar7.cdragon.util.types.math.Vector2;
import no.stelar7.cdragon.viewer.rendering.Renderer;
import no.stelar7.cdragon.viewer.rendering.entities.*;
import no.stelar7.cdragon.viewer.rendering.models.*;
import no.stelar7.cdragon.viewer.rendering.models.skeleton.*;
import no.stelar7.cdragon.viewer.rendering.models.texture.*;
import no.stelar7.cdragon.viewer.rendering.shaders.*;
import org.joml.*;
import org.lwjgl.glfw.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.Math;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

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
    
    @Override
    public void initPostGL()
    {
        System.out.println("LOADING ASSETS");
        
        // enable gl.log
        UtilHandler.debug = true;
        
        Path           assetRoot = UtilHandler.CDRAGON_FOLDER.resolve("pbe");
        List<SkinData> skinList  = new ArrayList<>();
        
        try
        {
            Files.deleteIfExists(UtilHandler.CDRAGON_FOLDER.resolve("gl.log"));
            
            Path characterBase = assetRoot.resolve("data\\characters");
            List<Path> skins = Files.walk(characterBase, FileVisitOption.FOLLOW_LINKS)
                                    .filter(f -> f.toString().contains("\\skins\\"))
                                    .filter(f -> f.toString().endsWith(".bin"))
                                    .collect(Collectors.toList());
            skins.stream().limit(5).map(p -> SkinData.createFromBin(assetRoot, p)).filter(Objects::nonNull).forEach(skinList::add);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        SkinData readData = skinList.get(0);
        SKNFile  skn      = new SKNParser().parse(assetRoot.resolve(readData.getSimpleSkin()));
        
        SKLFile skl = new SKLParser().parse(assetRoot.resolve(readData.getSkeleton()));
        skeleton = new Skeleton(skl);
        
        /*
        readData.getAnimations().forEach((k, v) -> {
            System.out.println(k);
            Animation anim = new Animation(v, skeleton);
            animations.add(anim);
        });*/
        
        Animation anim = new Animation(readData.getAnimations().get("157F3634"), skeleton);
        animations.add(anim);
        
        List<Vector2<String, Model>> models  = new ArrayList<>();
        String                       texPath = ((List<Pair<String, String>>) readData.getMaterial().values().toArray()[0]).get(0).getB();
        for (SKNMaterial submesh : skn.getMaterials())
        {
            Path mat = assetRoot.resolve(readData.getMaterialOverride().getOrDefault(submesh.getName(), new ArrayList<>())
                                                 .stream()
                                                 .filter(p -> p.getA().equalsIgnoreCase("Diffuse_Texture"))
                                                 .findFirst().orElseGet(() -> new Pair<>("", texPath)).getB());
            
            BufferedImage matImg = new DDSParser().parse(mat);
            Texture       tex    = new Texture(submesh, matImg);
            models.add(new Vector2<>(submesh.getName(), new Model(new Mesh(submesh), tex, skeleton)));
        }
        
        for (Vector2<String, Model> model : models)
        {
            BaseEntity e = new BaseEntity(model.getSecond());
            if (readData.getInitialSubmeshToHide().contains(model.getFirst()))
            {
                e.visible = false;
            }
            
            entities.add(e);
        }
        
        float fov  = (float) Math.toRadians(65);
        float near = 0.001f;
        float far  = 10000f;
        
        camera = new Camera(fov, width, height, near, far);
        camera.getPosition().set(0, 0, -10);
        
        Shader vert = new Shader("shaders/basic.vert");
        Shader frag = new Shader("shaders/basic.frag");
        
        activeProgram = new Program();
        activeProgram.attach(vert);
        activeProgram.attach(frag);
        
        activeProgram.bindVertLocation("position", 0);
        activeProgram.bindVertLocation("uv", 1);
        
        activeProgram.bindFragLocation("color", 0);
        
        activeProgram.link();
        activeProgram.bind();
    }
    
    boolean[] forceRotate = {true, false, false, false};
    boolean   dirty       = true;
    float     updateTime  = 0;
    float     distance    = 10;
    
    List<BaseEntity> entities   = new ArrayList<>();
    List<Animation>  animations = new ArrayList<>();
    Skeleton         skeleton;
    Camera           camera;
    Program          activeProgram;
    
    Animation currentAnimation;
    
    private void updateMVP(BaseEntity entity)
    {
        if (!dirty)
        {
            return;
        }
        
        List<Matrix4f> bones = new ArrayList<>(skeleton.bones.size());
        if (entity.getModel().hasSkeleton())
        {
            if (currentAnimation == null)
            {
                Matrix4f identity = new Matrix4f().identity();
                for (int i = 0; i < bones.size(); i++)
                {
                    bones.add(identity);
                }
                
                currentAnimation = animations.get(0);
            } else
            {
                setupAnimation(bones, updateTime);
            }
        }
        
        Matrix4f projection = camera.getPerspectiveMatrix();
        
        Matrix4f view = new Matrix4f().setLookAt(
                new Vector3f(camera.getPosition()),
                new Vector3f(entity.getPosition()),
                new Vector3f(0, 1f, 0));
        
        Matrix4f modelMat = new Matrix4f().translationRotateScale(entity.getPosition(), entity.getRotation(), entity.getScale());
        
        Matrix4f mvp = projection.mul(view, new Matrix4f()).mul(modelMat, new Matrix4f());
        
        activeProgram.bind();
        activeProgram.setMatrix4f("mvp", mvp);
        activeProgram.setArrayMatrix4f("bones", bones);
        activeProgram.setInt("texImg", 0);
        dirty = false;
    }
    
    private void setupAnimation(List<Matrix4f> bones, float updateTime)
    {
        Matrix4f inverseRoot = new Matrix4f().identity();
        // TODO
    }
    
    
    @Override
    public void update()
    {
        updateTime += 1f / 60f;
        System.out.println(updateTime);
        
        for (int i = 0; i < forceRotate.length; i++)
        {
            boolean rot = forceRotate[i];
            if (i == 0 && rot)
            {
                float x = (float) Math.sin(updateTime) * distance;
                float z = (float) Math.cos(updateTime) * distance;
                camera.getPosition().set(x, 0.5f, z);
            }
            /*
            if (i == 1 && rot)
            {
                entity.getRotation().rotate(0.01f, 0, 0);
            }
            
            if (i == 2 && rot)
            {
                entity.getRotation().rotate(0, 0.01f, 0);
            }
            
            if (i == 3 && rot)
            {
                entity.getRotation().rotate(0, 0, 0.01f);
            }
            */
        }
        
        dirty = true;
    }
    
    @Override
    public void render()
    {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        UtilHandler.logToFile("gl.log", "glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)");
        
        for (BaseEntity e : entities)
        {
            if (!e.visible)
            {
                continue;
            }
            
            updateMVP(e);
            e.getModel().bind();
            
            glDrawElements(GL_TRIANGLES, e.getModel().getMesh().getIndexCount(), GL_UNSIGNED_INT, 0);
            UtilHandler.logToFile("gl.log", String.format("glDrawElements(GL_TRIANGLES, %s, GL_UNSIGNED_INT, 0)", e.getModel().getMesh().getIndexCount()));
        }
        
    }
    
    @Override
    protected void keyCallback(long window, int key, int scanCode, int action, int mods)
    {
        if (action == GLFW.GLFW_RELEASE)
        {
            if (key == GLFW.GLFW_KEY_1)
            {
                forceRotate[0] = !forceRotate[0];
            }
            if (key == GLFW.GLFW_KEY_2)
            {
                forceRotate[1] = !forceRotate[1];
            }
            if (key == GLFW.GLFW_KEY_3)
            {
                forceRotate[2] = !forceRotate[2];
            }
            if (key == GLFW.GLFW_KEY_4)
            {
                forceRotate[3] = !forceRotate[3];
            }
            
            if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN)
            {
                distance += (key == GLFW.GLFW_KEY_UP ? 1 : -1);
            }
        }
        
        if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_S)
        {
            float z = (key == GLFW.GLFW_KEY_W ? 1 : -1);
            camera.getPosition().add(0, 0, z);
        }
        
        if (key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_D)
        {
            float x = (key == GLFW.GLFW_KEY_A ? 1 : -1);
            camera.getPosition().add(x, 0, 0);
        }
        
        if (key == GLFW.GLFW_KEY_Q || key == GLFW.GLFW_KEY_E)
        {
            float y = (key == GLFW.GLFW_KEY_Q ? 1 : -1);
            camera.getPosition().add(0, y, 0);
        }
    }
}
    
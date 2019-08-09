package no.stelar7.cdragon.viewer;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.skl.SKLParser;
import no.stelar7.cdragon.types.skl.data.*;
import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.Pair;
import no.stelar7.cdragon.util.types.math.Vector2;
import no.stelar7.cdragon.viewer.rendering.Renderer;
import no.stelar7.cdragon.viewer.rendering.models.*;
import no.stelar7.cdragon.viewer.rendering.shaders.*;
import org.joml.*;
import org.lwjgl.glfw.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.Math;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
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
    
    List<Vector2<String, Model>> models = new ArrayList<>();
    
    Function<JsonElement, String>      getFirstChildKey     = obj -> obj.getAsJsonObject().keySet().toArray(String[]::new)[0];
    Function<JsonElement, JsonElement> getFirstChildElement = obj -> obj.getAsJsonObject().get(getFirstChildKey.apply(obj));
    Function<JsonElement, JsonObject>  getFirstChildObject  = obj -> getFirstChildElement.apply(obj).getAsJsonObject();
    
    BiFunction<List<String>, String, JsonObject> createJsonObject = (data, key) -> {
        for (String item : data)
        {
            if (item.contains(key) && !item.substring(item.indexOf(key) - 13).startsWith("LINK_OFFSET"))
            {
                String     realData = "{" + item.substring(item.indexOf(key) - 1);
                JsonReader reader   = new JsonReader(new StringReader(realData));
                reader.setLenient(true);
                return UtilHandler.getJsonParser().parse(reader).getAsJsonObject();
            }
        }
        return null;
    };
    
    static class SkinData
    {
        String skinId;
        String skeleton;
        String simpleSkin;
        String initialSubmeshToHide;
        
        Map<String, List<Pair<String, String>>> material;
        Map<String, List<Pair<String, String>>> materialOverride;
    }
    
    @Override
    public void initPostGL()
    {
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
            skins.stream().limit(10).forEach(p -> parseSkinInfoFromBin(assetRoot, skinList, p));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        SkinData           readData = skinList.get(0);
        SKNFile            skn      = new SKNParser().parse(assetRoot.resolve(readData.simpleSkin));
        SKLFile            skl      = new SKLParser().parse(assetRoot.resolve(readData.skeleton));
        List<ReadableBone> bones    = skl.toReadableBones();
        String             texPath  = ((List<Pair<String, String>>) readData.material.values().toArray()[0]).get(0).getB();
        for (SKNMaterial submesh : skn.getMaterials())
        {
            Path mat = assetRoot.resolve(readData.materialOverride.getOrDefault(submesh.getName(), new ArrayList<>())
                                                                  .stream()
                                                                  .filter(p -> p.getA().equalsIgnoreCase("Diffuse_Texture"))
                                                                  .findFirst().orElseGet(() -> new Pair<>("", texPath)).getB());
            
            BufferedImage matImg = new DDSParser().parse(mat);
            Texture       tex    = new Texture(submesh, matImg);
            models.add(new Vector2<>(submesh.getName(), new Model(new Mesh(submesh), tex, skn.getDataForSubmesh(submesh))));
        }
        
        meshIndex = 0;
        queuedModel = models.get(meshIndex).getSecond();
        
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
    
    private Path resolveLinkToAsset(Path assetRoot, String assetString)
    {
        Path hashPath = assetRoot.resolve(assetString);
        if (hashPath.toString().length() > 255)
        {
            String hashMe = hashPath.toString().substring(assetRoot.toString().length() + 1).replace("\\", "/");
            String hash   = HashHandler.computeXXHash64(hashMe.toLowerCase());
            hashPath = hashPath.resolveSibling("too_long_filename_" + hash + ".bin");
        }
        return hashPath;
    }
    
    
    private List<Pair<String, String>> extractMaterialData(List<BINFile> linkedFiles, Optional<BINValue> material, Optional<BINValue> texture)
    {
        if (material.isPresent())
        {
            Optional<BINEntry> materialData = linkedFiles.stream()
                                                         .map(f -> f.get((String) material.get().getValue()))
                                                         .filter(Optional::isPresent)
                                                         .findFirst().get();
            
            if (materialData.isEmpty())
            {
                System.out.println("Unable to find link in linked files!?");
                return null;
            }
            
            List<Pair<String, String>> samplers = new ArrayList<>();
            
            BINValue samplerData = materialData.get().get("samplerValues").get();
            if (samplerData.getType() != BINValueType.CONTAINER)
            {
                return samplers;
            }
            
            BINContainer samples = (BINContainer) samplerData.getValue();
            for (Object sample : samples.getData())
            {
                BINStruct          sampleStruct   = (BINStruct) sample;
                Optional<BINValue> samplerName    = sampleStruct.get("samplerName");
                Optional<BINValue> samplerTexture = sampleStruct.get("textureName");
                
                if (samplerName.isPresent() && samplerTexture.isPresent())
                {
                    samplers.add(new Pair<>((String) samplerName.get().getValue(), (String) samplerTexture.get().getValue()));
                }
            }
            return samplers;
        }
        
        //noinspection OptionalIsPresent
        if (texture.isPresent())
        {
            return Collections.singletonList(new Pair<>("Diffuse_Texture", (String) texture.get().getValue()));
        }
        
        return Collections.emptyList();
    }
    
    private void parseSkinInfoFromBin(Path assetRoot, List<SkinData> skinList, Path p)
    {
        if (!p.toString().matches(".*skins\\\\skin\\d+\\.bin"))
        {
            return;
        }
        BINParser parser = new BINParser();
        BINFile   file   = parser.parse(p);
        
        List<BINFile> linkedFiles = new ArrayList<>();
        linkedFiles.add(file);
        file.getLinkedFiles().forEach(f -> linkedFiles.add(parser.parse(resolveLinkToAsset(assetRoot, f))));
        
        SkinData  storedData = new SkinData();
        BINEntry  skinData   = file.getEntries().stream().filter(e -> e.getType().equalsIgnoreCase("SkinCharacterDataProperties")).findFirst().get();
        BINValue  meshData   = skinData.getIfPresent("skinMeshProperties");
        BINStruct skinMesh   = (BINStruct) meshData.getValue();
        
        String skinName = skinData.get("championSkinName").map(v -> (String) v.getValue()).orElse("UNKNOWN SKIN NAME");
        storedData.skinId = skinData.get("championSkinId").map(v -> String.valueOf(v.getValue())).orElse(skinName);
        
        Optional<BINValue> skeleton = skinMesh.get("skeleton");
        if (skeleton.isEmpty())
        {
            // ignore objects with no skeleton
            return;
        }
        storedData.skeleton = (String) skeleton.get().getValue();
        
        Optional<BINValue> skin = skinMesh.get("simpleSkin");
        if (skin.isEmpty())
        {
            // ignore objects with no model
            return;
        }
        storedData.simpleSkin = (String) skin.get().getValue();
        storedData.initialSubmeshToHide = (String) skinMesh.get("initialSubmeshToHide").get().getValue();
        
        storedData.material = new HashMap<>();
        storedData.materialOverride = new HashMap<>();
        
        Optional<BINValue> material         = skinMesh.get("material");
        Optional<BINValue> texture          = skinMesh.get("texture");
        Optional<BINValue> materialOverride = skinMesh.get("materialOverride");
        
        storedData.material.put("BASE_CHARACTER_MATERIAL", extractMaterialData(linkedFiles, material, texture));
        if (materialOverride.isPresent())
        {
            BINContainer container = (BINContainer) materialOverride.get().getValue();
            for (Object data : container.getData())
            {
                BINStruct          struct  = (BINStruct) data;
                Optional<BINValue> submesh = struct.get("submesh");
                
                Optional<BINValue> overrideMaterial = struct.get("material");
                Optional<BINValue> overrideTexture  = struct.get("texture");
                
                List<Pair<String, String>> content = extractMaterialData(linkedFiles, overrideMaterial, overrideTexture);
                submesh.ifPresent(binValue -> storedData.materialOverride.put((String) binValue.getValue(), content));
            }
            
        }
        skinList.add(storedData);
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
        
        activeProgram.bind();
        activeProgram.setMatrix4f("mvp", mvp);
        activeProgram.setInt("texImg", 0);
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
    }
    
    float last = 0;
    
    
    Model   queuedModel;
    Model   activeModel;
    Program activeProgram;
    
    @Override
    public void render()
    {
        updateMVP();
        
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        UtilHandler.logToFile("gl.log", "glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)");
        
        if (queuedModel != activeModel)
        {
            activeModel = queuedModel;
            activeModel.bind();
        }
        
        glDrawElements(GL_TRIANGLES, activeModel.getMesh().getIndexCount(), GL_UNSIGNED_INT, 0);
        UtilHandler.logToFile("gl.log", String.format("glDrawElements(GL_TRIANGLES, %s, GL_UNSIGNED_INT, 0)", activeModel.getMesh().getIndexCount()));
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
                if (queuedModel == data.getSecond())
                {
                    System.out.println("Only one model present!");
                    return;
                }
                
                queuedModel = data.getSecond();
                meshIndex = index;
                
            }
            
            if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN)
            {
                distance += (key == GLFW.GLFW_KEY_UP ? 1 : -1);
            }
        }
    }
}
    
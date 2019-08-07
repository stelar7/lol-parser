package no.stelar7.cdragon.viewer;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
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
                                    .filter(f -> f.toString().endsWith(".json"))
                                    .collect(Collectors.toList());
            skins.stream().limit(10).forEach(p -> {
                try
                {
                    if (!p.toString().matches(".*skins\\\\skin\\d+\\.json"))
                    {
                        return;
                    }
                    String     content = String.join("", Files.readAllLines(p));
                    JsonObject parsed  = UtilHandler.getJsonParser().parse(content).getAsJsonObject();
                    
                    List<String> linkedFiles = new ArrayList<>();
                    linkedFiles.add(content);
                    parsed.get("linkedBinFiles").getAsJsonArray().forEach(f -> {
                        try
                        {
                            Path hashPath = assetRoot.resolve(f.getAsString());
                            Path scanPath = assetRoot.resolve(UtilHandler.replaceEnding(f.getAsString(), "bin", "json"));
                            if (scanPath.toString().length() > 255)
                            {
                                String hashMe = hashPath.toString().substring(assetRoot.toString().length() + 1).replace("\\", "/");
                                String hash   = HashHandler.computeXXHash64(hashMe.toLowerCase());
                                scanPath = scanPath.resolveSibling("too_long_filename_" + hash + ".json");
                            }
                            
                            linkedFiles.add(String.join("", Files.readAllLines(scanPath)));
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    });
                    
                    SkinData storedData = new SkinData();
                    
                    JsonObject skinData = parsed.getAsJsonObject("SkinCharacterDataProperties");
                    JsonObject next     = getFirstChildObject.apply(skinData);
                    JsonObject meshData = next.getAsJsonObject("skinMeshProperties");
                    JsonObject skinMesh = getFirstChildObject.apply(meshData);
                    
                    String skinName = next.has("championSkinName") ? next.get("championSkinName").getAsString() : getFirstChildKey.apply(skinData);
                    storedData.skinId = next.has("championSkinId") ? next.get("championSkinId").getAsString() : skinName;
                    if (!skinMesh.has("skeleton"))
                    {
                        // ignore objects with no skeleton
                        return;
                    }
                    storedData.skeleton = skinMesh.get("skeleton").getAsString();
                    
                    if (!skinMesh.has("simpleSkin"))
                    {
                        // ignore objects with no model
                        return;
                    }
                    storedData.simpleSkin = skinMesh.get("simpleSkin").getAsString();
                    storedData.initialSubmeshToHide = skinMesh.has("skinMesh") ? skinMesh.get("initialSubmeshToHide").getAsString() : "";
                    
                    storedData.material = new HashMap<>();
                    
                    if (skinMesh.has("material"))
                    {
                        JsonObject materialData = createJsonObject.apply(linkedFiles, skinMesh.get("material").getAsString().substring(13));
                        if (materialData == null)
                        {
                            System.out.println("Unable to find link in linked files!?");
                            return;
                        }
                        
                        JsonObject                 materialDataEntry = getFirstChildObject.apply(materialData);
                        List<Pair<String, String>> samplers          = new ArrayList<>();
                        
                        materialDataEntry.getAsJsonArray("samplerValues").forEach(s -> {
                            JsonObject samplerEntry = getFirstChildObject.apply(s);
                            
                            String samplerName = samplerEntry.get("samplerName").getAsString();
                            if (samplerEntry.has("textureName"))
                            {
                                String textureName = samplerEntry.get("textureName").getAsString();
                                samplers.add(new Pair<>(samplerName, textureName));
                            }
                        });
                        
                        storedData.material.put(materialDataEntry.get("name").getAsString(), samplers);
                    } else if (skinMesh.has("texture"))
                    {
                        List<Pair<String, String>> realMat = Collections.singletonList(new Pair<>("Diffuse_Texture", skinMesh.get("texture").getAsString()));
                        storedData.material.put("BASE_CHARACTER_MATERIAL", realMat);
                    } else
                    {
                        storedData.material.put("BASE_CHARACTER_MATERIAL", Collections.emptyList());
                    }
                    
                    storedData.materialOverride = new HashMap<>();
                    if (skinMesh.has("materialOverride"))
                    {
                        skinMesh.get("materialOverride").getAsJsonArray().forEach(e -> {
                            JsonObject entry = getFirstChildObject.apply(e);
                            if (entry.has("material"))
                            {
                                JsonObject parseData = createJsonObject.apply(linkedFiles, entry.get("material").getAsString().substring(13));
                                if (parseData == null)
                                {
                                    System.out.println("Unable to find link in linked files!?");
                                    return;
                                }
                                JsonObject                 parsedData       = getFirstChildObject.apply(parseData);
                                List<Pair<String, String>> materialSamplers = new ArrayList<>();
                                if (parsedData.has("samplerValues"))
                                {
                                    parsedData.getAsJsonArray("samplerValues").forEach(s -> {
                                        JsonObject samplerEntry = getFirstChildObject.apply(s);
                                        
                                        String samplerName = samplerEntry.get("samplerName").getAsString();
                                        if (samplerEntry.has("textureName"))
                                        {
                                            String textureName = samplerEntry.get("textureName").getAsString();
                                            materialSamplers.add(new Pair<>(samplerName, textureName));
                                        }
                                    });
                                }
                                storedData.materialOverride.put(entry.get("submesh").getAsString(), materialSamplers);
                            } else if (skinMesh.has("texture"))
                            {
                                if (entry.has("texture"))
                                {
                                    List<Pair<String, String>> realMat = Collections.singletonList(new Pair<>("Diffuse_Texture", entry.get("texture").getAsString()));
                                    storedData.materialOverride.put(entry.get("submesh").getAsString(), realMat);
                                } else
                                {
                                    storedData.material.put("BASE_CHARACTER_MATERIAL", Collections.emptyList());
                                }
                            } else
                            {
                                storedData.material.put("BASE_CHARACTER_MATERIAL", Collections.emptyList());
                            }
                        });
                    }
                    
                    skinList.add(storedData);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
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
            BufferedImage matImg = new DDSParser().parse(assetRoot.resolve(readData.materialOverride.getOrDefault(submesh.getName(), new ArrayList<>())
                                                                                                    .stream()
                                                                                                    .filter(p -> p.getA().equalsIgnoreCase("Diffuse_Texture"))
                                                                                                    .findFirst().orElseGet(() -> new Pair<>("", texPath)).getB()));
            Texture tex = new Texture(submesh, matImg);
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

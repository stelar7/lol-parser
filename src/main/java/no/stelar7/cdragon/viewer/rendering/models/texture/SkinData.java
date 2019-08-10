package no.stelar7.cdragon.viewer.rendering.models.texture;

import no.stelar7.cdragon.types.anm.ANMParser;
import no.stelar7.cdragon.types.anm.data.*;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.util.handlers.HashHandler;
import no.stelar7.cdragon.util.types.Pair;
import no.stelar7.cdragon.util.types.math.Vector2;

import java.nio.file.Path;
import java.util.*;

public class SkinData
{
    private String skinId;
    private String skeleton;
    private String simpleSkin;
    private String initialSubmeshToHide;
    
    private Map<String, List<Pair<String, String>>> material         = new HashMap<>();
    private Map<String, List<Pair<String, String>>> materialOverride = new HashMap<>();
    private Map<String, ANMFile>                    animations       = new HashMap<>();
    
    public Map<String, ANMFile> getAnimations()
    {
        return animations;
    }
    
    public String getSkinId()
    {
        return skinId;
    }
    
    public void setSkinId(String skinId)
    {
        this.skinId = skinId;
    }
    
    public String getSkeleton()
    {
        return skeleton;
    }
    
    public void setSkeleton(String skeleton)
    {
        this.skeleton = skeleton;
    }
    
    public String getSimpleSkin()
    {
        return simpleSkin;
    }
    
    public void setSimpleSkin(String simpleSkin)
    {
        this.simpleSkin = simpleSkin;
    }
    
    public String getInitialSubmeshToHide()
    {
        return initialSubmeshToHide;
    }
    
    public void setInitialSubmeshToHide(String initialSubmeshToHide)
    {
        this.initialSubmeshToHide = initialSubmeshToHide;
    }
    
    public Map<String, List<Pair<String, String>>> getMaterial()
    {
        return material;
    }
    
    public void setMaterial(Map<String, List<Pair<String, String>>> material)
    {
        this.material = material;
    }
    
    public Map<String, List<Pair<String, String>>> getMaterialOverride()
    {
        return materialOverride;
    }
    
    public void setMaterialOverride(Map<String, List<Pair<String, String>>> materialOverride)
    {
        this.materialOverride = materialOverride;
    }
    
    
    public static SkinData createFromBin(Path assetRoot, Path p)
    {
        if (!p.toString().matches(".*skins\\\\skin\\d+\\.bin"))
        {
            return null;
        }
        
        ANMParser anmParser = new ANMParser();
        
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
            return null;
        }
        storedData.skeleton = (String) skeleton.get().getValue();
        
        Optional<BINValue> skin = skinMesh.get("simpleSkin");
        if (skin.isEmpty())
        {
            // ignore objects with no model
            return null;
        }
        storedData.simpleSkin = (String) skin.get().getValue();
        storedData.initialSubmeshToHide = (String) skinMesh.get("initialSubmeshToHide").get().getValue();
        
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
        
        Optional<BINValue> animationProperties = skinData.get("skinAnimationProperties");
        if (animationProperties.isEmpty())
        {
            System.out.println("No animations present for " + storedData.skinId);
            return storedData;
        }
        
        BINStruct animStructContainer = (BINStruct) animationProperties.get().getValue();
        BINValue  graphData           = animStructContainer.getIfPresent("animationGraphData");
        if (graphData.getType() == BINValueType.LINK_OFFSET)
        {
            Optional<BINEntry> realGraph = findEntryInFiles(linkedFiles, String.valueOf(graphData.getValue()));
            if (realGraph.isEmpty())
            {
                System.out.println("Unable to find animations for " + storedData.skinId);
                return storedData;
            }
            
            BINValue                      clipData   = realGraph.get().getIfPresent("mClipDataMap");
            BINMap                        clipMap    = (BINMap) clipData.getValue();
            List<Vector2<Object, Object>> clipEnties = clipMap.getData();
            for (Vector2<Object, Object> entry : clipEnties)
            {
                String hash     = (String) entry.getFirst();
                String realName = HashHandler.getBinHashes().getOrDefault(hash, hash);
                
                BINStruct          data          = (BINStruct) entry.getSecond();
                Optional<BINValue> resourceDataM = data.get("mAnimationResourceData");
                if (resourceDataM.isEmpty())
                {
                    continue;
                }
                
                BINStruct          resourceData               = (BINStruct) resourceDataM.get().getValue();
                Optional<BINValue> animationFilePathContainer = resourceData.get("mAnimationFilePath");
                if (animationFilePathContainer.isEmpty())
                {
                    continue;
                }
                
                Path animationFilePath = resolveLinkToAsset(assetRoot, (String) animationFilePathContainer.get().getValue());
                
                ANMFile animationFile = anmParser.parse(animationFilePath);
                storedData.animations.put(realName, animationFile);
            }
        }
        
        return storedData;
    }
    
    private static Path resolveLinkToAsset(Path assetRoot, String assetString)
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
    
    private static Optional<BINEntry> findEntryInFiles(List<BINFile> files, String key)
    {
        return files.stream()
                    .map(f -> f.get(key))
                    .filter(Optional::isPresent)
                    .findFirst().get();
    }
    
    
    private static List<Pair<String, String>> extractMaterialData(List<BINFile> linkedFiles, Optional<BINValue> material, Optional<BINValue> texture)
    {
        if (material.isPresent())
        {
            Optional<BINEntry> materialData = findEntryInFiles(linkedFiles, (String) material.get().getValue());
            
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
    
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        SkinData skinData = (SkinData) o;
        return Objects.equals(skinId, skinData.skinId) &&
               Objects.equals(skeleton, skinData.skeleton) &&
               Objects.equals(simpleSkin, skinData.simpleSkin) &&
               Objects.equals(initialSubmeshToHide, skinData.initialSubmeshToHide) &&
               Objects.equals(material, skinData.material) &&
               Objects.equals(materialOverride, skinData.materialOverride);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(skinId, skeleton, simpleSkin, initialSubmeshToHide, material, materialOverride);
    }
}

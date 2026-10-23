package no.stelar7.cdragon.types.mgeo.data;

import no.stelar7.cdragon.util.types.math.*;

import java.util.*;

public class MGEOFileMesh
{
    // only stored in the file for versions 11 and below
    private String                    name;
    // index of the first vertex declaration used by this mesh; buffer i uses declaration (id + i)
    private int                       baseVertexDeclarationId;
    private int[]                     vertexBufferIds;
    private int                       indexBufferId;
    private List<MGEOFileMeshVertex>  vertices  = new ArrayList<>();
    // unsigned 16 bit indices
    private int[]                     indices   = new int[0];
    private List<MGeoFileMeshSubMesh> subMeshes = new ArrayList<>();

    // version 13+ (before that: 7-12, stored after the quality filter)
    private int     visibilityFlags = 0xFF;
    // version 18+
    private int     regionHash;
    // version 15+
    private int     visibilityControllerPathHash;
    private boolean disableBackfaceCulling;

    private BoundingBox boundingBox;
    private Matrix4f    transformationMatrix;
    private int         qualityFilter;
    // version 14+
    private int         layerTransitionBehavior;
    // version 11+
    private int         renderFlags;

    // versions below 7, if the file uses separate point lights
    private Vector3f   pointLight;
    // versions below 9
    private Vector3f[] sphericalHarmonics;

    private MGEOChannel bakedLight;
    // version 9+
    private MGEOChannel stationaryLight;
    // version 12+ (baked paint for 12-16, index 0)
    private List<MGEOTextureOverride> textureOverrides = new ArrayList<>();
    // version 17+
    private Vector2f bakedPaintScale;
    private Vector2f bakedPaintBias;
    // version 19+
    private int      version19Byte = -1;
    // version 20+
    private byte[]   extensionData;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getBaseVertexDeclarationId()
    {
        return baseVertexDeclarationId;
    }

    public void setBaseVertexDeclarationId(int baseVertexDeclarationId)
    {
        this.baseVertexDeclarationId = baseVertexDeclarationId;
    }

    public int[] getVertexBufferIds()
    {
        return vertexBufferIds;
    }

    public void setVertexBufferIds(int[] vertexBufferIds)
    {
        this.vertexBufferIds = vertexBufferIds;
    }

    public int getIndexBufferId()
    {
        return indexBufferId;
    }

    public void setIndexBufferId(int indexBufferId)
    {
        this.indexBufferId = indexBufferId;
    }

    public List<MGEOFileMeshVertex> getVertices()
    {
        return vertices;
    }

    public void setVertices(List<MGEOFileMeshVertex> vertices)
    {
        this.vertices = vertices;
    }

    public int[] getIndices()
    {
        return indices;
    }

    public void setIndices(int[] indices)
    {
        this.indices = indices;
    }

    public List<MGeoFileMeshSubMesh> getSubMeshes()
    {
        return subMeshes;
    }

    public void setSubMeshes(List<MGeoFileMeshSubMesh> subMeshes)
    {
        this.subMeshes = subMeshes;
    }

    public int getVisibilityFlags()
    {
        return visibilityFlags;
    }

    public void setVisibilityFlags(int visibilityFlags)
    {
        this.visibilityFlags = visibilityFlags;
    }

    public int getRegionHash()
    {
        return regionHash;
    }

    public void setRegionHash(int regionHash)
    {
        this.regionHash = regionHash;
    }

    public int getVisibilityControllerPathHash()
    {
        return visibilityControllerPathHash;
    }

    public void setVisibilityControllerPathHash(int visibilityControllerPathHash)
    {
        this.visibilityControllerPathHash = visibilityControllerPathHash;
    }

    public boolean isDisableBackfaceCulling()
    {
        return disableBackfaceCulling;
    }

    public void setDisableBackfaceCulling(boolean disableBackfaceCulling)
    {
        this.disableBackfaceCulling = disableBackfaceCulling;
    }

    public BoundingBox getBoundingBox()
    {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox boundingBox)
    {
        this.boundingBox = boundingBox;
    }

    public Matrix4f getTransformationMatrix()
    {
        return transformationMatrix;
    }

    public void setTransformationMatrix(Matrix4f transformationMatrix)
    {
        this.transformationMatrix = transformationMatrix;
    }

    public int getQualityFilter()
    {
        return qualityFilter;
    }

    public void setQualityFilter(int qualityFilter)
    {
        this.qualityFilter = qualityFilter;
    }

    public int getLayerTransitionBehavior()
    {
        return layerTransitionBehavior;
    }

    public void setLayerTransitionBehavior(int layerTransitionBehavior)
    {
        this.layerTransitionBehavior = layerTransitionBehavior;
    }

    public int getRenderFlags()
    {
        return renderFlags;
    }

    public void setRenderFlags(int renderFlags)
    {
        this.renderFlags = renderFlags;
    }

    public Vector3f getPointLight()
    {
        return pointLight;
    }

    public void setPointLight(Vector3f pointLight)
    {
        this.pointLight = pointLight;
    }

    public Vector3f[] getSphericalHarmonics()
    {
        return sphericalHarmonics;
    }

    public void setSphericalHarmonics(Vector3f[] sphericalHarmonics)
    {
        this.sphericalHarmonics = sphericalHarmonics;
    }

    public MGEOChannel getBakedLight()
    {
        return bakedLight;
    }

    public void setBakedLight(MGEOChannel bakedLight)
    {
        this.bakedLight = bakedLight;
    }

    /**
     * The baked light texture
     */
    public String getLightMap()
    {
        return bakedLight == null ? null : bakedLight.getTexture();
    }

    public MGEOChannel getStationaryLight()
    {
        return stationaryLight;
    }

    public void setStationaryLight(MGEOChannel stationaryLight)
    {
        this.stationaryLight = stationaryLight;
    }

    public List<MGEOTextureOverride> getTextureOverrides()
    {
        return textureOverrides;
    }

    public void setTextureOverrides(List<MGEOTextureOverride> textureOverrides)
    {
        this.textureOverrides = textureOverrides;
    }

    public Vector2f getBakedPaintScale()
    {
        return bakedPaintScale;
    }

    public void setBakedPaintScale(Vector2f bakedPaintScale)
    {
        this.bakedPaintScale = bakedPaintScale;
    }

    public Vector2f getBakedPaintBias()
    {
        return bakedPaintBias;
    }

    public void setBakedPaintBias(Vector2f bakedPaintBias)
    {
        this.bakedPaintBias = bakedPaintBias;
    }

    /**
     * Version 19+: the byte after the baked paint scale/bias (default 1; the client stores it but never uses it), -1 for older versions
     */
    public int getVersion19Byte()
    {
        return version19Byte;
    }

    public void setVersion19Byte(int version19Byte)
    {
        this.version19Byte = version19Byte;
    }

    /**
     * Version 20+: the raw reflection-serialized MapGeoExtension object (class hash 0xD3F07247) that follows the mesh record, null for older versions.
     * The client parses and discards it, so it is kept unparsed.
     */
    public byte[] getExtensionData()
    {
        return extensionData;
    }

    public void setExtensionData(byte[] extensionData)
    {
        this.extensionData = extensionData;
    }
}

package no.stelar7.cdragon.types.skn.data;

import lombok.Data;
import no.stelar7.cdragon.util.readers.types.*;

@Data
public class SKNData
{
    private Vector3f position;
    private Vector4b boneIndecies;
    private Vector4f weight;
    private Vector3f normals;
    private Vector2f uv;
}

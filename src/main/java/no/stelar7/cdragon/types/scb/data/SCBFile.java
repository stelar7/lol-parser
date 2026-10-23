package no.stelar7.cdragon.types.scb.data;

import org.joml.Vector3f;

import java.text.*;
import java.util.Locale;

/**
 * A static mesh, parsed from either a binary .scb (SCBParser) or a text .sco (SCOParser)
 */
public final class SCBFile
{
    private final SCBHeader  header;
    private final SCBContent content;
    
    public SCBFile(SCBHeader header, SCBContent content)
    {
        this.header = header;
        this.content = content;
    }

    public SCBHeader getHeader()
    {
        return header;
    }

    public SCBContent getContent()
    {
        return content;
    }

    /**
     * Wavefront OBJ export of the mesh.
     * Static meshes store uvs per face corner, so every face gets its own three texture coordinates.
     * Faces are grouped by material with "usemtl" (no .mtl file is written). The uvs are written as stored, like SKNFile.toOBJ.
     */
    public String toOBJ()
    {
        DecimalFormat df = new DecimalFormat("0.#######", new DecimalFormatSymbols(Locale.ENGLISH));
        StringBuilder sb = new StringBuilder();

        if (header != null && header.getFilename() != null && !header.getFilename().isBlank())
        {
            sb.append("o ").append(header.getFilename().strip()).append('\n');
        }

        for (Vector3f pos : content.getVertices())
        {
            sb.append("v ").append(df.format(pos.x)).append(' ').append(df.format(pos.y)).append(' ').append(df.format(pos.z)).append('\n');
        }

        for (SCBFace face : content.getFaces())
        {
            Vector3f u = face.getU();
            Vector3f v = face.getV();
            sb.append("vt ").append(df.format(u.x)).append(' ').append(df.format(v.x)).append('\n');
            sb.append("vt ").append(df.format(u.y)).append(' ').append(df.format(v.y)).append('\n');
            sb.append("vt ").append(df.format(u.z)).append(' ').append(df.format(v.z)).append('\n');
        }

        String material = null;
        int    uvIndex  = 1;
        for (SCBFace face : content.getFaces())
        {
            String faceMaterial = face.getMaterial() == null ? "" : face.getMaterial().strip();
            if (!faceMaterial.equals(material))
            {
                material = faceMaterial;
                if (!material.isEmpty())
                {
                    sb.append("usemtl ").append(material).append('\n');
                }
            }

            sb.append("f ")
              .append(face.getIndecies().x + 1).append('/').append(uvIndex).append(' ')
              .append(face.getIndecies().y + 1).append('/').append(uvIndex + 1).append(' ')
              .append(face.getIndecies().z + 1).append('/').append(uvIndex + 2).append('\n');
            uvIndex += 3;
        }

        return sb.toString();
    }
}

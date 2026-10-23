package no.stelar7.cdragon.types.mgeo.data;

/**
 * One element of a vertex declaration (Riot::Renderer::Mesh::Elem / ElemFormat)
 */
public class MGEOVertexElement
{
    public static final int POSITION        = 0;
    public static final int BLEND_WEIGHT    = 1;
    public static final int NORMAL          = 2;
    public static final int FOG_COORDINATE  = 3;
    public static final int PRIMARY_COLOR   = 4;
    public static final int SECONDARY_COLOR = 5;
    public static final int BLEND_INDEX     = 6;
    public static final int TEXCOORD_0      = 7;
    public static final int TEXCOORD_1      = 8;
    public static final int TEXCOORD_2      = 9;
    public static final int TEXCOORD_3      = 10;
    public static final int TEXCOORD_4      = 11;
    public static final int TEXCOORD_5      = 12;
    public static final int TEXCOORD_6      = 13;
    public static final int TEXCOORD_7      = 14;
    public static final int TANGENT         = 15;

    private static final String[] NAMES = {
            "POSITION", "BLEND_WEIGHT", "NORMAL", "FOG_COORDINATE", "PRIMARY_COLOR", "SECONDARY_COLOR", "BLEND_INDEX",
            "TEXCOORD_0", "TEXCOORD_1", "TEXCOORD_2", "TEXCOORD_3", "TEXCOORD_4", "TEXCOORD_5", "TEXCOORD_6", "TEXCOORD_7",
            "TANGENT"
    };

    public static final int FORMAT_X_FLOAT32             = 0;
    public static final int FORMAT_XY_FLOAT32            = 1;
    public static final int FORMAT_XYZ_FLOAT32           = 2;
    public static final int FORMAT_XYZW_FLOAT32          = 3;
    public static final int FORMAT_BGRA_PACKED8888       = 4;
    public static final int FORMAT_ZYXW_PACKED8888       = 5;
    public static final int FORMAT_RGBA_PACKED8888       = 6;
    public static final int FORMAT_XY_PACKED1616         = 7;
    public static final int FORMAT_XYZ_PACKED161616      = 8;
    public static final int FORMAT_XYZW_PACKED16161616   = 9;
    public static final int FORMAT_XY_PACKED88           = 10;
    public static final int FORMAT_XYZ_PACKED888         = 11;
    public static final int FORMAT_XYZW_PACKED8888       = 12;

    private int name;
    private int format;

    public MGEOVertexElement()
    {
    }

    public MGEOVertexElement(int name, int format)
    {
        this.name = name;
        this.format = format;
    }

    public int getName()
    {
        return name;
    }

    public void setName(int name)
    {
        this.name = name;
    }

    public int getFormat()
    {
        return format;
    }

    public void setFormat(int format)
    {
        this.format = format;
    }

    public String getNameString()
    {
        return name >= 0 && name < NAMES.length ? NAMES[name] : "UNKNOWN_" + name;
    }

    /**
     * Size of this element in bytes, or -1 if the format is unknown
     */
    public int getSize()
    {
        return getFormatSize(format);
    }

    public static int getFormatSize(int format)
    {
        return switch (format)
        {
            case FORMAT_X_FLOAT32 -> 4;
            case FORMAT_XY_FLOAT32 -> 8;
            case FORMAT_XYZ_FLOAT32 -> 12;
            case FORMAT_XYZW_FLOAT32 -> 16;
            case FORMAT_BGRA_PACKED8888, FORMAT_ZYXW_PACKED8888, FORMAT_RGBA_PACKED8888, FORMAT_XY_PACKED1616, FORMAT_XYZW_PACKED8888 -> 4;
            // half floats; format 8 is 8 bytes in LeagueToolkit C# VertexElement.cs, ltk_mesh (XYZW_Float16), Aventurine and MapgeoAddon
            case FORMAT_XYZ_PACKED161616, FORMAT_XYZW_PACKED16161616 -> 8;
            case FORMAT_XY_PACKED88 -> 2;
            case FORMAT_XYZ_PACKED888 -> 3;
            default -> -1;
        };
    }
}

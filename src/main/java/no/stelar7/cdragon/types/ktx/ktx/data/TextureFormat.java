package no.stelar7.cdragon.types.ktx.ktx.data;

public enum TextureFormat
{
    // Uncompressed formats (texture format = pixel format).
    PIXEL_FORMAT_RGB8(true, true, "RGB8", "", 1, 1, 0x1907, 0x1907, 0x1401, "", 0),
    PIXEL_FORMAT_RGBA8(true, true, "RGBA8", "", 1, 1, 0x1908, 0x1908, 0x1401, "DX10", 28),
    PIXEL_FORMAT_R8(true, true, "R8", "", 1, 1, 0x8229, 0x1903, 0x1401, "DX10", 61),
    PIXEL_FORMAT_SIGNED_R8(true, true, "SIGNED_R8", "", 1, 1, 0x8F49, 0x1903, 0x1400, "DX10", 63),
    PIXEL_FORMAT_RG8(true, true, "RG8", "", 1, 1, 0x822B, 0x8227, 0x1401, "DX10", 49),
    PIXEL_FORMAT_SIGNED_RG8(true, true, "SIGNED_RG8", "", 1, 1, 0x8F95, 0x8227, 0x1400, "DX10", 51),
    PIXEL_FORMAT_R16(true, true, "R16", "", 1, 1, 0x822A, 0x1903, 0x1403, "DX10", 56),
    PIXEL_FORMAT_SIGNED_R16(true, true, "SIGNED_R16", "", 1, 1, 0x8F98, 0x1903, 0x1402, "DX10", 58),
    PIXEL_FORMAT_RG16(true, true, "RG16", "", 1, 1, 0x8226, 0x8227, 0x1403, "DX10", 35),
    PIXEL_FORMAT_SIGNED_RG16(true, true, "SIGNED_RG16", "", 1, 1, 0x8F99, 0x8227, 0x1402, "DX10", 37),
    PIXEL_FORMAT_RGB16(true, false, "RGB16", "", 1, 1, 0x8054, 0x1907, 0x1403, "", 0),
    PIXEL_FORMAT_RGBA16(true, true, "RGBA16", "", 1, 1, 0x805B, 0x8227, 0x1403, "DX10", 11),
    PIXEL_FORMAT_FLOAT_R16(true, true, "FLOAT_R16", "", 1, 1, 0x822D, 0x1903, 0x140B, "DX10", 54),
    PIXEL_FORMAT_FLOAT_RG16(true, true, "FLOAT_RG16", "", 1, 1, 0x822F, 0x8227, 0x140B, "DX10", 34),
    PIXEL_FORMAT_FLOAT_RGB16(true, false, "FLOAT_RGB16", "", 1, 1, 0x1907, 0x1907, 0x140B, "", 0),
    PIXEL_FORMAT_FLOAT_RGBA16(true, true, "FLOAT_RGBA16", "", 1, 1, 0x1908, 0x1908, 0x140B, "DX10", 10),
    PIXEL_FORMAT_FLOAT_R32(true, true, "FLOAT_R32", "", 1, 1, 0x822E, 0x1903, 0x1406, "DX10", 41),
    PIXEL_FORMAT_FLOAT_RG32(true, true, "FLOAT_RG32", "", 1, 1, 0x8230, 0x8227, 0x1406, "DX10", 16),
    PIXEL_FORMAT_FLOAT_RGB32(true, true, "FLOAT_RGB32", "", 1, 1, 0x8815, 0x1907, 0x1406, "DX10", 6),
    PIXEL_FORMAT_FLOAT_RGBA32(true, true, "FLOAT_RGBA32", "", 1, 1, 0x8814, 0x1908, 0x1406, "DX10", 2),
    PIXEL_FORMAT_A8(true, true, "A8", "", 1, 1, 0x1906, 0x1906, 0x1401, "DX10", 65),
    
    // Compressed formats
    TEXTURE_FORMAT_BC1(true, true, "BC1", "DXT1", 4, 4, 0x83F0, 0, 0, "DXT1", 0),
    TEXTURE_FORMAT_BC1A(true, true, "BC1A", "DXT1A", 4, 4, 0x83F1, 0, 0, "", 0),
    TEXTURE_FORMAT_BC2(true, true, "BC2", "DXT3", 4, 4, 0x83F2, 0, 0, "DXT3", 0),
    TEXTURE_FORMAT_BC3(true, true, "BC3", "DXT5", 4, 4, 0x83F3, 0, 0, "DXT5", 0),
    TEXTURE_FORMAT_RGTC1(true, true, "RGTC1", "BC4_UNORM", 4, 4, 0x8DBB, 0, 0, "DX10", 80),
    TEXTURE_FORMAT_SIGNED_RGTC1(true, true, "SIGNED_RGTC1", "BC4_SNORM", 4, 4, 0x8DBC, 0, 0, "DX10", 81),
    TEXTURE_FORMAT_RGTC2(true, true, "RGTC2", "BC5_UNORM", 4, 4, 0x8DBD, 0, 0, "DX10", 83),
    TEXTURE_FORMAT_SIGNED_RGTC2(true, true, "SIGNED_RGTC2", "BC5_SNORM", 4, 4, 0x8DBE, 0, 0, "DX10", 84),
    TEXTURE_FORMAT_BPTC_FLOAT(true, true, "BPTC_FLOAT", "BC6H_UF16", 4, 4, 0x8E8F, 0, 0, "DX10", 95),
    TEXTURE_FORMAT_BPTC_SIGNED_FLOAT(true, true, "BPTC_SIGNED_FLOAT", "BC6H_SF16", 4, 4, 0x8E8E, 0, 0, "DX10", 96),
    TEXTURE_FORMAT_BPTC(true, true, "BPTC", "BC7", 4, 4, 0x8E8C, 0, 0, "DX10", 98),
    TEXTURE_FORMAT_ETC1(true, false, "ETC1", "", 4, 4, 0x8D64, 0, 0, "", 0),
    TEXTURE_FORMAT_ETC2(true, false, "ETC2", "ETC2_RGB8", 4, 4, 0x9274, 0, 0, "", 0),
    TEXTURE_FORMAT_ETC2_PUNCHTHROUGH(true, false, "ETC2_PUNCHTHROUGH", "", 4, 4, 0x9275, 0, 0, "", 0),
    TEXTURE_FORMAT_ETC2_EAC(true, false, "ETC2_EAC", "EAC", 4, 4, 0x9278, 0, 0, "", 0),
    TEXTURE_FORMAT_EAC_R11(true, false, "EAC_R11", "", 4, 4, 0x9270, 0, 0, "", 0),
    TEXTURE_FORMAT_EAC_SIGNED_R11(true, false, "EAC_SIGNED_R11", "", 4, 4, 0x9271, 0, 0, "", 0),
    TEXTURE_FORMAT_EAC_RG11(true, false, "EAC_RG11", "", 4, 4, 0x9272, 0, 0, "", 0),
    TEXTURE_FORMAT_EAC_SIGNED_RG11(true, false, "EAC_SIGNED_RG11", "", 4, 4, 0x9273, 0, 0, "", 0),
    TEXTURE_FORMAT_ETC2_SRGB8(true, false, "SRGB_ETC2", "", 4, 4, 0x9275, 0, 0, "", 0),
    TEXTURE_FORMAT_ETC2_SRGB_EAC(true, false, "SRGB_ETC2_EAC", "", 4, 4, 0x9279, 0, 0, "", 0),
    TEXTURE_FORMAT_ETC2_SRGB_PUNCHTHROUGH(true, false, "SRGB_ETC2_PUNCHTHROUGH", "", 4, 4, 0x9277, 0, 0, "", 0),
    TEXTURE_FORMAT_ASTC_4X4(true, false, "ASTC_4x4", "", 4, 4, 0x93B0, 0, 0, "DX10", 134),
    TEXTURE_FORMAT_RGBA_ASTC_5X4(true, false, "astc_5x4", "", 5, 4, 0x93B1, 0, 0, "", 0),
    TEXTURE_FORMAT_RGBA_ASTC_5X5(true, false, "astc_5x5", "", 5, 5, 0x93B2, 0, 0, "", 0),
    TEXTURE_FORMAT_RGBA_ASTC_6X5(true, false, "astc_6x4", "", 6, 5, 0x93B3, 0, 0, "", 0),
    TEXTURE_FORMAT_RGBA_ASTC_6X6(true, false, "astc_6x6", "", 6, 6, 0x93B4, 0, 0, "", 0),
    TEXTURE_FORMAT_RGBA_ASTC_8X5(true, false, "astc_8x5", "", 8, 5, 0x93B5, 0, 0, "", 0),
    TEXTURE_FORMAT_RGBA_ASTC_8X6(true, false, "astc_8x6", "", 8, 6, 0x93B6, 0, 0, "", 0),
    TEXTURE_FORMAT_RGBA_ASTC_8X8(true, false, "astc_8x8", "", 8, 8, 0x93B7, 0, 0, "", 0),
    TEXTURE_FORMAT_RGBA_ASTC_10X5(true, false, "astc_10x5", "", 10, 5, 0x93B8, 0, 0, "", 0),
    TEXTURE_FORMAT_RGBA_ASTC_10X6(true, false, "astc_10x6", "", 10, 6, 0x93B9, 0, 0, "", 0),
    TEXTURE_FORMAT_RGBA_ASTC_10X8(true, false, "astc_10x8", "", 10, 8, 0x93BA, 0, 0, "", 0),
    TEXTURE_FORMAT_RGBA_ASTC_10X10(true, false, "astc_10x10", "", 10, 10, 0x93BB, 0, 0, "", 0),
    TEXTURE_FORMAT_RGBA_ASTC_12X10(true, false, "astc_12x10", "", 12, 10, 0x93BC, 0, 0, "", 0),
    TEXTURE_FORMAT_RGBA_ASTC_12X12(true, false, "astc_12x12", "", 12, 12, 0x93BD, 0, 0, "", 0),
    
    // Pseudo-formats (not present in files, but used for name look-up).
    PIXEL_FORMAT_RGBX8(false, false, "RGBX8", "", 1, 1, 0, 0, 0, "", 0),
    PIXEL_FORMAT_BGRX8(false, false, "BGRX8", "", 1, 1, 0, 0, 0, "", 0),
    PIXEL_FORMAT_FLOAT_RGBX16(false, false, "FLOAT_RGBX16", "", 1, 1, 0, 0, 0, "", 0),
    PIXEL_FORMAT_FLOAT_BGRX16(false, false, "FLOAT_BGRX16", "", 1, 1, 0, 0, 0, "", 0),
    PIXEL_FORMAT_FLOAT_R16_HDR(false, false, "FLOAT_R16_HDR", "", 1, 1, 0, 0, 0, "", 0),
    PIXEL_FORMAT_FLOAT_RG16_HDR(false, false, "FLOAT_RG16_HDR", "", 1, 1, 0, 0, 0, "", 0),
    PIXEL_FORMAT_FLOAT_RGB16_HDR(false, false, "FLOAT_RGB16_HDR", "", 1, 1, 0, 0, 0, "", 0),
    PIXEL_FORMAT_FLOAT_RGBA16_HDR(false, false, "FLOAT_RGBA16_HDR", "", 1, 1, 0, 0, 0, "", 0),
    PIXEL_FORMAT_FLOAT_R32_HDR(false, false, "FLOAT_R32_HDR", "", 1, 1, 0, 0, 0, "", 0),
    PIXEL_FORMAT_FLOAT_RG32_HDR(false, false, "FLOAT_RG32_HDR", "", 1, 1, 0, 0, 0, "", 0),
    PIXEL_FORMAT_FLOAT_RGB32_HDR(false, false, "FLOAT_RGB32_HDR", "", 1, 1, 0, 0, 0, "", 0),
    PIXEL_FORMAT_FLOAT_RGBA32_HDR(false, false, "FLOAT_RGBA32_HDR", "", 1, 1, 0, 0, 0, "", 0),
    ;
    
    
    boolean ktxSupport;
    boolean ddsSupport;
    String  identifier;
    String  altIdentifier;
    int     blockWidth;
    int     blockHeight;
    int     glInternalFormat;
    int     glFormat;
    int     glType;
    String  dxFourCC;
    int     dx10Format;
    
    TextureFormat(boolean ktxSupport, boolean ddsSupport, String identifier, String altIdentifier, int blockWidth, int blockHeight, int glInternalFormat, int glFormat, int glType, String dxFourCC, int dx10Format)
    {
        this.ktxSupport = ktxSupport;
        this.ddsSupport = ddsSupport;
        this.identifier = identifier;
        this.altIdentifier = altIdentifier;
        this.blockWidth = blockWidth;
        this.blockHeight = blockHeight;
        this.glInternalFormat = glInternalFormat;
        this.glFormat = glFormat;
        this.glType = glType;
        this.dxFourCC = dxFourCC;
        this.dx10Format = dx10Format;
    }
}



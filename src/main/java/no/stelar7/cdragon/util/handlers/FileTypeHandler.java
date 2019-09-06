package no.stelar7.cdragon.util.handlers;

import com.google.gson.*;
import no.stelar7.cdragon.types.filemanifest.ManifestContentParser;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class FileTypeHandler
{
    
    private FileTypeHandler()
    {
        // Hide public constructor
    }
    
    public static String findFileType(ByteArray magic)
    {
        if (magic.size() == 0)
        {
            return "empty";
        }
        
        if (FileTypeHandler.isProbableBOM(magic))
        {
            return findFileType(magic.copyOfRange(3, magic.getData().length));
        }
        
        if (!(magic.getData().length > 3))
        {
            if (isProbableJSON(magic))
            {
                return "json";
            }
            
            return "txt";
        }
        
        if (FileTypeHandler.isProbableSofdec(magic))
        {
            return "sfd";
        }
        
        if (FileTypeHandler.isProbableJavascript(magic))
        {
            return "js";
        }
        
        if (FileTypeHandler.isProbableHTML(magic))
        {
            return "html";
        }
        
        if (FileTypeHandler.isProbableCSS(magic))
        {
            return "css";
        }
        
        if (FileTypeHandler.isProbableSKL(magic))
        {
            return "skl";
        }
        
        if (FileTypeHandler.isProbableSCB(magic))
        {
            return "scb";
        }
        
        if (FileTypeHandler.isProbableANM(magic))
        {
            return "anm";
        }
        
        if (FileTypeHandler.isProbableBNK(magic))
        {
            return "bnk";
        }
        
        if (FileTypeHandler.isProbableTGA(magic))
        {
            return "tga";
        }
        
        if (FileTypeHandler.isProbableSCO(magic))
        {
            return "sco";
        }
        if (FileTypeHandler.isProbablePRELOAD(magic))
        {
            return "preload";
        }
        
        if (FileTypeHandler.isProbableKTX(magic))
        {
            // OpenGL ES shaders
            return "ktx";
        }
        
        if (FileTypeHandler.isProbableGLSLF(magic))
        {
            //  OpenGL fragment profile
            return "glsl-f";
        }
        
        if (FileTypeHandler.isProbableGLSLV(magic))
        {
            // OpenGL vertex profile
            return "glsl-v";
        }
        
        if (FileTypeHandler.isProbableCSO(magic))
        {
            // Compiled Shader Object
            return "dx9-cso";
        }
        
        if (FileTypeHandler.isProbableCGC(magic))
        {
            // Compiled Shader Config
            return "dx9-cgc";
        }
        
        if (FileTypeHandler.isProbableWPK(magic))
        {
            return "wpk";
        }
        
        String result = FileTypeHandler.getMagicNumbers().get(magic.copyOfRange(0, 4));
        if (result != null)
        {
            return result;
        }
        
        if (FileTypeHandler.isProbablePATCH(magic))
        {
            return "bin";
        }
        
        if (FileTypeHandler.isProbableWGEO(magic))
        {
            return "wgeo";
        }
        
        if (FileTypeHandler.isProbableNVR(magic))
        {
            return "nvr";
        }
        
        if (FileTypeHandler.isProbableELF(magic))
        {
            return "elf";
        }
        
        if (FileTypeHandler.isProbableMOB(magic))
        {
            return "mob";
        }
        
        if (FileTypeHandler.isProbableTROYBIN(magic))
        {
            return "troybin";
        }
        
        if (FileTypeHandler.isProbableJSON(magic))
        {
            return "json";
        }
        
        if (FileTypeHandler.isProbableHLS(magic))
        {
            return "hls";
        }
        
        if (FileTypeHandler.isProbableManifestV2(magic))
        {
            return "manifestv2";
        }
        
        if (FileTypeHandler.isProbableManifestV1(magic))
        {
            return "manifestv1";
        }
        
        if (FileTypeHandler.isProbableManifestV0(magic))
        {
            return "manifestv0";
        }
        
        if (FileTypeHandler.isProbableTXT(magic))
        {
            return "txt";
        }
        
        return "unknown";
    }
    
    private static boolean isProbableManifestV0(ByteArray magic)
    {
        if (magic.size() > 4)
        {
            try
            {
                new ManifestContentParser().parseV0(magic);
                return true;
            } catch (Exception e)
            {
                return false;
            }
        }
        
        return false;
    }
    
    private static boolean isProbableManifestV1(ByteArray magic)
    {
        if (magic.size() > 4)
        {
            try
            {
                new ManifestContentParser().parseV1(magic);
                return true;
            } catch (Exception e)
            {
                return false;
            }
        }
        
        return false;
    }
    
    private static boolean isProbableManifestV2(ByteArray magic)
    {
        if (magic.size() > 4)
        {
            try
            {
                new ManifestContentParser().parseV2(magic);
                return true;
            } catch (Exception e)
            {
                return false;
            }
        }
        
        return false;
    }
    
    private static boolean isProbableHLS(ByteArray magic)
    {
        boolean probableShader = false;
        String  content        = new String(magic.getData());
        probableShader |= content.contains("#include ");
        probableShader |= content.contains("void main(VERTEX");
        probableShader |= content.contains("// Shader");
        
        return probableShader;
    }
    
    private static boolean isProbableGLSLV(ByteArray magic)
    {
        return new String(magic.getData()).contains("// glslv output by Cg compiler");
    }
    
    private static boolean isProbableGLSLF(ByteArray magic)
    {
        return new String(magic.getData()).contains("// glslf output by Cg compiler");
    }
    
    private static boolean isProbableKTX(ByteArray magic)
    {
        return magic.startsWith(new ByteArray(new byte[]{(byte) 0xAB, 0x4B, 0x54, 0x58, 0x20, 0x31, 0x31, (byte) 0xBB, 0x0D, 0x0A, 0x1A, 0x0A}));
    }
    
    // Compiled Shader Object
    private static boolean isProbableCSO(ByteArray magic)
    {
        return new String(magic.getData()).contains("Microsoft (R) HLSL Shader Compiler");
    }
    
    // Compiled Shader Config
    private static boolean isProbableCGC(ByteArray magic)
    {
        return magic.startsWith(new ByteArray(new byte[]{(byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00}));
    }
    
    public static byte[] makePrettyJson(byte[] jsonString)
    {
        StringBuilder sb = new StringBuilder(new String(jsonString, StandardCharsets.UTF_8));
        String        dataString;
        
        String rev = sb.reverse().toString().replaceAll("\n", "");
        if (rev.charAt(1) == ',')
        {
            dataString = new StringBuilder(rev).replace(1, 2, "").reverse().toString();
        } else
        {
            sb.reverse();
            dataString = sb.toString();
        }
        
        try
        {
            JsonElement obj    = new JsonParser().parse(dataString);
            String      pretty = UtilHandler.getGson().toJson(obj);
            return pretty.getBytes(StandardCharsets.UTF_8);
        } catch (JsonSyntaxException e)
        {
            return jsonString;
        }
    }
    
    private static Map<ByteArray, String> magicNumbers = new HashMap<>();
    
    public static Map<ByteArray, String> getMagicNumbers()
    {
        if (magicNumbers.isEmpty())
        {
            System.out.println("Loading magic numbers");
            
            ByteArray oggMagic  = new ByteArray(new byte[]{(byte) 0x4f, (byte) 0x67, (byte) 0x67, (byte) 0x53});
            ByteArray webmMagic = new ByteArray(new byte[]{(byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3});
            ByteArray ddsMagic  = new ByteArray(new byte[]{(byte) 0x44, (byte) 0x44, (byte) 0x53, (byte) 0x20});
            ByteArray pngMagic  = new ByteArray(new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47});
            ByteArray jpgMagic  = new ByteArray(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
            ByteArray jpg2Magic = new ByteArray(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1});
            ByteArray jpg3Magic = new ByteArray(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xEC});
            ByteArray jpg4Magic = new ByteArray(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB});
            ByteArray bnkMagic  = new ByteArray(new byte[]{(byte) 0x42, (byte) 0x4B, (byte) 0x48, (byte) 0x44});
            ByteArray binMagic  = new ByteArray(new byte[]{(byte) 0x50, (byte) 0x52, (byte) 0x4F, (byte) 0x50});
            ByteArray lcovMagic = new ByteArray(new byte[]{(byte) 0x54, (byte) 0x4E, (byte) 0x3A, (byte) 0x0A});
            ByteArray gifMagic  = new ByteArray(new byte[]{(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38});
            ByteArray zipMagic  = new ByteArray(new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04});
            ByteArray ttfMagic  = new ByteArray(new byte[]{(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00});
            ByteArray otfMagic  = new ByteArray(new byte[]{(byte) 0x4F, (byte) 0x54, (byte) 0x54, (byte) 0x4F});
            ByteArray sknMagic  = new ByteArray(new byte[]{(byte) 0x33, (byte) 0x22, (byte) 0x11, (byte) 0x00});
            ByteArray objMagic  = new ByteArray(new byte[]{(byte) 0x5B, (byte) 0x4F, (byte) 0x62, (byte) 0x6A});
            ByteArray unkMagic  = new ByteArray(new byte[]{(byte) 0x74, (byte) 0x22, (byte) 0x00, (byte) 0x00});
            ByteArray luaMagic  = new ByteArray(new byte[]{(byte) 0x1B, (byte) 0x4C, (byte) 0x75, (byte) 0x61});
            ByteArray hlslMagic = new ByteArray(new byte[]{(byte) 0x23, (byte) 0x70, (byte) 0x72, (byte) 0x61});
            ByteArray oegmMagic = new ByteArray(new byte[]{(byte) 0x4F, (byte) 0x45, (byte) 0x47, (byte) 0x4D});
            ByteArray fcnfMagic = new ByteArray(new byte[]{(byte) 0x5B, (byte) 0x46, (byte) 0x6F, (byte) 0x6E});
            
            
            magicNumbers = new HashMap<>();
            // Sound
            magicNumbers.put(oggMagic, "ogg");
            
            // Video
            magicNumbers.put(webmMagic, "webm");
            
            // Image
            magicNumbers.put(pngMagic, "png");
            magicNumbers.put(jpgMagic, "jpg");
            magicNumbers.put(jpg2Magic, "jpg");
            magicNumbers.put(jpg3Magic, "jpg");
            magicNumbers.put(jpg4Magic, "jpg");
            magicNumbers.put(gifMagic, "gif");
            
            // Fonts
            magicNumbers.put(ttfMagic, "ttf");
            magicNumbers.put(otfMagic, "otf");
            
            // Div
            magicNumbers.put(zipMagic, "zip");
            magicNumbers.put(luaMagic, "luaobj");
            magicNumbers.put(hlslMagic, "hlsl");
            
            // 3D model
            magicNumbers.put(bnkMagic, "bnk");
            magicNumbers.put(ddsMagic, "dds");
            magicNumbers.put(binMagic, "bin");
            magicNumbers.put(lcovMagic, "info");
            magicNumbers.put(sknMagic, "skn");
            magicNumbers.put(objMagic, "obj");
            
            // i dont know...?
            magicNumbers.put(unkMagic, "unk");
            magicNumbers.put(oegmMagic, "oegm");
            magicNumbers.put(fcnfMagic, "fcnf");
        }
        
        return magicNumbers;
    }
    
    private static boolean isSame(byte a, byte b)
    {
        return a == b;
    }
    
    //<editor-fold desc="Decent-ish byte comparisons">
    public static boolean isProbableDEFLATE(byte[] data)
    {
        boolean isNoCompress      = isSame(data[0], (byte) 0x78) && isSame(data[1], (byte) 0x01);
        boolean isBestCompress    = isSame(data[0], (byte) 0x78) && isSame(data[1], (byte) 0xDA);
        boolean isDefaultCompress = isSame(data[0], (byte) 0x78) && isSame(data[1], (byte) 0x9C);
        return isNoCompress || isBestCompress || isDefaultCompress;
    }
    
    public static boolean isProbableGZIP(byte[] data)
    {
        return isSame(data[0], (byte) 0x1f) && isSame(data[1], (byte) 0x8b);
    }
    
    public static boolean isProbableZSTD(byte[] data)
    {
        return isSame(data[0], (byte) 0x28) && isSame(data[1], (byte) 0xB5) && isSame(data[2], (byte) 0x2F) && isSame(data[3], (byte) 0xFD);
    }
    
    public static boolean isProbableBOM(ByteArray wrapper)
    {
        byte[]  data       = wrapper.getData();
        boolean isUTF8BOM  = isSame(data[0], (byte) 0xEF) && isSame(data[1], (byte) 0xBB) && isSame(data[2], (byte) 0xBF);
        boolean isUTF16BOM = isSame(data[0], (byte) 0xFE) && isSame(data[1], (byte) 0xFF);
        boolean isUTF32BOM = isSame(data[0], (byte) 0x00) && isSame(data[1], (byte) 0x00) && isSame(data[2], (byte) 0xFE) && isSame(data[3], (byte) 0xFF);
        
        return isUTF8BOM || isUTF16BOM || isUTF32BOM;
    }
    //</editor-fold>
    
    
    //<editor-fold desc="Shitty byte-comparisons, need to fix..">
    
    public static boolean isProbableELF(ByteArray magic)
    {
        return magic.indexMatch(0, (byte) 0x07) && magic.indexMatch(1, (byte) 0x01) && magic.indexMatch(2, (byte) 0x00);
    }
    
    public static boolean isProbableNVR(ByteArray magic)
    {
        return magic.indexMatch(0, (byte) 0x4E) && magic.indexMatch(1, (byte) 0x56) && magic.indexMatch(2, (byte) 0x52) && magic.indexMatch(3, (byte) 0x00);
    }
    
    public static boolean isProbableWGEO(ByteArray magic)
    {
        return magic.indexMatch(0, (byte) 0x57) && magic.indexMatch(1, (byte) 0x47) && magic.indexMatch(2, (byte) 0x45) && magic.indexMatch(3, (byte) 0x4F);
    }
    
    public static boolean isProbableMOB(ByteArray magic)
    {
        return magic.indexMatch(0, (byte) 0x4F) && magic.indexMatch(1, (byte) 0x50) && magic.indexMatch(2, (byte) 0x41) && magic.indexMatch(3, (byte) 0x4D);
    }
    
    public static boolean isProbablePATCH(ByteArray magic)
    {
        return magic.indexMatch(0, (byte) 0x50) && magic.indexMatch(1, (byte) 0x54) && magic.indexMatch(2, (byte) 0x43) && magic.indexMatch(3, (byte) 0x48);
    }
    
    public static boolean isProbableTROYBIN(ByteArray magic)
    {
        return magic.indexMatch(0, (byte) 0x02);
    }
    
    
    public static boolean isProbableTGA(ByteArray magic)
    {
        return magic.endsWith(new ByteArray(new byte[]{0x54, 0x52, 0x55, 0x45, 0x56, 0x49, 0x53, 0x49, 0x4f, 0x4e, 0x2d, 0x58, 0x46, 0x49, 0x4c, 0x45, 0x2e, 0x00})) ||
               (magic.indexMatch(0, (byte) 0x00) &&
                magic.indexMatch(1, (byte) 0x00) &&
                magic.indexMatch(2, (byte) 0x0A) &&
                magic.indexMatch(3, (byte) 0x00));
    }
    
    public static boolean isProbableJSON(ByteArray wrapper)
    {
        return (wrapper.indexMatch(0, (byte) 0x7B) || wrapper.indexMatch(0, (byte) 0x5B));
    }
    
    public static boolean isProbableCSS(ByteArray wrapper)
    {
        byte[]  data  = wrapper.getData();
        boolean isCSS = isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x62) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x6F);
        
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x70) && isSame(data[2], (byte) 0x6C) && isSame(data[3], (byte) 0x6E);
        isCSS |= isSame(data[0], (byte) 0x62) && isSame(data[1], (byte) 0x6F) && isSame(data[2], (byte) 0x64) && isSame(data[3], (byte) 0x79);
        isCSS |= isSame(data[0], (byte) 0x2F) && isSame(data[1], (byte) 0x2A) && isSame(data[2], (byte) 0x40) && isSame(data[3], (byte) 0x69);
        isCSS |= isSame(data[0], (byte) 0x73) && isSame(data[1], (byte) 0x70) && isSame(data[2], (byte) 0x61) && isSame(data[3], (byte) 0x6E);
        isCSS |= isSame(data[0], (byte) 0x40) && isSame(data[1], (byte) 0x69) && isSame(data[2], (byte) 0x6D) && isSame(data[3], (byte) 0x70);
        isCSS |= isSame(data[0], (byte) 0x2F) && isSame(data[1], (byte) 0x2A) && isSame(data[2], (byte) 0x20) && isSame(data[3], (byte) 0x53);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x68) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x6E);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x6C) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x79);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x63) && isSame(data[2], (byte) 0x68) && isSame(data[3], (byte) 0x65);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x72) && isSame(data[2], (byte) 0x65) && isSame(data[3], (byte) 0x70);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x75) && isSame(data[2], (byte) 0x70) && isSame(data[3], (byte) 0x64);
        isCSS |= isSame(data[0], (byte) 0x40) && isSame(data[1], (byte) 0x66) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x6E);
        isCSS |= isSame(data[0], (byte) 0x0A) && isSame(data[1], (byte) 0x3A) && isSame(data[2], (byte) 0x72) && isSame(data[3], (byte) 0x6F);
        isCSS |= isSame(data[0], (byte) 0x3A) && isSame(data[1], (byte) 0x72) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x6F);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x6C) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x6C);
        isCSS |= isSame(data[0], (byte) 0x64) && isSame(data[1], (byte) 0x69) && isSame(data[2], (byte) 0x76) && isSame(data[3], (byte) 0x5B);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x72) && isSame(data[2], (byte) 0x63) && isSame(data[3], (byte) 0x70);
        isCSS |= isSame(data[0], (byte) 0x20) && isSame(data[1], (byte) 0x2E) && isSame(data[2], (byte) 0x70) && isSame(data[3], (byte) 0x6C);
        
        return isCSS;
    }
    
    public static boolean isProbableJavascript(ByteArray wrapper)
    {
        byte[]  data = wrapper.getData();
        boolean isJS = isSame(data[0], (byte) 0x21) && isSame(data[1], (byte) 0x66) && isSame(data[2], (byte) 0x75) && isSame(data[3], (byte) 0x6E);
        
        isJS |= isSame(data[0], (byte) 0x77) && isSame(data[1], (byte) 0x65) && isSame(data[2], (byte) 0x62) && isSame(data[3], (byte) 0x70);
        isJS |= isSame(data[0], (byte) 0x76) && isSame(data[1], (byte) 0x61) && isSame(data[2], (byte) 0x72) && isSame(data[3], (byte) 0x20);
        isJS |= isSame(data[0], (byte) 0x77) && isSame(data[1], (byte) 0x69) && isSame(data[2], (byte) 0x6E) && isSame(data[3], (byte) 0x64);
        isJS |= isSame(data[0], (byte) 0x22) && isSame(data[1], (byte) 0x75) && isSame(data[2], (byte) 0x73) && isSame(data[3], (byte) 0x65);
        isJS |= isSame(data[0], (byte) 0x50) && isSame(data[1], (byte) 0x72) && isSame(data[2], (byte) 0x65) && isSame(data[3], (byte) 0x4C);
        isJS |= isSame(data[0], (byte) 0x5F) && isSame(data[1], (byte) 0x4E) && isSame(data[2], (byte) 0x41) && isSame(data[3], (byte) 0x4D);
        
        return isJS;
    }
    
    public static boolean isProbableHTML(ByteArray wrapper)
    {
        byte[]  data   = wrapper.getData();
        boolean isHTML = isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x73) && isSame(data[2], (byte) 0x63) && isSame(data[3], (byte) 0x72);
        
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x21) && isSame(data[2], (byte) 0x64) && isSame(data[3], (byte) 0x6F);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x6C) && isSame(data[2], (byte) 0x69) && isSame(data[3], (byte) 0x6E);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x3F) && isSame(data[2], (byte) 0x78) && isSame(data[3], (byte) 0x6D);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x74) && isSame(data[2], (byte) 0x65) && isSame(data[3], (byte) 0x6D);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x6C) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x6C);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x73) && isSame(data[2], (byte) 0x76) && isSame(data[3], (byte) 0x67);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x21) && isSame(data[2], (byte) 0x2D) && isSame(data[3], (byte) 0x2D);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x64) && isSame(data[2], (byte) 0x69) && isSame(data[3], (byte) 0x76);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x69) && isSame(data[2], (byte) 0x6D) && isSame(data[3], (byte) 0x67);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x68) && isSame(data[2], (byte) 0x74) && isSame(data[3], (byte) 0x6D);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x73) && isSame(data[2], (byte) 0x74) && isSame(data[3], (byte) 0x79);
        isHTML |= isSame(data[0], (byte) 0x7B) && isSame(data[1], (byte) 0x7B) && isSame(data[2], (byte) 0x23) && isSame(data[3], (byte) 0x75);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x68) && isSame(data[2], (byte) 0x65) && isSame(data[3], (byte) 0x61);
        
        return isHTML;
    }
    
    private static List<ByteArray> possibleTextTargets = loadTextTargets();
    
    public static boolean isProbableTXT(ByteArray wrapper)
    {
        byte[]    data        = wrapper.getData();
        ByteArray checkTarget = new ByteArray(Arrays.copyOf(data, 3));
        boolean   isTXT       = new String(data, StandardCharsets.UTF_8).isEmpty();
        
        return isTXT || possibleTextTargets.contains(checkTarget);
    }
    
    private static List<ByteArray> loadTextTargets()
    {
        List<ByteArray> list = new ArrayList<>();
        
        byte[] magicText3Wide = {
                (byte) 0x63, (byte) 0x60, (byte) 0x63, (byte) 0x21, (byte) 0x30, (byte) 0x31,
                (byte) 0x61, (byte) 0x73, (byte) 0x73, (byte) 0x68, (byte) 0x70, (byte) 0x0d,
                (byte) 0x32, (byte) 0x67, (byte) 0x69, (byte) 0x70, (byte) 0x6f, (byte) 0x0f,
                (byte) 0x62, (byte) 0x65, (byte) 0x6e, (byte) 0x70, (byte) 0x6f, (byte) 0x73,
                (byte) 0x63, (byte) 0x61, (byte) 0x63, (byte) 0x62, (byte) 0x6c, (byte) 0x6f,
                (byte) 0x34, (byte) 0x6d, (byte) 0x6f, (byte) 0x63, (byte) 0x75, (byte) 0x6d,
                (byte) 0x70, (byte) 0x6f, (byte) 0x6e, (byte) 0x72, (byte) 0x6f, (byte) 0x61,
                (byte) 0xea, (byte) 0xb0, (byte) 0x80, (byte) 0x6a, (byte) 0x61, (byte) 0x0d,
                (byte) 0x6c, (byte) 0x80, (byte) 0x3c, (byte) 0x61, (byte) 0x6c, (byte) 0x6c,
                (byte) 0x67, (byte) 0x61, (byte) 0x72, (byte) 0x72, (byte) 0x69, (byte) 0x6f,
                (byte) 0x62, (byte) 0x69, (byte) 0x6d, (byte) 0x72, (byte) 0x65, (byte) 0x67,
                (byte) 0x21, (byte) 0x69, (byte) 0x0d, (byte) 0x21, (byte) 0x20, (byte) 0x22,
                (byte) 0x61, (byte) 0x6d, (byte) 0x0d, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xc3, (byte) 0x80, (byte) 0xc3, (byte) 0x61, (byte) 0x6d, (byte) 0x0d,
                (byte) 0x62, (byte) 0x61, (byte) 0x6e, (byte) 0x61, (byte) 0x73, (byte) 0x61,
                (byte) 0x63, (byte) 0x6f, (byte) 0x77, (byte) 0x63, (byte) 0x64, (byte) 0x63,
                (byte) 0x61, (byte) 0x72, (byte) 0x73, (byte) 0x62, (byte) 0x6f, (byte) 0x62,
                (byte) 0x70, (byte) 0x65, (byte) 0x64, (byte) 0x63, (byte) 0x38, (byte) 0x0d,
                (byte) 0x6d, (byte) 0x61, (byte) 0x64, (byte) 0x6b, (byte) 0x75, (byte) 0x6e,
                (byte) 0x73, (byte) 0x6c, (byte) 0x75, (byte) 0x36, (byte) 0x39, (byte) 0x0d,
                (byte) 0x62, (byte) 0x64, (byte) 0x6d, (byte) 0x70, (byte) 0x69, (byte) 0x73,
                (byte) 0x36, (byte) 0x39, (byte) 0x0d, (byte) 0x61, (byte) 0x63, (byte) 0x63,
                (byte) 0x62, (byte) 0x61, (byte) 0x6b, (byte) 0xc4, (byte) 0x8d, (byte) 0x75,
                (byte) 0x21, (byte) 0x20, (byte) 0x27, (byte) 0x30, (byte) 0x31, (byte) 0x32,
                (byte) 0x61, (byte) 0x64, (byte) 0x6d, (byte) 0x37, (byte) 0x33, (byte) 0x37,
                (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x2d, (byte) 0xe8, (byte) 0x83,
                (byte) 0x62, (byte) 0x6f, (byte) 0x6d, (byte) 0x63, (byte) 0x75, (byte) 0x0d,
                (byte) 0x62, (byte) 0x75, (byte) 0x67, (byte) 0x62, (byte) 0x79, (byte) 0x6d,
                (byte) 0x30, (byte) 0x6f, (byte) 0x0d, (byte) 0x30, (byte) 0x6f, (byte) 0x0d,
                (byte) 0x72, (byte) 0x65, (byte) 0x63, (byte) 0x6c, (byte) 0x6f, (byte) 0x6f,
                (byte) 0x54, (byte) 0x45, (byte) 0x52, (byte) 0x45, (byte) 0x4e, (byte) 0x44,
                (byte) 0x45, (byte) 0x6e, (byte) 0x64, (byte) 0x45, (byte) 0x6c, (byte) 0x20,
                (byte) 0x54, (byte) 0x45, (byte) 0x52, (byte) 0xd0, (byte) 0xa3, (byte) 0xd0,
                (byte) 0xd0, (byte) 0x9b, (byte) 0xd0, (byte) 0xec, (byte) 0x8b, (byte) 0x9c,
                (byte) 0x43, (byte) 0x6f, (byte) 0x6e, (byte) 0x09, (byte) 0x0d, (byte) 0x0a,
                (byte) 0x4c, (byte) 0x45, (byte) 0x41, (byte) 0xe3, (byte) 0x82, (byte) 0xa2,
                (byte) 0x6c, (byte) 0x65, (byte) 0x73, (byte) 0x37, (byte) 0x31, (byte) 0x37,
                (byte) 0x61, (byte) 0x34, (byte) 0x75, (byte) 0x40, (byte) 0x21, (byte) 0x40,
                (byte) 0xd1, (byte) 0x85, (byte) 0xd1, (byte) 0xe3, (byte) 0x84, (byte) 0x85,
                (byte) 0x31, (byte) 0x6d, (byte) 0x65, (byte) 0x20, (byte) 0x21, (byte) 0x23,
                (byte) 0x28, (byte) 0x29, (byte) 0x2a, (byte) 0x73, (byte) 0x69, (byte) 0x6b,
                (byte) 0xe6, (byte) 0x8b, (byte) 0xa6, (byte) 0x70, (byte) 0x65, (byte) 0x72,
                (byte) 0xe3, (byte) 0x83, (byte) 0xaa, (byte) 0x74, (byte) 0x75, (byte) 0x72,
                (byte) 0x44, (byte) 0x41, (byte) 0x54, (byte) 0xe2, (byte) 0x94, (byte) 0x80,
                (byte) 0x61, (byte) 0x61, (byte) 0x74, (byte) 0xeb, (byte) 0xa6, (byte) 0xac,
                (byte) 0x66, (byte) 0x6c, (byte) 0x6f, (byte) 0x23, (byte) 0x69, (byte) 0x6e,
                (byte) 0x2f, (byte) 0x2f, (byte) 0x20, (byte) 0x23, (byte) 0x23, (byte) 0x23,
                (byte) 0x44, (byte) 0x72, (byte) 0x69, (byte) 0x53, (byte) 0x52, (byte) 0x55,
                (byte) 0x41, (byte) 0x49, (byte) 0x50, (byte) 0x0d, (byte) 0x0a, (byte) 0x5b,
                (byte) 0x2f, (byte) 0x2f, (byte) 0x41, (byte) 0x65, (byte) 0x6e, (byte) 0x76,
                (byte) 0x2f, (byte) 0x2f, (byte) 0x2f, (byte) 0x63, (byte) 0x6f, (byte) 0x6c,
                (byte) 0x3b, (byte) 0x3b, (byte) 0x3b, (byte) 0x54, (byte) 0x75, (byte) 0x72,
                (byte) 0x54, (byte) 0x54, (byte) 0x5f, (byte) 0x63, (byte) 0x68, (byte) 0x61,
                (byte) 0x4c, (byte) 0x65, (byte) 0x76, (byte) 0x6f, (byte) 0x72, (byte) 0x64,
                (byte) 0x63, (byte) 0x70, (byte) 0x5f, (byte) 0x2e, (byte) 0x6c, (byte) 0x6f,
                (byte) 0x61, (byte) 0x6e, (byte) 0x61, (byte) 0x61, (byte) 0x66, (byte) 0x75,
                (byte) 0x65, (byte) 0x6c, (byte) 0x69, (byte) 0x61, (byte) 0x62, (byte) 0x6f,
                (byte) 0x3f, (byte) 0x61, (byte) 0x72, (byte) 0x61, (byte) 0x62, (byte) 0x73,
                (byte) 0x61, (byte) 0x62, (byte) 0x61, (byte) 0x61, (byte) 0x62, (byte) 0x62,
                (byte) 0x61, (byte) 0x6c, (byte) 0x6a, (byte) 0x0d, (byte) 0x0a, (byte) 0x0d,
                (byte) 0x73, (byte) 0x72, (byte) 0x75, (byte) 0xe8, (byte) 0x94, (byte) 0xa1,
                (byte) 0x67, (byte) 0x61, (byte) 0x73, (byte) 0x65, (byte) 0x73, (byte) 0x74,
                };
        
        
        for (int i = 0; i < magicText3Wide.length; i += 3)
        {
            byte[] temp = Arrays.copyOfRange(magicText3Wide, i, i + 3);
            list.add(new ByteArray(temp));
        }
        
        return list;
    }
    
    public static boolean isProbable3DModelStuff(ByteArray wrapper)
    {
        return wrapper.indexMatch(2, (byte) 0x00) && wrapper.indexMatch(3, (byte) 0x00);
    }
    
    public static boolean isProbableSCB(ByteArray wrapper)
    {
        return wrapper.equals(new ByteArray("r3d2Mesh".getBytes(StandardCharsets.UTF_8)));
    }
    
    public static boolean isProbableSKL(ByteArray wrapper)
    {
        boolean isSKL = wrapper.equals(new ByteArray("r3d2sklt".getBytes(StandardCharsets.UTF_8)));
        if (!isSKL)
        {
            // edge case where the filetype is determined by the first bytes being equal to the filesize
            ByteBuffer b     = ByteBuffer.wrap(wrapper.copyOfRange(0, 4).getData()).order(ByteOrder.LITTLE_ENDIAN);
            int        guess = b.getInt();
            if (guess == wrapper.getData().length)
            {
                return true;
            }
        }
        
        return isSKL;
    }
    
    public static boolean isProbableANM(ByteArray wrapper)
    {
        boolean isEqual = wrapper.equals(new ByteArray("r3d2anmd".getBytes(StandardCharsets.UTF_8)));
        isEqual |= wrapper.equals(new ByteArray("r3d2canm".getBytes(StandardCharsets.UTF_8)));
        return isEqual;
    }
    
    public static boolean isProbableBNK(ByteArray wrapper)
    {
        return wrapper.equals(new ByteArray("BKHD".getBytes(StandardCharsets.UTF_8)));
    }
    
    public static boolean isProbableSCO(ByteArray wrapper)
    {
        return wrapper.equals(new ByteArray("[Obj".getBytes(StandardCharsets.UTF_8)));
    }
    
    public static boolean isProbablePRELOAD(ByteArray wrapper)
    {
        return wrapper.equals(new ByteArray("PreLoad".getBytes(StandardCharsets.UTF_8)));
    }
    
    private static boolean isProbableWPK(ByteArray wrapper)
    {
        return wrapper.equals(new ByteArray("r3d2".getBytes(StandardCharsets.UTF_8)));
    }
    
    private static boolean isProbableSofdec(ByteArray magic)
    {
        return magic.equals(new ByteArray("CRID".getBytes(StandardCharsets.UTF_8)));
    }
    
    
    public static boolean isIgnoredType(String name)
    {
        List<String> types = Arrays.asList(".dll", ".exe", ".dat");
        return types.stream().anyMatch(name.toLowerCase(Locale.ENGLISH)::endsWith);
    }
    
    public static boolean isContainerFormat(String name)
    {
        List<String> types = Arrays.asList(".wad", ".wad.client", ".raf", ".bnk", ".wpk");
        return types.stream().anyMatch(name.toLowerCase(Locale.ENGLISH)::endsWith);
    }
    
    public static boolean isTextFormat(String name)
    {
        List<String> types = Arrays.asList(".js", ".json", ".txt", ".ini", ".cfg");
        return types.stream().anyMatch(name.toLowerCase(Locale.ENGLISH)::endsWith);
    }
    
    public static boolean isImageFormat(String name)
    {
        List<String> types = Arrays.asList(".png", ".jpg", ".jpeg", ".dds");
        return types.stream().anyMatch(name.toLowerCase(Locale.ENGLISH)::endsWith);
    }
    
    //</editor-fold>
    
    
}

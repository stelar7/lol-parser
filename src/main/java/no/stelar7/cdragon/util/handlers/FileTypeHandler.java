package no.stelar7.cdragon.util.handlers;

import no.stelar7.cdragon.types.atlas.AtlasParser;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.filemanifest.ManifestContentParser;
import no.stelar7.cdragon.types.rst.RSTParser;
import no.stelar7.cdragon.types.scb.SCBParser;
import no.stelar7.cdragon.types.sco.SCOParser;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class FileTypeHandler
{
    // only used for their (stateless) canParse checks
    private static final AtlasParser ATLAS = new AtlasParser();
    private static final RSTParser   RST   = new RSTParser();
    private static final SCBParser   SCB   = new SCBParser();
    private static final SCOParser   SCO   = new SCOParser();
    private static final ManifestContentParser MANIFEST = new ManifestContentParser();
    
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
        
        int bomLength = TextTypeHandler.getBOMLength(magic);
        if (bomLength > 0)
        {
            return findFileType(magic.copyOfRange(bomLength));
        }
        
        if (!(magic.getDataRaw().length > 3))
        {
            if (TextTypeHandler.isProbableJSON(magic))
            {
                return "json";
            }
            
            return "txt";
        }
        
        if (FileTypeHandler.isProbableSofdec(magic))
        {
            return "sfd";
        }
        
        if (TextTypeHandler.isProbableJavascript(magic))
        {
            return "js";
        }
        
        if (TextTypeHandler.isProbableHTML(magic))
        {
            return "html";
        }
        
        if (TextTypeHandler.isProbableCSS(magic))
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
            return "ktx";
        }
        
        if (FileTypeHandler.isProbableKTX2(magic))
        {
            return "ktx2";
        }
        
        if (FileTypeHandler.isProbableJPG(magic))
        {
            // every jpg starts with a start of image marker (FF D8) followed by another marker (FF xx)
            return "jpg";
        }
        
        if (FileTypeHandler.isProbableEncryptedTEX(magic))
        {
            return "encrypted.tex";
        }
        
        if (FileTypeHandler.isProbableGLSLF(magic))
        {
            //  OpenGL fragment profile
            return "ps_2_0.glsl";
        }
        
        if (FileTypeHandler.isProbableGLSLV(magic))
        {
            // OpenGL vertex profile
            return "vs_2_0.glsl";
        }
        
        if (FileTypeHandler.isProbableCSO(magic))
        {
            // Compiled Shader Object
            return "ps_2_0.glsl";
        }
        
        if (FileTypeHandler.isProbableCGC(magic))
        {
            // Compiled Shader Config
            return "vs_2_0.dx9";
        }
        
        if (FileTypeHandler.isProbableWPK(magic))
        {
            return "wpk";
        }
        
        if (FileTypeHandler.isProbableRST(magic))
        {
            return "rst";
        }
        
        if (FileTypeHandler.isProbableMTLB(magic))
        {
            // A list of metal shaders?
            return "mtlb";
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
        
        if (TextTypeHandler.isProbableJSON(magic))
        {
            return "json";
        }
        
        if (FileTypeHandler.isProbableSpineSkeleton(magic))
        {
            // binary spine animation, always next to a (text) spine .atlas
            return "skel";
        }
        
        if (TextTypeHandler.isProbableSpineAtlas(magic))
        {
            return "atlas";
        }
        
        if (TextTypeHandler.isProbableINI(magic))
        {
            return "ini";
        }
        
        if (FileTypeHandler.isProbableHLS(magic))
        {
            return "hls";
        }
        
        // formats without a magic number. These are checked before the manifests, as the manifest checks accept almost anything
        // that starts with a small count (eg. every atlas is also a valid "manifestv1")
        if (ATLAS.canParse(magic.getDataRaw()))
        {
            return "atlas";
        }
        
        // (BINParser is only created here, as loading it preloads the bin hashes)
        if (new BINParser().canParse(magic.getDataRaw()))
        {
            // a headerless bin (a single container or structure), PROP/PTCH files are found by magic above
            return "bin";
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
        
        if (FileTypeHandler.isProbableTEX(magic))
        {
            return "tex";
        }
        
        if (TextTypeHandler.isProbableTXT(magic))
        {
            return "txt";
        }
        
        // anything else that decodes as text (eg. minified js and css from the client)
        String textType = TextTypeHandler.findTextType(magic);
        if (textType != null)
        {
            return textType;
        }
        
        // some files start with whitespace, so substring it and try again
        if (magic.startsWith(new ByteArray(new byte[]{13, 10})))
        {
            return findFileType(magic.copyOfRange(2, magic.size()));
        }
        
        return "unknown";
    }
    
    private static boolean isProbableManifestV0(ByteArray magic)
    {
        return magic.size() > 4 && MANIFEST.canParseV0(magic.getDataRaw());
    }
    
    private static boolean isProbableManifestV1(ByteArray magic)
    {
        return magic.size() > 4 && MANIFEST.canParseV1(magic.getDataRaw());
    }
    
    private static boolean isProbableManifestV2(ByteArray magic)
    {
        return magic.size() > 4 && MANIFEST.canParseV2(magic.getDataRaw());
    }
    
    private static boolean isProbableHLS(ByteArray magic)
    {
        boolean probableShader = false;
        String  content        = new String(magic.getDataRaw());
        probableShader |= content.contains("#include ");
        probableShader |= content.contains("void main(VERTEX");
        probableShader |= content.contains("// Shader");
        
        return probableShader;
    }
    
    private static boolean isProbableGLSLV(ByteArray magic)
    {
        return new String(magic.getDataRaw()).contains("// glslv output by Cg compiler");
    }
    
    private static boolean isProbableGLSLF(ByteArray magic)
    {
        return new String(magic.getDataRaw()).contains("// glslf output by Cg compiler");
    }
    
    private static boolean isProbableKTX(ByteArray magic)
    {
        return magic.startsWith(new ByteArray(new byte[]{(byte) 0xAB, 0x4B, 0x54, 0x58, 0x20, 0x31, 0x31, (byte) 0xBB, 0x0D, 0x0A, 0x1A, 0x0A}));
    }
    
    private static boolean isProbableKTX2(ByteArray magic)
    {
        return magic.startsWith(new ByteArray(new byte[]{(byte) 0xAB, 0x4B, 0x54, 0x58, 0x20, 0x32, 0x30, (byte) 0xBB, 0x0D, 0x0A, 0x1A, 0x0A}));
    }
    
    private static boolean isProbableJPG(ByteArray magic)
    {
        return magic.indexMatch(0, (byte) 0xFF) && magic.indexMatch(1, (byte) 0xD8) && magic.indexMatch(2, (byte) 0xFF);
    }
    
    /**
     * Encrypted textures (eg. assets/esports/sponsoredbanners/secret/*.tex), they all start with the same bytes
     */
    private static boolean isProbableEncryptedTEX(ByteArray magic)
    {
        return magic.startsWith(new ByteArray(new byte[]{(byte) 0xC9, (byte) 0xE3, (byte) 0x44, (byte) 0x26, (byte) 0x64, (byte) 0xBB, (byte) 0x01, (byte) 0x61}));
    }
    
    /**
     * Binary spine skeletons: a 64 bit hash followed by the spine version (eg. "4.2.38") as a varint length prefixed string
     */
    private static boolean isProbableSpineSkeleton(ByteArray magic)
    {
        byte[] data = magic.getDataRaw();
        if (data.length < 16)
        {
            return false;
        }
        
        // the length is the string length + 1
        int length = (data[8] & 0xFF) - 1;
        if (length < 3 || length > 15 || 9 + length > data.length || !Character.isDigit(data[9]))
        {
            return false;
        }
        
        boolean hasDot = false;
        for (int i = 9; i < 9 + length; i++)
        {
            if (data[i] == '.')
            {
                hasDot = true;
            } else if (!Character.isDigit(data[i]))
            {
                return false;
            }
        }
        return hasDot;
    }
    
    private static boolean isProbableCSO(ByteArray magic)
    {
        return new String(magic.getDataRaw()).contains("Microsoft (R) HLSL Shader Compiler");
    }
    
    private static boolean isProbableCGC(ByteArray magic)
    {
        return magic.startsWith(new ByteArray(new byte[]{(byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x54, (byte) 0x4F, (byte) 0x43, (byte) 0x33}));
    }
    
    private static boolean isProbableTEX(ByteArray magic)
    {
        return magic.startsWith(new ByteArray(new byte[]{(byte) 0x54, (byte) 0x45, (byte) 0x58, (byte) 0x00}));
    }
    
    // volatile so it can be read without the lock, it is only assigned once fully built
    private static volatile Map<ByteArray, String> magicNumbers;
    
    public static Map<ByteArray, String> getMagicNumbers()
    {
        // called for every file whose type is detected, often from many threads
        Map<ByteArray, String> loaded = magicNumbers;
        return loaded != null ? loaded : loadMagicNumbers();
    }
    
    private static synchronized Map<ByteArray, String> loadMagicNumbers()
    {
        if (magicNumbers == null)
        {
            System.out.println("Loading magic numbers");
            
            ByteArray oggMagic          = new ByteArray(new byte[]{(byte) 0x4f, (byte) 0x67, (byte) 0x67, (byte) 0x53});
            ByteArray webmMagic         = new ByteArray(new byte[]{(byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3});
            ByteArray ddsMagic          = new ByteArray(new byte[]{(byte) 0x44, (byte) 0x44, (byte) 0x53, (byte) 0x20});
            ByteArray encryptedDdsMagic = new ByteArray(new byte[]{(byte) 0x2A, (byte) 0x6C, (byte) 0x04, (byte) 0x50});
            ByteArray pngMagic          = new ByteArray(new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47});
            ByteArray bnkMagic          = new ByteArray(new byte[]{(byte) 0x42, (byte) 0x4B, (byte) 0x48, (byte) 0x44});
            ByteArray binMagic          = new ByteArray(new byte[]{(byte) 0x50, (byte) 0x52, (byte) 0x4F, (byte) 0x50});
            ByteArray lcovMagic         = new ByteArray(new byte[]{(byte) 0x54, (byte) 0x4E, (byte) 0x3A, (byte) 0x0A});
            ByteArray gifMagic          = new ByteArray(new byte[]{(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38});
            ByteArray zipMagic          = new ByteArray(new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04});
            ByteArray ttfMagic          = new ByteArray(new byte[]{(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00});
            ByteArray otfMagic          = new ByteArray(new byte[]{(byte) 0x4F, (byte) 0x54, (byte) 0x54, (byte) 0x4F});
            ByteArray sknMagic          = new ByteArray(new byte[]{(byte) 0x33, (byte) 0x22, (byte) 0x11, (byte) 0x00});
            ByteArray objMagic          = new ByteArray(new byte[]{(byte) 0x5B, (byte) 0x4F, (byte) 0x62, (byte) 0x6A});
            ByteArray unkMagic          = new ByteArray(new byte[]{(byte) 0x74, (byte) 0x22, (byte) 0x00, (byte) 0x00});
            ByteArray luaMagic          = new ByteArray(new byte[]{(byte) 0x1B, (byte) 0x4C, (byte) 0x75, (byte) 0x61});
            ByteArray hlslMagic         = new ByteArray(new byte[]{(byte) 0x23, (byte) 0x70, (byte) 0x72, (byte) 0x61});
            ByteArray oegmMagic         = new ByteArray(new byte[]{(byte) 0x4F, (byte) 0x45, (byte) 0x47, (byte) 0x4D});
            ByteArray fcnfMagic         = new ByteArray(new byte[]{(byte) 0x5B, (byte) 0x46, (byte) 0x6F, (byte) 0x6E});
            ByteArray gmshMagic         = new ByteArray("GMSH".getBytes(StandardCharsets.US_ASCII));
            ByteArray imaaMagic         = new ByteArray("IMAA".getBytes(StandardCharsets.US_ASCII));
            ByteArray nvrgMagic         = new ByteArray("NVRG".getBytes(StandardCharsets.US_ASCII));
            ByteArray nsdiMagic         = new ByteArray("NSDI".getBytes(StandardCharsets.US_ASCII));
            
            
            Map<ByteArray, String> magicNumbers = new HashMap<>();
            // Sound
            magicNumbers.put(oggMagic, "ogg");
            
            // Video
            magicNumbers.put(webmMagic, "webm");
            
            // Image
            magicNumbers.put(pngMagic, "png");
            magicNumbers.put(gifMagic, "gif");
            
            // Fonts
            magicNumbers.put(ttfMagic, "ttf");
            magicNumbers.put(otfMagic, "otf");
            magicNumbers.put(new ByteArray("wOFF".getBytes(StandardCharsets.US_ASCII)), "woff");
            magicNumbers.put(new ByteArray("wOF2".getBytes(StandardCharsets.US_ASCII)), "woff2");

            // Div
            magicNumbers.put(zipMagic, "zip");
            magicNumbers.put(luaMagic, "luaobj");
            magicNumbers.put(hlslMagic, "hlsl");
            
            // 3D model
            magicNumbers.put(bnkMagic, "bnk");
            magicNumbers.put(ddsMagic, "dds");
            magicNumbers.put(encryptedDdsMagic, "encrypted.dds");
            magicNumbers.put(binMagic, "bin");
            magicNumbers.put(lcovMagic, "info");
            magicNumbers.put(sknMagic, "skn");
            magicNumbers.put(objMagic, "obj");
            magicNumbers.put(gmshMagic, "gmesh");

            // i dont know...? (named after their magic)
            magicNumbers.put(unkMagic, "unk");
            // eg. assets/items/icons2d/autoatlas/largeicons/atlas_info.bin
            magicNumbers.put(imaaMagic, "imaa");
            // 12 byte files (magic, version 1, 4 zero bytes) in the map wads
            magicNumbers.put(nvrgMagic, "nvrg");
            magicNumbers.put(nsdiMagic, "nsdi");
            magicNumbers.put(oegmMagic, "oegm");
            magicNumbers.put(fcnfMagic, "fcnf");
            
            FileTypeHandler.magicNumbers = magicNumbers;
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
        if (data.length < 2)
        {
            return false;
        }
        
        boolean isNoCompress      = isSame(data[0], (byte) 0x78) && isSame(data[1], (byte) 0x01);
        boolean isBestCompress    = isSame(data[0], (byte) 0x78) && isSame(data[1], (byte) 0xDA);
        boolean isDefaultCompress = isSame(data[0], (byte) 0x78) && isSame(data[1], (byte) 0x9C);
        return isNoCompress || isBestCompress || isDefaultCompress;
    }
    
    public static boolean isProbableGZIP(byte[] data)
    {
        return data.length >= 2 && isSame(data[0], (byte) 0x1f) && isSame(data[1], (byte) 0x8b);
    }
    
    public static boolean isProbableZSTD(byte[] data)
    {
        return data.length >= 4 && isSame(data[0], (byte) 0x28) && isSame(data[1], (byte) 0xB5) && isSame(data[2], (byte) 0x2F) && isSame(data[3], (byte) 0xFD);
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
    
    public static boolean isProbableMTLB(ByteArray magic)
    {
        return magic.indexMatch(4, (byte) 0x4D) && magic.indexMatch(5, (byte) 0x54) && magic.indexMatch(6, (byte) 0x4C) && magic.indexMatch(7, (byte) 0x42);
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
    
    public static boolean isProbable3DModelStuff(ByteArray wrapper)
    {
        return wrapper.indexMatch(2, (byte) 0x00) && wrapper.indexMatch(3, (byte) 0x00);
    }
    
    public static boolean isProbableSCB(ByteArray wrapper)
    {
        return SCB.canParse(wrapper.getDataRaw());
    }
    
    public static boolean isProbableSKL(ByteArray wrapper)
    {
        boolean isSKL = wrapper.startsWith(new ByteArray("r3d2sklt".getBytes(StandardCharsets.UTF_8)));
        if (!isSKL)
        {
            // edge case where the filetype is determined by the first bytes being equal to the filesize
            ByteBuffer b     = ByteBuffer.wrap(wrapper.copyOfRange(0, 4).getDataRaw()).order(ByteOrder.LITTLE_ENDIAN);
            int        guess = b.getInt();
            if (guess == wrapper.getDataRaw().length)
            {
                return true;
            }
        }
        
        return isSKL;
    }
    
    public static boolean isProbableANM(ByteArray wrapper)
    {
        boolean isEqual = wrapper.startsWith(new ByteArray("r3d2anmd".getBytes(StandardCharsets.UTF_8)));
        isEqual |= wrapper.startsWith(new ByteArray("r3d2canm".getBytes(StandardCharsets.UTF_8)));
        return isEqual;
    }
    
    public static boolean isProbableRST(ByteArray wrapper)
    {
        return RST.canParse(wrapper.getDataRaw());
    }
    
    public static boolean isProbableBNK(ByteArray wrapper)
    {
        return wrapper.startsWith(new ByteArray("BKHD".getBytes(StandardCharsets.UTF_8)));
    }
    
    public static boolean isProbableSCO(ByteArray wrapper)
    {
        return SCO.canParse(wrapper.getDataRaw());
    }
    
    public static boolean isProbablePRELOAD(ByteArray wrapper)
    {
        return wrapper.startsWith(new ByteArray("PreLoad".getBytes(StandardCharsets.UTF_8)));
    }
    
    private static boolean isProbableWPK(ByteArray wrapper)
    {
        return wrapper.startsWith(new ByteArray("r3d2".getBytes(StandardCharsets.UTF_8)));
    }
    
    private static boolean isProbableSofdec(ByteArray magic)
    {
        return magic.startsWith(new ByteArray("CRID".getBytes(StandardCharsets.UTF_8)));
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

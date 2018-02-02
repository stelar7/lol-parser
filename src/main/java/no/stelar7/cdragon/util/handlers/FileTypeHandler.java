package no.stelar7.cdragon.util.handlers;

import com.google.gson.*;
import no.stelar7.cdragon.util.readers.types.ByteArray;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public final class FileTypeHandler
{
    
    private FileTypeHandler()
    {
        // Hide public constructor
    }
    
    public static String findFileType(byte[] data, Path file)
    {
        ByteArray magic4 = new ByteArray(Arrays.copyOf(data, 4));
        ByteArray magic8 = new ByteArray(Arrays.copyOf(data, 8));
        
        if (FileTypeHandler.isProbableBOM(magic4))
        {
            return findFileType(Arrays.copyOfRange(data, 3, 7), file);
        }
        
        if (FileTypeHandler.isProbableJSON(magic4))
        {
            return "json";
        }
        
        if (FileTypeHandler.isProbableJavascript(magic4))
        {
            return "js";
        }
        
        if (FileTypeHandler.isProbableHTML(magic4))
        {
            return "html";
        }
        
        if (FileTypeHandler.isProbableCSS(magic4))
        {
            return "css";
        }
        
        if (FileTypeHandler.isProbableSKL(magic8))
        {
            return "skl";
        }
        
        if (FileTypeHandler.isProbableSCB(magic8))
        {
            return "scb";
        }
        
        if (FileTypeHandler.isProbableANM(magic8))
        {
            return "anm";
        }
        
        if (FileTypeHandler.isProbableBNK(magic4))
        {
            return "bnk";
        }
        
        if (FileTypeHandler.isProbableSCO(magic4))
        {
            return "sco";
        }
        if (FileTypeHandler.isProbableLUAOBJ(magic8))
        {
            return "luaobj";
        }
        if (FileTypeHandler.isProbablePRELOAD(magic4))
        {
            return "preload";
        }
        
        if (FileTypeHandler.isProbableIDX(magic4))
        {
            return "idx";
        }
        
        if (FileTypeHandler.isProbable3DModelStuff(magic4))
        {
            return "skn";
        }
        
        if (FileTypeHandler.isProbableWPK(magic4))
        {
            return "wpk";
        }
        
        String result = FileTypeHandler.getMagicNumbers().get(magic4);
        if (result != null)
        {
            return result;
        }
        
        if (FileTypeHandler.isProbableTXT(magic4))
        {
            return "txt";
        }
        
        
        System.out.print("Unknown filetype: ");
        System.out.print(file.toString());
        System.out.println(magic4.toString());
        return "txt";
    }
    
    
    public static byte[] makePrettyJson(byte[] jsonString)
    {
        String      dataString = new String(jsonString, StandardCharsets.UTF_8);
        JsonElement obj        = new JsonParser().parse(dataString);
        String      pretty     = UtilHandler.getGson().toJson(obj);
        return pretty.getBytes(StandardCharsets.UTF_8);
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
            ByteArray cgcMagic  = new ByteArray(new byte[]{(byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00});
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
            magicNumbers.put(luaMagic, "lua");
            magicNumbers.put(hlslMagic, "hlsl");
            
            // 3D model
            magicNumbers.put(bnkMagic, "bnk");
            magicNumbers.put(ddsMagic, "dds");
            magicNumbers.put(cgcMagic, "cgc");
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
    
    public static boolean isProbableJSON(ByteArray wrapper)
    {
        byte[]  data   = wrapper.getData();
        boolean isJSON = (isSame(data[0], (byte) 0x7B) && (isSame(data[1], (byte) 0x22) || isSame(data[1], (byte) 0x0D)));
        
        isJSON |= (isSame(data[0], (byte) 0x7B) && isSame(data[1], (byte) 0x0A) && isSame(data[2], (byte) 0x20) && isSame(data[3], (byte) 0x20));
        isJSON |= (isSame(data[0], (byte) 0x7B) && isSame(data[1], (byte) 0x7D));
        isJSON |= (isSame(data[0], (byte) 0x5B) && isSame(data[1], (byte) 0x5D));
        isJSON |= (isSame(data[0], (byte) 0x7B) && isSame(data[1], (byte) 0x0A) && isSame(data[2], (byte) 0x7D) && isSame(data[3], (byte) 0x0A));
        isJSON |= (isSame(data[0], (byte) 0x7B) && isSame(data[1], (byte) 0x0A) && isSame(data[2], (byte) 0x0A) && isSame(data[3], (byte) 0x7D));
        isJSON |= (isSame(data[0], (byte) 0x5B) && isSame(data[1], (byte) 0x7B) && isSame(data[2], (byte) 0x22));
        isJSON |= (isSame(data[0], (byte) 0x5B) && isSame(data[1], (byte) 0x0A) && isSame(data[2], (byte) 0x20) && isSame(data[3], (byte) 0x20));
        
        isJSON |= (isSame(data[0], (byte) 0x5B) && new String(Arrays.copyOfRange(data, 1, 4), StandardCharsets.UTF_8).matches("\\d*,?\\d*"));
        
        return isJSON;
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
                (byte) 0x61, (byte) 0x73, (byte) 0x73, (byte) 0x68, (byte) 0x70, (byte) 0x0D,
                (byte) 0x32, (byte) 0x67, (byte) 0x69, (byte) 0x70, (byte) 0x6F, (byte) 0x0F,
                (byte) 0x62, (byte) 0x65, (byte) 0x6E, (byte) 0x70, (byte) 0x6F, (byte) 0x73,
                (byte) 0x63, (byte) 0x61, (byte) 0x63, (byte) 0x62, (byte) 0x6C, (byte) 0x6F,
                (byte) 0x34, (byte) 0x6D, (byte) 0x6F, (byte) 0x63, (byte) 0x75, (byte) 0x6D,
                (byte) 0x70, (byte) 0x6F, (byte) 0x6E, (byte) 0x72, (byte) 0x6F, (byte) 0x61,
                (byte) 0xEA, (byte) 0xB0, (byte) 0x80, (byte) 0x6A, (byte) 0x61, (byte) 0x0D,
                (byte) 0x6C, (byte) 0x80, (byte) 0x3C, (byte) 0x61, (byte) 0x6C, (byte) 0x6C,
                (byte) 0x67, (byte) 0x61, (byte) 0x72, (byte) 0x72, (byte) 0x69, (byte) 0x6F,
                (byte) 0x62, (byte) 0x69, (byte) 0x6D, (byte) 0x72, (byte) 0x65, (byte) 0x67,
                (byte) 0x21, (byte) 0x69, (byte) 0x0D, (byte) 0x21, (byte) 0x20, (byte) 0x22,
                (byte) 0x61, (byte) 0x6D, (byte) 0x0D, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xC3, (byte) 0x80, (byte) 0xC3, (byte) 0x61, (byte) 0x6D, (byte) 0x0D,
                (byte) 0x62, (byte) 0x61, (byte) 0x6E, (byte) 0x61, (byte) 0x73, (byte) 0x61,
                (byte) 0x63, (byte) 0x6F, (byte) 0x77, (byte) 0x63, (byte) 0x64, (byte) 0x63,
                (byte) 0x61, (byte) 0x72, (byte) 0x73, (byte) 0x62, (byte) 0x6F, (byte) 0x62,
                (byte) 0x70, (byte) 0x65, (byte) 0x64, (byte) 0x63, (byte) 0x38, (byte) 0x0D,
                (byte) 0x6D, (byte) 0x61, (byte) 0x64, (byte) 0x6B, (byte) 0x75, (byte) 0x6E,
                (byte) 0x73, (byte) 0x6C, (byte) 0x75, (byte) 0x36, (byte) 0x39, (byte) 0x0D,
                (byte) 0x62, (byte) 0x64, (byte) 0x6D, (byte) 0x70, (byte) 0x69, (byte) 0x73,
                (byte) 0x36, (byte) 0x39, (byte) 0x0D, (byte) 0x61, (byte) 0x63, (byte) 0x63,
                (byte) 0x62, (byte) 0x61, (byte) 0x6B, (byte) 0xC4, (byte) 0x8D, (byte) 0x75,
                (byte) 0x21, (byte) 0x20, (byte) 0x27, (byte) 0x30, (byte) 0x31, (byte) 0x32,
                (byte) 0x61, (byte) 0x64, (byte) 0x6D, (byte) 0x37, (byte) 0x33, (byte) 0x37,
                (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x2D, (byte) 0xE8, (byte) 0x83,
                (byte) 0x62, (byte) 0x6F, (byte) 0x6D, (byte) 0x63, (byte) 0x75, (byte) 0x0D,
                (byte) 0x62, (byte) 0x75, (byte) 0x67, (byte) 0x62, (byte) 0x79, (byte) 0x6D,
                (byte) 0x30, (byte) 0x6F, (byte) 0x0D, (byte) 0x30, (byte) 0x6F, (byte) 0x0D,
                (byte) 0x72, (byte) 0x65, (byte) 0x63, (byte) 0x6C, (byte) 0x6F, (byte) 0x6F,
                (byte) 0x54, (byte) 0x45, (byte) 0x52, (byte) 0x45, (byte) 0x4E, (byte) 0x44,
                (byte) 0x45, (byte) 0x6E, (byte) 0x64, (byte) 0x45, (byte) 0x6C, (byte) 0x20,
                (byte) 0x54, (byte) 0x45, (byte) 0x52, (byte) 0xD0, (byte) 0xA3, (byte) 0xD0,
                (byte) 0xD0, (byte) 0x9B, (byte) 0xD0, (byte) 0xEC, (byte) 0x8B, (byte) 0x9C,
                (byte) 0x43, (byte) 0x6F, (byte) 0x6E, (byte) 0x09, (byte) 0x0D, (byte) 0x0A,
                (byte) 0x4C, (byte) 0x45, (byte) 0x41, (byte) 0xE3, (byte) 0x82, (byte) 0xA2,
                (byte) 0x6C, (byte) 0x65, (byte) 0x73, (byte) 0x37, (byte) 0x31, (byte) 0x37,
                (byte) 0x61, (byte) 0x34, (byte) 0x75, (byte) 0x40, (byte) 0x21, (byte) 0x40,
                (byte) 0xD1, (byte) 0x85, (byte) 0xD1, (byte) 0xE3, (byte) 0x84, (byte) 0x85,
                (byte) 0x31, (byte) 0x6D, (byte) 0x65, (byte) 0x20, (byte) 0x21, (byte) 0x23,
                (byte) 0x28, (byte) 0x29, (byte) 0x2A, (byte) 0x73, (byte) 0x69, (byte) 0x6B,
                (byte) 0xE6, (byte) 0x8B, (byte) 0xA6, (byte) 0x70, (byte) 0x65, (byte) 0x72,
                (byte) 0xE3, (byte) 0x83, (byte) 0xAA, (byte) 0x74, (byte) 0x75, (byte) 0x72,
                
                };
        
        
        for (int i = 0; i < magicText3Wide.length; i += 3)
        {
            byte[] temp = Arrays.copyOfRange(magicText3Wide, i, i + 3);
            list.add(new ByteArray(temp));
        }
        
        return list;
    }
    
    public static boolean isProbableIDX(ByteArray wrapper)
    {
        byte[] data = wrapper.getData();
        return !isSame(data[0], (byte) 0x00) && isSame(data[1], (byte) 0x00) && isSame(data[2], (byte) 0x00) && isSame(data[3], (byte) 0x00);
    }
    
    public static boolean isProbable3DModelStuff(ByteArray wrapper)
    {
        byte[] data = wrapper.getData();
        return isSame(data[2], (byte) 0x00) && isSame(data[3], (byte) 0x00);
    }
    
    public static boolean isProbableSCB(ByteArray wrapper)
    {
        return wrapper.equals(new ByteArray("r3d2Mesh".getBytes(StandardCharsets.UTF_8)));
    }
    
    public static boolean isProbableSKL(ByteArray wrapper)
    {
        return wrapper.equals(new ByteArray("r3d2sklt".getBytes(StandardCharsets.UTF_8)));
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
    
    public static boolean isProbableLUAOBJ(ByteArray wrapper)
    {
        return wrapper.equals(new ByteArray("LuaQ".getBytes(StandardCharsets.UTF_8)));
    }
    
    public static boolean isProbablePRELOAD(ByteArray wrapper)
    {
        return wrapper.equals(new ByteArray("PreLoad".getBytes(StandardCharsets.UTF_8)));
    }
    
    private static boolean isProbableWPK(ByteArray wrapper)
    {
        return wrapper.equals(new ByteArray("r3d2".getBytes(StandardCharsets.UTF_8)));
    }
    
    //</editor-fold>
    
    
}

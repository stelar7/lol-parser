package no.stelar7.cdragon.util.handlers;

import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/**
 * Detects text files (json, css, js, html and plain text) and byte order marks, used by {@link FileTypeHandler#findFileType}
 */
public final class TextTypeHandler
{
    private TextTypeHandler()
    {
        // Hide public constructor
    }
    
    // the first 4 bytes of the css, js and html files seen in the client so far
    private static final String[] CSS_STARTS = {
            ".boo", ".pln", "body", "/*@i", "span", "@imp", "/* S", ".hon", ".loy", ".che", ".rep", ".upd", "@fon", "\n:ro", ":roo", ".lol",
            "div[", ".rcp", " .pl",
    };
    
    private static final String[] JS_STARTS = {
            "!fun", "webp", "var ", "wind", "\"use", "PreL", "_NAM", "(()=",
    };
    
    private static final String[] HTML_STARTS = {
            "<scr", "<!do", "<lin", "<?xm", "<tem", "<lol", "<svg", "<!--", "<div", "<img", "<htm", "<sty", "{{#u", "<hea",
    };
    
    public static boolean isProbableBOM(ByteArray wrapper)
    {
        return getBOMLength(wrapper) > 0;
    }
    
    /**
     * Returns the length of the byte order mark at the start of the data, or 0 if there is none
     */
    public static int getBOMLength(ByteArray wrapper)
    {
        if (wrapper.startsWith(new ByteArray(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF})))
        {
            return 4;
        }
        
        if (wrapper.startsWith(new ByteArray(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF})))
        {
            return 3;
        }
        
        if (wrapper.startsWith(new ByteArray(new byte[]{(byte) 0xFE, (byte) 0xFF})))
        {
            return 2;
        }
        
        return 0;
    }
    
    public static boolean isProbableJSON(ByteArray wrapper)
    {
        boolean startsWithJson                    = (wrapper.indexMatch(0, (byte) 0x7B) || wrapper.indexMatch(0, (byte) 0x5B));
        boolean endsWithJson                      = (wrapper.indexMatch(wrapper.size() - 1, (byte) 0x7D) || wrapper.indexMatch(wrapper.size() - 1, (byte) 0x5D));
        boolean endsWithJsonMinusLinefeed         = (wrapper.indexMatch(wrapper.size() - 2, (byte) 0x7D) || wrapper.indexMatch(wrapper.size() - 2, (byte) 0x5D));
        boolean endsWithJsonMinusLinefeedAndBreak = (wrapper.indexMatch(wrapper.size() - 3, (byte) 0x7D) || wrapper.indexMatch(wrapper.size() - 3, (byte) 0x5D));
        
        return startsWithJson && (endsWithJson || endsWithJsonMinusLinefeed || endsWithJsonMinusLinefeedAndBreak);
    }
    
    public static boolean isProbableCSS(ByteArray wrapper)
    {
        return startsWithAny(wrapper, CSS_STARTS);
    }
    
    public static boolean isProbableJavascript(ByteArray wrapper)
    {
        return startsWithAny(wrapper, JS_STARTS);
    }
    
    public static boolean isProbableHTML(ByteArray wrapper)
    {
        return startsWithAny(wrapper, HTML_STARTS);
    }
    
    private static boolean startsWithAny(ByteArray wrapper, String... starts)
    {
        for (String start : starts)
        {
            if (wrapper.startsWith(new ByteArray(start.getBytes(StandardCharsets.ISO_8859_1))))
            {
                return true;
            }
        }
        return false;
    }
    
    // how much of a file is looked at to decide if it is text
    private static final int TEXT_SAMPLE_SIZE = 64 * 1024;
    
    /**
     * The start of the file as text, or null if it is not valid UTF-8 or contains control characters other than whitespace
     */
    private static String readTextSample(ByteArray wrapper)
    {
        byte[] data   = wrapper.getDataRaw();
        int    length = Math.min(data.length, TEXT_SAMPLE_SIZE);
        for (int i = 0; i < length; i++)
        {
            int value = data[i] & 0xFF;
            if (value < 0x20 && value != '\t' && value != '\n' && value != '\r' && value != '\f')
            {
                return null;
            }
        }
        
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                                                       .onMalformedInput(CodingErrorAction.REPORT)
                                                       .onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer out = CharBuffer.allocate(length);
        // when the sample ends inside a multibyte character, the decoder stops there instead of failing (endOfInput is only true for the whole file)
        CoderResult result = decoder.decode(ByteBuffer.wrap(data, 0, length), out, length == data.length);
        if (result.isError())
        {
            return null;
        }
        return out.flip().toString();
    }
    
    /**
     * Spine texture atlases (text): a page name like "x.tex" or "x.png", followed by "size:", "format:" or "filter:" lines
     */
    public static boolean isProbableSpineAtlas(ByteArray wrapper)
    {
        String text = readTextSample(wrapper);
        if (text == null)
        {
            return false;
        }
        
        String[] lines = text.strip().split("\\r?\\n", 3);
        if (lines.length < 2 || !lines[0].contains("."))
        {
            return false;
        }
        
        String second = lines[1].strip();
        return second.startsWith("size:") || second.startsWith("format:") || second.startsWith("filter:") || second.startsWith("pma:");
    }
    
    /**
     * Text starting with a [section] or a ; comment
     */
    public static boolean isProbableINI(ByteArray wrapper)
    {
        byte[] data = wrapper.getDataRaw();
        if (data.length == 0 || (data[0] != '[' && data[0] != ';'))
        {
            return false;
        }
        
        String text = readTextSample(wrapper);
        if (text == null)
        {
            return false;
        }
        
        String firstLine = text.lines().findFirst().orElse("").strip();
        return firstLine.startsWith(";") || (firstLine.endsWith("]") && firstLine.length() > 2);
    }
    
    /**
     * Guesses the type of a text file that did not match anything more specific: "js", "css" or "txt", or null if it is not text
     */
    public static String findTextType(ByteArray wrapper)
    {
        String text = readTextSample(wrapper);
        if (text == null || text.isBlank())
        {
            return null;
        }
        
        String start = text.stripLeading();
        if (start.startsWith("(()=>") || start.startsWith("(function") || start.startsWith("!function") || start.startsWith("\"use strict\"") ||
            start.startsWith("'use strict'") || start.startsWith("export ") || start.startsWith("import ") || start.startsWith("const ") || start.startsWith("let "))
        {
            return "js";
        }
        
        // css starts with a selector (or an @rule) followed by a { ... } block
        int brace = start.indexOf('{');
        if (brace > 0 && (start.charAt(0) == '.' || start.charAt(0) == '#' || start.charAt(0) == '@' || start.charAt(0) == ':' || start.charAt(0) == '*' || Character.isLetter(start.charAt(0))))
        {
            String selector = start.substring(0, brace);
            if (!selector.contains(";") && !selector.contains("(") && !selector.contains("=") && start.indexOf('}', brace) > 0)
            {
                return "css";
            }
        }
        
        return "txt";
    }
    
    private static final List<ByteArray> possibleTextTargets = loadTextTargets();
    
    public static boolean isProbableTXT(ByteArray wrapper)
    {
        byte[]    data        = wrapper.getDataRaw();
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
                (byte) 0x74, (byte) 0x72, (byte) 0x20, (byte) 0x40, (byte) 0x73, (byte) 0x65,
                (byte) 0x54, (byte) 0x68, (byte) 0x69,
                };
        
        
        for (int i = 0; i < magicText3Wide.length; i += 3)
        {
            byte[] temp = Arrays.copyOfRange(magicText3Wide, i, i + 3);
            list.add(new ByteArray(temp));
        }
        
        return list;
    }
}

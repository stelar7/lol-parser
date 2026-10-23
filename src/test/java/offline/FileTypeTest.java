package offline;

import com.github.luben.zstd.Zstd;
import no.stelar7.cdragon.types.tex.TEXParser;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.*;

import java.awt.image.BufferedImage;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * File types that used to end up as "unknown" when extracting the PBE wads (patch 16.22), TEX format 0x17, and zstd data that compresses well
 */
public class FileTypeTest
{
    @BeforeAll
    public static void useEmptyHashTables()
    {
        OfflineHashes.useEmpty();
    }

    private static String typeOf(byte[] data)
    {
        return FileTypeHandler.findFileType(new ByteArray(data));
    }

    private static String typeOf(String text)
    {
        return typeOf(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testMagicNumbers()
    {
        assertEquals("gmesh", typeOf(new LE().ascii("GMSH").i32(1).zeros(8).toBytes()));
        assertEquals("imaa", typeOf(new LE().ascii("IMAA").i32(2).zeros(40).toBytes()));
        assertEquals("nvrg", typeOf(new LE().ascii("NVRG").i32(1).i32(0).toBytes()));
        assertEquals("nsdi", typeOf(new LE().ascii("NSDI").i32(1).i32(0).toBytes()));
        assertEquals("woff", typeOf(new LE().ascii("wOFF").zeros(40).toBytes()));
        assertEquals("ktx2", typeOf(new LE().u8(0xAB).ascii("KTX 20").u8(0xBB).u8(0x0D).u8(0x0A).u8(0x1A).u8(0x0A).zeros(16).toBytes()));
        // jpg with an ICC profile (APP2 marker), the old list only had E0, E1, EC and DB
        assertEquals("jpg", typeOf(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE2, 0, 16, 'I', 'C', 'C'}));
        assertEquals("encrypted.tex", typeOf(new LE().u8(0xC9).u8(0xE3).u8(0x44).u8(0x26).u8(0x64).u8(0xBB).u8(0x01).u8(0x61).zeros(24).toBytes()));
    }

    @Test
    public void testSpine()
    {
        // binary skeleton: 64 bit hash, then the version as a varint length (+1) prefixed string
        byte[] skel = new LE().i64(0x0D1CCF30702806FL).u8(7).ascii("4.2.38").f32(1, 2, 3, 4).toBytes();
        assertEquals("skel", typeOf(skel));

        assertEquals("atlas", typeOf("ST_TraitTracker_Blue.tex\nsize:128,128\nfilter:Linear,Linear\nP_Sparks\nbounds:2,27,86,68\n"));
        assertEquals("atlas", typeOf("\npage.png\r\nsize: 64, 64\r\nformat: RGBA8888\r\n"));
    }

    @Test
    public void testText()
    {
        assertEquals("ini", typeOf("[HQ_Objects]\nModel=hq.sco\n"));
        assertEquals("ini", typeOf(";This file is generated\n[Global]\nA=1\n"));
        assertEquals("js", typeOf("(()=>{var e=[,e=>{\"use strict\";let t;}]})();"));
        assertEquals("css", typeOf(".dialog-alert .dialog-frame,.dialog-celebration{color:red}"));
        assertEquals("css", typeOf(".sanctum-root .banner{background:url(a.png)}"));
        assertEquals("txt", typeOf("Some notes\nwith a few lines\nand ünïcödé\n"));
        assertEquals("unknown", typeOf(new byte[]{0x79, (byte) 0xDC, 0x24, 0x1C, 0x00, 0x13, (byte) 0x88, 0x02, 0x7F, 0x01}));
    }

    private static byte[] texR8(int width, int height)
    {
        LE tex = new LE().ascii("TEX").u8(0).i16(width).i16(height).u8(1).u8(TEXParser.FORMAT_R8).u8(3).u8(0);
        for (int i = 0; i < width * height; i++)
        {
            tex.u8(i);
        }
        return tex.toBytes();
    }

    @Test
    public void testTexR8()
    {
        byte[]        data  = texR8(16, 16);
        BufferedImage image = new TEXParser().parse(new ByteArray(data));
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        assertEquals(0xFF000000, image.getRGB(0, 0));
        assertEquals(0xFF050505, image.getRGB(5, 0));
        assertEquals(0xFF141414, image.getRGB(4, 1));

        byte[] dds = TEXParser.toDDS(data);
        assertEquals(128 + 20 + 16 * 16, dds.length);
        ByteBuffer buffer = ByteBuffer.wrap(dds).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals("DX10", new String(dds, 84, 4, StandardCharsets.US_ASCII));
        assertEquals(61, buffer.getInt(128), "R8_UNORM");
        assertEquals(5, dds[148 + 5]);
    }

    @Test
    public void testZstdWithHighCompressionRatio()
    {
        // a mostly flat texture compresses far better than 6:1, which is what uncompress used to assume
        byte[] raw = new byte[1 << 20];
        Arrays.fill(raw, 1000, 2000, (byte) 7);
        byte[] compressed = Zstd.compress(raw);
        assertTrue(compressed.length * 6 < raw.length);
        assertArrayEquals(raw, CompressionHandler.uncompress(compressed));

        // two frames after each other
        byte[] twoFrames = new byte[compressed.length * 2];
        System.arraycopy(compressed, 0, twoFrames, 0, compressed.length);
        System.arraycopy(compressed, 0, twoFrames, compressed.length, compressed.length);
        assertEquals(raw.length * 2, CompressionHandler.uncompress(twoFrames).length);

        assertNull(CompressionHandler.uncompress(new byte[]{0x28}), "too short to be anything");
    }
}

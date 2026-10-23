package offline;

import no.stelar7.cdragon.types.anm.ANMParser;
import no.stelar7.cdragon.types.anm.data.ANMFile;
import no.stelar7.cdragon.types.scb.SCBParser;
import no.stelar7.cdragon.types.scb.data.*;
import no.stelar7.cdragon.types.sco.SCOParser;
import no.stelar7.cdragon.types.skl.SKLParser;
import no.stelar7.cdragon.types.skl.data.*;
import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.SKNFile;
import no.stelar7.cdragon.util.readers.*;
import org.joml.*;
import org.junit.jupiter.api.Test;

import java.lang.Math;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline checks of the mesh/skeleton/animation parsers (SKN, SKL, ANM, SCB, SCO) on synthetic files
 */
public class MeshTest
{
    private static final float EPSILON = 1e-3f;

    private static RandomAccessReader reader(byte[] data)
    {
        return new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN);
    }

    // ---------------------------------------------------------------- SKN

    /**
     * v4, vertex type 1 (color) but a stride of 60 (4 unknown trailing bytes), 2 ranges
     */
    private static byte[] skn()
    {
        LE w = new LE();
        w.i32(0x00112233).i16(4).i16(1).i32(2);
        w.fixed("matA", 64).i32(0).i32(3).i32(0).i32(3);
        w.fixed("matB", 64).i32(3).i32(3).i32(3).i32(3);
        w.i32(0).i32(6).i32(6).i32(60).i32(1);
        w.f32(0, 0, 0, 1, 1, 1, 0, 0, 0, 1);
        for (int x : new int[]{0, 1, 2, 5, 4, 3})
        {
            w.i16(x);
        }
        for (int v = 0; v < 6; v++)
        {
            w.f32(v, v * 10, v * 100).bytes(new byte[]{1, 2, 3, 4}).f32(1, 0, 0, 0).f32(0, 1, 0).f32(0.5f, v).bytes(new byte[]{9, 8, 7, 6}).i32(0xDEAD);
        }
        return w.toBytes();
    }

    @Test
    public void testSKN()
    {
        SKNFile f = new SKNParser().parse(reader(skn()));
        assertEquals(6, f.getVertices().size());
        assertEquals(50, f.getVertices().get(5).getPosition().y, "vertex stride honored");
        assertEquals(9, f.getVertices().get(5).getColor().getX(), "vertex color");
        assertEquals("matB", f.getMaterials().get(1).getName(), "range name without NULs");
        assertEquals(List.of(2, 1, 0), f.getMaterials().get(1).getIndecies(), "global indices made range-local");
        assertEquals(3, f.getDataForSubmesh(f.getMaterials().get(1)).size());
    }

    @Test
    public void testSKNRejectsBadData()
    {
        byte[] bad = skn();
        bad[0] = 0;
        assertThrows(ParseException.class, () -> new SKNParser().parse(reader(bad)), "bad magic");

        byte[] badVersion = skn();
        badVersion[4] = 3;
        assertThrows(ParseException.class, () -> new SKNParser().parse(reader(badVersion)), "unsupported major version");
    }

    @Test
    public void testSKNIndicesAreUnsigned()
    {
        // v1 with 40000 vertices, index 39999
        int n = 40000;
        LE  w = new LE();
        w.i32(0x00112233).i16(1).i16(1).i32(1).fixed("m", 64).i32(0).i32(n).i32(0).i32(3);
        w.i32(3).i32(n);
        w.i16(0).i16(39999).i16(1);
        for (int v = 0; v < n; v++)
        {
            w.f32(v, 0, 0).zeros(4).f32(1, 0, 0, 0).f32(0, 1, 0).f32(0, 0);
        }
        SKNFile f = new SKNParser().parse(reader(w.toBytes()));
        assertEquals(39999, f.getIndecies()[1]);
        assertEquals(39999, f.getMaterials().get(0).getIndecies().get(1));
    }

    // ---------------------------------------------------------------- SKL

    @Test
    public void testSKLModern()
    {
        LE  w            = new LE();
        int jointsOffset = 64;
        int hashOffset   = jointsOffset + 200;
        int inflOffset   = hashOffset + 16;
        int namesOffset  = inflOffset + 4;
        w.i32(0).i32(SKLParser.FORMAT_TOKEN).i32(0);
        w.i16(0).i16(2).i32(2);
        w.i32(jointsOffset).i32(hashOffset).i32(inflOffset).i32(namesOffset + 9).i32(namesOffset + 12).i32(namesOffset);
        w.i32(-1).i32(-1).i32(-1).i32(-1).i32(-1);
        assertEquals(64, w.size(), "header size");

        int[] nameOffsets = {namesOffset, namesOffset + 5};
        for (int j = 0; j < 2; j++)
        {
            w.i16(0).i16(j).i16(j - 1).i16(0).i32(0x1000 + j).f32(2);
            w.f32(1, 2, 3).f32(1, 1, 1).f32(0, 0, 0, 1);
            w.f32(-1, -2, -3).f32(2, 2, 2).f32(0, 1, 0, 0);
            int fieldPos = w.size();
            w.i32(nameOffsets[j] - fieldPos);
        }
        w.i16(1).i16(0).i32(0x1001).i16(0).i16(0).i32(0x1000);
        w.i16(0).i16(1);
        w.ascii("Root\0Arm\0").ascii("sk\0").ascii("asset\0");

        SKLFile f = new SKLParser().parse(reader(w.toBytes()));
        var     d = f.getDataV0();
        assertEquals("Root", d.getBones().get(0).getName());
        assertEquals("Arm", d.getBones().get(1).getName());
        assertEquals(2, d.getBones().get(1).getInverseBindScale().x);
        assertEquals(1, d.getBones().get(1).getInverseBindRotation().y);
        assertEquals(-3, d.getBones().get(1).getCt().z);
        assertEquals("sk", d.getName());
        assertEquals("asset", d.getAssetName());
        assertEquals(List.of((short) 0, (short) 1), d.getBoneIndecies(), "influences");
        assertEquals(1, d.getBoneExtra().get(0).getBoneId());
        assertEquals(0x1001, d.getBoneExtra().get(0).getBoneHash());

        List<ReadableBone> bones = f.toReadableBones();
        assertEquals(2, bones.size());
        assertSame(bones.get(0), bones.get(1).getParent());
    }

    private static byte[] legacySkl()
    {
        // joint 0 translated by (5,6,7), joint 1 global translation (5,6,10)
        LE w = new LE();
        w.ascii("r3d2sklt").i32(2).i32(0).i32(2);
        w.fixed("root", 32).i32(-1).f32(1);
        w.f32(1, 0, 0, 5, 0, 1, 0, 6, 0, 0, 1, 7);
        w.fixed("child", 32).i32(0).f32(1);
        w.f32(1, 0, 0, 5, 0, 1, 0, 6, 0, 0, 1, 10);
        w.i32(2).i32(0).i32(1);
        return w.toBytes();
    }

    @Test
    public void testSKLLegacy()
    {
        SKLFile  f = new SKLParser().parse(reader(legacySkl()));
        var      m = f.getDataV2().getBones().get(0).getMatrix();
        assertEquals(5, m.m30());
        assertEquals(6, m.m31());
        assertEquals(7, m.m32());
        assertEquals(0, m.m01(), "matrix not transposed");
        assertEquals("child", f.getDataV2().getBones().get(1).getName());

        List<ReadableBone> bones = f.toReadableBones();
        assertEquals(3, bones.get(1).getPosition().z, "local transform relative to the parent");
        assertSame(bones.get(0), bones.get(1).getParent());

        byte[] badVersion = legacySkl();
        badVersion[8] = 9;
        assertThrows(ParseException.class, () -> new SKLParser().parse(reader(badVersion)));
    }

    // ---------------------------------------------------------------- ANM

    /**
     * Port of LeagueToolkit QuantizedQuaternion.Compress
     */
    private static int[] compressQuat(Quaternionf q)
    {
        float[] c   = {q.x, q.y, q.z, q.w};
        int     max = 3;
        for (int i = 0; i < 4; i++)
        {
            if (Math.abs(c[i]) > Math.abs(c[max]))
            {
                max = i;
            }
        }
        if (c[max] < 0)
        {
            for (int i = 0; i < 4; i++)
            {
                c[i] = -c[i];
            }
        }
        long bits = (long) max << 45;
        for (int i = 0, ci = 0; i < 4; i++)
        {
            if (i == max)
            {
                continue;
            }
            int comp = (int) Math.round(32767.0 / 2.0 * (1.41421356237 * c[i] + 1.0));
            bits |= (long) (comp & 0x7FFF) << (15 * (2 - ci));
            ci++;
        }
        return new int[]{(int) (bits & 0xFFFF), (int) ((bits >> 16) & 0xFFFF), (int) ((bits >> 32) & 0xFFFF)};
    }

    private static final Quaternionf Q = new Quaternionf().rotateY(0.7f);

    private static void assertSameRotation(Quaternionf expected, Quaternionf actual)
    {
        assertEquals(1, Math.abs(expected.dot(actual)), 1e-4, "rotation " + actual + " vs " + expected);
    }

    @Test
    public void testANMCompressed()
    {
        // r3d2canm v3, 2 joints, 3 frames
        LE w = new LE();
        w.ascii("r3d2canm").i32(3);
        w.i32(0).i32(0).i32(0).i32(2).i32(3).i32(1).f32(2f).f32(30f);
        w.zeros(24);
        w.f32(-10, -10, -10).f32(10, 10, 10).f32(0, 0, 0).f32(2, 2, 2);
        // section offsets are relative to byte 12, so the frames (written right after the 12 offset bytes) start at "here"
        int frames = w.size();
        int jump   = frames + 30;
        int joints = jump + 24 * 2;
        w.i32(frames).i32(jump).i32(joints);
        int[] cq = compressQuat(Q);
        w.i16(0).i16(1).i16(cq[0]).i16(cq[1]).i16(cq[2]);
        w.i16(65535).i16(1 << 14).i16(65535).i16(0).i16(32768);
        w.i16(32768).i16((2 << 14) | 1).i16(0).i16(65535).i16(32768);
        w.zeros(48);
        w.i32(0xAAAA).i32(0xBBBB);

        ANMFile f = new ANMParser().parse(reader(w.toBytes()));
        assertNotNull(f.getVersion1(), "r3d2canm routed to the compressed reader");
        assertNull(f.getVersion3());

        var e = f.getVersion1().getEntries();
        assertEquals(3, e.size());
        assertEquals(1, e.get(0).getHashId());
        assertEquals(0xBBBB, e.get(0).getJointHash());
        assertEquals(0, e.get(0).getDataType());
        assertSameRotation(Q, e.get(0).getRotation());

        assertEquals(1, e.get(1).getDataType());
        assertEquals(10, e.get(1).getVector().x, EPSILON, "translation dequantized");
        assertEquals(-10, e.get(1).getVector().y, EPSILON);
        assertEquals(2f, e.get(1).getTime(), 1e-4);

        assertEquals(2, e.get(2).getDataType());
        assertEquals(2, e.get(2).getVector().y, EPSILON, "scale dequantized");
        assertEquals(List.of(0xAAAA, 0xBBBB), f.getVersion1().getBoneHashes());
    }

    @Test
    public void testANMVersion5()
    {
        // 2 tracks, 1 frame
        LE  w        = new LE();
        int posOff   = 64;
        int rotOff   = posOff + 24;
        int hashOff  = rotOff + 6;
        int frameOff = hashOff + 8;
        w.ascii("r3d2anmd").i32(5);
        w.i32(0).i32(0).i32(0).i32(0).i32(2).i32(1).f32(1 / 30f).i32(hashOff).i32(0).i32(0).i32(posOff).i32(rotOff).i32(frameOff).i32(0).i32(0).i32(0);
        w.f32(1, 2, 3, 4, 5, 6);
        int[] cq = compressQuat(Q);
        w.i16(cq[0]).i16(cq[1]).i16(cq[2]);
        w.i32(0x1234).i32(0x5678);
        w.i16(1).i16(0).i16(0).i16(0xFFFF).i16(1).i16(0);

        ANMFile f  = new ANMParser().parse(reader(w.toBytes()));
        var     fr = f.getVersion5().getFrames().get(0);
        assertEquals(0x1234, fr.get(0).getBoneHash());
        assertEquals(0x5678, fr.get(1).getBoneHash());
        assertEquals(0xFFFF, fr.get(1).getPositionId(), "palette index unsigned");
        assertSameRotation(Q, f.getVersion5().getQuaternions().get(0));
    }

    @Test
    public void testANMVersion4()
    {
        LE  w        = new LE();
        int posOff   = 64;
        int rotOff   = posOff + 12;
        int frameOff = rotOff + 16;
        w.ascii("r3d2anmd").i32(4);
        w.i32(0).i32(0).i32(0).i32(0).i32(1).i32(1).f32(1 / 30f).i32(0).i32(0).i32(0).i32(posOff).i32(rotOff).i32(frameOff).i32(0).i32(0).i32(0);
        w.f32(1, 2, 3).f32(0, 0, 0, 1);
        w.i32(0x42).i16(0).i16(0).i16(0).i16(0);

        ANMFile f = new ANMParser().parse(reader(w.toBytes()));
        assertEquals(30, f.getVersion4().getFPS(), EPSILON, "fps from the float frame duration");
        assertEquals(0x42, f.getVersion4().getFrames().get(0).get(0).getBoneHash());
    }

    @Test
    public void testANMLegacyVersion3()
    {
        LE w = new LE();
        w.ascii("r3d2anmd").i32(3).i32(0).i32(1).i32(1).i32(30);
        w.fixed("bone", 32).i32(0).f32(0, 0, 0, 1).f32(1, 2, 3);
        byte[] data = w.toBytes();

        ANMFile f = new ANMParser().parse(reader(data));
        assertEquals("bone", f.getVersion3().getBones().get(0).getName());
        assertEquals(3, f.getVersion3().getBones().get(0).getFrames().get(0).getTranslation().z);

        byte[] bad = data.clone();
        bad[8] = 1;
        assertThrows(ParseException.class, () -> new ANMParser().parse(reader(bad)), "r3d2anmd v1 rejected");
    }

    // ---------------------------------------------------------------- SCB / SCO

    @Test
    public void testSCB()
    {
        LE w = new LE();
        w.ascii("r3d2Mesh").i16(3).i16(2).fixed("meshname", 128);
        w.i32(3).i32(1).i32(1).f32(0, 0, 0, 1, 1, 1).i32(1);
        w.f32(0, 0, 0, 1, 0, 0, 0, 1, 0);
        w.bytes(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        w.f32(0.5f, 0.5f, 0);
        w.i32(0).i32(1).i32(2).fixed("mat", 64).f32(0, 1, 0, 0, 0, 1);
        w.bytes(new byte[]{10, 20, 30, 40, 50, 60, 70, 80, 90});
        byte[] data = w.toBytes();

        SCBParser parser = new SCBParser();
        assertTrue(parser.canParse(data));
        assertFalse(parser.canParse("[ObjectBegin]".getBytes(StandardCharsets.UTF_8)));
        assertFalse(parser.canParse(new byte[3]));

        SCBFile    f = parser.parse(reader(data));
        SCBContent c = f.getContent();
        assertEquals("meshname", f.getHeader().getFilename(), "filename without NULs");
        assertEquals(1, c.getBoundingBoxMax().x);
        assertEquals(0.5f, c.getCenter().x, "central point");
        assertEquals(3, c.getVertexColors().size(), "per-vertex BGRA colors (v3.2)");
        assertEquals(9, c.getVertexColors().get(2).getX());
        assertEquals("mat", c.getFaces().get(0).getMaterial());
        assertEquals(3, c.getColors().size(), "per-face corner colors");
        assertEquals(90, c.getColors().get(2).getZ());
        assertEquals(1, c.getVertices().get(2).y);
    }

    @Test
    public void testSCO()
    {
        String text = "[ObjectBegin]\r\nName= Box01\r\nCentralPoint= 1.0 2.0 3.0\r\nPivotPoint= 0 0 0\r\nVerts= 3\r\n0 0 0\r\n1 0 0\r\n0 1 0\r\n" +
                      "Faces= 1\r\n3\t0\t1\t2\tlambert1\t0.0 1.0 0.0 0.0 0.0 1.0\r\n[ObjectEnd]\r\n";
        byte[] data = text.getBytes(StandardCharsets.UTF_8);

        SCOParser parser = new SCOParser();
        assertTrue(parser.canParse(data));
        assertTrue(parser.canParse(("﻿ \r\n" + text).getBytes(StandardCharsets.UTF_8)), "BOM and leading whitespace allowed");
        assertFalse(parser.canParse("r3d2Mesh".getBytes(StandardCharsets.UTF_8)));

        SCBFile    f = parser.parse(reader(data));
        SCBContent c = f.getContent();
        assertEquals("Box01", f.getHeader().getFilename());
        assertEquals(3, c.getCenter().z);
        assertNotNull(c.getPivot());
        assertEquals(3, c.getVertices().size());
        assertEquals(2, c.getFaces().get(0).getIndecies().z);
        assertEquals("lambert1", c.getFaces().get(0).getMaterial());
        assertEquals(1, c.getFaces().get(0).getV().z);
    }
}

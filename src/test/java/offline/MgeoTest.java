package offline;

import no.stelar7.cdragon.types.mgeo.MGEOParser;
import no.stelar7.cdragon.types.mgeo.data.*;
import no.stelar7.cdragon.util.readers.*;
import org.junit.jupiter.api.*;

import java.nio.ByteOrder;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline check of the mapgeometry parser: one synthetic file per supported version (5 - 20)
 */
public class MgeoTest
{
    private static RandomAccessReader reader(byte[] data)
    {
        return new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * One mesh with 3 vertices spread over 2 vertex buffers, one index buffer, one submesh, one bucket grid and (v13+) one planar reflector.
     * <p>
     * Buffer 0: POSITION xyz, NORMAL xyz, TEXCOORD0 xy, PRIMARY_COLOR bgra (36 bytes).
     * Buffer 1: TEXCOORD1 x (float) and TEXCOORD2 as XYZ_Packed161616 (4 half floats; 12 bytes).
     */
    static byte[] build(int version)
    {
        LE w = new LE();
        w.ascii("OEGM").i32(version);
        if (version < 7)
        {
            w.u8(0);
        }
        if (version >= 17)
        {
            w.i32(1).i32(0).str32("tex0");
        } else
        {
            if (version >= 9)
            {
                w.str32("tex0");
            }
            if (version >= 11)
            {
                w.str32("tex1");
            }
        }

        // vertex declarations
        w.i32(2).i32(0).i32(4);
        w.i32(0).i32(2).i32(2).i32(2).i32(7).i32(1).i32(4).i32(4);
        w.zeros(8 * 11);
        w.i32(0).i32(2).i32(8).i32(0).i32(9).i32(8);
        w.zeros(8 * 13);

        // vertex buffers
        w.i32(2);
        if (version >= 13)
        {
            w.u8(0xFF);
        }
        w.i32(3 * 36);
        for (int v = 0; v < 3; v++)
        {
            w.f32(v, v + 1, v + 2).f32(0, 1, 0).f32(0.25f * v, 0.5f).bytes(new byte[]{1, 2, 3, (byte) 200});
        }
        if (version >= 13)
        {
            w.u8(0xFF);
        }
        w.i32(3 * 12);
        for (int v = 0; v < 3; v++)
        {
            // half floats 1.5, -2, 0.5, 0
            w.f32(v * 10).i16(0x3E00).i16(0xC000).i16(0x3800).i16(0);
        }

        // index buffers
        w.i32(1);
        if (version >= 13)
        {
            w.u8(0xFF);
        }
        w.i32(6).i16(0).i16(2).i16(0xFFFF);

        // mesh
        w.i32(1);
        if (version <= 11)
        {
            w.str32("mesh0");
        }
        w.i32(3).i32(2).i32(0).i32(0).i32(1);
        w.i32(3).i32(0);
        if (version >= 13)
        {
            w.u8(3);
        }
        if (version >= 18)
        {
            w.i32(77);
        }
        if (version >= 15)
        {
            w.i32(88);
        }
        w.i32(1).i32(0x99).str32("mat").i32(0).i32(3).i32(0).i32(2);
        if (version != 5)
        {
            w.u8(1);
        }
        w.f32(0, 0, 0, 2, 3, 4);
        w.f32(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 7, 8, 9, 1);
        w.u8(5);
        if (version >= 7 && version <= 12)
        {
            w.u8(6);
        }
        if (version >= 11 && version < 14)
        {
            w.u8(1);
        } else if (version >= 14)
        {
            w.u8(2);
            if (version < 16)
            {
                w.u8(1);
            } else
            {
                w.i16(0x101);
            }
        }
        if (version < 9)
        {
            w.zeros(9 * 12).str32("lightmap").f32(1, 1, 0, 0);
        } else
        {
            w.str32("lightmap").f32(1, 1, 0, 0).str32("stationary").f32(1, 1, 0, 0);
            if (version >= 12 && version < 17)
            {
                w.str32("paint").f32(1, 1, 0, 0);
            }
            if (version >= 17)
            {
                w.i32(1).i32(2).str32("override").f32(3, 3).f32(4, 4);
            }
        }
        if (version >= 19)
        {
            w.u8(1);
        }
        if (version >= 20)
        {
            w.i32(5).bytes(new byte[]{0x47, 0x72, (byte) 0xF0, (byte) 0xD3, 9});
        }

        // bucketed geometry
        if (version >= 15)
        {
            w.i32(1);
        }
        if (version >= 18)
        {
            w.i32(0x1234);
        }
        if (version >= 15)
        {
            w.i32(5);
        }
        w.f32(-1, -1, 1, 1, 0, 0, 1, 1);
        w.i16(1).u8(0).u8(1).i32(3).i32(3);
        w.f32(0, 0, 0, 1, 0, 0, 0, 0, 1);
        w.i16(0).i16(1).i16(2);
        w.f32(0, 0).i32(0).i32(0).i16(1).i16(0);
        w.u8(7);

        // planar reflectors
        if (version >= 13)
        {
            w.i32(1).f32(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1).f32(0, 0, 0, 1, 1, 1).f32(0, 1, 0);
        }
        return w.toBytes();
    }

    @TestFactory
    public Stream<DynamicTest> testVersions()
    {
        return IntStream.rangeClosed(5, 20).mapToObj(v -> DynamicTest.dynamicTest("mapgeo v" + v, () -> check(v)));
    }

    private static void check(int version)
    {
        MGEOFile           f  = new MGEOParser().parse(reader(build(version)));
        MGEOFileMesh       m  = f.getMeshes().get(0);
        MGEOFileMeshVertex v2 = m.getVertices().get(2);

        // vertex 2 decoded from the declarations
        assertEquals(2, v2.getPosition().x);
        assertEquals(4, v2.getPosition().z);
        assertEquals(0.5f, v2.getUv1().x);
        assertEquals((byte) 200, v2.getPrimaryColor().getW());
        assertEquals(20, v2.getExtra().get("TEXCOORD_1")[0]);
        assertArrayEquals(new float[]{1.5f, -2f, 0.5f, 0}, v2.getExtra().get("TEXCOORD_2"), "XYZ_Packed161616 decoded as 4 half floats");

        assertEquals(0xFFFF, m.getIndices()[2], "indices unsigned");

        // mesh tail
        assertEquals(7, m.getTransformationMatrix().m30());
        assertEquals("lightmap", m.getLightMap());
        assertEquals("mat", m.getSubMeshes().get(0).getName());
        assertEquals(version <= 11, "mesh0".equals(m.getName()), "mesh names only up to v11");

        int expectedFlags = version < 11 ? 0 : version < 16 ? 1 : 0x101;
        assertEquals(expectedFlags, m.getRenderFlags(), "render flags");
        if (version >= 14)
        {
            assertEquals(2, m.getLayerTransitionBehavior());
        }
        assertEquals(version < 9, m.getSphericalHarmonics() != null, "spherical harmonics before v9");
        assertEquals(version < 9, m.getStationaryLight() == null, "stationary light from v9");
        if (version == 17)
        {
            assertEquals(3, m.getBakedPaintScale().x, "texture override scale");
        }

        assertEquals(version >= 19 ? 1 : -1, m.getVersion19Byte());
        if (version >= 20)
        {
            assertNotNull(m.getExtensionData());
            assertEquals(5, m.getExtensionData().length);
            assertEquals(9, m.getExtensionData()[4]);
        } else
        {
            assertNull(m.getExtensionData());
        }

        int expectedSamplers = version >= 17 ? 1 : version >= 11 ? 2 : version >= 9 ? 1 : 0;
        assertEquals(expectedSamplers, f.getShaderTextureOverrides().size(), "sampler definitions");

        // bucketed geometry
        var g = f.getBucketGeometry();
        assertEquals(3, g.getIndices().length);
        assertEquals(1, g.getBuckets()[0][0].getInsideFaceCount());
        assertEquals(7, g.getFaceVisibilityFlags()[0]);
        assertEquals(version >= 18 ? 0x1234 : 0, g.getRegionPathHash());
        assertEquals(version >= 15 ? 5 : 0, g.getVisibilityControllerPathHash());

        assertEquals(version >= 13 ? 1 : 0, f.getPlanarReflectors() == null ? 0 : f.getPlanarReflectors().size(), "planar reflectors from v13");
    }

    @Test
    public void testUnknownVersionRejected()
    {
        byte[] data = new LE().ascii("OEGM").i32(21).toBytes();
        assertThrows(ParseException.class, () -> new MGEOParser().parse(reader(data)));

        byte[] badMagic = build(13);
        badMagic[0] = 'X';
        assertThrows(ParseException.class, () -> new MGEOParser().parse(reader(badMagic)));
    }

    @Test
    public void testTruncatedRejected()
    {
        byte[] full      = build(18);
        byte[] truncated = java.util.Arrays.copyOf(full, full.length / 2);
        // a truncated file fails cleanly (the reader throws IndexOutOfBoundsException past the end, not a ParseException) instead of returning garbage
        assertThrows(RuntimeException.class, () -> new MGEOParser().parse(reader(truncated)));
    }
}

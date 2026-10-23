package offline;

import no.stelar7.cdragon.types.ngrid.NGridParser;
import no.stelar7.cdragon.types.ngrid.data.*;
import no.stelar7.cdragon.types.ngrid.data.flags.*;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.ByteArray;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Navigation grids (versions 2, 3, 5 and 7) built in code: a 2x1 cell grid, 2x2 height samples and 2 hint nodes
 */
public class NGridTest
{
    private static final int COUNT_X     = 2;
    private static final int COUNT_Z     = 1;
    private static final int CELLS       = COUNT_X * COUNT_Z;
    private static final int UNKNOWN_BLK = 132;

    private static LE header(int major)
    {
        LE w = new LE().u8(major);
        if (major != 2)
        {
            w.i16(1);
        }
        return w.f32(-10, 0, -20).f32(10, 5, 20).f32(50).i32(COUNT_X).i32(COUNT_Z);
    }

    private static void cellV5(LE w, int i, int arrivalDirection, int unknown2, int hint1, int hint2)
    {
        w.f32(1.5f + i).i32(100 + i).f32(2.5f).i32(0).f32(3.5f).i32(7);
        w.i16(i).i16(0);
        w.f32(0.25f).f32(0.5f).i32(3).i32(4).f32(0.75f);
        w.i16(arrivalDirection).i16(unknown2).i16(hint1).i16(hint2);
    }

    private static void cellV7(LE w, int i)
    {
        w.f32(1.5f + i).i32(100 + i).f32(2.5f).i32(0).f32(3.5f);
        w.i16(i).i16(0);
        w.i32(7).i32(8).i32(4).f32(0.75f);
        w.i16(9).i16(0xFFFF).i16(11).i16(12);
    }

    private static void tail(LE w)
    {
        // height samples: 2x2, offsets, values
        w.i32(2).i32(2).f32(0.5f).f32(0.25f).f32(3, -1, 7, 2);
        // hint nodes: 2 nodes of 2 distances and 2 coordinates
        w.f32(0, 1).i16(1).i16(2);
        w.f32(1, 0).i16(3).i16(4);
    }

    private static byte[] version7()
    {
        LE w = header(7);
        for (int i = 0; i < CELLS; i++)
        {
            cellV7(w, i);
        }
        // vision flags (u16 per cell), then river, jungle/main, lane/poi, ring bytes per cell
        w.i16(0b11).i16(0b100);
        w.u8(0b10000).u8(0x21).u8(0x12).u8(1);
        w.u8(0).u8(0).u8(0).u8(0);
        w.zeros(8 * UNKNOWN_BLK);
        tail(w);
        return w.toBytes();
    }

    private static byte[] version5(int major)
    {
        LE w = header(major);
        for (int i = 0; i < CELLS; i++)
        {
            if (major == 2)
            {
                // version 2 keeps the vision flags in the high byte of the arrival direction when the second hint node is 0
                cellV5(w, i, (0b10 << 8) | 0x05, 21, 22, 0);
            } else
            {
                cellV5(w, i, 0x105, 21, 22, 23);
            }
        }
        if (major == 5)
        {
            w.u8(0b1).u8(0x21);
            w.u8(0b10).u8(0x00);
            w.zeros(4 * UNKNOWN_BLK);
        }
        tail(w);
        return w.toBytes();
    }

    private static void checkCommon(NGridFile f)
    {
        assertEquals(-10f, f.getMin().x);
        assertEquals(20f, f.getMax().z);
        assertEquals(50f, f.getSize());
        assertEquals(COUNT_X, f.getCountX());
        assertEquals(COUNT_Z, f.getCountZ());
        assertEquals(CELLS, f.getCells().size());

        assertEquals(2, f.getHeightSamplesX());
        assertEquals(0.25f, f.getHeightSampleOffsetZ());
        assertArrayEquals(new float[]{3, -1, 7, 2}, f.getHeightSamples());
        assertEquals(-1f, f.getMinHeightSample());
        assertEquals(7f, f.getMaxHeightSample());

        assertEquals(2, f.getHintNodeCount(), "hint node count derived from the section size");
        assertArrayEquals(new float[]{0, 1, 1, 0}, f.getHintNodeDistances());
        assertArrayEquals(new short[]{1, 2, 3, 4}, f.getHintNodeCoordinates());

        NGridFileCell c = f.getCells().get(1);
        assertEquals(1, c.getIndex());
        assertEquals(2.5f, c.getCenterHeight());
        assertEquals(101, c.getSessionId());
        assertEquals(1, c.getX());
        assertEquals(7, c.getActorList());
    }

    @Test
    public void testVersion7()
    {
        NGridFile f = new NGridParser().parse(new ByteArray(version7()));
        assertEquals(7, f.getMajor());
        assertEquals(1, f.getMinor());
        checkCommon(f);

        NGridFileCell c0 = f.getCells().get(0);
        assertEquals(8, c0.getUnknown1());
        assertEquals(4, c0.getGoodCellSessionId());
        assertEquals(0xFFFF, c0.getArrivalDirection(), "arrival direction is unsigned");
        assertEquals(11, c0.getHintNode1());
        assertEquals(12, c0.getHintNode2());

        // the right raw values reach the flag decoders (what they decode to is checked in testFlagDecoding)
        assertEquals(VisionPathFlag.valueOf(0b11), c0.getVisionPath());
        assertEquals(VisionPathFlag.valueOf(0b100), f.getCells().get(1).getVisionPath());
        assertEquals(RiverRegionFlag.valueOf(0b10000), c0.getRiverRegion());
        assertEquals(JungleQuadrantFlag.valueOf(1), c0.getJungleQuadrant());
        assertEquals(MainRegionFlag.valueOf(2), c0.getMainRegion());
        assertEquals(NearestLaneFlag.valueOf(2), c0.getNearestLane());
        assertEquals(POIFlag.valueOf(1), c0.getPoi());
        assertEquals(RingFlag.valueOf(1), c0.getRing());
    }

    @Test
    public void testVersion5()
    {
        NGridFile f = new NGridParser().parse(new ByteArray(version5(5)));
        assertEquals(5, f.getMajor());
        checkCommon(f);

        NGridFileCell c0 = f.getCells().get(0);
        assertEquals(0.25f, c0.getAdditionalCost());
        assertEquals(0.5f, c0.getGoodCellHint());
        assertEquals(3, c0.getAdditionalCostCount());
        assertEquals(0x105, c0.getArrivalDirection());
        assertEquals(22, c0.getHintNode1());
        assertEquals(23, c0.getHintNode2());
        assertEquals(RiverRegionFlag.valueOf(0b1), c0.getRiverRegion());
        assertEquals(JungleQuadrantFlag.valueOf(1), c0.getJungleQuadrant());
        assertEquals(MainRegionFlag.valueOf(2), c0.getMainRegion());
        assertEquals(RiverRegionFlag.valueOf(0b10), f.getCells().get(1).getRiverRegion());
    }

    @Test
    public void testVersion3()
    {
        NGridFile f = new NGridParser().parse(new ByteArray(version5(3)));
        assertEquals(3, f.getMajor());
        checkCommon(f);
        assertEquals(0x105, f.getCells().get(0).getArrivalDirection(), "no vision flags unpacking outside version 2");
    }

    @Test
    public void testVersion2()
    {
        NGridFile f = new NGridParser().parse(new ByteArray(version5(2)));
        assertEquals(2, f.getMajor());
        assertEquals(0, f.getMinor(), "version 2 has no minor version");
        checkCommon(f);

        NGridFileCell c0 = f.getCells().get(0);
        assertEquals(0x05, c0.getArrivalDirection());
        assertEquals(VisionPathFlag.valueOf(0b10), c0.getVisionPath(), "vision flags from the high byte");
        assertEquals(21, c0.getHintNode1(), "hint nodes are shifted");
        assertEquals(22, c0.getHintNode2());
    }

    @Test
    public void testFlagDecoding()
    {
        // bit masks: every set bit, WALKABLE / NON_JUNGLE only when nothing is set
        assertEquals(List.of(VisionPathFlag.BRUSH, VisionPathFlag.WALL), VisionPathFlag.valueOf(0b11));
        assertEquals(List.of(VisionPathFlag.BRUSH), VisionPathFlag.valueOf(0b1));
        assertEquals(List.of(VisionPathFlag.STRUCTURE, VisionPathFlag.ALWAYS_VISIBLE), VisionPathFlag.valueOf(0b100000100));
        assertEquals(List.of(VisionPathFlag.WALKABLE), VisionPathFlag.valueOf(0));
        assertEquals(List.of(RiverRegionFlag.RIVER), RiverRegionFlag.valueOf(0b10000));
        assertEquals(List.of(RiverRegionFlag.JUNGLE, RiverRegionFlag.BARON), RiverRegionFlag.valueOf(0b11));

        // plain values: exactly the one matching constant
        assertEquals(List.of(MainRegionFlag.TOP_LANE), MainRegionFlag.valueOf(2));
        assertEquals(List.of(MainRegionFlag.SPAWN), MainRegionFlag.valueOf(0));
        assertEquals(List.of(JungleQuadrantFlag.WEST), JungleQuadrantFlag.valueOf(3));
        assertEquals(List.of(NearestLaneFlag.BLUE_BOT_LANE), NearestLaneFlag.valueOf(2));
        assertEquals(List.of(POIFlag.TURRET), POIFlag.valueOf(1));
        assertEquals(List.of(RingFlag.BLUE_INNER_TO_OUTER), RingFlag.valueOf(3));
    }

    @Test
    public void testInvalid()
    {
        NGridParser parser = new NGridParser();

        byte[] v4 = version5(3);
        v4[0] = 4;
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(v4)), "unsupported version");

        byte[] hugeCount = header(7).toBytes();
        hugeCount[hugeCount.length - 8] = 0x7F;
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(hugeCount)), "cell count larger than the data");

        byte[] noFlags = Arrays.copyOf(version7(), 1 + 2 + 7 * 4 + 8 + CELLS * 48 + 10);
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(noFlags)), "truncated region flags");

        byte[] v7          = version7();
        byte[] badSamples  = v7.clone();
        int    sampleCount = v7.length - (2 * 12) - 16 - 16;
        badSamples[sampleCount] = 0x7F;
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(badSamples)), "height sample count larger than the data");

        byte[] badHints = Arrays.copyOf(v7, v7.length - 3);
        assertThrows(ParseException.class, () -> parser.parse(new ByteArray(badHints)), "hint node section of an unknown size");
    }
}

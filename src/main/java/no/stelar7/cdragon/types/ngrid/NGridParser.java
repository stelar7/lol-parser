package no.stelar7.cdragon.types.ngrid;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.ngrid.data.*;
import no.stelar7.cdragon.types.ngrid.data.flags.*;
import no.stelar7.cdragon.util.readers.*;

/**
 * Navigation grid (.aimesh_ngrid) reader. Supported versions: 2, 3, 5 and 7.
 */
public class NGridParser implements Parseable<NGridFile>
{
    // cell record sizes in bytes
    private static final int CELL_SIZE_V5 = 56;
    private static final int CELL_SIZE_V7 = 48;

    // size of the unknown blocks following the region flags
    private static final int UNKNOWN_BLOCK_SIZE = 132;

    @Override
    public NGridFile parse(RandomAccessReader raf)
    {
        NGridFile file = new NGridFile();

        file.setMajor(raf.readU8());
        if (file.getMajor() != 2)
        {
            file.setMinor(raf.readU16());
        }

        if (file.getMajor() != 2 && file.getMajor() != 3 && file.getMajor() != 5 && file.getMajor() != 7)
        {
            throw new ParseException("Unsupported ngrid version " + file.getMajor(), raf);
        }

        file.setMin(raf.readVec3F());
        file.setMax(raf.readVec3F());

        file.setSize(raf.readFloat());
        file.setCountX(raf.readInt());
        file.setCountZ(raf.readInt());

        long totalCount = (long) file.getCountX() * file.getCountZ();
        int  cellSize   = file.getMajor() == 7 ? CELL_SIZE_V7 : CELL_SIZE_V5;
        if (file.getCountX() < 0 || file.getCountZ() < 0 || totalCount * cellSize > raf.remaining())
        {
            throw new ParseException("Invalid ngrid cell count " + file.getCountX() + "x" + file.getCountZ(), raf);
        }

        if (file.getMajor() == 7)
        {
            readCellsVersion7(file, raf, (int) totalCount);
        } else
        {
            readCellsVersion5(file, raf, (int) totalCount);
        }

        readHeightSamples(file, raf);
        readHintNodes(file, raf);

        return file;
    }

    /**
     * The hint grid is N nodes, each with N float distances followed by two shorts (the node's cell coordinates).
     * N is not stored in the file, so it is derived from the size of the section (N = 900 in every known file).
     */
    private void readHintNodes(NGridFile file, RandomAccessReader raf)
    {
        long remaining = raf.remaining();
        if (remaining == 0)
        {
            return;
        }

        long count = Math.round((Math.sqrt(1 + (double) remaining) - 1) / 2);
        if (count * (count * 4 + 4) != remaining)
        {
            // trailing data we do not know about, fall back to the known size
            count = 900;
            if (count * (count * 4 + 4) > remaining)
            {
                throw new ParseException("Invalid ngrid hint node section size " + remaining, raf);
            }
        }

        int     n           = (int) count;
        float[] distances   = new float[n * n];
        short[] coordinates = new short[n * 2];
        for (int i = 0; i < n; i++)
        {
            float[] row = raf.readFloatArray(n);
            System.arraycopy(row, 0, distances, i * n, n);

            coordinates[i * 2] = raf.readShort();
            coordinates[i * 2 + 1] = raf.readShort();
        }

        file.setHintNodeCount(n);
        file.setHintNodeDistances(distances);
        file.setHintNodeCoordinates(coordinates);
    }

    private void readHeightSamples(NGridFile file, RandomAccessReader raf)
    {
        file.setHeightSamplesX(raf.readInt());
        file.setHeightSamplesZ(raf.readInt());

        file.setHeightSampleOffsetX(raf.readFloat());
        file.setHeightSampleOffsetZ(raf.readFloat());

        long totalCount = (long) file.getHeightSamplesX() * file.getHeightSamplesZ();
        if (file.getHeightSamplesX() < 0 || file.getHeightSamplesZ() < 0 || totalCount * 4 > raf.remaining())
        {
            throw new ParseException("Invalid ngrid height sample count", raf);
        }

        float[] samples = raf.readFloatArray((int) totalCount);
        for (int i = 0; i < samples.length; i++)
        {
            float sample = samples[i];
            if (i == 0 || sample < file.getMinHeightSample())
            {
                file.setMinHeightSample(sample);
            }

            if (i == 0 || sample > file.getMaxHeightSample())
            {
                file.setMaxHeightSample(sample);
            }
        }
        file.setHeightSamples(samples);
    }

    private void readCellsVersion5(NGridFile file, RandomAccessReader raf, int totalCount)
    {
        for (int i = 0; i < totalCount; i++)
        {
            NGridFileCell cell = new NGridFileCell();

            cell.setIndex(i);
            cell.setCenterHeight(raf.readFloat());
            cell.setSessionId(raf.readInt());
            cell.setArrivalCost(raf.readFloat());
            cell.setOpen(raf.readInt());
            cell.setHeuristic(raf.readFloat());
            cell.setActorList(raf.readInt());

            cell.setX(raf.readShort());
            cell.setZ(raf.readShort());

            cell.setAdditionalCost(raf.readFloat());
            cell.setGoodCellHint(raf.readFloat());
            cell.setAdditionalCostCount(raf.readInt());
            cell.setGoodCellSessionId(raf.readInt());
            cell.setHintWeight(raf.readFloat());

            cell.setArrivalDirection(raf.readU16());
            cell.setUnknown2(raf.readShort());
            cell.setHintNode1(raf.readShort());
            cell.setHintNode2(raf.readShort());

            if (file.getMajor() == 2 && cell.getHintNode2() == 0)
            {
                // version 2 packs the vision flags into the high byte of the arrival direction
                cell.setHintNode2(cell.getHintNode1());
                cell.setHintNode1(cell.getUnknown2());
                cell.setVisionPath(VisionPathFlag.valueOf((cell.getArrivalDirection() >>> 8) & 0xFF));
                cell.setArrivalDirection(cell.getArrivalDirection() & 0xFF);
            }

            file.getCells().add(cell);
        }

        if (file.getMajor() == 5)
        {
            if ((long) totalCount * 2 + 4L * UNKNOWN_BLOCK_SIZE > raf.remaining())
            {
                throw new ParseException("ngrid region flags are truncated", raf);
            }

            byte[] flags = raf.readBytes(totalCount * 2L);
            for (int i = 0; i < totalCount; i++)
            {
                NGridFileCell cell = file.getCells().get(i);
                cell.setRiverRegion(RiverRegionFlag.valueOf(flags[i * 2] & 0xFF));

                int jungleAndMain = flags[i * 2 + 1] & 0xFF;
                cell.setJungleQuadrant(JungleQuadrantFlag.valueOf(jungleAndMain & 0x0F));
                cell.setMainRegion(MainRegionFlag.valueOf(jungleAndMain >>> 4));
            }

            // version 5 only has 4 blocks of 132 bytes each instead of version 7's 8 blocks of 132 bytes each
            raf.seekFromCurrentPosition(4L * UNKNOWN_BLOCK_SIZE);
        }
    }

    private void readCellsVersion7(NGridFile file, RandomAccessReader raf, int totalCount)
    {
        for (int i = 0; i < totalCount; i++)
        {
            NGridFileCell cell = new NGridFileCell();

            cell.setIndex(i);
            cell.setCenterHeight(raf.readFloat());
            cell.setSessionId(raf.readInt());
            cell.setArrivalCost(raf.readFloat());
            cell.setOpen(raf.readInt());
            cell.setHeuristic(raf.readFloat());
            cell.setX(raf.readShort());
            cell.setZ(raf.readShort());
            cell.setActorList(raf.readInt());
            cell.setUnknown1(raf.readInt());
            cell.setGoodCellSessionId(raf.readInt());
            cell.setHintWeight(raf.readFloat());
            cell.setUnknown2(raf.readShort());
            cell.setArrivalDirection(raf.readU16());
            cell.setHintNode1(raf.readShort());
            cell.setHintNode2(raf.readShort());

            file.getCells().add(cell);
        }

        // vision flags (u16) + river (u8) + jungle/main (u8) + lane/poi (u8) + ring (u8)
        if ((long) totalCount * 6 + 8L * UNKNOWN_BLOCK_SIZE > raf.remaining())
        {
            throw new ParseException("ngrid region flags are truncated", raf);
        }

        int[] vision = raf.readU16Array(totalCount);
        for (int i = 0; i < totalCount; i++)
        {
            file.getCells().get(i).setVisionPath(VisionPathFlag.valueOf(vision[i]));
        }

        byte[] flags = raf.readBytes(totalCount * 4L);
        for (int i = 0; i < totalCount; i++)
        {
            NGridFileCell cell = file.getCells().get(i);
            int           o    = i * 4;

            cell.setRiverRegion(RiverRegionFlag.valueOf(flags[o] & 0xFF));

            int jungleAndMain = flags[o + 1] & 0xFF;
            cell.setJungleQuadrant(JungleQuadrantFlag.valueOf(jungleAndMain & 0x0F));
            cell.setMainRegion(MainRegionFlag.valueOf(jungleAndMain >>> 4));

            int laneAndPOI = flags[o + 2] & 0xFF;
            cell.setNearestLane(NearestLaneFlag.valueOf(laneAndPOI & 0x0F));
            cell.setPoi(POIFlag.valueOf(laneAndPOI >>> 4));

            cell.setRing(RingFlag.valueOf(flags[o + 3] & 0xFF));
        }

        // appears to be 8 blocks of 132 bytes each, but in practice only 7 are used and the 8th is all zeros
        //
        // roughly appears to be 8 bytes of maybe some sort of hash followed by alternating between four bytes of zero and four bytes
        // of garbage (a couple make valid floats, most are invalid floats, maybe more hashes?)
        //
        // at a certain point, each block becomes all zero for the rest of the block, but this varies by block (appears to be around
        // 40-48 bytes after the first 8 bytes until the rest is all zero)
        raf.seekFromCurrentPosition(8L * UNKNOWN_BLOCK_SIZE);
    }
}

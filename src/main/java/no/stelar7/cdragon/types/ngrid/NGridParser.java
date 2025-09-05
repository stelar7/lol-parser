package no.stelar7.cdragon.types.ngrid;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.ngrid.data.*;
import no.stelar7.cdragon.types.ngrid.data.flags.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;

public class NGridParser implements Parseable<NGridFile>
{
    @Override
    public NGridFile parse(Path path)
    {
        RandomAccessReader raf = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN);
        return parse(raf);
    }
    
    @Override
    public NGridFile parse(ByteArray data)
    {
        RandomAccessReader raf = new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN);
        return parse(raf);
    }
    
    @Override
    public NGridFile parse(RandomAccessReader raf)
    {
        NGridFile file = new NGridFile();
        
        file.setMajor(raf.readByte());
        if (file.getMajor() != 2)
        {
            file.setMinor(raf.readShort());
        }
        
        if (file.getMajor() != 2 && file.getMajor() != 3 && file.getMajor() != 5 && file.getMajor() != 7)
        {
            System.err.println("Unsupported file version: " + file.getMajor());
            return null;
        }
        
        
        file.setMin(raf.readVec3F());
        file.setMax(raf.readVec3F());
        
        file.setSize(raf.readFloat());
        file.setCountX(raf.readInt());
        file.setCountZ(raf.readInt());
        
        if (file.getMajor() == 7)
        {
            readCellsVersion7(file, raf);
        } else
        {
            readCellsVersion5(file, raf);
        }
        
        readHeightSamples(file, raf);
        readHintNodes(file, raf);
        
        return file;
    }
    
    private void readHintNodes(NGridFile file, RandomAccessReader raf)
    {
        // this 900 value *might* be variable, in practice it's constant though
        for (int i = 0; i < 900; i++)
        {
            for (int j = 0; j < 900; j++)
            {
                // this appears to be a distance to another cell, but not sure how cells are indexed here, also seems to be mostly whole numbers
                raf.readFloat();
            }
    
            // are these what is referred to by 'hint nodes' in NavGridCell?
            raf.readShort();
            raf.readShort();
        }
    }
    
    private void readHeightSamples(NGridFile file, RandomAccessReader raf)
    {
        file.setHeightSamplesX(raf.readInt());
        file.setHeightSamplesZ(raf.readInt());
        
        file.setHeightSampleOffsetX(raf.readFloat());
        file.setHeightSampleOffsetZ(raf.readFloat());
        
        int totalCount = file.getHeightSamplesX() * file.getHeightSamplesZ();
        for (int i = 0; i < totalCount; i++)
        {
            float sample = raf.readFloat();
            if (i == 0 || sample < file.getMinHeightSample())
            {
                file.setMinHeightSample(sample);
            }
            
            if (i == 0 || sample > file.getMaxHeightSample())
            {
                file.setMaxHeightSample(sample);
            }
            
            file.getHeightSamples().add(sample);
        }
    }
    
    private void readCellsVersion5(NGridFile file, RandomAccessReader raf)
    {
        int totalCount = file.getCountX() * file.getCountZ();
        
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
            
            cell.setArrivalDirection(raf.readShort());
            cell.setUnknown2(raf.readShort());
            cell.setHintNode1(raf.readShort());
            cell.setHintNode2(raf.readShort());
            
            if (file.getMajor() == 2 && cell.getHintNode2() == 0)
            {
                cell.setHintNode2(cell.getHintNode1());
                cell.setHintNode1(cell.getUnknown2());
                cell.setVisionPath(VisionPathFlag.valueOf((cell.getArrivalDirection() & ~0xff) >> 8));
                cell.setArrivalDirection((cell.getArrivalDirection() & 0xff));
            }
            
            file.getCells().add(cell);
        }
        
        if (file.getMajor() == 5)
        {
            for (int i = 0; i < totalCount; i++)
            {
                file.getCells().get(i).setRiverRegion(RiverRegionFlag.valueOf(raf.readByte()));
                
                byte jungleAndMain = raf.readByte();
                file.getCells().get(i).setJungleQuadrant(JungleQuadrantFlag.valueOf(jungleAndMain & 0x0f));
                file.getCells().get(i).setMainRegion(MainRegionFlag.valueOf((jungleAndMain & ~0x0f) >> 4));
            }
            
            // version 5 only has 4 blocks of 132 bytes each instead of version 7's 8 blocks of 132 bytes each
            for (int i = 0; i < 4; i++)
            {
                for (int i1 = 0; i1 < 132; i1++)
                {
                    raf.readByte();
                }
            }
        }
    }
    
    private void readCellsVersion7(NGridFile file, RandomAccessReader raf)
    {
        int totalCount = file.getCountX() * file.getCountZ();
        
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
            cell.setArrivalDirection(raf.readShort());
            cell.setHintNode1(raf.readShort());
            cell.setHintNode2(raf.readShort());
            
            file.getCells().add(cell);
        }
        
        for (int i = 0; i < totalCount; i++)
        {
            file.getCells().get(i).setVisionPath(VisionPathFlag.valueOf(raf.readShort()));
        }
        
        for (int i = 0; i < totalCount; i++)
        {
            file.getCells().get(i).setRiverRegion(RiverRegionFlag.valueOf(raf.readByte()));
            
            byte jungleAndMain = raf.readByte();
            file.getCells().get(i).setJungleQuadrant(JungleQuadrantFlag.valueOf(jungleAndMain & 0x0f));
            file.getCells().get(i).setMainRegion(MainRegionFlag.valueOf((jungleAndMain & ~0x0f) >> 4));
            
            byte laneAndPOI = raf.readByte();
            file.getCells().get(i).setNearestLane(NearestLaneFlag.valueOf(laneAndPOI & 0x0f));
            file.getCells().get(i).setPoi(POIFlag.valueOf((laneAndPOI & ~0x0f) >> 4));
            
            file.getCells().get(i).setRing(RingFlag.valueOf(raf.readByte()));
        }
        
        
        // appears to be 8 blocks of 132 bytes each, but in practice only 7 are used and the 8th is all zeros
        //
        // roughly appears to be 8 bytes of maybe some sort of hash followed by alternating between four bytes of zero and four bytes
        // of garbage (a couple make valid floats, most are invalid floats, maybe more hashes?)
        //
        // at a certain point, each block becomes all zero for the rest of the block, but this varies by block (appears to be around
        // 40-48 bytes after the first 8 bytes until the rest is all zero)
        for (int i = 0; i < 8; i++)
        {
            for (int i1 = 0; i1 < 132; i1++)
            {
                raf.readByte();
            }
        }
    }
}

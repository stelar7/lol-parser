package no.stelar7.cdragon.types.ngrid.data;

import no.stelar7.cdragon.types.ngrid.data.flags.*;

import java.util.*;

public class NGridFileCell
{
    private int x;
    private int z;
    private int index;
    
    private List<VisionPathFlag>     visionPath;
    private List<RiverRegionFlag>    riverRegion;
    private List<JungleQuadrantFlag> jungleQuadrant;
    private List<MainRegionFlag>     mainRegion;
    private List<NearestLaneFlag>    nearestLane;
    private List<POIFlag>            poi;
    private List<RingFlag>           ring;
    
    private float centerHeight;
    private int   sessionId;
    private float arrivalCost;
    private int   open;
    private float heuristic;
    private int   actorList;
    private int   unknown1;
    private int   goodCellSessionId;
    private float hintWeight;
    private short unknown2;
    private int   arrivalDirection;
    private short hintNode1;
    private short hintNode2;
    private float additionalCost;
    private float goodCellHint;
    private int   additionalCostCount;
    
    public float getAdditionalCost()
    {
        return additionalCost;
    }
    
    public void setAdditionalCost(float additionalCost)
    {
        this.additionalCost = additionalCost;
    }
    
    public int getAdditionalCostCount()
    {
        return additionalCostCount;
    }
    
    public void setAdditionalCostCount(int additionalCostCount)
    {
        this.additionalCostCount = additionalCostCount;
    }
    
    public float getGoodCellHint()
    {
        return goodCellHint;
    }
    
    public void setGoodCellHint(float goodCellHint)
    {
        this.goodCellHint = goodCellHint;
    }
    
    public int getX()
    {
        return x;
    }
    
    public void setX(int x)
    {
        this.x = x;
    }
    
    public int getZ()
    {
        return z;
    }
    
    public void setZ(int z)
    {
        this.z = z;
    }
    
    public int getIndex()
    {
        return index;
    }
    
    public void setIndex(int index)
    {
        this.index = index;
    }
    
    public List<VisionPathFlag> getVisionPath()
    {
        return visionPath;
    }
    
    public void setVisionPath(List<VisionPathFlag> visionPath)
    {
        this.visionPath = visionPath;
    }
    
    public List<RiverRegionFlag> getRiverRegion()
    {
        return riverRegion;
    }
    
    public void setRiverRegion(List<RiverRegionFlag> riverRegion)
    {
        this.riverRegion = riverRegion;
    }
    
    public List<JungleQuadrantFlag> getJungleQuadrant()
    {
        return jungleQuadrant;
    }
    
    public void setJungleQuadrant(List<JungleQuadrantFlag> jungleQuadrant)
    {
        this.jungleQuadrant = jungleQuadrant;
    }
    
    public List<MainRegionFlag> getMainRegion()
    {
        return mainRegion;
    }
    
    public void setMainRegion(List<MainRegionFlag> mainRegion)
    {
        this.mainRegion = mainRegion;
    }
    
    public List<NearestLaneFlag> getNearestLane()
    {
        return nearestLane;
    }
    
    public void setNearestLane(List<NearestLaneFlag> nearestLane)
    {
        this.nearestLane = nearestLane;
    }
    
    public List<POIFlag> getPoi()
    {
        return poi;
    }
    
    public void setPoi(List<POIFlag> poi)
    {
        this.poi = poi;
    }
    
    public List<RingFlag> getRing()
    {
        return ring;
    }
    
    public void setRing(List<RingFlag> ring)
    {
        this.ring = ring;
    }
    
    public float getCenterHeight()
    {
        return centerHeight;
    }
    
    public void setCenterHeight(float centerHeight)
    {
        this.centerHeight = centerHeight;
    }
    
    public int getSessionId()
    {
        return sessionId;
    }
    
    public void setSessionId(int sessionId)
    {
        this.sessionId = sessionId;
    }
    
    public float getArrivalCost()
    {
        return arrivalCost;
    }
    
    public void setArrivalCost(float arrivalCost)
    {
        this.arrivalCost = arrivalCost;
    }
    
    public int getOpen()
    {
        return open;
    }
    
    public void setOpen(int open)
    {
        this.open = open;
    }
    
    public float getHeuristic()
    {
        return heuristic;
    }
    
    public void setHeuristic(float heuristic)
    {
        this.heuristic = heuristic;
    }
    
    public int getActorList()
    {
        return actorList;
    }
    
    public void setActorList(int actorList)
    {
        this.actorList = actorList;
    }
    
    public int getUnknown1()
    {
        return unknown1;
    }
    
    public void setUnknown1(int unknown1)
    {
        this.unknown1 = unknown1;
    }
    
    public int getGoodCellSessionId()
    {
        return goodCellSessionId;
    }
    
    public void setGoodCellSessionId(int goodCellSessionId)
    {
        this.goodCellSessionId = goodCellSessionId;
    }
    
    public float getHintWeight()
    {
        return hintWeight;
    }
    
    public void setHintWeight(float hintWeight)
    {
        this.hintWeight = hintWeight;
    }
    
    public short getUnknown2()
    {
        return unknown2;
    }
    
    public void setUnknown2(short unknown2)
    {
        this.unknown2 = unknown2;
    }
    
    public int getArrivalDirection()
    {
        return arrivalDirection;
    }
    
    public void setArrivalDirection(int arrivalDirection)
    {
        this.arrivalDirection = arrivalDirection;
    }
    
    public short getHintNode1()
    {
        return hintNode1;
    }
    
    public void setHintNode1(short hintNode1)
    {
        this.hintNode1 = hintNode1;
    }
    
    public short getHintNode2()
    {
        return hintNode2;
    }
    
    public void setHintNode2(short hintNode2)
    {
        this.hintNode2 = hintNode2;
    }
    
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        NGridFileCell that = (NGridFileCell) o;
        return x == that.x &&
               z == that.z &&
               index == that.index &&
               Float.compare(that.centerHeight, centerHeight) == 0 &&
               sessionId == that.sessionId &&
               Float.compare(that.arrivalCost, arrivalCost) == 0 &&
               open == that.open &&
               Float.compare(that.heuristic, heuristic) == 0 &&
               actorList == that.actorList &&
               unknown1 == that.unknown1 &&
               goodCellSessionId == that.goodCellSessionId &&
               Float.compare(that.hintWeight, hintWeight) == 0 &&
               unknown2 == that.unknown2 &&
               arrivalDirection == that.arrivalDirection &&
               hintNode1 == that.hintNode1 &&
               hintNode2 == that.hintNode2 &&
               additionalCost == that.additionalCost &&
               additionalCostCount == that.additionalCostCount &&
               goodCellHint == that.goodCellHint &&
               Objects.equals(visionPath, that.visionPath) &&
               Objects.equals(riverRegion, that.riverRegion) &&
               Objects.equals(jungleQuadrant, that.jungleQuadrant) &&
               Objects.equals(mainRegion, that.mainRegion) &&
               Objects.equals(nearestLane, that.nearestLane) &&
               Objects.equals(poi, that.poi) &&
               Objects.equals(ring, that.ring);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(x, z, index, visionPath, riverRegion, jungleQuadrant, mainRegion, nearestLane, poi, ring, centerHeight, sessionId, arrivalCost, open, heuristic, actorList, unknown1, goodCellSessionId, hintWeight, unknown2, arrivalDirection, hintNode1, hintNode2, additionalCost, additionalCostCount, goodCellHint);
    }
    
    @Override
    public String toString()
    {
        return "NGridFileCell{" +
               "x=" + x +
               ", z=" + z +
               ", index=" + index +
               ", visionPath=" + visionPath +
               ", riverRegion=" + riverRegion +
               ", jungleQuadrant=" + jungleQuadrant +
               ", mainRegion=" + mainRegion +
               ", nearestLane=" + nearestLane +
               ", poi=" + poi +
               ", ring=" + ring +
               ", centerHeight=" + centerHeight +
               ", sessionId=" + sessionId +
               ", arrivalCost=" + arrivalCost +
               ", open=" + open +
               ", heuristic=" + heuristic +
               ", actorList=" + actorList +
               ", unknown1=" + unknown1 +
               ", goodCellSessionId=" + goodCellSessionId +
               ", hintWeight=" + hintWeight +
               ", unknown2=" + unknown2 +
               ", arrivalDirection=" + arrivalDirection +
               ", hintNode1=" + hintNode1 +
               ", hintNode2=" + hintNode2 +
               ", additionalCost=" + additionalCost +
               ", additionalCostCount=" + additionalCostCount +
               ", goodCellHint=" + goodCellHint +
               '}';
    }
}

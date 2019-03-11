package no.stelar7.cdragon.types.ngrid.data;

import no.stelar7.cdragon.util.types.math.Vector3f;

import java.util.*;

public class NGridFile
{
    private int major;
    private int minor;
    
    private Vector3f min;
    private Vector3f max;
    
    private float size;
    private int   countX;
    private int   countZ;
    
    private List<NGridFileCell> cells = new ArrayList<>();
    
    private int heightSamplesX;
    private int heightSamplesZ;
    
    private float heightSampleOffsetX;
    private float heightSampleOffsetZ;
    
    private List<Float> heightSamples = new ArrayList<>();
    
    private float minHeightSample;
    private float maxHeightSample;
    
    public int getMajor()
    {
        return major;
    }
    
    public void setMajor(int major)
    {
        this.major = major;
    }
    
    public int getMinor()
    {
        return minor;
    }
    
    public void setMinor(int minor)
    {
        this.minor = minor;
    }
    
    public Vector3f getMin()
    {
        return min;
    }
    
    public void setMin(Vector3f min)
    {
        this.min = min;
    }
    
    public Vector3f getMax()
    {
        return max;
    }
    
    public void setMax(Vector3f max)
    {
        this.max = max;
    }
    
    public float getSize()
    {
        return size;
    }
    
    public void setSize(float size)
    {
        this.size = size;
    }
    
    public int getCountX()
    {
        return countX;
    }
    
    public void setCountX(int countX)
    {
        this.countX = countX;
    }
    
    public int getCountZ()
    {
        return countZ;
    }
    
    public void setCountZ(int countZ)
    {
        this.countZ = countZ;
    }
    
    public List<NGridFileCell> getCells()
    {
        return cells;
    }
    
    public void setCells(List<NGridFileCell> cells)
    {
        this.cells = cells;
    }
    
    public int getHeightSamplesX()
    {
        return heightSamplesX;
    }
    
    public void setHeightSamplesX(int heightSamplesX)
    {
        this.heightSamplesX = heightSamplesX;
    }
    
    public int getHeightSamplesZ()
    {
        return heightSamplesZ;
    }
    
    public void setHeightSamplesZ(int heightSamplesZ)
    {
        this.heightSamplesZ = heightSamplesZ;
    }
    
    public float getHeightSampleOffsetX()
    {
        return heightSampleOffsetX;
    }
    
    public void setHeightSampleOffsetX(float heightSampleOffsetX)
    {
        this.heightSampleOffsetX = heightSampleOffsetX;
    }
    
    public float getHeightSampleOffsetZ()
    {
        return heightSampleOffsetZ;
    }
    
    public void setHeightSampleOffsetZ(float heightSampleOffsetZ)
    {
        this.heightSampleOffsetZ = heightSampleOffsetZ;
    }
    
    public List<Float> getHeightSamples()
    {
        return heightSamples;
    }
    
    public void setHeightSamples(List<Float> heightSamples)
    {
        this.heightSamples = heightSamples;
    }
    
    public float getMinHeightSample()
    {
        return minHeightSample;
    }
    
    public void setMinHeightSample(float minHeightSample)
    {
        this.minHeightSample = minHeightSample;
    }
    
    public float getMaxHeightSample()
    {
        return maxHeightSample;
    }
    
    public void setMaxHeightSample(float maxHeightSample)
    {
        this.maxHeightSample = maxHeightSample;
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
        NGridFile nGridFile = (NGridFile) o;
        return major == nGridFile.major &&
               minor == nGridFile.minor &&
               Float.compare(nGridFile.size, size) == 0 &&
               countX == nGridFile.countX &&
               countZ == nGridFile.countZ &&
               heightSamplesX == nGridFile.heightSamplesX &&
               heightSamplesZ == nGridFile.heightSamplesZ &&
               heightSampleOffsetX == nGridFile.heightSampleOffsetX &&
               heightSampleOffsetZ == nGridFile.heightSampleOffsetZ &&
               Float.compare(nGridFile.minHeightSample, minHeightSample) == 0 &&
               Float.compare(nGridFile.maxHeightSample, maxHeightSample) == 0 &&
               Objects.equals(min, nGridFile.min) &&
               Objects.equals(max, nGridFile.max) &&
               Objects.equals(cells, nGridFile.cells) &&
               Objects.equals(heightSamples, nGridFile.heightSamples);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(major, minor, min, max, size, countX, countZ, cells, heightSamplesX, heightSamplesZ, heightSampleOffsetX, heightSampleOffsetZ, heightSamples, minHeightSample, maxHeightSample);
    }
    
    @Override
    public String toString()
    {
        return "NGridFile{" +
               "major=" + major +
               ", minor=" + minor +
               ", min=" + min +
               ", max=" + max +
               ", size=" + size +
               ", countX=" + countX +
               ", countZ=" + countZ +
               ", cells=" + cells.size() +
               ", heightSamplesX=" + heightSamplesX +
               ", heightSamplesZ=" + heightSamplesZ +
               ", heightSampleOffsetX=" + heightSampleOffsetX +
               ", heightSampleOffsetZ=" + heightSampleOffsetZ +
               ", heightSamples=" + heightSamples.size() +
               ", minHeightSample=" + minHeightSample +
               ", maxHeightSample=" + maxHeightSample +
               '}';
    }
}

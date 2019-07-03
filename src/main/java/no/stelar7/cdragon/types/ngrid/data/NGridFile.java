package no.stelar7.cdragon.types.ngrid.data;

import no.stelar7.cdragon.types.ngrid.data.flags.*;
import no.stelar7.cdragon.util.types.Color;
import no.stelar7.cdragon.util.types.math.Vector3f;
import no.stelar7.cdragon.util.writers.ByteWriter;

import java.nio.file.Path;
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
    
    private Color walkableColor                      = new Color(255, 255, 255);
    private Color brushColor                         = new Color(0, 122, 14);
    private Color wallColor                          = new Color(64, 64, 64);
    private Color brushWallColor                     = new Color(0, 216, 111);
    private Color transparentWallColor               = new Color(0, 210, 214);
    private Color alwaysVisibleColor                 = new Color(192, 192, 0);
    private Color blueTeamOnlyColor                  = new Color(87, 79, 255);
    private Color redTeamOnlyColor                   = new Color(255, 124, 124);
    private Color neutralZoneVisibilityColor         = new Color(255, 165, 0);
    private Color blueTeamNeutralZoneVisibilityColor = new Color(12, 0, 255);
    private Color redTeamNeutralZoneVisibilityColor  = new Color(255, 0, 0);
    private Color heightSampleBaseColor              = new Color(255, 0, 0);
    
    
    private Color[] flagColors = new Color[]{
            new Color(64, 0, 0),
            new Color(140, 0, 0),
            new Color(240, 0, 0),
            new Color(0, 100, 0),
            new Color(0, 240, 0),
            new Color(0, 0, 100),
            new Color(0, 0, 240),
            new Color(100, 0, 100),
            new Color(240, 0, 240),
            new Color(140, 140, 0),
            new Color(240, 240, 0),
            new Color(0, 140, 140),
            new Color(0, 240, 240),
            new Color(64, 64, 64),
            new Color(160, 160, 160),
            new Color(240, 240, 240)
    };
    
    public void toBMP(Path outputFolder)
    {
        Path visionPathingOutput   = outputFolder.resolve("visionPathing.bmp");
        Path riverRegionsOutput    = outputFolder.resolve("riverRegions.bmp");
        Path jungleQuadrantsOutput = outputFolder.resolve("jungleQuadrants.bmp");
        Path mainRegionsOutput     = outputFolder.resolve("mainRegions.bmp");
        Path nearestLaneOutput     = outputFolder.resolve("nearestLane.bmp");
        Path POIOutput             = outputFolder.resolve("POI.bmp");
        Path ringsOutput           = outputFolder.resolve("rings.bmp");
        Path heightSampleOutput    = outputFolder.resolve("heightSample.bmp");
        
        ByteWriter vision  = generateBMPHeader();
        ByteWriter river   = generateBMPHeader();
        ByteWriter jungle  = generateBMPHeader();
        ByteWriter main    = generateBMPHeader();
        ByteWriter nearest = generateBMPHeader();
        ByteWriter poi     = generateBMPHeader();
        ByteWriter rings   = generateBMPHeader();
        ByteWriter height  = generateBMPHeader(this.heightSamplesX, this.heightSamplesZ);
        
        int padding = getBMPPadding();
        
        for (int i = 0; i < this.cells.size(); i++)
        {
            NGridFileCell cell = this.cells.get(i);
            
            List<VisionPathFlag> flags = cell.getVisionPath();
            flags.remove(VisionPathFlag.UNOBSERVED128);
            
            vision.writeColor(getColorVision(flags));
            jungle.writeColor(getColorJungle(cell.getJungleQuadrant()));
            main.writeColor(getColorMain(cell.getMainRegion()));
            nearest.writeColor(getColorNearest(cell.getNearestLane()));
            poi.writeColor(getColorPOI(cell.getPoi()));
            rings.writeColor(getColorRings(cell.getRing()));
            river.writeColor(getColorRiver(cell.getRiverRegion()));
            
            if ((i % this.countX) == (this.countX - 1))
            {
                for (int j = 0; j < padding; j++)
                {
                    vision.writeByte((byte) 0);
                    jungle.writeByte((byte) 0);
                    main.writeByte((byte) 0);
                    nearest.writeByte((byte) 0);
                    poi.writeByte((byte) 0);
                    rings.writeByte((byte) 0);
                    river.writeByte((byte) 0);
                }
            }
        }
        
        float heightDiff    = this.maxHeightSample - this.minHeightSample;
        int   heightPadding = getBMPPadding(this.heightSamplesX);
        for (int i = 0; i < this.heightSamples.size(); i++)
        {
            float sample      = this.heightSamples.get(i);
            float lerp        = (sample - this.minHeightSample) / heightDiff;
            Color sampleColor = heightSampleBaseColor.multiply(lerp);
            height.writeColor(sampleColor);
            
            if ((i % this.heightSamplesX) == (this.heightSamplesX - 1))
            {
                for (int j = 0; j < heightPadding; j++)
                {
                    height.writeByte((byte) 0);
                }
            }
        }
        
        vision.save(visionPathingOutput);
        river.save(riverRegionsOutput);
        jungle.save(jungleQuadrantsOutput);
        main.save(mainRegionsOutput);
        nearest.save(nearestLaneOutput);
        poi.save(POIOutput);
        rings.save(ringsOutput);
        height.save(heightSampleOutput);
        
    }
    
    
    private Color getCellColor(int flags)
    {
        int index = flags;
        
        if (index >= flagColors.length)
        {
            index = flagColors.length - 1;
        }
        
        return flagColors[index];
    }
    
    private Color getColorRings(List<RingFlag> flags)
    {
        return getCellColor(RingFlag.valueFrom(flags));
    }
    
    private Color getColorPOI(List<POIFlag> flags)
    {
        return getCellColor(POIFlag.valueFrom(flags));
    }
    
    private Color getColorNearest(List<NearestLaneFlag> flags)
    {
        return getCellColor(NearestLaneFlag.valueFrom(flags));
    }
    
    private Color getColorMain(List<MainRegionFlag> flags)
    {
        return getCellColor(MainRegionFlag.valueFrom(flags));
    }
    
    private Color getColorJungle(List<JungleQuadrantFlag> flags)
    {
        return getCellColor(JungleQuadrantFlag.valueFrom(flags));
    }
    
    
    private Color getColorRiver(List<RiverRegionFlag> flags)
    {
        int index = 0;
        
        if (flags.contains(RiverRegionFlag.JUNGLE))
        {
            index |= 1;
        }
        
        if (flags.contains(RiverRegionFlag.BARON))
        {
            index |= 2;
        }
        
        if (flags.contains(RiverRegionFlag.RIVER))
        {
            index |= 4;
        }
        
        if (flags.contains(RiverRegionFlag.RIVER_ENTRANCE))
        {
            index |= 8;
        }
        
        if (flags.contains(RiverRegionFlag.UNKNOWN32))
        {
            index |= 16;
        }
        
        
        return getCellColor(index);
    }
    
    private Color getColorVision(List<VisionPathFlag> flags)
    {
        Color color = this.walkableColor;
        if (flags.contains(VisionPathFlag.BRUSH))
        {
            color = this.brushColor;
        }
        
        if (flags.contains(VisionPathFlag.WALL))
        {
            if (color.equals(this.brushColor))
            {
                color = this.brushWallColor;
            } else
            {
                color = this.wallColor;
            }
        }
        
        if (flags.contains(VisionPathFlag.TRANSPARENT_WALL))
        {
            color = this.transparentWallColor;
        }
        
        if (flags.contains(VisionPathFlag.ALWAYS_VISIBLE))
        {
            color = this.alwaysVisibleColor;
        }
        
        if (flags.contains(VisionPathFlag.BLUE_TEAM_ONLY))
        {
            color = this.blueTeamOnlyColor;
        }
        
        if (flags.contains(VisionPathFlag.RED_TEAM_ONLY))
        {
            color = this.redTeamOnlyColor;
        }
        
        if (flags.contains(VisionPathFlag.NEUTRAL_ZONE_VISIBILITY))
        {
            if (color == this.blueTeamOnlyColor)
            {
                color = this.blueTeamNeutralZoneVisibilityColor;
            } else if (color == this.redTeamOnlyColor)
            {
                color = this.redTeamNeutralZoneVisibilityColor;
            } else
            {
                color = this.neutralZoneVisibilityColor;
            }
        }
        
        return color;
        
    }
    
    private int getBMPPadding(int xOff)
    {
        int perPixel = 3; // rgb
        int perRow   = xOff * perPixel;
        int offset   = perRow % 4;
        return offset != 0 ? 4 - offset : offset;
    }
    
    private int getBMPPadding()
    {
        return getBMPPadding(this.countX);
    }
    
    private ByteWriter generateBMPHeader()
    {
        return this.generateBMPHeader(this.countX, this.countZ);
    }
    
    private ByteWriter generateBMPHeader(int xSize, int ySize)
    {
        ByteWriter bw = new ByteWriter();
        bw.writeString("BM");
        
        int fileHeaderSize = 14;
        int dataHeaderSize = 40;
        int headerSize     = fileHeaderSize + dataHeaderSize;
        int perPixel       = 3; // rgb
        int perRow         = xSize * perPixel;
        int offset         = perRow % 4;
        int padding        = getBMPPadding();
        perRow += padding;
        
        int pixelSize = perRow * ySize;
        int totalSize = headerSize + pixelSize;
        bw.writeInt(totalSize);
        bw.writeInt(0);
        bw.writeInt(headerSize);
        
        bw.writeInt(dataHeaderSize);
        bw.writeInt(xSize);
        bw.writeInt(ySize);
        bw.writeShort((short) 1);
        bw.writeShort((short) (perPixel * 8));
        bw.writeInt(0);
        bw.writeInt(pixelSize);
        bw.writeInt(0);
        bw.writeInt(0);
        bw.writeInt(0);
        bw.writeInt(0);
        
        return bw;
        
    }
}

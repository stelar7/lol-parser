package no.stelar7.cdragon.types.crid.data;

public class CRIDStreamInfo
{
    String filename;
    int    filesize;
    int    datasize;
    String streamType;
    int    channels;
    int    minChunk;
    int    minBuffer;
    int    avgBps;
    
    public String getFilename()
    {
        return filename;
    }
    
    public void setFilename(String filename)
    {
        this.filename = filename;
    }
    
    public int getFilesize()
    {
        return filesize;
    }
    
    public void setFilesize(int filesize)
    {
        this.filesize = filesize;
    }
    
    public int getDatasize()
    {
        return datasize;
    }
    
    public void setDatasize(int datasize)
    {
        this.datasize = datasize;
    }
    
    public String getStreamType()
    {
        return streamType;
    }
    
    public void setStreamType(String streamType)
    {
        this.streamType = streamType;
    }
    
    public int getChannels()
    {
        return channels;
    }
    
    public void setChannel(int channel)
    {
        this.channels = channel;
    }
    
    public int getMinChunk()
    {
        return minChunk;
    }
    
    public void setMinChunk(int minChunk)
    {
        this.minChunk = minChunk;
    }
    
    public int getMinBuffer()
    {
        return minBuffer;
    }
    
    public void setMinBuffer(int minBuffer)
    {
        this.minBuffer = minBuffer;
    }
    
    public int getAvgBps()
    {
        return avgBps;
    }
    
    public void setAvgBps(int avgBps)
    {
        this.avgBps = avgBps;
    }
    
    @Override
    public String toString()
    {
        return "CRIDStreamInfo{" +
               "filename='" + filename + '\'' +
               ", filesize=" + filesize +
               ", datasize=" + datasize +
               ", streamType='" + streamType + '\'' +
               ", channels=" + channels +
               ", minChunk=" + minChunk +
               ", minBuffer=" + minBuffer +
               ", avgBps=" + avgBps +
               '}';
    }
}

package no.stelar7.cdragon.types.crid.data;

import no.stelar7.cdragon.types.crid.data.utf.CRIDUTFTable;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.Pair;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class CRIDFile
{
    CRIDHeader                 header;
    CRIDUTFTable               metadata;
    List<CRIDStreamInfo>       streams;
    List<Pair<String, byte[]>> streamData;
    
    public List<Pair<String, byte[]>> getStreamData()
    {
        return streamData;
    }
    
    public void setStreamData(List<Pair<String, byte[]>> streamData)
    {
        this.streamData = streamData;
    }
    
    public CRIDHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(CRIDHeader header)
    {
        this.header = header;
    }
    
    public CRIDUTFTable getMetadata()
    {
        return metadata;
    }
    
    public void setMetadata(CRIDUTFTable metadata)
    {
        this.metadata = metadata;
    }
    
    public List<CRIDStreamInfo> getStreams()
    {
        return streams;
    }
    
    public void setStreams(List<CRIDStreamInfo> streams)
    {
        this.streams = streams;
    }
    
    @Override
    public String toString()
    {
        return "CRIDFile{" +
               "header=" + header +
               ", metadata=" + metadata +
               ", streams=" + streams +
               ", streamData=" + streamData +
               '}';
    }
    
    public void extractStreams(Path extractPath)
    {
        try
        {
            boolean hasAlpha = false;
            for (Pair<String, byte[]> pair : getStreamData())
            {
                String filename = pair.getA();
                byte[] data     = pair.getB();
                
                if (filename.contains("@ALP"))
                {
                    hasAlpha = true;
                }
                
                Files.write(extractPath.resolve(filename), data);
            }
            
            if (hasAlpha)
            {
                String alphaFilename  = getStreamData().stream().filter(s -> s.getA().contains("@ALP")).findFirst().get().getA();
                String colorFilename  = getStreamData().stream().filter(s -> s.getA().contains("@SFV")).findFirst().get().getA();
                String outputFilename = getStreamData().stream().filter(s -> s.getA().contains("@SFV")).findFirst().get().getA().replace("@SFV", "");
                
                Path alphaFile  = extractPath.resolve(alphaFilename);
                Path colorFile  = extractPath.resolve(colorFilename);
                Path outputFile = extractPath.resolve(outputFilename);
                
                alphaFile.toFile().deleteOnExit();
                colorFile.toFile().deleteOnExit();
                
                alphaFilename = alphaFile.toAbsolutePath().toString();
                colorFilename = colorFile.toAbsolutePath().toString();
                outputFilename = UtilHandler.replaceEnding(outputFile.toAbsolutePath().toString(), "m2v", "mp4");
                
                String         FFMPEG_COMMAND = "ffmpeg -y -i " + colorFilename + " -i " + alphaFilename + " -filter_complex \"[0:v][1:v]premultiply\" -c:v libx264 -an " + outputFilename;
                ProcessBuilder pb             = new ProcessBuilder();
                pb.command(FFMPEG_COMMAND.split(" "));
                pb.inheritIO();
                Process pr = pb.start();
                pr.waitFor();
            }
        } catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}

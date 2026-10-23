package no.stelar7.cdragon.types.crid.data;

import no.stelar7.cdragon.types.crid.data.utf.CRIDUTFTable;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class CRIDFile
{
    CRIDHeader                 header;
    CRIDUTFTable               metadata;
    List<CRIDStreamInfo>       streams;
    List<Pair<String, byte[]>> streamData;
    List<CRIDSubtitle>         subtitles       = new ArrayList<>();
    Map<Integer, CRIDUTFTable> subtitleHeaders = new TreeMap<>();
    List<CRIDCuePoint>         cuePoints       = new ArrayList<>();
    long                       cueTimeUnit;

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

    /**
     * All subtitle records of all @SBT channels, in file order
     */
    public List<CRIDSubtitle> getSubtitles()
    {
        return subtitles;
    }

    public void setSubtitles(List<CRIDSubtitle> subtitles)
    {
        this.subtitles = subtitles;
    }

    /**
     * The SUBTITLE_HDRINFO table of each @SBT channel
     */
    public Map<Integer, CRIDUTFTable> getSubtitleHeaders()
    {
        return subtitleHeaders;
    }

    public void setSubtitleHeaders(Map<Integer, CRIDUTFTable> subtitleHeaders)
    {
        this.subtitleHeaders = subtitleHeaders;
    }

    public List<CRIDCuePoint> getCuePoints()
    {
        return cuePoints;
    }

    public void setCuePoints(List<CRIDCuePoint> cuePoints)
    {
        this.cuePoints = cuePoints;
    }

    /**
     * The "time_unit" of the CUEPOINT_HDRINFO table (ticks per second), 0 if the file has none
     */
    public long getCueTimeUnit()
    {
        return cueTimeUnit;
    }

    public void setCueTimeUnit(long cueTimeUnit)
    {
        this.cueTimeUnit = cueTimeUnit;
    }

    /**
     * Converts the subtitles of one language to SubRip (.srt) text
     */
    public String toSRT(long languageId)
    {
        StringBuilder sb    = new StringBuilder();
        int           index = 1;
        for (CRIDSubtitle subtitle : subtitles)
        {
            if (subtitle.getLanguageId() != languageId)
            {
                continue;
            }

            sb.append(index++).append('\n');
            sb.append(srtTime(subtitle.getStartMillis())).append(" --> ").append(srtTime(subtitle.getEndMillis())).append('\n');
            sb.append(subtitle.getText().replace("\r\n", "\n")).append("\n\n");
        }
        return sb.toString();
    }

    private static String srtTime(long millis)
    {
        return String.format("%02d:%02d:%02d,%03d", millis / 3_600_000, (millis / 60_000) % 60, (millis / 1000) % 60, millis % 1000);
    }

    @Override
    public String toString()
    {
        return "CRIDFile{" +
               "header=" + header +
               ", metadata=" + metadata +
               ", streams=" + streams +
               ", streamData=" + streamData +
               ", subtitles=" + subtitles +
               ", cuePoints=" + cuePoints +
               '}';
    }

    /**
     * Writes every demultiplexed stream (and one .srt file per subtitle language) to the folder.
     * Does not need ffmpeg.
     */
    public void extractStreams(Path extractPath)
    {
        extractStreams(extractPath, false);
    }

    /**
     * Writes every demultiplexed stream (and one .srt file per subtitle language) to the folder.
     * <p>
     * If mergeAlpha is set and the file has an alpha (@ALP) video stream next to its color (@SFV) stream,
     * ffmpeg is used to combine them into one premultiplied .mp4 (named after the color stream, without "@SFV").
     * Files without an alpha stream, or without a color stream, are only demultiplexed.
     *
     * @throws IllegalStateException if the streams need merging but ffmpeg is not on the PATH, or ffmpeg fails
     */
    public void extractStreams(Path extractPath, boolean mergeAlpha)
    {
        try
        {
            Files.createDirectories(extractPath);
            for (Pair<String, byte[]> pair : getStreamData())
            {
                Files.write(extractPath.resolve(pair.getA()), pair.getB());
            }

            String baseName = getStreamData().stream()
                                             .map(Pair::getA)
                                             .filter(s -> s.contains("@"))
                                             .map(s -> s.substring(0, s.indexOf('@')))
                                             .findFirst()
                                             .orElse("subtitles");
            for (long language : subtitles.stream().map(CRIDSubtitle::getLanguageId).distinct().toList())
            {
                Files.writeString(extractPath.resolve(baseName + "@SBT_" + language + ".srt"), toSRT(language), StandardCharsets.UTF_8);
            }

            if (mergeAlpha)
            {
                mergeAlphaStreams(extractPath);
            }
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void mergeAlphaStreams(Path extractPath) throws IOException
    {
        Optional<String> alphaName = getStreamData().stream().map(Pair::getA).filter(s -> s.contains("@ALP")).findFirst();
        Optional<String> colorName = getStreamData().stream().map(Pair::getA).filter(s -> s.contains("@SFV")).findFirst();
        if (alphaName.isEmpty() || colorName.isEmpty())
        {
            return;
        }

        Path ffmpeg = findFFmpeg().orElseThrow(() -> new IllegalStateException("Merging the alpha stream needs ffmpeg, but it was not found on the PATH. The demultiplexed streams were written to " + extractPath));

        Path alphaFile  = extractPath.resolve(alphaName.get()).toAbsolutePath();
        Path colorFile  = extractPath.resolve(colorName.get()).toAbsolutePath();
        Path outputFile = Paths.get(UtilHandler.replaceEnding(extractPath.resolve(colorName.get().replace("@SFV", "")).toAbsolutePath().toString(), "m2v", "mp4"));

        List<String> command = new ArrayList<>(List.of(ffmpeg.toString(), "-y", "-i", colorFile.toString(), "-i", alphaFile.toString(), "-filter_complex", "[0:v][1:v]premultiply"));
        int averageBps = getStreams() == null ? 0 : getStreams().stream()
                                                                 .filter(s -> "@SFV".equals(s.getStreamType()) || "@ALP".equals(s.getStreamType()))
                                                                 .mapToInt(CRIDStreamInfo::getAvgBps)
                                                                 .max()
                                                                 .orElse(0);
        if (averageBps > 0)
        {
            command.addAll(List.of("-b:v", String.valueOf(averageBps)));
        }
        command.addAll(List.of("-c:v", "libx264", "-an", outputFile.toString()));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        try
        {
            int exit = pb.start().waitFor();
            if (exit != 0)
            {
                throw new IllegalStateException("ffmpeg exited with code " + exit + " while merging " + colorFile + " and " + alphaFile);
            }
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for ffmpeg", e);
        }
    }

    /**
     * Looks for an ffmpeg executable on the PATH
     */
    public static Optional<Path> findFFmpeg()
    {
        String path = System.getenv("PATH");
        if (path == null)
        {
            return Optional.empty();
        }

        boolean      windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        List<String> names   = windows ? List.of("ffmpeg.exe", "ffmpeg") : List.of("ffmpeg");
        for (String dir : path.split(File.pathSeparator))
        {
            if (dir.isBlank())
            {
                continue;
            }

            for (String name : names)
            {
                try
                {
                    Path candidate = Paths.get(dir.replace("\"", "")).resolve(name);
                    if (Files.isRegularFile(candidate) && Files.isExecutable(candidate))
                    {
                        return Optional.of(candidate);
                    }
                } catch (InvalidPathException e)
                {
                    // ignore malformed PATH entries
                }
            }
        }
        return Optional.empty();
    }
}

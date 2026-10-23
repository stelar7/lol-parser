package no.stelar7.cdragon.types.crid.data;

import java.nio.charset.StandardCharsets;

/**
 * One subtitle record from a USM "@SBT" stream.
 * <p>
 * Each stream chunk of an @SBT stream holds one (or more) little-endian records:
 * <pre>
 * u32 language id
 * u32 time unit      ticks per second (usually 1000)
 * u32 start time     in ticks
 * u32 duration       in ticks
 * u32 text size      including the 0x00 terminator(s)
 * u8[text size] text
 * </pre>
 * See SbtRecordHeader in https://github.com/Youjose/PyCriCodecs (CriCodecs/src/usm/usm_subtitle.cpp)
 */
public class CRIDSubtitle
{
    private int    channel;
    private long   languageId;
    private long   timeUnit;
    private long   startTime;
    private long   duration;
    private byte[] rawText;

    public int getChannel()
    {
        return channel;
    }

    public void setChannel(int channel)
    {
        this.channel = channel;
    }

    public long getLanguageId()
    {
        return languageId;
    }

    public void setLanguageId(long languageId)
    {
        this.languageId = languageId;
    }

    public long getTimeUnit()
    {
        return timeUnit;
    }

    public void setTimeUnit(long timeUnit)
    {
        this.timeUnit = timeUnit;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    public long getDuration()
    {
        return duration;
    }

    public void setDuration(long duration)
    {
        this.duration = duration;
    }

    /**
     * The text bytes without the trailing 0x00 terminator(s)
     */
    public byte[] getRawText()
    {
        return rawText;
    }

    public void setRawText(byte[] rawText)
    {
        this.rawText = rawText;
    }

    /**
     * The text decoded as UTF-8
     */
    public String getText()
    {
        return new String(rawText, StandardCharsets.UTF_8);
    }

    public long getStartMillis()
    {
        return timeUnit == 0 ? 0 : startTime * 1000 / timeUnit;
    }

    public long getEndMillis()
    {
        return timeUnit == 0 ? 0 : (startTime + duration) * 1000 / timeUnit;
    }

    @Override
    public String toString()
    {
        return "CRIDSubtitle{" +
               "channel=" + channel +
               ", languageId=" + languageId +
               ", timeUnit=" + timeUnit +
               ", startTime=" + startTime +
               ", duration=" + duration +
               ", text='" + getText() + '\'' +
               '}';
    }
}

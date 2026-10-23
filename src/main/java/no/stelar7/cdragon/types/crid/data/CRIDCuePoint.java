package no.stelar7.cdragon.types.crid.data;

/**
 * One row of the "CUEPOINT_INFO" @UTF table carried by the metadata chunk of a USM "@CUE" stream.
 * The time is in ticks of {@link CRIDFile#getCueTimeUnit()} (the "time_unit" of the CUEPOINT_HDRINFO table, usually 1000).
 */
public class CRIDCuePoint
{
    private long   time;
    private long   timeUnit;
    private String name;
    private String parameter;

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    public long getTimeUnit()
    {
        return timeUnit;
    }

    public void setTimeUnit(long timeUnit)
    {
        this.timeUnit = timeUnit;
    }

    public long getTimeMillis()
    {
        return timeUnit == 0 ? time : time * 1000 / timeUnit;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getParameter()
    {
        return parameter;
    }

    public void setParameter(String parameter)
    {
        this.parameter = parameter;
    }

    @Override
    public String toString()
    {
        return "CRIDCuePoint{" +
               "time=" + time +
               ", timeUnit=" + timeUnit +
               ", name='" + name + '\'' +
               ", parameter='" + parameter + '\'' +
               '}';
    }
}

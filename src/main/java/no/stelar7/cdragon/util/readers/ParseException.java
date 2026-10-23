package no.stelar7.cdragon.util.readers;

/**
 * Thrown when a file can not be parsed, either because it is corrupt or because its version is not supported.
 */
public class ParseException extends RuntimeException
{
    private final long offset;

    public ParseException(String message, RandomAccessReader raf)
    {
        this(message, raf == null ? -1 : raf.pos());
    }

    public ParseException(String message, long offset)
    {
        super(offset < 0 ? message : message + " (at offset " + offset + ")");
        this.offset = offset;
    }

    public ParseException(String message, long offset, Throwable cause)
    {
        super(offset < 0 ? message : message + " (at offset " + offset + ")", cause);
        this.offset = offset;
    }

    /**
     * The reader position where parsing failed, or -1 if unknown
     */
    public long getOffset()
    {
        return offset;
    }
}

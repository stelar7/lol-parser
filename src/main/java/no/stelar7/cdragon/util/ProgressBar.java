package no.stelar7.cdragon.util;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe console progress reporter, showing a line like {@code [Extracting WADs] [#####---------------] 25% 123/492 (0:42 elapsed, ~2:06 left)}.
 * <p>
 * Bars that are started while another bar is running (eg. a bar per extracted wad inside "Extracting WADs") do not print lines of their own,
 * they are shown at the end of the outer bar's line instead: {@code [Extracting WADs] [#####-----] 25% ... | Extracting Aatrox 45%}.
 * Only the outer bar prints a line when it finishes.
 * <p>
 * There are two modes, chosen with the system property {@code cdragon.progress} ({@code inline} or {@code lines}):
 * <ul>
 * <li>inline (the default in a terminal): the line is redrawn in place with '\r' at the bottom of the console.
 * Anything else printed to System.out/System.err is written above it.</li>
 * <li>lines (the default otherwise, eg. in the IntelliJ console, which only shows text once a line is complete):
 * the line is printed at most once every {@link #LINE_INTERVAL_MS}.</li>
 * </ul>
 * The width of the inline status line can be set with the system property {@code cdragon.progress.width} (or the COLUMNS environment variable).
 */
public class ProgressBar
{
    /**
     * Total to use when the size is not known up front, only the current value and elapsed time is shown
     */
    public static final long UNKNOWN = -1;

    private static final long LINE_INTERVAL_MS   = 2000;
    private static final long INLINE_INTERVAL_MS = 200;
    private static final int  BAR_WIDTH          = 20;

    private static final boolean INLINE = useInline();

    private final String     label;
    private final long       total;
    private final boolean    bytes;
    private final boolean    topLevel;
    private final long       errorsAtStart = ErrorLog.getCount();
    private final long       start         = System.currentTimeMillis();
    private final AtomicLong current       = new AtomicLong();

    private volatile boolean finished;
    private volatile String  message;

    /**
     * @param label text shown in front of the bar
     * @param total the value that means 100%, or {@link #UNKNOWN}
     * @param bytes true if the values are byte counts, and should be shown as KB/MB/GB
     */
    public ProgressBar(String label, long total, boolean bytes)
    {
        this.label = label;
        this.total = total < 0 ? UNKNOWN : total;
        this.bytes = bytes;
        this.topLevel = StatusLine.add(this);
    }

    public ProgressBar(String label, long total)
    {
        this(label, total, false);
    }

    private boolean isUnknown()
    {
        return total == UNKNOWN;
    }

    public void step()
    {
        step(1);
    }

    public void step(long amount)
    {
        long value = current.addAndGet(amount);
        if (!isUnknown() && value >= total)
        {
            finish();
            return;
        }

        StatusLine.update(false);
    }

    /**
     * Moves on to the next named step, and always shows it.
     * Use with a total equal to the amount of steps, eg. {@code [GAME hashes] [####----] 20% 3/15: guessScripts}
     */
    public void stage(String name)
    {
        long value = current.incrementAndGet();
        message = value + "/" + format(total) + ": " + name;
        StatusLine.update(true);
    }

    /**
     * Shows the final line, unless it has already been shown
     */
    public void finish()
    {
        synchronized (this)
        {
            if (finished)
            {
                return;
            }
            finished = true;
            message = null;
        }

        StatusLine.remove(this, topLevel ? describe(current.get(), System.currentTimeMillis()) : null);
    }

    private String describe(long value, long now)
    {
        long          elapsed = now - start;
        StringBuilder sb      = new StringBuilder();
        sb.append('[').append(label).append("] ");

        if (isUnknown())
        {
            sb.append(format(value));
            sb.append(" (").append(formatTime(elapsed)).append(" elapsed").append(finished ? ", done" : "");
        } else
        {
            // while a stage is running, the bar shows the stages that are done, not the one that is starting
            String stage  = message;
            double ratio  = ratio(stage != null ? value - 1 : value);
            int    filled = (int) (ratio * BAR_WIDTH);

            sb.append('[').append("#".repeat(filled)).append("-".repeat(BAR_WIDTH - filled)).append("] ");
            sb.append(String.format("%3d%%", (int) (ratio * 100))).append(' ');
            sb.append(stage != null ? stage : format(value) + "/" + format(total));
            sb.append(" (").append(formatTime(elapsed)).append(" elapsed");

            if (finished)
            {
                sb.append(", done");
            } else if (ratio > 0 && stage == null)
            {
                long remaining = (long) (elapsed / ratio) - elapsed;
                sb.append(", ~").append(formatTime(remaining)).append(" left");
            }
        }
        sb.append(')');

        long errors = ErrorLog.getCount() - errorsAtStart;
        if (finished && errors > 0)
        {
            sb.append(" - ").append(errors).append(errors == 1 ? " problem" : " problems").append(" logged to ").append(ErrorLog.getPath());
        }

        return sb.toString();
    }

    /**
     * The short form used for bars that share the line with an outer bar, eg. {@code Extracting Aatrox 45%}
     */
    private String describeShort()
    {
        long value = current.get();
        return label + " " + (isUnknown() ? format(value) : (int) (ratio(value) * 100) + "%");
    }

    private double ratio(long value)
    {
        return total == 0 ? 1 : Math.min(1, (double) value / total);
    }

    private String format(long value)
    {
        if (!bytes)
        {
            return String.valueOf(value);
        }

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double   size  = value;
        int      unit  = 0;
        while (size >= 1024 && unit < units.length - 1)
        {
            size /= 1024;
            unit++;
        }

        return unit == 0 ? value + "B" : String.format("%.1f%s", size, units[unit]);
    }

    private static String formatTime(long millis)
    {
        long seconds = millis / 1000;
        long hours   = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs    = seconds % 60;

        return hours > 0
               ? String.format("%d:%02d:%02d", hours, minutes, secs)
               : String.format("%d:%02d", minutes, secs);
    }

    private static boolean useInline()
    {
        String mode = System.getProperty("cdragon.progress");
        if (mode != null)
        {
            return mode.equalsIgnoreCase("inline");
        }

        Console console = System.console();
        return console != null && console.isTerminal();
    }

    /**
     * The bars that are running, and the line showing them.
     * In inline mode, System.out and System.err are replaced (when the first bar is created) with streams that
     * clear the status line before writing, and redraw it after every complete line.
     */
    private static final class StatusLine
    {
        private static final Object            LOCK   = new Object();
        private static final List<ProgressBar> ACTIVE = new ArrayList<>();

        private static long lastUpdate;

        // inline mode only
        private static PrintStream out;
        private static int         width;
        private static int         shownLength;
        private static boolean     atLineStart = true;

        /**
         * @return true if no other bar was active
         */
        static boolean add(ProgressBar bar)
        {
            synchronized (LOCK)
            {
                ACTIVE.add(bar);
                boolean topLevel = ACTIVE.size() == 1;
                if (INLINE)
                {
                    install();
                    draw();
                } else if (topLevel)
                {
                    System.out.println("[" + bar.label + "] Started" + (bar.isUnknown() ? "" : " (" + bar.format(bar.total) + ")"));
                    lastUpdate = System.currentTimeMillis();
                }
                return topLevel;
            }
        }

        static void remove(ProgressBar bar, String finalLine)
        {
            synchronized (LOCK)
            {
                ACTIVE.remove(bar);
                if (INLINE)
                {
                    clear();
                    if (finalLine != null)
                    {
                        out.println(finalLine);
                        atLineStart = true;
                    }
                    draw();
                } else if (finalLine != null)
                {
                    System.out.println(finalLine);
                }
            }
        }

        /**
         * Shows the current progress, at most once per interval unless forced
         */
        static void update(boolean force)
        {
            synchronized (LOCK)
            {
                long now = System.currentTimeMillis();
                if (!force && now - lastUpdate < (INLINE ? INLINE_INTERVAL_MS : LINE_INTERVAL_MS))
                {
                    return;
                }
                lastUpdate = now;

                if (INLINE)
                {
                    draw();
                } else if (!ACTIVE.isEmpty())
                {
                    System.out.println(describeActive(now));
                }
            }
        }

        /**
         * The outer bar, followed by the short form of the bars inside it
         */
        private static String describeActive(long now)
        {
            ProgressBar   outer = ACTIVE.get(0);
            StringBuilder line  = new StringBuilder(outer.describe(outer.current.get(), now));
            for (int i = 1; i < ACTIVE.size(); i++)
            {
                line.append(" | ").append(ACTIVE.get(i).describeShort());
            }
            return line.toString();
        }

        private static void install()
        {
            if (out != null)
            {
                return;
            }

            out = System.out;
            width = readWidth();
            Charset charset = out.charset();
            System.setOut(new PrintStream(new AroundStatusLine(out), true, charset));
            System.setErr(new PrintStream(new AroundStatusLine(System.err), true, charset));
        }

        private static int readWidth()
        {
            for (String value : new String[]{System.getProperty("cdragon.progress.width"), System.getenv("COLUMNS")})
            {
                try
                {
                    if (value != null && Integer.parseInt(value.trim()) > 20)
                    {
                        return Integer.parseInt(value.trim());
                    }
                } catch (NumberFormatException e)
                {
                    // try the next one
                }
            }
            return 120;
        }

        private static void draw()
        {
            // a partial line is being printed, the bar is drawn when it is done
            if (!atLineStart || ACTIVE.isEmpty())
            {
                return;
            }

            StringBuilder line = new StringBuilder(describeActive(System.currentTimeMillis()));

            // the line must not wrap, or '\r' only goes back to the start of the last part
            int max = width - 1;
            if (line.length() > max)
            {
                line.setLength(max - 3);
                line.append("...");
            }

            int length = line.length();
            if (length < shownLength)
            {
                line.append(" ".repeat(shownLength - length));
            }

            out.print("\r" + line);
            out.flush();
            shownLength = length;
        }

        private static void clear()
        {
            if (shownLength > 0)
            {
                out.print("\r" + " ".repeat(shownLength) + "\r");
                out.flush();
                shownLength = 0;
            }
        }

        /**
         * Writes to the original stream, keeping the status line below the output
         */
        private static final class AroundStatusLine extends OutputStream
        {
            private final PrintStream target;

            AroundStatusLine(PrintStream target)
            {
                this.target = target;
            }

            @Override
            public void write(int b)
            {
                write(new byte[]{(byte) b}, 0, 1);
            }

            @Override
            public void write(byte[] b, int off, int len)
            {
                if (len <= 0)
                {
                    return;
                }

                synchronized (LOCK)
                {
                    if (atLineStart)
                    {
                        clear();
                    }

                    target.write(b, off, len);
                    target.flush();

                    atLineStart = b[off + len - 1] == '\n';
                    draw();
                }
            }

            @Override
            public void flush()
            {
                target.flush();
            }
        }
    }
}

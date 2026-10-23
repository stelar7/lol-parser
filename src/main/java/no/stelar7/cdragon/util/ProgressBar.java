package no.stelar7.cdragon.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe console progress reporter.
 * <p>
 * Prints a line like {@code [Extracting WADs] [#####---------------] 25% 123/492 (0:42 elapsed, ~2:06 left)}
 * at most once every {@link #PRINT_INTERVAL_MS}, and always once when finished.
 * Lines are printed instead of redrawn with '\r', since other threads print to the same console.
 */
public class ProgressBar
{
    /**
     * Total to use when the size is not known up front, only the current value and elapsed time is shown
     */
    public static final long UNKNOWN = -1;

    private static final long PRINT_INTERVAL_MS = 2000;
    private static final int  BAR_WIDTH         = 20;

    private final String     label;
    private final long       total;
    private final boolean    bytes;
    private final long       start     = System.currentTimeMillis();
    private final AtomicLong current   = new AtomicLong();
    private final AtomicLong lastPrint = new AtomicLong();

    private volatile boolean finished;

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
        System.out.println("[" + label + "] Started" + (isUnknown() ? "" : " (" + format(this.total) + ")"));
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
        long    value = current.addAndGet(amount);
        long    now   = System.currentTimeMillis();
        long    last  = lastPrint.get();
        boolean done  = !isUnknown() && value >= total;

        if (done)
        {
            finish();
            return;
        }

        // only one thread gets to print per interval
        if (now - last < PRINT_INTERVAL_MS || !lastPrint.compareAndSet(last, now))
        {
            return;
        }

        print(value, now, null);
    }

    /**
     * Moves on to the next named step, and always prints it.
     * Use with a total equal to the amount of steps, eg. {@code [GAME hashes] [####----] 20% 3/15: guessScripts}
     */
    public void stage(String name)
    {
        long value = current.incrementAndGet();
        long now   = System.currentTimeMillis();
        lastPrint.set(now);
        print(value - 1, now, value + "/" + format(total) + ": " + name);
    }

    /**
     * Prints the final line, unless it has already been printed
     */
    public synchronized void finish()
    {
        if (finished)
        {
            return;
        }

        finished = true;
        print(current.get(), System.currentTimeMillis(), null);
    }

    private void print(long value, long now, String message)
    {
        long          elapsed = now - start;
        StringBuilder sb      = new StringBuilder();
        sb.append('[').append(label).append("] ");

        if (isUnknown())
        {
            sb.append(format(value));
            sb.append(" (").append(formatTime(elapsed)).append(" elapsed").append(finished ? ", done)" : ")");
            System.out.println(sb);
            return;
        }

        double ratio  = total == 0 ? 1 : Math.min(1, (double) value / total);
        int    filled = (int) (ratio * BAR_WIDTH);

        sb.append('[').append("#".repeat(filled)).append("-".repeat(BAR_WIDTH - filled)).append("] ");
        sb.append(String.format("%3d%%", (int) (ratio * 100))).append(' ');
        sb.append(message != null ? message : format(value) + "/" + format(total));
        sb.append(" (").append(formatTime(elapsed)).append(" elapsed");

        if (finished)
        {
            sb.append(", done");
        } else if (ratio > 0 && message == null)
        {
            long remaining = (long) (elapsed / ratio) - elapsed;
            sb.append(", ~").append(formatTime(remaining)).append(" left");
        }
        sb.append(')');

        System.out.println(sb);
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
}

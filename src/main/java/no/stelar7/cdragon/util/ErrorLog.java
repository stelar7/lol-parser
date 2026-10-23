package no.stelar7.cdragon.util;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe log for problems that should not interrupt (or scroll) the console, like files of an unknown type or textures that can not be converted.
 * <p>
 * Lines are appended to {@code <cdragon folder>/errors.log}, or the file set with the system property {@code cdragon.errorlog}.
 * Each line looks like {@code 2026-10-23 14:50:01 [unknown filetype] C:/cdragon/pbe/unknown/FINAL/0139B33BB98061A8.unknown: 4E 56 52 47 01 00 00 00 "NVRG...." (12 bytes)}.
 * If the file can not be written, the lines go to stderr instead.
 */
public final class ErrorLog
{
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final AtomicLong        COUNT       = new AtomicLong();

    private static Writer  writer;
    private static boolean opened;

    private ErrorLog()
    {
        // Hide public constructor
    }

    public static Path getPath()
    {
        String path = System.getProperty("cdragon.errorlog");
        return path != null ? Paths.get(path) : UtilHandler.CDRAGON_FOLDER.resolve("errors.log");
    }

    /**
     * The amount of lines logged since the program started
     */
    public static long getCount()
    {
        return COUNT.get();
    }

    public static void log(String category, String message)
    {
        log(category, message, null);
    }

    public static void log(String category, String message, Throwable error)
    {
        COUNT.incrementAndGet();

        StringBuilder line = new StringBuilder();
        line.append(LocalDateTime.now().format(TIME_FORMAT)).append(" [").append(category).append("] ").append(message);
        if (error != null)
        {
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));
            line.append(System.lineSeparator()).append(trace.toString().stripTrailing());
        }

        write(line.toString());
    }

    private static synchronized void write(String line)
    {
        Writer out = getWriter();
        if (out == null)
        {
            System.err.println(line);
            return;
        }

        try
        {
            out.write(line);
            out.write(System.lineSeparator());
            // flushed every line, so the log is complete even if the program is killed
            out.flush();
        } catch (IOException e)
        {
            System.err.println(line);
        }
    }

    private static Writer getWriter()
    {
        if (!opened)
        {
            opened = true;
            Path path = getPath();
            try
            {
                if (path.getParent() != null)
                {
                    Files.createDirectories(path.getParent());
                }
                writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                writer.write("---- " + LocalDateTime.now().format(TIME_FORMAT) + " ----" + System.lineSeparator());
            } catch (IOException e)
            {
                System.err.println("Unable to open the error log " + path + ", logging to stderr instead (" + e.getMessage() + ")");
                writer = null;
            }
        }
        return writer;
    }
}

package no.stelar7.cdragon.util.handlers;

import no.stelar7.cdragon.types.lockfile.LockfileParser;
import no.stelar7.cdragon.types.lockfile.data.Lockfile;
import no.stelar7.cdragon.util.types.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Finds the running league client and how to connect to it
 */
public final class LCUHandler
{
    private LCUHandler()
    {
        // Hide public constructor
    }
    
    /**
     * Finds the running LeagueClientUx process and returns its (password, port).
     * <p>
     * The arguments are read from the command line where the OS exposes it (Linux/macOS),
     * otherwise from the lockfile in the client install folder (Windows only exposes the executable path).
     */
    public static Pair<String, String> getLCUConnectionData()
    {
        String passwordRegex = "--remoting-auth-token=(?<password>[^ \\\"]+)";
        String portRegex     = "--app-port=(?<port>\\d+)";

        List<ProcessHandle> clients = ProcessHandle.allProcesses()
                                                   .filter(p -> p.info().command().map(c -> c.contains("LeagueClientUx")).orElse(false))
                                                   .toList();

        for (ProcessHandle client : clients)
        {
            String line = client.info().commandLine().orElse("");

            String password = UtilHandler.extractRegex(line, passwordRegex, "password");
            String port     = UtilHandler.extractRegex(line, portRegex, "port");
            if (password != null && port != null)
            {
                return new Pair<>(password, port);
            }

            Optional<Path> lockfile = client.info().command().flatMap(LCUHandler::findLockfile);
            if (lockfile.isPresent())
            {
                try
                {
                    Lockfile data = new LockfileParser().parse(Files.readString(lockfile.get()));
                    return new Pair<>(data.getPassword(), String.valueOf(data.getPort()));
                } catch (IOException | RuntimeException e)
                {
                    System.err.println("Unable to read " + lockfile.get() + ": " + e.getMessage());
                }
            }
        }

        System.err.println("The league client does not appear to be running!");
        return null;
    }

    /**
     * The lockfile is in the install folder, which is the executable folder on Windows and a few levels up inside the .app on macOS
     */
    private static Optional<Path> findLockfile(String executable)
    {
        for (Path dir = Paths.get(executable).getParent(); dir != null; dir = dir.getParent())
        {
            Path lockfile = dir.resolve("lockfile");
            if (Files.isRegularFile(lockfile))
            {
                return Optional.of(lockfile);
            }
        }
        return Optional.empty();
    }
    
    public static Map<String, String> getLCUAuthorizationHeader()
    {
        Pair<String, String> info    = getLCUConnectionData();
        String               val     = "riot:" + info.getA();
        String               encoded = new String(Base64.getEncoder().encode(val.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        
        return Map.of("Authorization", "Basic " + encoded);
    }
}

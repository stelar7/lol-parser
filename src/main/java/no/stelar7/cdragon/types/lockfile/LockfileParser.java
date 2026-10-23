package no.stelar7.cdragon.types.lockfile;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.lockfile.data.Lockfile;
import no.stelar7.cdragon.util.readers.*;

/**
 * Parses the League client lockfile, which has the format {@code name:pid:port:password:protocol}
 */
public class LockfileParser implements Parseable<Lockfile>
{
    @Override
    public Lockfile parse(RandomAccessReader raf)
    {
        return parse(raf.readAsString());
    }

    public Lockfile parse(String content)
    {
        String   line = content.strip();
        String[] data = line.split(":", -1);
        if (data.length < 5)
        {
            throw new ParseException("Invalid lockfile: expected name:pid:port:password:protocol, found " + data.length + " fields", -1);
        }

        Lockfile file = new Lockfile();
        file.setProcess(data[0]);
        file.setPID(parseInt(data[1], "pid"));
        file.setPort(parseInt(data[2], "port"));

        // the password is the only field that can contain ':', the protocol is always last
        file.setPassword(String.join(":", java.util.Arrays.copyOfRange(data, 3, data.length - 1)));
        file.setProtocol(data[data.length - 1]);

        return file;
    }

    private int parseInt(String value, String field)
    {
        try
        {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e)
        {
            throw new ParseException("Invalid lockfile " + field + ": '" + value + "'", -1, e);
        }
    }
}

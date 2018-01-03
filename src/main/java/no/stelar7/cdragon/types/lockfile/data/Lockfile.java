package no.stelar7.cdragon.types.lockfile.data;

import lombok.Data;

@Data
public class Lockfile
{
    private String process;
    private int    PID;
    private int    port;
    private String protocol;
    private String password;
}

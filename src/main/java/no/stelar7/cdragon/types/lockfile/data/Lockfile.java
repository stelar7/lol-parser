package no.stelar7.cdragon.types.lockfile.data;

public class Lockfile
{
    private String process;
    private int    PID;
    private int    port;
    private String protocol;
    private String password;
    
    public String getProcess()
    {
        return process;
    }
    
    public void setProcess(String process)
    {
        this.process = process;
    }
    
    public int getPID()
    {
        return PID;
    }
    
    public void setPID(int PID)
    {
        this.PID = PID;
    }
    
    public int getPort()
    {
        return port;
    }
    
    public void setPort(int port)
    {
        this.port = port;
    }
    
    public String getProtocol()
    {
        return protocol;
    }
    
    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }
    
    public String getPassword()
    {
        return password;
    }
    
    public void setPassword(String password)
    {
        this.password = password;
    }
}

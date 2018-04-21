package no.stelar7.cdragon.types.scb.data;

public class SCBFile
{
    private SCBHeader  header;
    private SCBContent content;
    
    public SCBHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(SCBHeader header)
    {
        this.header = header;
    }
    
    public SCBContent getContent()
    {
        return content;
    }
    
    public void setContent(SCBContent content)
    {
        this.content = content;
    }
}

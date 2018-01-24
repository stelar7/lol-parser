package no.stelar7.cdragon.types.scb.data;

import lombok.Data;

@Data
public class SCBFile
{
    private SCBHeader  header;
    private SCBContent content;
    
}

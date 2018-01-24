package no.stelar7.cdragon.types.scb.data;

import lombok.Data;

@Data
public class SCBHeader
{
    private String magic;
    private short  major;
    private short  minor;
    private String filename;
}

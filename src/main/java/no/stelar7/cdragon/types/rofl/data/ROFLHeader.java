package no.stelar7.cdragon.types.rofl.data;

import lombok.Data;

@Data
public class ROFLHeader
{
    private String       magic;
    private byte[]       signature;
    private short        headerSize;
    private int          fileSize;
    private int          metadataOffset;
    private int          metadataLength;
    private int          payloadHeaderOffset;
    private int          payloadHeaderLength;
    private int          payloadOffset;
    private ROFLMetadata metadata;
}

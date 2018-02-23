package no.stelar7.cdragon.types.rofl.data;

import lombok.Data;

import java.util.List;

@Data
public class ROFLFile
{
    private ROFLHeader             header;
    private ROFLPayloadHeader      payloadHeader;
    private List<ROFLPayloadEntry> entries;
}

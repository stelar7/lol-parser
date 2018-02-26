package no.stelar7.cdragon.types.skn.data;

import lombok.Data;

@Data
public class SKNMaterial
{
    private String name;
    private int    startVertex;
    private int    numVertex;
    private int    startIndex;
    private int    numIndex;
}

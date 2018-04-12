package no.stelar7.cdragon.types.bin.data;

import lombok.Data;
import no.stelar7.cdragon.util.types.Vector2;

import java.util.*;

@Data
public class BINMap
{
    private BINValueType                  type1;
    private BINValueType                  type2;
    private int                           size;
    private int                           count;
    private List<Vector2<Object, Object>> data = new ArrayList<>();
}

package no.stelar7.cdragon.util.reader.types;

import lombok.Data;

@Data
public class Quaternion<T>
{
    private T x;
    private T y;
    private T z;
    private T w;
}


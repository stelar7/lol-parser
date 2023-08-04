package no.stelar7.cdragon.types.rman.data;

import java.util.Arrays;

/**
 * Created: 04/08/2023 11:54
 * Author: hawolt
 * Reference: see Table section https://flatbuffers.dev/flatbuffers_internals.html
 **/

public class RMANVTable
{
    private int   objects;
    private int   size;
    private int[] offsets;

    public int getObjects()
    {
        return objects;
    }

    public void setObjects(int objects)
    {
        this.objects = objects;
    }

    public int getSize()
    {
        return size;
    }

    public void setSize(int size)
    {
        this.size = size;
    }

    public int[] getOffsets()
    {
        return offsets;
    }

    public void setOffsets(int[] offsets)
    {
        this.offsets = offsets;
    }

    @Override
    public String toString()
    {
        return "VTable{" +
                "objects=" + objects +
                ", size=" + size +
                ", offsets=" + Arrays.toString(offsets) +
                '}';
    }
}

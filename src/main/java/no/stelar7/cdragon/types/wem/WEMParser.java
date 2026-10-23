package no.stelar7.cdragon.types.wem;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.wem.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

public class WEMParser implements Parseable<WEMFile>
{
    @Override
    public WEMFile parse(RandomAccessReader raf)
    {
        int length = (int) raf.remaining();

        // reuse the backing array when the reader covers exactly this wem
        byte[] raw  = raf.getRawBytes();
        byte[] data = (raw != null && raf.pos() == 0 && raw.length == raf.size()) ? raw : raf.readBytes(raf.remaining());
        raf.seek(raf.size());

        return new WEMFile(0, length, null, new WEMData(data), data, null);
    }
}

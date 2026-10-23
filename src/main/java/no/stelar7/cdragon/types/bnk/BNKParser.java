package no.stelar7.cdragon.types.bnk;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.bnk.data.*;
import no.stelar7.cdragon.util.readers.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Wwise soundbank parser. Section layouts follow wwiser (wwiser/parser/wparser.py)
 */
public class BNKParser implements Parseable<BNKFile>
{
    // DIDX must come before DATA, BKHD (bank version) before HIRC/STID
    private final List<String> order = Arrays.asList("BKHD", "DIDX", "DATA", "ENVS", "FXPR", "HIRC", "STID", "STMG");

    @Override
    public BNKFile parse(RandomAccessReader raf)
    {
        BNKFile file = new BNKFile();

        List<BNKHeader> headers = new ArrayList<>();
        while (raf.remaining() >= 8)
        {
            BNKHeader header = parseHeader(raf);
            long      end    = header.getDataStart() + Integer.toUnsignedLong(header.getLength());
            if (end > raf.size())
            {
                throw new ParseException("Section " + header.getSection() + " truncated", header.getDataStart() - 8);
            }
            headers.add(header);
            raf.seek(end);
        }

        headers.sort(Comparator.comparingInt(e -> {
            int index = order.indexOf(e.getSection());
            return index < 0 ? Integer.MAX_VALUE : index;
        }));

        for (BNKHeader header : headers)
        {
            try
            {
                parseSection(header, raf, file);
            } catch (IndexOutOfBoundsException e)
            {
                throw new ParseException("Section " + header.getSection() + " truncated", raf.pos(), e);
            }
        }

        return file;
    }

    private void parseSection(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        switch (header.getSection())
        {
            case "BKHD" -> parseBKDH(header, raf, file);
            case "DIDX" -> parseDIDX(header, raf, file);
            case "DATA" -> parseDATA(header, raf, file);
            case "HIRC" -> parseHIRC(header, raf, file);
            case "STID" -> parseSTID(header, raf, file);
            // ENVS, FXPR, STMG, INIT, PLAT and anything unknown
            default -> parseRaw(header, raf, file);
        }
    }

    private void parseRaw(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        raf.seek(header.getDataStart());
        file.getOtherSections().add(new BNKRawSection(header, raf.readBytes(Integer.toUnsignedLong(header.getLength()))));
    }

    private int getBankVersion(BNKFile file)
    {
        // assume a modern bank when there is no header
        return file.getBankHeader() == null ? Integer.MAX_VALUE : file.getBankHeader().getBankVersion();
    }

    // CAkBankMgr__ProcessStringMappingChunk
    private void parseSTID(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        if (getBankVersion(file) <= 26)
        {
            // the old string format is a tree that is not understood, keep it as is
            parseRaw(header, raf, file);
            return;
        }

        raf.seek(header.getDataStart());
        long end = header.getDataStart() + Integer.toUnsignedLong(header.getLength());

        int                  stringType = raf.readInt();
        Map<Integer, String> bankNames  = new LinkedHashMap<>();
        long                 count      = raf.readU32();
        for (long i = 0; i < count; i++)
        {
            if (raf.pos() + 5 > end)
            {
                throw new ParseException("STID entry out of range", raf);
            }

            int bankId     = raf.readInt();
            int nameLength = raf.readU8();
            if (raf.pos() + nameLength > end)
            {
                throw new ParseException("STID name out of range", raf);
            }

            bankNames.put(bankId, raf.readString(nameLength));
        }

        file.setBankIds(new BNKSTID(header, stringType, bankNames));
    }

    // CAkBankMgr__ProcessHircChunk
    private void parseHIRC(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        List<BNKHIRCObject> objects = new ArrayList<>();
        raf.seek(header.getDataStart());

        long    end       = header.getDataStart() + Integer.toUnsignedLong(header.getLength());
        int     version   = getBankVersion(file);
        boolean wideType  = version <= 48;
        int     typeBytes = wideType ? 4 : 1;

        long objectCount = raf.readU32();
        for (long i = 0; i < objectCount; i++)
        {
            long objectStart = raf.pos();
            if (objectStart + typeBytes + 4 > end)
            {
                throw new ParseException("HIRC object " + i + " header out of range", objectStart);
            }

            BNKHIRCObject ho = new BNKHIRCObject();
            ho.setType(wideType ? raf.readInt() : raf.readU8(), version);

            // the length includes the id
            long length     = raf.readU32();
            long nextObject = raf.pos() + length;
            if (nextObject > end)
            {
                throw new ParseException("HIRC object " + i + " out of range", objectStart);
            }

            ho.setLength((int) length);
            if (length >= 4)
            {
                ho.setId(raf.readInt());
                ho.setData(raf.readBytes(length - 4));
            } else
            {
                ho.setData(raf.readBytes(length));
            }

            // the object internals are parsed from the (bounded) object data only; on failure the raw data is kept and getParseError() is set
            ho.parse();

            objects.add(ho);

            // never trust the object internals to leave the stream at the next object
            raf.seek(nextObject);
        }

        file.setObjectHierarchy(new BNKHIRC(header, objects));
    }

    private void parseDATA(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        List<BNKDATAWEMFile> wemFiles = new ArrayList<>();
        long                 start    = header.getDataStart();
        long                 length   = Integer.toUnsignedLong(header.getLength());

        // without an index there is no way to know where the files are
        if (file.getDataIndex() != null)
        {
            for (BNKDIDXEntry entry : file.getDataIndex().getEntries())
            {
                long offset = Integer.toUnsignedLong(entry.getFileOffset());
                long size   = Integer.toUnsignedLong(entry.getFileSize());
                if (offset + size > length)
                {
                    throw new ParseException("DIDX entry " + Integer.toUnsignedString(entry.getFileId()) + " is outside the DATA section", start + offset);
                }

                raf.seek(start + offset);

                wemFiles.add(new BNKDATAWEMFile(entry.getFileId(), raf.readBytes(size)));
            }
        }

        file.setData(new BNKDATA(header, wemFiles));
    }

    private void parseDIDX(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        raf.seek(header.getDataStart());
        long didxCount = Integer.toUnsignedLong(header.getLength()) / 12;

        List<BNKDIDXEntry> entries = new ArrayList<>();
        for (long i = 0; i < didxCount; i++)
        {
            int fileId     = raf.readInt();
            int fileOffset = raf.readInt();
            int fileSize   = raf.readInt();

            entries.add(new BNKDIDXEntry(fileId, fileOffset, fileSize));
        }

        file.setDataIndex(new BNKDIDX(header, entries));
    }

    private void parseBKDH(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        raf.seek(header.getDataStart());
        long length = Integer.toUnsignedLong(header.getLength());

        if (length < 8)
        {
            throw new ParseException("BKHD too small", header.getDataStart());
        }

        int bankVersion = raf.readInt();
        int bankId      = raf.readInt();
        int languageId  = 0;
        int feedback    = 0;
        if (length >= 12)
        {
            languageId = raf.readInt();
        }
        if (length >= 16)
        {
            feedback = raf.readInt();
        }

        file.setBankHeader(new BNKBKHD(header, bankVersion, bankId, languageId, feedback));
    }

    private BNKHeader parseHeader(RandomAccessReader raf)
    {
        String section = new String(raf.readBytes(4), StandardCharsets.ISO_8859_1);
        int    length  = raf.readInt();
        return new BNKHeader(section, length, (int) raf.pos());

    }
}

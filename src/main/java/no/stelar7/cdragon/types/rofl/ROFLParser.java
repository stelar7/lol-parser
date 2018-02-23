package no.stelar7.cdragon.types.rofl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import no.stelar7.cdragon.types.rofl.data.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.lang.reflect.Type;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;


//https://github.com/robertabcd/lol-ob/wiki/ROFL-Container-Notes
public class ROFLParser
{
    public ROFLFile parse(Path path)
    {
        RandomAccessReader raf = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN);
        
        return parse(raf);
    }
    
    private ROFLFile parse(RandomAccessReader raf)
    {
        ROFLFile file = new ROFLFile();
        file.setHeader(parseHeader(raf));
        file.setPayloadHeader(parsePayloadHeader(raf));
        file.setEntries(parseEntries(raf, file));
        return file;
    }
    
    private List<ROFLPayloadEntry> parseEntries(RandomAccessReader raf, ROFLFile file)
    {
        List<ROFLPayloadEntry> entries = new ArrayList<>();
        
        int currentEntry    = 0;
        int maxEntries      = file.getPayloadHeader().getChunkCount() + file.getPayloadHeader().getKeyframeCount();
        int entryDataOffset = file.getHeader().getPayloadOffset() + (17 * maxEntries);
        
        while (currentEntry < maxEntries)
        {
            raf.seek(file.getHeader().getPayloadOffset() + (17 * currentEntry));
            ROFLPayloadEntry entry = parsePayloadEntry(raf, entryDataOffset);
            entries.add(entry);
            currentEntry++;
        }
        
        
        return entries;
    }
    
    private ROFLPayloadEntry parsePayloadEntry(RandomAccessReader raf, int entryDataOffset)
    {
        ROFLPayloadEntry entry = new ROFLPayloadEntry();
        
        entry.setId(raf.readInt());
        entry.setType(raf.readByte());
        entry.setLength(raf.readInt());
        entry.setNextChunkId(raf.readInt());
        entry.setOffset(raf.readInt());
        
        raf.seek(entryDataOffset + entry.getOffset());
        entry.setData(raf.readBytes(entry.getLength()));
        return entry;
    }
    
    private ROFLPayloadHeader parsePayloadHeader(RandomAccessReader raf)
    {
        ROFLPayloadHeader header = new ROFLPayloadHeader();
        
        header.setGameId(raf.readLong());
        header.setGameLength(raf.readInt());
        header.setKeyframeCount(raf.readInt());
        header.setChunkCount(raf.readInt());
        header.setEndStartupChunkId(raf.readInt());
        header.setStartGameChunkId(raf.readInt());
        header.setKeyframeInterval(raf.readInt());
        header.setEncryptionKeyLength(raf.readShort());
        header.setEncryptionKey(Base64.getDecoder().decode(raf.readString(header.getEncryptionKeyLength(), StandardCharsets.UTF_8)));
        
        return header;
    }
    
    private ROFLHeader parseHeader(RandomAccessReader raf)
    {
        ROFLHeader header = new ROFLHeader();
        header.setMagic(raf.readString(6));
        header.setSignature(raf.readBytes(256));
        header.setHeaderSize(raf.readShort());
        header.setFileSize(raf.readInt());
        header.setMetadataOffset(raf.readInt());
        header.setMetadataLength(raf.readInt());
        header.setPayloadHeaderOffset(raf.readInt());
        header.setPayloadHeaderLength(raf.readInt());
        header.setPayloadOffset(raf.readInt());
        
        header.setMetadata(parseMetadata(raf.readString(header.getMetadataLength())));
        
        return header;
    }
    
    private ROFLMetadata parseMetadata(String json)
    {
        JsonParser parser = new JsonParser();
        JsonObject object = (JsonObject) parser.parse(json);
        
        ROFLMetadata metadata = new ROFLMetadata();
        metadata.setGameLength(object.get("gameLength").getAsInt());
        metadata.setGameVersion(object.get("gameVersion").getAsString());
        metadata.setLastGameChunkId(object.get("lastGameChunkId").getAsInt());
        metadata.setLastKeyframeId(object.get("lastKeyFrameId").getAsInt());
        
        JsonElement             stats     = parser.parse(object.get("statsJson").getAsString());
        Type                    statsType = new TypeToken<List<ROFLMetadataStats>>() {}.getType();
        List<ROFLMetadataStats> statList  = UtilHandler.getGson().fromJson(stats, statsType);
        metadata.setStatsJson(statList);
        
        return metadata;
    }
}

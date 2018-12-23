package no.stelar7.cdragon.types.rman;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

public class RMANParser implements Parseable<RMANFile>
{
    
    @Override
    public RMANFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public RMANFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public RMANFile parse(RandomAccessReader raf)
    {
        RMANFile file = new RMANFile();
        file.setHeader(parseHeader(raf));
        
        raf.seek(file.getHeader().getOffset());
        file.setCompressedBody(raf.readBytes(file.getHeader().getLength()));
        
        if (file.getHeader().getSignatureType() != 0)
        {
            file.setSignature(raf.readBytes(256));
        }
        
        file.setBody(parseCompressedBody(file));
        file.buildChunkMap();
        return file;
    }
    
    private RMANFileBody parseCompressedBody(RMANFile file)
    {
        byte[]             uncompressed = CompressionHandler.uncompressZSTD(file.getCompressedBody(), file.getHeader().getDecompressedLength());
        RandomAccessReader raf          = new RandomAccessReader(uncompressed, ByteOrder.LITTLE_ENDIAN);
        RMANFileBody       body         = new RMANFileBody();
        body.setHeaderOffset(raf.readInt());
        raf.seek(body.getHeaderOffset());
        
        RMANFileBodyHeader header = new RMANFileBodyHeader();
        header.setOffsetTableOffset(raf.readInt());
        
        int offset = raf.readInt();
        raf.seek(raf.pos() + offset);
        header.setBundleListOffset(raf.pos() - 4);
        raf.seek(raf.pos() - offset);
        
        offset = raf.readInt();
        raf.seek(raf.pos() + offset);
        header.setLanguageListOffset(raf.pos() - 4);
        raf.seek(raf.pos() - offset);
        
        offset = raf.readInt();
        raf.seek(raf.pos() + offset);
        header.setFileListOffset(raf.pos() - 4);
        raf.seek(raf.pos() - offset);
        
        offset = raf.readInt();
        raf.seek(raf.pos() + offset);
        header.setFolderListOffset(raf.pos() - 4);
        raf.seek(raf.pos() - offset);
        
        offset = raf.readInt();
        raf.seek(raf.pos() + offset);
        header.setKeyHeaderOffset(raf.pos() - 4);
        raf.seek(raf.pos() - offset);
        
        offset = raf.readInt();
        raf.seek(raf.pos() + offset);
        header.setUnknownOffset(raf.pos() - 4);
        raf.seek(raf.pos() - offset);
        
        body.setHeader(header);
        
        
        body.setBundles(parseBundles(raf, header));
        body.setLanguages(parseLanguages(raf, header));
        body.setFiles(parseFiles(raf, header));
        body.setDirectories(parseDirectories(raf, header));
        
        return body;
    }
    
    private List<RMANFileBodyDirectory> parseDirectories(RandomAccessReader raf, RMANFileBodyHeader header)
    {
        List<RMANFileBodyDirectory> data = new ArrayList<>();
        raf.seek(header.getFolderListOffset());
        
        int count = raf.readInt();
        for (int i = 0; i < count; i++)
        {
            RMANFileBodyDirectory dir = new RMANFileBodyDirectory();
            
            dir.setOffset(raf.readInt());
            int nextFileOffset = raf.pos();
            raf.seek(nextFileOffset + dir.getOffset() - 4);
            
            dir.setOffsetTableOffset(raf.readInt());
            int resumeOffset = raf.pos();
            raf.seek(raf.pos() - dir.getOffsetTableOffset());
            dir.setDirectoryIdOffset(raf.readShort());
            dir.setParentIdOffset(raf.readShort());
            raf.seek(resumeOffset);
            dir.setNameOffset(raf.readInt());
            raf.seek(raf.pos() + dir.getNameOffset() - 4);
            dir.setName(raf.readString(raf.readInt()));
            raf.seek(nextFileOffset + dir.getOffset() + 4);
            
            if (dir.getDirectoryIdOffset() > 0)
            {
                dir.setDirectoryId(raf.readLong());
            }
            
            if (dir.getParentIdOffset() > 0)
            {
                dir.setParentId(raf.readLong());
            }
            
            raf.seek(nextFileOffset);
            data.add(dir);
        }
        
        return data;
    }
    
    private List<RMANFileBodyFile> parseFiles(RandomAccessReader raf, RMANFileBodyHeader header)
    {
        List<RMANFileBodyFile> data = new ArrayList<>();
        raf.seek(header.getFileListOffset());
        
        int count = raf.readInt();
        for (int i = 0; i < count; i++)
        {
            RMANFileBodyFile bfile = new RMANFileBodyFile();
            
            bfile.setOffset(raf.readInt());
            int nextFileOffset = raf.pos();
            raf.seek(nextFileOffset + bfile.getOffset() - 4);
            
            bfile.setOffsetTableOffset(raf.readInt());
            bfile.setUnknown1(raf.readInt());
            bfile.setNameOffset(raf.readInt());
            raf.seek(raf.pos() + bfile.getNameOffset() - 4);
            bfile.setName(raf.readString(raf.readInt()));
            raf.seek(nextFileOffset + bfile.getOffset() + 8);
            bfile.setStructSize(raf.readInt());
            bfile.setSymlinkOffset(raf.readInt());
            raf.seek(raf.pos() + bfile.getSymlinkOffset() - 4);
            bfile.setSymlink(raf.readString(raf.readInt()));
            raf.seek(nextFileOffset + bfile.getOffset() + 16);
            bfile.setFileId(raf.readLong());
            
            if (bfile.getStructSize() > 28)
            {
                bfile.setDirectoryId(raf.readLong());
            }
            
            bfile.setFileSize(raf.readInt());
            bfile.setPermissions(raf.readInt());
            
            if (bfile.getStructSize() > 36)
            {
                bfile.setLanguageId(raf.readInt());
                bfile.setUnknown2(raf.readInt());
            }
            bfile.setUnknown3(raf.readInt());
            bfile.setChunkIds(raf.readLongs(raf.readInt()));
            
            raf.seek(nextFileOffset);
            data.add(bfile);
        }
        
        return data;
    }
    
    private List<RMANFileBodyLanguage> parseLanguages(RandomAccessReader raf, RMANFileBodyHeader header)
    {
        List<RMANFileBodyLanguage> data = new ArrayList<>();
        raf.seek(header.getLanguageListOffset());
        
        int count = raf.readInt();
        for (int i = 0; i < count; i++)
        {
            RMANFileBodyLanguage language = new RMANFileBodyLanguage();
            
            language.setOffset(raf.readInt());
            int nextLanguageOffset = raf.pos();
            raf.seek(nextLanguageOffset + language.getOffset() - 4);
            
            language.setOffsetTableOffset(raf.readInt());
            language.setId(raf.readInt());
            language.setNameOffset(raf.readInt());
            raf.seek(raf.pos() + language.getNameOffset() - 4);
            language.setName(raf.readString(raf.readInt()));
            
            raf.seek(nextLanguageOffset);
            data.add(language);
        }
        
        return data;
    }
    
    private List<RMANFileBodyBundle> parseBundles(RandomAccessReader raf, RMANFileBodyHeader header)
    {
        List<RMANFileBodyBundle> data = new ArrayList<>();
        raf.seek(header.getBundleListOffset());
        
        int count = raf.readInt();
        for (int i = 0; i < count; i++)
        {
            RMANFileBodyBundle bundle = new RMANFileBodyBundle();
            
            bundle.setOffset(raf.readInt());
            int nextBundleOffset = raf.pos();
            raf.seek(nextBundleOffset + bundle.getOffset() - 4);
            
            bundle.setOffsetTableOffset(raf.readInt());
            bundle.setHeaderSize(raf.readInt());
            bundle.setBundleId(HashHandler.toHex(raf.readLong(), 16));
            if (bundle.getHeaderSize() > 12)
            {
                bundle.setSkipped(raf.readBytes(bundle.getHeaderSize() - 12));
            }
            
            List<RMANFileBodyBundleChunk> chunks     = new ArrayList<>();
            int                           chunkCount = raf.readInt();
            for (int j = 0; j < chunkCount; j++)
            {
                int chunkOffset     = raf.readInt();
                int nextChunkOffset = raf.pos();
                raf.seek(chunkOffset + nextChunkOffset - 4);
                
                RMANFileBodyBundleChunk chunk = new RMANFileBodyBundleChunk();
                chunk.setOffsetTableOffset(raf.readInt());
                chunk.setCompressedSize(raf.readInt());
                chunk.setUncompressedSize(raf.readInt());
                chunk.setChunkId(HashHandler.toHex(raf.readLong(), 16));
                chunks.add(chunk);
                
                raf.seek(nextChunkOffset);
            }
            
            bundle.setChunks(chunks);
            raf.seek(nextBundleOffset);
            data.add(bundle);
        }
        
        return data;
    }
    
    private RMANFileHeader parseHeader(RandomAccessReader raf)
    {
        RMANFileHeader header = new RMANFileHeader();
        header.setMagic(raf.readString(4));
        header.setMajor(raf.readByte());
        header.setMinor(raf.readByte());
        header.setUnknown(raf.readByte());
        header.setSignatureType(raf.readByte());
        header.setOffset(raf.readInt());
        header.setLength(raf.readInt());
        header.setManifestId(raf.readLong());
        header.setDecompressedLength(raf.readInt());
        return header;
    }
}
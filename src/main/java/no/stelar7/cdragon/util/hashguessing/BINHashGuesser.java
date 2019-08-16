package no.stelar7.cdragon.util.hashguessing;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.BINFile;
import no.stelar7.cdragon.util.handlers.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class BINHashGuesser extends HashGuesser
{
    private BINParser     parser = new BINParser();
    private List<BINFile> files  = new ArrayList<>();
    
    public BINHashGuesser(Collection<String> strings, Path dataPath)
    {
        super(HashGuesser.hashFileBIN, strings);
        
        try
        {
            files = Files.walk(dataPath)
                         .filter(UtilHandler.IS_BIN_PREDICATE)
                         .map(parser::parse).collect(Collectors.toList());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void guessNewCharacters()
    {
        files.stream()
             .flatMap(b -> b.getEntries().stream())
             .filter(b -> b.getType().equalsIgnoreCase("character"))
             .forEach(e -> {
                 String name   = (String) e.getValues().get(0).getValue();
                 String toHash = "Characters/" + name;
                 this.check(toHash);
             });
    }
    
    public void guessNewAnimations()
    {
        files.stream()
             .flatMap(b -> b.getEntries().stream())
             .filter(b -> b.getType().equalsIgnoreCase("animationGraphData"))
             .forEach(e -> {
                 System.out.println();
             });
    }
    
    
    /**
     * returns false if there are no more hashes
     */
    @Override
    public boolean check(String path)
    {
        Long   hashNum = HashHandler.computeBINHash(path);
        String hash    = HashHandler.toHex(hashNum, 8);
        if (this.unknown.contains(hash))
        {
            this.addKnown(hash, path);
            return true;
        }
        
        if (this.unknown.isEmpty())
        {
            System.out.println("No more unknown hashes!");
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean isKnown(String path)
    {
        long   hashNum = HashHandler.computeBINHash(path);
        String hash    = HashHandler.toHex(hashNum, 8);
        if (this.unknown.contains(hash))
        {
            this.addKnown(hash, path);
            return true;
        }
        
        return this.known.containsKey(hash);
    }
}

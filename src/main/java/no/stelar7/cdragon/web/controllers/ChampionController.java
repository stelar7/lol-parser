package no.stelar7.cdragon.web.controllers;

import no.stelar7.cdragon.web.services.ChampionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;

@Controller
@RequestMapping("{version}/champion")
public class ChampionController
{
    
    @Autowired
    ChampionService championService;
    
    
    @ResponseBody
    @RequestMapping(value = "{key}/square", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<InputStreamResource> getSquare(@PathVariable String key) throws IOException
    {
        Optional<Path> result = championService.findChampionSquare(key);
        return pathToResource(result);
    }
    
    @ResponseBody
    @RequestMapping(value = {"{key}/tile", "{key}/tile/{skin}",}, method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<InputStreamResource> getTile(@PathVariable String key, @PathVariable @Nullable String skin) throws IOException
    {
        Optional<Path> result = championService.findChampionTile(key, skin);
        return pathToResource(result);
    }
    
    @ResponseBody
    @RequestMapping(value = {"{key}/portrait", "{key}/portrait/{skin}",}, method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<InputStreamResource> getPortait(@PathVariable String key, @PathVariable @Nullable String skin) throws IOException
    {
        Optional<Path> result = championService.findChampionPortrait(key, skin);
        return pathToResource(result);
    }
    
    
    @ResponseBody
    @RequestMapping(value = {"{key}/splash", "{key}/splash/{skin}",}, method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<InputStreamResource> getCenteredSplash(@PathVariable String key, @PathVariable @Nullable String skin) throws IOException
    {
        Optional<Path> result = championService.findChampionSplash(key, skin);
        return pathToResource(result);
    }
    
    @ResponseBody
    @RequestMapping(value = {"{key}/uncentered", "{key}/uncentered/{skin}",}, method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<InputStreamResource> getUncenteredSplash(@PathVariable String key, @PathVariable @Nullable String skin) throws IOException
    {
        Optional<Path> result = championService.findUncenteredChampionSplash(key, skin);
        return pathToResource(result);
    }
    
    public ResponseEntity<InputStreamResource> pathToResource(Optional<Path> result) throws IOException
    {
        if (result.isPresent())
        {
            Path file = result.get();
            
            return ResponseEntity.ok()
                                 .contentLength(Files.size(file))
                                 .contentType(MediaType.IMAGE_JPEG)
                                 .body(new InputStreamResource(Files.newInputStream(file)));
            
        } else
        {
            return ResponseEntity.notFound().build();
        }
    }
}

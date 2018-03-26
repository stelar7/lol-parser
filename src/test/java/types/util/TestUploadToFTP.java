package types.util;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.apache.commons.net.ftp.*;
import org.junit.Test;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class TestUploadToFTP
{
    @Test
    public void uploadTest() throws IOException
    {
        List<String> prefix = new ArrayList<>(Arrays.asList("www", "cdragon", "latest"));
        
        FTPClient client = new FTPClient();
        client.connect("ftp.domeneshop.no");
        client.login(SecretFile.USERNAME, SecretFile.PASSWORD);
        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.changeWorkingDirectory(prefix.stream().reduce("", (o, n) -> o + "/" + n));
        
        Path files = UtilHandler.DOWNLOADS_FOLDER.resolve("rcp-be-lol-game-data\\pretty\\");
        Files.walkFileTree(files, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                if (UtilHandler.pathToFolderName(dir).equalsIgnoreCase("pretty"))
                {
                    return FileVisitResult.CONTINUE;
                }
                
                String folder = UtilHandler.pathToFolderName(dir);
                
                prefix.add(folder);
                client.makeDirectory(folder);
                client.changeWorkingDirectory(prefix.stream().reduce("", (o, n) -> o + "/" + n));
                
                System.out.println("Uploading to " + client.printWorkingDirectory());
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                prefix.remove(UtilHandler.pathToFolderName(dir));
                client.changeWorkingDirectory(prefix.stream().reduce("", (o, n) -> o + "/" + n));
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                FileInputStream is  = new FileInputStream(file.toFile());
                String          pre = prefix.stream().reduce("", (o, n) -> o + "/" + n);
                
                client.storeFile(file.getFileName().toString(), is);
                return FileVisitResult.CONTINUE;
            }
        });
        
        
        client.logout();
        client.disconnect();
        
    }
}

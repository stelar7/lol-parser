import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.BINFile;
import org.junit.Test;

import java.nio.file.*;

public class TestBIN
{
    @Test
    public void testBIN()
    {
        BINParser parser = new BINParser();
        
        Path file = Paths.get(System.getProperty("user.home"), "Downloads\\parser_test", "Aatrox.bin");
        System.out.println("Parsing: " + file.toString());
    
        BINFile data = parser.parse(file);
        data.print();
        System.out.println();
    }
}

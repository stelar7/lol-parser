package types;

import no.stelar7.cdragon.types.scb.SCBParser;
import no.stelar7.cdragon.types.scb.data.SCBFile;
import org.junit.Test;

import java.nio.file.*;

public class TestSCB
{
    @Test
    public void testSCB() {
        SCBParser parser = new SCBParser();
    
        Path file = Paths.get(System.getProperty("user.home"), "Downloads\\parser_test", "00a356da16b6715f.scb");
        System.out.println("Parsing: " + file.toString());
    
        SCBFile data = parser.parse(file);
        System.out.println();
    }
    
}

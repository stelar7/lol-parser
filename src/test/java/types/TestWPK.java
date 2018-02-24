package types;

import no.stelar7.cdragon.types.wpk.WPKParser;
import no.stelar7.cdragon.types.wpk.data.WPKFile;
import org.junit.Test;

import java.nio.file.*;

public class TestWPK
{
    @Test
    public void testWPK()
    {
        WPKParser parser = new WPKParser();
        
        Path file = Paths.get(System.getProperty("user.home"), "Downloads\\parser_test", "15646bae0aecf5be.wpk");
        System.out.println("Parsing: " + file.toString());
        
        WPKFile data = parser.parse(file);
        data.extract(file.getParent());
        System.out.println();
    }
}

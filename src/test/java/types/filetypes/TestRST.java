package types.filetypes;

import no.stelar7.cdragon.types.rst.RSTParser;
import org.junit.jupiter.api.Test;

import java.nio.file.*;

public class TestRST
{
    @Test
    public void testRST()
    {
        Path file = Paths.get("D:\\cdragon\\rst\\fontconfig_en_us.txt");
        RSTParser parser = new RSTParser();
        parser.parse(file);
    }
}

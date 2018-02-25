package viewer;

import no.stelar7.cdragon.types.skl.SKLParser;
import no.stelar7.cdragon.types.skl.data.SKLFile;
import viewer.util.Renderer;

import java.nio.file.*;

public class SKLViewer extends Renderer
{
    public static void main(String[] args)
    {
        Path   basePath    = Paths.get(System.getProperty("user.home"), "Downloads\\parser_test");
        String sklFilePath = "";
        String sknFilePath = "";
        
        SKLParser sklParser = new SKLParser();
        //SKNParser sknParser = new SKNParser();
        
        SKLFile skl = sklParser.parse(basePath.resolve(sklFilePath));
        //SKNFile skn = sknParser.parse(basePath.resolve(sknFilePath));
        
        new SKLViewer(600, 600).start();
        //new SKLViewer(600, 600, skl, skn).start();
    }
    
    public SKLViewer(int width, int height)
    {
        super(width, height);
    }

//    public SKLViewer(int width, int height, SKLFile skl, SKNFile skn)
//    {
//        super(width, height);
//    }
    
    @Override
    public void initPostGL()
    {
    
    }
    
    @Override
    public void update()
    {
    
    }
    
    @Override
    public void render()
    {
    
    }
}

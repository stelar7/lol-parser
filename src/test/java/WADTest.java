import no.stelar7.cdragon.util.UtilHandler;
import no.stelar7.cdragon.wad.WADParser;
import no.stelar7.cdragon.wad.data.WADFile;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Paths;

public class WADTest
{
    @Test
    public void testWAD() throws Exception
    {
        WADParser parser = new WADParser();
        WADFile   file   = parser.parseLatest(Paths.get("C:\\Users\\Steffen\\Downloads"));
        
        file.extractFiles(Paths.get("C:\\Users\\Steffen\\Downloads"));
    }
    
    public static void main(String[] args)
    {
        testXXHashLive();
    }
    
    public static void testXXHashLive()
    {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        
        GridLayout layout = new GridLayout(2, 1);
        JPanel     panel  = new JPanel(layout);
        JTextArea  input  = new JTextArea();
        input.setSize(600, 50);
        
        JTextField output = new JTextField();
        output.setHorizontalAlignment(JTextField.CENTER);
        output.setSize(600, 50);
        output.setEnabled(false);
        output.setDisabledTextColor(Color.BLACK);
        
        input.addKeyListener(new KeyListener()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {
            }
            
            @Override
            public void keyPressed(KeyEvent e)
            {
            }
            
            @Override
            public void keyReleased(KeyEvent e)
            {
                output.setText(UtilHandler.getHash(input.getText()));
            }
        });
        
        panel.add(input);
        panel.add(output);
        
        frame.add(panel);
        frame.setVisible(true);
        frame.setSize(600, 200);
        frame.setLocationRelativeTo(null);
        
        System.out.println("");
    }
}
import no.stelar7.cdragon.util.UtilHandler;

import javax.swing.*;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class LiveXXHashTest
{
    public static void main(String[] args) throws IOException
    {
        testXXHashLive();
    }
    
    public static void testXXHashLive() throws IOException
    {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        UndoManager undoManager = new UndoManager();
        
        GridLayout layout = new GridLayout(2, 1);
        JPanel     panel  = new JPanel(layout);
        JTextField input  = new JTextField();
        input.setHorizontalAlignment(JTextField.CENTER);
        input.setSize(600, 50);
        
        input.getDocument().addUndoableEditListener(undoManager);
        
        input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Undo");
        input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Redo");
        
        input.getActionMap().put("Undo", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    if (undoManager.canUndo())
                    {
                        undoManager.undo();
                    }
                } catch (CannotUndoException exp)
                {
                    exp.printStackTrace();
                }
            }
        });
        input.getActionMap().put("Redo", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    if (undoManager.canRedo())
                    {
                        undoManager.redo();
                    }
                } catch (CannotUndoException exp)
                {
                    exp.printStackTrace();
                }
            }
        });
        
        JTextField output = new JTextField();
        output.setHorizontalAlignment(JTextField.CENTER);
        output.setSize(600, 50);
        output.setEnabled(false);
        output.setDisabledTextColor(Color.BLACK);
        
        JFileChooser chooser = new JFileChooser();
        chooser.showOpenDialog(null);
        
        List<String> uk = Files.readAllLines(chooser.getSelectedFile().toPath());
        
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
                output.setDisabledTextColor(uk.contains(output.getText()) ? Color.GREEN : Color.RED);
            }
        });
        
        panel.add(input);
        panel.add(output);
        
        frame.add(panel);
        frame.setVisible(true);
        frame.setSize(600, 200);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
    }
}

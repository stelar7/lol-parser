package types.util;

import no.stelar7.cdragon.util.handlers.HashHandler;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.*;

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
        
        input.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                try
                {
                    
                    
                    String text      = e.getDocument().getText(0, e.getDocument().getLength());
                    String clipboard = ((String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor)).toLowerCase(Locale.ENGLISH);
                    
                    if (e.getLength() != clipboard.length())
                    {
                        return;
                    }
                    
                    if (text.equals(clipboard))
                    {
                        return;
                    }
                    
                    EventQueue.invokeLater(() -> input.setText(clipboard));
                } catch (BadLocationException | IOException | UnsupportedFlavorException e1)
                {
                    e1.printStackTrace();
                }
            }
            
            @Override
            public void removeUpdate(DocumentEvent e)
            {
            
            }
            
            @Override
            public void changedUpdate(DocumentEvent e)
            {
            
            }
        });
        
        input.getDocument().addUndoableEditListener(undoManager);
        input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "Undo");
        input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "Redo");
        input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "Select");
        
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
        
        input.getActionMap().put("Select", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                input.select(0, input.getText().length());
            }
        });
        
        JTextField output = new JTextField();
        output.setHorizontalAlignment(JTextField.CENTER);
        output.setSize(600, 50);
        output.setEnabled(false);
        output.setDisabledTextColor(Color.BLACK);
        output.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                StringSelection data = new StringSelection(output.getText());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data, data);
            }
        });
        
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(Paths.get(System.getProperty("user.home"), "Downloads").toFile());
        chooser.showOpenDialog(null);
        
        List<String> uk = Collections.emptyList();
        try
        {
            Files.readAllLines(chooser.getSelectedFile().toPath());
        } catch (NullPointerException e)
        {
            // ignore
        }
        
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
                output.setText(HashHandler.computeXXHash64(input.getText()));
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

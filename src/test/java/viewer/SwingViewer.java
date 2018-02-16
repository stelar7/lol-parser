package viewer;

import lombok.*;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.types.wad.data.content.WADContentHeaderV1;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.types.ByteArray;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;

public class SwingViewer
{
    public static void main(String[] args)
    {
        new SwingViewer();
    }
    
    private JTree tree;
    private List<Path>             singles    = new ArrayList<>();
    private List<Path>             containers = new ArrayList<>();
    private NaturalOrderComparator comparator = new NaturalOrderComparator();
    
    public SwingViewer()
    {
        DefaultMutableTreeNode top  = new DefaultMutableTreeNode("Riot Games folder");
        DefaultMutableTreeNode top2 = new DefaultMutableTreeNode("single files");
        DefaultMutableTreeNode top3 = new DefaultMutableTreeNode("containers");
        top.add(top2);
        top.add(top3);
        
        tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null)
            {
                return;
            }
            
            if (node.getUserObject() instanceof DataPair)
            {
                
                if (((DataPair) node.getUserObject()).getContent() instanceof Path)
                {
                    DataPair<Path> path = (DataPair) node.getUserObject();
                    
                    if (FileTypeHandler.isContainerFormat(path.getContent().getFileName().toString()))
                    {
                        List<DataPair> content = getContent(path.getContent());
                        content.forEach(s -> node.add(new DefaultMutableTreeNode(s)));
                        tree.expandPath(e.getPath());
                    }
                }
                
                if (((DataPair) node.getUserObject()).getContent() instanceof ByteArray)
                {
                    DataPair<ByteArray> data     = (DataPair) node.getUserObject();
                    String              filename = data.getName();
                    
                    if (filename.endsWith(".jpg") || filename.endsWith(".png"))
                    {
                        try
                        {
                            ByteArray            bContent = data.getContent();
                            ByteArrayInputStream is       = new ByteArrayInputStream(bContent.getData());
                            BufferedImage        image    = ImageIO.read(is);
                            JLabel               label    = new JLabel(new ImageIcon(image));
                            JScrollPane          pane     = new JScrollPane(label);
                            JFrame               frame    = new JFrame(filename);
                            frame.setMaximumSize(new Dimension(800, 600));
                            frame.getContentPane().add(pane);
                            frame.pack();
                            frame.setLocationRelativeTo(null);
                            frame.setVisible(true);
                        } catch (IOException e1)
                        {
                            e1.printStackTrace();
                        }
                    } else if (filename.endsWith(".json") || filename.endsWith(".txt") || filename.endsWith("js"))
                    {
                        ByteArray bContent = data.getContent();
                        JTextArea label    = new JTextArea(new String(data.getContent().getData(), StandardCharsets.UTF_8));
                        label.setEditable(false);
                        JScrollPane pane  = new JScrollPane(label);
                        JFrame      frame = new JFrame(filename);
                        frame.getContentPane().add(pane);
                        frame.pack();
                        frame.setMaximumSize(new Dimension(800, 600));
                        frame.setMinimumSize(new Dimension(400, 600));
                        frame.setSize(400, 600);
                        frame.setLocationRelativeTo(null);
                        frame.setVisible(true);
                    } else
                    {
                        JOptionPane.showMessageDialog(null, "This filetype is not supported for opening yet..\n" + filename, "Sorry, im lazy", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });
        
        
        addBaseNodes(top);
        
        JScrollPane view  = new JScrollPane(tree);
        JFrame      frame = new JFrame("LoL-Parser");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().add(view);
        frame.pack();
        frame.setVisible(true);
    }
    
    
    private void addBaseNodes(DefaultMutableTreeNode top)
    {
        Path baseFolder = null;
        Path ritoDir    = Paths.get("C:/Riot Games");
        if (Files.exists(ritoDir))
        {
            baseFolder = ritoDir;
        } else
        {
            JFileChooser chooser = new JFileChooser("C:/Riot Games");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = chooser.showOpenDialog(null);
            
            if (option == JFileChooser.APPROVE_OPTION)
            {
                Path selectedFolder = chooser.getSelectedFile().toPath();
                if (!Files.isDirectory(selectedFolder))
                {
                    System.out.println("Please choose the Riot Games base directory");
                    return;
                }
                baseFolder = selectedFolder;
            }
        }
        
        try
        {
            Files.walkFileTree(baseFolder, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    if (FileTypeHandler.isIgnoredType(file.getFileName().toString()))
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    if (FileTypeHandler.isContainerFormat(file.getFileName().toString()))
                    {
                        containers.add(file);
                        return FileVisitResult.CONTINUE;
                    }
                    
                    singles.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
            
            containers.sort((a, b) -> comparator.compare(extractParentFileName(a), extractParentFileName(b)));
            singles.sort((a, b) -> comparator.compare(extractParentFileName(a), extractParentFileName(b)));
            
            containers.forEach(file -> add(file, (DefaultMutableTreeNode) tree.getModel().getChild(top, 1)));
            singles.forEach(file -> add(file, (DefaultMutableTreeNode) tree.getModel().getChild(top, 0)));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private String extractParentFileName(Path file)
    {
        return file.getParent().getFileName() + "/" + file.getFileName();
    }
    
    private void add(Path file, DefaultMutableTreeNode parent)
    {
        String                 path = extractParentFileName(file);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new DataPair(path, file));
        parent.add(node);
    }
    
    private List<DataPair> getContent(Path path)
    {
        String         name    = path.getFileName().toString();
        List<DataPair> content = new ArrayList<>();
        
        if (name.endsWith(".wad") || name.endsWith(".wad.client"))
        {
            WADParser parser = new WADParser();
            WADFile   file   = parser.parse(path);
            
            String plugin = path.toString().substring(0, path.toString().lastIndexOf("\\"));
            plugin = plugin.substring(plugin.lastIndexOf("\\") + 1);
            
            for (WADContentHeaderV1 header : file.getContentHeaders())
            {
                String hash     = String.format("%016X", header.getPathHash()).toLowerCase(Locale.ENGLISH);
                String filename = UtilHandler.getKnownWADFileHashes(plugin).getOrDefault(hash, hash);
                content.add(new DataPair(filename, new ByteArray(file.readContentFromHeaderData(header))));
            }
        }
        
        Collections.sort(content);
        return content;
    }
    
    @Getter
    @AllArgsConstructor
    class DataPair<T> implements Comparable<DataPair<T>>
    {
        private String name;
        private T      content;
        
        @Override
        public int compareTo(DataPair<T> o)
        {
            boolean own   = name.contains(".");
            boolean other = o.name.contains(".");
            
            if (own && !other)
            {
                return -1;
            }
            
            if (!own && other)
            {
                return 1;
            }
            
            if ((!own && !other) || (own && other))
            {
                return comparator.compare(name, o.name);
            }
            
            return comparator.compare(name, o.name);
        }
        
        @Override
        public String toString()
        {
            return name;
        }
    }
    
}

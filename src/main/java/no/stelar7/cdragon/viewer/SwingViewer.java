package no.stelar7.cdragon.viewer;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.raf.RAFParser;
import no.stelar7.cdragon.types.raf.data.*;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.*;
import no.stelar7.cdragon.types.wad.data.content.*;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.ByteArray;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
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
    
    private final JTree                  tree;
    private final List<Path>             singles     = new ArrayList<>();
    private final List<Path>             containers  = new ArrayList<>();
    private final NaturalOrderComparator comparator  = new NaturalOrderComparator();
    private final JScrollPane            contentPane = new JScrollPane();
    
    @SuppressWarnings("unchecked")
    public SwingViewer()
    {
        DefaultMutableTreeNode top  = new DefaultMutableTreeNode("Riot Games folder");
        DefaultMutableTreeNode top2 = new DefaultMutableTreeNode("single files");
        DefaultMutableTreeNode top3 = new DefaultMutableTreeNode("containers");
        top.add(top2);
        top.add(top3);
        
        tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.addTreeSelectionListener(this::valueChangedListener);
        tree.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (!SwingUtilities.isRightMouseButton(e))
                {
                    return;
                }
                
                TreePath[] min = tree.getSelectionModel().getSelectionPaths();
                
                if (min == null || min.length == 0)
                {
                    return;
                }
                
                List<DataPair<ByteArray>> data = new ArrayList<>();
                
                for (TreePath treePath : min)
                {
                    DefaultMutableTreeNode element = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                    DataPair               elem    = (DataPair) element.getUserObject();
                    
                    if (elem.getContent() instanceof ByteArray)
                    {
                        data.add((DataPair<ByteArray>) elem);
                    }
                }
                
                showSaveDialog(data, e.getComponent(), e.getX(), e.getY());
            }
        });
        
        
        addBaseNodes(top);
        
        
        JScrollPane treePane = new JScrollPane(tree);
        JSplitPane  view     = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePane, contentPane);
        view.setResizeWeight(0.6);
        view.getLeftComponent().setMinimumSize(new Dimension(400, 600));
        
        JFrame frame = new JFrame("LoL-Parser");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().add(view);
        frame.pack();
        frame.setVisible(true);
    }
    
    private void showSaveDialog(List<DataPair<ByteArray>> data, Component invoker, int x, int y)
    {
        
        JPopupMenu menu = new JPopupMenu();
        JMenuItem  save = new JMenuItem("Save as...");
        menu.add(save);
        save.addActionListener(ev -> {
            try
            {
                Map<File, ByteArray> files = new HashMap<>();
                data.forEach(f -> {
                    String filename = f.getName();
                    if (filename.contains("/"))
                    {
                        filename = filename.substring(filename.lastIndexOf("/"));
                    }
                    String newName = UtilHandler.replaceEnding(filename, "dds", "png");
                    files.put(new File(newName), f.getContent());
                });
                
                JFileChooser saveDialog = new JFileChooser();
                saveDialog.setSelectedFiles(files.keySet().toArray(new File[0]));
                
                int option = saveDialog.showSaveDialog(null);
                if (option == JFileChooser.APPROVE_OPTION)
                {
                    String filePath  = saveDialog.getSelectedFile().toString();
                    Path   parent    = Paths.get(filePath.substring(0, filePath.lastIndexOf("\\")));
                    File[] fileSaves = saveDialog.getSelectedFiles();
                    for (File file : fileSaves)
                    {
                        if (FileTypeHandler.isImageFormat(file.getName()))
                        {
                            try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ByteArrayInputStream bis = new ByteArrayInputStream(files.get(file).getDataRaw()))
                            {
                                BufferedImage image = ImageIO.read(bis);
                                ImageIO.write(image, "png", bos);
                                Files.write(parent.resolve(file.getName()), bos.toByteArray());
                            }
                        } else
                        {
                            Files.write(parent.resolve(file.getName()), files.get(file).getDataRaw());
                        }
                    }
                }
                
            } catch (IOException e1)
            {
                e1.printStackTrace();
            }
        });
        
        menu.show(invoker, x, y);
    }
    
    
    private void addBaseNodes(DefaultMutableTreeNode top)
    {
        Path         baseFolder = null;
        JFileChooser chooser    = new JFileChooser("C:/Riot Games");
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
        
        try
        {
            Files.walkFileTree(baseFolder, new SimpleFileVisitor<>()
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
    
    @SuppressWarnings("unchecked")
    private void add(Path file, DefaultMutableTreeNode parent)
    {
        String                 path = extractParentFileName(file);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new DataPair(path, file));
        parent.add(node);
    }
    
    Map<Path, List<DataPair>> parsed = new HashMap<>();
    
    @SuppressWarnings("unchecked")
    private List<DataPair> getContent(Path path)
    {
        if (parsed.containsKey(path))
        {
            return parsed.get(path);
        }
        
        String         name    = path.getFileName().toString();
        List<DataPair> content = new ArrayList<>();
        
        if (name.endsWith(".wad") || name.endsWith(".wad.client"))
        {
            WADParser parser = new WADParser();
            WADFile   file   = parser.parse(path);
            
            for (WADContentHeaderV1 header : file.getContentHeaders())
            {
                String filename = HashHandler.getWadHash(header.getPathHash());
                byte[] data     = file.readContentFromHeaderData(header);
                if (filename.equals(header.getPathHash()))
                {
                    String type = FileTypeHandler.findFileType(new ByteArray(data));
                    filename = "unknown/" + filename + "." + type;
                }
                
                if (header.getCompressionType() != WADCompressionType.REFERENCE)
                {
                    content.add(new DataPair(filename, new ByteArray(data)));
                }
            }
        } else if (name.endsWith(".raf"))
        {
            RAFParser parser = new RAFParser();
            RAFFile   file   = parser.parse(path);
            
            for (RAFContentFile contentFile : file.getFiles())
            {
                String filename = file.getStrings().get(contentFile.getPathIndex());
                byte[] data     = file.readContentFromData(contentFile);
                
                content.add(new DataPair(filename, new ByteArray(data)));
            }
        }
        
        Collections.sort(content);
        parsed.put(path, content);
        return content;
    }
    
    @SuppressWarnings("unchecked")
    private void valueChangedListener(TreeSelectionEvent e)
    {
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
                
                if (FileTypeHandler.isImageFormat(filename))
                {
                    try
                    {
                        ByteArray            bContent = data.getContent();
                        ByteArrayInputStream is       = new ByteArrayInputStream(bContent.getDataRaw());
                        BufferedImage        image    = ImageIO.read(is);
                        JLabel               label    = new JLabel(new ImageIcon(image));
                        contentPane.setViewportView(label);
                    } catch (IOException e1)
                    {
                        e1.printStackTrace();
                    }
                } else if (FileTypeHandler.isTextFormat(filename))
                {
                    ByteArray bContent = data.getContent();
                    JTextArea label    = new JTextArea(new String(data.getContent().getDataRaw(), StandardCharsets.UTF_8));
                    label.setEditable(false);
                    contentPane.setViewportView(label);
                } else if (filename.endsWith(".bin"))
                {
                    ByteArray bContent = data.getContent();
                    String    datum    = new BINParser().parse(bContent).toJson();
                    byte[]    datb     = FileTypeHandler.makePrettyJson(datum.getBytes(StandardCharsets.UTF_8));
                    JTextArea label    = new JTextArea(new String(datb, StandardCharsets.UTF_8));
                    label.setEditable(false);
                    contentPane.setViewportView(label);
                }
            }
        }
    }
    
    class DataPair<T> implements Comparable<DataPair<T>>
    {
        private String name;
        private T      content;
        
        public DataPair(String name, T content)
        {
            this.name = name;
            this.content = content;
        }
        
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
    
            return comparator.compare(name, o.name);
        }
        
        public String getName()
        {
            return name;
        }
        
        public void setName(String name)
        {
            this.name = name;
        }
        
        public T getContent()
        {
            return content;
        }
        
        public void setContent(T content)
        {
            this.content = content;
        }
        
        @Override
        public String toString()
        {
            return name;
        }
    }
    
}

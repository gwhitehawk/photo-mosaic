import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
* App to choose photos for the photo-mosaic with tiling 
**/

/** Define allowed image file extensions. **/
interface ExtConstants {
    public static final String[] IMAGE_EXTENSIONS = { ".tiff", ".tif", ".gif    ", ".jpeg", ".jpg" };
}

/** Define image filter. **/
class ImageFilter extends javax.swing.filechooser.FileFilter
{
    public boolean accept(File f) {
        boolean accepted = false;
        
        if (f.isDirectory()) {
            return true;
        }
        
        for (int i = 0; i < ExtConstants.IMAGE_EXTENSIONS.length; i++)
             accepted = accepted || f.getName().toLowerCase().endsWith(ExtConstants.IMAGE_EXTENSIONS[i]);

        return accepted;
    }   

    public String getDescription () {
        return "Image files";
  }
} // class ImageFilter

/** Checkbox wrapper. **/
class CheckBoxData {
    public JCheckBox checkBoxName;
    public JLabel checkBoxThumb;

    public CheckBoxData(JCheckBox checkBoxName, JLabel checkBoxThumb) {
        this.checkBoxName = checkBoxName;
        this.checkBoxThumb = checkBoxThumb;
    }
}
/** Selected photos checkbox list. **/
class CheckBoxList extends JPanel {
    public GridBagConstraints c;
    public int rowCount;

    public CheckBoxList() {
        setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipadx = 10;
        c.ipady = 5;
        rowCount = 0;
    }

    public void addCheckBox(File image) {
        JCheckBox imageName = new JCheckBox(image.getName());
        imageName.setSelected(true);

        ImageIcon icon = new ImageIcon(image.getPath());
        Image img = icon.getImage();  
        Image thumb = img.getScaledInstance(100, 67,  java.awt.Image.SCALE_SMOOTH);  
        ImageIcon thumbIcon = new ImageIcon(thumb);  
        JLabel imageThumb = new JLabel(thumbIcon);
       
        c.gridx = 0;
        c.gridy = rowCount;
        this.add(imageName, c);
        c.gridx = 1;
        this.add(imageThumb, c);
        rowCount++;
    }
}

/** Main class. **/
public class Mosaic extends JFrame
       implements ActionListener
{
    JMenuItem fMenuOpen = null;
    JMenuItem fMenuSave  = null;
    JMenuItem fMenuClose = null;

    //JTextArea checkBoxArea;
    CheckBoxList checkBoxArea;
    JTextField layoutField;
    JTextField widthField;
    JTextField borderField;

    ImageFilter fImageFilter = new ImageFilter();
    File fFile = new File ("default");
    static String sourceFile = "source.txt";
    static String paramFile = "param.txt";
    
    ArrayList<String> photoList = new ArrayList<String>();

    /** Routines: printToFile. **/
    static boolean printToFile(String data, String filename, boolean append) {
        try {
            FileOutputStream out = new FileOutputStream(filename, append);
            PrintStream pPrint = new PrintStream(out);
            pPrint.println(data);
            pPrint.close();
        }   
        catch (FileNotFoundException e) {
            return false;
        }   

        return true;
    }   

    /** Create a frame with param text fields, editable 
    *   checkbox list, and "File" dropdown menu
    **/
    Mosaic(String title) {
        super(title);

        Container content_pane = getContentPane();

        // Create a user interface.
        
        // Components
        checkBoxArea = new CheckBoxList();
        JScrollPane checkScroll = new JScrollPane(checkBoxArea); 
        layoutField = new JTextField(50);
        widthField = new JTextField(50);
        borderField = new JTextField(50);

        JLabel layoutLabel = new JLabel("Layout:");
        JLabel widthLabel = new JLabel("Mosaic Width:");
        JLabel borderLabel = new JLabel("Border:");
 
        // Layout
        GroupLayout grpLayout = new GroupLayout(content_pane);
        content_pane.setLayout(grpLayout);

        grpLayout.setAutoCreateGaps(true);
        grpLayout.setAutoCreateContainerGaps(true);
        
        grpLayout.setHorizontalGroup(
            grpLayout.createSequentialGroup()
                .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(grpLayout.createSequentialGroup()
                        .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(layoutLabel)
                            .addComponent(widthLabel)
                            .addComponent(borderLabel)
                        )
                        .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(layoutField)
                            .addComponent(widthField)
                            .addComponent(borderField)
                        )
                    )
                    .addComponent(checkScroll)
                )
        );
        
        grpLayout.setVerticalGroup(
            grpLayout.createSequentialGroup()
                .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(layoutLabel)
                    .addComponent(layoutField)
                )
                .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(widthLabel)
                    .addComponent(widthField)
                )
                .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(borderLabel)
                    .addComponent(borderField)
                )
                .addComponent(checkScroll)
        );
    
        // Use the helper method makeMenuItem
        // for making the menu items and registering
        // their listener.
        JMenu m = new JMenu("File");

        // Modify task names to something relevant to
        // the particular program.
        m.add(fMenuOpen  = makeMenuItem("Add"));
        m.add(fMenuClose = makeMenuItem("Apply"));

        JMenuBar mb = new JMenuBar();
        mb.add(m);

        setJMenuBar(mb);
        setSize(500,500);
    } // constructor

    /** Process events from the chooser. **/
    public void actionPerformed(ActionEvent e ) {
        boolean status = false;

        String command = e.getActionCommand();
        if  (command.equals("Add")) {
        // Add an image file
            status = addFile();
            if (!status)
            JOptionPane.showMessageDialog (
                null,
                "Error adding file!", 
                "File Open Error",
                JOptionPane.ERROR_MESSAGE
            );

        } else if (command.equals("Apply")) {
            status = saveParams();
            dispose();
        }
    } // actionPerformed

    /** This "helper method" makes a menu item and then
    * registers this object as a listener to it.
    **/
    private JMenuItem makeMenuItem(String name) {
        JMenuItem m = new JMenuItem(name);
        m.addActionListener(this);
        return m;
    } // makeMenuItem

    /** Creates arraylist of mosaic pictures. **/
    boolean addFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Add File");

        // Choose only files, not directories
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // Start in current directory
        if (photoList.isEmpty())
            fc.setCurrentDirectory(new File("."));
        else 
            fc.setCurrentDirectory(new File(photoList.get(photoList.size()-1)));

        // Set filter for Java source files.
        fc.setFileFilter(fImageFilter);

        // Now open chooser
        int result = fc.showOpenDialog(this);

        if (result == JFileChooser.CANCEL_OPTION) {
            return true;
        } else if (result == JFileChooser.APPROVE_OPTION) {
            checkBoxArea.setVisible(false);
            fFile = fc.getSelectedFile();
            photoList.add(fFile.getPath());
            checkBoxArea.addCheckBox(fFile);
            checkBoxArea.setVisible(true);
        } else {
            return false;
        }
        return true;
    } // addFile

    /** Save layout into a pic source file;  width and border into a text file. **/
    boolean saveParams() {
        String layout = layoutField.getText();
        String width =  widthField.getText();
        String border = borderField.getText();
        Component[] components = checkBoxArea.getComponents();

        for (int i = components.length-2; i >= 0; i=i-2) {
            if (components[i] instanceof JCheckBox && ! ((JCheckBox)components[i]).isSelected()) 
                photoList.remove(i);
        }

        boolean success;
        int rowIndex = 0;
        String tempBuffer = "";
        
        String[] splitRows = layout.split(" ");
        
        int rowCount = Integer.parseInt(splitRows[rowIndex]);
        for (int i = 0; i < photoList.size(); i++) 
        {
            if (rowCount - i > 0) {
                tempBuffer += " " + photoList.get(i);
            } else if (splitRows.length > rowIndex + 1) {
                rowIndex++;
                rowCount += Integer.parseInt(splitRows[rowIndex]);
                tempBuffer += "\n" + photoList.get(i);
            }      
        }

        printToFile(tempBuffer, sourceFile, true);

        success = printToFile(width, paramFile, false);
        success = success && printToFile(border, paramFile, true);
        
        return success;
    } // saveParams

    /** Create the framed application and show it. **/
    public static void main (String [] args) {
        // Test whether there is a mosaic source file. If so, delete
        File file = new File(sourceFile);
        
        if (file.exists())
            file.delete();
        
        // Can pass frame title in command line arguments
        String title="Mosaic";
        Mosaic f = new Mosaic(title);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setVisible (true);
    } // main

}// class Mosaic

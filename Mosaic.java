import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.Math;

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
        c.ipadx = 20;
        c.ipady = 5;
        rowCount = 0;
    }

    public void addCheckBox(File image) {
        JCheckBox imageName = new JCheckBox(image.getName());
        imageName.setSelected(true);

        ImageIcon icon = new ImageIcon(image.getPath());
        Image img = icon.getImage();  
        Image thumb = img.getScaledInstance(100, 67,  java.awt.Image.SCALE_FAST);  
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
    JMenuItem fMenuApply  = null;
    JMenuItem fMenuClose = null;

    JCheckBox autoLayout;
    CheckBoxList checkBoxArea;
    JScrollPane checkScroll;
    JTextField layoutField;
    JTextField widthField;
    JTextField borderField;

    JLabel layoutLabel;
    JLabel widthLabel;
    JLabel borderLabel;

    ImageFilter fImageFilter = new ImageFilter();
    File fFile = new File ("default");
    static String sourceFile = "source.txt";
    static String paramFile = "param.txt";
    
    ArrayList<String> photoList = new ArrayList<String>();

    /** Routines: printToFile. **/
    private static boolean printToFile(String data, String filename, boolean append) {
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
    
    /** Layout. **/
    private GroupLayout layout(Container contentPane) {
        GroupLayout grpLayout = new GroupLayout(contentPane);

        grpLayout.setAutoCreateGaps(true);
        grpLayout.setAutoCreateContainerGaps(true);

        grpLayout.setHorizontalGroup(
            grpLayout.createSequentialGroup()
                .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(autoLayout)
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
                .addComponent(autoLayout)
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

        return grpLayout;
    }

    /** Create a frame with param text fields, editable 
    *   checkbox list, and "File" dropdown menu
    **/
    Mosaic(String title) {
        super(title);

        Container contentPane = getContentPane();

        // Create a user interface.
        
        // Components
        autoLayout = new JCheckBox("Auto Layout");
        autoLayout.setToolTipText("For n photos, creates a ~sqrt(n) x ~sqrt(n) grid.");

        checkBoxArea = new CheckBoxList();
        checkScroll = new JScrollPane(checkBoxArea); 
        
        layoutField = new JTextField(100);
        layoutField.setToolTipText("#Pic in each row, separated by space.");

        widthField = new JTextField(50);
        borderField = new JTextField(50);

        layoutLabel = new JLabel("Layout:");
        widthLabel = new JLabel("Mosaic Width:");
        borderLabel = new JLabel("Border:");
 
        contentPane.setLayout(layout(contentPane));

        // Use the helper method makeMenuItem
        // for making the menu items and registering
        // their listener.
        JMenu m = new JMenu("File");

        // Modify task names to something relevant to
        // the particular program.
        m.add(fMenuOpen  = makeMenuItem("Add"));
        m.add(fMenuApply = makeMenuItem("Apply"));
        m.add(fMenuClose = makeMenuItem("Quit"));

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
        } else if (command.equals("Quit")) {
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

    /** Open file chooser. **/
    private JFileChooser fileChooserDialog(String title, String currentDir) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);

        // Choose only files, not directories
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        fc.setCurrentDirectory(new File(currentDir));
        
        // Set filter for Java source files.
        fc.setFileFilter(fImageFilter);
        return fc;
    }

    /** Creates arraylist of mosaic pictures. **/
    boolean addFile() {
        String currentDir;
        //Start in last accessed directory
        if (photoList.isEmpty())
            currentDir = ".";
        else
            currentDir = photoList.get(photoList.size()-1);

        JFileChooser fc = fileChooserDialog("Add File", currentDir);

        // Now open chooser
        int result = fc.showOpenDialog(this);

        if (result == JFileChooser.CANCEL_OPTION) {
            return true;
        } else if (result == JFileChooser.APPROVE_OPTION) {
            checkBoxArea.setVisible(false);
            fFile = fc.getSelectedFile();
            photoList.add(fFile.getPath());
            checkBoxArea.addCheckBox(fFile);
            int height = (int)checkBoxArea.getPreferredSize().getHeight();
            checkScroll.getVerticalScrollBar().setValue(height);
            
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
        int startPhotoNum = photoList.size();
        
        for (int i = startPhotoNum - 1; i >= 0; i--) {
            if (components[2*i] instanceof JCheckBox && ! ((JCheckBox)components[2*i]).isSelected())
                photoList.remove(i);
        }

        boolean success;
        
        int rowIndex = 0;
        String tempBuffer = "";
        
        if (autoLayout.isSelected()) {
            int imgNumber = photoList.size();
            int rowNumber = (int)Math.round(Math.sqrt(imgNumber));
            int colNumber = imgNumber/rowNumber;
            
            int[] layoutArr = new int[rowNumber];
            for (int i = 0; i < rowNumber; i++) 
                layoutArr[i] = colNumber;

            int imgToPlace = imgNumber - rowNumber * colNumber;
            
            Random generator = new Random();
            for (int i = 0; i < imgToPlace; i++) {
                int newIndex = generator.nextInt(rowNumber);
                layoutArr[newIndex]++;
            }
            
            layout = "";
            for (int i = 0; i < rowNumber; i++)
                layout += " " + layoutArr[i];
            
            layout = layout.trim(); 
        }
        
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

        success = printToFile(tempBuffer, sourceFile, true);

        success = success &&printToFile(width, paramFile, false);
        success = success && printToFile(border, paramFile, true);
       
        JFileChooser fc = fileChooserDialog("Save As", ".");

        // Open chooser
        int result = fc.showSaveDialog(this);

        if (result  == JFileChooser.CANCEL_OPTION) {
            return true;        
        } else if (result == JFileChooser.APPROVE_OPTION) { 
            File targetFile = fc.getSelectedFile();
            success = success && printToFile(targetFile.getPath(), paramFile, true);
        } else {
            return false;
        }

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

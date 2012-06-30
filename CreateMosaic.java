//backend logic including image magick 
//Image data: Dimension(width, height) 
//Image layout: Dimension[][] 

import java.lang.*;
import java.util.*; 
import java.io.*;

import java.awt.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import javax.imageio.ImageIO;

import java.lang.Math;

/* ====================================================
** Backend
** ====================================================
*/

//helper class ImageData 
class ImageData {
    String title;
    Dimension measures = null;
    double scaleFactor = 1;
   
    public ImageData(String title, Dimension measures) {
        this.title = title;
        this.measures = measures;
    }
}

class Mosaic {
    //const
    public final int MAX = 15;

    //fields
    public int[] layout;
    public int side;
    public int border;
    public boolean byRows;
    public ArrayList<String> imageArray;

    //constructor, input parsing
    public Mosaic(String layoutStr, String sideStr, 
        String borderStr, boolean byRows, ArrayList<String> imageArray) {
        String[] temp = layoutStr.split(" ");
        int[] layout = new int[temp.length];
        for (int i = 0; i < temp.length; i++) {
            layout[i] = Integer.parseInt(temp[i]);
        }
        this.layout = layout;

        this.side = Integer.parseInt(sideStr);
        this.border = 2*(Integer.parseInt(borderStr)/2); 
        this.byRows = byRows;
        this.imageArray = imageArray;
    }

    //getSizes
    public ArrayList<ImageData> getSizes() {
        BufferedImage readImage = null;
        ArrayList<ImageData> imgWithSize = new ArrayList<ImageData>();

        for (String img : imageArray) {
            try {
                readImage = ImageIO.read(new File(img));
                int height = readImage.getHeight();
                int width = readImage.getWidth();
                imgWithSize.add(new ImageData(img, new Dimension(width, height)));
            } catch (Exception e) {
            }
        }

        return imgWithSize;
    }

    //makeMatrix
    public ImageData[][] makeMatrix() {
        //enhance the photo file-name list with sizes
        ArrayList<ImageData> imgWithSize = getSizes();
        ImageData[][] matrix = new ImageData[MAX][MAX];
        
        int rowIndex = 0;
        int imgCount = 0;

        int imgNumber = imgWithSize.size();

        for (int row : layout) {
            for (int j = 0; j < row; j++) {
                if (j+imgCount >= imgNumber) 
                    break;
                matrix[rowIndex][j] = imgWithSize.get(j+imgCount);  
            }
            imgCount += row;
            if (imgCount >= imgNumber)
                break;
            rowIndex++;
        }

        return matrix;
    }

    //helper method to retrieve width or height 
    //according to aligning
    private int[] getMeasures(ImageData input, boolean byRows) {
        int[] result = new int[2];

        if (byRows) {
            result[0] = input.measures.width;
            result[1] = input.measures.height;
        } else {
            result[0] = input.measures.height;
            result[1] = input.measures.width;
        }

        return result;
    }

    //getScaleFactors 
    public ImageData[][] getScaleFactors() {
        ImageData[][] matrix = makeMatrix();
        int rowIndex = 0; 

        for (ImageData[] row : matrix) {
            if (row[0] == null)
                break;

            int sideSum = 0;
            int sideCount = 0;

            int[] normSide = new int[MAX];

            for (ImageData cell : row) {
                if (cell == null)
                    break;
                
                int[] ordMeasures = getMeasures(cell, byRows);
                int newSide = 1000*ordMeasures[0]/ordMeasures[1];
                normSide[sideCount] = newSide;
                sideSum += newSide;
                sideCount++;
            }

            double sideCoeff = (double) (side - border*(sideCount+1))/(double) sideSum;
            
            for (int i = 0; i < sideCount; i++) {
                double coeff = (double)(normSide[i]*sideCoeff)/(double)getMeasures(row[i], byRows)[0];
                row[i].scaleFactor = coeff;
            }
        }

        return matrix;
    }
    
    // helper method execProcess
    private void execProcess(String command) {
        try {
            Process p = Runtime.getRuntime().exec(command);
            try { 
                p.waitFor();
            } catch (InterruptedException e) {
                System.out.println("Process interrupted.");
            }   
        } catch (IOException e) {
        }   
    }

    //imageMagick code
    public void saveMosaic(String targetFile) {
        ImageData[][] result = getScaleFactors();
        int end_index = 0;
        int start_index;
        int row_index = 0;
        String command;
        String tiling = "x1";
        String tiling2 = "1x";

        File theDir = new File("img");
        if (!theDir.exists()) 
            theDir.mkdir();
        
        if (!byRows) {
            tiling = "1x";
            tiling2 = "x1";
        }
        
        for (ImageData[] row : result) {
            if (row[0] == null)
                break;
            
            start_index = end_index;
            for (ImageData cell : row) {
                if (cell == null) 
                    break;
                
                command = String.format("convert %s -resize %.2f%% img/pic_%d.jpg\n", cell.title, 100*cell.scaleFactor, end_index);
                execProcess(command);
                end_index++;
            }

            command = String.format("montage img/pic_%s.jpg -mode Concatenate -gravity center -border %d -bordercolor White -tile %s -geometry +0+0 img/row_%d.jpg\n", String.format("[%d-%d]", start_index, end_index-1), border/2, tiling, row_index);
            execProcess(command); 
            row_index++;
        }
        
        
        command = String.format("montage img/row_%s.jpg -tile %s -geometry +0+0 %s\n", String.format("[0-%d]", row_index-1), tiling2, targetFile);
        execProcess(command);    
        command = String.format("convert %s -bordercolor White -border %d %s\n", targetFile, border/2, targetFile);
        execProcess(command);
        System.out.println("Mosaic created.");
        
        File f;

        for (int i = 0; i < end_index; i++) {
            f = new File(String.format("img/pic_%d.jpg", i));
            f.delete();
        }

        for (int i = 0; i < row_index; i++) {
            f = new File(String.format("img/row_%d.jpg", i));
            f.delete();
        }
    }
}

/* ====================================================
** GUI
** ====================================================
*/

/** Define allowed image file extensions. **/
interface ExtConstants {
    public static final String[] IMAGE_EXTENSIONS = { ".tiff", ".tif", ".gif    ", ".jpeg", ".jpg" };
} // interface ExtConstants

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
} // class CheckBoxData

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
        Image thumb = img.getScaledInstance(75, 75,  java.awt.Image.SCALE_FAST);
        ImageIcon thumbIcon = new ImageIcon(thumb);
        JLabel imageThumb = new JLabel(thumbIcon);

        c.gridx = 0;
        c.gridy = rowCount;
        this.add(imageName, c);
        c.gridx = 1;
        this.add(imageThumb, c);
        rowCount++;
    }
} // class CheckBoxList

/** Main class. **/
public class CreateMosaic extends JFrame
       implements ActionListener
{
    JMenuItem fMenuOpen = null;
    JMenuItem fMenuApply  = null;
    JMenuItem fMenuClose = null;

    JCheckBox autoLayout;
    JCheckBox byColumns;

    CheckBoxList checkBoxArea;
    JScrollPane checkScroll;

    JTextField layoutField;
    JTextField sizeField;
    JTextField borderField;

    JLabel layoutLabel;
    JLabel sizeLabel;
    JLabel borderLabel;

    ImageFilter fImageFilter = new ImageFilter();
    File fFile = new File ("default");

    ArrayList<String> photoList = new ArrayList<String>();

    // Layout
    private GroupLayout layout(Container contentPane) {
        GroupLayout grpLayout = new GroupLayout(contentPane);

        grpLayout.setAutoCreateGaps(true);
        grpLayout.setAutoCreateContainerGaps(true);

        grpLayout.setHorizontalGroup(
            grpLayout.createSequentialGroup()
                .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(grpLayout.createSequentialGroup()
                        .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(autoLayout)
                        )
                        .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(byColumns)
                        )
                    )
                    .addGroup(grpLayout.createSequentialGroup()
                        .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(layoutLabel)
                            .addComponent(sizeLabel)
                            .addComponent(borderLabel)
                        )
                        .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(layoutField)
                            .addComponent(sizeField)
                            .addComponent(borderField)
                        )
                    )
                    .addComponent(checkScroll)
                )
        );

        grpLayout.setVerticalGroup(
            grpLayout.createSequentialGroup()
                .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(autoLayout)
                    .addComponent(byColumns)
                )
                .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(layoutLabel)
                    .addComponent(layoutField)
                )
                .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(sizeLabel)
                    .addComponent(sizeField)
                )
                .addGroup(grpLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(borderLabel)
                    .addComponent(borderField)
                )
                .addComponent(checkScroll)
        );

        return grpLayout;
    }

    // Constructor creates a frame with param text fields, editable 
    // checkbox list, and "File" dropdown menu
    CreateMosaic(String title) {
        super(title);

        Container contentPane = getContentPane();

        // Create a user interface.

        // Components
        autoLayout = new JCheckBox("Auto Layout");
        autoLayout.setToolTipText("For n photos, creates a ~sqrt(n) x ~sqrt(n) grid.");

        byColumns = new JCheckBox("By Columns");
        byColumns.setToolTipText("If checked, aligns photos in columns, else by rows.");

        checkBoxArea = new CheckBoxList();
        checkScroll = new JScrollPane(checkBoxArea);

        layoutField = new JTextField(100);
        layoutField.setToolTipText("#Pic in each row, separated by space.");

        sizeField = new JTextField(50);
        sizeField.setToolTipText("If by columns, set height, else set width.");
        borderField = new JTextField(50);

        layoutLabel = new JLabel("Layout:");
        sizeLabel = new JLabel("Side:");
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

    // Process events from the chooser. 
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
            generateMosaic();
        } else if (command.equals("Quit")) {
            dispose();
        }
    } // actionPerformed

    // Helper method makeMenuItem makes a menu item and then
    // registers this object as a listener to it.
    private JMenuItem makeMenuItem(String name) {
        JMenuItem m = new JMenuItem(name);
        m.addActionListener(this);
        return m;
    } // makeMenuItem

    // Open file chooser
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

    // Create arraylist of mosaic pictures
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

    // Generate mosaic
    void generateMosaic() {
        String layoutStr = layoutField.getText();
        String sideStr =  sizeField.getText();
        String borderStr = borderField.getText();
        Component[] components = checkBoxArea.getComponents();
        int startPhotoNum = photoList.size();

        for (int i = startPhotoNum - 1; i >= 0; i--) {
            if (components[2*i] instanceof JCheckBox && ! ((JCheckBox)components[2*i]).isSelected())
                photoList.remove(i);
        }

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

            layoutStr = "";
            for (int i = 0; i < rowNumber; i++)
                layoutStr += " " + layoutArr[i];

            layoutStr = layoutStr.trim();
        }
        
        boolean byRows = true;
        if (byColumns.isSelected()) 
            byRows = false;
            
        Mosaic newMosaic = new Mosaic(layoutStr, sideStr, borderStr, byRows, photoList);

        JFileChooser fc = fileChooserDialog("Save As", ".");

        // Open chooser
        int result = fc.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File targetFile = fc.getSelectedFile();
            newMosaic.saveMosaic(targetFile.getPath());
        } 
    } // generateMosaic

    // Create the framed application and show it
    public static void main (String [] args) {
        // Can pass frame title in command line arguments
        String title="Mosaic";
        CreateMosaic f = new CreateMosaic(title);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setVisible (true);
    } // main

} // class CreateMosaic

/*public class CreateMosaic {
    public static void main(String[] args) {
        ArrayList<String> imgs = new ArrayList<String>();
        String layoutStr = "1 1";
        String widthStr = "200";
        String borderStr = "2";
        boolean byRows = true;

        String testImg = "/Users/miroslavasotakova/programming/Python/Mosaic/./test.jpg";
        imgs.add(testImg);
        imgs.add(testImg);

        Mosaic newMosaic = new Mosaic(layoutStr, widthStr, borderStr, byRows, imgs);
        newMosaic.saveMosaic();
    }
}*/

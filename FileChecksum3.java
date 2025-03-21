/*
  File Checksum #3 - Compute CRC32, MD5, SHA File Checksums
  Written by: Keith Fenske, http://kwfenske.github.io/
  Thursday, 30 October 2008
  Java class name: FileChecksum3
  Copyright (c) 2008 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 application to compute common checksums for files: CRC32,
  MD5, SHA1, and optional SHA256 or SHA512.  Checksums are small hexadecimal
  "signatures" for testing whether or not files have been copied correctly,
  such as over a network.  One person sends a file along with the checksum
  computed on the original computer.  A second person calculates a similar
  checksum for the received file, and if the two checksums agree, then the
  received file is assumed to be correct.  MD5 is more reliable than and
  preferred over the older and simpler CRC32.  Many web sites provide MD5
  signatures for their downloads; use this program to verify files that you
  download.

  SHA256 and SHA512 checksums are noticeably slower than MD5 or SHA1 and must
  be selected before a file is read.  See the CompareFolders Java application
  for comparing two folders to determine if all files and subfolders are
  identical.  See FindDupFiles to look for duplicate files by size and MD5
  checksum.

  Apache License or GNU General Public License
  --------------------------------------------
  FileChecksum3 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain parameters for a file name and optional
  checksums.  If no parameters are given on the command line, then this program
  runs as a graphical or "GUI" application with the usual dialog boxes and
  windows.  See the "-?" option for a help summary:

      java  FileChecksum3  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.  If parameters are given on the command line, then
  this program runs as a console application without a graphical interface.
  The first parameter must be a file name.  Checksums are calculated for that
  file.  A generated report is written on standard output, and may be
  redirected with the ">" or "1>" operators.  (Standard error may be redirected
  with the "2>" operator.)  An example command line is:

      java  FileChecksum3  README.TXT  >output.txt

  This will calculate checksums for a file named "README.TXT".  Standard output
  will be written to a file named "output.txt".  The second and following
  parameters, if given, must be hexadecimal checksums.  For example:

      java  FileChecksum3  README.TXT  d36952838c47c701745293e1a16333f3

  Second and following parameters are compared against the generated checksums
  (CRC32, MD5, SHA1).  If each parameter matches a checksum, then the result is
  considered successful.  The console application will return an exit status of
  1 for success, -1 for failure, and 0 for unknown.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.security.*;           // MD5 and SHA1 message digests (checksums)
import java.text.*;               // number formatting
import java.util.regex.*;         // regular expressions
import java.util.zip.*;           // CRC32 checksums
import javax.swing.*;             // newer Java GUI support

public class FileChecksum3
{
  /* constants */

  static final int BUFFER_DEFAULT = 0x10000;
                                  // default input buffer size in bytes (64 KB)
  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2008 by Keith Fenske.  Apache License or GNU GPL.";
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
  static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final String HELLO_TEXT =
    "Open a file to compute checksums, or compare against known checksum.";
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Compute CRC32, MD5, SHA File Checksums - by: Keith Fenske";
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final int TIMER_DELAY = 400; // 0.400 seconds between timed updates
  static final String WAIT_TEXT =
    "Calculating checksums.  Please wait or click the Cancel button.";

  /* class variables */

  static int bufferSize;          // default or chosen input buffer size
  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static JLabel compareLabel;     // dialog label for comparison text from user
  static JTextField compareText;  // text field for comparison text from user
  static boolean consoleFlag;     // true if running as a console application
  static JButton copyCrc32Button; // "Copy CRC32" checksum button
  static JButton copyMd5Button;   // "Copy MD5" checksum button
  static JButton copySha1Button;  // "Copy SHA1" checksum button
  static JButton copySha256Button; // "Copy SHA256" checksum button
  static JButton copySha512Button; // "Copy SHA512" checksum button
  static JLabel crc32Label;       // dialog label for CRC32 checksum
  static String crc32String;      // calculated CRC32 checksum
  static JTextField crc32Text;    // graphical text box for <crc32String>
  static boolean debugFlag;       // true if we show debug information
  static JButton exitButton;      // "Exit" button
  static JFileChooser fileChooser; // asks for input and output file names
  static JButton filenameButton;  // button for input file name
  static JTextField filenameText; // text field for input file name
  static JLabel filesizeLabel;    // dialog label for file size in bytes
  static String filesizeString;   // formatted size of file in bytes
  static JTextField filesizeText; // graphical text box for <filesizeString>
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static NumberFormat formatPointOne; // formats with one decimal digit
  static JLabel legalNotice;      // boring legal notice about copyright, etc
  static JFrame mainFrame;        // this application's window
  static JCheckBox md5Checkbox;   // graphical option for <md5Flag>
  static boolean md5Flag;         // true if we calculate/show MD5 checksum
  static String md5String;        // calculated MD5 checksum
  static JTextField md5Text;      // graphical text box for <md5String>
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static File openFileObject;     // file to be opened in a separate thread
  static Thread openFileThread;   // separate thread for openFile() method
  static JButton pasteCompareButton; // button for pasting comparison text
  static JProgressBar progressBar; // graphical display needed for big files
  static javax.swing.Timer progressTimer; // timer for updating progress text
  static JCheckBox sha1Checkbox;  // graphical option for <sha1Flag>
  static boolean sha1Flag;        // true if we calculate/show SHA1 checksum
  static String sha1String;       // calculated SHA1 checksum
  static JTextField sha1Text;     // graphical text box for <sha1String>
  static JCheckBox sha256Checkbox; // graphical option for <sha256Flag>
  static boolean sha256Flag;      // true if we calculate/show SHA256 checksum
  static String sha256String;     // calculated SHA256 checksum
  static JTextField sha256Text;   // graphical text box for <sha256String>
  static JCheckBox sha512Checkbox; // graphical option for <sha512Flag>
  static boolean sha512Flag;      // true if we calculate/show SHA512 checksum
  static String sha512String;     // calculated SHA512 checksum
  static JTextField sha512Text;   // graphical text box for <sha512String>
  static long sizeDone;           // how much of <sizeTotal> has been finished
  static String sizeSuffix;       // pre-formatted portion of size message
  static long sizeTotal;          // total number of bytes in current file
  static java.applet.AudioClip soundsBad; // sound if checksums do not agree
  static JButton startButton;     // "Start" button
  static JLabel statusText;       // text area for displaying status messages

/*
  main() method

  If we are running as a GUI application, set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    ActionListener action;        // our shared action listener
    Color buttonColor, labelColor, statusColor, textColor; // GUI colors
    Font buttonFont, labelFont, statusFont, textFont; // GUI font elements
    int exitStatus;               // exit status for console application
    int gapSize;                  // basis for pixel gap between GUI elements
    GridBagConstraints gbc;       // reuse the same constraint object
    File givenFile;               // calculate checksums for this file object
    int i;                        // index variable
    boolean maximizeFlag;         // true if we maximize our main window
    Insets textMargins;           // margins for input and output text areas
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    bufferSize = BUFFER_DEFAULT;  // default input buffer size in bytes
    buttonColor = labelColor = statusColor = textColor = null;
                                  // by default, no custom colors or fonts
    buttonFont = labelFont = statusFont = textFont = null;
    cancelFlag = false;           // don't cancel unless user complains
    consoleFlag = false;          // assume no parameters on command line
    crc32String = "";             // set CRC32 checksum to empty string
    debugFlag = false;            // by default, don't show debug information
    exitStatus = EXIT_SUCCESS;    // assume success for console application
    filesizeString = "";          // set formatted file size to empty string
    maximizeFlag = false;         // by default, don't maximize our main window
    md5Flag = true;               // by default, show MD5 checksum
    md5String = "";               // set MD5 checksum to empty string
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    sha1Flag = true;              // by default, show SHA1 checksum
    sha1String = "";              // set SHA1 checksum to empty string
    sha256Flag = false;           // by default, don't show SHA256 checksum
    sha256String = "";            // set SHA256 checksum to empty string
    sha512Flag = false;           // by default, don't show SHA512 checksum
    sha512String = "";            // set SHA512 checksum to empty string
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    formatPointOne = NumberFormat.getInstance(); // current locale
    formatPointOne.setGroupingUsed(true); // use commas or digit groups
    formatPointOne.setMaximumFractionDigits(1); // force one decimal digit
    formatPointOne.setMinimumFractionDigits(1);

    /* Check command-line parameters for options.  Anything we don't recognize
    as an option is assumed to be a file name or checksum. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
      }

      else if (word.startsWith("-b") || (mswinFlag && word.startsWith("/b")))
      {
        /* This is an advanced option to set the input buffer size when reading
        files.  The default size is good for most situations.  Some unusual
        cases may benefit from larger or smaller buffers, such as when reading
        from low-speed devices or devices with long latency (CD/DVD).  We limit
        the overall range of the size, but we do not force it to be a power of
        two.  CAREFUL TESTING IS STRONGLY RECOMMENDED. */

        long size = -1;           // default value for buffer size in bytes
        Pattern pattern = Pattern.compile("(\\d{1,9})(|b|k|kb|kib|m|mb|mib)");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          size = Long.parseLong(matcher.group(1)); // numeric part of size
          String suffix = matcher.group(2); // what was given after numbers
          if ((suffix == null) || (suffix.length() == 0) || suffix.equals("b"))
            { /* do nothing: accept number as a size in bytes */ }
          else if (suffix.startsWith("k")) // if "K" or "KB" suffix given
            size *= 0x400;        // multiply by kilobytes
          else                    // otherwise, assume "M" or "MB" suffix
            size *= 0x100000;     // multiply by megabytes
        }
        else                      // bad syntax or too many digits
        {
          size = -1;              // set result to an illegal value
        }

        /* We accept buffer sizes smaller and larger than the documented range,
        so that die-hard experimenters have something to play with.  Java 1.4
        on Windows 2000/XP starts to freeze at sizes bigger than 64 MB (for the
        buffer size in this program), and the standard -Xmx heap size of around
        60 MB allows us only one buffer of 32 MB. */

        if ((size < 0x100) || (size > 0x4000000)) // 256 bytes to 64 megabytes
        {
          System.err.println("Buffer size must be from 4KB to 16MB: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        bufferSize = (int) size;  // user's choice becomes input buffer size
      }

      else if (word.equals("-d") || (mswinFlag && word.equals("/d")))
      {
        debugFlag = true;         // show debug information
        System.err.println("main args.length = " + args.length);
        for (int k = 0; k < args.length; k ++)
          System.err.println("main args[" + k + "] = <" + args[k] + ">");
      }

      else if (word.equals("-md5") || (mswinFlag && word.equals("/md5")))
      {
        /* Turn on specific checksums, if not already on by default. */

        md5Flag = true;           // show MD5 checksum
      }
      else if (word.equals("-all") || (mswinFlag && word.equals("/all")))
        md5Flag = sha1Flag = sha256Flag = sha512Flag = true; // enable all
      else if (word.equals("-none") || (mswinFlag && word.equals("/none")))
        md5Flag = sha1Flag = sha256Flag = sha512Flag = false; // disable all
      else if (word.equals("-sha1") || (mswinFlag && word.equals("/sha1")))
        sha1Flag = true;          // show SHA1 checksum
      else if (word.equals("-sha256") || (mswinFlag && word.equals("/sha256")))
        sha256Flag = true;        // show SHA256 checksum
      else if (word.equals("-sha512") || (mswinFlag && word.equals("/sha512")))
        sha512Flag = true;        // show SHA512 checksum

      else if (word.startsWith("-u") || (mswinFlag && word.startsWith("/u")))
      {
        /* This option is followed by a font point size that will be used for
        buttons, dialogs, labels, etc. */

        int size = -1;            // default value for font point size
        try                       // try to parse remainder as unsigned integer
        {
          size = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          size = -1;              // set result to an illegal value
        }
        if ((size < 10) || (size > 99))
        {
          System.err.println("Dialog font size must be from 10 to 99: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        buttonColor = new Color(102, 102, 102); // reduce to medium gray
        buttonFont = new Font(SYSTEM_FONT, Font.PLAIN, size); // for big sizes
//      buttonFont = new Font(SYSTEM_FONT, Font.BOLD, size); // for small sizes
        labelColor = buttonColor; // no need for anything different
        labelFont = buttonFont;
        statusColor = buttonColor; // status message (top)
        statusFont = buttonFont;
        textColor = Color.BLACK;  // checksum text should be high contrast
        textFont = buttonFont;
      }

      else if (word.startsWith("-w") || (mswinFlag && word.startsWith("/w")))
      {
        /* This option is followed by a list of four numbers for the initial
        window position and size.  All values are accepted, but small heights
        or widths will later force the minimum packed size for the layout. */

        Pattern pattern = Pattern.compile(
          "\\s*\\(\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*\\)\\s*");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          windowLeft = Integer.parseInt(matcher.group(1));
          windowTop = Integer.parseInt(matcher.group(2));
          windowWidth = Integer.parseInt(matcher.group(3));
          windowHeight = Integer.parseInt(matcher.group(4));
        }
        else                      // bad syntax or too many digits
        {
          System.err.println("Invalid window position or size: " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.equals("-x") || (mswinFlag && word.equals("/x")))
        maximizeFlag = true;      // true if we maximize our main window

      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }

      else
      {
        /* Parameter does not look like an option.  Assume that this is a file
        name or checksum.  We ignore <cancelFlag> because the user has no way
        of interrupting us at this point (no graphical interface). */

        if (!consoleFlag)         // first non-option parameter is a file name
        {
          consoleFlag = true;     // don't allow GUI methods to be called
          givenFile = new File(args[i]); // convert name to Java File object
          if (givenFile.isFile() == false) // if parameter is not a real file
          {
            System.err.println("File not found: " + args[i]);
            showHelp();           // show help summary
            System.exit(EXIT_FAILURE); // exit application after printing help
          }
          System.out.println("      file name: " + givenFile.getName());
          System.out.println("     file bytes: "
            + formatComma.format(givenFile.length()));
                                  // show file name, size before start checksum
          calcFileChecksum(givenFile); // calculate checksums (may be slow)
          if (cancelFlag)         // did something go wrong?
            System.exit(EXIT_FAILURE); // exit from application with status
          System.out.println(" CRC32 checksum: " + crc32String);
          if (md5Flag) System.out.println("   MD5 checksum: " + md5String);
          if (sha1Flag) System.out.println("  SHA1 checksum: " + sha1String);
          if (sha256Flag) System.out.println("SHA256 checksum: " + sha256String);
          if (sha512Flag) System.out.println("SHA512 checksum: " + sha512String);
        }
        else                      // second and later non-options are checksums
        {
          if (compareChecksum(cleanChecksum(args[i])) != EXIT_SUCCESS)
            exitStatus = EXIT_FAILURE; // one failure means application fails
        }
      }
    }

    /* If running as a console application, exit to the system with an integer
    status for success or failure. */

    if (consoleFlag)              // was a file name given?
      System.exit(exitStatus);    // exit from application with status

    /* There was no file name on the command line.  Open the graphical user
    interface (GUI).  We don't need to be inside an if-then-else construct here
    because the console application called System.exit() above.  The standard
    Java interface style is the most reliable, but you can switch to something
    closer to the local system, if you want. */

//  try
//  {
//    UIManager.setLookAndFeel(
//      UIManager.getCrossPlatformLookAndFeelClassName());
////    UIManager.getSystemLookAndFeelClassName());
//  }
//  catch (Exception ulafe)
//  {
//    System.err.println("Unsupported Java look-and-feel: " + ulafe);
//  }

    /* Initialize shared graphical objects. */

    action = new FileChecksum3User(); // create our shared action listener
    fileChooser = new JFileChooser(); // create our shared file chooser
    progressTimer = new javax.swing.Timer(TIMER_DELAY, action);
                                  // update progress text on clock ticks only
    textMargins = new Insets(3, 5, 3, 5); // top, left, bottom, right

    try { soundsBad = java.applet.Applet.newAudioClip(new java.net.URL(
      "file:FileChecksum3.au")); } // play sound if checksums do not agree
    catch (java.net.MalformedURLException mue) { soundsBad = null; }

    /* We allow a tremendous range for the GUI font size, so it only makes
    sense to adjust spacing of the layout to match.  This is necessary for
    vertical spacing; constant horizontal spacing is acceptable for tested
    font sizes from 10 to 30 points.  This effectively uses a font size in
    points as a display size in pixels.  To be more accurate, we could call
    getFontMetrics() after a JButton or other Component is defined. */

    if (buttonFont != null)       // if defined, use the button font size
      gapSize = buttonFont.getSize(); // ... to set the basic pixel gap
    else                          // otherwise, default Java look-and-feel
      gapSize = 12;               // ... has this approximate font size

    /* Put everything into one "grid bag" layout.  Most of this code is just
    plain ugly.  There isn't much chance of understanding it unless you read
    the documentation for GridBagLayout ... if you can understand that! */

    JPanel panel1 = new JPanel(new GridBagLayout()); // create grid bag layout
    gbc = new GridBagConstraints(); // modify and reuse these constraints

    /* First layout line has status with informational messages. */

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    statusText = new JLabel(HELLO_TEXT, JLabel.CENTER);
    if (statusFont != null) statusText.setFont(statusFont);
    if (statusColor != null) statusText.setForeground(statusColor);
    panel1.add(statusText, gbc);

    /* Second line has the file name with two types of "open" buttons. */

    panel1.add(Box.createVerticalStrut((int) (1.5 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 1;
    filenameButton = new JButton("File Name...");
    filenameButton.addActionListener(action);
    filenameButton.setEnabled(true);
    if (buttonFont != null) filenameButton.setFont(buttonFont);
    if (buttonColor != null) filenameButton.setForeground(buttonColor);
    filenameButton.setMnemonic(KeyEvent.VK_F);
    filenameButton.setToolTipText("Find a file for checksumming.");
    panel1.add(filenameButton, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    filenameText = new JTextField("", 20);
    filenameText.addActionListener(action); // listen if Enter key pushed
    filenameText.setEditable(true); // enter file name, or click Open button
    filenameText.setEnabled(true);
    if (textFont != null) filenameText.setFont(textFont);
    if (textColor != null) filenameText.setForeground(textColor);
    filenameText.setMargin(textMargins);
    panel1.add(filenameText, gbc);
    panel1.add(Box.createHorizontalStrut((int) (2.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    startButton = new JButton("Start");
    startButton.addActionListener(action);
    startButton.setEnabled(true);
    if (buttonFont != null) startButton.setFont(buttonFont);
    if (buttonColor != null) startButton.setForeground(buttonColor);
    startButton.setMnemonic(KeyEvent.VK_S);
    startButton.setToolTipText("Open named file for checksumming.");
    panel1.add(startButton, gbc);

    /* Third line has the file size, the CRC32 checksum, and the CRC's "Copy"
    button. */

    panel1.add(Box.createVerticalStrut((int) (2.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    filesizeLabel = new JLabel("File size (bytes):");
    if (labelFont != null) filesizeLabel.setFont(labelFont);
    if (labelColor != null) filesizeLabel.setForeground(labelColor);
    panel1.add(filesizeLabel, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    filesizeText = new JTextField("", 12);
    filesizeText.setEditable(false); // user can't change this field
    if (textFont != null) filesizeText.setFont(textFont);
    if (textColor != null) filesizeText.setForeground(textColor);
    filesizeText.setMargin(textMargins);
    filesizeText.setText(filesizeString);
    panel2.add(filesizeText);
    panel2.add(Box.createHorizontalStrut((int) (2.0 * gapSize)));

    crc32Label = new JLabel("CRC32:");
    if (labelFont != null) crc32Label.setFont(labelFont);
    if (labelColor != null) crc32Label.setForeground(labelColor);
    panel2.add(crc32Label);
    panel2.add(Box.createHorizontalStrut((int) (0.8 * gapSize)));

    crc32Text = new JTextField("", 7);
    crc32Text.setEditable(false); // user can't change this field
    if (textFont != null) crc32Text.setFont(textFont);
    if (textColor != null) crc32Text.setForeground(textColor);
    crc32Text.setMargin(textMargins);
    crc32Text.setText(crc32String);
    panel2.add(crc32Text);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.NONE;
    panel1.add(panel2, gbc);
    panel1.add(Box.createHorizontalStrut((int) (2.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    copyCrc32Button = new JButton("Copy CRC32");
    copyCrc32Button.addActionListener(action);
    copyCrc32Button.setEnabled(false);
    if (buttonFont != null) copyCrc32Button.setFont(buttonFont);
    if (buttonColor != null) copyCrc32Button.setForeground(buttonColor);
    copyCrc32Button.setMnemonic(KeyEvent.VK_R);
    copyCrc32Button.setToolTipText("Copy CRC32 checksum to clipboard.");
    panel1.add(copyCrc32Button, gbc);

    /* Fourth line has the MD5 checksum and its "Copy" button. */

    panel1.add(Box.createVerticalStrut((int) (1.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    md5Checkbox = new JCheckBox("MD5 checksum:", md5Flag);
    if (labelFont != null) md5Checkbox.setFont(labelFont);
    if (labelColor != null) md5Checkbox.setForeground(labelColor);
    md5Checkbox.addActionListener(action); // do last so don't fire early
    panel1.add(md5Checkbox, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.NONE;
    md5Text = new JTextField("", 24);
    md5Text.setEditable(false);   // user can't change this field
    if (textFont != null) md5Text.setFont(textFont);
    if (textColor != null) md5Text.setForeground(textColor);
    md5Text.setMargin(textMargins);
    md5Text.setText(md5String);
    panel1.add(md5Text, gbc);
    panel1.add(Box.createHorizontalStrut((int) (2.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    copyMd5Button = new JButton("Copy MD5");
    copyMd5Button.addActionListener(action);
    copyMd5Button.setEnabled(false);
    if (buttonFont != null) copyMd5Button.setFont(buttonFont);
    if (buttonColor != null) copyMd5Button.setForeground(buttonColor);
    copyMd5Button.setMnemonic(KeyEvent.VK_M);
    copyMd5Button.setToolTipText("Copy MD5 checksum to clipboard.");
    panel1.add(copyMd5Button, gbc);

    /* Next line has the SHA1 checksum and its "Copy" button. */

    panel1.add(Box.createVerticalStrut((int) (1.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    sha1Checkbox = new JCheckBox("SHA1 checksum:", sha1Flag);
    if (labelFont != null) sha1Checkbox.setFont(labelFont);
    if (labelColor != null) sha1Checkbox.setForeground(labelColor);
    sha1Checkbox.addActionListener(action); // do last so don't fire early
    panel1.add(sha1Checkbox, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    sha1Text = new JTextField("", 30);
    sha1Text.setEditable(false);  // user can't change this field
    if (textFont != null) sha1Text.setFont(textFont);
    if (textColor != null) sha1Text.setForeground(textColor);
    sha1Text.setMargin(textMargins);
    sha1Text.setText(sha1String);
    panel1.add(sha1Text, gbc);
    panel1.add(Box.createHorizontalStrut((int) (2.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    copySha1Button = new JButton("Copy SHA1");
    copySha1Button.addActionListener(action);
    copySha1Button.setEnabled(false);
    if (buttonFont != null) copySha1Button.setFont(buttonFont);
    if (buttonColor != null) copySha1Button.setForeground(buttonColor);
    copySha1Button.setMnemonic(KeyEvent.VK_H);
    copySha1Button.setToolTipText("Copy SHA1 checksum to clipboard.");
    panel1.add(copySha1Button, gbc);

    /* Next line has the SHA256 checksum and its "Copy" button. */

    panel1.add(Box.createVerticalStrut((int) (1.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    sha256Checkbox = new JCheckBox("SHA256 checksum:", sha256Flag);
    if (labelFont != null) sha256Checkbox.setFont(labelFont);
    if (labelColor != null) sha256Checkbox.setForeground(labelColor);
    sha256Checkbox.addActionListener(action); // do last so don't fire early
    panel1.add(sha256Checkbox, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    sha256Text = new JTextField("", 30);
    sha256Text.setEditable(false); // user can't change this field
    if (textFont != null) sha256Text.setFont(textFont);
    if (textColor != null) sha256Text.setForeground(textColor);
    sha256Text.setMargin(textMargins);
    sha256Text.setText(sha256String);
    panel1.add(sha256Text, gbc);
    panel1.add(Box.createHorizontalStrut((int) (2.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    copySha256Button = new JButton("Copy SHA256");
    copySha256Button.addActionListener(action);
    copySha256Button.setEnabled(false);
    if (buttonFont != null) copySha256Button.setFont(buttonFont);
    if (buttonColor != null) copySha256Button.setForeground(buttonColor);
    copySha256Button.setMnemonic(KeyEvent.VK_2);
    copySha256Button.setToolTipText("Copy SHA256 checksum to clipboard.");
    panel1.add(copySha256Button, gbc);

    /* Next line has the SHA512 checksum and its "Copy" button. */

    panel1.add(Box.createVerticalStrut((int) (1.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    sha512Checkbox = new JCheckBox("SHA512 checksum:", sha512Flag);
    if (labelFont != null) sha512Checkbox.setFont(labelFont);
    if (labelColor != null) sha512Checkbox.setForeground(labelColor);
    sha512Checkbox.addActionListener(action); // do last so don't fire early
    panel1.add(sha512Checkbox, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    sha512Text = new JTextField("", 30);
    sha512Text.setEditable(false); // user can't change this field
    if (textFont != null) sha512Text.setFont(textFont);
    if (textColor != null) sha512Text.setForeground(textColor);
    sha512Text.setMargin(textMargins);
    sha512Text.setText(sha512String);
    panel1.add(sha512Text, gbc);
    panel1.add(Box.createHorizontalStrut((int) (2.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    copySha512Button = new JButton("Copy SHA512");
    copySha512Button.addActionListener(action);
    copySha512Button.setEnabled(false);
    if (buttonFont != null) copySha512Button.setFont(buttonFont);
    if (buttonColor != null) copySha512Button.setForeground(buttonColor);
    copySha512Button.setMnemonic(KeyEvent.VK_5);
    copySha512Button.setToolTipText("Copy SHA512 checksum to clipboard.");
    panel1.add(copySha512Button, gbc);

    /* Next line has a comparison field where the user can enter a checksum to
    compare against our calculated checksums. */

    panel1.add(Box.createVerticalStrut((int) (1.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    compareLabel = new JLabel("Compare against:");
    if (labelFont != null) compareLabel.setFont(labelFont);
    if (labelColor != null) compareLabel.setForeground(labelColor);
    panel1.add(compareLabel, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    compareText = new JTextField("", 20);
    compareText.addActionListener(action); // listen if Enter key pushed
    compareText.setEditable(true); // user can put anything he/she wants here
    if (textFont != null) compareText.setFont(textFont);
    if (textColor != null) compareText.setForeground(textColor);
    compareText.setMargin(textMargins);
    panel1.add(compareText, gbc);
    panel1.add(Box.createHorizontalStrut((int) (2.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    pasteCompareButton = new JButton("Paste");
    pasteCompareButton.addActionListener(action);
    if (buttonFont != null) pasteCompareButton.setFont(buttonFont);
    if (buttonColor != null) pasteCompareButton.setForeground(buttonColor);
    pasteCompareButton.setMnemonic(KeyEvent.VK_P);
    pasteCompareButton.setToolTipText("Paste checksum for comparison.");
    panel1.add(pasteCompareButton, gbc);

    /* Next line has the "Cancel" button, a progress bar, and the standard
    "Exit" button. */

    panel1.add(Box.createVerticalStrut((int) (2.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 1;
    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(action);
    cancelButton.setEnabled(false);
    if (buttonFont != null) cancelButton.setFont(buttonFont);
    if (buttonColor != null) cancelButton.setForeground(buttonColor);
    cancelButton.setMnemonic(KeyEvent.VK_C);
    cancelButton.setToolTipText("Stop checking/opening files.");
    panel1.add(cancelButton, gbc);
    panel1.add(Box.createHorizontalStrut((int) (0.8 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.BOTH;
    progressBar = new JProgressBar(0, 100);
    if (textFont != null) progressBar.setFont(textFont);
    progressBar.setString("");
    progressBar.setStringPainted(true);
    progressBar.setValue(0);
    panel1.add(progressBar, gbc);
    panel1.add(Box.createHorizontalStrut((int) (2.0 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    if (buttonFont != null) exitButton.setFont(buttonFont);
    if (buttonColor != null) exitButton.setForeground(buttonColor);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel1.add(exitButton, gbc);

    /* Last line is our copyright notice in subdued gray text. */

    panel1.add(Box.createVerticalStrut((int) (1.5 * gapSize)), gbc);

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    legalNotice = new JLabel(COPYRIGHT_NOTICE, JLabel.CENTER);
    if (labelFont != null) legalNotice.setFont(labelFont);
    if (labelColor != null) legalNotice.setForeground(labelColor);
    panel1.add(legalNotice, gbc);

    /* The layout in a grid bag goes strange if there isn't enough space.  Box
    the grid bag inside a flow layout to center it horizontally and stop
    expansion, then inside a plain box to center it vertically. */

    JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
    panel3.add(panel1);           // put grid bag in a fancy horizontal box

    Box panel4 = Box.createVerticalBox(); // create a basic vertical box
    panel4.add(Box.createGlue()); // stretch to the top
    panel4.add(Box.createVerticalStrut(30)); // top margin
    panel4.add(panel3);           // put boxed grid bag in center
    panel4.add(Box.createVerticalStrut(30)); // bottom margin
//  panel4.add(Box.createGlue()); // stretch to bottom (assumed by layout)

    /* Create the main window frame for this application. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel5 = mainFrame.getContentPane(); // where content meets frame
    panel5.setLayout(new BorderLayout(0, 0));
    panel5.add(panel4, BorderLayout.CENTER); // just the boxed grid bag layout

    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setLocation(windowLeft, windowTop); // normal top-left corner
    if ((windowHeight < MIN_FRAME) || (windowWidth < MIN_FRAME))
      mainFrame.pack();           // do component layout with minimum size
    else                          // the user has given us a window size
      mainFrame.setSize(windowWidth, windowHeight); // size of normal window
    if (maximizeFlag) mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    mainFrame.validate();         // recheck application window layout
    mainFrame.setVisible(true);   // and then show application window

    /* Let the graphical interface run the application now. */

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  calcFileChecksum() method

  Calculate the CRC32, MD5, and SHA checksums for a given file.  We watch the
  <cancelFlag> while we are running, and if that flag is true, then we close
  the file and set the checksums to empty strings.

  This method should only be called from a console application or from inside a
  separate thread started by the openFile() method.

  If the basic file I/O loop takes 13 seconds per gigabyte (GB), then the CRC32
  checksum is too fast to measure, adding less than one (1) second per GB.  The
  MD5 adds about 2 seconds per GB.  SHA1 adds about 9 seconds.  SHA256 adds 16
  seconds.  SHA512 adds 27 seconds.  Hence, SHA256 and SHA512 are optional, and
  MD5 and SHA1 can be disabled with a boolean global variable.  (These numbers
  are from Sun Java 6 on Microsoft Windows with an Intel Core2 Duo processor at
  3.00 GHz.  Your results will differ.)
*/
  static void calcFileChecksum(File givenFile)
  {
    byte[] buffer;                // input buffer for reading file
    CRC32 crc32digest;            // object for calculating CRC32 checksum
    int i;                        // index variable
    FileInputStream input;        // input file stream
    MessageDigest md5digest;      // object for calculating MD5 checksum
    String rawtext;               // text string in middle of hex conversion
    MessageDigest sha1digest;     // object for calculating SHA1 checksum
    MessageDigest sha256digest;   // object for calculating SHA256 checksum
    MessageDigest sha512digest;   // object for calculating SHA512 checksum
    long startTime;               // starting time in milliseconds

    /* Clear global checksum strings, and displayed text if GUI application. */

    clearChecksums();             // clear all checksum fields, file size, etc
    sizeDone = 0;                 // how much of <sizeTotal> has been finished
    sizeSuffix = null;            // no pre-formatted text for progress bar
    sizeTotal = givenFile.length(); // get total size of user's file in bytes
    filesizeString = formatComma.format(sizeTotal); // format and save size
    if (!consoleFlag)             // displayed text, status timer only if GUI
    {
      filesizeText.setText(filesizeString); // show size before start checksums
      progressTimer.start();      // start updating the progress bar and text
    }

    /* Create message digests, read file, and convert resulting checksums into
    hexadecimal strings. */

    try
    {
      /* The correct spelling of message digest algorithm names differs from
      how we portray them to the user: we say "SHA1" but the standard name is
      "SHA-1".  The MessageDigest.getInstance() parameter is very literal. */

      buffer = new byte[bufferSize]; // allocate big/faster input buffer
      crc32digest = new CRC32();  // allocate new object for CRC32 checksum
      md5digest = calcFileGetDigest(md5Flag, "MD5"); // get MD5 message digest
      sha1digest = calcFileGetDigest(sha1Flag, "SHA-1");
      sha256digest = calcFileGetDigest(sha256Flag, "SHA-256");
      sha512digest = calcFileGetDigest(sha512Flag, "SHA-512");
      startTime = System.currentTimeMillis(); // starting time in milliseconds

      /* Tight loop to open file, read data bytes, and update checksums. */

      input = new FileInputStream(givenFile); // open file for reading bytes
      while ((i = input.read(buffer, 0, bufferSize)) > 0)
      {
        if (cancelFlag) break;    // stop if user hit the panic button

        /* Update the checksum calculations.  While it would be more obvious to
        use a boolean variable like <md5Flag> in the <if> statements, this does
        not allow for failure when creating the message digests.  Hence, we use
        a comparison against <null> for the digest objects. */

        crc32digest.update(buffer, 0, i); // update CRC32 checksum
        if (md5digest != null)
          md5digest.update(buffer, 0, i); // update MD5 checksum
        if (sha1digest != null)
          sha1digest.update(buffer, 0, i); // update SHA1 checksum
        if (sha256digest != null)
          sha256digest.update(buffer, 0, i); // update SHA256 checksum
        if (sha512digest != null)
          sha512digest.update(buffer, 0, i); // update SHA512 checksum
        sizeDone += i;            // add to number of bytes finished
      }
      input.close();              // close input file

      /* This program is sometimes used to evaluate input buffer sizes on
      different operating systems and versions of Java.  Avoid SHA family
      checksums when testing the read loop above. */

      if (debugFlag)              // does user want details?
        System.err.println(givenFile.getName() + " - buffer size "
          + formatComma.format(bufferSize) + " read file "
          + formatComma.format(sizeDone) + " bytes in "
          + formatComma.format(System.currentTimeMillis() - startTime)
          + " milliseconds");

      /* If we weren't cancelled by the user, then convert the final checksums
      into hexadecimal strings. */

      if (!cancelFlag)            // don't do more work if cancelled by user
      {
        /* Convert the CRC32 checksum to a hexadecimal string.  We must pad
        with leading zeros since the toHexString() method doesn't do this. */

        rawtext = "00000000" + Long.toHexString(crc32digest.getValue());
        crc32String = rawtext.substring(rawtext.length() - 8);

        /* Convert the MD5 checksum to a hexadecimal string.  We call another
        method to convert raw bytes to hex, because SHA needs the same. */

        if (md5digest != null) md5String = formatHexBytes(md5digest.digest());

        /* Convert the SHA1 checksum to a hexadecimal string. */

        if (sha1digest != null)
          sha1String = formatHexBytes(sha1digest.digest());

        /* Convert the SHA256 checksum to a hexadecimal string. */

        if (sha256digest != null)
          sha256String = formatHexBytes(sha256digest.digest());

        /* Convert the SHA512 checksum to a hexadecimal string. */

        if (sha512digest != null)
          sha512String = formatHexBytes(sha512digest.digest());

        /* Force the progress bar to one hundred percent. */

        if (!consoleFlag)
        {
          progressTimer.stop();   // stop updating the progress text by timer
          progressBar.setString("100 %"); // final text label
          progressBar.setValue(100); // final position
        }
      }
    }
    catch (IOException ioe)
    {
      if (consoleFlag)
        System.err.println("Can't read from file: " + ioe.getMessage());
      else
        statusText.setText("Unable to open or read from selected file.");
      cancelFlag = true;          // tell caller that we cancelled
    }

    /* If running as a graphical application, copy the checksum strings into
    text boxes visible to the user.  Some may be empty if there was a problem
    above (cancelled or error). */

    if (!consoleFlag)
    {
      progressTimer.stop();       // do again, in case of cancel or error
      calcFileSetText(true, crc32String, crc32Text, copyCrc32Button);
      calcFileSetText(md5Flag, md5String, md5Text, copyMd5Button);
      calcFileSetText(sha1Flag, sha1String, sha1Text, copySha1Button);
      calcFileSetText(sha256Flag, sha256String, sha256Text, copySha256Button);
      calcFileSetText(sha512Flag, sha512String, sha512Text, copySha512Button);
    }
  } // end of calcFileChecksum() method


/*
  calcFileGetDigest() method

  Not all message digest algorithms are available on every Java run-time
  environment.  This method tries to create a message digest, and returns
  <null> if that fails.
*/
  static MessageDigest calcFileGetDigest(
    boolean flag,                 // true if we try to create message digest
    String name)                  // message digest algorithm name
  {
    MessageDigest result;         // a proper message digest or <null>

    if (flag) try                 // if user wants this checksum
    {
      result = MessageDigest.getInstance(name); // create message digest
    }
    catch (NoSuchAlgorithmException nsae) // bad name or unsupported
    {
      if (consoleFlag)            // warn console user but not GUI user
        System.err.println("Unsupported checksum algorithm: "
          + nsae.getMessage());
      result = null;
    }
    else                          // user does not want this checksum
      result = null;

    return(result);

  } // end of calcFileGetDigest() method


/*
  calcFileSetText() method

  After checksums have been calculated, we have three possible situations:
  success, failure, and nobody wanted checksums in the first place.
*/
  static void calcFileSetText(
    boolean flag,                 // true if user wants this checksum
    String checksum,              // calculated checksum as text
    JTextField field,             // text field where checksum goes
    JButton copy)                 // copy button to enable or disable
  {
    if (flag == false)            // if user doesn't want this checksum
    {
      copy.setEnabled(false);     // disable copy button
      field.setText("");          // remove anything in text field
    }
    else if ((checksum != null) && (checksum.length() > 0))
    {
      copy.setEnabled(true);      // enable copy button
      field.setText(checksum);    // put checksum into text field
    }
    else                          // cancelled or an error
    {
      copy.setEnabled(false);     // disable copy button
      field.setText("(error)");   // warn user something went wrong
    }
  } // end of calcFileSetText() method


/*
  cleanChecksum() method

  Do some mild cleaning up for a string that is supposed to be a hexadecimal
  checksum: remove spaces, some punctuation, and convert proper hex digits to
  lowercase.  Anything else is left untouched.  Bad input is left in the string
  so that later comparisons with valid checksums will fail.
*/
  static String cleanChecksum(String input)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    int i;                        // index variable
    int length;                   // size of input string in characters

    buffer = new StringBuffer();  // allocate empty string buffer for result
    length = input.length();      // get size of input string in characters
    for (i = 0; i < length; i ++)
    {
      ch = input.charAt(i);       // get one character from input string
      if ((ch == '\t') || (ch == ' ') || (ch == ',') || (ch == '-')
        || (ch == '.') || (ch == ':'))
        { /* do nothing: ignore selected punctuation */ }
      else if ((ch >= '0') && (ch <= '9'))
        buffer.append(ch);        // accept decimal digit and append to result
      else if ((ch >= 'a') && (ch <= 'f'))
        buffer.append(ch);        // accept lowercase hexadecimal digit
      else if ((ch >= 'A') && (ch <= 'F'))
        buffer.append((char) (ch - 'A' + 'a')); // but convert uppercase hex
      else
        buffer.append(ch);        // don't change so that comparison will fail
    }
    return(buffer.toString());    // give caller our converted string

  } // end of cleanChecksum() method


/*
  clearChecksums() method

  Clear all checksum text fields to empty strings, and clear the progress bar.
*/
  static void clearChecksums()
  {
    crc32String = "";             // set CRC32 checksum to empty string
    filesizeString = "";          // set formatted file size to empty string
    md5String = "";               // set MD5 checksum to empty string
    sha1String = "";              // set SHA1 checksum to empty string
    sha256String = "";            // set SHA256 checksum to empty string
    sha512String = "";            // set SHA512 checksum to empty string

    if (!consoleFlag)             // if running as graphical application
    {
      copyCrc32Button.setEnabled(false); // no copy button for empty strings
      copyMd5Button.setEnabled(false);
      copySha1Button.setEnabled(false);
      copySha256Button.setEnabled(false);
      copySha512Button.setEnabled(false);
      crc32Text.setText(crc32String); // put empty strings into text boxes
      filesizeText.setText(filesizeString);
      md5Text.setText(md5String);
      progressBar.setString("");  // remove any text label from progress bar
      progressBar.setValue(0);    // reset progress bar to beginning (empty)
      sha1Text.setText(sha1String);
      sha256Text.setText(sha256String);
      sha512Text.setText(sha512String);
    }
  } // end of clearChecksums() method


/*
  compareChecksum() method

  Check if a given string matches any of the computed checksums, and print or
  set the status message to reflect the result.  We don't change or clean up
  the caller's string in any way.  We do return an integer status, if the
  caller wants to check.
*/
  static int compareChecksum(String given)
  {
    int status;                   // the status that we return

    status = EXIT_UNKNOWN;        // assume that string does not match

    if ((given.length() > 0) && (crc32String.length() > 0)) // anything to do?
    {
      /* It shouldn't be possible for one string to match more than one of the
      CRC32, MD5, and SHA1 checksums because they have different lengths. */

      if (given.equals(crc32String)) // match for the CRC32 checksum?
      {
        status = EXIT_SUCCESS;    // yes, indicate success
        if (consoleFlag)
          System.out.println("Successfully matched the CRC32 checksum.");
        else
          statusText.setText("Successfully matched the CRC32 checksum.");
      }
      else if (md5Flag && given.equals(md5String)) // match the MD5 checksum?
      {
        status = EXIT_SUCCESS;    // yes, indicate success
        if (consoleFlag)
          System.out.println("Successfully matched the MD5 checksum.");
        else
          statusText.setText("Successfully matched the MD5 checksum.");
      }
      else if (sha1Flag && given.equals(sha1String)) // match the SHA1?
      {
        status = EXIT_SUCCESS;    // yes, indicate success
        if (consoleFlag)
          System.out.println("Successfully matched the SHA1 checksum.");
        else
          statusText.setText("Successfully matched the SHA1 checksum.");
      }
      else if (sha256Flag && given.equals(sha256String)) // match the SHA256?
      {
        status = EXIT_SUCCESS;    // yes, indicate success
        if (consoleFlag)
          System.out.println("Successfully matched the SHA256 checksum.");
        else
          statusText.setText("Successfully matched the SHA256 checksum.");
      }
      else if (sha512Flag && given.equals(sha512String)) // match the SHA512?
      {
        status = EXIT_SUCCESS;    // yes, indicate success
        if (consoleFlag)
          System.out.println("Successfully matched the SHA512 checksum.");
        else
          statusText.setText("Successfully matched the SHA512 checksum.");
      }
      else if ((!consoleFlag) && (startButton.isEnabled() == false))
      {
        statusText.setText(WAIT_TEXT); // tell impatient user to wait
//      status = EXIT_UNKNOWN;    // repeat default status of "know nothing"
      }
      else
      {
        /* The comparison failed, and not because we are otherwise busy
        calculating a new checksum. */

        status = EXIT_FAILURE;    // doesn't match, isn't pending, etc
        if (consoleFlag)
          System.out.println("Supplied checksum <" + given
            + "> does not match calculated checksums.");
        else
        {
          statusText.setText(
            "Supplied checksum does not match calculated checksums.");
          if (soundsBad != null)  // sound file may not have loaded properly
            soundsBad.play();     // play sound if checksums do not agree
        }
      }
    }
    else if (!consoleFlag)        // nothing given, but running graphical?
    {
      if (startButton.isEnabled())
        statusText.setText(HELLO_TEXT); // return to welcome status message
      else
        statusText.setText(WAIT_TEXT); // tell impatient user to wait
    }
    return(status);               // return the indicated status to caller

  } // end of compareChecksum() method


/*
  doCancelButton() method

  This method is called while we are opening files if the user wants to end the
  processing early, perhaps because it is taking too long.  We must cleanly
  terminate any secondary threads.
*/
  static void doCancelButton()
  {
    cancelFlag = true;            // tell other threads that all work stops now
    statusText.setText("Checksum calculation cancelled by user.");
  }


/*
  doFilenameButton() method

  Open a dialog box to browse for a file.  Then open that file and calculate
  the checksums.
*/
  static void doFilenameButton()
  {
    File givenFile;               // user's selected file

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Browse or Open File...");
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION)
    {
      givenFile = fileChooser.getSelectedFile(); // get selected file
      filenameText.setText(givenFile.getPath()); // save file name in text box
      openFile(givenFile);        // and open that file for checksumming
    }
  } // end of doFilenameButton() method


/*
  doFilenameEnter() method

  The user typed something into the file name text area.  Inspect the contents,
  and if it looks like a valid file name, then open the file to calculate the
  checksums.
*/
  static void doFilenameEnter()
  {
    String filename;              // entered file name after some cleaning up

    filename = filenameText.getText().trim(); // remove leading/trailing spaces
    if (filename.length() == 0)   // was anything actually entered?
    {
      clearChecksums();           // nothing entered, clear all checksum fields
      filenameText.setText("");   // and clear file name text field too
      statusText.setText(HELLO_TEXT); // return to welcome status message
    }
    else
    {
      filenameText.setText(filename); // reset to trimmed (cleaned) file name
      openFile(new File(filename)); // assume string is a file name and open
    }
  } // end of doFilenameEnter() method


/*
  doStartButton() method

  The only purpose for the "Start" button is when people enter a file name,
  without using the "File Name" button, and without pressing the Enter key.
*/
  static void doStartButton()
  {
    doFilenameEnter();            // gotta love modular programming!
  }


/*
  doTimer() method

  Update the progress bar when the GUI timer is activated.  We rely on several
  global variables initialized and updated by the calcFileChecksum() method.
*/
  static void doTimer()
  {
    if (sizeTotal > 0)            // none of this makes sense for empty files
    {
      int percent = (int) (((double) sizeDone) * 100.0 / ((double) sizeTotal));
      progressBar.setValue(percent); // always update progress bar

      if (sizeTotal > 99999999)   // one format of progress text for big files
      {
        if (sizeSuffix == null)   // have we formatted the total file size?
          sizeSuffix = " of " + formatMegabytes(sizeTotal) + " MB";
        progressBar.setString(formatMegabytes(sizeDone) + sizeSuffix);
      }
      else if (sizeTotal > 999999) // another format for medium-sized files
      {
        progressBar.setString(percent + " %"); // show only percent complete
      }
    }
  } // end of doTimer() method


/*
  formatHexBytes() method

  Format a raw array of binary bytes as a hexadecimal string.
*/
  static String formatHexBytes(byte[] raw)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c', 'd', 'e', 'f'}; // for converting binary to hexadecimal
    int i;                        // index variable
    int value;                    // one byte value from raw array

    buffer = new StringBuffer(raw.length * 2);
                                  // allocate empty string buffer for result
    for (i = 0; i < raw.length; i ++)
    {
      value = raw[i];             // get one byte value from raw array
      buffer.append(hexDigits[(value >> 4) & 0x0F]); // hex high-order nibble
      buffer.append(hexDigits[value & 0x0F]); // hex low-order nibble
    }
    return(buffer.toString());    // give caller our converted string

  } // end of formatHexBytes() method


/*
  formatMegabytes() method

  Given a file size in bytes, return a formatted string with the size in real
  megabytes.  The caller must append any "MB" tag.
*/
  static String formatMegabytes(long filesize)
  {
    return(formatPointOne.format(((double) filesize) / 1048576.0));
  }


/*
  openFile() method

  The caller gives us a File object that may or may not be a valid file.  Try
  to open this file and calculate the checksums.  Since this may take a long
  time for a big file, we do the heavy processing in a separate thread and have
  a "Cancel" button to terminate processing early.
*/
  static void openFile(File givenFile)
  {
    openFileObject = givenFile;   // save caller's parameter as global variable

    /* Disable the "Start" button until we are done, and enable a "Cancel"
    button in case our secondary thread runs for a long time and the user
    panics. */

    cancelButton.setEnabled(true); // enable button to cancel this processing
    cancelFlag = false;           // but don't cancel unless user complains
    crc32Label.setEnabled(false); // so label looks like method checkboxes
    filenameButton.setEnabled(false); // suspend browsing for input files
    filenameText.setEnabled(false); // suspend entering of file name text
    md5Checkbox.setEnabled(false); // suspend changes to method checkboxes
    sha1Checkbox.setEnabled(false);
    sha256Checkbox.setEnabled(false);
    sha512Checkbox.setEnabled(false);
    startButton.setEnabled(false); // suspend "Start" button until we are done
    statusText.setText(WAIT_TEXT); // tell user to wait or cancel

    openFileThread = new Thread(new FileChecksum3User(), "openFileRunner");
    openFileThread.setPriority(Thread.MIN_PRIORITY);
                                  // use low priority for heavy-duty workers
    openFileThread.start();       // run separate thread to open files, report

  } // end of openFile() method


/*
  openFileRunner() method

  This method is called inside a separate thread by the runnable interface of
  our "user" class to process the user's selected files in the context of the
  "main" class.  By doing all the heavy-duty work in a separate thread, we
  won't stall the main thread that runs the graphical interface, and we allow
  the user to cancel the processing if it takes too long.
*/
  static void openFileRunner()
  {
    /* Call a common routine for calculating the checksums. */

    calcFileChecksum(openFileObject);

    /* We are done the dirty work, so turn off the "Cancel" button and allow
    the user to click the "Start" button again. */

    cancelButton.setEnabled(false); // disable "Cancel" button when idle
    crc32Label.setEnabled(true);  // return this label back to normal
    filenameButton.setEnabled(true); // resume browsing for input files
    filenameText.setEnabled(true); // resume entering of file name text
    md5Checkbox.setEnabled(true); // resume changes to method checkboxes
    sha1Checkbox.setEnabled(true);
    sha256Checkbox.setEnabled(true);
    sha512Checkbox.setEnabled(true);
    startButton.setEnabled(true); // allow "Start" button once again

    /* If we weren't cancelled, then compare any checksum supplied by the user
    with the calculated checksums.  We do this after enabling the regular
    buttons, so that compareChecksum() can set the appropriate status message.
    */

    if (!cancelFlag)
    {
      compareText.setText(cleanChecksum(compareText.getText())); // clean up
      compareChecksum(compareText.getText()); // compare with calculated
    }
  } // end of openFileRunner() method


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("To run as a console application, first parameter must be a file name.  Second");
    System.err.println("and following parameters are optional checksums to be tested against calculated");
    System.err.println("checksums for the given file.  Output may be redirected with the \">\" operator.");
    System.err.println();
    System.err.println("    java  FileChecksum3  [options]  filename  [checksums]");
    System.err.println();
    System.err.println("To run as a graphical application, don't put a file name on the command line:");
    System.err.println();
    System.err.println("    java  FileChecksum3  [options]");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
//  System.err.println("  -b# = buffer size from 4KB to 16MB for reading files.  Please accept the");
//  System.err.println("      default size except in unusual situations after careful testing.");
    System.err.println("  -d = show debug information (may be verbose)");
//  System.err.println("  -md5 = show MD5 checksum"); // enabled by default
//  System.err.println("  -sha1 = show SHA1 checksum"); // enabled by default
    System.err.println("  -sha256 = show SHA256 checksum (slow)");
    System.err.println("  -sha512 = show SHA512 checksum (slower)");
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main FileChecksum3 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == cancelButton)   // "Cancel" button
    {
      doCancelButton();           // stop calculating current checksum
    }
    else if (source == compareText) // user typed text and pressed Enter
    {
      compareText.setText(cleanChecksum(compareText.getText())); // clean up
      compareChecksum(compareText.getText()); // compare with calculated
    }
    else if (source == copyCrc32Button) // copy CRC32 checksum to clipboard
    {
      crc32Text.selectAll();      // select all characters in text field
      crc32Text.copy();           // and copy those characters to the clipboard
    }
    else if (source == copyMd5Button) // copy MD5 checksum to clipboard
    {
      md5Text.selectAll();        // select all characters in text field
      md5Text.copy();             // and copy those characters to the clipboard
    }
    else if (source == copySha1Button) // copy SHA1 checksum to clipboard
    {
      sha1Text.selectAll();       // select all characters in text field
      sha1Text.copy();            // and copy those characters to the clipboard
    }
    else if (source == copySha256Button) // copy SHA256 checksum to clipboard
    {
      sha256Text.selectAll();     // select all characters in text field
      sha256Text.copy();          // and copy those characters to the clipboard
    }
    else if (source == copySha512Button) // copy SHA512 checksum to clipboard
    {
      sha512Text.selectAll();     // select all characters in text field
      sha512Text.copy();          // and copy those characters to the clipboard
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == filenameButton) // "File Name" button
    {
      doFilenameButton();         // browse or select input file
    }
    else if (source == filenameText) // user typed text and pressed Enter
    {
      doFilenameEnter();          // inspect file name field and open file
    }
    else if (source == md5Checkbox) // enable or disable MD5 checksum
    {
      md5Flag = md5Checkbox.isSelected();
    }
    else if (source == pasteCompareButton) // paste from clipboard and compare
    {
      compareText.setText("");    // clear any existing comparison text
      compareText.paste();        // paste clipboard into our comparison text
      compareText.setText(cleanChecksum(compareText.getText())); // clean up
      compareChecksum(compareText.getText()); // compare with calculated
    }
    else if (source == progressTimer) // update progress text on clock ticks
    {
      doTimer();                  // recalculate megabytes or percent done
    }
    else if (source == sha1Checkbox) // enable or disable SHA1 checksum
    {
      sha1Flag = sha1Checkbox.isSelected();
    }
    else if (source == sha256Checkbox) // enable or disable SHA256 checksum
    {
      sha256Flag = sha256Checkbox.isSelected();
    }
    else if (source == sha512Checkbox) // enable or disable SHA512 checksum
    {
      sha512Flag = sha512Checkbox.isSelected();
    }
    else if (source == startButton) // "Start" button
    {
      doStartButton();            // start calculating checksum for named file
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method

} // end of FileChecksum3 class

// ------------------------------------------------------------------------- //

/*
  FileChecksum3User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class FileChecksum3User implements ActionListener, Runnable
{
  /* empty constructor */

  public FileChecksum3User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    FileChecksum3.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run()
  {
    FileChecksum3.openFileRunner();
  }

} // end of FileChecksum3User class

/* Copyright (c) 2008 by Keith Fenske.  Apache License or GNU GPL. */

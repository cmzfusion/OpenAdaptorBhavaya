package org.bhavaya.util;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.*;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector; // For test program (main)

/**
 * grabbed from http://www.ipso-facto.demon.co.uk/
 *
 * Class to handle loading and saving of CSV files. The data is represented
 * using the JDK1.2 TableModel interface. The methods of this class are static
 * and can be used like this:
 * <P>
 * <PRE>
 *    TableModel model= CSVFile.load(new URL("file:test.csv"));
 *    CSVFile.save(new File("output.csv"), model);
 * </PRE>
 * <P>
 * The model contains the data from the file stored as Strings.
 * <P>
 * <FONT COLOR="red">Does not handle fields with nested comma, even if the
 * fields are quoted.</FONT>
 */
public class CsvFileHandler
{
   static boolean hasHeadings= true;
   static boolean hasQuotes= true;

   /**
    * Split a line into chunks using ',' as a delimiter. The substrings
    * are returned as a Vector of strings, the commas are swallowed.
    * Empty columns are represented as the empty string, not null.
    */
   protected static Vector parseLine(String line)
   {
      Vector entries= new Vector();

      StringTokenizer st = new StringTokenizer(line, ",", true);
      boolean precedingTokenWasComma= false;

      while (st.hasMoreTokens()) {
         String tok= st.nextToken();

         if (precedingTokenWasComma && tok.equals(",")) {
            entries.addElement("");
         }
         else if (tok.equals(",")) {
            precedingTokenWasComma= true;
         }
         else {
            precedingTokenWasComma= false;

            if (tok.startsWith("\"")) {
               while(!tok.endsWith("\"")) {
                  tok= tok + st.nextToken();
               }
            }

            if (hasQuotes) {
               if (tok.startsWith("\"") && tok.endsWith("\"")) {
                  tok= tok.substring(1, tok.length()-2);
               }
            }

            // Convert ^F used for newline in some CSV programs to it's real representation.
            tok= tok.replace('\006', '\n');
            entries.addElement(tok);
         }

         //         System.err.println("Token: '" + tok + "' Comma State: " + precedingTokenWasComma);
      }
       if (precedingTokenWasComma) {
          entries.addElement("");
       }

      return entries;
   }

   /**
    * Load a CSVFile from the specified url. For most cases, that's all you need
    * to know.
    * <P>
    * If the hasHeadings property is true then the first line of the file is used
    * to set the TableModel column headings, instead of as normal data. If the
    * property is false then the model will be given auto-generated headings. The
    * hasHeadings property is true by default. If hasQuotes is true then starting
    * and leading quotes will be automatically stripped from the fields (only if both
    * present).
    */
   public static TableModel load(URL url) throws IOException {
       InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
       return load(inputStreamReader);
   }

    public static TableModel load(String csvString) {
        StringReader stringReader = new StringReader(csvString);
        TableModel tableModel = null;
        try {
            tableModel = load(stringReader);
        } catch (IOException e) {
            throw new RuntimeException("You didn't get this exception. It shouldn't be possible", e);
        }
        return tableModel;
    }

   public static TableModel load(Reader reader) throws IOException
   {
      BufferedReader bufferedReader= new BufferedReader(reader);

      DefaultTableModel model= null;

      boolean first= true;
      String line= bufferedReader.readLine();
      while (line != null) {
         // If start load column defs
         if (first) {
            first= false;

            if (hasHeadings)
               model= new DefaultTableModel(parseLine(line), 0);
            else {
               Vector attrs= parseLine(line);
               model= new DefaultTableModel(0, attrs.size());
               model.addRow(attrs);
            }
         }
         else {
            model.addRow(parseLine(line));
         }

         line= bufferedReader.readLine();
      }

      return model;
   }

    public static void save(TableModel model, File file) throws IOException {
        save(model, new PrintWriter(new FileWriter(file)));
    }

    public static String save(TableModel model) {
        StringWriter out = new StringWriter();
        try {
            save(model, new PrintWriter(out));
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException("You didn't get this exception. It shouldn't be possible", e);
        }
    }

   /**
    * Save a TableModel to the specified file in CSV format.
    * <P>
    * If the hasHeadings property is true then the first line of the file is set
    * according to the TableModel column headings, instead of from the data. If
    * hasQuotes is true then quotes are automatically added to the start and end
    * of each field.
    */
    public static void save(TableModel model, PrintWriter out) throws IOException {
      // Write columns
      boolean first= true;

      if (hasHeadings) {
         for (int i= 0; i < model.getColumnCount(); i++) {
            if (first) {
               first= false;

               if (hasQuotes)
                  out.print("\"" + model.getColumnName(i) + "\"");
               else
                  out.print(model.getColumnName(i));
            }
            else {
               if (hasQuotes)
                  out.print(",\"" + model.getColumnName(i) + "\"");
               else
                  out.print("," + model.getColumnName(i));
            }
         }
         out.println();
      }

      // Write data
      for (int row= 0; row < model.getRowCount(); row++) {
         first= true;
         for (int col= 0; col < model.getColumnCount(); col++) {
            if (first) {
               first= false;

               if (hasQuotes)
                  out.print("\"" + model.getValueAt(row, col) + "\"");
               else
                  out.print(model.getValueAt(row, col));
            }
            else {
               if (hasQuotes)
                  out.print(",\"" + model.getValueAt(row, col) + "\"");
               else
                  out.print(","  + model.getValueAt(row, col));
            }
         }
         out.println();
      }

      out.close();
   }

   public static void main(String[] args)
   {
      try {
         TableModel model= CsvFileHandler.load(new URL(args[0]));
         CsvFileHandler.save(model, new File("output.csv"));

         JFrame frame = new JFrame();
         JTable table= new JTable(model);
         table.createDefaultColumnsFromModel();
         frame.getContentPane().add(new JScrollPane(table));

         frame.pack();
         frame.setVisible(true);

      }
      catch (Exception e)
         {
            System.err.println(e);
            e.printStackTrace();
         }
   }
}

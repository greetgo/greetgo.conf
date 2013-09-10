package kz.greetgo.msoffice.xlsx.gen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kz.greetgo.msoffice.UtilOffice;
import kz.greetgo.msoffice.xlsx.parse.SharedStrings;

public class Sheet {
  private final Style style;
  private final String name;
  private final String workDir;
  private final SharedStrings strs;
  private double defaultRowHeight = 15;
  private boolean summaryBelow = true;
  private boolean summaryRight = true;
  
  private int curRow = 0;
  private boolean inRow = false;
  
  private final Map<Integer, Double> widths = new HashMap<Integer, Double>();
  final boolean selected;
  private boolean started = false;
  private boolean finished = false;
  private PrintStream out;
  private final PageMargins pageMargins = new PageMargins();
  private String displayName;
  private final MergeCells mergeCells = new MergeCells();
  
  public PageMargins pageMargins() {
    return pageMargins;
  }
  
  public String getDisplayName() {
    return displayName;
  }
  
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
  
  public double getDefaultRowHeight() {
    return defaultRowHeight;
  }
  
  public void setDefaultRowHeight(double defaultRowHeight) {
    this.defaultRowHeight = defaultRowHeight;
  }
  
  public Sheet cellStr(int col, String strValue) {
    if (col <= 0) throw new IllegalArgumentException("must be col > 0, but col = " + col);
    init();
    style.clearNumFmt();
    
    if (strValue == null) {
      int styleIndex = style.index();
      String pos = UtilOffice.toTablePosition(curRow, col - 1);
      out.printf("<c r=\"%s\" s=\"%d\" t=\"s\"></c>", pos, styleIndex);
      out.println();
    } else {
      int styleIndex = style.index();
      int strIndex = strs.index(strValue);
      String pos = UtilOffice.toTablePosition(curRow, col - 1);
      out.printf("<c r=\"%s\" s=\"%d\" t=\"s\"><v>%d</v></c>", pos, styleIndex, strIndex);
      out.println();
    }
    return this;
  }
  
  public Sheet cellInt(int col, int intValue) {
    if (col <= 0) throw new IllegalArgumentException("must be col > 0, but col = " + col);
    init();
    style.clearNumFmt();
    
    int styleIndex = style.index();
    String pos = UtilOffice.toTablePosition(curRow, col - 1);
    out.printf("<c r=\"%s\" s=\"%d\"><v>%d</v></c>", pos, styleIndex, intValue);
    out.println();
    return this;
  }
  
  public Sheet cellDouble(int col, double doubleValue, NumFmt numFmt) {
    if (col <= 0) throw new IllegalArgumentException("must be col > 0, but col = " + col);
    init();
    style.numFmt = numFmt;
    
    int styleIndex = style.index();
    String pos = UtilOffice.toTablePosition(curRow, col - 1);
    out.printf("<c r=\"%s\" s=\"%d\"><v>" + doubleValue + "</v></c>", pos, styleIndex);
    out.println();
    return this;
  }
  
  public Sheet cellDouble(int col, double doubleValue) {
    if (col <= 0) throw new IllegalArgumentException("must be col > 0, but col = " + col);
    init();
    style.clearNumFmt();
    
    int styleIndex = style.index();
    String pos = UtilOffice.toTablePosition(curRow, col - 1);
    out.printf("<c r=\"%s\" s=\"%d\"><v>" + doubleValue + "</v></c>", pos, styleIndex);
    out.println();
    return this;
  }
  
  private Sheet cellDate(int col, Date date, NumFmt numFmt) {
    if (col <= 0) throw new IllegalArgumentException("must be col > 0, but col = " + col);
    init();
    style.numFmt = numFmt;
    
    int styleIndex = style.index();
    String pos = UtilOffice.toTablePosition(curRow, col - 1);
    out.printf("<c r=\"%s\" s=\"%d\"><v>%s</v></c>", pos, styleIndex,
        UtilOffice.toExcelDateTime(date));
    out.println();
    return this;
  }
  
  public Sheet cellYMD(int col, Date date) {
    return cellDate(col, date, NumFmt.YMD);
  }
  
  public Sheet cellYMD_HMS(int col, Date date) {
    return cellDate(col, date, NumFmt.YMD_HMS);
  }
  
  public Sheet cellDMY(int col, Date date) {
    return cellDate(col, date, NumFmt.DMY);
  }
  
  public void skipRows(int rowCount) {
    if (rowCount <= 0) {
      throw new IllegalArgumentException("rowCount must be > 0");
    }
    init();
    innerFinishRow();
    curRow += rowCount;
  }
  
  /**
   * Instead use
   * 
   * <pre>
   * row().finish()
   * </pre>
   */
  @Deprecated
  public void finishRow() {
    innerFinishRow();
  }
  
  public void innerFinishRow() {
    if (!inRow) return;
    out.println("</row>");
    inRow = false;
  }
  
  public void skipRow() {
    init();
    skipRows(1);
  }
  
  /**
   * Instead use
   * 
   * <pre>
   * row().start()
   * </pre>
   */
  @Deprecated
  public void startRow() {
    row().start();
  }
  
  /**
   * Instead use
   * 
   * <pre>
   * row().height(123).start()
   * </pre>
   */
  @Deprecated
  public void startRow(double height) {
    row().height(height).start();
  }
  
  public class Row {
    private Double height = null;
    private boolean collapsed = false;
    private boolean hidden = false;
    private int outlineLevel = 0;
    
    private Row() {}
    
    public Row height(double height) {
      this.height = height;
      return this;
    }
    
    public Row collapsed() {
      collapsed = true;
      return this;
    }
    
    public Row hidden() {
      hidden = true;
      return this;
    }
    
    public Row hidden(boolean hidden) {
      this.hidden = hidden;
      return this;
    }
    
    public Row collapsed(boolean collapsed) {
      this.collapsed = collapsed;
      return this;
    }
    
    public Row outline() {
      outlineLevel = 1;
      return this;
    }
    
    public Row outline(boolean hidden) {
      outlineLevel = 1;
      this.hidden = hidden;
      return this;
    }
    
    public Row hiddenOutline() {
      outlineLevel = 1;
      hidden = true;
      return this;
    }
    
    public Row outlineLevel(int outlineLevel) {
      if (outlineLevel < 1) throw new IllegalArgumentException("outlineLevel must be > 0");
      this.outlineLevel = outlineLevel;
      return this;
    }
    
    public Row outlineLevel(int outlineLevel, boolean hidden) {
      if (outlineLevel < 1) throw new IllegalArgumentException("outlineLevel must be > 0");
      this.hidden = hidden;
      this.outlineLevel = outlineLevel;
      return this;
    }
    
    public Row start() {
      init();
      innerFinishRow();
      
      StringBuilder after = new StringBuilder();
      out.printf("<row r=\"%d\"", ++curRow);
      if (height != null) {
        after.append(" customHeight=\"1\"");
        out.printf(" ht=\"%s\"", height.toString());
      }
      if (hidden) {
        out.print(" hidden=\"1\"");
      }
      if (outlineLevel > 0) {
        out.printf(" outlineLevel=\"%d\"", outlineLevel);
      }
      if (collapsed) {
        out.print(" collapsed=\"1\"");
      }
      out.print(after.toString());
      out.println('>');
      
      inRow = true;
      
      return this;
    }
    
    public void finish() {
      innerFinishRow();
    }
  }
  
  public Row row() {
    return new Row();
  }
  
  public void cleanWidths() {
    checkNotStarted();
    widths.clear();
  }
  
  private void checkNotStarted() {
    if (started) {
      throw new IllegalStateException("Cannot change widths when data filling has begun");
    }
  }
  
  public void setWidth(int col, double width) {
    if (col <= 0) throw new IllegalArgumentException("must be: col > 0, but col = " + col);
    checkNotStarted();
    widths.put(col, width);
  }
  
  public Style style() {
    return style;
  }
  
  Sheet(Styles styles, int sheetNo, String workDir, SharedStrings strs, boolean selected) {
    this.name = "sheet" + sheetNo;
    this.displayName = "Лист" + sheetNo;
    this.workDir = workDir;
    this.strs = strs;
    this.selected = selected;
    
    style = new Style(styles);
  }
  
  public String name() {
    return name;
  }
  
  private final void init() {
    if (started) return;
    try {
      initEx();
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException)e;
      }
      throw new RuntimeException(e);
    }
  }
  
  private void initEx() throws Exception {
    if (started) {
      throw new IllegalArgumentException("Cannot start already started sheet");
    }
    if (finished) {
      throw new IllegalArgumentException("Cannot start already finished sheet");
    }
    {
      File dir = new File(workDir + "/xl/worksheets");
      dir.mkdirs();
      out = new PrintStream(new FileOutputStream(dir.getPath() + "/" + name + ".xml"), true,
          "UTF-8");
      printHeader();
    }
    started = true;
  }
  
  private void printHeader() {
    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
    out.println("<worksheet xmlns=\"http://schemas.openxmlformats.org/"
        + "spreadsheetml/2006/main\""
        + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
    printSheetPr();
    printSelected();
    out.println("<sheetFormatPr defaultRowHeight=\"" + defaultRowHeight + "\" />");
    printWidths();
    out.println("<sheetData>");
  }
  
  private void printSheetPr() {
    out.println("<sheetPr>");
    out.println("<outlinePr summaryBelow=\"" + (summaryBelow ? 1 :0) + "\" summaryRight=\""
        + (summaryRight ? 1 :0) + "\" />");
    
    out.println("</sheetPr>");
  }
  
  private void printSelected() {
    if (selected) {
      out.println("<sheetViews>");
      out.println("<sheetView tabSelected=\"1\" workbookViewId=\"0\">");
      out.println("<selection activeCell=\"A1\" sqref=\"A1\" />");
      out.println("</sheetView>");
      out.println("</sheetViews>");
    } else {
      out.println("<sheetViews>");
      out.println("<sheetView workbookViewId=\"0\" />");
      out.println("</sheetViews>");
    }
  }
  
  private void printWidths() {
    if (widths.size() == 0) return;
    List<Integer> keys = new ArrayList<Integer>();
    keys.addAll(widths.keySet());
    Collections.sort(keys);
    out.println("<cols>");
    for (Integer key : keys) {
      out.printf("<col min=\"%d\" max=\"%d\" width=\"%s\" customWidth=\"1\" />", key, key, widths
          .get(key).toString());
    }
    out.println("</cols>");
  }
  
  void finish() {
    out.println("</sheetData>");
    mergeCells.print(out);
    pageMargins.print(out);
    out.println("</worksheet>");
  }
  
  public void addMergeInRow(int colFrom, int colTo) {
    mergeCells.addMerge(curRow, colFrom, curRow, colTo);
  }
  
  public void addMerge(int rowFrom, int colFrom, int rowTo, int colTo) {
    mergeCells.addMerge(rowFrom, colFrom, rowTo, colTo);
  }
  
  public void addMergeRel(int rowFrom, int colFrom, int rowTo, int colTo) {
    mergeCells.addMerge(curRow + rowFrom, colFrom, curRow + rowTo, colTo);
  }
  
  public void addMergeCount(int colFrom, int colTo, int rowsCount) {
    if (rowsCount > 0) {
      mergeCells.addMerge(curRow, colFrom, curRow + rowsCount, colTo);
    } else {
      mergeCells.addMerge(curRow + rowsCount, colFrom, curRow, colTo);
    }
  }
  
  public boolean isSummaryBelow() {
    return summaryBelow;
  }
  
  public void setSummaryBelow(boolean summaryBelow) {
    this.summaryBelow = summaryBelow;
  }
  
  public boolean isSummaryRight() {
    return summaryRight;
  }
  
  public void setSummaryRight(boolean summaryRight) {
    this.summaryRight = summaryRight;
  }
}
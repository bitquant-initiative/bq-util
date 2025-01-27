package bq.sql;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import com.google.common.collect.Lists;

import bq.util.BqException;

public class TablePrinter implements ResultSetProcessor<String> {

  TablePrinter() {
    super();
  }

  public static TablePrinter create() {
    return new TablePrinter();
  }
  
  public String toString(Results r) {
    return toString(r.getResultSet());
  }
  public String toString(ResultSet rs) {
    try {
      StringWriter sw = new StringWriter();
      render(rs, sw);
      sw.close();
      return sw.toString();
    } catch (IOException e) {
      throw new BqException(e);
    }
  }

  String truncate(String input, int len) {
    if (input == null) {
      return "";
    }
    if (input.length() > len) {
      return input.substring(0, len);
    }
    return input;
  }

  public void render(ResultSet rs, Writer output) {
    try {

      int show = 20;
      int truncatedAt = show / 2;
      List<List<String>> rows = Lists.newArrayList();
      try {
        ResultSetMetaData md = rs.getMetaData();
        int columnCount = md.getColumnCount();
        List<String> columns = Lists.newArrayList();
        columns.add("#");
        for (int i = 1; i <= columnCount; i++) {
          columns.add(md.getColumnName(i));
        }

        int rowCount = 0;
        while (rs.next()) {

          List<String> row = Lists.newArrayList();
          row.add("" + rowCount);
          for (int i = 1; i <= columnCount; i++) {
            String val = rs.getString(i);
            if (val == null) {
              val = "";
            }
            row.add(val);
          }
          rows.add(row);
          if (rows.size() > show) {
            rows.remove(show / 2);
          }
          rowCount++;
        }

        List<Integer> maxLengths = Lists.newArrayList();
        for (int i = 0; i < columns.size(); i++) {
          int max = Math.max(0, columns.get(i).length());
          for (List<String> row : rows) {

            max = Math.max(max, row.get(i).length());
            max = Math.max(2, max);
          }
          maxLengths.add(max);
        }

        StringWriter sw = new StringWriter();

        for (int i = 0; i <= columnCount; i++) {
          String val = center(columns.get(i), maxLengths.get(i));
          if (i == 0) {
            sw.append("|  ");
          } else {
            sw.append("  |  ");
          }
          sw.append(val);
        }
        sw.append("  |");

        String headerLine = sw.toString();

        output.append("\n");

        int totalWidth = headerLine.length();
        for (int i = 0; i < totalWidth; i++) {
          output.append("-");
        }
        output.append("\n");
        output.append(headerLine);
        output.append("\n");
        for (int i = 0; i < totalWidth; i++) {
          output.append("-");
        }
        output.append("\n");

        for (int i = 0; i < rows.size(); i++) {

          List<String> row = rows.get(i);
          if (i == truncatedAt) {
            for (int j = 0; j < row.size(); j++) {
              if (j == 0) {
                output.append("|  ");
              } else {
                output.append("  |  ");
              }
              output.append(center("...", maxLengths.get(j)));
            }
            output.append("  |\n");
          }

          for (int j = 0; j < row.size(); j++) {
            if (j == 0) {
              output.append("|  ");
            } else {
              output.append("  |  ");
            }
            int w = maxLengths.get(j);
            String val = lpad(row.get(j), maxLengths.get(j));
            output.append(val);
          }
          output.append("  |\n");
        }

        for (int i = 0; i < totalWidth; i++) {
          output.append("-");
        }
        output.append("\n");

        output.flush();

      } catch (SQLException e) {
        throw new DbException(e);
      }
    } catch (IOException e) {
      throw new BqException(e);
    }
  }

  String lpad(String s, int total) {
    StringBuffer sb = new StringBuffer();
    if (s == null) {
      s = "";
    }
    s = truncate(s, total);

    int pc = total - s.length();

    for (int i = 0; pc > 0 && i < pc; i++) {
      sb.append(" ");
    }
    sb.append(s);

    return sb.toString();
  }

  String center(String s, int total) {
    StringBuffer sb = new StringBuffer();
    if (s == null) {
      s = "";
    }
    s = truncate(s, total);
    int totalPadding = total - s.length();

    int lpad = totalPadding / 2;
    int rpad = (total - lpad) - s.length();

    for (int i = 0; i < lpad; i++) {
      sb.append(' ');
    }
    sb.append(s);
    for (int i = 0; i < rpad; i++) {
      sb.append(' ');
    }

    return sb.toString();
  }

  @Override
  public <T> T process(Results rs) throws SQLException {
    return (T) toString(rs);
  
  }
}

package bq.util;

import java.awt.geom.QuadCurve2D;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class Symbol {

  String qualifier;
  String ticker;

  static Set<String> validQualifiers = Set.of("S", "I", "Q", "X");

  public String getTicker() {
    return ticker;
  }

  public Optional<String> getQualifier() {
    return S.notBlank(qualifier);
  }

  public boolean isIndex() {
    return getQualifier().orElse("").equals("I");
  }

  public boolean isStock() {
    return getQualifier().orElse("").equals("S");
  }

  public boolean isCrypto() {
    return getQualifier().orElse("").equals("X");
  }

  public boolean isIndicator() {
    return getQualifier().orElse("").equals("Q");
  }

  public static Symbol parseTableName(final String input) {
    String s = input;
    if (S.isBlank(s)) {
      throw new IllegalArgumentException("unable to parse symbol: " + input);
    }
    if (s.toLowerCase().endsWith(".csv")) {
      s = s.substring(0, s.length() - 4);
    }

    String qualifier = null;
    List<String> parts = Lists.newArrayList(Splitter.on("_").splitToList(s.toUpperCase()));
    if (parts.size()>1) {
     qualifier = parts.removeFirst();
     if (!validQualifiers.contains(qualifier)) {
       throw new IllegalArgumentException("invalid qualifier: " + input);
     }
    }

   

    Symbol symbol = new Symbol();
    symbol.qualifier = qualifier;
    symbol.ticker = Joiner.on(".").join(parts);
    return symbol;
  }

  public static Symbol parse(String input) {
    if (S.isBlank(input)) {
      throw new IllegalArgumentException("invalid symbol: " + input);
    }
   
      Symbol s = new Symbol();
      List<String> parts = Splitter.on(":").trimResults().splitToList(input);
      if (parts.size() == 1) {
        s.ticker = parts.get(0).toUpperCase();
        return s;
      } else if (parts.size() == 2) {

        s.qualifier = parts.get(0).toUpperCase();
        if (!validQualifiers.contains(s.qualifier)) {
          throw new IllegalArgumentException("invalid qualfiier: " + s.qualifier);
        }
        s.ticker = parts.get(1).toUpperCase();
        if (S.isBlank(s.qualifier)) {
          throw new IllegalArgumentException("invalid symbol: " + input);
        }
        if (S.isBlank(s.ticker)) {
          throw new IllegalArgumentException("invalid symbol: " + input);
        }
        return s;
      }
    

    throw new IllegalArgumentException("invalid symbol: " + input);

  }

  public String getTableName() {
    if (getQualifier().isEmpty()) {
      throw new BqException("no qualfiier: " + toString());
    }
    return String.format("%s_%s", getQualifier().get().toUpperCase(), getTicker().toUpperCase());
  }


  public String toSymbol(String table) {

    String symbol = table.toUpperCase().trim();

    List<String> s = Lists.newArrayList(Splitter.on("_").splitToList(symbol));
    String qualifier = s.removeFirst();

    return String.format("%s:%s", symbol, Joiner.on(".").join(s));

  }

  private String getTypeName() {
    if (isCrypto()) {
      return "crypto";
    }
    if (isStock()) {
      return "stocks";
    }
    if (isIndicator()) {
      return "indicators";
    }
    if (isIndex()) {
      return "indices";
    }
    throw new IllegalStateException("unknown symbol type");
  }
  
  public String toCsvS3Key() {
    return String.format("%s/1d/%s.csv", getTypeName(), getTicker());
  }
  public String toString() {
    if (S.isBlank(qualifier)) {
      return ticker;
    } else {
      return String.format("%s:%s", qualifier, ticker);
    }
  }
}

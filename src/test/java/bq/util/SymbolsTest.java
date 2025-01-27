package bq.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class SymbolsTest {

  @Test
  public void testIt() {
    
    Assertions.assertThat(Symbol.parse("S:MSTR").getTicker()).isEqualTo("MSTR");
    Assertions.assertThat(Symbol.parse("S:MSTR").getQualifier().get()).isEqualTo("S");
    Assertions.assertThat(Symbol.parse("MSTR").getTicker()).isEqualTo("MSTR");
    Assertions.assertThat(Symbol.parse("MSTR").getQualifier()).isEmpty();
   
    Assertions.assertThat(Symbol.parse(" S:MSTR ").getTicker()).isEqualTo("MSTR");
    Assertions.assertThat(Symbol.parse(" S:MSTR ").getQualifier().get()).isEqualTo("S");
    Assertions.assertThat(Symbol.parse(" MSTR ").getTicker()).isEqualTo("MSTR");
    Assertions.assertThat(Symbol.parse(" MSTR ").getQualifier()).isEmpty();
    
    checkException(":MSTR");
    checkException("S:");
    checkException("");
    checkException(null);
    
    Assertions.assertThat(Symbol.parse("X:BTC").getTableName()).isEqualTo("X_BTC");
     
    Assertions.assertThat(Symbol.parseTableName("X_BTC").getQualifier().get()).isEqualTo("X");
    Assertions.assertThat(Symbol.parseTableName("BTC").toString()).isEqualTo("BTC");
    Assertions.assertThat(Symbol.parseTableName("x_btc").toString()).isEqualTo("X:BTC");
    Assertions.assertThat(Symbol.parseTableName("Q_BTC_MVRVZ").toString()).isEqualTo("Q:BTC.MVRVZ");
    Assertions.assertThat(Symbol.parseTableName("Q_BTC_MvrvZ.csv").toString()).isEqualTo("Q:BTC.MVRVZ");
    
    
    Assertions.assertThat(Symbol.parse("X:BTC").toCsvS3Key()).isEqualTo("crypto/1d/BTC.csv");
    Assertions.assertThat(Symbol.parse("I:SPX").toCsvS3Key()).isEqualTo("indices/1d/SPX.csv");
    Assertions.assertThat(Symbol.parse("S:MSTR").toCsvS3Key()).isEqualTo("stocks/1d/MSTR.csv");
    Assertions.assertThat(Symbol.parse("Q:BTC.MVRVZ").toCsvS3Key()).isEqualTo("indicators/1d/BTC.MVRVZ.csv");
  }
  
  void checkException(String input) {
    try {
      Symbol.parse(input);
      Assertions.failBecauseExceptionWasNotThrown(BqException.class);
    }
    catch (IllegalArgumentException e) {
      // ok
    }
  }
}

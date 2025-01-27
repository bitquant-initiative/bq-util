package bq.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetProcessor<T> {

  public <T> T process(Results rs) throws SQLException;
  
}

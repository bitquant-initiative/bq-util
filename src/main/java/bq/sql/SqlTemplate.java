package bq.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class SqlTemplate {

  Supplier<Connection> connectionSupplier = null;

  static Supplier<Connection> globalSupplier = null;

  public static SqlTemplate create(Supplier<Connection> cs) {
    Preconditions.checkNotNull(cs);
    SqlTemplate t = new SqlTemplate();
    t.connectionSupplier = cs;
    return t;
  }

  public static SqlTemplate create(DataSource ds) {
    Preconditions.checkNotNull(ds);

    Supplier<Connection> cs = new Supplier<Connection>() {

      @Override
      public Connection get() {
        try {
          return ds.getConnection();
        } catch (SQLException e) {
          throw new DbException(e);
        }
      }

    };
    return create(cs);
  }

  private static Supplier<Connection> supplier(DataSource ds) {
    Supplier<Connection> cs = new Supplier<Connection>() {

      @Override
      public Connection get() {
        try {
          return ds.getConnection();
        } catch (SQLException e) {
          throw new DbException(e);
        }
      }

    };
    return cs;
  }

  public static void setGlobalDataSource(DataSource ds) {
    Preconditions.checkState(globalSupplier == null);
    globalSupplier = supplier(ds);
  }

  public static void setGlobalDataSource(Supplier<Connection> ds) {
    Preconditions.checkState(globalSupplier == null);
    globalSupplier = ds;
  }

  public static SqlTemplate create() {
    Preconditions.checkState(globalSupplier != null);
    return SqlTemplate.create(globalSupplier);
  }

  private Connection getConnection() {
    return this.connectionSupplier.get();
  }

  public String queryString(Consumer<StatementBuilder> builder) {
    Optional<String> r = queryFirst(builder, m -> {
      Optional<String> v = m.getString(1);
      if (v.isEmpty()) {
        return null;
      }
      return v.get();
    });
    if (r.isEmpty()) {
      throw new DbException("queryString expected a result");
    }
    return r.get();
  }

  public int queryInt(Consumer<StatementBuilder> builder) {
    Optional<Integer> r = queryFirst(builder, m -> {
      Optional<Integer> v = m.getInt(1);
      if (v.isEmpty()) {
        return 0;
      }
      return v.get();
    });
    if (r.isEmpty()) {
      throw new DbException("queryInt expected a result");
    }
    return r.get();
  }


  public <T> Optional<T> queryFirst(Consumer<StatementBuilder> builder, RowMapper mapper) {
    List<T> x = query(builder, mapper);
    if (x.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(x.getFirst());
  }

  public <T> T queryResult(Consumer<StatementBuilder> builder, ResultSetProcessor processor) {
    Preconditions.checkNotNull(builder, "builder cannot be null");
    Preconditions.checkNotNull(processor, "ResultSetProcessor cannot be null");
    StatementBuilder b = StatementBuilder.create();

    builder.accept(b);

    String sql = b.getSql();

    try (SqlCloser closer = SqlCloser.create()) {
      Connection c = getConnection();
      closer.register(c);

      PreparedStatement ps = c.prepareStatement(sql);
      closer.register(ps);

      b.bind(ps);

      ResultSet rs = ps.executeQuery();
      Results rsx = Results.create(rs);
      return (T) processor.process(rsx);
    }
    catch (SQLException e) {
      throw new DbException(e);
    }
   }
  
 
  public <T> List<T> query(Consumer<StatementBuilder> builder, RowMapper mapper) {

    Preconditions.checkNotNull(builder, "builder cannot be null");
    Preconditions.checkNotNull(mapper, "mapper cannot be null");

    StatementBuilder b = StatementBuilder.create();

    builder.accept(b);

    String sql = b.getSql();

    try (SqlCloser closer = SqlCloser.create()) {
      Connection c = getConnection();
      closer.register(c);
      List<T> results = Lists.newArrayList();
      PreparedStatement ps = c.prepareStatement(sql);
      closer.register(ps);

      b.bind(ps);

      ResultSet rs = ps.executeQuery();
      closer.register(rs);
      Results r = Results.create(rs);
      while (r.next()) {
        T t = (T) mapper.map(r);
        results.add(t);
        ;
      }
      return results;
    } catch (SQLException e) {
      throw new DbException(e);
    }

  }

  public boolean execute(Consumer<StatementBuilder> builder) {
    Preconditions.checkNotNull(builder);
    StatementBuilder b = StatementBuilder.create();

    builder.accept(b);

    String sql = b.getSql();

    try (SqlCloser closer = SqlCloser.create()) {
      Connection c = getConnection();
      closer.register(c);

      PreparedStatement ps = c.prepareStatement(sql);
      closer.register(ps);

      b.bind(ps);

      return ps.execute();
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  private List<Object> toList(Object... args) {
    if (args == null) {
      return Lists.newArrayList();
    }
    List<Object> list = Lists.newArrayList();
    for (Object v : args) {
      list.add(v);
    }
    return list;
  }

  public int executeUpdate(Consumer<StatementBuilder> builder) {
    Preconditions.checkNotNull(builder);
    StatementBuilder b = StatementBuilder.create();

    builder.accept(b);

    String sql = b.getSql();

    try (SqlCloser closer = SqlCloser.create()) {
      Connection c = getConnection();
      closer.register(c);

      PreparedStatement ps = c.prepareStatement(sql);
      closer.register(ps);

      b.bind(ps);

      return ps.executeUpdate();
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  ////////
  ////////
  ////////
  ///
  ///

  public boolean execute(String sql) {
    return execute(c -> {
      c.sql(sql);
    });
  }

  public boolean execute(String sql, Object v0) {
    return execute(c -> {
      c.sql(sql);
      c.bind(1, v0);
    });
  }

  public boolean execute(String sql, Object v1, String v2) {
    return execute(c -> {
      c.sql(sql);
      c.bind(1, v1);
      c.bind(2, v2);
    });
  }

  public int executeUpdate(String sql) {

    return executeUpdate(b -> {
      b.sql(sql);
    });
  }
  public int executeUpdate(String sql, Object v1) {

    return executeUpdate(b -> {
      b.sql(sql);
      b.bind(1, v1);
    });
  }
  public int executeUpdate(String sql, Object v1, Object v2) {

    return executeUpdate(b -> {
      b.sql(sql);
      b.bind(1, v1);
      b.bind(2, v2);
    });
  }
 
}

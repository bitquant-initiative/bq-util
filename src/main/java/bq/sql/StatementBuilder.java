package bq.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class StatementBuilder {

  static final Pattern parsePattern = Pattern.compile("(.*?)\\{\\{(.*?)\\}\\}(.*)");
  List<String> fragments = Lists.newArrayList();
  int index = 0;
  List<String> paramNames = Lists.newArrayList();

  Map<String, Object> bindings = Maps.newHashMap();

  List<Consumer<Statement>> deferredBindings = Lists.newArrayList();

  private StatementBuilder() {

  }

  public static StatementBuilder create() {
    return new StatementBuilder();
  }

  public StatementBuilder sql(String sql) {

    return sqlWithBindings(sql, List.of());
  }

  public StatementBuilder sql(String sql, Object v1) {

    return sqlWithBindings(sql, Lists.newArrayList(v1));
  }

  public StatementBuilder sql(String sql, Object v1, Object v2) {

    return sqlWithBindings(sql, Lists.newArrayList(v1, v2));
  }

  public StatementBuilder sql(String sql, Object v1, Object v2, Object v3) {

    return sqlWithBindings(sql, Lists.newArrayList(v1, v2, v3));
  }

  public StatementBuilder sql(String sql, Object v1, Object v2, Object v3, Object v4) {

    return sqlWithBindings(sql, Lists.newArrayList(v1, v2, v3, v4));
  }

  public StatementBuilder bind(int pos, Object val) {
    this.bindings.put(toKey(pos), val);
    return this;
  }

  public StatementBuilder bind(String name, Object val) {
    this.bindings.put(name, val);
    return this;
  }

  public StatementBuilder bind(Consumer<Statement> deferredBinding) {
    Preconditions.checkArgument(deferredBinding != null);
    deferredBindings.add(deferredBinding);
    return this;
  }

  StatementBuilder sqlWithBindings(String s, List<Object> bindVals) {

    if (s == null) {
      s = "";
    }

    if (bindVals == null) {
      bindVals = Lists.newArrayList();
    }

    List<Object> newList = Lists.newArrayList();
    newList.addAll(bindVals);
    bindVals = newList;

    int expectedTotalParamCount = paramNames.size() + bindVals.size();

    Matcher m = parsePattern.matcher(s);
    if (m.matches()) {
      String pre = m.group(1);
      String paramName = m.group(2).trim();
      String post = m.group(3);

      sqlWithBindings(pre, null);
      if (bindVals.isEmpty()) {
        addParam(paramName);
      } else {

        addParam(paramName, bindVals.removeFirst());
      }

      sqlWithBindings(post, bindVals);
    } else {
      fragments.add(s);
    }

    if (expectedTotalParamCount != paramNames.size()) {
      if (bindVals.size() == 0) {
        // if no positional parameters were provided, it is not a problem
        // that there are unbound parameters. They may be bound at a later time.
      } else {
        // BUT, if positional parameters were supplied, they need to match
        throw new DbException(String.format("expected %d bind values but got %d sql=<%s>",
            expectedTotalParamCount - bindVals.size(), bindVals.size(), s));
      }
    }

    return this;
  }

  private String toKey(int index) {
    return String.format("_%s", index);
  }

  private void addParam(String name) {
    paramNames.add(name);
    fragments.add("?");
  }

  private void addParam(String name, Object orderedVal) {

    addParam(name);

    bindings.put(toKey(paramNames.size()), orderedVal);

  }

  public String getSql() {
    String sql = "";
    for (String fragment : fragments) {
      boolean addBlank = true;

      fragment = fragment.trim();
      if (sql.isBlank() || sql.endsWith(" ") || fragment.startsWith(" ")) {
        addBlank = false;
      } else if (fragment.startsWith("?")) {
        if (sql.endsWith("=") || sql.endsWith(",")) {
          addBlank = false;
        }
      } else if (sql.endsWith("?")) {
        if (fragment.startsWith(" ")) {

        }
      }

      if (addBlank) {
        sql += " ";
      }
      sql += fragment;
    }
    return sql.trim();
  }

  public StatementBuilder bind(PreparedStatement ps) throws SQLException {
    for (int i = 0; i < paramNames.size(); i++) {
      int index = i + 1;
      String paramName = paramNames.get(i);

      Object val = null;

      if (bindings.containsKey(paramName)) {
        val = bindings.get(paramName);
      } else {
        val = bindings.get(toKey(index));
      }

      Object convertedVal = SqlUtil.toSqlBindType(val);

      ps.setObject(index, convertedVal);

      
      // now apply and deferred bindings that might be set
      for (Consumer<Statement> deferred : deferredBindings) {

        deferred.accept(ps);
      }

    }

    return this;
  }
}

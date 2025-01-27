package bq.sql;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

public class StatementBuilderTest {

  
 @Test
 public void testIt() {
   StatementBuilder b =  StatementBuilder.create().sql("select * from  test where foo={{bar}} and fizz={{buzz}}");
   
   Assertions.assertThat(b.getSql()).isEqualTo("select * from  test where foo=? and fizz=?");
   Assertions.assertThat(b.paramNames).containsExactly("bar","buzz");
   Assertions.assertThat(b.bindings).isEmpty();
 }
 @Test
 public void testIt3() {
   StatementBuilder b =  StatementBuilder.create().sql("select * from  test where foo={{ bar}} and fizz={{buzz }}");
   
   Assertions.assertThat(b.getSql()).isEqualTo("select * from  test where foo=? and fizz=?");
   Assertions.assertThat(b.paramNames).containsExactly("bar","buzz");
   Assertions.assertThat(b.bindings).isEmpty();
 }
 
 @Test
 public void testIt2() {
   StatementBuilder b =  StatementBuilder.create().sql("select * from  test");
   
   b = b.sql("where foo={{bar}}","foo_val");
   b = b.sql("and fizz={{buzz}}");
   
   Assertions.assertThat(b.getSql()).isEqualTo("select * from  test where foo=? and fizz=?");
   Assertions.assertThat(b.paramNames).containsExactly("bar","buzz");
   Assertions.assertThat(b.bindings).containsEntry("_1", "foo_val").hasSize(1);
 }
 
 @Test
 public void testIt4() {
   StatementBuilder b =  StatementBuilder.create().sql("select * from  test");
   
   b.sql("where foo={{foo_val}}","foo_val");
   b.sql("and fizz={{fizz_val}}");
   b.sql(" and baz={{baz_val}}", null);
   
   
   Assertions.assertThat(b.getSql()).isEqualTo("select * from  test where foo=? and fizz=? and baz=?");
   Assertions.assertThat(b.paramNames).containsExactly("foo_val","fizz_val","baz_val");
   Assertions.assertThat(b.bindings).containsEntry("_1", "foo_val").containsEntry("_3", null).hasSize(2);
 }
}

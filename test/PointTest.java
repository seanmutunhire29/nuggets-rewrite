import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.Set;

/** 
 * tests for the Point Class
 * 11 tests for checking getters, equality, hash code, hashset, and to string
 * 
 * @author Sarah Parigela 11/09/26
 */

public class PointTest {

  @Test
  public void gettersReturnWhatWasGiven() {
    Point p = new Point(4, 7, '#');
    assertEquals(4, p.getRow());
    assertEquals(7, p.getCol());
    assertEquals('#', p.getSymbol());
  }

  @Test
  public void equalityIgnoresTheSymbol() {
    Point a = new Point(2,3,'.');
    Point b = new Point(2,3,'#');
    assertEquals(a,b);
  }

  @Test
  public void differentRowsAreNotEqual() {
    assertNotEquals(new Point(2,3,'.'), new Point(5,3,'.'));
  }

  @Test
  public void differentColumnsAreNotEqual(){
    assertNotEquals(new Point(2,3,'.'), new Point(2,4,'.'));
  }
  @Test
  public void rowAndColumnSwappedAreNotEqual(){
    assertNotEquals(new Point(2,3,'.'), new Point(3,2,'.'));
  }

  @Test 
  public void aPointIsNotEqualToNullOrString(){
    Point p = new Point(2,3,'.');
    assertFalse(p.equals(null));
    assertFalse(p.equals("(2,3)"));
  }

  @Test
  public void equalPointsHaveEqualHashCodes(){
    Point a = new Point(9,4,'.');
    Point b = new Point(9,4,'#');
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void pointsInTheSameRowWithDifferentColumnsHashDifferently(){
    assertNotEquals(new Point(2,3,'.').hashCode(), new Point(2,4,'.').hashCode());
  }

  @Test
  public void toStringPrintsCorrectly(){
    assertEquals("(2,3,'.')", new Point(2,3,'.').toString());
  }

  @Test
  public void aHashSetFindsAPointBuiltSeparately() {
    Set<Point> set = new HashSet<>();
    set.add(new Point(1, 1, '.'));
    assertTrue(set.contains(new Point(1, 1, '.')));
  }

  @Test
  public void aHashSetKeepsOnlyOneOfTwoEqualPoints() {
    Set<Point> set = new HashSet<>();
    set.add(new Point(4, 4, '.'));
    set.add(new Point(4, 4, '.'));
    assertEquals(1, set.size());
  }
  
}

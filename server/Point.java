/**
 * Point - defines the row,col location and type of a spot on the grid
 * @author Sarah Parigela 11/9/26
 */

public final class Point{
    /** The row of this point, counted from 0 at the top. */
    private final int row;
    /** The column of this point, counted from 0 at the left. */
    private final int col;
    /** The character at this place on the map. Not used by equals or hashCode. */
    private final char symbol;
    
    /**
     * Creates a point at a place on the grid.
     *
     * @param row    the row, counted from 0 at the top
     * @param col    the column, counted from 0 at the left
     * @param symbol the character shown at this place on the map, such as a wall or a room spot
     */
    public Point(int row, int col, char symbol){
        this.row = row;
        this.col = col;
        this.symbol = symbol;
    }

    /**
     * Returns the row of this point.
     *
     * @return the row, counted from 0 at the top
     */
    public int getRow(){
        return row;
    }

    /**
     * Returns the column of this point.
     *
     * @return the column, counted from 0 at the left
     */
    public int getCol(){
        return col;
    }

    /**
     * Returns the map symbol at this point. Two points in the same place are equal even if
     * their symbols differ, so the symbol is not part of equals or hashCode.
     *
     * @return the character at this place on the map
     */
    public char getSymbol(){
        return symbol;
    }

    /** equals - checks if two points exist on the same location on the grid,                  
     * @param o object to compare to a Point                       
     * @return is o a Point with the same row and col? t/f
     */
    @Override
    public boolean equals(Object o){
        if (!(o instanceof Point)){
            return false;
        }
        else{
            Point other = (Point) o;
            return row == other.row && col == other.col;
        }
    }


    /**
     * Returns a hash code built from the row and column only, so it always agrees with
     * {@link #equals(Object)}.
     *
     * @return the hash code for this place on the grid
     */
    @Override
    public int hashCode(){
        return 31 * row + col;
    }
   
    /** toString - turns info into human readable information                 
     * @return string of a Point and its definition
     */
    @Override
    public String toString(){
        return "(" + row + "," + col + ",'"+ symbol + "')";
    }
    
}


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Grid - describes a 2d array and what can exist on it for the playing of a Nuggets game
 * @author Mia Jacobson 11/9/26
 */

public class Grid{
  
    public static final char ROOM_SPOT = '.';
    public static final char PASSAGE_SPOT = '#';
    public static final char HORIZONTAL_BOUNDARY = '-';
    public static final char VERTICAL_BOUNDARY = '|';
    public static final char CORNER_BOUNDARY = '+';
    public static final char ROCK = ' ';

    private static final int GOLD_TOTAL = 250;
    private static final int GOLD_MIN_PILES = 10;
    private static final int GOLD_MAX_PILES = 30;

    private int goldTotal = GOLD_TOTAL;
    private int minPiles = GOLD_MIN_PILES;
    private int maxPiles = GOLD_MAX_PILES;

    private  List<Point> roomSpotsList;
    private  Map<Point, Integer> goldMap;
    private int goldRemaining;
    private final Point[][] map;

    private int numRows;
    private int numCols;
    private int numPiles;

    //constructor
    public Grid(String mapPath, Map<String, Integer> gameDefaults) throws IOException { //read map
        for (String key: gameDefaults.keySet()){
            switch (key){
                case "gold":
                    goldTotal = gameDefaults.get("gold");
                    break;
                case "minpiles":
                    minPiles = gameDefaults.get("minpiles");
                    break;
                case "maxpiles":
                    maxPiles = gameDefaults.get("maxpiles");
                    break;
            }
        }

        List<String> lines = Files.readAllLines(Path.of(mapPath));
        numRows = lines.size();
        numCols = 0;
        int longestLine = 0;
        for (String line : lines){
            if(line.length() > longestLine){
                longestLine = line.length();
            }
        }
        numCols = longestLine;

        map = new Point[numRows][numCols]; //initialize data structures
        roomSpotsList = new ArrayList<>();
        goldMap = new HashMap<>();
        goldRemaining = 0;

        for (int r = 0; r < numRows; r++) { // fill map with symbols
            for (int c = 0; c < numCols; c++) {
                String current = lines.get(r);
                char symbol;
                if(c<current.length()){
                    symbol = current.charAt(c);
                }
                else{
                    symbol = ROCK;
                }
                map[r][c] = new Point(r,c,symbol);

                if(symbol == ROOM_SPOT){
                    roomSpotsList.add(map[r][c]);
                }
            }
        }
    }

    //getters
    public int getNumRows(){
        return numRows;
    }

    public int getNumCols(){
        return numCols;
    }

    public int getNumPiles(){
        return numPiles;
    }

    public int getGoldRemaining(){
        return goldRemaining;
    }

    public List<Point> getRoomSpotsList(){
        return roomSpotsList;
    } 
    
    public int getMaxPiles(){
        return maxPiles;
    } 
    
    public int getMinPiles(){
        return minPiles;
    } 
    
    public int getGoldTotal(){
        return goldTotal;
    } 

    /** isInBounds - checks if a r,c pair is on the grid                 
     * @param r	row of position to be checked                             
     * @param c	column of position to be checked                                                              
     * @return is r less than total number of rows and >= 0,
     * and is c less than total number of cols >= 0? t/f
     */

    public boolean isInBounds(int r, int c){
        return(r<numRows && c<numCols && r>=0 && c>=0);
    }

    /** placeGold - drops minPiles to maxPiles piles of gold on distinct room spots, 
     * each at least 1, totaling goldTotal             
     * @param rng seeded random generator for reproducing same layout of gold using same seed
     */

    public void placeGold(Random rng){ 
        
        numPiles = random(rng, minPiles, maxPiles); //for chosing how many piles
        if (numPiles > goldTotal){ //every pile holds at least one nugget
            numPiles = goldTotal;  //so we can never have more piles than nuggets
        }

        int goldLeftToPlace = goldTotal-numPiles; //for choosing how much gold in each pile
        int[] pileValues = new int[numPiles];
        for (int i = 0; i < numPiles; i++) {
            pileValues[i] = 1;
        }
        for (int i = 0; i < goldLeftToPlace; i++) {
            int pickPile = random(rng, 0, numPiles-1);
            pileValues[pickPile] +=1;
        }
        
        Collections.shuffle(roomSpotsList, rng); //for choosing where to put the piles
        for (int i = 0; i < numPiles; i++) {
            Point spot = roomSpotsList.get(i);
            goldMap.put(spot,pileValues[i]);
        }
        goldRemaining = goldTotal;
    }


    /**
     * Helper Method
     * Random number in range [low, high];
     * if low > high (nonsensical), return low.
     * @author Professor Kotz 2026
     */

    private static int random (Random rng, final int low, final int high){
        if (low <= high) {
        return low + rng.nextInt(high - low + 1);
        } else {
        return low; //didn't throw exception because in above code, low could never be greater than high because of final static values of minPiles, maxPiles, and goldTotal
        }
    }
    

    /** isVisible - checks to see if a point on the grid is visible to a player 
     * in their current position            
     * @param from point where player is standing                        
     * @param to point which player is checking if it is visible                                      
     * @return is point visible? t/f
     */

    public boolean isVisible(Point from, Point to){ //line of sight using only the base map
        int pr = from.getRow();
        int pc = from.getCol();
        int r = to.getRow();
        int c = to.getCol();


        for(int row = Math.min(pr, r)+1; row< Math.max(pr, r); row++){
            int denominator = r-pr;
            int numerator = pc*denominator + (row-pr)*(c-pc);
            int col = numerator/denominator;

            if(numerator%denominator ==0){ //hits a gridpoint exactly
                if(!isRoomSpot(row,col)){
                    return false;
                }
            }
            else{ //hits between gridpoints
                if(!isRoomSpot(row, col) && !isRoomSpot(row, col+1)){
                    return false;
                }
            }

        }

        for(int col = Math.min(pc, c)+1; col< Math.max(pc, c); col++){
            int den =c-pc;
            int num =pr*den + (col-pc)*(r-pr);
            int row = num/den;

            if(num%den ==0){ //hits a gridpoint exactly
                if(!isRoomSpot(row,col)){
                    return false;
                }
            }
            else{ //hits between gridpoints
                if(!isRoomSpot(row, col) && !isRoomSpot(row+1, col)){
                    return false;
                }
            }
        }

        return true;
    
    }

    /** visibleFrom -  every point visible from Point from, including itself        
     * @param from	point where player is standing                       
     * @return set of points describing points able to be seen by player from "from" position
     */

    public HashSet<Point> visibleFrom(Point from){
        HashSet<Point> visible = new HashSet<>();
        visible.add(from);
        for (int r = 0; r < numRows; r++){
            for (int c = 0; c < numCols; c++){
                Point check = map[r][c];
                if(isVisible(from, check)){
                    visible.add(check);
                }
            }
        }
        return visible;
    }


    /** takeGold - removes the gold pile from grid, lowers goldRemaining, returns its size             
     * @param p	a given point on the grid                                 
     * @return value of gold picked up
     */

    public int takeGold(Point p){ 
        int amount = goldAt(p);
        if (amount == 0){ //no pile here; leave goldRemaining alone
            return 0;
        }
        goldMap.remove(p);
        goldRemaining = (goldRemaining - amount);
        return amount;
    } 

    /** goldLocations - set of points where gold exists on the map               
     * @return	set of gold locations
     */

    public Set<Point> goldLocations(){
        return goldMap.keySet();
    }

    /** goldAt - retrieves value of gold at a given point            
     * @param p	a given point on the grid                               
     * @return value of gold at a given spot on the grid, 0 if none
     */
    
    public int goldAt(Point p){
        if (goldMap.containsKey(p)){
          return goldMap.get(p);
        }
        else{
            return 0;
        }
    }

    /** isBoundary - checks if a spot on the grid is a boundary (vertical, horizontal, or corner)               
     * @param r	row value of spot on grid to be checked                                   
     * @param c	column value of spot on grid to be checked                                                     
     * @return	is spot "-", "|", "+"? t/f
     */

    public boolean isBoundary(int r, int c){
        if (isInBounds(r,c)){
            char compare = map[r][c].getSymbol();
            return compare == HORIZONTAL_BOUNDARY || compare == VERTICAL_BOUNDARY || compare == CORNER_BOUNDARY;
        }
        else{
            return false;
        }
    } 
    
    /** isHorizontalBoundary - checks if a spot on the grid is a horizontal boundary                  
     * @param r	row value of spot on grid to be checked                                   
     * @param c	column value of spot on grid to be checked                                                     
     * @return	is spot "-"? t/f
     */
    
    public boolean isHorizontalBoundary(int r, int c){
        if (isInBounds(r,c)){
            char compare = map[r][c].getSymbol();
            return compare == HORIZONTAL_BOUNDARY;
        }
        else{
            return false;
        }
    } 

     /** isVerticalBoundary - checks if a spot on the grid is a vertical boundary                  
     * @param r	row value of spot on grid to be checked                                   
     * @param c	column value of spot on grid to be checked                                                     
     * @return	is spot "|" t/f
     */

    public boolean isVerticalBoundary(int r, int c){
        if (isInBounds(r,c)){
            char compare = map[r][c].getSymbol();
            return compare == VERTICAL_BOUNDARY;
        }
        else{
            return false;
        }
    } 
    
    /** isCorner - checks if a spot on the grid is a corner                  
     * @param r	row value of spot on grid to be checked                                   
     * @param c	column value of spot on grid to be checked                                                     
     * @return	is spot "+"? t/f
     */
    public boolean isCorner(int r, int c){
        if (isInBounds(r,c)){
            char compare = map[r][c].getSymbol();
            return compare == CORNER_BOUNDARY;
        }
        else{
            return false;
        }
    } 

    /** isRoomSpot - checks if a spot on the grid is a room spot                  
     * @param r	row value of spot on grid to be checked                                   
     * @param c	column value of spot on grid to be checked                                                     
     * @return	is spot "."? t/f
     */

    public boolean isRoomSpot(int r, int c){
        if (isInBounds(r,c)){
            char compare = map[r][c].getSymbol();
            return compare == ROOM_SPOT;
        }
        else{
            return false;
        }
    } 
    
    /** isPassageSpot - checks if a spot on the grid is a passage spot                  
     * @param r	row value of spot on grid to be checked                                   
     * @param c	column value of spot on grid to be checked                                                     
     * @return	is spot "#"? t/f
     */

    public boolean isPassageSpot(int r, int c){ 
        if (isInBounds(r,c)){
            char compare = map[r][c].getSymbol();
            return compare == PASSAGE_SPOT;
        }
        else{
            return false;
        } 
    }

     /** charAt - retreives symbol that exists at a given spot              
     * @param r	row value of spot on grid to be checked                                   
     * @param c	column value of spot on grid to be checked                                                     
     * @return	symbol, or " "
     */

    public char charAt(int r, int c){
        if (isInBounds(r,c)){
            return map[r][c].getSymbol();
        }
        else{
            return ROCK;
        }
    }

    /** pointAt - retreives point object from a given spot
     * @param r	row value of spot on grid to be checked                                   
     * @param c	column value of spot on grid to be checked                                                     
     * @return	point object, or null
     */

    public Point pointAt(int r, int c){
        if (isInBounds(r,c)){
            return map[r][c];
        }
        else{
            return null;
        } 
    }

}


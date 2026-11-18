import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * ValidateMap- Extra Credit #2 checks map for validity instead of assuming
 * @author Mia Jacobson 11/09/26
 * CS50 Fall Professor Kotz
 */

public class ValidateMap {

    private Grid map;

    public ValidateMap(Grid map) {
        this.map = map;
    }

    /** hasEnoughSpots - checks to make sure  grid has enough spots to accommodate MaxPlayers players and GoldMaxNumPiles gold piles
     * @return true if grid has enough room spots to accomodate max gold and max players, false otherwise
     */
    public boolean hasEnoughSpots(){
        int totalSpots = 0;
        for (int r = 0; r < map.getNumRows(); r++) {
            for (int c = 0; c < map.getNumCols(); c++) {
                if (map.isRoomSpot(r, c)) {
                    totalSpots++;
                }
            }
        }
        return totalSpots >= map.getMaxPiles() + 26;
    }

    /** horizontalAndVerticalBoundariesMeetAtCorners - checks to make sure a horizontal boundary always meets a vertical boundary at a corner boundary
     * @return true if horizontal and vertical boundaries meet at corners, false otherwise
     */
    public boolean horizontalAndVerticalBoundariesMeetAtCorners(){
        for (int r = 0; r < map.getNumRows(); r++) {
            for (int c = 0; c < map.getNumCols(); c++) {
                if (map.isHorizontalBoundary(r,c)){
                    boolean leftCorrect = map.isHorizontalBoundary(r, c-1) || map.isCorner(r, c-1) || map.isPassageSpot(r, c-1);
                    boolean rightCorrect = map.isHorizontalBoundary(r, c+1) || map.isCorner(r, c+1) || map.isPassageSpot(r, c+1);
                    if(!leftCorrect || !rightCorrect){
                        return false;
                    }
                }
                else if(map.isVerticalBoundary(r,c)){
                    boolean upCorrect = map.isVerticalBoundary(r-1, c)|| map.isCorner(r-1,c) || map.isPassageSpot(r-1,c);
                    boolean downCorrect = map.isVerticalBoundary(r+1, c)|| map.isCorner(r+1,c) || map.isPassageSpot(r+1,c);
                    if(!upCorrect || !downCorrect){
                    return false;
                    }
                }
            }
        }
        return true;
    }
    
    /** everyPointIsValidChar - checks to make sure every gridpoint is one of these characters:
     * solid rock, a horizontal boundary, a vertical boundary, a corner boundary, an empty room spot, an empty passage spot
     * @return true if a every spot on the map is one of these chars, false otherwise
     */
    public boolean everyPointIsValidChar(){
        for (int r = 0; r < map.getNumRows(); r++) {
            for (int c = 0; c < map.getNumCols(); c++) {
                if(map.charAt(r, c) != Grid.ROCK && map.charAt(r, c) != Grid.HORIZONTAL_BOUNDARY && map.charAt(r, c) != Grid.VERTICAL_BOUNDARY && map.charAt(r, c) != Grid.CORNER_BOUNDARY && map.charAt(r, c) != Grid.PASSAGE_SPOT && map.charAt(r, c) != Grid.ROOM_SPOT){
                    return false;
                }
            }
        }
        return true;
    }

    /** passagesNeverMeetCorners - checks to make sure a passage never meets a room at a corner
     * @return true if a passage spot touches both a horizontal and vertical boundary, false otherwise
     */
    public boolean passagesNeverMeetCorners(){
        for (int r = 0; r < map.getNumRows(); r++) {
            for (int c = 0; c < map.getNumCols(); c++) {
                if(map.isPassageSpot(r, c)){
                    boolean touchesHorizWall = map.isHorizontalBoundary(r, c-1) || map.isHorizontalBoundary(r, c+1);
                    boolean touchesVertWall = map.isVerticalBoundary(r-1,c) || map.isVerticalBoundary(r+1, c);
                    if(touchesHorizWall && touchesVertWall){
                        return false;
                    }
                }
            }
        }
        return true;
    }
     /** gridIsRectangular - checks to make sure grid is NR rows by NC columns; thus there are NR x NC gridpoints.             
     * @param mapPath path to map file to be read                               
     * @return true if length of every line is equal to the width of the first line, false otherwise
     */
    public boolean gridIsRectangular(String mapPath) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(mapPath));
        if(lines.isEmpty()){
            return false;
        }
        int width = lines.get(0).length();
        for(String line : lines){
            if(line.length() != width){
                return false;
            }
        }
        return true;
    }

    /** gridFitsInADisplayMessage - checks to make sure grid will fit in a DISPLAY message
     * @return true if the number of rows x columns + 10 is less than the max amount of
     * bytes a message could be, false otherwise
     */
    public boolean gridFitsInADisplayMessage(){
        return ((map.getNumRows()*map.getNumCols()) + 10) < Messenger.MAX_BYTES;
    }

    /** passagesAreOnlyOneSpotWide - checks to make sure A passage is one-spot wide
     * @return false if a 2x2 patch of passage spots is found anywhere on the grid, true otherwise
     */
    public boolean passagesAreOnlyOneSpotWide(){
        for (int r = 0; r < map.getNumRows(); r++) {
            for (int c = 0; c < map.getNumCols(); c++) {
                if(map.isPassageSpot(r, c) && map.isPassageSpot(r+1, c) && map.isPassageSpot(r, c+1) && map.isPassageSpot(r+1, c+1)){
                    return false;
                }
            }   
        }
        return true;
    }
    /** mapIsOneConnectedComponent - checks to make sure map is one connected component; 
     * one can reach any spot from any other spot by some sequence of the eight valid moves.
     * @return true if each the total amount of spots on the grid equals the amound of points 
     * able to be reached by another point, false otherwise
     */
    public boolean mapIsOneConnectedComponent(){
        int totalSpots = 0;
        Point start = null;
        for (int r = 0; r < map.getNumRows(); r++){
            for (int c = 0; c < map.getNumCols(); c++){
                if (map.charAt(r,c) == Grid.PASSAGE_SPOT || map.charAt(r,c) == Grid.ROOM_SPOT){
                    totalSpots +=1;
                    if(start == null){
                        start = map.pointAt(r, c);
                    }
                }
            }
        }
        if(start == null){ 
            return false;
        }
        HashSet<Point> reached = new HashSet<>();
        ArrayList<Point> toExplore = new ArrayList<>();
        reached.add(start);
        toExplore.add(start);

        while(!toExplore.isEmpty()){
            Point current = toExplore.remove(0);
            for (int dr = -1; dr <= 1; dr++){
                for (int dc = -1; dc <= 1; dc++){
                    int r = current.getRow() + dr;
                    int c = current.getCol() + dc;
                    if(map.isRoomSpot(r, c) || map.isPassageSpot(r, c)){
                        Point neighbor = map.pointAt(r, c);
                        if(!reached.contains(neighbor)){
                            reached.add(neighbor);
                            toExplore.add(neighbor);
                        }
                    }
                }
            }
        }
        return totalSpots == reached.size();
    }

    /** isValid - conbines each of the above methods into a single valid check
     * @return true if every method is true, false otherwise
     * @throws IOException if map is invalid
     */
    public boolean isValid(String mapPath) throws IOException {
        if (!gridIsRectangular(mapPath)){
            System.err.println("invalid map: not every line is the same length");
            return false;
        }
        if (!hasEnoughSpots()){
            System.err.println("invalid map: not enough spots");
            return false;
        }
        if(!everyPointIsValidChar()){
            System.err.println("invalid map: invalid char present on map");
            return false;
        }
        if(!mapIsOneConnectedComponent()){
            System.err.println("invalid map: map is disconnected somewhere");
            return false;
        }
        if(!gridFitsInADisplayMessage()){
            System.err.println("invalid map: too large");
            return false;
        }
        if(!horizontalAndVerticalBoundariesMeetAtCorners()){
            System.err.println("invalid map: invalid corners");
            return false;
        }
        if(!passagesNeverMeetCorners()){
            System.err.println("invalid map: passage meets a corner");
            return false;
        }
        if(!passagesAreOnlyOneSpotWide()){
            System.err.println("invalid map: passages are more than one spot wide");
            return false;
        }
        return true;
    }

}
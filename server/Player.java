import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Player - describes and stores all information about players of a Nuggets game
 * @author Mia Jacobson 11/9/26
 */

public class Player{

    private final char letter;
    private final String name;
    private final InetSocketAddress address;
    private int purse;
    private boolean active; 
    private Point position;
    
    private HashSet<Point> known = new HashSet<Point>(); //set of prevoiusly visible points that can always be seen
    private HashSet<Point> visible = new HashSet<Point>(); //set of currently visible points that can currently be seen 

    public Player(char letter, String name, InetSocketAddress address, Point position){
        this.letter = letter;
        this.name = name;
        this.purse = 0;
        this.address = address;
        this.active = true;
        this.position = position;
    }

    //getters

    public char getLetter(){
        return letter;
    }

    public String getName(){
        return name;
    }

    public InetSocketAddress getAddress(){
        return address;
    }

    public int getPurse(){
        return purse;
    }

    public Point getPosition(){
        return position;
    }

    public Set<Point> getKnown(){
        return Collections.unmodifiableSet(known);
    }

    public Set<Point> getVisible(){
        return Collections.unmodifiableSet(visible);
    }

    public boolean getStatus(){
        return active;
    }

    /** increasePlayerPurse - increments a player's purse by amount of gold in                 
     * @param gold number to be divided                                       
     */

    public void adjustPurse(int gold){ //increments a player's purse
        purse += gold;

    }

    /** moveTo - updates player position on the map                  
     * @param newPoint	point to be moved to                                       
     */

    public void moveTo(Point newPoint){ 
        position = newPoint;

    }
    
    /** isActive - checks status of player                  
     * @return active? t/f
     */

    public boolean isActive(){ //checks active status
        return active;
    }

    /** quit - changes player's active status to false                  
     */

    public void quit(){ 
        active = false;

    }

    /** updateView - records what a player can see at a given point in time                  
     * @param nowVisible the gridpoints visible from a player's position	                                       
     */

    public void updateView(HashSet<Point> nowVisible){
        visible.clear();
        for (Point p : nowVisible){
            visible.add(p);
            known.add(p);
        }
    }

}

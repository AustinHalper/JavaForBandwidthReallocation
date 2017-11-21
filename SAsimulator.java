@@ -0,0 +1,897 @@

/*************************************************************************
 *
 *  Paper:
 *  "Station Assignment with Reallocation"
 *  Austin Halper, Miguel A. Mosteiro, Yulia Rossikova, and Prudence W. H. Wong
 *  Proceedings of 14th International Symposium on Experimental Algorithms (SEA 2015)
 *
 *  Description: Simulator for CPR protocol
 *
 *  Input:
 *  - input file with the following columns: {item, id, arrival time, laxity, 1/laxity, departure time}
 *  - type in the main program number of clients N, arrival distribution, and multiplicative factor that distinguish 3 classifications, and then recompile.
 *  Laxities must be powers of 2 (for arbitrary round them down first)
 *
 *  Output: 
 *  - Display status at each time step, and alpha max and beta max at the end.
 *  - Output to a file the status at each time step.
 *
 *  Classes and data structures:
 *  public class SAsimulator        // this simulator implemented as a list of classes and an array of clients
 *  class group                     // a class of stations implemented as a list of stations
 *  class station                   // a station implemented as a matrix (rows are trees and trees are arrays)
 *  class client                    // all fields of a client
 *
 *   Remarks
 *   -------
 *
 *  Compile with option -ea to activate invariant checker
 *
 *************************************************************************/

import java.io.*;
import java.util.*;


/*************************************************************************
 *
 *  Paper:
 *  "Station Assignment with Reallocation"
 *  Austin Halper ,Miguel A. Mosteiro, Yulia Rossikova, and Prudence W. H. Wong
 *  Proceedings of 14th International Symposium on Experimental Algorithms (SEA 2015)
 *
 *  Description: Simulator for CPR protocol
 *  Class: SAsimulator
 *
 *  Data fields:
 private ArrayList<group> groups = new ArrayList<group>();            // we use the name 'group' to avoid 'class'
 private client[] clientSet;                                          // list of clients with their attributes
 private client hole = new client();         // non-existent client to mark a hole (id=0 => make all client ID's >0)
 private int factor=0;                         // type of simulator
 private double departedWeight=0;              // weight of departed clients
 private double activeWeight=0;                // weight of active clients
 private double currentReallocatedWeight=0;    // weight of clients reallocated in the current time step
 *
 *  Methods:
 SAsimulator(int factor, File file, int N){
 public void add(client myClient){
 public boolean delete(client myClient){
 public void reallocate(int time){
 public int numberOfStations(){
 public String status(){
 private int[] limits(client myClient, int factor){ // works only for powers of 2
 private void readInput(File file, client[] clientSet){
 public void printStructure(){
 public boolean contains(int id){
 public static void main(String[] args){
 *
 *   Remarks
 *   -------
 *
 *************************************************************************/

public class SAsimulator{

    // types of simulator
    final static int CONSTANT = 0;
    final static int LOGARITHMIC = 1;
    final static int LINEAR = 2;
    // arrivals distributions
    final static int UNIFORM = 1;
    final static int BATCHED = 2;
    final static int POISSON = 3;

    // data fields
    private ArrayList<group> groups = new ArrayList<group>();            // we use the name 'group' to avoid 'class'
    private client[] clientSet;                                          // list of clients with their attributes
    private client hole = new client();         // non-existent client to mark a hole (id=0 => make all client ID's >0)
    private int factor=0;                         // type of simulator
    private double departedWeight=0;              // weight of departed clients
    private double activeWeight=0;                // weight of active clients
    private double currentReallocatedWeight=0;    // weight of clients reallocated in the current time step
    
    // constructor
    SAsimulator(int factor, File file, int N){
        this.clientSet = new client[N];
        // read input file
        readInput(file, this.clientSet);
        this.factor = factor;
    }
    
    /////////////
    // ADD
    /////////////
    // adds client to the schedule
    public void add(client myClient){
        // update status
        this.activeWeight += ((double)1)/((double)myClient.laxity);
        // find if the new client fits in one of the existing groups
        for(int i=0; i<this.groups.size(); i++){
            if (this.groups.get(i).wMax > myClient.laxity && myClient.laxity >= this.groups.get(i).wMin){
                // add to this group
                this.groups.get(i).add(myClient, this.hole);
                return;
            }
        }
        // new client does not fit in any of the existing groups, create a new one
        // find out which group should this client be added
        int[] bounds = limits(myClient, this.factor);
        // add a new group with those bounds to the list of groups
        group newGroup = new group(bounds[1],bounds[0]);
        this.groups.add(newGroup);
        // add the new client to the new group
        newGroup.add(myClient, this.hole);
    }

    /////////////
    // DELETE
    /////////////
    // removes client from the schedule
    public boolean delete(client myClient){
        // update status
        this.activeWeight -= ((double)1)/((double)myClient.laxity);
        this.departedWeight += ((double)1)/((double)myClient.laxity);
        // find the client's group
        for(int i=0; i<this.groups.size(); i++){
            if (this.groups.get(i).wMax > myClient.laxity && myClient.laxity >= this.groups.get(i).wMin){
                // remove from this group
                return this.groups.get(i).delete(myClient, this.hole);
            }
        }
        // if we get here there was an error
        return false;
    }
    
    ///////////////
    // REALLLOCATE
    ///////////////
    // reallocates clients to restore the invariant
    public void reallocate(int time){
        // clients are reallocated within their class only
        for(int i=0; i<this.groups.size(); i++){
            if(this.groups.get(i).numberOfStations()>0) this.groups.get(i).reallocate(time, this.hole);
        }
        // eliminate empty classes
        for(int i=0; i<this.groups.size(); i++)
            if(this.groups.get(i).numberOfStations()==0) this.groups.remove(i);
    }
    
    // computes number of active stations
    public int numberOfStations(){
        int counter=0;
        for(int i=0; i<this.groups.size();i++)
            counter+=this.groups.get(i).numberOfStations();
        return counter;
    }
    
    // display current status of the simulation
    public String status(){
        return "departed_weight= "+this.departedWeight+" H= "+Math.ceil(this.activeWeight)+" stations= "+this.numberOfStations()+" reallocated_weight= "+this.currentReallocatedWeight+" active-weight= "+this.activeWeight;
    }

    //finds the class of the given client
    private int[] limits(client myClient, int factor){ // works only for powers of 2
        int [] bounds = new int[2];     //return 2 values
        bounds[0]=1;
        bounds[1]=2;
        if (myClient.laxity < bounds[1] && myClient.laxity >= bounds[0]) return bounds;
        bounds[0]=2;
        bounds[1]=4;
        if (myClient.laxity < bounds[1] && myClient.laxity >= bounds[0]) return bounds;
        bounds[0]=4;
        switch(factor){
            case CONSTANT:
                while(myClient.laxity >= 2*bounds[0])
                    bounds[0] = 2*bounds[0];
                bounds[1] = 2*bounds[0];
                break;
            case LOGARITHMIC:
                while(myClient.laxity >= bounds[0]*(31 - Integer.numberOfLeadingZeros(bounds[0])))
                    bounds[0] = bounds[0]*(31 - Integer.numberOfLeadingZeros(bounds[0]));
                bounds[1] = bounds[0]*(31 - Integer.numberOfLeadingZeros(bounds[0]));
                // round up to next power of 2 if not a power of 2 already
                // for instance, for max window = 1024, the classes must be
                // [1,2)
                // [2,4)
                // [4,8)
                // 8,24 -> [8,32)
                // 24,110 -> [32,128)
                // 110,745 -> [128,1024)
                if(bounds[0] != 1<<(31-Integer.numberOfLeadingZeros(bounds[0])))
                    bounds[0] = 1<<(32-Integer.numberOfLeadingZeros(bounds[0]));
                if(bounds[1] != 1<<(31-Integer.numberOfLeadingZeros(bounds[1])))
                    bounds[1] = 1<<(32-Integer.numberOfLeadingZeros(bounds[1]));
                break;
            default:    //LINEAR
                while(myClient.laxity >= bounds[0]*bounds[0])
                    bounds[0] = bounds[0]*bounds[0];
                bounds[1] = bounds[0]*bounds[0];
        }
        return bounds;
    }
    
    // reads the input file
    private void readInput(File file, client[] clientSet){
        try{
            Scanner input = new Scanner(file);
            input.nextLine();   //ignore 1st line
            input.nextLine();   //ignore 2nd line
            for(int i=0;input.hasNext();i++){
                clientSet[i] = new client();
                input.next();   //ignore
                clientSet[i].id = 1+Integer.parseInt(input.next()); // we need client ID's starting from 1
                clientSet[i].arrivaltime = (int)Double.parseDouble(input.next());
                clientSet[i].laxity = Integer.parseInt(input.next());
                input.next();   //ignore
                clientSet[i].departuretime = (int)Double.parseDouble(input.next());
                
                // check input integrity
                for(int j=0; j<i;j++){
                    if (clientSet[j].id==clientSet[i].id){
                        System.out.println("Input contains a duplicate client.");
                        System.exit(0);
                    }
                }
                if(clientSet[i].arrivaltime>clientSet[i].departuretime){
                    System.out.println("Input contains a client with arrival time > departure time.");
                    System.exit(0);
                }
            }
        }catch(IOException excp){System.out.println("File not found");}
    }
    
    // for debugging: prints all trees
    public void printStructure(){
        System.out.println("Structure print out: ");
        for(int i=0;i<this.groups.size(); i++){
            this.groups.get(i).print(hole);
        }
    }
    
    // for debugging: search for a client id
    public boolean contains(int id){
        boolean result=false;
        for(int h=0;h<this.groups.size();h++){
            if(this.groups.get(h).contains(id,this.hole)) result=true;
        }
        return result;
    }

    public static void main(String[] args){
        // init
        int N = 4000;                                                                   // number of clients
        int factor = LINEAR;                                                            // type of simulator
        int arrivals = BATCHED;                                                         // arrivals distribution
        File inputFile = new File("./inputs1"+arrivals+factor+".txt");      // input file
        SAsimulator mySim = new SAsimulator(factor, inputFile, N);                      // create simulator
        mySim.hole.id = 2*N;                                                            // put an inexistent id in the hole client
        double alphaMax = 0;
        double betaMax = 0;
        
        try{
            File outputFile = new File("./results1"+arrivals+factor+".txt");
            PrintWriter output = new PrintWriter(outputFile);
            boolean departures=false;
            // process events
            for(int t=0; t<=2*N; t++){                                  // for each time slot
                departures=false;
                for(int j=0; j<mySim.clientSet.length; j++){            // for each client
                    if(mySim.clientSet[j].arrivaltime==t)               // this client arrived at this time
                        mySim.add(mySim.clientSet[j]);                  // add it to the system
                    else{
                        if (mySim.clientSet[j].departuretime==t){       // this client departed at this time
                            if(!mySim.delete(mySim.clientSet[j])){      // remove it from the system
                                System.out.println("Attempt to remove a non-existent client."+mySim.clientSet[j].toString()+".");
                                System.exit(0);
                            }
                            departures=true;
                        }
                    }
                }
                if(departures){                                             // if there were departures
                    mySim.reallocate(t);                                    // reallocate if necessary
                }
                
                mySim.currentReallocatedWeight = 0;                     // compute reallocated weight
                for(int i=0; i<mySim.clientSet.length;i++){
                    if(t==mySim.clientSet[i].lastReallocTime) mySim.currentReallocatedWeight+=((double)1)/((double)mySim.clientSet[i].laxity);
                }

                output.println("t= "+t+" "+mySim.status());                         // output status at this time
                System.out.println("t= "+t+" "+mySim.status());                     // display status

                if(mySim.numberOfStations()/Math.ceil(mySim.activeWeight)>alphaMax) // update alpha max
                    alphaMax=mySim.numberOfStations()/Math.ceil(mySim.activeWeight);

                if (mySim.currentReallocatedWeight>0){                                                  // if there were reallocations
                    if(((double)mySim.currentReallocatedWeight)/((double)mySim.departedWeight)>betaMax) // update beta max
                        betaMax=((double)mySim.currentReallocatedWeight)/((double)mySim.departedWeight);
                    mySim.departedWeight=0;                                                             // reset departed weight until new realloc event
                }
            
            }
            output.close();
            System.out.println("max alpha = "+alphaMax);                            // display alpha max
            System.out.println("max beta = "+betaMax);                              // display beta max
        }catch(IOException excp){System.out.println("File not found");}
    }
}


/*************************************************************************
 *
 *  Paper:
 *  "Station Assignment with Reallocation"
 *  Austin Halper, Miguel A. Mosteiro, Yulia Rossikova, and Prudence W. H. Wong
 *  Proceedings of 14th International Symposium on Experimental Algorithms (SEA 2015)
 *
 *  Description: Simulator for CPR protocol
 *  Class: group
 *
 *  Data fields:
 public ArrayList<station> myGroup;
 public int wMax;
 public int wMin;
 *
 *  Methods:
 public void add(client myClient, client hole){
 public boolean delete(client myClient, client hole){
 public void reallocate(int time, client hole){
 public boolean invariant(client hole){
 public int numberOfStations(){
 public void print(client hole){
 public boolean contains(int id, client hole){
 *
 *   Remarks
 *   -------
 *
 *************************************************************************/


class group{    // a group of stations implemented as a list of stations
    
    // data fields
    public ArrayList<station> myGroup;
    public int wMax;
    public int wMin;
    
    // constructor
    group(int wMax, int wMin){
        this.myGroup = new ArrayList<station>();
        this.wMax = wMax;
        this.wMin = wMin;
    }
    
    /////////////
    // ADD
    /////////////
    public void add(client myClient, client hole){
        // find if the new client fits in one of the existing stations, checking level by level upwards
        for(int levelLax = myClient.laxity ; levelLax >= this.wMin  ; levelLax=levelLax/2){
            for(int i=0; i<this.myGroup.size(); i++){
                if (this.myGroup.get(i).add(myClient,levelLax, hole)){
                    return;
                }
            }
        }
        // does not fit in any of the existing stations, create a new one with a hole in the root of each tree
        station newStation = new station(this.wMax, this.wMin, hole);
        // add the new client as a caterpillar at the root
        newStation.add(myClient, this.wMin, hole);
        this.myGroup.add(newStation);
    }

    /////////////
    // DELETE
    /////////////
    public boolean delete(client myClient, client hole){
        // try each station
        for(int i=0; i<this.myGroup.size(); i++){
            if (this.myGroup.get(i).delete(myClient,hole)) return true;
        }
        // did not find it in any of the stations
        return false;
    }

    /////////////
    // REALLOCATE
    /////////////
    public void reallocate(int time, client hole){
        boolean stopIter = false;
        // consolidate holes within stations for free
        for(int i=0; i<this.myGroup.size(); i++)
            this.myGroup.get(i).reallocateWithinStation(hole);
        // after the above consolidation, no station can have more than one hole per non-top level (but this could change now)
        // reallocate among stations level by level upwards (top level comes later)
        for(int levelLax = this.wMax/2 ; levelLax > this.wMin  ; levelLax=levelLax/2){
            // find pairs of stations with a hole
            for(int i=0; i<this.myGroup.size()-1; i++){     // -1 because there must be pairs
                if (this.myGroup.get(i).hasHole(levelLax, hole)){   // station i has hole, look for another one
                    stopIter=false;
                    for(int j=i+1; j<this.myGroup.size() && !stopIter; j++){
                        if (this.myGroup.get(j).hasHole(levelLax, hole)){   // station j has hole, reallocate
                            // direction of reallocation is arbitrary (2nd to 1st parameter)
                            // because checking all the combinations upwards would be exponential
                            if(!station.reallocate(this.myGroup.get(i),this.myGroup.get(j),levelLax,time,hole)){
                                System.out.println("A scheduled reallocation at lower level was not performed, simulator stopped.");
                                System.exit(0);
                            }
                            // the latter reallocation may leave 2 holes in station j
                            // consolidate within station for free
                            this.myGroup.get(j).reallocateWithinStation(hole);
                            stopIter=true;  // stop inner iteration
                        }
                    }
                }
            }
        }
        // consolidate holes within stations for free
        for(int i=0; i<this.myGroup.size(); i++)
            this.myGroup.get(i).reallocateWithinStation(hole);
        // reallocate among stations at top level
        // 1) sort them by top-holes
        // 2) fill the leftmost top-hole with the rightmost top-non-hole
        // create an array with the number of topholes on each station
        int[] topHolesCounter = new int[this.myGroup.size()];
        for(int i=0; i<this.myGroup.size(); i++)
            topHolesCounter[i] = this.myGroup.get(i).numberOfTopHoles(hole);
        // find out the max number of top holes
        int maxTopHoles=0;
        for(int i=0; i<topHolesCounter.length; i++)
            if (topHolesCounter[i]>maxTopHoles) maxTopHoles = topHolesCounter[i];
        // create a new array with the indices of the top hole counter sorted by number of top holes
        int[] sortedStations = new int[topHolesCounter.length];
        for(int topHoles=0,j=0; topHoles<=maxTopHoles; topHoles++){
            for(int i=0; i<topHolesCounter.length; i++){
                if (topHolesCounter[i] == topHoles){
                    sortedStations[j]=i;
                    j++;
                }
            }
        }
        // using the indirect index created
        // reallocate top holes moving from the rightmost non-empty station to the leftmost non-full station
        int leftpointer = 0;
        while(this.myGroup.get(sortedStations[leftpointer]).isTaken(hole) && leftpointer<sortedStations.length-1)
            leftpointer++; // move right until there is a non-taken
        int rightpointer = sortedStations.length-1;
        while(this.myGroup.get(sortedStations[rightpointer]).isEmpty(hole) && rightpointer>0)
            rightpointer--; // move left until there is a non-empty
        while(leftpointer<rightpointer){    // while the pointers do not cross there is something to reallocate
            if (!station.reallocate(this.myGroup.get(sortedStations[leftpointer]),this.myGroup.get(sortedStations[rightpointer]),this.wMin,time,hole)){
                System.out.println("A scheduled reallocation at top level was not performed, simulator stopped.");
                System.exit(0);
            }
            // update pointers
            if ( this.myGroup.get(sortedStations[leftpointer]).isTaken(hole)) leftpointer++;
            if ( this.myGroup.get(sortedStations[rightpointer]).isEmpty(hole)) rightpointer--;
        }
        // deactivate empty stations
        for(int i=0; i<this.myGroup.size(); i++){
            if(this.myGroup.get(i).isEmpty(hole))
                this.myGroup.remove(i);
        }
        // check invariant:
        assert invariant(hole): "Invariant violated after reallocation.";
    }
    
    // invariant
    // 1) throughout stations, at most one hole per non-top level
    // 2) at most one station has holes in top level
    // 3) no station is empty
    public boolean invariant(client hole){
        // 1) throughout stations, at most one hole per non-top level
        int holesCounter=0;
        for(int levelLax = this.wMax/2 ; levelLax > this.wMin  ; levelLax=levelLax/2){
            holesCounter=0;
            for(int i=0; i<this.myGroup.size(); i++){
                holesCounter += this.myGroup.get(i).numberOfHoles(levelLax, hole);
            }
            if(holesCounter>1){
                System.out.println(holesCounter+" holes at level "+levelLax+" in class ["+this.wMin+","+this.wMax+").");
                return false;
            }
        }
        // 2) at most one station has holes in top level
        int topHoleStationsCounter=0;
        for(int i=0; i<this.myGroup.size(); i++){
            if(this.myGroup.get(i).hasHole(wMin, hole))
                topHoleStationsCounter++;
        }
        if(topHoleStationsCounter>1) return false;
        // 3) no station is empty
        for(int i=0; i<this.myGroup.size(); i++){
            if(this.myGroup.get(i).isEmpty(hole))
                return false;
        }
        return true;
    }
    
    // computes number of stations
    public int numberOfStations(){
        return myGroup.size();
    }
    
    // for debugging: prints all trees
    public void print(client hole){
        System.out.println("Class ["+this.wMin+","+this.wMax+") with "+this.myGroup.size()+" stations.");
        for(int i=0;i<this.myGroup.size(); i++){
            this.myGroup.get(i).print(hole);
        }
    }
    
    // for debugging: search for a client id
    public boolean contains(int id, client hole){
        boolean result=false;
        for(int h=0;h<this.myGroup.size();h++){
            if(this.myGroup.get(h).contains(id,hole)) result=true;
        }
        return result;
    }
    
}


/*************************************************************************
 *
 *  Paper:
 *  "Station Assignment with Reallocation"
 *  Austin Halper, Miguel A. Mosteiro, Yulia Rossikova, and Prudence W. H. Wong
 *  Proceedings of 14th International Symposium on Experimental Algorithms (SEA 2015)
 *
 *  Description: Simulator for CPR protocol
 *  Class: station
 *
 *  Data fields:
 private client[][] myStation;
 private int wMax;
 private int wMin;
 *
 *  Methods:
 station(int wMax, int wMin, client hole){
 public boolean add(client myClient, int levelLax, client hole){
 public boolean delete(client myClient, client hole){
 public static boolean reallocate(station destination,station origin,int levelLax,int time,client hole){
 public void reallocateWithinStation(client hole){
 private int[] findHole(int levelLax, client hole){
 private int[] findSiblingOfHole(int levelLax, client hole){
 public int numberOfHoles(int levelLax, client hole){
 public boolean hasHole(int levelLax, client hole){
 public int numberOfTopHoles(client hole){
 public boolean isEmpty(client hole){
 public boolean isTaken(client hole){
 public void print(client hole){
 public boolean contains(int id, client hole){
 *
 *   Remarks
 *   -------
 *
 *************************************************************************/


class station{
    // implemented as wMin binary trees (arrays) each of size wMax/wMin (the length of the array)
    // using that the children of i is at 2i and 2i+1 and the parent at floor(i/2), do not use position 0
    // the reason for wMax/wMin is that in the last level the laxity is wMax/2. In the last level there are then wMax/(2wMin) nodes. In the whole tree there are wMax/wMin-1 nodes. And we need one more to ignore the first position.
    // the definition of the tree nodes usage is the following:
    // available : hole client
    // taken (by a client) : client reference
    // unavailable : null
    
    // data fields
    private client[][] myStation;
    private int wMax;
    private int wMin;
    
    // constructor
    station(int wMax, int wMin, client hole){
        this.wMax = wMax;
        this.wMin = wMin;
        this.myStation = new client[this.wMin][this.wMax/this.wMin];    // rows are the trees, columns >0 are the nodes, ignore column 0
        // put holes in the root of all trees (all the other nodes are unavailable (null) by default
        for(int i=0; i<this.myStation.length; i++){
            this.myStation[i][0]=null;      // not used
            this.myStation[i][1]=hole;      // the root
            for(int j=2; j<this.myStation[i].length; j++)
                this.myStation[i][j]=null;  // descendants
        }
    }
    
    /////////////
    // ADD
    /////////////
    // a client to an available position, store the reference in that position and mark as unavailable all its ancestors and descendants.
    public boolean add(client myClient, int levelLax, client hole){
        // try to allocate myClient to this station at the given laxity level, return true if successful
        for(int tree=0; tree<this.myStation.length; tree++){
            //positions from levelLax
            //if levelLax/this.wMin = 1 then position = 1
            //if levelLax/this.wMin = 2 then positions = 2 and 3
            //if levelLax/this.wMin = 4 then positions = 4,5,6 and 7
            // ... in general:
            for(int i=(levelLax/this.wMin); i<(2*levelLax/this.wMin); i++){ // for each position at the desired level
                if(this.myStation[tree][i]==hole){                               // found an available position
                    // ancestors are already null, because there cannot be a hole who is a descendant of another hole
                    // assign at that position an appropriate caterpillar of length according to myClient.laxity
                    while(i<myClient.laxity/this.wMin){ // if this is not the level that corresponds to myClient, go down
                        this.myStation[tree][i] = null;
                        this.myStation[tree][2*i+1] = hole;
                        i=2*i;
                    }
                    this.myStation[tree][i] = myClient;
                    // descendants are already null, because there cannot be a hole or a client who is a descendant of another hole
                    return true;
                }
            }
        }
        return false;
    }
    
    /////////////
    // DELETE
    /////////////
    // and consolidate sibling holes up to the root.
    public boolean delete(client myClient, client hole){
        // attempt to delete, if not found, return false
        for(int tree=0; tree<this.myStation.length; tree++){
            for(int i=1; i<this.myStation[tree].length; i++){
                if(this.myStation[tree][i]==myClient){          // my Client found
                    this.myStation[tree][i]=hole;               // de-allocate myClient
                    while(i>1){                            // consolidate holes up to the root
                        int sibling=0;
                        if((i&1)==0) sibling=i+1;  // i is even, sibling is to the right
                        else sibling = i-1;        // i is odd, sibling is to the left
                        if (this.myStation[tree][sibling]==hole){
                            this.myStation[tree][i]=null;
                            this.myStation[tree][sibling]=null;
                            this.myStation[tree][i/2]=hole;
                            i=i/2;
                        }
                        else{   // sibling is not hole => consolidation complete
                            return true;
                        }
                    }// hole at root
                    return true;
                }
            }
        }
        return false;
    }

    /////////////
    // REALLOCATE
    /////////////
    public static boolean reallocate(station destination,station origin,int levelLax,int time,client hole){
        // check input integrity
        assert (destination.wMin==origin.wMin && destination.wMax==origin.wMax) : "Origin and destination classes do not match.";
        assert (levelLax>=destination.wMin && levelLax<destination.wMax) : "Requested level of reallocation does not belong to this class.";
        // find the destination hole
        int [] retval = destination.findHole(levelLax,hole);
        if (retval==null){
            System.out.println("Destination hole not found");
            return false;
        }
        int i = retval[0];
        int j = retval[1];
        // find the origin sibling of hole
        retval = origin.findSiblingOfHole(levelLax,hole);
        if (retval==null){
            System.out.println("Origin sibling of hole not found");
            return false;
        }
        int k = retval[0];
        int l = retval[1];
        // reallocate origin node and all descendants (even nulls) j,2j,2j+1,4j,4j+1,4j+2,4j+3,8j,...
        for(int mult=1; mult*levelLax<destination.wMax; mult*=2){
            for(int offset=0; offset<mult; offset++){
                destination.myStation[i][j*mult+offset]=origin.myStation[k][l*mult+offset];
                if(mult==1) origin.myStation[k][l*mult+offset]=hole;
                else origin.myStation[k][l*mult+offset]=null;
                if(destination.myStation[i][j*mult+offset]!=null && destination.myStation[i][j*mult+offset]!=hole){
                    destination.myStation[i][j*mult+offset].lastReallocTime=time;
                }
            }
        }
        // consolidate holes in the origin up to the root
        while(l>1){
            int sibling=0;
            if((l&1)==0) sibling=l+1;  // l is even, sibling is to the right
            else sibling = l-1;        // l is odd, sibling is to the left
            if (origin.myStation[k][sibling]==hole){
                origin.myStation[k][l]=null;
                origin.myStation[k][sibling]=null;
                origin.myStation[k][l/2]=hole;
                l=l/2;  // computes floor
            }
            else       // sibling is not hole => consolidation complete
                return true;
        }// hole at root
        return true;
    }
    
    /////////////////////////////
    // REALLOCATE WITHIN STATION
    /////////////////////////////
    // within station reallocations are free
    // for simplicity, reset station moving all clients.
    public void reallocateWithinStation(client hole){
        //copy clients to a list
        ArrayList<client> temp = new ArrayList<client>();
        for(int i=0; i<this.myStation.length; i++){
            for(int j=0; j<this.myStation[i].length; j++){
                if(this.myStation[i][j]!=hole && this.myStation[i][j]!=null){
                    temp.add(this.myStation[i][j]);
                }
            }
        }
        //reset station: put holes in the root of all trees and all the other nodes unavailable (null)
        for(int i=0; i<this.myStation.length; i++){
            this.myStation[i][0]=null;      // not used
            this.myStation[i][1]=hole;      // the root
            for(int j=2; j<this.myStation[i].length; j++)
                this.myStation[i][j]=null;  // descendants
        }
        // add again from list
        boolean stopIter=false;
        for(int h=0;h<temp.size();h++){
            stopIter=false;
            for(int levelLax=temp.get(h).laxity;levelLax>=this.wMin && !stopIter;levelLax/=2){
                if(this.add(temp.get(h),levelLax,hole)) stopIter=true;
            }
        }
    }

    // finds a hole at the given laxity level
    private int[] findHole(int levelLax, client hole){
        int[] retval = null;
        for(int i=0;i<this.myStation.length;i++){    // for each tree in the station
            for(int j=levelLax/this.wMin; j<2*levelLax/this.wMin; j++){   // for each node in the level of laxity levelLax
                if(this.myStation[i][j]==hole){      // found hole
                    retval = new int[2];
                    retval[0]=i;
                    retval[1]=j;
                    return retval;
                }
            }
        }
        return retval;
    }

    //  finds the sibling of a hole at the given laxity level
    private int[] findSiblingOfHole(int levelLax, client hole){
        int[] retval = null;
        int sibling = 0;
        if(levelLax==this.wMin){     // find sibling tree
            for(int i=0;i<this.myStation.length;i++){    // for each tree in the station
                if(this.myStation[i][1]!=hole){      // found sibling tree
                    retval = new int[2];
                    retval[0]=i;
                    retval[1]=1;
                    return retval;
                }
            }
        }
        else{                       // find sibling node
            for(int i=0;i<this.myStation.length;i++){    // for each tree in the station
                for(int j=levelLax/this.wMin; j<2*levelLax/this.wMin; j++){   // for each node in the level of laxity levelLax
                    if(this.myStation[i][j]==hole){      // found hole
                        // find sibling
                        if((j&1)==0) sibling=j+1;  // j is even, sibling is to the right
                        else sibling = j-1;        // j is odd, sibling is to the left
                        if (this.myStation[i][sibling]==hole){  // sibling holes => error
                            System.out.println("Unexpected sibling holes.");
                            System.exit(0);
                        }
                        retval = new int[2];
                        retval[0]=i;
                        retval[1]=sibling;
                        return retval;
                    }
                }
            }
        }
        return retval;
    }

    // auxiliary computations
    public int numberOfHoles(int levelLax, client hole){
        int counter=0;
        for(int i=0; i<this.myStation.length; i++){                        // for each tree
            for(int j=levelLax/this.wMin; j<2*levelLax/this.wMin; j++){    // for each position at the corresponding level
                if(this.myStation[i][j]==hole) counter++;
            }
        }
        return counter;
    }
    public boolean hasHole(int levelLax, client hole){
        return (this.numberOfHoles(levelLax,hole)>0);
    }
    public int numberOfTopHoles(client hole){
        return this.numberOfHoles(this.wMin,hole);
    }
    public boolean isEmpty(client hole){
        return (this.numberOfTopHoles(hole)==this.wMin);
    }
    public boolean isTaken(client hole){
        return (this.numberOfTopHoles(hole)==0);
    }
    
    // for debugging: prints the trees
    public void print(client hole){
        for(int i=0;i<this.myStation.length; i++){          // rows are the trees
            for(int j=1;j<this.myStation[i].length; j++){   // columns are tree nodes
                if(this.myStation[i][j]!=null){
                    if(this.myStation[i][j]!=hole){         // an assigned client
                        System.out.print("1");
//                        System.out.print(" "+this.myStation[i][j].id+" ");
                    }
                    else                                    // a hole
                        System.out.print("0");
                }
                else                                        // a null
                    System.out.print(".");
            }
            System.out.println();
        }
    }
    
    // for debugging: search for a client id
    public boolean contains(int id, client hole){
        boolean result=false;
        for(int h=0;h<this.myStation.length;h++){
            for(int k=0;k<this.myStation[h].length;k++){
                if(this.myStation[h][k]!=hole && this.myStation[h][k]!=null){
                    if(this.myStation[h][k].id==3) result=true;
                }
            }
        }
        return result;
    }
    
}


/*************************************************************************
 *
 *  Paper:
 *  "Station Assignment with Reallocation"
 *  Austin Halper, Miguel A. Mosteiro, Yulia Rossikova, and Prudence W. H. Wong
 *  Proceedings of 14th International Symposium on Experimental Algorithms (SEA 2015)
 *
 *  Description: Simulator for CPR protocol
 *  Class: client
 *
 *  Data fields:
 public int id=0;
 public int arrivaltime=0;
 public int departuretime=0;
 public int laxity=0;
 public int lastReallocTime=-1;      // there is a t=0
 *
 *  Methods:
 public String toString(){
 *
 *   Remarks
 *   -------
 *
 *************************************************************************/


class client{
    
    // data fields
    public int id=0;
    public int arrivaltime=0;
    public int departuretime=0;
    public int laxity=0;
    public int lastReallocTime=-1;      // there is a t=0
    
    // for debugging: prints a client
    public String toString(){
        return ("id = "+id+", arrival time = "+arrivaltime+", departure time = "+departuretime+", laxity = "+laxity+" last reallocation time = "+lastReallocTime);
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////

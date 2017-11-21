@@ -0,0 +1,169 @@

/*************************************************************************
 *
 *  Paper:
 *  "Station Assignment with Reallocation"
 *  Austin Halper, Miguel A. Mosteiro, Yulia Rossikova, and Prudence W. H. Wong
 *  Proceedings of 14th International Symposium on Experimental Algorithms (SEA 2015)
 *
 *  Description: Simulator for CPR protocol
 *  Purpose: input generator, produce input file with arrival/departure events
 *
 *  Input: (stdio) 
 number of clients
 range of laxities
 *
 *  Output: file in XML format as follows. 
 For each client, the following record:
 <task>
	<id>...</id>
	<t_arrive>...</t_arrive>
	<size>...</size>
	<w_size>...</w_size>
	<t_leave>...</t_leave>
 </task>
 *
 *  Visible data fields: none
 *
 *  Visible methods: main
 *
 *
 *   Remarks
 *   -------
 *
 *
 *************************************************************************/
import java.io.*;
import java.util.*;
public class inputGen{
	public static void main(String[] args) throws IOException{
        // init
        int arrival=0;
        int laxity=0;
        int roundedlaxity=0;
        int departure=0;
        int n=0;
        int laxRange=0;
        int laxDist=0;
        int arrivalDist=0;
        Scanner input = new Scanner(System.in);
        Random rand = new Random();
        StringBuilder outputFileName = new StringBuilder();
        //input
        // input number of clients
        System.out.println("Enter the number of clients: ");
        n = input.nextInt();
        // input range of laxities
        System.out.println("Enter the maximum laxity: ");
        laxRange = input.nextInt();
        // input range of laxities
        do{
            System.out.println("Enter the laxity distribution (1=uniform, 2=small-biased, 3=large-biased): ");
            laxDist = input.nextInt();
        }while(laxDist<1 || laxDist>3);
        // input arrival distribution
        do{
            System.out.println("Enter the arrival distribution (1=uniform, 2=batched, 3=Poisson): ");
            arrivalDist = input.nextInt(); //!
        }while(arrivalDist<1 || arrivalDist>3);
        // initialize output file
        switch(laxDist){
            case 1:
                outputFileName.append("UnifLaxity");
                break;
            case 2:
                outputFileName.append("SmallBiasedLaxity");
                break;
            case 3:
                outputFileName.append("LargeBiasedLaxity");
                break;
        }
        switch(arrivalDist){
            case 1:
                outputFileName.append("UnifArrivals");
                break;
            case 2:
                outputFileName.append("BatchedArrivals");
                break;
            case 3:
                outputFileName.append("PoissonArrivals");
                break;
        }
        outputFileName.append(".xml");
        File file = new File(outputFileName.toString());
        if (file.exists()){
            System.out.println("File already exists");
            System.exit(0);
        }
        PrintWriter output = new PrintWriter(file);
        
        // output first line
        // <?xml version="1.0" encoding="UTF-8"?><input>
        output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?><input>");
        // output records
        for(int id=0;id<n;id++){
            // compute a client
            switch(arrivalDist){
                case 1: // uniform distribution
                    arrival = rand.nextInt(2*n);
                    break;
                case 2: // 3 batches of n/3 clients arriving at t=1, t=n/2, and t=n
                    if(id<n/3) arrival = 1;
                    else{
                        if (id<2*n/3) arrival = n/2;
                        else arrival = n;
                    }
                    break;
                case 3: // Poisson distribution with rate 0.7
                    arrival = arrival+poisson(0.7);
                    break;
            }
            departure = rand.nextInt(2*n-arrival)+arrival;
            switch(laxDist){
                case 1:
                    laxity = rand.nextInt(laxRange)+1; // uniform distribution
                    break;
                case 2:
                    if(Math.random()>.3) // with probability .7 draw a laxity in the lower half of the range
                        laxity = rand.nextInt(laxRange/2)+1;
                    else // with probability .3 draw a laxity in the upper half of the range
                        laxity = rand.nextInt(laxRange/2)+laxRange/2+1;
                    break;
                case 3:
                    if(Math.random()>.3) // with probability .7 draw a laxity in the upper half of the range
                        laxity = rand.nextInt(laxRange/2)+laxRange/2+1;
                    else // with probability .3 draw a laxity in the lower half of the range
                        laxity = rand.nextInt(laxRange/2)+1;
                    break;
            }
            roundedlaxity = 1<<(31-Integer.numberOfLeadingZeros(laxity));
            // output the client record
            output.println("<task>");
            output.println("   <id>"+id+"</id>");
            output.println("   <t_arrive>"+arrival+"</t_arrive>");
            output.println("   <size>"+laxity+"</size>");
            output.println("   <w_size>"+roundedlaxity+"</w_size>");
            output.println("   <t_leave>"+departure+"</t_leave>");
            output.println("</task>");
        }
        // output last line
        output.println("</input>");
    
        output.close();
    }
    
    // poisson arrivals generator
    private static int poisson(double mean) {
        int r = 0;
        Random random = new Random();
        double a = random.nextDouble();
        double p = Math.exp(-mean);
        
        while (a > p) {
            r++;
            a = a - p;
            p = p * mean / r;
        }
        return r;
    }
}

@@ -0,0 +1,201 @@

/*************************************************************************
 *
 *  Paper:
 *  "Station Assignment with Reallocation"
 *  Austin Halper, Miguel A. Mosteiro, Yulia Rossikova, and Prudence W. H. Wong
 *  Proceedings of 14th International Symposium on Experimental Algorithms (SEA 2015)
 *
 *  Description: Worst case simulator for CPR protocol
 *  Purpose: input generator, produce input file with arrival/departure events
 *
 *  Input: (stdio) 
 number of clients
 range of laxities
 *
 *  Output: file in TXT format as follows. 
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
public class inputGenWC{
	public static void main(String[] args) throws IOException{
        // init
        int arrival=0;
        int laxity=0;

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
        outputFileName.append("_" + n + "clients_" + laxRange +"w.txt");
        File file = new File(outputFileName.toString());
        if (file.exists()){
            System.out.println("File already exists");
            System.exit(0);
        }
        PrintWriter output = new PrintWriter(file);
		output.print("========================print out input==========================\n");
		output.print("it id  Arrive  weight    1/weight     Departure\n");
 
        // output records
		
/*		int n1s = 0;
		int n2s = 0;
		int n4s = 0;
	int n8s = 0;
		int n16s = 0;
	int n32s = 0;
		int n64s = 0;
	int n128s = 0;
		int n256s = 0;
		int n512s = 0;
		int n1024s = 0;
		int n2048s = 0;
	int n4096s = 0;
		int n8192s = 0;
		int n16384s = 0; */
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
                	if (laxRange ==1024) {
                	int power = (int)(Math.random()*7+4);
                	laxity = (int)(Math.pow(2,power)); // uniform distribution
					break;
                	}
					if (laxRange==4096) {
						int power = (int)(Math.random()*9+4);
	                	laxity = (int)(Math.pow(2,power)); // uniform distribution
						break;
					}
					if (laxRange == 16384){	
						int power = (int)(Math.random()*11+4);
	                	laxity = (int)(Math.pow(2,power)); // uniform distribution
						break;
					}
					
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
    /*        switch(laxity){
           
            	case 1: n1s++;break;
            	case 2: n2s++;break;
            	case 4: n4s++;break;
            	case 8: n8s++;break;
            	case 16: n16s++;break;
            	case 32: n32s++;break;
        	case 64: n64s++;break;
          	case 128: n128s++;break;
           	case 256: n256s++;break;
            	case 512: n512s++;break;
            	case 1024: n1024s++;break;
            	case 2048: n2048s++;break;
           	case 4096: n4096s++;break;
           	case 8192: n8192s++;break;
            	case 16384: n16384s++;break;
            }*/
    				output.printf("%8d   %8d   %5d   %20.14f   %8d\n",(int)(id), (int)(arrival), (int)(laxity), 1.0/laxity, (int)(departure));
    					}
  //      System.out.println("There are " + n1s + " 1s " + n2s + " 2s \n"+ n4s + " 4s\n "+ n8s + " 8s \n"+ n16s + " 16s \n"+ n32s + " 32s \n"+ n64s + " 64s \n"+ n128s + " 128s \n"+ n256s + " 256s \n" + n512s + " 512s \n" + n1024s + " 1024s \n" + n2048s + " 2048s \n" + n4096s + " 4096s \n" + n8192s + " 8192s \n" + n16384s + " 16384s \n");		
 
    
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

import java.io.*;
import java.util.Scanner;

public class Main {
    // Main variables
    static final int BUSY = 1,IDLE = 0,Q_LIMIT = 100; // Maximum queue size
    static double sim_time,time_last_event;
    static int server_status,num_in_q,num_custs_delayed;
    static double total_of_delays,area_num_in_q,area_server_status;
    static double[] time_next_event = new double[4];  // Matching size with C (3 elements)
    static double[] time_arrival = new double[Q_LIMIT + 1];  // Queue limit of 100 + 1 element
    static double mean_interarrival,mean_service,time_end;
    static int num_events;  // Number of events (like in C)
    static int next_event_type;
    static int customer_count=0;
    static int event_count=0;


    static BufferedReader infile;
    static BufferedWriter outfile,writer;

    public static void main(String[] args) {

        try {
            // Open input and output files
            infile = new BufferedReader(new FileReader("input.txt"));
            outfile = new BufferedWriter(new FileWriter("output.txt"));
            writer = new BufferedWriter(new FileWriter("output1.txt"));


            // Specify the number of events for the timing function
            num_events = 2;

            // Read input parameters
            Scanner scanner = new Scanner(infile);
            mean_interarrival = scanner.nextFloat();
            mean_service = scanner.nextFloat();
            time_end = scanner.nextFloat();

            // Write report heading and input parameters
            outfile.write("Single-server queueing system \n\n");
            outfile.write("Mean interarrival time " + mean_interarrival + " minutes\n\n");
            outfile.write("Mean service time " + mean_service + " minutes\n\n");
            outfile.write("Number of customers " + time_end + "\n\n");


            // Initialize the simulation
           initialize();

            // Run the simulation until it terminates after an end-simulation event (type 3) occurs
            while (num_custs_delayed<time_end) {
                // Determine the next event
                timing();

                // Update time-average statistical accumulators
                update_time_avg_stats();

                // Invoke the appropriate event function
                switch (next_event_type) {
                    case 1:
                        arrive();
                        break;
                    case 2:
                        depart();
                        break;

                }
            }
            report();
            // Close the files
            infile.close();
            outfile.close();
            writer.close();

        } catch (IOException e) {
            System.out.println("Error handling files: " + e.getMessage());
        }
    }
//
public static void initialize() {

    sim_time = 0.0;
    server_status = IDLE;
    num_in_q = 0;
    time_last_event = 0.0;
    num_custs_delayed = 0;
    total_of_delays = 0.0;
    area_num_in_q = 0.0;
    area_server_status = 0.0;

    // Initialize event list. Since no customers are present, the departure
    // (service completion) event is eliminated from consideration.
    // The end-simulation event (type 3) is scheduled for time time_end.
    time_next_event[1] = sim_time + expon(mean_interarrival);  // Schedule next arrival
    time_next_event[2] = 1.0e+30;  // No departure (event type 2)
    time_next_event[3] = time_end;  // End-simulation event scheduled
}
    public static double expon(double mean) {
        return -mean * Math.log(Math.random());
    }
    public static void update_time_avg_stats() {
        // Compute time since last event, and update last-event-time marker
        double time_since_last_event = sim_time - time_last_event;
        time_last_event = sim_time;

        // Update area under number-in-queue function
        area_num_in_q += num_in_q * time_since_last_event;

        // Update area under server-busy indicator function
        area_server_status += server_status * time_since_last_event;
    }
//
public static void timing() {
    try {
        double min_time_next_event = 1.0e+30;
        next_event_type = 0;

        // Determine the event type of the next event to occur
        for (int i = 1; i <= num_events; i++) {
            if (time_next_event[i] < min_time_next_event) {
                min_time_next_event = time_next_event[i];
                next_event_type = i;
            }
        }
        // Check to see whether the event list is empty
        if (next_event_type == 0) {
            // The event list is empty, so stop the simulation and report an error
            outfile.write(String.format("\nEvent list empty at time %f", sim_time));
            outfile.flush();
            System.exit(1);  // Exit the simulation
        }
        // The event list is not empty, so advance the simulation clock
        sim_time = min_time_next_event;
    } catch (IOException e) {
        e.printStackTrace();
    }
}
public static void arrive() {
    try {
        double delay;
        customer_count++;
        event_count++;
        // Schedule the next arrival
        time_next_event[1] = sim_time + expon(mean_interarrival);
        // Check whether the server is busy
        if (server_status == BUSY) {
            // Server is busy, increment the number of customers in queue
            num_in_q++;
            // Check for queue overflow
            if (num_in_q > Q_LIMIT) {
                // If overflow, stop the simulation and report the error
                outfile.write(String.format("\nOverflow of the array time_arrival at time %f", sim_time));
                outfile.flush();
                System.exit(2); // Exit with an error
            }

            // No overflow, store the time of arrival of the new customer at the end of the queue
            time_arrival[num_in_q] = sim_time;
            writer.write(event_count+".   "+"Next event: Customer " + customer_count + " Arrival\n");

        } else {
            // Server is idle, so arriving customer has a delay of 0
            delay = 0.0;
            total_of_delays += delay;
            // Increment the number of delayed customers and make the server busy
            num_custs_delayed++;
            server_status = BUSY;
            // Schedule the next departure (service completion)
            time_next_event[2] = sim_time + expon(mean_service);
            writer.write(event_count+".   "+"Next event: Customer " + customer_count + " Arrival\n");
            writer.write("--------No. of customers delayed: " + num_custs_delayed + "--------\n");
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
public static void depart() {
        try {
            event_count++;
            writer.write(event_count+".   "+"Next event: Customer " + customer_count + " Departure\n");
            if (num_in_q == 0) {
                // The queue is empty, make the server idle and eliminate the departure event
                server_status = IDLE;
                time_next_event[2] = 1.0e+30;  // Represent no next departure event
            } else {
                // The queue is not empty, decrement the number of customers in queue
                num_in_q--;
                // Compute the delay for the customer starting service
                double delay = sim_time - time_arrival[1];
                total_of_delays += delay;
                // Increment the number of delayed customers
                num_custs_delayed++;
                // Schedule the next departure event
                time_next_event[2] = sim_time + expon(mean_service);
                // Move each customer in the queue up one position
                for (int i = 1; i <= num_in_q; i++) {
                    time_arrival[i] = time_arrival[i + 1];
                }
                writer.write("--------No. of customers delayed: " + num_custs_delayed + "--------\n");
            }

        }

    catch (IOException e) {
        e.printStackTrace();
    }
}
public static void report() {
    try {
        // Write estimates of desired measures of performance to the file
        outfile.write(String.format("\n\nAverage delay in queue minutes\n\n",
                total_of_delays / num_custs_delayed));
        outfile.write(String.format("Average number in queue\n\n",
                area_num_in_q / sim_time));
        outfile.write(String.format("Server utilization\n\n",
                area_server_status / sim_time));
        outfile.write(String.format("Time simulation ended minutes\n", sim_time));

        // Ensure all data is written to the file
        outfile.flush();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
}


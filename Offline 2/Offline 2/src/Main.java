import java.io.*;
import java.util.Random;
public class Main {
    static int amount, bigs, initial_inv_level, inv_level, next_event_type, num_events,
            num_months, num_values_demand, smalls;
    static float area_holding, area_shortage, holding_cost, incremental_cost, maxlag,
            mean_interdemand, minlag, prob_distrib_demand[] = new float[26], setup_cost,
            shortage_cost, sim_time, time_last_event, time_next_event[] = new float[5],
            total_ordering_cost;
    static BufferedReader infile;
    static BufferedWriter outfile;

    public static void main(String[] args) throws IOException {
        int i, num_policies;
        infile = new BufferedReader(new FileReader("in.txt"));
        outfile = new BufferedWriter(new FileWriter("out.txt"));
        num_events = 4;
        String[] inputParams = infile.readLine().split("\\s+");
        initial_inv_level = Integer.parseInt(inputParams[0]); // I
        num_months = Integer.parseInt(inputParams[1]);        // M
        num_policies = Integer.parseInt(inputParams[2]);      // P

        inputParams = infile.readLine().split("\\s+");
        num_values_demand = Integer.parseInt(inputParams[0]);  // D
        mean_interdemand = Float.parseFloat(inputParams[1]);   // beta_D

// (Setup Cost, Incremental Cost, Holding Cost, Shortage Cost)
        inputParams = infile.readLine().split("\\s+");
        setup_cost = Float.parseFloat(inputParams[0]);         // K
        incremental_cost = Float.parseFloat(inputParams[1]);   // i
        holding_cost = Float.parseFloat(inputParams[2]);       // h
        shortage_cost = Float.parseFloat(inputParams[3]);      // pi

// (Minimum delivery lag, Maximum delivery lag)
        inputParams = infile.readLine().split("\\s+");
        minlag = Float.parseFloat(inputParams[0]);             // min_lag
        maxlag = Float.parseFloat(inputParams[1]);

        // (Cumulative distribution function of demand sizes)
        String[] distributionLine = infile.readLine().trim().split("\\s+");
        for ( i = 1; i <= num_values_demand; ++i) {
            // Parse each value separately
            prob_distrib_demand[i] = Float.parseFloat(distributionLine[i - 1]); // D cumulative values
        }


        outfile.write("Single-product inventory system\n\n");
        outfile.write(String.format("Initial inventory level%24d items\n\n", initial_inv_level));
        outfile.write(String.format("Number of demand sizes%25d\n\n", num_values_demand));
        outfile.write("Distribution function of demand sizes ");
        for (i = 1; i <= num_values_demand; ++i) {

            outfile.write(String.format("%8.3f", prob_distrib_demand[i]));
        }
        outfile.write("\n\n");
        outfile.write(String.format("Mean interdemand time%26.2f\n\n", mean_interdemand));
        outfile.write(String.format("Delivery lag range%29.2f to%10.2f months\n\n", minlag, maxlag));
        outfile.write(String.format("Length of the simulation%23d months\n\n", num_months));
        outfile.write(String.format("K =%6.1f i =%6.1f h =%6.1f pi =%6.1f\n\n",
                setup_cost, incremental_cost, holding_cost, shortage_cost));
        //System.out.println("no of policies"+num_policies);
        outfile.write(String.format("Number of policies: %d\n\n", num_policies));

        // Write header for policies
        outfile.write("Policies:\n");
        outfile.write("--------------------------------------------------------------------------------------------------\n");
        outfile.write(String.format(" %-15s %-20s %-20s %-20s %-20s\n", "Policy", "Avg_total_cost", "Avg_ordering_cost", "Avg_holding_cost", "Avg_shortage_cost"));
        outfile.write("--------------------------------------------------------------------------------------------------\n");



        // Run the simulation varying the inventory policy
       for (i = 1; i <= num_policies; ++i) {
         // Read the inventory policy, and initialize the simulation
           String[] policy = infile.readLine().split("\\s+");
            smalls = Integer.parseInt(policy[0]);
            bigs = Integer.parseInt(policy[1]);
            initialize();

            do {
                // Determine the next event
                timing();

                // Update time-average statistical accumulators
                update_time_avg_stats();

                // Invoke the appropriate event function
                switch (next_event_type) {
                    case 1:
                        orderArrival();
                        break;
                    case 2:
                        demand();
                        break;
                    case 4:
                        evaluate();
                        break;
                    case 3:
                        report();
                        break;
                }
            } while (next_event_type != 3);
        }
        outfile.write("--------------------------------------------------------------------------------------------------\n");
        infile.close();
        outfile.flush();
        outfile.close();
    }

    // Initialize the simulation parameters
    public static void initialize() {
        sim_time = 0.0f;
        inv_level = initial_inv_level;
        time_last_event = 0.0f;

        area_holding = 0.0f;
        area_shortage = 0.0f;
        total_ordering_cost = 0.0f;

        time_next_event[1] = Float.MAX_VALUE; // Order arrival event
        time_next_event[2] = sim_time + expon(mean_interdemand); // Demand event
        time_next_event[3] = num_months; // End simulation event
        time_next_event[4] = 0.0f ; // Evaluation event
    }

    static void timing() {
        float min_time_next_event = Float.MAX_VALUE;
        next_event_type = 0;

        // Determine the event type of the next event to occur
        for (int i = 1; i <= num_events; i++) {
            if (time_next_event[i] < min_time_next_event) {
                min_time_next_event = time_next_event[i];
                next_event_type = i;
            }
        }

        // Update the simulation time
        sim_time = min_time_next_event;
    }

    // Functions related to the inventory management system (placeholders)
    public static void orderArrival() { // Order arrival event function
        // Increment the inventory level by the amount ordered
        inv_level += amount;

        // Since no order is now outstanding, eliminate the order-arrival event from consideration
        time_next_event[1] = Float.MAX_VALUE;
    }


    public static void demand() { // Demand event function
        // Decrement the inventory level by a generated demand size
        inv_level -= random_integer(prob_distrib_demand);

        // Schedule the time of the next demand
        time_next_event[2] = sim_time + expon(mean_interdemand);
    }
    public static void evaluate() { // Inventory-evaluation event function.
        // Check whether the inventory level is less than smalls.
        if (inv_level < smalls) {
            // The inventory level is less than smalls, so place an order for the appropriate amount.
            amount = bigs - inv_level;
            total_ordering_cost += setup_cost + incremental_cost * amount;

            // Schedule the arrival of the order.
            time_next_event[1] = sim_time + uniform(minlag, maxlag);
        }

        // Regardless of the place-order decision, schedule the next inventory evaluation.
        time_next_event[4] = sim_time + 1.0f;
    }


    public static void report() { // Report generator function.
        // Compute and write estimates of desired measures of performance.
        float avg_holding_cost, avg_ordering_cost, avg_shortage_cost;

        try {
            avg_ordering_cost = total_ordering_cost / num_months;
            avg_holding_cost = holding_cost * area_holding / num_months;
            avg_shortage_cost = shortage_cost * area_shortage / num_months;

            // Write the report to the output using the specified format
            outfile.write(String.format("\n\n (%3d, %3d) %20.2f %20.2f %20.2f %20.2f\n",
                    smalls, bigs,
                    avg_ordering_cost + avg_holding_cost + avg_shortage_cost,
                    avg_ordering_cost, avg_holding_cost, avg_shortage_cost));
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }



    public static void update_time_avg_stats() { // Update area accumulators for time-average statistics.
        float time_since_last_event;

        // Compute time since last event, and update last-event-time marker.
        time_since_last_event = sim_time - time_last_event;
        time_last_event = sim_time;

        // Determine the status of the inventory level during the previous interval.
        // If the inventory level during the previous interval was negative, update area_shortage.
        // If it was positive, update area_holding. If it was zero, no update is needed.
        if (inv_level < 0) {
            area_shortage -= inv_level * time_since_last_event; // Inv_level is negative, thus subtracting.
        } else if (inv_level > 0) {
            area_holding += inv_level * time_since_last_event; // Inv_level is positive, thus adding.
        }
    }


    // Utility functions for random number generation and distributions
    static float expon(float mean) {
        Random rand = new Random();
        return (float) (-mean * Math.log(rand.nextFloat()));
    }

    static int random_integer(float[] prob_distrib) {
        Random rand = new Random();
        float u = rand.nextFloat();
        int i = 1;
        while (u >= prob_distrib[i]) {
            u -= prob_distrib[i];
            i++;
        }
        return i;
    }


    static float uniform(float a, float b) {
        Random rand = new Random();
        return a + (b - a) * rand.nextFloat();
    }
}
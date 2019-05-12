package it.polito.5t.sumo.processor;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.*;
import org.json.JSONException;

public class Main {

//    final static float INTERSECTION_POSITION_X = 1573.69f;
//    final static float INTERSECTION_POSITION_Y = 1791.14f;
//    final static int INTERSECTION_RADIUS = 100;
    final static float VIL_A_POSITION_X = 1527.14f;
    final static float VIL_A_POSITION_Y = 1702.63f;
    final static int VIL_A_RADIUS = 10;
    final static String VIL_A_HEADING = "H1";
    final static float VIL_B_POSITION_X = 1484.88f;
    final static float VIL_B_POSITION_Y = 1837.1f;
    final static int VIL_B_RADIUS = 10;
    final static String VIL_B_HEADING = "H2";
    final static float VIL_C_POSITION_X = 1620.51f;
    final static float VIL_C_POSITION_Y = 1879.5f;
    final static int VIL_C_RADIUS = 10;
    final static String VIL_C_HEADING = "H3";
    final static float VIL_D_POSITION_X = 1662.77f;
    final static float VIL_D_POSITION_Y = 1745.7f;
    final static int VIL_D_RADIUS = 10;
    final static String VIL_D_HEADING = "H4";

    final static float VIL_E_POSITION_X = VIL_A_POSITION_X;
    final static float VIL_E_POSITION_Y = VIL_A_POSITION_Y;
    final static int VIL_E_RADIUS = VIL_A_RADIUS;
    final static String VIL_E_HEADING = VIL_C_HEADING;
    final static float VIL_F_POSITION_X = VIL_C_POSITION_X;
    final static float VIL_F_POSITION_Y = VIL_C_POSITION_Y;
    final static int VIL_F_RADIUS = VIL_C_RADIUS;
    final static String VIL_F_HEADING = VIL_A_HEADING;

    final static float VIL_INT_POSITION_X = 1573.69f;
    final static float VIL_INT_POSITION_Y = 1791.14f;
    final static int VIL_INT_RADIUS = 15;
    final static String VIL_INT_HEADING = "%";

    final static int H1_LOWER = 343;
    final static int H1_UPPER = 73;
    final static int H2_LOWER = 73;
    final static int H2_UPPER = 163;
    final static int H3_LOWER = 163;
    final static int H3_UPPER = 253;
    final static int H4_LOWER = 253;
    final static int H4_UPPER = 343;

    static int NUMBER_OF_THREADS = -1;

    static final DecimalFormat df = new DecimalFormat("#.##");

    static int STEP_SIZE = -1;
    static float PENETRATION_RATE = -1;
    static float PENETRATION_RATE_OLD = -1;
    static int SEED = -1;
    static float ERROR = -1;
    static String SIM_NAME = "";

    static String INPUT_TABLE_NAME = "";
    static String OUTPUT_TABLE_NAME = "";

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("r", "random-seed", true, "Random seed");
        options.addOption("o", "penetration-rate-old", true, "Penetration rate original [%]");
        options.addOption("p", "penetration-rate", true, "Penetration rate [%]");
        options.addOption("s", "step-size", true, "Step size [s]");
        options.addOption("t", "threads", true, "Number of threads");
        options.addOption("a", "average-speed", false, "Process for Average Speed");
        options.addOption("d", "delta-time", false, "Process for Delta Time");
        options.addOption("n", "simulation-name", true, "Name of simulation");
        options.addOption("e", "error-percent", true, "Percentage of error");
        options.addOption("h", "help", false, "Show usage/help");
        String plan = "";
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("help") | !cmd.hasOption("step-size") | !cmd.hasOption("penetration-rate") | !cmd.hasOption("random-seed") | !cmd.hasOption("simulation-name")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("5T-SUMO-Processor", options);
                System.exit(1);
                return;
            }
            SIM_NAME = cmd.getOptionValue("simulation-name");
            STEP_SIZE = Integer.valueOf(cmd.getOptionValue("step-size"));
            PENETRATION_RATE = Float.valueOf(cmd.getOptionValue("penetration-rate"));
            if (cmd.hasOption("penetration-rate-old")) {
                PENETRATION_RATE_OLD = Float.valueOf(cmd.getOptionValue("penetration-rate-old"));
                if (PENETRATION_RATE_OLD < PENETRATION_RATE) {
                    System.err.println(Thread.currentThread().getName() + ":\tOriginal penetration rate cannot be less than new/reduced penetration rate.");
                    return;
                }
            }
            SEED = Integer.valueOf(cmd.getOptionValue("random-seed"));
            NUMBER_OF_THREADS = Integer.valueOf(cmd.getOptionValue("threads"));
            if (cmd.hasOption("error-percent")) {
                ERROR = Float.valueOf(cmd.getOptionValue("error-percent"));
            }
            if (cmd.hasOption("average-speed")) {
                plan = "a";
                if (cmd.hasOption("penetration-rate-old")) {
                    OUTPUT_TABLE_NAME = SIM_NAME + "_" + df.format(PENETRATION_RATE) + "prd_" + STEP_SIZE + "s";
                } else {
                    OUTPUT_TABLE_NAME = SIM_NAME + "_" + df.format(PENETRATION_RATE) + "pr_" + STEP_SIZE + "s";
                }
            } else if (cmd.hasOption("delta-time")) {
                plan = "d";
                if (cmd.hasOption("penetration-rate-old")) {
                    OUTPUT_TABLE_NAME = SIM_NAME + "_" + df.format(PENETRATION_RATE) + "prd_" + STEP_SIZE + "s";
                } else {
                    OUTPUT_TABLE_NAME = SIM_NAME + "_" + df.format(PENETRATION_RATE) + "pr_" + STEP_SIZE + "s";
                }
            } else {
                plan = "b";
                if (cmd.hasOption("penetration-rate-old")) {
                    OUTPUT_TABLE_NAME = SIM_NAME + "_" + df.format(PENETRATION_RATE) + "prd_" + STEP_SIZE + "s_" + df.format(ERROR) + "e";
                } else {
                    OUTPUT_TABLE_NAME = SIM_NAME + "_" + df.format(PENETRATION_RATE) + "pr_" + STEP_SIZE + "s_" + df.format(ERROR) + "e";
                }
            }

        } catch (org.apache.commons.cli.ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("5T-SUMO-Processor-VILs", options);
            System.exit(1);
            return;
        }

        if (plan.equals("a")) {
            System.out.println(Thread.currentThread().getName() + ":\tProcessing for Average Speeds");
        } else if (plan.equals("d")) {
            System.out.println(Thread.currentThread().getName() + ":\tProcessing for Delta Times");
        } else if (plan.equals("b")) {
            System.out.println(Thread.currentThread().getName() + ":\tProcessing for both Delta Times and Average Speeds");
        }
        if (PENETRATION_RATE_OLD != -1) {
            INPUT_TABLE_NAME = "raw_planb_" + SIM_NAME + "_" + df.format(PENETRATION_RATE_OLD) + "pr_" + SEED + "seed";
        } else {
            INPUT_TABLE_NAME = "raw_planb_" + SIM_NAME + "_" + df.format(PENETRATION_RATE) + "pr_" + SEED + "seed";
        }
        System.out.println(Thread.currentThread().getName() + ":\tSTEP_SIZE=" + STEP_SIZE + "s");
        System.out.println(Thread.currentThread().getName() + ":\tSEED=" + SEED);
        System.out.println(Thread.currentThread().getName() + ":\tNUMBER_OF_THREADS=" + NUMBER_OF_THREADS);
        System.out.println(Thread.currentThread().getName() + ":\tPENETRATION_RATE=" + df.format(PENETRATION_RATE) + "%");
        if (PENETRATION_RATE_OLD != -1) {
            System.out.println(Thread.currentThread().getName() + ":\tPENETRATION_RATE_OLD=" + df.format(PENETRATION_RATE_OLD) + "%");
        }
        if (ERROR != -1) {
            System.out.println(Thread.currentThread().getName() + ":\tERROR=" + df.format(ERROR) + "%");
        }
        System.out.println(Thread.currentThread().getName() + ":\tInput table name = " + INPUT_TABLE_NAME);
        System.out.println(Thread.currentThread().getName() + ":\tOutput table name = " + OUTPUT_TABLE_NAME);

        DBHelper dbHelper = new DBHelper();
        dbHelper.jdbcConnect();
        int maxStep = dbHelper.queryMaxStep();
        int threadStart = STEP_SIZE;
        int threadEnd = -1;
        int threadStep = Math.round(maxStep / NUMBER_OF_THREADS / STEP_SIZE) * STEP_SIZE;
        dbHelper.jdbcClose();

        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            threadEnd = threadStart + threadStep;
            System.out.println(Thread.currentThread().getName() + ":\tTotal=" + maxStep + " Thread-" + i + " " + threadStart + " -> " + threadEnd);
            if (plan.equals("a")) {
                new Thread(new ProcessorRunnableAverageSpeedVILs(threadStart, threadEnd)).start();
            } else if (plan.equals("d")) {
                new Thread(new ProcessorRunnableDeltaTimeVils(threadStart, threadEnd)).start();
            } else if (plan.equals("b")) {
                new Thread(new ProcessorRunnableDeltaTimeAndAverageSpeedVils(threadStart, threadEnd)).start();
            }
            threadStart += threadStep + STEP_SIZE;
        }
    }

    public static class ProcessorRunnableAverageSpeedVILs implements Runnable {

        int start;
        int stop;

        public ProcessorRunnableAverageSpeedVILs(int start, int stop) {
            this.start = start;
            this.stop = stop;
        }

        public void run() {
            DBHelper dbHelper = new DBHelper();
            dbHelper.jdbcConnect();
            float vilA = -1;
            float vilB = -1;
            float vilC = -1;
            float vilD = -1;
            float vilE = -1;
            float vilF = -1;
            float vilInt = -1;
            float vilIntA2C = -1;
            float vilIntB2C = -1;
            float vilIntD2C = -1;
            float vilIntC2A = -1;
            float vilIntB2A = -1;
            float vilIntD2A = -1;
            float vilIntB2D = -1;
            float vilIntD2B = -1;
            try {
                if (PENETRATION_RATE_OLD != -1) {
                    int originalIDs = dbHelper.queryDistinctIds();
                    int newIDs = (int) (originalIDs * PENETRATION_RATE / PENETRATION_RATE_OLD);
                    System.out.println(Thread.currentThread().getName() + ":\tPenetration rate " + PENETRATION_RATE_OLD + "% -> " + PENETRATION_RATE + "% (Ratio is " + asFraction(PENETRATION_RATE, PENETRATION_RATE_OLD) + ")");
                    System.out.println(Thread.currentThread().getName() + ":\tOriginal number of uniquie vehicles " + originalIDs + " reduced to " + newIDs);

                    // entry vils
                    dbHelper.createVilTempTable("t_vil_a_h1", VIL_A_POSITION_X, VIL_A_POSITION_Y, VIL_A_RADIUS, VIL_A_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_b_h2", VIL_B_POSITION_X, VIL_B_POSITION_Y, VIL_B_RADIUS, VIL_B_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_c_h3", VIL_C_POSITION_X, VIL_C_POSITION_Y, VIL_C_RADIUS, VIL_C_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_d_h4", VIL_D_POSITION_X, VIL_D_POSITION_Y, VIL_D_RADIUS, VIL_D_HEADING, newIDs);

                    // exit vils
                    dbHelper.createVilTempTable("t_vil_a_h3", VIL_A_POSITION_X, VIL_A_POSITION_Y, VIL_A_RADIUS, VIL_C_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_c_h1", VIL_C_POSITION_X, VIL_C_POSITION_Y, VIL_C_RADIUS, VIL_A_HEADING, newIDs);

                    // intersection vils
                    dbHelper.createVilTempTable("t_vil_int", VIL_INT_POSITION_X, VIL_INT_POSITION_Y, VIL_INT_RADIUS, VIL_INT_HEADING, newIDs);
                } else {
                    // entry vils
                    dbHelper.createVilTempTable("t_vil_a_h1", VIL_A_POSITION_X, VIL_A_POSITION_Y, VIL_A_RADIUS, VIL_A_HEADING);
                    dbHelper.createVilTempTable("t_vil_b_h2", VIL_B_POSITION_X, VIL_B_POSITION_Y, VIL_B_RADIUS, VIL_B_HEADING);
                    dbHelper.createVilTempTable("t_vil_c_h3", VIL_C_POSITION_X, VIL_C_POSITION_Y, VIL_C_RADIUS, VIL_C_HEADING);
                    dbHelper.createVilTempTable("t_vil_d_h4", VIL_D_POSITION_X, VIL_D_POSITION_Y, VIL_D_RADIUS, VIL_D_HEADING);

                    // exit vils
                    dbHelper.createVilTempTable("t_vil_a_h3", VIL_A_POSITION_X, VIL_A_POSITION_Y, VIL_A_RADIUS, VIL_C_HEADING);
                    dbHelper.createVilTempTable("t_vil_c_h1", VIL_C_POSITION_X, VIL_C_POSITION_Y, VIL_C_RADIUS, VIL_A_HEADING);

                    // intersection vils
                    dbHelper.createVilTempTable("t_vil_int", VIL_INT_POSITION_X, VIL_INT_POSITION_Y, VIL_INT_RADIUS, VIL_INT_HEADING);
                }
                for (int i = start; i <= stop; i = i + STEP_SIZE) {
                    System.out.println(Thread.currentThread().getName() + ":\t" + (i - STEP_SIZE) + " -> " + (i));
                    vilA = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_a_h1", ".");
                    vilB = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_b_h2", ".");
                    vilC = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_c_h3", ".");
                    vilD = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_d_h4", ".");
                    vilE = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_a_h3", ".");
                    vilF = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_c_h1", ".");
                    vilInt = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", ".");
                    vilIntA2C = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([n][a-c])");
                    vilIntB2C = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([n][d])");
                    vilIntD2C = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([n][e])");
                    vilIntC2A = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([s][a-b])");
                    vilIntB2A = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([s][c])");
                    vilIntD2A = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([s][d])");
                    vilIntB2D = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([n][g])");
                    vilIntD2B = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([n][f])");
                    dbHelper.InsertRowVILAverageSpeed(i - STEP_SIZE, i, vilA, vilB, vilC, vilD, vilE, vilF, vilInt, vilIntA2C, vilIntB2C, vilIntD2C, vilIntC2A, vilIntB2A, vilIntD2A, vilIntB2D, vilIntD2B);
                }
            } catch (SQLException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            dbHelper.jdbcClose();
        }
    }

    public static class ProcessorRunnableDeltaTimeVils implements Runnable {

        int start;
        int stop;

        public ProcessorRunnableDeltaTimeVils(int start, int stop) {
            this.start = start;
            this.stop = stop;
        }

        public void run() {
            DBHelper dbHelper = new DBHelper();
            dbHelper.jdbcConnect();

            float deltaTimeA2C = -1;
            float deltaTimeB2C = -1;
            float deltaTimeD2C = -1;
            float deltaTimeB2A = -1;
            float deltaTimeC2A = -1;
            float deltaTimeD2A = -1;
            float deltaTimeB2D = -1;
            float deltaTimeD2B = -1;
            try {
                if (PENETRATION_RATE_OLD != -1) {
                    int originalIDs = dbHelper.queryDistinctIds();
                    int newIDs = (int) (originalIDs * PENETRATION_RATE / PENETRATION_RATE_OLD);
                    System.out.println(Thread.currentThread().getName() + ":\tPenetration rate " + PENETRATION_RATE_OLD + "% -> " + PENETRATION_RATE + "% (Ratio is " + asFraction(PENETRATION_RATE, PENETRATION_RATE_OLD) + ")");
                    System.out.println(Thread.currentThread().getName() + ":\tOriginal number of uniquie vehicles " + originalIDs + " reduced to " + newIDs);

                    // entry vils
                    dbHelper.createVilTempTable("t_vil_a_h1", VIL_A_POSITION_X, VIL_A_POSITION_Y, VIL_A_RADIUS, VIL_A_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_b_h2", VIL_B_POSITION_X, VIL_B_POSITION_Y, VIL_B_RADIUS, VIL_B_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_c_h3", VIL_C_POSITION_X, VIL_C_POSITION_Y, VIL_C_RADIUS, VIL_C_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_d_h4", VIL_D_POSITION_X, VIL_D_POSITION_Y, VIL_D_RADIUS, VIL_D_HEADING, newIDs);

                    // exit vils
                    dbHelper.createVilTempTable("t_vil_a_h3", VIL_A_POSITION_X, VIL_A_POSITION_Y, VIL_A_RADIUS, VIL_C_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_b_h4", VIL_B_POSITION_X, VIL_B_POSITION_Y, VIL_B_RADIUS, VIL_D_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_c_h1", VIL_C_POSITION_X, VIL_C_POSITION_Y, VIL_C_RADIUS, VIL_A_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_d_h2", VIL_D_POSITION_X, VIL_D_POSITION_Y, VIL_D_RADIUS, VIL_B_HEADING, newIDs);
                } else {
                    // entry vils
                    dbHelper.createVilTempTable("t_vil_a_h1", VIL_A_POSITION_X, VIL_A_POSITION_Y, VIL_A_RADIUS, VIL_A_HEADING);
                    dbHelper.createVilTempTable("t_vil_b_h2", VIL_B_POSITION_X, VIL_B_POSITION_Y, VIL_B_RADIUS, VIL_B_HEADING);
                    dbHelper.createVilTempTable("t_vil_c_h3", VIL_C_POSITION_X, VIL_C_POSITION_Y, VIL_C_RADIUS, VIL_C_HEADING);
                    dbHelper.createVilTempTable("t_vil_d_h4", VIL_D_POSITION_X, VIL_D_POSITION_Y, VIL_D_RADIUS, VIL_D_HEADING);

                    // exit vils
                    dbHelper.createVilTempTable("t_vil_a_h3", VIL_A_POSITION_X, VIL_A_POSITION_Y, VIL_A_RADIUS, VIL_C_HEADING);
                    dbHelper.createVilTempTable("t_vil_b_h4", VIL_B_POSITION_X, VIL_B_POSITION_Y, VIL_B_RADIUS, VIL_D_HEADING);
                    dbHelper.createVilTempTable("t_vil_c_h1", VIL_C_POSITION_X, VIL_C_POSITION_Y, VIL_C_RADIUS, VIL_A_HEADING);
                    dbHelper.createVilTempTable("t_vil_d_h2", VIL_D_POSITION_X, VIL_D_POSITION_Y, VIL_D_RADIUS, VIL_B_HEADING);
                }
                for (int i = start; i <= stop; i = i + STEP_SIZE) {
                    System.out.println(Thread.currentThread().getName() + ":\t" + (i - STEP_SIZE) + " -> " + (i));
                    deltaTimeA2C = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_a_h1", "t_vil_c_h1");
                    deltaTimeB2C = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_b_h2", "t_vil_c_h1");
                    deltaTimeD2C = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_d_h4", "t_vil_c_h1");
                    deltaTimeB2A = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_b_h2", "t_vil_a_h3");
                    deltaTimeC2A = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_c_h3", "t_vil_a_h3");
                    deltaTimeD2A = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_d_h4", "t_vil_a_h3");
                    deltaTimeB2D = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_b_h2", "t_vil_d_h2");
                    deltaTimeD2B = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_d_h4", "t_vil_b_h4");
                    dbHelper.InsertRowVILDeltaTime(i - STEP_SIZE, i, deltaTimeA2C, deltaTimeB2C, deltaTimeD2C, deltaTimeC2A, deltaTimeB2A, deltaTimeD2A, deltaTimeB2D, deltaTimeD2B);
                }
            } catch (SQLException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            dbHelper.jdbcClose();
        }
    }

    public static class ProcessorRunnableDeltaTimeAndAverageSpeedVils implements Runnable {

        int start;
        int stop;

        public ProcessorRunnableDeltaTimeAndAverageSpeedVils(int start, int stop) {
            this.start = start;
            this.stop = stop;
        }

        public void run() {
            DBHelper dbHelper = new DBHelper();
            dbHelper.jdbcConnect();

            float deltaTimeA2C = -1;
            float deltaTimeB2C = -1;
            float deltaTimeD2C = -1;
            float deltaTimeB2A = -1;
            float deltaTimeC2A = -1;
            float deltaTimeD2A = -1;
            float deltaTimeB2D = -1;
            float deltaTimeD2B = -1;
            float vilA = -1;
            float vilB = -1;
            float vilC = -1;
            float vilD = -1;
            float vilE = -1;
            float vilF = -1;
            float vilInt = -1;
            float vilIntA2C = -1;
            float vilIntB2C = -1;
            float vilIntD2C = -1;
            float vilIntC2A = -1;
            float vilIntB2A = -1;
            float vilIntD2A = -1;
            float vilIntB2D = -1;
            float vilIntD2B = -1;
            try {
                if (PENETRATION_RATE_OLD != -1) {
                    int originalIDs = dbHelper.queryDistinctIds();
                    int newIDs = (int) (originalIDs * PENETRATION_RATE / PENETRATION_RATE_OLD);
                    System.out.println(Thread.currentThread().getName() + ":\tPenetration rate " + PENETRATION_RATE_OLD + "% -> " + PENETRATION_RATE + "% (Ratio is " + asFraction(PENETRATION_RATE, PENETRATION_RATE_OLD) + ")");
                    if (ERROR != -1) {
                        System.out.println(Thread.currentThread().getName() + ":\tOriginal number of uniquie vehicles " + originalIDs + " reduced to " + newIDs + ". " + ((int) (newIDs * ERROR / 100)) + " are skipped randomly.");
                    } else {
                        System.out.println(Thread.currentThread().getName() + ":\tOriginal number of uniquie vehicles " + originalIDs + " reduced to " + newIDs);
                    }

                    // entry vils
                    dbHelper.createVilTempTable("t_vil_a_h1", VIL_A_POSITION_X, VIL_A_POSITION_Y, VIL_A_RADIUS, VIL_A_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_b_h2", VIL_B_POSITION_X, VIL_B_POSITION_Y, VIL_B_RADIUS, VIL_B_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_c_h3", VIL_C_POSITION_X, VIL_C_POSITION_Y, VIL_C_RADIUS, VIL_C_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_d_h4", VIL_D_POSITION_X, VIL_D_POSITION_Y, VIL_D_RADIUS, VIL_D_HEADING, newIDs);

                    // exit vils
                    dbHelper.createVilTempTable("t_vil_a_h3", VIL_A_POSITION_X, VIL_A_POSITION_Y, VIL_A_RADIUS, VIL_C_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_b_h4", VIL_B_POSITION_X, VIL_B_POSITION_Y, VIL_B_RADIUS, VIL_D_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_c_h1", VIL_C_POSITION_X, VIL_C_POSITION_Y, VIL_C_RADIUS, VIL_A_HEADING, newIDs);
                    dbHelper.createVilTempTable("t_vil_d_h2", VIL_D_POSITION_X, VIL_D_POSITION_Y, VIL_D_RADIUS, VIL_B_HEADING, newIDs);

                    // intersection vils
                    dbHelper.createVilTempTable("t_vil_int", VIL_INT_POSITION_X, VIL_INT_POSITION_Y, VIL_INT_RADIUS, VIL_INT_HEADING, newIDs);
                } else {
                    // entry vils
                    dbHelper.createVilTempTable("t_vil_a_h1", VIL_A_POSITION_X, VIL_A_POSITION_Y, VIL_A_RADIUS, VIL_A_HEADING);
                    dbHelper.createVilTempTable("t_vil_b_h2", VIL_B_POSITION_X, VIL_B_POSITION_Y, VIL_B_RADIUS, VIL_B_HEADING);
                    dbHelper.createVilTempTable("t_vil_c_h3", VIL_C_POSITION_X, VIL_C_POSITION_Y, VIL_C_RADIUS, VIL_C_HEADING);
                    dbHelper.createVilTempTable("t_vil_d_h4", VIL_D_POSITION_X, VIL_D_POSITION_Y, VIL_D_RADIUS, VIL_D_HEADING);

                    // exit vils
                    dbHelper.createVilTempTable("t_vil_a_h3", VIL_A_POSITION_X, VIL_A_POSITION_Y, VIL_A_RADIUS, VIL_C_HEADING);
                    dbHelper.createVilTempTable("t_vil_b_h4", VIL_B_POSITION_X, VIL_B_POSITION_Y, VIL_B_RADIUS, VIL_D_HEADING);
                    dbHelper.createVilTempTable("t_vil_c_h1", VIL_C_POSITION_X, VIL_C_POSITION_Y, VIL_C_RADIUS, VIL_A_HEADING);
                    dbHelper.createVilTempTable("t_vil_d_h2", VIL_D_POSITION_X, VIL_D_POSITION_Y, VIL_D_RADIUS, VIL_B_HEADING);

                    // intersection vils
                    dbHelper.createVilTempTable("t_vil_int", VIL_INT_POSITION_X, VIL_INT_POSITION_Y, VIL_INT_RADIUS, VIL_INT_HEADING);
                }
                for (int i = start; i <= stop; i = i + STEP_SIZE) {
                    System.out.println(Thread.currentThread().getName() + ":\t" + (i - STEP_SIZE) + " -> " + (i));
                    deltaTimeA2C = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_a_h1", "t_vil_c_h1");
                    deltaTimeB2C = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_b_h2", "t_vil_c_h1");
                    deltaTimeD2C = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_d_h4", "t_vil_c_h1");
                    deltaTimeB2A = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_b_h2", "t_vil_a_h3");
                    deltaTimeC2A = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_c_h3", "t_vil_a_h3");
                    deltaTimeD2A = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_d_h4", "t_vil_a_h3");
                    deltaTimeB2D = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_b_h2", "t_vil_d_h2");
                    deltaTimeD2B = dbHelper.queryDeltaTimeBetween2Vils(i - STEP_SIZE, i, "t_vil_d_h4", "t_vil_b_h4");
                    dbHelper.InsertRowVILDeltaTime(i - STEP_SIZE, i, deltaTimeA2C, deltaTimeB2C, deltaTimeD2C, deltaTimeC2A, deltaTimeB2A, deltaTimeD2A, deltaTimeB2D, deltaTimeD2B);
                    vilA = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_a_h1", ".");
                    vilB = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_b_h2", ".");
                    vilC = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_c_h3", ".");
                    vilD = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_d_h4", ".");
                    vilE = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_a_h3", ".");
                    vilF = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_c_h1", ".");
                    vilInt = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", ".");
                    vilIntA2C = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([n][a-c])");
                    vilIntB2C = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([n][d])");
                    vilIntD2C = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([n][e])");
                    vilIntC2A = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([s][a-b])");
                    vilIntB2A = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([s][c])");
                    vilIntD2A = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([s][d])");
                    vilIntB2D = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([n][g])");
                    vilIntD2B = dbHelper.queryAverageSpeedAtVil(i - STEP_SIZE, i, "t_vil_int", "([n][f])");
                    dbHelper.InsertRowVILAverageSpeed(i - STEP_SIZE, i, vilA, vilB, vilC, vilD, vilE, vilF, vilInt, vilIntA2C, vilIntB2C, vilIntD2C, vilIntC2A, vilIntB2A, vilIntD2A, vilIntB2D, vilIntD2B);
                }
            } catch (SQLException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }

            dbHelper.jdbcClose();
        }
    }

    public static float gcm(float a, float b) {
        return b == 0 ? a : gcm(b, a % b);
    }

    public static String asFraction(float a, float b) {
        float gcm = gcm(a, b);
        return (a / gcm) + "/" + (b / gcm);
    }
}

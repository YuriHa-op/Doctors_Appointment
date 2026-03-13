package server;

import database.JsonDatabase;
import net.ClinicService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class ServerMain {

    private final int port;
    private final JsonDatabase database;
    private final ActivityLogger activityLogger;
    private final AuthService authService;
    private final ScheduleService scheduleService;
    private final CallbackManager callbackManager;

    private Registry registry;
    private ClinicServiceImpl clinicService;
    private volatile boolean running = false;

    public ServerMain(int port, String dataDir) {
        this.port = port;
        this.database = new JsonDatabase(dataDir);
        this.activityLogger = new ActivityLogger(dataDir + "/activity_log.json");
        this.callbackManager = new CallbackManager();
        this.authService = new AuthService(database, activityLogger);
        this.scheduleService = new ScheduleService(database, activityLogger, callbackManager);
    }

    // Start the RMI registry and bind the clinic service

    public void start() {
        if (running) {
            System.out.println("[Server] Already running.");
            return;
        }
        try {
            registry = LocateRegistry.createRegistry(port);
            clinicService = new ClinicServiceImpl(authService, scheduleService, callbackManager);
            registry.rebind("ClinicService", clinicService);
            running = true;
            activityLogger.log("SERVER", "START", "Server started on port " + port);
            System.out.println("[Server] RMI Registry started on port " + port);
            System.out.println("[Server] ClinicService bound via rebind.");
        } catch (Exception e) {
            System.out.println("[Server] Failed to start: " + e.getMessage());
        }
    }

    // Unbind the service and stop the registry

    public void stop() {
        if (!running) {
            System.out.println("[Server] Not running.");
            return;
        }
        try {
            if (registry != null) {
                registry.unbind("ClinicService");
            }
        } catch (Exception e) {
            // ignore
        }
        running = false;
        activityLogger.log("SERVER", "STOP", "Server stopped.");
        System.out.println("[Server] Stopped.");
    }

    // Print activity log entries
    public void printLog() {
        activityLogger.printLog();
    }

    //MAIN 

    public static void main(String[] args) {
        String dataDir = "src/main/resources";
        ServerMain server = new ServerMain(1099, dataDir);

        System.out.println("=== WellnessLink Server (RMI) ===");
        System.out.println("Commands: start, stop, log, status, exit");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String cmd = scanner.nextLine().trim().toLowerCase();

            switch (cmd) {
                case "start":
                    server.start();
                    break;
                case "stop":
                    server.stop();
                    break;
                case "log":
                    server.printLog();
                    break;
                case "status":
                    System.out.println("[Server] " + (server.running ? "Running" : "Stopped"));
                    break;
                case "exit":
                    server.stop();
                    System.out.println("[Server] Exiting.");
                    scanner.close();
                    System.exit(0);
                    return;
                default:
                    System.out.println("Unknown command. Use: start, stop, log, status, exit");
            }
        }
    }
}
# Socket-to-RMI Migration Documentation

## WellnessLink — Migration from Java Sockets to Java RMI

**Date:** March 12, 2026  
**Project:** WellnessLink — Clinic Appointment Scheduler  
**Scope:** Replace the TCP socket-based client–server communication with Java RMI (Remote Method Invocation), using `rebind` for service registration.

---

## 1. Overview — What Changed and Why

### Before (Socket-Based Architecture)
The original application used **raw TCP sockets** for communication:
- The **server** (`ServerMain`) opened a `ServerSocket` on port 9090 and spawned a new thread (`ClientHandler`) per client connection via `ExecutorService`.
- The **client** (`SocketClient`) opened a new `Socket` for every request, serialized a `Request` object to XML text, sent it over the socket, read the XML response, and deserialized it into a `Response` object.
- The XML serialization was handled by `ProtocolUtils` (for Request/Response wire format) and `SimpleXMLParser` (for parsing XML tag→value maps).

### After (RMI-Based Architecture)
The application now uses **Java RMI** for communication:
- The **server** creates an RMI `Registry` and exposes a remote `ClinicService` interface via `registry.rebind()`.
- The **client** looks up the remote `ClinicService` via `Naming.lookup()` and calls `processRequest(Request)` directly — no manual socket management, no XML serialization for transport.
- `Request` and `Response` objects are passed as Java objects (serialized automatically by RMI).

### Why RMI?
| Aspect | Sockets | RMI |
|--------|---------|-----|
| Transport | Manual TCP connection per request | Automatic via RMI runtime |
| Serialization | Manual XML string building/parsing | Automatic Java serialization |
| Error handling | IOException per connection | RemoteException per call |
| Thread management | Manual thread pool (ExecutorService) | Automatic by RMI runtime |
| Type safety | Strings (XML) passed and parsed | Java objects passed directly |

---

## 2. Step-by-Step Migration Process

### Step 1: Make Request and Response Serializable

**Files modified:**
- `src/main/java/net/Request.java`
- `src/main/java/net/Response.java`

**What was done:**
- Added `implements java.io.Serializable` to both classes.
- Added `private static final long serialVersionUID = 1L;` to each class.
- Updated Javadoc comments from "XML representation of..." to "Represents a ... sent/returned via RMI."

**Why:**
Java RMI automatically serializes method parameters and return values when sending them between JVMs. Both `Request` and `Response` are passed across the network in RMI calls, so they must implement `Serializable`.

**Code change (Request.java):**
```java
// BEFORE
public class Request {
    private String action;
    ...

// AFTER
import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private String action;
    ...
```

**Code change (Response.java):**
```java
// BEFORE
public class Response {
    private String status;
    ...

// AFTER
import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private String status;
    ...
```

---

### Step 2: Create the Remote Interface — `ClinicService`

**File created:** `src/main/java/net/ClinicService.java`

**What was done:**
- Created a new interface `ClinicService` that extends `java.rmi.Remote`.
- Defined a single method: `Response processRequest(Request request) throws RemoteException`.
- Placed it in the `net` package (shared between client and server).

**Why:**
In Java RMI, the remote interface defines the contract between client and server. We chose a single `processRequest` method (instead of one method per action) to minimize changes — the existing action-routing logic (switch statement) stays the same, just moved from `ClientHandler` to the implementation.

**Code:**
```java
package net;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClinicService extends Remote {
    Response processRequest(Request request) throws RemoteException;
}
```

---

### Step 3: Create the Remote Implementation — `ClinicServiceImpl`

**File created:** `src/main/java/server/ClinicServiceImpl.java`

**What was done:**
- Created `ClinicServiceImpl extends UnicastRemoteObject implements ClinicService`.
- Moved the request-routing `switch` statement from the old `ClientHandler.process()` method into `processRequest()`.
- Constructor accepts `AuthService` and `ScheduleService` (same as `ClientHandler` did).

**Why:**
`UnicastRemoteObject` provides the RMI export mechanism — it makes the object remotely accessible. The routing logic is identical to the old `ClientHandler.process()` — we just changed where it lives.

**Key code:**
```java
public class ClinicServiceImpl extends UnicastRemoteObject implements ClinicService {

    private final AuthService authService;
    private final ScheduleService scheduleService;

    public ClinicServiceImpl(AuthService authService, ScheduleService scheduleService)
            throws RemoteException {
        super();
        this.authService = authService;
        this.scheduleService = scheduleService;
    }

    @Override
    public Response processRequest(Request request) throws RemoteException {
        String action = request.getAction() != null ? request.getAction().toUpperCase() : "";
        switch (action) {
            case "LOGIN":                 return authService.login(request);
            case "REGISTER":              return authService.register(request);
            case "CREATE_SCHEDULE":       return scheduleService.createSchedule(request);
            // ... all other cases identical to old ClientHandler.process()
            default:
                Response err = new Response();
                err.setStatus("error");
                err.setCode(400);
                err.setMessage("Unknown action: " + action);
                return err;
        }
    }
}
```

---

### Step 4: Rewrite ServerMain for RMI

**File modified:** `src/main/java/server/ServerMain.java`

**What was removed:**
- `ServerSocket`, `Socket`, `ExecutorService`, `Thread` (accept thread) — all socket-related fields.
- Imports: `java.io.IOException`, `java.net.ServerSocket`, `java.net.Socket`, `java.util.concurrent.*`.
- The `start()` method's socket-accept loop.
- The `stop()` method's `serverSocket.close()` call.
- `pool.shutdownNow()` in the exit command.

**What was added:**
- `Registry` field and `ClinicServiceImpl` field.
- Import: `java.rmi.registry.LocateRegistry`, `java.rmi.registry.Registry`.
- `start()`: Creates RMI registry via `LocateRegistry.createRegistry(port)`, creates `ClinicServiceImpl`, binds it via `registry.rebind("ClinicService", clinicService)`.
- `stop()`: Calls `registry.unbind("ClinicService")`.
- Port changed from 9090 to 1099 (standard RMI port).
- Data directory changed from `src/main/xmldata` to `src/main/resources`.

**Why `rebind` instead of `bind`:**
- `bind()` throws `AlreadyBoundException` if the name is already registered (e.g., after a server restart without clean shutdown).
- `rebind()` overwrites any existing binding, making the server more resilient to restarts. This was a specific project requirement.

**Code (start method):**
```java
// BEFORE — Socket-based
public void start() {
    serverSocket = new ServerSocket(port);
    running = true;
    acceptThread = new Thread(() -> {
        while (running) {
            Socket client = serverSocket.accept();
            pool.submit(new ClientHandler(client, authService, scheduleService));
        }
    });
    acceptThread.start();
}

// AFTER — RMI-based
public void start() {
    registry = LocateRegistry.createRegistry(port);
    clinicService = new ClinicServiceImpl(authService, scheduleService);
    registry.rebind("ClinicService", clinicService);
    running = true;
}
```

---

### Step 5: Rewrite ClientMain for RMI Lookup

**File modified:** `src/main/java/client/ClientMain.java`

**What was removed:**
- `SocketClient` creation (`new SocketClient(host, port)`).
- Import: `net.SocketClient`.

**What was added:**
- RMI lookup: `ClinicService clinicService = (ClinicService) Naming.lookup("rmi://localhost:1099/ClinicService")`.
- Import: `net.ClinicService`, `java.rmi.Naming`.
- Error handling: If RMI lookup fails, shows a dialog with the error message.

**Code:**
```java
// BEFORE
SocketClient socketClient = new SocketClient(host, port);
MainFrame frame = new MainFrame(socketClient);

// AFTER
ClinicService clinicService = (ClinicService) Naming.lookup("rmi://localhost:1099/ClinicService");
MainFrame frame = new MainFrame(clinicService);
```

---

### Step 6: Update MainFrame

**File modified:** `src/main/java/MainFrame.java`

**What was changed:**
- Field type: `SocketClient socketClient` → `ClinicService clinicService`.
- Constructor parameter: `SocketClient socketClient` → `ClinicService clinicService`.
- All controller creation calls updated: `new LoginController(socketClient)` → `new LoginController(clinicService)`, same for `DoctorController` and `PatientController`.
- Import: `net.SocketClient` → `net.ClinicService`.

---

### Step 7: Update All Three Controllers

**Files modified:**
- `src/main/java/controller/LoginController.java`
- `src/main/java/controller/DoctorController.java`
- `src/main/java/controller/PatientController.java`

**What was changed in each controller:**
1. **Field type:** `SocketClient socketClient` → `ClinicService clinicService`.
2. **Constructor parameter:** Same change.
3. **Import:** `net.SocketClient` → `net.ClinicService` + `java.rmi.RemoteException`.
4. **Send calls:** `socketClient.send(request)` → `sendRequest(request)`.
5. **Added helper method** `sendRequest(Request)` that wraps `clinicService.processRequest(request)` in a try/catch for `RemoteException`, returning an error `Response` on failure (same pattern the old `SocketClient.send()` used for `IOException`).

**Code pattern (all controllers):**
```java
// BEFORE
private final SocketClient socketClient;
public XxxController(SocketClient socketClient) { this.socketClient = socketClient; }
...
Response response = socketClient.send(request);

// AFTER
private final ClinicService clinicService;
public XxxController(ClinicService clinicService) { this.clinicService = clinicService; }
...
Response response = sendRequest(request);

private Response sendRequest(Request request) {
    try {
        return clinicService.processRequest(request);
    } catch (RemoteException e) {
        Response errorResp = new Response();
        errorResp.setStatus("error");
        errorResp.setCode(500);
        errorResp.setMessage("Could not connect to server: " + e.getMessage());
        return errorResp;
    }
}
```

---

### Step 8: Delete Obsolete Files

**Files deleted:**
- `src/main/java/net/SocketClient.java` — Replaced by RMI lookup + `ClinicService` interface.
- `src/main/java/server/ClientHandler.java` — Routing logic moved to `ClinicServiceImpl`.
- `src/main/java/net/ProtocolUtils.java` — XML serialization/deserialization for transport no longer needed (RMI handles serialization automatically).

**Why deleted (not just deprecated):**
These files have zero references in the codebase after migration. Keeping them would cause confusion and suggest they're still part of the architecture.

---

## 3. File Change Summary

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/net/Request.java` | Modified | Added `Serializable`, `serialVersionUID` |
| `src/main/java/net/Response.java` | Modified | Added `Serializable`, `serialVersionUID` |
| `src/main/java/net/ClinicService.java` | **Created** | RMI remote interface |
| `src/main/java/server/ClinicServiceImpl.java` | **Created** | RMI remote implementation with routing |
| `src/main/java/server/ServerMain.java` | Modified | RMI registry + `rebind`, data dir → `src/main/resources` |
| `src/main/java/client/ClientMain.java` | Modified | RMI `Naming.lookup()` |
| `src/main/java/MainFrame.java` | Modified | `ClinicService` instead of `SocketClient` |
| `src/main/java/controller/LoginController.java` | Modified | `ClinicService` + `sendRequest()` |
| `src/main/java/controller/DoctorController.java` | Modified | `ClinicService` + `sendRequest()` |
| `src/main/java/controller/PatientController.java` | Modified | `ClinicService` + `sendRequest()` |
| `src/main/java/net/SocketClient.java` | **Deleted** | Replaced by RMI |
| `src/main/java/server/ClientHandler.java` | **Deleted** | Logic moved to `ClinicServiceImpl` |
| `src/main/java/net/ProtocolUtils.java` | **Deleted** | No longer needed (RMI handles serialization) |

---

## 4. Architecture Diagram — Before and After

### Before (Socket)
```
Client (Swing GUI)
    │
    ├── Controller (LoginController, DoctorController, PatientController)
    │       │
    │       ▼
    │   SocketClient.send(Request)
    │       │  ┌──────────────────────────────────┐
    │       │  │ 1. Open new TCP Socket            │
    │       │  │ 2. ProtocolUtils.toXml(Request)   │
    │       │  │ 3. Send XML string over socket    │
    │       │  │ 4. Read XML response string       │
    │       │  │ 5. ProtocolUtils.responseFromXml() │
    │       │  │ 6. Close socket                   │
    │       │  └──────────────────────────────────┘
    │       ▼
    │   Response
    │
    ▼
Server (Port 9090)
    │
    ├── ServerSocket.accept() → new Thread
    │       │
    │       ▼
    │   ClientHandler.run()
    │       │  ┌──────────────────────────────────┐
    │       │  │ 1. Read XML from socket           │
    │       │  │ 2. ProtocolUtils.fromXml()        │
    │       │  │ 3. Route via switch(action)       │
    │       │  │ 4. Call AuthService/ScheduleService│
    │       │  │ 5. ProtocolUtils.toXml(Response)  │
    │       │  │ 6. Write XML to socket            │
    │       │  └──────────────────────────────────┘
    │       ▼
    │   AuthService / ScheduleService → XMLDatabase
```

### After (RMI)
```
Client (Swing GUI)
    │
    ├── Controller (LoginController, DoctorController, PatientController)
    │       │
    │       ▼
    │   clinicService.processRequest(Request)   ← RMI remote call
    │       │                                      (automatic serialization)
    │       ▼
    │   Response                                ← returned automatically
    │
    ▼
Server (RMI Registry, Port 1099)
    │
    ├── ClinicServiceImpl (bound via rebind)
    │       │
    │       ▼
    │   processRequest(Request)
    │       │  ┌──────────────────────────────────┐
    │       │  │ Route via switch(action)          │
    │       │  │ Call AuthService/ScheduleService  │
    │       │  │ Return Response                   │
    │       │  └──────────────────────────────────┘
    │       ▼
    │   AuthService / ScheduleService → XMLDatabase
```

---

## 5. How to Run After Migration

### Start Server
1. Compile: `mvn clean compile`
2. Run `server.ServerMain` (has a `main` method)
3. Type `start` at the `>` prompt
4. Server prints: `[Server] RMI Registry started on port 1099` and `[Server] ClinicService bound via rebind.`

### Start Client
1. Run `client.ClientMain` (has a `main` method)
2. The login window appears — RMI automatically connects to `rmi://localhost:1099/ClinicService`
3. If the server is not running, an error dialog is shown

### Server Console Commands
| Command | Action |
|---------|--------|
| `start` | Start RMI registry and bind `ClinicService` |
| `stop` | Unbind service and mark as stopped |
| `log` | Print activity log entries |
| `status` | Show if server is running or stopped |
| `exit` | Stop server and exit |

---

## 6. Key Design Decisions

1. **Single `processRequest(Request)` method** — Chose one method instead of individual remote methods (e.g., `login()`, `register()`, `createSchedule()`) to minimize refactoring. The switch-based routing in `ClinicServiceImpl` is identical to the old `ClientHandler.process()`.

2. **`rebind` over `bind`** — `rebind` is used per project requirements. It overwrites any existing binding, so the server can restart without getting `AlreadyBoundException`.

3. **`sendRequest()` wrapper in controllers** — Each controller has a private `sendRequest(Request)` method that wraps `clinicService.processRequest()` in a try/catch for `RemoteException`. This mirrors the old error handling in `SocketClient.send()` (which caught `IOException`).

4. **Port 1099** — Standard RMI registry port, replacing the custom 9090 socket port.

5. **XML payload format preserved** — The internal XML payloads (`xmlPayload` in Request, `xmlData` in Response) are NOT changed in this phase. Only the transport layer is swapped. XML-to-JSON migration is a separate phase.

6. **Data directory moved** — Changed from `src/main/xmldata` to `src/main/resources` to use the standard Maven resources directory.

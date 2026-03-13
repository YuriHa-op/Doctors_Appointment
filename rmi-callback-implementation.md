# RMI Callback Implementation Documentation

## Overview
This document describes the **real-time callback notification system** added to the WellnessLink Clinic Appointment Scheduler. The callback mechanism allows the server to **push notifications to connected clients** whenever schedule-related events occur, eliminating the need for clients to poll for updates.

---

## Architecture

```
┌─────────────────────┐         RMI          ┌─────────────────────────┐
│   Client (Swing)    │ ◄──────────────────► │     Server (RMI)        │
│                     │                       │                         │
│  ClinicCallbackImpl │ ◄── onNotification ── │  CallbackManager        │
│    (Remote Object)  │                       │    (userId → callback)  │
│         │           │                       │         ▲               │
│         ▼           │                       │         │               │
│  DoctorView /       │                       │  ScheduleService        │
│  PatientView        │                       │  (fires notifications)  │
│  (auto-refresh)     │                       │                         │
└─────────────────────┘                       └─────────────────────────┘
```

### How It Works

1. **Client logs in** → `MainFrame` creates a `ClinicCallbackImpl` (exported as an RMI remote object) and registers it with the server via `clinicService.registerCallback(userId, callback)`.

2. **Server stores the callback** → `CallbackManager` maps the user ID to the callback stub in a thread-safe `ConcurrentHashMap`.

3. **An event occurs** (e.g., a patient books a slot) → `ScheduleService` looks up the affected user (e.g., the doctor who owns the slot) and calls `callbackManager.notifyUser(doctorId, eventType, message)`.

4. **Server invokes the callback** → `CallbackManager` calls `callback.onNotification(eventType, message)` on the client's remote object.

5. **Client receives the notification** → `ClinicCallbackImpl.onNotification()` dispatches to all registered `NotificationListener`s on the Swing EDT via `SwingUtilities.invokeLater()`.

6. **View auto-refreshes** → `DoctorView` / `PatientView` reload their data tables and display a notification dialog.

7. **Client logs out** → `MainFrame` calls `clinicService.unregisterCallback(userId)` to remove the callback.

---

## New Files

### `net/ClinicCallback.java` — Remote Callback Interface

```java
public interface ClinicCallback extends Remote {
    void onNotification(String eventType, String message) throws RemoteException;
}
```

- Extends `java.rmi.Remote` so the server can invoke it across JVMs.
- Single method: `onNotification(eventType, message)`.
- `eventType` identifies the kind of event; `message` is a human-readable description.

### `client/ClinicCallbackImpl.java` — Client-Side Implementation

```java
public class ClinicCallbackImpl extends UnicastRemoteObject implements ClinicCallback {
    void onNotification(String eventType, String message);  // dispatches to listeners
    void addListener(NotificationListener listener);
    void removeListener(NotificationListener listener);
}
```

- Extends `UnicastRemoteObject` to be automatically exported as an RMI stub.
- Uses `CopyOnWriteArrayList<NotificationListener>` for thread-safe listener management.
- Dispatches notifications on the **Swing EDT** via `SwingUtilities.invokeLater()`.
- Inner interface `NotificationListener` provides the contract for UI components.

### `server/CallbackManager.java` — Server-Side Registry

```java
public class CallbackManager {
    void register(String userId, ClinicCallback callback);
    void unregister(String userId);
    void notifyUser(String userId, String eventType, String message);
}
```

- Thread-safe `ConcurrentHashMap<String, ClinicCallback>` maps user IDs to callback stubs.
- `notifyUser()` catches `RemoteException` and **automatically removes stale callbacks** when a client is unreachable.

---

## Modified Files

### `net/ClinicService.java`

Added two new remote methods:

```java
void registerCallback(String userId, ClinicCallback callback) throws RemoteException;
void unregisterCallback(String userId) throws RemoteException;
```

### `server/ClinicServiceImpl.java`

- Now accepts `CallbackManager` in the constructor.
- Implements `registerCallback()` and `unregisterCallback()` by delegating to `CallbackManager`.

### `server/ServerMain.java`

- Creates a `CallbackManager` instance.
- Passes it to `ScheduleService` and `ClinicServiceImpl`.

### `server/ScheduleService.java`

- Accepts `CallbackManager` in the constructor.
- Fires callbacks after these operations:

| Method           | Event Type          | Notified User | Notification Message                                      |
|------------------|---------------------|---------------|-----------------------------------------------------------|
| `bookSlot()`     | `SLOT_BOOKED`       | Doctor        | "A patient has booked your slot on {date} ({time})."      |
| `cancelBooking()`| `BOOKING_CANCELLED` | Doctor        | "A patient has cancelled the appointment on {date}."      |
| `updateSlot()`   | `SLOT_UPDATED`      | Patient       | "Your appointment on {date} has been rescheduled to {time}." |
| `deleteSlot()`   | `SLOT_DELETED`      | Patient       | "Your appointment on {date} ({time}) has been cancelled." |

- Uses `db.getScheduleById()` to look up the affected user before each operation.
- Only sends notifications when the slot is in "Booked" status (i.e., a real user is affected).

### `database/JsonDatabase.java`

Added:

```java
public synchronized ScheduleModel getScheduleById(String scheduleId)
```

Searches all schedules for a matching ID — used by `ScheduleService` to find affected users before modifying slots.

### `MainFrame.java`

- Creates `ClinicCallbackImpl` on successful login.
- Calls `clinicService.registerCallback(userId, callback)` to register with the server.
- Passes `callbackImpl` to `DoctorView` / `PatientView` constructors.
- Calls `clinicService.unregisterCallback(userId)` on logout.

### `view/DoctorView.java`

- Constructor now accepts `ClinicCallbackImpl`.
- Registers a `NotificationListener` that **auto-refreshes** the appointment table and shows a notification dialog.

### `view/PatientView.java`

- Constructor now accepts `ClinicCallbackImpl`.
- Registers a `NotificationListener` that **auto-refreshes** the doctor list and shows a notification dialog.

---

## Event Types

| Event Type          | Triggered By           | Who Is Notified | When                                                  |
|---------------------|------------------------|-----------------|-------------------------------------------------------|
| `SLOT_BOOKED`       | Patient books a slot   | Doctor          | After successful booking                              |
| `BOOKING_CANCELLED` | Patient cancels        | Doctor          | After successful cancellation                         |
| `SLOT_UPDATED`      | Doctor updates time    | Patient         | After successful time update on a booked slot         |
| `SLOT_DELETED`      | Doctor deletes a slot  | Patient         | After successful deletion of a booked slot            |

---

## Notification Flow Examples

### Patient Books a Slot
```
Patient Client                    Server                      Doctor Client
     │                              │                              │
     │── bookSlot(patientId, ───►   │                              │
     │   scheduleId)                │                              │
     │                              │ getScheduleById(scheduleId)  │
     │                              │ bookSlot(scheduleId, patId)  │
     │                              │ callbackManager.notifyUser(  │
     │                              │   doctorId, "SLOT_BOOKED",   │
     │                              │   message)  ────────────────►│
     │                              │                              │ onNotification()
     │◄── "Booked successfully!" ── │                              │ loadAppointments()
     │                              │                              │ show dialog
```

### Doctor Deletes a Booked Slot
```
Doctor Client                     Server                     Patient Client
     │                              │                              │
     │── deleteSlot(scheduleId) ──► │                              │
     │                              │ getScheduleById(scheduleId)  │
     │                              │ → status="Booked",           │
     │                              │   patientId="PAT-xxx"        │
     │                              │ deleteSlotById(scheduleId)   │
     │                              │ callbackManager.notifyUser(  │
     │                              │   patientId, "SLOT_DELETED", │
     │                              │   message)  ────────────────►│
     │                              │                              │ onNotification()
     │◄── "Slot deleted." ──────── │                              │ loadDoctors()
     │                              │                              │ show dialog
```

---

## Error Handling

- **Client disconnect**: If a client crashes or disconnects without logging out, the `CallbackManager.notifyUser()` method catches the `RemoteException` and automatically removes the stale callback entry.
- **Registration failure**: If callback registration fails during login (e.g., network issue), the application still works normally — it just won't receive real-time push notifications.
- **Thread safety**: `CallbackManager` uses `ConcurrentHashMap`; `ClinicCallbackImpl` uses `CopyOnWriteArrayList`; all UI updates happen on the Swing EDT.

---

## Summary

| Component              | Role                                           |
|------------------------|-------------------------------------------------|
| `ClinicCallback`       | Remote interface for server → client calls     |
| `ClinicCallbackImpl`   | Client stub — dispatches to Swing listeners    |
| `CallbackManager`      | Server registry — maps userId to callback      |
| `ClinicService`        | Exposes register/unregister remote methods     |
| `ClinicServiceImpl`    | Delegates to CallbackManager                   |
| `ScheduleService`      | Fires notifications on schedule events         |
| `MainFrame`            | Manages callback lifecycle (login/logout)      |
| `DoctorView`           | Auto-refreshes on SLOT_BOOKED, BOOKING_CANCELLED |
| `PatientView`          | Auto-refreshes on SLOT_UPDATED, SLOT_DELETED   |

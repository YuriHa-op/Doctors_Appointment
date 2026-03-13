package net;

import java.rmi.Remote;
import java.rmi.RemoteException;


 //implemented by clients.
 //server invokes these methods to push real-time notifications
//when schedule-related events occur (booking, cancellation, updates)

public interface ClinicCallback extends Remote {


// Called by the server to notify the client of a schedule event
//
// the type of event (e.g. slot booked, booking cancelled, slot updated, slot deleted)
// message   a human-readable description of what happened
// RemoteException if a communication error occurs

    void onNotification(String eventType, String message) throws RemoteException;
}

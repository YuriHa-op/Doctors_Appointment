package net;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for the WellnessLink clinic service.
 * Exposes a single method to process all client requests via Java RMI.
 */
public interface ClinicService extends Remote {

    /**
     * Process a client request and return the server response.
     *
     * @param request the client request containing action, entity, and payload
     * @return the server response with status, code, message, and optional data
     * @throws RemoteException if a communication error occurs
     */
    Response processRequest(Request request) throws RemoteException;

    /**
     * Register a callback so the server can push real-time notifications to this client.
     *
     * @param userId   the logged-in user's ID
     * @param callback the client's callback stub
     * @throws RemoteException if a communication error occurs
     */
    void registerCallback(String userId, ClinicCallback callback) throws RemoteException;

    /**
     * Unregister a previously registered callback (e.g. on logout).
     *
     * @param userId the user ID whose callback should be removed
     * @throws RemoteException if a communication error occurs
     */
    void unregisterCallback(String userId) throws RemoteException;
}

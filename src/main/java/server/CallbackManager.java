package server;

import net.ClinicCallback;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side registry that maps logged-in user IDs to their RMI callback stubs.
 * Used by services to push real-time notifications to specific clients.
 */
public class CallbackManager {

    private final Map<String, ClinicCallback> callbacks = new ConcurrentHashMap<>();

    /** Register a client callback for a given userId. */
    public void register(String userId, ClinicCallback callback) {
        callbacks.put(userId, callback);
        System.out.println("[CallbackManager] Registered callback for " + userId);
    }

    /** Remove a client callback when the user logs out or disconnects. */
    public void unregister(String userId) {
        callbacks.remove(userId);
        System.out.println("[CallbackManager] Unregistered callback for " + userId);
    }

    /**
     * Send a notification to a specific user.
     * If the client is unreachable, the stale callback is automatically removed.
     */
    public void notifyUser(String userId, String eventType, String message) {
        ClinicCallback cb = callbacks.get(userId);
        if (cb != null) {
            try {
                cb.onNotification(eventType, message);
            } catch (RemoteException e) {
                System.out.println("[CallbackManager] Client unreachable for " + userId + ", removing.");
                callbacks.remove(userId);
            }
        }
    }
}

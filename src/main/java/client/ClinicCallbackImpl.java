package client;

import net.ClinicCallback;

import javax.swing.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


 //Client-side RMI callback implementation
// Receives server notifications and sends them to registered UI listeners
 //on the Swing Event Dispatch Thread for thread-safe GUI updates

public class ClinicCallbackImpl extends UnicastRemoteObject implements ClinicCallback {

      //Listener interface for UI components that want to react to server notifications.
    public interface NotificationListener {
        void onNotification(String eventType, String message);
    }

    private final List<NotificationListener> listeners = new CopyOnWriteArrayList<>();

    public ClinicCallbackImpl() throws RemoteException {
        super();
    }

    @Override
    public void onNotification(String eventType, String message) throws RemoteException {
        // Dispatch to all listeners on the EDT so Swing components can safely refresh
        SwingUtilities.invokeLater(() -> {
            for (NotificationListener listener : listeners) {
                listener.onNotification(eventType, message);
            }
        });
    }

    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }
}

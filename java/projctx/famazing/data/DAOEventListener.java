package projctx.famazing.data;

import android.support.annotation.Nullable;

import java.util.HashMap;

/**
 * Methods called by DAO object every time a connection, a disconnection or a response happens.
 */
public interface DAOEventListener {
    /**
     * Method called by DAO when connection with database is established.
     * @param e : the possible exception generated during connection.
     */
    void handleConnectionEvent(@Nullable SQLRuntimeException e);

    /**
     * Method called by DAO when diconnection with database is established.
     * @param e : the possible exception generated during diconnection.
     */
    void handleDisconnectionEvent(@Nullable SQLRuntimeException e);

    /**
     * Method called by DAO when a request by an object is satisfied.
     * @param result : a dictionary containing two keys: one to retrieve the code of the request
     *               served and the second to retrieve the object to serve the request or the exception generated.
     */
    void handleResult(HashMap<String, Object> result);
}

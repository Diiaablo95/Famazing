package projctx.famazing.data;

import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import projctx.famazing.data.Family.Membership;
import projctx.famazing.data.Task.Status;

import projctx.famazing.utility.StringPositionConverter;

/**
 * Class responsible for communication with the remote SQL database. It exposes the APIs through which application can interact with the remote db.
 * Every request is performed on a secondary thread and responses are passed back to the listener on the main thread through the methods <a href = "handleConnectionEvent">handleConnectionEvent(SQLRuntimeException e)</a>,
 * <a href = "handleDisconnectionEvent">handleDisconnectionEvent(SQLRuntimeException e)</a> and <a href = "handleResult">handleResult(Object result)</a> which respectively notify the listener about
 * connection with server established or the error generated, request response obtained and disconnection from server request served or the error generated.
 * Results to requests are handled in the Map received as parameter in the <a href = "handleResult">handleResult(Object result)</a> method or in the <a href = "handleConnectionEvent">handleConnectionEvent(SQLRuntimeException e)</a>/<a href = "handleDisconnectionEvent">handleDisconnectionEvent(SQLRuntimeException e)</a>
 */
public final class DAO {

    /**
     * Error generated when trying to pass a NULL value to a NOT NULL column.
     */
    public static final int ERROR_FIELD_MISSING = 1048;

    /**
     * Error generated when trying to pass a duplicate value to a UNIQUE column.
     */
    public static final int ERROR_DUPLICATE_FIELD = 1062;

    /**
     * Error generated when trying to pass a value not existing in the database to a column referencing another one.
     */
    public static final int ERROR_FOREIGN_KEY_INTEGRITY = 1452;

    //Operation code sent from the secondary thread to the requester (listener) to specify the particular operation just satisfied.
    public static final int DISCONNECTION = -1;

    public static final int CONNECTION = 0;

    public static final int CREATE_USER_CODE = 1;

    public static final int AUTHENTICATE_USER_CODE = 2;

    public static final int GET_TASK_CODE = 3;

    public static final int GET_MEMBERS_LOCATIONS_CODE = 4;

    public static final int UPDATE_USER_LOCATION_CODE = 5;

    public static final int GET_USER_CODE = 6;

    public static final int CREATE_ALERT_CODE = 7;

    public static final int GET_ALERT_CODE = 8;

    public static final int CREATE_PLACE_CODE = 9;

    public static final int GET_PLACE_CODE = 10;

    public static final int CREATE_TASK_CODE = 11;

    public static final int COMPLETE_TASK_CODE = 12;

    public static final int REFUSE_TASK_CODE = 13;

    public static final int GET_TASKS_COMPLETED_CODE = 14;

    public static final int GET_TASKS_PENDING_CODE = 15;

    public static final int GET_TASKS_REFUSED_CODE = 16;

    public static final int GET_TASKS_CODE = 17;

    public static final int GET_ALERTS_CODE = 18;

    public static final int GET_MEMBERS_CODE = 19;

    public static final int GET_PLACES_CODE = 20;

    public static final int CREATE_FAMILY_CODE = 21;

    public static final int GET_FAMILY_NAME_CODE = 22;

    public static final int GET_FAMILY_ID_CODE = 23;

    public static final int REMOVE_FAMILY_CODE = 24;

    /**
     * Key to access the object returned by the secondary thread after satisfying the request.
     */
    public static final String RESULT_OBJECT_KEY = "Object";

    /**
     * Key to access the id of the operation just satisfied by the secondary thread.
     */
    public static final String OPERATION_CODE_KEY = "Operation";

    //Name of driver responsible for communication
    private static String driverName = "jdbc:mysql";

    //TODO: Change with proper credentials!
    //Host where server is running
    private static String dbURL = "##########";

    //Name of db
    private static String dbName = "###########";

    //Username credential to connect to db
    private static String dbUser = "###############";

    //Password credential to connect to db
    private static String dbPassword = "##########";

    //Port on which server is listening for new connections
    private static int dbPort = 3306;

    //Complete URL used to connect to database
    private static String completeURL = String.format(Locale.ENGLISH, "%s://%s:%d/%s", driverName, dbURL, dbPort, dbName);

    //Object used to perform operations towards the database
    private Connection con;

    //Instance returned when requests. Follows the singleton pattern.
    private static DAO self;

    //Handler for the results returned by methods that run on a secondary Thread.
    private DAOEventListener listener;

    //Statement which encapsulate the actual request to send to database.
    private PreparedStatement statement;

    //Result given back by server once request has been elaborated.
    private ResultSet result;

    private boolean connected;

    public DAO(DAOEventListener listener) {
        this.listener = listener;
    }

    //Fills the response for the listener. The response contains the code of the operation executed as well as the result of the operation: either the error or the value.
    private void returnResponse(int operationCode, @Nullable SQLRuntimeException e, Object response) {
        if (operationCode == CONNECTION) {
            listener.handleConnectionEvent(response == null ? null : (SQLRuntimeException) response);
        } else if (operationCode == DISCONNECTION) {
            listener.handleDisconnectionEvent(response == null ? null : (SQLRuntimeException) response);
        } else {
            HashMap<String, Object> result = new HashMap<>();

            result.put(OPERATION_CODE_KEY, operationCode);                          //1 dictionary entry: operation code
            result.put(RESULT_OBJECT_KEY, e != null ? e : response);                //2 dictionary entry: result of operation (exception or response)

            listener.handleResult(result);
        }
    }

    /**
     * Open a new connection with the database. Opening a connection is mandatory before any request sending to the server.
     */
    public void connect() throws SQLRuntimeException {
        connected = true;
        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e = null;

            @Override
            protected Object doInBackground(Void... params) {
                new Callable<Void>() {

                    @Override
                    public Void call() {
                        try {
                            Class.forName("com.mysql.jdbc.Driver");
                            con = DriverManager.getConnection(completeURL, dbUser, dbPassword);
                            con.setAutoCommit(false);
                        } catch (Exception exp) {
                            connected = false;
                            e = new SQLRuntimeException(exp.getMessage());
                        }
                        return null;
                    }
                }.call();
                return e;
            }

            @Override
            protected void onPostExecute(Object o) {
                returnResponse(CONNECTION, null, o);
            }
        }.execute();
    }

    /**
     * Close the existing connection to the database. Connection should be closed every time there is no need to send further requests to the database.
     */
    public void disconnect() throws SQLRuntimeException {
        connected = false;
        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e = null;

            @Override
            protected Object doInBackground(Void... params) {
                new Callable<Void>() {
                    @Override
                    public Void call() throws SQLRuntimeException {
                        try {
                            if (statement != null) {
                                statement.close();
                                statement = null;
                            }
                            if (result != null) {
                                result.close();
                                result = null;
                            }
                        } catch (SQLException exp) {
                            connected = true;
                            e = new SQLRuntimeException(exp.getMessage());
                        }
                        return null;
                    }
                }.call();
                return e;
            }

            @Override
            protected void onPostExecute(Object o) {
                returnResponse(DISCONNECTION, null, o);
            }
        }.execute();
    }

    /**
     * Return whether the object is still keeping a connection with the database or not.
     * @return true if object is still connected, false otherwise.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Add a new user to the database.
     * @param _email : email of the new user. Must be unique in the database.
     * @param _password : password of the new user.
     * @param _user : the user object containing the data to store in the database.
     */
    public void createNewUser(String _email, String _password, User _user) {
        final String email = _email;
        final String password = _password;
        final User user = _user;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e = null;

            @Override
            protected Integer doInBackground(Void... params) {

                final Integer[] res = new Integer[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            int newUserId = -1;

                            try {
                                statement = con.prepareStatement("INSERT INTO user_credentials VALUES (NULL, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
                                statement.setObject(1, email);
                                statement.setObject(2, password);
                                statement.executeUpdate();
                                result = statement.getGeneratedKeys();
                                if (result.next()) {
                                    newUserId = result.getInt(1);
                                }
                            } catch (SQLException e) {
                                if (e.getErrorCode() == ERROR_DUPLICATE_FIELD) {
                                    throw new DuplicateFieldException();
                                } else if (e.getErrorCode() == ERROR_FIELD_MISSING) {
                                    throw new NullFieldException();
                                } else {
                                    throw new SQLRuntimeException(e.getMessage());
                                }
                            }

                            String name = user.getName();
                            Date birthday = user.getBirthday();
                            Membership membership = user.getMembership();
                            int familyId = user.getFamilyId();

                            try {
                                statement = con.prepareStatement("INSERT INTO user VALUES (?, ?, ?, ?, ?, NULL)");
                                statement.setObject(1, newUserId);
                                statement.setObject(2, name);
                                statement.setObject(3, birthday);
                                statement.setObject(4, membership == null ? null : membership.toString());
                                statement.setObject(5, familyId);
                                statement.executeUpdate();          //We don't need return value here, since we already have user id. What we need
                                con.commit();
                            } catch (SQLException e) {              //is to see if an exception is generated or not.
                                try {
                                    con.rollback();
                                    throw new SQLRuntimeException(e.getMessage());
                                } catch (SQLException e2) {
                                    throw new SQLRuntimeException(e.getMessage());
                                }
                            }
                            res[0] = newUserId;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object newUserId) {
                returnResponse(CREATE_USER_CODE, e, newUserId);
            }
        }.execute();
    }

    /**
     * Verify the credentials passed belong to an existing user in the database or not.
     * @param _email : email of the user to authenticate.
     * @param _password : password of the user to authenticate.
     */
    public void authenticateUser(String _email, String _password) {
        final String email = _email;
        final String password = _password;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Integer doInBackground(Void... params) {

                final Integer[] res = new Integer[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            int userId = -1;
                            try {
                                statement = con.prepareStatement("SELECT id FROM user_credentials WHERE email = ? AND password = ?");
                                statement.setObject(1, email);
                                statement.setObject(2, password);

                                result = statement.executeQuery();
                                if (result.next()) {
                                    userId = result.getInt(1);
                                }
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = userId;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object userId) {
                returnResponse(AUTHENTICATE_USER_CODE, e, userId);
            }
        }.execute();
    }

    /**
     * Return all the tasks in a certain status for a specified user.
     * @param _userId : the user id for who the tasks need to be retrieved.
     * @param _taskStatus : the tasks of the status to retrieve. If null is passed, all the tasks in which the user is involved (for its completion) are returned.
     * @param _from : starting date for which the tasks need to be fetched.
     * @param _to : ending date for which the tasks need to be fetched.
     */
    public void getTasks(int _userId, @Nullable Task.Status _taskStatus, @Nullable Date _from, @Nullable Date _to) {
        final int userId = _userId;
        final Status taskStatus = _taskStatus;
        final Date from = _from;
        final Date to = _to;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected List<Task> doInBackground(Void... params) {

                final List<Task>[] res = (List<Task>[]) new List[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            ArrayList<Task> tasks = new ArrayList<>();
                            StringBuilder queryString = new StringBuilder();
                            ArrayList<Object> parameters = new ArrayList<Object>();
                            try {
                                if (taskStatus != null) {
                                    switch (taskStatus) {
                                        case REFUSED: {
                                            queryString.append("SELECT * FROM task WHERE doer = ? AND status = ? ");
                                            parameters.add(userId);
                                            parameters.add(taskStatus.toString());
                                            break;
                                        }
                                        case PENDING: {
                                            queryString.append("SELECT * FROM task WHERE doer = ? AND status = ? ");
                                            parameters.add(userId);
                                            parameters.add(taskStatus.toString());
                                            break;
                                        }
                                        case COMPLETED: {
                                            queryString.append("SELECT * FROM task WHERE completed_by = ? ");
                                            parameters.add(userId);
                                            break;
                                        }
                                    }
                                } else {        //If status is not passed, then selects all the
                                    queryString.append("SELECT * FROM task WHERE (doer = ? AND status = ?) OR (doer = ? AND status = ?) OR completed_by = ? ");
                                    parameters.add(userId);
                                    parameters.add(Task.Status.REFUSED.toString());
                                    parameters.add(userId);
                                    parameters.add(Task.Status.PENDING.toString());
                                    parameters.add(userId);
                                }
                                if (from != null) {
                                    queryString.append("AND deadline >= ? ");
                                    parameters.add(from);
                                }
                                if (to != null) {
                                    queryString.append("AND deadline <= ? ");
                                    parameters.add(to);
                                }
                                queryString.append("ORDER BY deadline ASC, id ASC");
                                statement = con.prepareStatement(queryString.toString());

                                for (int i = 0; i < parameters.size(); i++) {
                                    statement.setObject(i + 1, parameters.get(i));
                                }

                                result = statement.executeQuery();

                                //Take all the results and populate the list to return.
                                while (result.next()) {
                                    int id = result.getInt(1);
                                    String name = result.getString(2);
                                    int giver = result.getInt(3);
                                    Integer doer = (Integer) result.getObject(4);
                                    Integer completer = (Integer) result.getObject(5);
                                    Date deadline = result.getDate(6);
                                    Integer locationId = (Integer) result.getObject(7);
                                    String description = result.getString(8);
                                    Task.Status status = Task.Status.valueOf(result.getString(9));

                                    Task task = new Task(id, name, giver, doer, completer, deadline, locationId, description, status);
                                    tasks.add(task);
                                }
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = tasks;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object tasks) {
                int opCode;

                if (taskStatus == null) {
                    opCode = GET_TASKS_CODE;
                } else if (taskStatus == Task.Status.COMPLETED) {
                    opCode = GET_TASKS_COMPLETED_CODE;
                } else if (taskStatus == Task.Status.PENDING) {
                    opCode = GET_TASKS_PENDING_CODE;
                } else {
                    opCode = GET_TASKS_REFUSED_CODE;
                }
                returnResponse(opCode, e, tasks);
            }
        }.execute();
    }

    /**
     * Return all the members location.
     * @param _userId : the id for whose family members the location needs to be retrieved.
     */
    public void getMembersLastLocations(int _userId) {
        final int userId = _userId;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Map<User, LatLng> doInBackground(Void... params) {

                final Map<User, LatLng>[] res = (Map<User, LatLng>[]) new Map[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            int familyId = -1;
                            Map<User, LatLng> membersLocations = new HashMap<>();

                            try {
                                statement = con.prepareStatement("SELECT family FROM user WHERE id = ?");
                                statement.setObject(1, userId);

                                result = statement.executeQuery();
                                if (result.next()) {
                                    familyId = result.getInt(1);
                                }

                                if (familyId != -1) {
                                    statement = con.prepareStatement("SELECT * from user WHERE id != ? AND family = ?");
                                    statement.setObject(1, userId);
                                    statement.setObject(2, familyId);

                                    result = statement.executeQuery();

                                    while (result.next()) {
                                        int id = result.getInt(1);
                                        String name = result.getString(2);
                                        Date birthday = result.getDate(3);
                                        Membership membership = Membership.valueOf(result.getString(4).toUpperCase());
                                        LatLng lastLocation = StringPositionConverter.convert(result.getString(6));

                                        User user = new User(id, name, birthday, membership, familyId);

                                        membersLocations.put(user, lastLocation);
                                    }
                                }
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = membersLocations;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object locations) {
                returnResponse(GET_MEMBERS_LOCATIONS_CODE, e, locations);
            }
        }.execute();
    }

    /**
     * Update the actual location of a user.
     * @param _userId : the id for who the location needs to be updated.
     * @param _location : the new coordinates for the user.
     */
    public void updateUserLocation(int _userId, LatLng _location) {
        final int userId = _userId;
        final LatLng location = _location;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Boolean doInBackground(Void... params) {

                final Boolean[] res = new Boolean[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            int rowsAffected;

                            try {
                                statement = con.prepareStatement("UPDATE user SET last_location = ? WHERE id = ?");
                                statement.setObject(1, StringPositionConverter.convert(location));
                                statement.setObject(2, userId);

                                rowsAffected = statement.executeUpdate();
                                con.commit();
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = rowsAffected > 0;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object result) {
                returnResponse(UPDATE_USER_LOCATION_CODE, e, result);
            }
        }.execute();
    }

    /**
     * Get all the details for a user.
     * @param _userId : user whose details need to be retrieved.
     */
    public void getUser(int _userId) {
        final int userId = _userId;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected User doInBackground(Void... params) {

                final User[] res = new User[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            User user = null;

                            try {
                                statement = con.prepareStatement("SELECT * FROM user WHERE id = ?");
                                statement.setObject(1, userId);

                                result = statement.executeQuery();
                                if (result.next()) {
                                    String name = result.getString(2);
                                    Date birthday = result.getDate(3);
                                    Membership membership = Membership.valueOf(result.getString(4).toUpperCase());
                                    int familyId = result.getInt(5);

                                    user = new User(userId, name, birthday, membership, familyId);
                                }
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = user;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object user) {
                returnResponse(GET_USER_CODE, e, user);
            }
        }.execute();
    }

    /**
     * Add a new alert to the alerts history.
     * @param _alert : the alert to add to the history.
     */
    public void createNewAlert(Alert _alert) {
        final Alert alert = _alert;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e = null;

            @Override
            protected Integer doInBackground(Void... params) {

                final Integer[] res = new Integer[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            int newAlertId = -1;

                            try {
                                int userId = alert.getUserId();
                                Date date = alert.getAlertDate();
                                String location = StringPositionConverter.convert(alert.getUserLocation());

                                statement = con.prepareStatement("INSERT INTO alert VALUES (NULL, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
                                statement.setObject(1, userId);
                                statement.setObject(2, date);
                                statement.setObject(3, location);
                                statement.executeUpdate();
                                result = statement.getGeneratedKeys();
                                if (result.next()) {
                                    newAlertId = result.getInt(1);
                                }
                                con.commit();
                            } catch (SQLException e) {
                                if (e.getErrorCode() == ERROR_DUPLICATE_FIELD) {
                                    throw new DuplicateFieldException();
                                } else if (e.getErrorCode() == ERROR_FIELD_MISSING) {
                                    throw new NullFieldException();
                                } else if (e.getErrorCode() == ERROR_FOREIGN_KEY_INTEGRITY) {
                                    throw new ForeignKeyException();
                                } else {
                                    throw new SQLRuntimeException(e.getMessage());
                                }
                            }
                            res[0] = newAlertId;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object newAlertId) {
                returnResponse(CREATE_ALERT_CODE, e, newAlertId);
            }
        }.execute();
    }

    /**
     * Get all the details for an alert.
     * @param _alertId : alert which details need to be retrieved.
     */
    public void getAlert(int _alertId) {
        final int alertId = _alertId;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Alert doInBackground(Void... params) {

                final Alert[] res = new Alert[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            Alert alert = null;

                            try {
                                statement = con.prepareStatement("SELECT * FROM alert WHERE id = ?");
                                statement.setObject(1, alertId);

                                result = statement.executeQuery();
                                if (result.next()) {
                                    int user_id = result.getInt(2);
                                    Date date = result.getDate(3);
                                    LatLng position = StringPositionConverter.convert(result.getString(4));

                                    alert = new Alert(alertId, user_id, date, position);
                                }
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = alert;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object alert) {
                returnResponse(GET_ALERT_CODE, e, alert);
            }
        }.execute();
    }

    /**
     * Add a new preferred place to the database. The place is linked to a specific family.
     * @param _place : the place to add to the database.
     */
    public void createNewPlace(Place _place) {
        final Place place = _place;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e = null;

            @Override
            protected Integer doInBackground(Void... params) {

                final Integer[] res = new Integer[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            int newPlaceId = -1;

                            try {
                                String name = place.getName();
                                int familyId = place.getFamilyId();
                                LatLng location = place.getCoordinates();

                                statement = con.prepareStatement("INSERT INTO place VALUES (NULL, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
                                statement.setObject(1, name);
                                statement.setObject(2, StringPositionConverter.convert(location));
                                statement.setObject(3, familyId);
                                statement.executeUpdate();
                                result = statement.getGeneratedKeys();
                                if (result.next()) {
                                    newPlaceId = result.getInt(1);
                                }
                                con.commit();
                            } catch (SQLException e) {
                                if (e.getErrorCode() == ERROR_DUPLICATE_FIELD) {
                                    throw new DuplicateFieldException();
                                } else if (e.getErrorCode() == ERROR_FIELD_MISSING) {
                                    throw new NullFieldException();
                                } else if (e.getErrorCode() == ERROR_FOREIGN_KEY_INTEGRITY) {
                                    throw new ForeignKeyException();
                                } else {
                                    throw new SQLRuntimeException(e.getMessage());
                                }
                            }
                            res[0] = newPlaceId;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object newPlaceId) {
                returnResponse(CREATE_PLACE_CODE, e, newPlaceId);
            }
        }.execute();
    }

    /**
     * Get all the details for a place.
     * @param _placeId : place which details need to be retrieved.
     */
    public void getPlace(int _placeId) {
        final int placeId = _placeId;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Place doInBackground(Void... params) {

                final Place[] res = new Place[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            Place place = null;

                            try {
                                statement = con.prepareStatement("SELECT * FROM place WHERE id = ?");
                                statement.setObject(1, placeId);

                                result = statement.executeQuery();

                                if (result.next()) {
                                    String name = result.getString(2);
                                    LatLng position = StringPositionConverter.convert(result.getString(3));
                                    int familyId = result.getInt(4);

                                    place = new Place(placeId, name, position, familyId);
                                }
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = place;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object place) {
                returnResponse(GET_PLACE_CODE, e, place);
            }
        }.execute();
    }

    /**
     * Create a new task for the family.
     * @param _task : the task to create.
     */
    public void createTask(Task _task) {
        final Task task = _task;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e = null;

            @Override
            protected Integer doInBackground(Void... params) {

                final Integer[] res = new Integer[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            int newTaskId = -1;

                            try {
                                String name = task.getName();
                                int giver = task.getGiver();
                                Integer doer = task.getDoer();
                                Date deadline = task.getDeadline();
                                Integer locationId = task.getLocationId();
                                String description = task.getDescription();

                                statement = con.prepareStatement("INSERT INTO task VALUES (NULL, ?, ?, ?, NULL, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
                                statement.setObject(1, name);
                                statement.setObject(2, giver);
                                statement.setObject(3, doer);
                                statement.setObject(4, deadline);
                                statement.setObject(5, locationId);
                                statement.setObject(6, description);
                                statement.setObject(7, Task.Status.PENDING.toString());

                                statement.executeUpdate();
                                result = statement.getGeneratedKeys();
                                if (result.next()) {
                                    newTaskId = result.getInt(1);
                                }
                                con.commit();
                            } catch (SQLException e) {
                                if (e.getErrorCode() == ERROR_FIELD_MISSING) {
                                    throw new NullFieldException();
                                } else if (e.getErrorCode() == ERROR_FOREIGN_KEY_INTEGRITY) {
                                    throw new ForeignKeyException();
                                } else {
                                    throw new SQLRuntimeException(e.getMessage());
                                }
                            }
                            res[0] = newTaskId;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object newTaskId) {
                returnResponse(CREATE_TASK_CODE, e, newTaskId);
            }
        }.execute();
    }

    /**
     * Change the status of a task to <a href = "Status">completed</a> and update its completer (who can be anyone if task didn't have a predefined doer.
     * @param _taskId : the task for which the status needs to be updated.
     * @param _completerId : the task completer id.
     */
    public void completeTask(int _taskId, int _completerId) {
        final int taskId = _taskId;
        final int completerId = _completerId;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Boolean doInBackground(Void... params) {

                final Boolean[] res = new Boolean[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            int rowsAffected;

                            try {
                                statement = con.prepareStatement("UPDATE task SET completed_by = ?, status = ? WHERE id = ?");
                                statement.setObject(1, completerId);
                                statement.setObject(2, Task.Status.COMPLETED.toString());
                                statement.setObject(3, taskId);

                                rowsAffected = statement.executeUpdate();
                                con.commit();
                            } catch (SQLException e) {
                                if (e.getErrorCode() == ERROR_FOREIGN_KEY_INTEGRITY) {
                                    throw new ForeignKeyException();
                                } else {
                                    throw new SQLRuntimeException(e.getMessage());
                                }
                            }
                            res[0] = rowsAffected > 0;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object result) {
                returnResponse(COMPLETE_TASK_CODE, e, result);
            }
        }.execute();
    }

    /**
     * Change the status of a task to <a href = "Status">refused</a>.
     * @param _taskId : the task for which the status needs to be updated.
     */
    public void refuseTask(int _taskId) {
        final int taskId = _taskId;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Boolean doInBackground(Void... params) {

                final Boolean[] res = new Boolean[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            int rowsAffected;

                            try {
                                statement = con.prepareStatement("UPDATE task SET status = ? WHERE id = ?");
                                statement.setObject(1, Task.Status.REFUSED.toString());
                                statement.setObject(2, taskId);

                                rowsAffected = statement.executeUpdate();
                                con.commit();
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = rowsAffected > 0;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object result) {
                returnResponse(REFUSE_TASK_CODE, e, result);
            }
        }.execute();
    }

    /**
     * Get all the details for a task.
     * @param _taskId : task which details need to be retrieved.
     */
    public void getTask(int _taskId) {
        final int taskId = _taskId;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Task doInBackground(Void... params) {

                final Task[] res = new Task[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            Task task = null;

                            try {
                                statement = con.prepareStatement("SELECT * FROM task WHERE id = ?");
                                statement.setObject(1, taskId);

                                result = statement.executeQuery();

                                if (result.next()) {
                                    String name = result.getString(2);
                                    int giver = result.getInt(3);
                                    Integer doer = (Integer) result.getObject(4);
                                    Integer completer = (Integer) result.getObject(5);
                                    Date deadline = result.getDate(6);
                                    Integer locationId = (Integer) result.getObject(7);
                                    String description = result.getString(8);
                                    Task.Status status = Task.Status.valueOf(result.getString(9));

                                    task = new Task(taskId, name, giver, doer, completer, deadline, locationId, description, status);
                                }
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = task;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object task) {
                returnResponse(GET_TASK_CODE, e, task);
            }
        }.execute();
    }

    /**
     * Get all the alerts history for the specified family.
     * @param _familyId : the family id for which the tasks need to be retrieved.
     */
    public void getFamilyAlerts(int _familyId) {
        final int familyId = _familyId;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected List<Alert> doInBackground(Void... params) {

                final List<Alert>[] res = (List<Alert>[]) new List[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            List<Alert> familyAlerts = new ArrayList<>();

                            try {
                                statement = con.prepareStatement("SELECT id from user WHERE family = ?");       //Take members' ids.
                                statement.setObject(1, familyId);

                                result = statement.executeQuery();
                                List<Integer> parameters = new ArrayList<>();
                                StringBuilder query = null;

                                while (result.next()) {
                                    if (query == null) {
                                        query = new StringBuilder("SELECT * FROM alert WHERE user_id = ? ");    //Take alerts generated by those ids.
                                    } else {
                                        query.append("OR user_id = ? ");
                                    }
                                    parameters.add(result.getInt(1));
                                }
                                if (query != null) {
                                    query.append("ORDER BY date DESC");
                                    statement = con.prepareStatement(query.toString());
                                    for (int i = 1; i <= parameters.size(); i++) {
                                        statement.setObject(i, parameters.get(i - 1));     //Since setInt counts as first index 1 and not 0
                                    }
                                    result = statement.executeQuery();

                                    while (result.next()) {
                                        int id = result.getInt(1);
                                        int userId = result.getInt(2);
                                        Date date = result.getDate(3);
                                        LatLng position = StringPositionConverter.convert(result.getString(4));

                                        Alert alert = new Alert(id, userId, date, position);
                                        familyAlerts.add(alert);
                                    }
                                }
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = familyAlerts;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object alerts) {
                returnResponse(GET_ALERTS_CODE, e, alerts);
            }
        }.execute();
    }

    /**
     * Get all the members of the specified family.
     * @param _familyId : if of family for which members need to be retrieved.
     */
    public void getFamilyMembers(int _familyId) {
        final int familyId = _familyId;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Set<User> doInBackground(Void... params) {

                final Set<User>[] res = (Set<User>[]) new Set[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            Set<User> members = new HashSet<>();

                            try {
                                statement = con.prepareStatement("SELECT id, name, birthday, membership FROM user WHERE family = ?");
                                statement.setObject(1, familyId);

                                result = statement.executeQuery();

                                while (result.next()) {
                                    int id = result.getInt(1);
                                    String name = result.getString(2);
                                    Date birthday = result.getDate(3);
                                    Family.Membership membership = Family.Membership.valueOf(result.getString(4).toUpperCase());

                                    User user = new User(id, name, birthday, membership, familyId);

                                    members.add(user);
                                }
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = members;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object members) {
                returnResponse(GET_MEMBERS_CODE, e, members);
            }
        }.execute();
    }

    /**
     * Get all the preferred places created by the family.
     * @param _familyId : the id of the family for which the preferred places need to be retrieved.
     */
    public void getFamilyPlaces(int _familyId) {
        final int familyId = _familyId;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Set<Place> doInBackground(Void... params) {

                final Set<Place>[] res = (Set<Place>[]) new Set[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            Set<Place> places = new HashSet<>();

                            try {
                                statement = con.prepareStatement("SELECT id, name, position FROM place WHERE family = ?");
                                statement.setObject(1, familyId);

                                result = statement.executeQuery();

                                while (result.next()) {
                                    int id = result.getInt(1);
                                    String name = result.getString(2);
                                    LatLng position = StringPositionConverter.convert(result.getString(3));

                                    Place place = new Place(id, name, position, familyId);

                                    places.add(place);
                                }
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = places;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object places) {
                returnResponse(GET_PLACES_CODE, e, places);
            }
        }.execute();
    }

    /**
     * Add a new family into the system.
     * @param _name : the name of the new family. It must be unique in the database.
     */
    public void createNewFamily(String _name) {
        final String name = _name;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e = null;

            @Override
            protected Integer doInBackground(Void... params) {

                final Integer[] res = new Integer[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            int newFamilyId = -1;

                            try {
                                statement = con.prepareStatement("INSERT INTO family VALUES (NULL, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
                                statement.setObject(1, name);

                                try {
                                    statement.executeUpdate();
                                    result = statement.getGeneratedKeys();
                                    if (result.next()) {
                                        newFamilyId = result.getInt(1);
                                    }
                                } catch (SQLException e) {
                                    if (e.getErrorCode() == ERROR_DUPLICATE_FIELD) {
                                        throw new DuplicateFieldException();
                                    } else if (e.getErrorCode() == ERROR_FIELD_MISSING) {
                                        throw new NullFieldException();
                                    } else {
                                        throw e;
                                    }
                                }
                                con.commit();
                            } catch (SQLException e) {
                                if (e.getErrorCode() == ERROR_FIELD_MISSING) {
                                    throw new NullFieldException();
                                } else if (e.getErrorCode() == ERROR_DUPLICATE_FIELD) {
                                    throw new DuplicateFieldException();
                                } else {
                                    throw new SQLRuntimeException(e.getMessage());
                                }
                            }
                            res[0] = newFamilyId;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object newFamilyId) {
                returnResponse(CREATE_FAMILY_CODE, e, newFamilyId);
            }
        }.execute();
    }

    /**
     * Get all the details for a family.
     * @param _familyId : family which details need to be retrieved.
     */
    public void getFamily(int _familyId) {
        final int familyId = _familyId;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Family doInBackground(Void... params) {

                final Family[] res = new Family[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            Family family = null;

                            try {
                                statement = con.prepareStatement("SELECT * FROM family WHERE id = ?");
                                statement.setObject(1, familyId);

                                result  = statement.executeQuery();

                                if (result.next()) {
                                    String name = result.getString(2);

                                    family = new Family(familyId, name);
                                }
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = family;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object family) {
                returnResponse(GET_FAMILY_NAME_CODE, e, family);
            }
        }.execute();
    }

    /**
     * Get family id given its name.
     * @param _familyName : name of family for which the id needs to be retrieved.
     */
    public void getFamilyId(String _familyName) {
        final String familyName = _familyName;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Integer doInBackground(Void... params) {

                final Integer[] res = new Integer[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            int familyId = -1;

                            try {
                                statement = con.prepareStatement("SELECT id FROM family WHERE name = ?");
                                statement.setObject(1, familyName);

                                result = statement.executeQuery();

                                if (result.next()) {
                                    familyId = result.getInt(1);
                                }
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = familyId;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object familyId) {
                returnResponse(GET_FAMILY_ID_CODE, e, familyId);
            }
        }.execute();
    }

    /**
     * Remove a family.
     * @param _familyId : id of family to remove.
     */
    public void removeFamily(int _familyId) {
        final int familyId = _familyId;

        new AsyncTask<Void, Void, Object>() {

            SQLRuntimeException e;

            @Override
            protected Boolean doInBackground(Void... params) {

                final Boolean[] res = new Boolean[1];

                try {
                    new Callable<Void>() {
                        @Override
                        public Void call() throws SQLRuntimeException {
                            int rowsAffected;
                            try {
                                statement = con.prepareStatement("DELETE FROM family WHERE id = ?");
                                statement.setObject(1, familyId);

                                rowsAffected = statement.executeUpdate();
                                con.commit();
                            } catch (SQLException e) {
                                throw new SQLRuntimeException(e.getMessage());
                            }
                            res[0] = rowsAffected > 0;
                            return null;
                        }
                    }.call();
                } catch (SQLRuntimeException e) {
                    this.e = e;
                }
                return res[0];
            }

            @Override
            protected void onPostExecute(Object familyId) {
                returnResponse(REMOVE_FAMILY_CODE, e, familyId);
            }
        }.execute();
    }
}
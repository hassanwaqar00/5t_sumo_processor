package it.polito.5t.sumo.processor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import static it.polito.5t.sumo.processor.Main.ERROR;
import static it.polito.5t.sumo.processor.Main.H1_LOWER;
import static it.polito.5t.sumo.processor.Main.H1_UPPER;
import static it.polito.5t.sumo.processor.Main.H2_LOWER;
import static it.polito.5t.sumo.processor.Main.H2_UPPER;
import static it.polito.5t.sumo.processor.Main.H3_LOWER;
import static it.polito.5t.sumo.processor.Main.H3_UPPER;
import static it.polito.5t.sumo.processor.Main.H4_LOWER;
import static it.polito.5t.sumo.processor.Main.H4_UPPER;
import static it.polito.5t.sumo.processor.Main.INPUT_TABLE_NAME;
import static it.polito.5t.sumo.processor.Main.OUTPUT_TABLE_NAME;
import static it.polito.5t.sumo.processor.Main.SEED;

public class DBHelper {

    // JDBC driver name and database URL
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://DB_IP_HERE:DB_PORT_HERE/DB_NAME_HERE?useSSL=false";
    static final String DB_USERNAME = "DB_USER_NAME_HERE";
    static final String DB_PASSWORD = "DB_PASSWORD_HERE";
    private Connection connection = null;

    public DBHelper() {
        jdbcConnect();
    }

    public Connection jdbcConnect() {
        try {
            Class.forName(JDBC_DRIVER).newInstance();
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            Logger.getLogger(DBHelper.class.getName() + ".jdbcConnect").log(Level.WARNING, ex.toString());
            connection = null;
        }
        return connection;
    }

    public void jdbcClose() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ex) {
                Logger.getLogger(DBHelper.class.getName() + ".jdbcClose").log(Level.WARNING, ex.toString());
            }
            connection = null;
        }
    }

    public int queryDistinctIds() throws SQLException, JSONException {
        String query = "SELECT COUNT(DISTINCT(id)) FROM `sumo`.`" + INPUT_TABLE_NAME + "`";
        connection.setCatalog("sumo");
        Statement statement;
        statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);
        int ids = -1;
        while (rs.next()) {
            ids = rs.getInt("COUNT(DISTINCT(id))");
        }
        statement.close();
        statement = null;
        return ids;
    }

    public int queryMaxStep() throws SQLException, JSONException {
        String query = "SELECT max(step) FROM `sumo`.`" + INPUT_TABLE_NAME + "`";
        connection.setCatalog("sumo");
        Statement statement;
        statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);
        int step = -1;
        while (rs.next()) {
            step = rs.getInt("max(step)");
        }
        statement.close();
        statement = null;
        return step;
    }

    public float queryAverageSpeedAtVil(int stepFrom, int stepTo, String vilTableName, String idRegExp) throws SQLException, JSONException {
        String query = "SELECT AVG(transitSpeed) AS averageSpeed FROM `" + vilTableName + "` WHERE transitTime>" + stepFrom + " AND transitTime<=" + stepTo + " AND id REGEXP '" + idRegExp + "'";
        connection.setCatalog("sumo");
        Statement statement;
        statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);
        float averageSpeed = -1;
        while (rs.next()) {
            averageSpeed = rs.getFloat("averageSpeed");
        }
        statement.close();
        statement = null;
        return averageSpeed;
    }

    public void createVilTempTable(String tableName, float vilX, float vilY, int vilRadius, String vilHeading, int newIDs) throws SQLException, JSONException {
        String query = "DROP TEMPORARY TABLE IF EXISTS " + tableName;
        Statement statement;
        connection.setCatalog("sumo");
        statement = connection.createStatement();
        statement.executeUpdate(query);
        if (ERROR == -1) {
            query = "CREATE TEMPORARY TABLE " + tableName
                    + " ENGINE=MEMORY SELECT a.id, AVG(step) AS transitTime, AVG(speed) AS transitSpeed FROM `sumo`.`"
                    + INPUT_TABLE_NAME + "` a RIGHT JOIN (SELECT id FROM `sumo`.`" + INPUT_TABLE_NAME
                    + "` GROUP BY id ORDER BY RAND(" + SEED + ") LIMIT " + newIDs + ") b ON a.id=b.id "
                    + "WHERE ST_DISTANCE(POINT(positionX,positionY), POINT(" + vilX + "," + vilY + "))<" + vilRadius
                    + " AND (IF((angle)>=" + H1_LOWER + " OR (angle)<" + H1_UPPER + ",'H1',IF((angle)>=" + H2_LOWER
                    + " AND (angle)<" + H2_UPPER + ",'H2',IF((angle)>=" + H3_LOWER + " AND (angle)<" + H3_UPPER
                    + ",'H3',IF((angle)>=" + H4_LOWER + " AND (angle)<" + H4_UPPER + ",'H4','HX'))))) LIKE '"
                    + vilHeading + "' GROUP BY a.id";
        } else {
            query = "CREATE TEMPORARY TABLE " + tableName
                    + " ENGINE=MEMORY SELECT a.id, AVG(step) AS transitTime, AVG(speed) AS transitSpeed FROM `sumo`.`"
                    + INPUT_TABLE_NAME + "` a RIGHT JOIN (SELECT * FROM (SELECT id FROM `sumo`.`" + INPUT_TABLE_NAME
                    + "` GROUP BY id ORDER BY RAND(" + SEED + ") LIMIT " + newIDs + ") as temp_tab ORDER BY RAND("
                    + (new Random().nextInt()) + ") LIMIT " + (newIDs - (int) (newIDs * ERROR / 100)) + ") b ON a.id=b.id "
                    + "WHERE ST_DISTANCE(POINT(positionX,positionY), POINT(" + vilX + "," + vilY + "))<" + vilRadius
                    + " AND (IF((angle)>=" + H1_LOWER + " OR (angle)<" + H1_UPPER + ",'H1',IF((angle)>=" + H2_LOWER
                    + " AND (angle)<" + H2_UPPER + ",'H2',IF((angle)>=" + H3_LOWER + " AND (angle)<" + H3_UPPER
                    + ",'H3',IF((angle)>=" + H4_LOWER + " AND (angle)<" + H4_UPPER + ",'H4','HX'))))) LIKE '"
                    + vilHeading + "' GROUP BY a.id";
        }
        statement = connection.createStatement();
        statement.executeUpdate(query);

        int rowCount = -1;
        statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + tableName);
        while (rs.next()) {
            rowCount = rs.getInt("COUNT(*)");
        }

        statement.close();
        statement = null;
        System.out.println(Thread.currentThread().getName() + ":\tCreated temp table: " + tableName + " " + rowCount + " rows");
    }

    public void createVilTempTable(String tableName, float vilX, float vilY, int vilRadius, String vilHeading) throws SQLException, JSONException {
        String query = "DROP TEMPORARY TABLE IF EXISTS " + tableName;
        Statement statement;
        connection.setCatalog("sumo");
        statement = connection.createStatement();
        statement.executeUpdate(query);
        query = "CREATE TEMPORARY TABLE " + tableName + " ENGINE=MEMORY SELECT id, AVG(step) AS transitTime, AVG(speed) AS transitSpeed FROM `sumo`.`" + INPUT_TABLE_NAME + "` WHERE ST_DISTANCE(POINT(positionX,positionY), POINT(" + vilX + "," + vilY + "))<" + vilRadius + " AND (IF((angle)>=" + H1_LOWER + " OR (angle)<" + H1_UPPER + ",'H1',IF((angle)>=" + H2_LOWER + " AND (angle)<" + H2_UPPER + ",'H2',IF((angle)>=" + H3_LOWER + " AND (angle)<" + H3_UPPER + ",'H3',IF((angle)>=" + H4_LOWER + " AND (angle)<" + H4_UPPER + ",'H4','HX'))))) LIKE '" + vilHeading + "' GROUP BY id";
        statement = connection.createStatement();
        statement.executeUpdate(query);

        int rowCount = -1;
        statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + tableName);
        while (rs.next()) {
            rowCount = rs.getInt("COUNT(*)");
        }

        statement.close();
        statement = null;
        System.out.println(Thread.currentThread().getName() + ":\tCreated temp table: " + tableName + " " + rowCount + " rows");
    }

    public float queryDeltaTimeBetween2Vils(int stepFrom, int stepTo, String entryTableName, String exitTableName) throws SQLException, JSONException {
        String query = "SELECT AVG(" + exitTableName + ".transitTime-" + entryTableName + ".transitTime) AS deltaTime FROM " + exitTableName + " LEFT JOIN " + entryTableName + " ON " + exitTableName + ".id = " + entryTableName + ".id WHERE " + exitTableName + ".transitTime>" + stepFrom + " AND " + exitTableName + ".transitTime<=" + stepTo;
        connection.setCatalog("sumo");
        Statement statement;
        statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);
        float result = -1;
        while (rs.next()) {
            result = rs.getFloat("deltaTime");
        }
        statement.close();
        statement = null;
        return result;
    }

    public void InsertRowVILAverageSpeed(int stepFrom, int stepTo, float vilA, float vilB, float vilC, float vilD, float vilE, float vilF, float vilInt, float vilIntA2C, float vilIntB2C, float vilIntD2C, float vilIntC2A, float vilIntB2A, float vilIntD2A, float vilIntB2D, float vilIntD2B) throws SQLException, JSONException {
        String query = "REPLACE INTO `sumo_processed_averagespeed`.`" + OUTPUT_TABLE_NAME + "` (`stepFrom`,`stepTo`,`VIL-A`,`VIL-B`,`VIL-C`,`VIL-D`,`VIL-E`,`VIL-F`,`VIL-INT`,`VIL-INT_A2C`,`VIL-INT_B2C`,`VIL-INT_D2C`,`VIL-INT_C2A`,`VIL-INT_B2A`,`VIL-INT_D2A`,`VIL-INT_B2D`,`VIL-INT_D2B`) VALUES (";
        query += "" + stepFrom + ",";
        query += "" + stepTo + ",";
        query += "" + vilA + ",";
        query += "" + vilB + ",";
        query += "" + vilC + ",";
        query += "" + vilD + ",";
        query += "" + vilE + ",";
        query += "" + vilF + ",";
        query += "" + vilInt + ",";
        query += "" + vilIntA2C + ",";
        query += "" + vilIntB2C + ",";
        query += "" + vilIntD2C + ",";
        query += "" + vilIntC2A + ",";
        query += "" + vilIntB2A + ",";
        query += "" + vilIntD2A + ",";
        query += "" + vilIntB2D + ",";
        query += "" + vilIntD2B + ")";
        connection.setCatalog("sumo_processed_averagespeed");
        Statement statement;
        statement = connection.createStatement();
        statement.execute(query);
        statement.close();
        statement = null;
    }

    public void InsertRowVILDeltaTime(int stepFrom, int stepTo, float deltaTimeA2C, float deltaTimeB2C, float deltaTimeD2C, float deltaTimeC2A, float deltaTimeB2A, float deltaTimeD2A, float deltaTimeB2D, float deltaTimeD2B) throws SQLException, JSONException {
        String query = "REPLACE INTO `sumo_processed_deltatime`.`" + OUTPUT_TABLE_NAME + "` (stepFrom,stepTo,deltaTimeA2C,deltaTimeB2C,deltaTimeD2C,deltaTimeC2A,deltaTimeB2A,deltaTimeD2A,deltaTimeB2D,deltaTimeD2B) VALUES (";
        query += "'" + stepFrom + "',";
        query += "'" + stepTo + "',";
        query += "'" + deltaTimeA2C + "',";
        query += "'" + deltaTimeB2C + "',";
        query += "'" + deltaTimeD2C + "',";
        query += "'" + deltaTimeC2A + "',";
        query += "'" + deltaTimeB2A + "',";
        query += "'" + deltaTimeD2A + "',";
        query += "'" + deltaTimeB2D + "',";
        query += "'" + deltaTimeD2B + "')";
        connection.setCatalog("sumo_processed_deltatime");
        Statement statement;
        statement = connection.createStatement();
        statement.execute(query);
        statement.close();
        statement = null;
    }
}

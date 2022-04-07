package utilities;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class User {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* Global Variables */
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String username, email, salt;
    private int iterations, id, groupID = 1;
    private ArrayList<String> group = new ArrayList<>();
    private String password;
    private Long numShares;
    private ResultSet rs;
    private Connection connection;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* Description of Tables */
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /* GRAMMAR OF DESCRIPTIONS
     * Table Name
     * Columns
     * Notes
     * */

    //CREATE TABLE `sql3272723`.`Groups`
    // ( `id` INT NOT NULL AUTO_INCREMENT COMMENT 'The identification number of the Group, used to relate user positions in each group seperately' ,
    // `name` VARCHAR(20) NOT NULL COMMENT 'The name of the group' ,
    // `password` VARCHAR(20) NULL COMMENT 'Not every group will need a password' ,
    // `start date` DATE NOT NULL COMMENT 'The start date of the group' ,
    // `end date` DATE NOT NULL COMMENT 'The end date of the group' ,
    // `commission` DECIMAL(12, 3) NOT NULL DEFAULT '4.99' COMMENT 'The commission per trade' ,
    // `lim` INT NOT NULL DEFAULT '20' COMMENT 'The limit, in percent, rounded to the nearest integer' ,
    // `low price` DECIMAL NOT NULL DEFAULT '1.000' COMMENT 'The lowest stock price that can be traded' ,
    // `account value` DECIMAL(12, 3) NOT NULL DEFAULT '100000.000' COMMENT 'The starting amount each user gets in the group' ,
    // `short` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether or not short selling is allowed, 0 is false' ,
    // `admin password` VARCHAR(160) NOT NULL COMMENT 'The password that allows the group to be altered, for members to be kicked',
    // `creator` VARCHAR(20) NOT NULL COMMENT 'Username of the creator of the group',
    // `salt` VARCHAR(32) NOT NULL COMMENT 'salt for the group's admin password'
    // `iterations` INT NOT NULL COMMENT 'the number of iterations of the hasher for the admin password'
    // PRIMARY KEY (`id`), UNIQUE `name` (`name`)) ENGINE = InnoDB;

    //CREATE TABLE `sql3272723`.`trades` (
    // `groupID` INT NOT NULL COMMENT 'group id for this user\'s trade' ,
    // `userID` INT NOT NULL COMMENT 'user id for this trade' ,
    // `symbol` VARCHAR(10) NOT NULL COMMENT 'the symbol in the trade' ,
    // `delta shares` INT NOT NULL COMMENT 'the change in the number of shares for this trade, negative is short/sell, positive is cover/long' ,
    // `execution price` DECIMAL(12, 3) NOT NULL COMMENT 'the price of the underlying security' ,
    // `execution time` TIMESTAMP NOT NULL COMMENT 'the time that the trade occurred' )
    // ENGINE = InnoDB COMMENT = 'for record keeping';

    //CREATE TABLE `sql3272723`.`group members` (
    // `groupID` INT NOT NULL ,
    // `userID` INT NOT NULL ,
    // `account value` DECIMAL(12, 3) NOT NULL ,
    // UNIQUE `uniqueness clause` (`groupID`, `userID`))
    // ENGINE = InnoDB;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* Constructors */
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    public User(int id) {
        this.id = id;
    }

    public User(int id, String username) {
        this(id);
        this.username = username;
    }

    /**
     * @param username The username that will be assigned to this Start.User object
     * @param password The password that will be assigned to this Start.User object
     *                 This constructor should be called when the user is trying to login
     *                 i.e., the user has already created an account and is trying to get the data for said account
     */
    public User(int id, String username, String password) {
        this(id, username);
        this.password = password;
    }

    public User(int id, String username, String password, String email) {
        this(id, username, password);
        this.email = email;
    }

    public User(int id, String username, String password, String email, String salt) {
        this(id, username, password, email);
        this.salt = salt;
    }

    public User(int id, String username, String password, String email, String salt, int iterations) {
        this(id, username, password, email, salt);
        this.iterations = iterations;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* Database Query Functions */
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    //For the acquisition of the users ID, unknown id
    private int getID() throws SQLException {
        try {
            connectToDB();
            String query = "SELECT * FROM users WHERE username = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, this.username);

            rs = preparedStatement.executeQuery();
            if (!rs.next()) return 0;
            return rs.getInt(1);
        } finally {
            disconnectFromDB();
        }
    }

    //For the passing of the users ID, known id
    public int getUserID() {
        return this.id;
    }

    private String getUsername() throws SQLException {
            connectToDB();
            String query = "SELECT * FROM `users` WHERE `id` = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, this.id);

            rs = preparedStatement.executeQuery();
            if (!rs.next()) return "Unknown User";
            disconnectFromDB();
            return rs.getString(2);

    }

    public void setGroupID(String groupID) throws SQLException {
        try {
            connectToDB();
            String query = "SELECT * FROM `Groups` WHERE `name` = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, groupID);
            rs = preparedStatement.executeQuery();
            if (!rs.next()) return;
            this.groupID = rs.getInt(1);
        } finally {
            disconnectFromDB();
        }
    }

    final public long getShareSize(String symbol) throws SQLException {
        try {
            connectToDB();
            String query = "SELECT * FROM `positions` WHERE `userID` = ? AND `groupID` = ? AND `symbol` = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, this.id);
            preparedStatement.setInt(1, this.groupID);
            preparedStatement.setString(1, symbol);
            rs = preparedStatement.executeQuery();

            long totalNumShares = 0;
            while (rs.next()) {
                totalNumShares += rs.getInt(5);
            }

            return totalNumShares;

        } finally {
            disconnectFromDB();
        }
    }

    private long getAccountValue() throws SQLException {
        try {
            rs = getGroupMembers();

            return rs.getInt(3);

        } finally {
            disconnectFromDB();
        }
    }

    private void changeAccountValue(Double delta) throws SQLException {
        try {
            connectToDB();
            String query = "UPDATE `group members` SET `account value` = `account value` + ? WHERE `groupID` = ? AND `userID` = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setDouble(1, delta);
            preparedStatement.setInt(2, this.groupID);
            preparedStatement.setInt(3, this.id);
            preparedStatement.execute();
        } finally {
            disconnectFromDB();
        }
    }

    final public Object[][] getOpenPositions() throws SQLException {
        try {
            connectToDB();
            String query = "SELECT * FROM `positions` WHERE `groupID` = ? AND `userID` = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, this.groupID);
            preparedStatement.setInt(2, this.id);
            rs = preparedStatement.executeQuery();

            Object[][] data = new Object[rs.getFetchSize()][3];

            int index = 0;
            while (rs.next()) {
                data[index][0] = rs.getString(3);
                data[index][1] = rs.getDouble(4);
                data[index][2] = rs.getLong(5);
                index++;
            }
            return data;
        } finally {
            disconnectFromDB();
        }
    }

    private void addPosition(String symbol, BigDecimal price, long numShares) throws SQLException {
        try {
            connectToDB();
            String query = "INSERT INTO `positions` (`groupID`, `userID`, `symbol`, `price`, `share size`) " +
                    "VALUES(?, ?, ?, ?, ?);";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, this.groupID);
            preparedStatement.setInt(2, this.id);
            preparedStatement.setString(3, symbol);
            preparedStatement.setDouble(4, price.doubleValue());
            preparedStatement.setLong(5, numShares);
            preparedStatement.execute();

        } finally {
            disconnectFromDB();
        }
    }

    final public Object[][] getTrades() throws SQLException {
        try {
            connectToDB();//symbol the symbol in the trade
            // delta shares the change in the number of shares for this trade,…
            // execution price the price of the underlying security	execution time the time that the trade occurred
            String query = "SELECT * FROM `trades` WHERE `groupID` = ? AND `userID` = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, this.groupID);
            preparedStatement.setInt(2, this.id);
            rs = preparedStatement.executeQuery();

            int index = 0;
            Object[][] data = new Object[rs.getFetchSize()][4];
            while (rs.next()) {
                data[index][0] = rs.getString(3);
                data[index][1] = rs.getLong(4);
                data[index][2] = rs.getDouble(5);
                data[index][3] = rs.getTime(6);
                index++;
            }

            return data;

        } finally {
            disconnectFromDB();
        }
    }

    final public Object[][] getGroupMemberData() throws SQLException {
        try {

            ResultSet resultSet = getGroupMembers();
            resultSet.last();
            Object[][] groupMemberData = new Object[resultSet.getRow()][2];
            resultSet.beforeFirst();

            int index = 0;
            while (resultSet.next()) {
                groupMemberData[index][0] = resultSet.getInt(2);//userID
                groupMemberData[index][1] = resultSet.getDouble(3);//Account value
                index++;
            }

            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < groupMemberData.length; i++) {
                stringBuilder.append(groupMemberData[i][0]);
                stringBuilder.append(',');
            }
            stringBuilder.deleteCharAt(stringBuilder.length()-1);

            String query = "SELECT * FROM `users` WHERE `id` IN(?);";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, stringBuilder.toString());

            resultSet = preparedStatement.executeQuery();
            index = 0;
            while (resultSet.next()) {
                groupMemberData[index][0] = resultSet.getString(2);//username
                index++;
            }

            return groupMemberData;
        } finally {
            disconnectFromDB();
        }
    }

    private ResultSet getGroupMembers() throws SQLException {
        connectToDB();
        String query = "SELECT * FROM `group members` WHERE `userID` = ? AND `groupID` = ? ORDER BY `account value` ASC;";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        preparedStatement.setInt(2, this.groupID);

        return preparedStatement.executeQuery();
    }

    final public boolean createGroup(String groupName,
                                     String password,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     double commission,
                                     int lim,
                                     double lowPrice,
                                     double startAcctVal,
                                     boolean canShort,
                                     String adminPassword) throws SQLException {

        String query = "INSERT INTO `Groups` (`name`, `pword`, `sdate`, " +
                "`edate`, `commission`, `lim`, `low price`, `acct val`, `short`, " +
                "`admin pword`, `creator`, `salt`, `iterations`)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try {
            connectToDB();
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, groupName);
            preparedStatement.setString(2, password);
            preparedStatement.setDate(3, Date.valueOf(startDate));
            preparedStatement.setDate(4, Date.valueOf(endDate));
            preparedStatement.setDouble(5, commission);
            preparedStatement.setInt(6, lim);
            preparedStatement.setDouble(7, lowPrice);
            preparedStatement.setDouble(8, startAcctVal);
            preparedStatement.setBoolean(9, canShort);
            PasswordHasher passwordHasher = new PasswordHasher(adminPassword);
            preparedStatement.setString(10, passwordHasher.getPassword());
            preparedStatement.setString(11, this.username);
            preparedStatement.setString(12, passwordHasher.getSalt());
            preparedStatement.setInt(13, passwordHasher.getMAXITER());
            return preparedStatement.execute();
        } finally {
            disconnectFromDB();
        }
    }

    final public boolean joinGroup(String groupName, String password) throws SQLException {
        try {
            connectToDB();
            //INSERT INTO orders(product_id, qty)
            //SELECT 2, 20 FROM products WHERE id = 2 AND qty_on_hand >= 20
            String query = "SELECT * FROM `Groups` WHERE `name` = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, groupName);
            rs = preparedStatement.executeQuery();

            if (!rs.next()) return false;
            if (!password.equals(rs.getString(3))) return false;

            query = "INSERT INTO `group members` (`groupID`, `userID`, `account value`) VALUES (?, ?, ?);";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, rs.getInt(1));
            preparedStatement.setInt(2, this.id);
            preparedStatement.setDouble(3, rs.getDouble(9));
            return preparedStatement.execute();
        } finally {
            disconnectFromDB();
        }
    }

    final public boolean leaveGroup() throws SQLException {
        try {
            connectToDB();
            String query = "DELETE FROM `group members` WHERE `groupID` = ? AND `userID` = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, this.groupID);
            preparedStatement.setInt(2, this.id);
            return preparedStatement.execute();

        } finally {
            disconnectFromDB();
        }
    }

    final public Object[][] getGroups() throws SQLException {
        try {
            connectToDB();
            String query = "SELECT * FROM `Groups`;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            rs = preparedStatement.executeQuery();

            Object[][] data = new Object[rs.getFetchSize()][3];
            int index = 0;
            while (rs.next()) {
                data[index][0] = rs.getString(2);
                data[index][1] = rs.getString(4);
                data[index][2] = rs.getString(5);
                index++;
            }
            return data;

        } finally {
            disconnectFromDB();
        }
    }

    final public ResultSet groupRules() throws SQLException {
        connectToDB();
        String query = "SELECT * FROM `Groups` WHERE `id` = ? LIMIT 1;";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.groupID);
        return preparedStatement.executeQuery();
    }

    final public Object[][] getGroupSet() throws SQLException {
        try {
            connectToDB();
            String query = "SELECT * FROM `group members` WHERE `userID` = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, this.id);
            rs = preparedStatement.executeQuery();

            query = "SELECT * FROM `Groups` WHERE `id` IN(?);";
            preparedStatement = connection.prepareStatement(query);

            StringBuilder stringBuilder = new StringBuilder();
            while (rs.next()) {
                stringBuilder.append(rs.getInt(1));
            }

            preparedStatement.setString(1, stringBuilder.toString());
            rs = preparedStatement.executeQuery();
            rs.last();
            Object[][] data = new Object[rs.getRow()][2];
            rs.beforeFirst();
            int i = 0;
            while (rs.next()){
                data[i][0] = rs.getString(2);
                data[i][1] = rs.getString(12);
                i++;
            }
            return data;

        } finally {
            disconnectFromDB();
        }
    }

    final public boolean executeTrade(String symbol, BigDecimal price, long delta) throws SQLException {
        try {
            //Check restrictions
            ResultSet resultSet = groupRules();
            if (!resultSet.next()) return false;//nothing was returned, probably an error
            if (price.doubleValue() < resultSet.getDouble(8))
                return false;//trying to trade a stock below the lowest price allowed
            if (System.currentTimeMillis() > rs.getTime(5).getTime()) return false;//trying to trade after its ended

            Object[][] openPositions = getOpenPositions();
            boolean canShort = rs.getBoolean(10);
            if (openPositions.length == 0)
                if (delta < 0 && !canShort) return false;//Trying to short when cant, does not have a position

            BigDecimal currentPositionSize = new BigDecimal("0");
            for (Object[] positions : openPositions) {
                if (positions[0].equals(symbol)) {
                    BigDecimal size = new BigDecimal(((Double) positions[1]) * (Double) positions[2]);
                    currentPositionSize = currentPositionSize.add(size);
                }
            }
            BigDecimal postPostionSize = currentPositionSize.add(new BigDecimal(price.doubleValue() * delta));
            if (postPostionSize.doubleValue() / getAccountValue() > rs.getInt(7))
                return false;//Trying to go over the groups limit
            if (postPostionSize.doubleValue() < 0 && !canShort) return false;//Trying to short when cant, has a position

            String order = "INSERT INTO `trades` (`groupID`, `userID`, `symbol`, `delta shares`, `execution price`)" +
                    " VALUES (?, ?, ?, ?, ?);";
            PreparedStatement preparedStatement = connection.prepareStatement(order);
            preparedStatement.setInt(1, this.groupID);
            preparedStatement.setInt(2, this.id);
            preparedStatement.setString(3, symbol);
            preparedStatement.setLong(4, delta);
            preparedStatement.setDouble(5, price.doubleValue());
            preparedStatement.execute();

            changeAccountValue(price.doubleValue() * delta);

            if (currentPositionSize.doubleValue() == 0.0) {
                addPosition(symbol, price, delta);
            } else {
                String query = "DELETE FROM `positions` WHERE `groupID` = ? AND `userID` = ? AND `symbol` = ?;";
                PreparedStatement preparedStatement1 = connection.prepareStatement(query);
                preparedStatement.setInt(1, this.groupID);
                preparedStatement.setInt(2, this.id);
                preparedStatement.setString(3, symbol);
                preparedStatement.execute();
            }
            return true;
        } finally {
            disconnectFromDB();
        }

    }

    /**
     * This method will insert the calling Start.User object into users
     * users has PRIMARY KEY (userName) duplicate values of userName are not allowed
     */
    final public void insertNewUser() throws Throwable {
        try {
            connectToDB();
            // Perform input validation to detect attacks
            String query = "INSERT INTO `users` (`username`, `pword`, `email`, `salt`, `iterations`) " +
                    "VALUES (?, ?, ?, ?, ?);";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, email);
            preparedStatement.setString(4, salt);
            preparedStatement.setInt(5, iterations);
            preparedStatement.execute();

        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            disconnectFromDB();
        }
    }

    /**
     * needs a username and password combo
     */
    final public boolean validateLogin() throws Throwable {
        try {
            this.id = getID();
            String query = "SELECT * FROM users WHERE id = ?;";
            connectToDB();
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, this.id);

            rs = preparedStatement.executeQuery();
            if(!rs.next())
                return false;
            String derivedKey = rs.getString(3);
            String salt = rs.getString(5);
            int iterations = rs.getInt(6);
            PasswordHasher validator = new PasswordHasher(this.password, salt, iterations);
            return derivedKey.equals(validator.getPassword());
        } finally {
            disconnectFromDB();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* Database Connectivity */
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Connect to the Database for future queries
     */
    private void connectToDB() {
        //Declare database connection variables
        //I wouldn't include these in an entreprise application but I don't want to create a webserver right now
        final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
        final String DB_URL = "jdbc:mysql://localhost:3306/syntheticequitiestrader";
        final String USER = "root";
        final String PASS = "376B19376b19";
        try {
            //Create mysql database connection
            Class.forName("com.mysql.jdbc.Driver");
            //Connect to database
            connection = DriverManager.getConnection(DB_URL,USER,PASS);
        } catch (Exception e) {
            //Print StackTrace
            e.printStackTrace();
        }
    }

    /**
     * Close all connections to the database
     */
    private void disconnectFromDB() throws SQLException {
        if (rs != null && !rs.isClosed()) rs.close();
        if (!connection.isClosed()) connection.close();
    }
}

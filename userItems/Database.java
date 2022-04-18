package userItems;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.sql.*;

@Aspect
public class Database {

  /*
  CREATE TABLE `user_account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_name` varchar(120) NOT NULL,
  `email` varchar(245) DEFAULT NULL,
  `password` varchar(160) NOT NULL,
  `password_salt` varchar(160) DEFAULT NULL,
  `password_hashing_algorithm` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `email_UNIQUE` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
   */



  private ResultSet rs;
  private Connection connection;
  PreparedStatement ps;

  //public constructor default
  public Database() {

  }

  //access the database with the hashed password and username
  public ResultSet retrieveData(User user) {
    if(validateLogin(user)) {
      //get all data from the tables associated with teh user
      return null;
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  /* Database Connectivity */
  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Connect to the Database for future queries for the user and group
   */
  //annotation to tell the compiler that this method is called before the method is called
  @Before("execution(* userItems.Database(..))")
  public boolean connect() {
    //Declare database connection variables
    //I wouldn't include these in an entreprise application but I don't want to create a webserver right now
    final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    final String DB_URL = "jdbc:mysql://localhost:3306/syntheticequitiestrader";
    final String USER = "root";
    final String PASS = "376B19376b19";

    //Create mysql database connection
    try {
      Class.forName(JDBC_DRIVER);
      connection = DriverManager.getConnection(DB_URL, USER, PASS);
      return true;
    } catch(SQLException | ClassNotFoundException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Connect to the database specified by the user
   */
  public boolean connect(final String JDBC_DRIVER, final String DB_URL, final String USER, final String PASS) {
    //Create mysql database connection
    try {
      Class.forName(JDBC_DRIVER);
      connection = DriverManager.getConnection(DB_URL, USER, PASS);
      return true;
    } catch(SQLException | ClassNotFoundException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Close all connections to the database
   */
  public void disconnect() {
    try {
      if(rs != null && !rs.isClosed()) rs.close();
      if(connection != null && !connection.isClosed()) connection.close();
    } catch(SQLException e) {
      e.printStackTrace();
    }
  }

  public boolean insertUser(User user) {
    //make a new prepared statement to insert the user into the database
    try {
      //create a new hasher to hash the password
      PasswordHasher hasher = new PasswordHasher(user.getPassword());
      //get the hashed password
      //create a new prepared statement to insert the user into the database
      ps = connection.prepareStatement("INSERT INTO user_account (user_name, password, password_salt, password_hashing_algorithm, email) VALUES (?, ?, ?, ? , ?)");
      //set the parameters for the prepared statement
      ps.setString(1, user.getUsername());
      ps.setString(2, hasher.getHashedPassword());
      ps.setString(3, hasher.getSalt());
      ps.setString(4, hasher.getAlgorithm());
      ps.setString(5, user.getEmail());
      //execute the prepared statement
      ps.executeUpdate();
      //return true if the user was inserted
      return true;
    } catch(SQLException e) {
      e.printStackTrace();
      //return false if the user was not inserted
      return false;
    }
  }

  public boolean validateLogin(User user) {
    try {
      //use a prepared statement to query the user table for the username and password
      ps = connection.prepareStatement("SELECT * FROM user_account WHERE user_name = ?");
      ps.setString(1, user.getUsername());
      rs = ps.executeQuery();
      if(rs != null && rs.next()) {
        //get the salt and algorithm from the database
        String salt = rs.getString("password_salt");
        String algorithm = rs.getString("password_hashing_algorithm");
        //create a hasher to hash the password
        PasswordHasher hasher = new PasswordHasher(user.getPassword(), salt, algorithm);
        //get the hashed password
        String hashedPassword = hasher.getHashedPassword();
        //check if the hashed password matches the hashed password in the database
        //if it does return the login info is valid, return false otherwise
        return hashedPassword.equals(rs.getString("password"));
      }
      return false;
    } catch(SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean userInDatabase(User user) {
    try {
      //use a prepared statement to query the user table for the username and password
      ps = connection.prepareStatement("SELECT * FROM user_account WHERE user_name = ?");
      ps.setString(1, user.getUsername());
      rs = ps.executeQuery();
      if(rs != null && rs.next()) {
        return true;
      }
      return false;
    } catch(SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

}

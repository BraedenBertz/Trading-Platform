package userItems;

import OrderTypes.StockOrder;
import org.aspectj.lang.annotation.Before;

import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * This class is used to store user information and perform database operations
 * as applied to the user table.
 */
public class User {

    private String username;
    private String password;
    private int id;
    private Database db = new Database();
    //Arraylist of the users orders
    private ArrayList<StockOrder> orders = new ArrayList<StockOrder>();
    private String email;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* Constructors */
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructor for the User class that takes an ID as a parameter.
     * @param id The ID of the user.
     */
    public User(int id) {
        this("", "");
        this.id = id;
    }

    /**
     * Constructor for User class.
     * @param username
     * @param password
     */

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * validateUser() is used to validate the user's username and password against the database.
     * @return true if the user is valid, and in the database, false if the user is not valid.
     */
    public boolean validateLogin() {
        return db.validateLogin(this);
    }

    private boolean getUserInfo() {

        return true;
    }

    private void parseUserData(ResultSet userDataFromDatabase) {
    }

    public boolean insertNewUser() {
        return false;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getId() {
        return id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setEmail(String text) {
        this.email = text;
    }

    public String getEmail() {

        return email;
    }
}

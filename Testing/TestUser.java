package Testing;

import org.junit.jupiter.api.Test;
import userItems.User;

public class TestUser {


  /**
   * Test the constructor of the User class.
   */
  @Test
  public void testUserConstructor() {
    User user = new User("testUser", "testPassword");
    assert user.getUsername().equals("testUser");
    assert user.getPassword().equals("testPassword");
  }

  /**
   * Test the getUsername method of the User class.
   */
  @Test
  public void testGetUsername() {
    User user = new User("testUser", "testPassword");
    assert user.getUsername().equals("testUser");
  }

  /**
   * Test the getPassword method of the User class.
   */
  @Test
  public void testGetPassword() {
    User user = new User("testUser", "testPassword");
    assert user.getPassword().equals("testPassword");
  }

  /**
   * Test the setUsername method of the User class.
   */
  @Test
  public void testSetUsername() {
    User user = new User("testUser", "testPassword");
    user.setUsername("newTestUser");
    assert user.getUsername().equals("newTestUser");
  }

  /**
   * Test the setPassword method of the User class.
   */
  @Test
  public void testSetPassword() {
    User user = new User("testUser", "testPassword");
    user.setPassword("newTestPassword");
    assert user.getPassword().equals("newTestPassword");
  }

  /**
   * Test the equals method of the User class.
   */
  @Test
  public void testEquals() {
    User user = new User("testUser", "testPassword");
    User user2 = new User("testUser", "testPassword");
    assert user.equals(user2);
  }

  /**
   * Test the toString method of the User class.
   */
  @Test
  public void testToString() {
    User user = new User("testUser", "testPassword");
    assert user.toString().equals("testUser");
  }
}


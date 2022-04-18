package userItems;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

//Hash logic for the user's password
public class PasswordHasher {

    private final int MAXITER = 80000, DKLEN = 160, SALTLENGTH = 32;
    private final String salt;
    private final String password;
    private static String algorithm = "PBKDF2WithHmacSHA512";



    /**
     * Constructor for a PasswordHasher given only the password, used for encrypting for db
     * @param password the password to be hashed
     */
    public PasswordHasher(String password) {
        StringBuilder stringBuilder = new StringBuilder();

        //Random salt
        for(int i = 0; i < SALTLENGTH; i++) {
            stringBuilder.append(getRandomCharacter('A', 'Z'));
        }
        this.salt = stringBuilder.toString();
        this.password = password;

    }

    /**
     * Constructor for a PasswordHasher given only the password, used for encrypting for db
     * @param password the password to be hashed
     */
    public PasswordHasher(String password, String algorithm) {
        StringBuilder stringBuilder = new StringBuilder();

        //Random salt
        for(int i = 0; i < SALTLENGTH; i++) {
            stringBuilder.append(getRandomCharacter('A', 'Z'));
        }
        this.salt = stringBuilder.toString();
        this.password = password;

    }

    /**
     * password hasher for presets, usually a validator
     * @param password the given password
     * @param salt the given salt
     * @param algorithm the given algorithm for hashing
     *                  PBKDF2WithHmacSHA512 is the default
     * @return a hashed password
     */
    public PasswordHasher(String password, String salt, String algorithm){
        this.salt = salt;
        this.password = password;
        PasswordHasher.algorithm = algorithm;
    }

    public String getPassword(){
        return password;
    }

    public String getSalt() {
        return salt;
    }

    public int getMAXITER() {
        return MAXITER;
    }

    /**
     * create the password for the user
     * @return a hashed password
     */
    public String getHashedPassword() {

        char[] passwordChars = password.toCharArray();
        byte[] saltBytes = salt.getBytes();
        byte[] hashedBytes = hashPassword(passwordChars, saltBytes, MAXITER, DKLEN);

        return Hex.encodeHexString(hashedBytes);
    }

    /**
     * get a random character for the salt between ch1 and ch2 inclusive
     * @param ch1 character begin
     * @param ch2 character end
     * @return a random character between ch1 and ch2 inclusive
     */
    private char getRandomCharacter(char ch1, char ch2) {
        return (char) (ch1 + Math.random() * (ch2 - ch1 + 1));
    }

    /**
     * Hash password with SHA512
     * @param password password to be hashed
     * @param salt salt for the password
     * @param iterations how many iterations to hash
     * @param keyLength how long we want the final key length to be
     * @return the hashed password
     */
    private static byte[] hashPassword(final char[] password, final byte[] salt, final int iterations, final int keyLength) {
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
            PBEKeySpec spec = new PBEKeySpec( password, salt, iterations, keyLength );
            SecretKey key = skf.generateSecret( spec );
            return key.getEncoded( );
        } catch ( NoSuchAlgorithmException | InvalidKeySpecException e ) {
            throw new RuntimeException( e );
        }
    }

    public String getAlgorithm() {
        return algorithm;
    }
}

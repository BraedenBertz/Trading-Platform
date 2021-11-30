package utilities;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class PasswordHasher {

    private int MAXITER = 80000, DKLEN = 512, SALTLENGTH = 32;
    private String salt, password;

    public PasswordHasher(String password){
        StringBuilder stringBuilder = new StringBuilder();

        //Random salt
        for (int i = 0; i < SALTLENGTH; i++){
            stringBuilder.append(getRandomCharacter('A', 'Z'));
        }
        this.salt = stringBuilder.toString();
        this.password = password;
        this.password = createPassword();
    }

    public PasswordHasher(String password, String salt, int maxIter){
        this.salt = salt;
        this.MAXITER = maxIter;
        this.password = password;
        this.password = createPassword();
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

    private String createPassword() {

        char[] passwordChars = password.toCharArray();
        byte[] saltBytes = salt.getBytes();
        byte[] hashedBytes = hashPassword(passwordChars, saltBytes, MAXITER, DKLEN);

        return Hex.encodeHexString(hashedBytes);
    }

    private char getRandomCharacter(char ch1, char ch2) {
        return (char) (ch1 + Math.random() * (ch2 - ch1 + 1));
    }

    private static byte[] hashPassword(final char[] password, final byte[] salt, final int iterations, final int keyLength) {

        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA512" );
            PBEKeySpec spec = new PBEKeySpec( password, salt, iterations, keyLength );
            SecretKey key = skf.generateSecret( spec );
            return key.getEncoded( );
        } catch ( NoSuchAlgorithmException | InvalidKeySpecException e ) {
            throw new RuntimeException( e );
        }
    }
}

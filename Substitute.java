// Author: Tait Kline
import java.security.SecureRandom;

public class Substitute implements SymCipher
{
    byte[] key;
    // parameterless constructor will create a 256 byte array which is a random permutation
    // of the 256 possible byte values and will serve as a map from bytes to their substitution
    // values.
    Substitute()
    {
        key = new byte[256];
        // byte values range from -128 to 127, store this range into the array
        byte value = -128;
        for (int i = 0; i < key.length; i++)
        {
            key[i] = value;
            value++;
        }

        SecureRandom rng = new SecureRandom();
        // shuffle the values in the array
        for (int i = 0; i < key.length; i++ )
        {
            int randomIndex = rng.nextInt(256);
            // swap current index with random index
            byte temp = key[randomIndex];
            key[randomIndex] = key[i];
            key[i] = temp;
        }
    }

    Substitute(byte[] key)
    {
        this.key = key.clone();
    }

    // Return an array of bytes that represent the key for the cipher
	public byte [] getKey()
    {
        return key;
    }	
	
	// Encode the string using the key and return the result as an array of
	// bytes.  Note that you will need to convert the String to an array of bytes
	// prior to encrypting it.  Also note that String S could have an arbitrary
	// length, so your cipher may have to "wrap" when encrypting (remember that
	// it is a block cipher)
	public byte [] encode(String S)
    {
        byte[] encryption = S.getBytes();       // get byte array representation of argument string
        // iterate through all of the bytes, substituting the appropriate bytes from the key
        for (int i = 0; i < encryption.length; i++)
        {
            // must add 128 to the encryption byte value to make it allign with the key index
            int index = encryption[i] + 128;
            encryption[i] = key[index];
        }

        return encryption;
    }
	
	// Decrypt the array of bytes and generate and return the corresponding String.
	public String decode(byte [] bytes)
    {
        // make decoding array
        byte[] decodeArray = new byte[256];
        for (int i = 0; i < key.length; i++)
        {
            int index = key[i] + 128;
            decodeArray[index] = (byte) (i - 128);
        }

        // decode bytes array
        for (int i = 0; i < bytes.length; i++)
        {
            int index = bytes[i] + 128;
            bytes[i] = decodeArray[index];
        }

        String decoded = new String(bytes);     // convert byte array back to a string
        return decoded;
        
    }
}

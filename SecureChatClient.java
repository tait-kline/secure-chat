// Author: Tait Kline
import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class SecureChatClient extends JFrame implements Runnable, ActionListener
{
    
    public static final int PORT = 8765;

	// In the SecureChatClient the reader below should be
	// ObjectInputStream and the writer should be ObjectOutputStream
	// See details in the SecureChatServer.java code.
    ObjectInputStream myReader;
    ObjectOutputStream myWriter;
    JTextArea outputArea;
    JLabel prompt;
    JTextField inputField;
    String myName, serverName;
	Socket connection;
    SymCipher cipher;

    public SecureChatClient()
    {
        try {

        myName = JOptionPane.showInputDialog(this, "Enter your user name: ");
        serverName = JOptionPane.showInputDialog(this, "Enter the server name: ");
        InetAddress addr = InetAddress.getByName(serverName);
        connection = new Socket(addr, PORT);   // Connect to server with new
                                               // Socket
        
        // create reader and writer on the socket
        myWriter = new ObjectOutputStream(connection.getOutputStream());
        myWriter.flush();
        myReader = new ObjectInputStream(connection.getInputStream());

        // Begin handshaking process:
        // read the server’s public key, E, as a BigInteger object
        BigInteger E = (BigInteger) myReader.readObject();
        System.out.println("Recieved E: " + E);
        // read the server’s public mod value, N, as a BigInteger object
        BigInteger N = (BigInteger) myReader.readObject();
        System.out.println("Recieved N: " + N);
        // read the server's preferred symmetric cipher (either "Sub" or "Add"), as a String object
        String cipherType = (String) myReader.readObject();
        // Based on the value of the cipher preference, create either a Substitute object or an Add128 object, 
        // storing the resulting object in a SymCipher variable.
        
        if (cipherType.equals("Sub"))
        { 
            cipher = new Substitute();
            System.out.println("Recieved cipher: Substitite");
        }  
        else
        { 
            cipher = new Add128();
            System.out.println("Recieved cipher: Add128");
        }
        // get the key from cipher object using the getKey() method, 
        byte [] key = cipher.getKey();
        // print key to console
        System.out.print("Unencrypted key to send:");
        for (byte curr : key) 
            System.out.print(curr + " ");
        System.out.println();
            
        
        // convert the result into a BigInteger object
        BigInteger bigKey = new BigInteger(1, key);
        // RSA-encrypt the BigInteger version of the key using E and N, 
        // and send the resulting BigInteger to the server
        bigKey = bigKey.modPow(E, N);
        myWriter.writeObject(bigKey); myWriter.flush();

        // encrypt user's name using the symmetric cipher and send it to the server.
        byte [] encodedName = cipher.encode(myName);
        myWriter.writeObject(encodedName); myWriter.flush();
        this.setTitle(myName);              // Set title to identify chatter

        Box b = Box.createHorizontalBox();  // Set up graphical environment for
        outputArea = new JTextArea(8, 30);  // user
        outputArea.setEditable(false);
        b.add(new JScrollPane(outputArea));

        outputArea.append("Welcome to the Chat Group, " + myName + "\n");

        inputField = new JTextField("");  // This is where user will type input
        inputField.addActionListener(this);

        prompt = new JLabel("Type your messages below:");
        Container c = getContentPane();

        c.add(b, BorderLayout.NORTH);
        c.add(prompt, BorderLayout.CENTER);
        c.add(inputField, BorderLayout.SOUTH);

        Thread outputThread = new Thread(this);  // Thread is to receive strings
        outputThread.start();                    // from Server

		addWindowListener(
                new WindowAdapter()
                {
                    public void windowClosing(WindowEvent e)
                    { 
                        try
                        {
                            byte [] closingMSG = cipher.encode("CLIENT CLOSING");
                            myWriter.writeObject(closingMSG); myWriter.flush();
                            System.out.println("Sending CLIENT CLOSING msg to server");
                            System.exit(0);
                        }
                        catch (Exception ex)
                        {
                            System.out.println("Problem sending client closing MSG!");
                            System.exit(0);
                        }
                     
                     }
                }
            );

        setSize(500, 200);
        setVisible(true);

        }
        catch (Exception e)
        {
            System.out.println("Problem starting client!");
        }
    }

    // Wait for a message to be received, then show it on the output area
    // In your SecureChatClient you will need to decode the message before
    // appending it.
    public void run()
    {
        while (true)
        {
            String currMsg = null;
            byte[] newBytes = null;
             try {
                newBytes = (byte []) myReader.readObject();

                // print bytes recieved from server
                System.out.println("Message recieved from server: ");
                System.out.print("\tBytes recieved: ");
                for (byte curr : newBytes) 
                    System.out.print(curr + " ");
                System.out.println();

				currMsg = cipher.decode(newBytes);  // decode msg
                // print decryted byte array
                byte [] decryptedBytes = currMsg.getBytes();    // get byte array from decrypted string
                System.out.print("\tBytes decoded: ");
                for (byte curr : decryptedBytes) 
                    System.out.print(curr + " ");
                System.out.println();

                // print string msg
                System.out.println("\tCorresponding String: " + currMsg);
                System.out.println();

			    outputArea.append(currMsg+"\n");
             }
             catch (Exception e)
             {
                System.out.println(e +  ", closing client!");
                break;
             }
        }
        System.exit(0);
    }

	// Get message typed in from user (from inputField) then add name and send
	// it to the server.  In your SecureChatClient you will need to encode the
	// message before sending it.
    public void actionPerformed(ActionEvent e)
    {
        String currMsg = e.getActionCommand();      // Get input value
        System.out.println("Sending Message to Server:");
        currMsg = myName + ":" + currMsg;           // add name
        // print original string
        System.out.println("\tOriginal String: " + currMsg);
        // print byte array
        byte [] decryptedBytes = currMsg.getBytes();    // get byte array from decrypted string
        System.out.print("\tBytes decoded: ");
            for (byte curr : decryptedBytes) 
                System.out.print(curr + " ");
            System.out.println();

        
        byte [] encodedMsg = cipher.encode(currMsg);    // encode msg
        // print encoded bytes
        System.out.print("\tBytes encoded: ");
            for (byte curr : encodedMsg) 
                System.out.print(curr + " ");
            System.out.println();
            System.out.println();

        inputField.setText("");

        try{
            myWriter.writeObject(encodedMsg); myWriter.flush();   // send msg
        }
        catch (Exception ex){
            System.out.println("Problem sending message to server!");
        }
    }                     
    
    public static void main(String [] args)
    {
         SecureChatClient JR = new SecureChatClient();
         JR.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }
}

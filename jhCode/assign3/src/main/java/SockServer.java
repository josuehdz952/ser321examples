import java.net.*;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.*;
import java.math.BigInteger;

import org.json.*;

/**
 * A class to demonstrate a simple client-server connection using sockets.
 * Ser321 Foundations of Distributed Software Systems see
 * http://pooh.poly.asu.edu/Ser321
 * 
 * @author Tim Lindquist Tim.Lindquist@asu.edu Software Engineering, CIDSE,
 *         IAFSE, ASU Poly
 * @version August 2020
 * 
 * @modified-by David Clements <dacleme1@asu.edu> September 2020
 * @modified-by Josue Hernandez <iherna31@asu.edu> February 2022
 */

// This code is just very rought and you can keep it that way. It is only
// supposed to give you a rough skeleton with very rough error handling to make
// sure
// the server will not crash. Should in theory be done a bit cleaner but the
// main pupose here is to practice creating a protocol so I want to keep it as
// simple
// as possible.

public class SockServer {
  public static void main(final String args[]) {
    Socket sock;
    int enc_len = 0;
    final int port = Integer.parseInt(args[0]); // no error handling on input arguments, you can assume they are correct
                                                // here

    try { // basic try catch just to catch exceptions
      // open socket
      final ServerSocket serv = new ServerSocket(port); // create server socket on port 8888
      while (true) {
        try { // this is not very pretty BUT it will keep the server running even if the
              // client sends a bad request which you might not handle in the server. The
              // server will print the
              // stacktrace and then wait for a new connection. The client will just stay in
              // wait for a response which it might never get

          System.out.println("Server ready for connections");

          sock = serv.accept(); // blocking wait

          // setup the object reading channel
          final ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

          // get output channel
          final OutputStream out = sock.getOutputStream();
          // create an object output writer (Java only)
          final ObjectOutputStream os = new ObjectOutputStream(out);

          // Read request
          final String s = (String) in.readObject();

          // QUESTION: What could you do to make sure the server does not crash if the
          // client does not send a valid JSON?
          final JSONObject request = new JSONObject(s);

          System.out.println("Received the JSON " + request);

          final JSONObject response = new JSONObject();

          // this is the part where you would add your own service. Replace the two
          // services here with your services and try to handle the part where the client
          // might send a wrong request
          if (request.getString("type").equals("echo")) {

            /*
             * Here I am very nit picky with the echo request, checking that it has a "data"
             * field, and checking that that field is actually a String. And returning an
             * appropriate message. If you send a wrong request in the "reverse" one, e.g.
             * you send an int instead of a String the try/catch block will catch it and the
             * client will just be in limbo since they will not get a reply. Not great way
             * to handle wrong requests.
             */

            if (request.has("data")) {
              if (request.get("data").getClass().getSimpleName().equals("String")) { // ok case
                response.put("type", "echo");
                response.put("data", request.getString("data"));
              } else {
                response.put("type", "error");
                response.put("message", "no String");
              }
            } else {
              response.put("type", "error");
              response.put("message", "Data missing");
            }

          } //custom request 1
          else if(request.getString("type").equals("encrypt")) {
        	  if (request.has("data")) {
                  if (request.get("data").getClass().getSimpleName().equals("String")) { // ok case
                	try {
                    String targetS = request.getString("data");
                    byte[] input = new byte[targetS.length()];
                    input = targetS.getBytes();
                    //key nad initialization vector bytes
                    byte[] keyBytes = new byte[8];
                    byte[] ivBytes = new byte[8];
                 // wrap key data in Key/IV specs to pass to cipher
                    SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
                    IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
                    // create the cipher with the algorithm you choose
                    // see javadoc for Cipher class for more info, e.g.
                    Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
                    cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
                    byte[] encrypted= new byte[cipher.getOutputSize(input.length)];
                    enc_len = cipher.update(input, 0, input.length, encrypted, 0);
                    enc_len += cipher.doFinal(encrypted, enc_len);
                    
                    int n = encrypted.length;
                    String encryptedString = new String(encrypted);
                    
                    
                    response.put("type", "encrypted");
                    response.put("data", Base64.getEncoder().encodeToString(encrypted));
                    byte rawKey[] = key.getEncoded();
                    String encodedKey = Base64.getEncoder().encodeToString(rawKey);
                    response.put("key", encodedKey);
                    byte rawIV[] = ivSpec.getIV();
                    String encodedIV = Base64.getEncoder().encodeToString(rawIV);
                    response.put("iv", encodedIV);
                    
                    System.out.println("The encrypted string is: " + Base64.getEncoder().encodeToString(encrypted));
                    System.out.println("The encrypted key is: " + Base64.getEncoder().encodeToString(key.getEncoded()));
                    System.out.println("The encrypted iv is: " + Base64.getEncoder().encodeToString(ivSpec.getIV()));
                  } 
                  catch(Exception e) {
                	  response.put("type", "error");
                      response.put("message", "Could not encrypt");
                  }
        	  }
        	  }
          }
          //custom request 2
          else if(request.getString("type").equals("decrypt")) {
        	  if (request.has("data")) {
                  if (request.get("data").getClass().getSimpleName().equals("String")) { // ok case
                    String targetS = request.getString("data");
                    byte[] input = Base64.getDecoder().decode(targetS);
                    
                    System.out.println(input);
                    //key nad initialization vector bytes
                    
                    	try {
                    	byte[] keyBytes = Base64.getDecoder().decode(request.getString("key"));
                    	
                    	byte[] ivBytes = Base64.getDecoder().decode(request.getString("iv"));
                    	SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
                    	IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
                    	Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
                        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
                        byte[] decrypted = new byte[cipher.getOutputSize(enc_len)];
                        int dec_len = cipher.update(input, 0, enc_len, decrypted, 0);
                        dec_len += cipher.doFinal(decrypted, dec_len);
                        
                        String decString = new String(decrypted);
                        System.out.println("The decrypted string is: " + decString);
                        response.put("type", "decrypted");
                        response.put("data", decString);
                        
                    	}catch(Exception e) {
                    		response.put("type", "error");
                            response.put("message", "Invalid Key and IV Parameter");
                    	}
                  } 
        	  } else {
        		  response.put("type", "error");
                  response.put("message", "No previously exchanged key");
        	  }
          }
          
          else if (request.getString("type").equals("reverse")) {
            response.put("type", "reverse");

            // things to reverse the String
            StringBuilder input1 = new StringBuilder();
            // append a string into StringBuilder input1
            input1.append(request.getString("data"));
            // reverse StringBuilder input1
            input1.reverse();
            response.put("data", input1.toString());

          } else if (request.getString("type").equals("exit")) {
            response.put("type", "exit");
            response.put("data", "Good bye!");
            // write the whole message
            os.writeObject(response.toString());
            // make sure it wrote and doesn't get cached in a buffer
            os.flush();

            // closing off the connection
            // sock.close();
            // in.close();
            // out.close();
            // serv.close();
            // break;
          } else { // very basic error handling here and one type or error message if request type
                   // is not known
            response.put("type", "error");
            response.put("message", "Request type not known");
          }

          // write the whole message
          os.writeObject(response.toString());
          // make sure it wrote and doesn't get cached in a buffer
          os.flush();

        } catch (final Exception e) { // this is in case something in your protocol goes wrong then the server
                                      // connection should still stay open
          e.printStackTrace();
        }
      }

    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
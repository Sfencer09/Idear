package com.example.idear;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import androidx.annotation.NonNull;

public final class IdearNetworking extends Thread {

    private static IdearNetworking instance;
    private static final int tcpPort = 30501;
    private static final int udpPort = 30501;

    /*static {
        Log.d("CREATE" ,"In the static block.");
        Thread dnsThread = new Thread(){
            @Override
            public void run() {
                Log.d("CREATE" ,"Running...");
                InetAddress temp = null;
                try {
                    Log.d("CREATE" ,"Attempting try catch.");
                    temp = InetAddress.getByName("project-treytech.com");
                    Log.d("CREATE" ,"Temp was assigned: " + temp.getHostAddress());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                IdearNetworking.serverAddr = temp;
                Log.d("CREATE", "In thread:" + serverAddr.getHostAddress());
            }
        };
        Log.d("CREATE" ,"Starting DNS Thread...");
        dnsThread.start();
        try {
            Log.d("CREATE" ,"Trying to join thread...");
            dnsThread.join(3000);
            Log.d("CREATE" ,"Thread joined. What went wrong??");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
     */

    private Socket tcpSocket;
    private final Semaphore tcpLock;
    private static Handler tcpThreadHandler;
    private final Semaphore readyLock;

    private DatagramSocket udpSocket;
    private UDPResponseHandler udpHandler;
    private Thread udpListenerThread;

    private final MessageDigest md;

    private boolean loggedIn;
    private final byte[] loginToken;
    private int requestNum;

    static {
        //initialize();
    }

    public static void initialize() throws IOException {
        Log.d("CREATE", "Starting initialization.");
        instance = new IdearNetworking();
        Log.d("CREATE", "Instance created.");
        instance.start();
        Log.d("CREATE", "Started.");
        instance.waitForReady();
        Log.d("CREATE", "Waited to ready.");
    }

    /**
     * Class to represent a single response from the Idear server.
     */
    public static final class IdearResponse implements Serializable {
        private final char responseType;
        private final String imageText;
        private final Serializable imageAudio;
        //private final Rect cropDimensions;
        private static final long serialVersionUID = 01L;

        @Override
        public String toString() {
            return "IdearResponse{" +
                    "responseType=" + responseType +
                    ", imageText='" + imageText + '\'' +
                    '}';
        }

        private IdearResponse() { //used only for deserializing
            this.responseType = 0;
            this.imageText = null;
            this.imageAudio = null;
            //this.cropDimensions = null;
        }

        private IdearResponse(String text, Serializable audio) {
            if (text == null) {
                throw new IllegalArgumentException("Response text cannot be null");
            }
            this.responseType = audio == null ? 't' : 'a';
            this.imageText = text;
            this.imageAudio = audio;
            //this.cropDimensions = null;
        }

        private IdearResponse(String text) {
            this(text, null);
        }

        /*private IdearResponse(Rect cropDim) {
            if(cropDim == null) {
                throw new IllegalArgumentException("Crop dimensions cannot be null");
            }
            this.responseType = 'c';
            this.cropDimensions = cropDim;
            this.imageText = null;
            this.imageAudio = null;
        }*/

        public char getResponseType() {
            return responseType;
        }

        public String getImageText() {
            return imageText;
        }

        public Serializable getImageAudio() {
            return imageAudio;
        }

        /*public Rect getCropDimensions() {
            return cropDimensions;
        }*/

    }

    /**
     * Abstract class for handling responses for images sent over UDP.
     */
    public abstract class UDPResponseHandler {

        /**
         * Handle a single UDP response. Implementations of this method only need to cover the
         * following response types:
         * <ul>
         *     <li>'t' - Transcribed text</li>
         *     <li>'a' - Transcribed text & audio</li>
         *     <li>'c' - Cropping dimensions</li>
         * </ul>
         *
         * @param response The IdearResponse class instance constructed from the server's response
         */
        public abstract void handle(IdearResponse response);

    }

    private final class UDPListener implements Runnable {

        private final UDPResponseHandler responseHandler;

        private UDPListener(UDPResponseHandler handler) {
            if (handler == null) {
                throw new IllegalArgumentException("Handler cannot be null");
            }
            this.responseHandler = handler;
        }

        @Override
        public void run() {

            while (true) {

                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * Creates a string representation of a single type of ancillary data. If using more than one
     * type of ancillary data, the strings returned by each call of this method should be
     * concatenated with the ';' character as a separator.
     *
     * @param type the type of data being represented, such as "acc" for accelerometer data or
     *             "gyro" for gyroscope data
     * @param data the data from the specified sensor. For 3-axis sensors such as the
     *             accelerometer or gyroscope, the data should be ordered as
     *             {x-axis, y-axis, z-axis}
     * @return the formatted string
     */
    public static String formatAncillaryData(@NonNull String type, @NonNull int[] data) {
        StringBuilder sb = new StringBuilder(type);
        sb.append('=');
        sb.append(data[0]);
        for (int i = 1; i < data.length; i++) {
            sb.append(',');
            sb.append(data[i]);
        }
        return sb.toString();
    }

    /**
     * Constructs a new instance of this class. Only one instance should exist at a time.
     * Initializes both TCP and UDP sockets but does not make any attempt to log in.
     *
     * @throws IOException if an IO error occurs while creating sockets
     */
    private IdearNetworking() throws IOException {
        //Log.d("CREATE", serverAddr.getHostAddress());
        tcpLock = new Semaphore(1);
        readyLock = new Semaphore(1);
        try {
            readyLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        loginToken = new byte[32];
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        loggedIn = false;
        requestNum = 0;
        udpHandler = null;
        this.udpListenerThread = null;
    }

    @Override
    public void run() {
        try {
            Log.d("CREATE", "Entering try catch.");
            this.tcpSocket = new Socket("project-treytech.com", tcpPort);
            Log.d("CREATE", "Socket created.");
            tcpSocket.setKeepAlive(true);
            this.udpSocket = new DatagramSocket();
            Log.d("CREATE", "Set udpSocket.");
            //udpSocket.connect("project-treytech.com", udpPort);
        }catch (IOException e){

        }
        Looper.prepare();
        Log.d("CREATE", "Looper prepared.");
        Looper looper = Looper.myLooper();
        tcpThreadHandler = Handler.createAsync(looper, new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                Bundle b = msg.getData();
                String type = b.getString("type");
                Log.d("CREATE", "HandleMessage received " + type);
                switch (type) {
                    case "image":
                        Log.d("CREATE", "Switch statement entered for image.");
                        IdearResponse response = sendImageTCP_internal(b.getString("image"), b.getBoolean("audio"), b.getString("ancillary"));
                        Log.d("CREATE", "Sending TCP...");
                        Log.d("CREATE", "Got response " + String.valueOf(response));
                        if (response == null) break;
                        if (response.getImageText().isEmpty()) break;
                        Log.d("CREATE" ,"Response received = " + response.imageText);
                        Settings.getInstance().setText(response.imageText);
                        Message ttsMsg = new Message();
                        Bundle imageBundle = new Bundle();
                        b.putString("type", "response");
                        b.putSerializable("response", response);
                        ttsMsg.setAsynchronous(true);
                        ttsMsg.setData(b);
                        //send message to TTS handler
                        break;
                    case "login":
                        int loginStatus = login_internal(b.getString("email"), b.getString("password"));
                        Message loginMsg = new Message();
                        Bundle loginBundle = new Bundle();
                        loginBundle.putString("type", "loginStatus");
                        loginBundle.putInt("status", loginStatus);
                        loginMsg.setAsynchronous(true);
                        loginMsg.setData(loginBundle);
                        //send message to main thread
                        break;
                    case "register":
                        int registerStatus = createAccount_internal(b.getString("email"), b.getString("password"));
                        Message registerMsg = new Message();
                        Bundle registerBundle = new Bundle();
                        registerBundle.putString("type", "loginStatus");
                        registerBundle.putInt("status", registerStatus);
                        registerMsg.setAsynchronous(true);
                        registerMsg.setData(registerBundle);
                        //send message to main thread
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
        Log.d("CREATE", "Done attempting to create handler.");
        if(tcpThreadHandler == null) {
            Log.d("CREATE", "Unable to create networking thread handler");
        }
        readyLock.release();
        Looper.loop();
    }

    /**
     * Logs into the Idear server with the given email address and password. If successful,
     * this method returns 0. If unsuccessful, one of the following values will be returned:
     * <ul>
     * <li>1 - indicates that the given email address was not found in the user database</li>
     * <li>2 - indicates that the password was incorrect for the given email address</li>
     * <li>5 - indicates that an IO exception occurred, most likely the server is offline or
     * unreachable</li>
     * <li>10 - indicates that a userless login attempt failed</li>
     * <li>20 - indicates that the method received a malformed response from the server</li>
     * <li>-1 - indicates that an unknown exception occurred, likely due to a bug</li>
     * </ul>
     * Development purposes only: if both email and password are null, and userlessLogin is true,
     * then this method attempts to perform a login without a user account. If userlessLogin is
     * false, or if only one argument is null, this method will throw an IllegalArgumentException.
     *
     * @param email
     * @param password
     * @return 0 if successful, or one of the above values if unsuccessful
     */
    private int login_internal(String email, String password) {
        if (loggedIn) {
            throw new IllegalStateException("Already logged in");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        } else if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        try {
            tcpLock.acquire();

            OutputStream tcpOut = tcpSocket.getOutputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            InputStream tcpIn = tcpSocket.getInputStream();
            DataInputStream dis = new DataInputStream(tcpIn);
            Log.d("CREATE", "Starting Login.");
            //send email address
            dos.writeByte((byte) 'l'); //this is stage 1 of logging in
            dos.writeUTF(email); //this also writes the string length as a short
            dos.flush();
            tcpOut.write(baos.toByteArray());
            tcpOut.flush();
            baos.reset();
            Log.d("CREATE", "Ending login.");

            Log.d("CREATE", "Starting password salt.");
            //read password salt

            int responseType = dis.readUnsignedByte();
            Log.d("CREATE", "Response type ="+responseType);
            if (responseType != 'l') {
                Log.d("CREATE", "Incorrect response, abort!");
                //did not receive correct response from server
                tcpLock.release();
                return 20;
            }
            int response = dis.readUnsignedByte();
            if (response == 'f') {
                //email address not found in database
                Log.d("CREATE", "No email found, abort!");
                tcpLock.release();
                return 1;
            } else if (response != 's') {
                //bad response from server, aborting
                Log.d("CREATE", "bad response, abort!");
                tcpLock.release();
                return 20;
            }
            Log.d("CREATE", "Response = " + dis.readUnsignedShort());
            byte[] b = new byte[16];
            dis.read(b);
            String salt = new String(b, Charset.forName("UTF-8"));
            //String salt = dis.readUTF();
            //String salt = "";
            Log.d("CREATE", "ending password salt.");

            Log.d("CREATE", "Starting password hash.");
            //construct password hash and send to server
            dos.write('L');
            dos.writeUTF(email);
            md.update(salt.getBytes());
            dos.write(md.digest(password.getBytes()));
            tcpOut.write(baos.toByteArray());
            tcpOut.flush();
            baos.reset();
            Log.d("CREATE", "Ending password hash.");

            Log.d("CREATE", "Starting server response.");
            //read response from server
            responseType = dis.readUnsignedByte();
            Log.d("CREATE", "ResponseType = " + responseType);
            if (responseType != 'L') {
                tcpLock.release();
                return 20;
            }
            response = dis.readUnsignedByte();
            Log.d("CREATE", "Response = " + response);

            if (response == 'f') {
                //password was incorrect
                tcpLock.release();
                return 2;
            } else if (response != 's') {
                //bad response from server, aborting
                tcpLock.release();
                return 20;
            }
            dis.readFully(loginToken);
            tcpLock.release();
            loggedIn = true;
            Log.d("CREATE", "We have successfully logged in. Huzzah!");
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            tcpLock.release();
            return 5;
        } catch (InterruptedException e) {
            e.printStackTrace();
            tcpLock.release();
            return -1;
        } catch (Exception e) {
            tcpLock.release();
            throw e;
        }
    }

    /**
     * Registers a new account the Idear server with the given email address and password. If
     * successful, this method returns 0. If unsuccessful, one of the following values will be
     * returned:
     * <ul>
     * <li>1 - indicates that the given email address is already registered in the user database</li>
     * <li>5 - indicates that an IO exception occurred, most likely the server is offline or
     * unreachable</li>
     * <li>20 - indicates that the method received a malformed response from the server</li>
     * <li>-1 - indicates that an unknown exception occurred, likely due to a bug</li>
     * </ul>
     *
     * @param email
     * @param password
     * @return 0 if successful, or one of the above values if unsuccessful
     */
    private int createAccount_internal(@NonNull String email, @NonNull String password) {
        if (loggedIn) {
            throw new IllegalStateException("Already logged in");
        }
        try {
            tcpLock.acquire();

            OutputStream tcpOut = tcpSocket.getOutputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            //send email address
            dos.writeByte((byte) 'n'); //this is stage 1 of logging in
            dos.writeUTF(email); //this also writes the string length as a short
            dos.flush();
            tcpOut.write(baos.toByteArray());
            tcpOut.flush();
            baos.reset();

            //read password salt
            InputStream tcpIn = tcpSocket.getInputStream();
            DataInputStream dis = new DataInputStream(tcpIn);
            int responseType = dis.readUnsignedByte();
            if (responseType != 'n') {
                //did not receive correct response from server
                tcpLock.release();
                return 20;
            }
            int response = dis.readUnsignedByte();
            if (response == 'f') {
                //email address already exists in database
                tcpLock.release();
                return 1;
            } else if (response != 's') {
                //bad response from server, aborting
                tcpLock.release();
                return 20;
            }
            String salt = dis.readUTF();

            //construct password hash and send to server
            dos.write('N');
            dos.writeUTF(email);
            md.update(salt.getBytes());
            dos.write(md.digest(password.getBytes()));
            tcpOut.write(baos.toByteArray());
            tcpOut.flush();
            baos.reset();

            //read response from server
            responseType = dis.readUnsignedByte();
            if (responseType != 'N') {
                tcpLock.release();
                return 20;
            }
            response = dis.readUnsignedByte();
            if (response == 'f') {
                //???????
                tcpLock.release();
                return -1;
            } else if (response != 's') {
                //bad response from server, aborting
                tcpLock.release();
                return 20;
            }
            dis.readFully(loginToken);
            tcpLock.release();
            loggedIn = true;
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            tcpLock.release();
            return 5;
        } catch (InterruptedException e) {
            e.printStackTrace();
            tcpLock.release();
            return -1;
        } catch (Exception e) {
            tcpLock.release();
            throw e;
        }
    }

    /**
     * Closes all open sockets and cleans up any resources held by this class. Once this method is
     * called this class instance can no longer be used.
     */
    private void cleanup() {
        try {
            boolean lockSuccess = tcpLock.tryAcquire(3, TimeUnit.SECONDS);
            tcpSocket.close();
            if (lockSuccess) {
                tcpLock.release();
            }
        } catch (IOException ioe) {
            System.err.println("IOException occured while closing TCP socket:");
            ioe.printStackTrace(System.err);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (udpListenerThread != null) {
            udpListenerThread.interrupt();
        }
        udpSocket.close();
    }

    private void waitForReady() {
        try {
            readyLock.acquire();
            readyLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a new image to the server for processing using TCP. This method is mostly guaranteed to
     * get a response from the server, but will likely be slower than UDP.
     *
     * @param image         the image to be processed
     * @param requestAudio  if set to true, this method will request that the server handle the
     *                      conversion to audio
     * @param ancillaryData string containing any gyro, accelerometer, or other sensor data to aid
     *                      in the processing of the image. May be null or an empty string.
     * @return an IdearResponse instance containing the text and, if requestAudio was set to true,
     * audio returned by the Idear server, or null if an error occurred
     * @throws IllegalStateException if not logged in or if the cleanup() method has been called
     * @throws IOException
     */
    private IdearResponse sendImageTCP_internal(@NonNull String image, boolean requestAudio, String ancillaryData) {
        if (ancillaryData == null) {
            ancillaryData = "";
        }
        if (!loggedIn) {
            throw new IllegalStateException("Must be logged in to send images");
        }
        if (tcpSocket.isClosed() || udpSocket.isClosed()) {
            throw new IllegalStateException("Sockets have been closed, cannot send data");
        }
        if (ancillaryData.length() >= Short.MAX_VALUE) {
            throw new IllegalArgumentException("Ancillary data is too large");
        }
        try {
            //convert image to PNG
            Path imagePath = Paths.get(image);
            Log.d("CREATE", "Image file is " + Files.size(imagePath) + " bytes");
            byte[] imageBuffer = Files.readAllBytes(imagePath);

            tcpLock.acquire();

            OutputStream tcpOut = tcpSocket.getOutputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeByte((byte) (requestAudio ? 'a' : 't'));
            dos.write(loginToken);
            dos.writeInt(requestNum++);
            dos.writeUTF(ancillaryData);
            Log.d("CREATE", ""+imageBuffer.length);
            dos.writeInt(imageBuffer.length);
            dos.write(imageBuffer);
            dos.flush();
            Log.d("CREATE", "dos successfully flushed.");
            tcpOut.write(baos.toByteArray());
            tcpOut.flush();
            Log.d("CREATE", "tcpOut successfully flushed.");

            InputStream tcpIn = tcpSocket.getInputStream();
            DataInputStream dis = new DataInputStream(tcpIn);
            int responseType = dis.readUnsignedByte();
            if (responseType != (requestAudio ? 'a' : 't')) {
                tcpLock.release();
                return null;
            }
            int responseNum = dis.readInt();
            if (responseNum != (requestNum - 1)) { //have to subtract one since we already incremented requestNum
                tcpLock.release();
                return null;
            }
            //Log.d("CREATE", "Response length = " + dis.readUnsignedShort());
            int responseLength = dis.readUnsignedShort();
            byte[] responseBytes = new byte[responseLength];
            dis.read(responseBytes);
            Log.d("CREATE", Arrays.toString(responseBytes));
            String transcribedText = new String(responseBytes, Charset.forName("UTF-8"));
            IdearResponse response;
            if (requestAudio) {
                int audioSize = dis.readInt();
                byte[] audioBuffer = new byte[audioSize];
                dis.readFully(audioBuffer);
                response = new IdearResponse(transcribedText, audioBuffer);
            } else {
                response = new IdearResponse(transcribedText);
            }
            tcpLock.release();
            return response;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            //ioe.printStackTrace();
            Log.d("CREATE", ioe.getMessage());
        }
        tcpLock.release();
        return null;
    }



    public static void login(String email, String password) {
        Message m = tcpThreadHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("type", "login");
        b.putString("email", email);
        b.putString("password", password);
        m.setAsynchronous(true);
        m.setData(b);
        tcpThreadHandler.sendMessage(m);
    }

    public static void createAccount(@NonNull String email, @NonNull String password) {
        Message m = tcpThreadHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("type", "register");
        b.putString("email", email);
        b.putString("password", password);
        m.setAsynchronous(true);
        m.setData(b);
        tcpThreadHandler.sendMessage(m);
    }

    public static void sendImageTCP(@NonNull String image, boolean requestAudio, String ancillaryData) {
        //send message to thread handler
        Message m = tcpThreadHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("type", "image");
        b.putString("image", image);
        b.putBoolean("audio", requestAudio);
        b.putString("ancillary", ancillaryData);
        m.setData(b);
        m.setAsynchronous(true);
        tcpThreadHandler.sendMessage(m);
    }

    /**
     * Sends a new image to the server for processing using TCP without any ancillary data. This
     * method is simply a shorthand for the three-argument sendImageTCP with ancillary data set
     * to an empty string.
     *
     * @param image        the image to be processed
     * @param requestAudio if set to true, this method will request that the server handle the
     *                     conversion to audio
     * @return an IdearResponse instance containing the text and, if requestAudio was set to true,
     * audio returned by the Idear server
     * @throws IllegalStateException if not logged in or if the cleanup() method has been called
     * @throws IOException
     */
    public static void sendImageTCP(@NonNull String image, boolean requestAudio) throws IOException {
        sendImageTCP(image, requestAudio, "");
    }

}
package com.example.idear;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import androidx.annotation.NonNull;

public final class IdearNetworking {
    private static InetAddress serverAddr = null;
    private static final int tcpPort = 30501;
    private static final int udpPort = 30501;
    public static final boolean userlessLoginEnabled = true;

    static {
        Thread dnsThread = new Thread(){
            @Override
            public void run() {
                InetAddress temp = null;
                try {
                    temp = InetAddress.getByName("project-treytech.com");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                IdearNetworking.serverAddr = temp;
            }
        };
        dnsThread.start();
        try {
            dnsThread.join();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private final Socket tcpSocket;
    private final Semaphore tcpLock;
    private final DatagramSocket udpSocket;
    private UDPResponseHandler udpHandler;
    private Thread udpListenerThread;

    private final MessageDigest md;

    private boolean loggedIn;
    private final byte[] loginToken;
    private int requestNum;

    /**
     * Class to represent a single response from the Idear server.
     */
    public final class IdearResponse {
        private final char responseType;
        private final String imageText;
        private final Object imageAudio;
        private final Rect cropDimensions;

        private IdearResponse(String text, Object audio) {
            if (text == null) {
                throw new IllegalArgumentException("Response text cannot be null");
            }
            this.responseType = audio == null ? 't' : 'a';
            this.imageText = text;
            this.imageAudio = audio;
            this.cropDimensions = null;
        }

        private IdearResponse(String text) {
            this(text, null);
        }

        private IdearResponse(Rect cropDim) {
            if(cropDim == null) {
                throw new IllegalArgumentException("Crop dimensions cannot be null");
            }
            this.responseType = 'c';
            this.cropDimensions = cropDim;
            this.imageText = null;
            this.imageAudio = null;
        }

        public char getResponseType() {
            return responseType;
        }

        public String getImageText() {
            return imageText;
        }

        public Object getImageAudio() {
            return imageAudio;
        }

        public Rect getCropDimensions() {
            return cropDimensions;
        }
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
         * @param response The IdearResponse class instance constructed from the server's response
         */
        public abstract void handle(IdearResponse response);

    }

    private final class UDPListener implements Runnable {

        private final UDPResponseHandler responseHandler;

        private UDPListener(UDPResponseHandler handler) {
            if(handler == null) {
                throw new IllegalArgumentException("Handler cannot be null");
            }
            this.responseHandler = handler;
        }

        @Override
        public void run() {

            while(true) {

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
    public IdearNetworking() throws IOException {
        this.tcpSocket = new Socket(serverAddr, tcpPort);
        tcpSocket.setKeepAlive(true);
        tcpLock = new Semaphore(1);
        loginToken = new byte[32];
        this.udpSocket = new DatagramSocket();
        udpSocket.connect(serverAddr, udpPort);
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

    private boolean loginNoUser() {
        throw new UnsupportedOperationException("Not implemented yet");
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
    public int login(String email, String password) {
        if (loggedIn) {
            throw new IllegalStateException("Already logged in");
        }
        if (!userlessLoginEnabled) {
            if (email == null) {
                throw new IllegalArgumentException("Email cannot be null");
            } else if (password == null) {
                throw new IllegalArgumentException("Password cannot be null");
            }
        } else {
            if (email == null) {
                if (password != null) {
                    throw new IllegalArgumentException("Both email and password must be null for userless login");
                }
                if (loginNoUser()) {
                    loggedIn = true;
                    return 0;
                }
            }
        }
        try {
            tcpLock.acquire();

            OutputStream tcpOut = tcpSocket.getOutputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            //send email address
            dos.writeByte((byte) 'l'); //this is stage 1 of logging in
            dos.writeUTF(email); //this also writes the string length as a short
            dos.flush();
            tcpOut.write(baos.toByteArray());
            tcpOut.flush();
            baos.reset();

            //read password salt
            InputStream tcpIn = tcpSocket.getInputStream();
            DataInputStream dis = new DataInputStream(tcpIn);
            int responseType = dis.readUnsignedByte();
            if (responseType != 'l') {
                //did not receive correct response from server
                tcpLock.release();
                return 20;
            }
            int response = dis.readUnsignedByte();
            if (response == 'f') {
                //email address not found in database
                tcpLock.release();
                return 1;
            } else if (response != 's') {
                //bad response from server, aborting
                tcpLock.release();
                return 20;
            }
            String salt = dis.readUTF();

            //construct password hash and send to server
            dos.write('L');
            dos.writeUTF(email);
            md.update(salt.getBytes());
            dos.write(md.digest(password.getBytes()));
            tcpOut.write(baos.toByteArray());
            tcpOut.flush();
            baos.reset();

            //read response from server
            responseType = dis.readUnsignedByte();
            if (responseType != 'L') {
                tcpLock.release();
                return 20;
            }
            response = dis.readUnsignedByte();

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
    public int createAccount(@NonNull String email, @NonNull String password) {
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

    private void startUDPListener() {

        throw new UnsupportedOperationException();
    }

    /**
     * Closes all open sockets and cleans up any resources held by this class. Once this method is
     * called this class instance can no longer be used.
     */
    public void cleanup() {
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
        if(udpListenerThread != null){
            udpListenerThread.interrupt();
        }
        udpSocket.close();
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
    public IdearResponse sendImageTCP(@NonNull Bitmap image, boolean requestAudio, String ancillaryData) throws IOException {
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
            ByteArrayOutputStream imageBuffer = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 0, imageBuffer);

            tcpLock.acquire();

            OutputStream tcpOut = tcpSocket.getOutputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeByte((byte) (requestAudio?'a':'t'));
            dos.write(loginToken);
            dos.writeInt(requestNum++);
            dos.writeUTF(ancillaryData);
            dos.writeInt(imageBuffer.size());
            dos.write(imageBuffer.toByteArray());
            dos.flush();
            tcpOut.write(baos.toByteArray());
            tcpOut.flush();

            InputStream tcpIn = tcpSocket.getInputStream();
            DataInputStream dis = new DataInputStream(tcpIn);
            int responseType = dis.readUnsignedByte();
            if (responseType != (requestAudio?'a':'t')){
                tcpLock.release();
                return null;
            }
            int responseNum = dis.readInt();
            if(responseNum != (requestNum-1)){ //have to subtract one since we already incremented requestNum
                tcpLock.release();
                return null;
            }
            String transcribedText = dis.readUTF();
            IdearResponse response;
            if(requestAudio){
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
        }
        return null;
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
    public IdearResponse sendImageTCP(@NonNull Bitmap image, boolean requestAudio) throws IOException {
        return sendImageTCP(image, requestAudio, "");
    }

    /**
     * Sends a new image to the server for processing using UDP. This method is not guaranteed to
     * get a response from the server, but will likely get faster network performance than TCP.
     * Because a response is not guaranteed, this method returns nothing and instead passes
     * responses to the UDPResponseHandler registered with the registerUDPHandler() method.
     *
     * @param image        the image to be processed
     * @param requestAudio if set to true, this method will request that the server handle the
     *                     conversion to audio
     * @throws IllegalStateException if not logged in or if the cleanup() method has been called
     * @throws IOException
     */
    public void sendImageUDP(@NonNull Bitmap image, boolean requestAudio) throws IOException {
        if (!loggedIn) {
            throw new IllegalStateException("Must be logged in to send images");
        }
        if (tcpSocket.isClosed() || udpSocket.isClosed()) {
            throw new IllegalStateException("Sockets have been closed, cannot send data");
        }
        if(udpListenerThread == null) {
            throw new IllegalStateException("No UDP listener running, ");
        }
        throw new UnsupportedOperationException("Sending images over UDP is not yet implemented");
    }

    /**
     * Resisters a request handler to take responses from UDP requests and starts a thread that
     * listens for UDP messages from the server. Because UDP does not guarantee message delivery,
     * there may not be a response for every request. There may also be more responses than
     * requests, since the server may send an image cropping message in addition to a text
     * transcription in response to a single request.
     *
     * @param handler
     */
    public void registerUDPHandler(@NonNull UDPResponseHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("UDP response handler cannot be null");
        }
        this.udpHandler = handler;
        if(this.udpListenerThread != null) {
            this.udpListenerThread.interrupt();
            while(!udpListenerThread.isInterrupted()){}
        }
        this.udpListenerThread = new Thread(new UDPListener(handler));
        this.udpListenerThread.start();
    }

}

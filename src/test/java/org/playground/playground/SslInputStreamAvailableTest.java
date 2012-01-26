// ========================================================================
// Copyright (c) 2009-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================

package org.playground.playground;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.junit.Before;
import org.junit.Test;

/* ------------------------------------------------------------ */
/**
 */
public class SslInputStreamAvailableTest
{
    private static final char[] PASS = "test123".toCharArray();
    private SSLServerSocketFactory serverSocketFactory;
    private SSLSocketFactory socketFactory;
    private SSLContext serverSslContext;
    private ServerSocket serverSocket;
    private Socket clientSocket;

    /* ------------------------------------------------------------ */
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        System.setProperty("javax.net.ssl.trustStore","src/test/resources/truststore");
        
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream keystoreInputStream = getClass().getClassLoader().getResourceAsStream("keystore.jks");
        ks.load(keystoreInputStream,PASS);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks,PASS);
        
        serverSslContext = SSLContext.getInstance("TLS");
        serverSslContext.init(kmf.getKeyManagers(), null, null);
        
        serverSocketFactory = serverSslContext.getServerSocketFactory();
        socketFactory = serverSslContext.getSocketFactory();
        serverSocket = serverSocketFactory.createServerSocket(0);
        clientSocket = socketFactory.createSocket("127.0.0.1",serverSocket.getLocalPort());
        
        new Thread(new Server()).start();
    }

    @Test
    public void test() throws IOException, NoSuchAlgorithmException
    {

        OutputStream os = clientSocket.getOutputStream();
        os.write("1234567890".getBytes());
        os.flush();
        
        os.write("abcdefghijklmnopqrst".getBytes());
        os.flush();
        
    }

    class Server implements Runnable
    {

        @Override
        public void run()
        {
            Socket connection = null;
            InputStream is = null;
            OutputStream os = null;
            try
            {
                System.out.println("Waiting for connection");
                connection = serverSocket.accept();
                System.out.println("Connection from: " + connection.getInetAddress() + ":" + connection.getPort());
                is = connection.getInputStream();
                os = connection.getOutputStream();

                int j=0;
                while (true)
                {
                    System.out.println("Iteration: " + j++ +"\n");
                    
                    int available = is.available();
                    System.out.println("avail: " + available);
                    StringBuffer buf = new StringBuffer();
                    char read = (char)is.read();
                    buf.append(read);
                    System.out.println("read this byte individually: " + read);
                    
                    available = is.available();
                    System.out.println("avail: " + available);
                    
                    if(available>0)
                        for(int i=0;i<available;i++)
                            buf.append((char)is.read());
                    
                    System.out.println("Buffer after we read all available bytes" + buf);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    is.close();
                    os.close();
                    connection.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

}

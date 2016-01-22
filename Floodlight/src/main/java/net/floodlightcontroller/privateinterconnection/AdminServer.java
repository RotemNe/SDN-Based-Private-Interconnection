package net.floodlightcontroller.privateinterconnection;


import java.net.*;
import java.io.*;

public class AdminServer implements Runnable 
{
	private ServerSocket serverSocket;
	private ICallBack callback;

	public AdminServer(int port,ICallBack callback)
	{
		try 
		{
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(500000);
			this.callback = callback;

		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}	
	}
	public void run() 
	{
		while (true) 
		{
			try 
			{
				System.out.println("Waiting for client on port "+ serverSocket.getLocalPort() + "...");				
				Socket server = serverSocket.accept();
				System.out.println("Just connected to "+ server.getRemoteSocketAddress());

				InputStream is = server.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String rcvMsg = br.readLine();
				System.out.println("Message received from client is " + rcvMsg);
				String[] reciveMsg = rcvMsg.split(" ");

				String ipSrc = reciveMsg[0];
				String ipDest = reciveMsg[1];
				System.out.println("IP source :" + ipSrc + "\n" + "IP dest : "+ ipDest);

				String mincut = callback.privateInterConnectionHandler(ipSrc,ipDest);

				DataOutputStream out = new DataOutputStream(server.getOutputStream());
				String outMsg = "\nNumber Edges In Minimun Cut-Set is: "+ mincut + 
						"\nThank you for connecting to " + server.getLocalSocketAddress() + 
						"\nGoodbye!";
				out.writeUTF(outMsg);
				server.close();
				return;

			} 
			catch (SocketTimeoutException s)
			{
				System.out.println("Socket timed out!");
				break;
			} 
			catch (IOException e)
			{
				e.printStackTrace();
				break;
			}
		}
	}

}

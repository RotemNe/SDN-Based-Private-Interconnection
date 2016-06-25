package net.floodlightcontroller.privateinterconnection;


import java.net.*;
import java.util.Map;
import java.io.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.net.Socket;

public class AdminServer implements Runnable 
{
	private ServerSocket serverSocket;
	private ICallBack callback;
	private final Lock lock = new ReentrantLock();

	public AdminServer(int port,ICallBack callback)
	{
		try 
		{
			serverSocket = new ServerSocket(port);
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
				new Thread(new MultiThreadServer(server,lock,callback)).start();


			} 
			catch (IOException e)
			{
				e.printStackTrace();
				break;
			}
		}
	}
	private class MultiThreadServer implements Runnable 
	{
	  private Socket csocket;
	  private  Lock lock;
	  private ICallBack callback;
	  
	  MultiThreadServer(Socket csocket, Lock lock ,ICallBack callback)
      {
		      this.csocket = csocket;
		      this.lock = lock;
		      this.callback = callback;
	  }
		@Override
		public void run() 
		{
			try 
				{
				InputStream is = csocket.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String rcvMsg = br.readLine();
				System.out.println("Message received from client is " + rcvMsg);
				String[] reciveMsg = rcvMsg.split(" ");
				Map<String, String> vlanToPrivateIPv4;
				String mincut;
				String ipSrc = reciveMsg[0];
				String ipDest = reciveMsg[1];
				System.out.println("IP source :" + ipSrc + "\n" + "IP dest : "+ ipDest);
				
				lock.lock();
	
				mincut = callback.privateInterConnectionHandler(ipSrc,ipDest);
				vlanToPrivateIPv4 = PrivateInterConnectionMng.getVlanToPrivateIPv4Map();

			    
				lock.unlock();
				
				DataOutputStream out = new DataOutputStream(csocket.getOutputStream());
				String outMsg = "\nNumber Edges In Minimun Cut Set is -"+ mincut + "\n";				
				for(String vlanNum : vlanToPrivateIPv4.keySet())
				{
					outMsg += vlanNum + "-" + vlanToPrivateIPv4.get(vlanNum)+ "\n";
				}
				
				
				int msgLength = outMsg.length();
				out.writeInt(msgLength);
				out.write(outMsg.getBytes());
				System.out.println("LENGTH: "+outMsg.length());
				System.out.println("DATA: "+outMsg);
				csocket.close();
			    PrivateInterConnectionMng.clearData();
				return;
			}
			catch (IOException e)
			{
				e.printStackTrace();
				
			}	
		}
	}
}

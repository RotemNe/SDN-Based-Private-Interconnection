
#!/usr/bin/python
import socket
import sys
import os
import struct
import select
import commands
from Tkinter import *
from time import sleep
import tkMessageBox
from secretsharing import PlaintextToHexSecretSharer
from ss import threshold_scheme as SS

"""
- Manage & creates SharesData instances.
- Maintain SharesData instance for each actual IP - <actual IP, SharesData instance>.
- Maintain virtual(= Private) IPv4 address for each actual IP - <Virtual IPv4 address, actual IP>.
- Handling data received from source host by initial processing and transfer further handling to adapt SharesData instance.
- Handling clearing of adapt dictionary entries when get notify about closed connection.  
"""
class SharesMng():

    def __init__(self,refConIp,refText,refFileData,refSharesData,refProg):
      self.shares_data_dict = {}
      self.privateIPv4_to_actual_ip = {}
      self.refConIp = refConIp
      self.refText = refText
      self.refFileData = refFileData
      self.refSharesData = refSharesData
      self.refFileProg = refProg

"""
  Method gets actual IP of given senderIP from internal maintained data and invoke remove() 
  for SharesData instance that match that actual IP. 
"""
    def remove(self,peerIp):
        actualIp = self.privateIPv4_to_actual_ip[peerIp]
        retVal = self.shares_data_dict[actualIp].remove(peerIp)
        if retVal == True:
           del  self.privateIPv4_to_actual_ip[peerIp]
           del self.shares_data_dict[actualIp]
           self.refConIp.set("")
           self.refSharesData.set("")
"""
  Method identify input data type and invokes adapt handler according it.
"""
    def handle(self,data, sender_ip):
      if "Hello" in data:
        return self._handle_hello(data,sender_ip)
      else:
        self._handle_share(data,sender_ip)

    def _handle_share(self,data,sender_ip):
        if self.privateIPv4_to_actual_ip.has_key(sender_ip):
            actual_ip = self.privateIPv4_to_actual_ip[sender_ip]
            self.shares_data_dict[actual_ip].add_share(data,sender_ip)

    def _handle_hello(self,data,sender_ip):
        parsed_hello = data.split('-')
        actual_ip = parsed_hello[1]
        private_ip = sender_ip
        threshold = parsed_hello[2]
        shares_num = parsed_hello[3]
        shares_range = parsed_hello[4]
        data_type = parsed_hello[5]
        file_size = None
        file_name = None

        if data_type == "File":
            file_size = parsed_hello[7]
            file_name = parsed_hello[6]

        # Update private IPv4  recived ip belongs to actual ip
        self.privateIPv4_to_actual_ip[sender_ip] = actual_ip

        if self.shares_data_dict.has_key(actual_ip):
            self.shares_data_dict[actual_ip].update(sender_ip,data_type,file_size,file_name,threshold,shares_num,shares_range)
            sharesDataStr = "Shares Number: " + shares_num + "\nThreshold:  " + threshold
            self.refSharesData.set(sharesDataStr)

        else:
            shares_data = SharesData(actual_ip, threshold,sender_ip,shares_num,self.refText,self.refFileProg,shares_range)
            self.shares_data_dict[actual_ip] = shares_data
            shares_data.data_type = data_type
            shares_data.file_name = file_name
            shares_data.file_size = file_size
            shares_data.refFileData = self.refFileData

            connectionStr = "Got Connection From: "+ actual_ip
            self.refConIp.set(connectionStr)
            sharesDataStr = "Shares Number: " + shares_num + "\nThreshold:  " + threshold
            self.refSharesData.set(sharesDataStr)

        if (self.shares_data_dict[actual_ip].file == None) and (data_type == "File") :
            self.shares_data_dict[actual_ip].create_file()
            self.refFileProg["value"] = 0
            self.refFileProg["maximum"] = int(file_size)
            self.refFileProg["length"] = int(file_size)

"""
-  Maintain shares from many virtual/Private IPv4 address which associated with one actual IP address.
-  Maintain list of virtual IPv4 which associated with actual_ip member.
-  Recover the data from shares when possible.

"""
class SharesData():
    def __init__(self,actual_ip,threshold,private_ip,shares_num,refText,refFileProg,shares_range):
      self.actual_ip = actual_ip
      self.threshold = threshold
      self.shares_list = []
      self.private_ip_list = []
      self.private_ip_list.append(private_ip)
      self.cur_shares = 0
      self.shares_num = shares_num
      self.data_recovered = ""
      self.refText = refText
      self.data_type = None
      self.file_name = None
      self.file_size = None
      self.file = None
      self.cur_file_size = 0
      self.refFileData = None
      self.refFileProg = refFileProg
      self.ip_to_shares_range = {}
      self.ip_to_shares = {}
      self.ip_to_cur_chunk = {}
      self.cur_chunk_to_recover = 0
      self.init_private_ip_data(private_ip,shares_range)

    def init_private_ip_data(self,private_ip,shares_range):

      self.ip_to_shares_range[str(private_ip)] = shares_range
      self.ip_to_shares[str(private_ip)] = {}
      self.ip_to_cur_chunk[str(private_ip)] = 0
      self.cur_chunk_to_recover = 0


    def create_file(self):
        self.file = open("filesReceived/" + self.file_name, "wb")
        self.cur_file_size = 0
        self.refFileData.set("Reciving File ... ")

    def _clear_file(self):
        self.file.close()
        fileDataStr = "You Recived A File  \n FileName: "+ str(self.file_name) + "\n File Size: " + str(self.cur_file_size) + "\n File Path: " +"filesReceived/" +self.file_name
        tkMessageBox.showinfo(" File Recived !",fileDataStr)
        self.refFileData.set(fileDataStr)
        print "Finish File ->  fileName "+ str(self.file_name)+"fileSize - curFileSize" +  str(self.cur_file_size)+str(self.file_size)
        self.file = None
        self.cur_file_size = 0
        self.file_size = None
        self.file_name = None

"""
Updates internal data with given data.
"""
    def update(self,sender_ip,data_type,file_size,file_name,threshold,shares_num,shares_range):
      if sender_ip not in self.private_ip_list:
          self.private_ip_list.append(sender_ip)

      self.data_type = data_type
      self.file_name = file_name
      self.file_size = file_size
      self.shares_num = shares_num
      self.threshold = threshold
      self.shares_range = shares_range
      self.init_private_ip_data(sender_ip,shares_range)
"""
  - Removes sender_IP from internal list about virtual IPv4 address.
  - Returns True when internal list about virtual IPv4 address is empty, otherwise False.
"""
    def remove(self,sender_ip):
      self.private_ip_list.remove(sender_ip)
      if len(self.private_ip_list) == 0:
        self.refText.configure(state = "normal")
        self.refText.delete('1.0',END)
        self.refFileData.set("")
        return True


    def _add_share(self,share,sender_ip):

      shares_range = self.ip_to_shares_range[str(sender_ip)]
      sender_ip_cur_chunk = int(self.ip_to_cur_chunk[str(sender_ip)])
      self._create_new_chunk_to_ip(sender_ip,sender_ip_cur_chunk)
      shares_list =self.ip_to_shares[str(sender_ip)][sender_ip_cur_chunk]


      if len(shares_list) < int(shares_range):
          shares_list.append(share)
          if len(shares_list) == int(shares_range):
              self.ip_to_cur_chunk[str(sender_ip)] = int(sender_ip_cur_chunk) + 1

      else:
          self.ip_to_cur_chunk[str(sender_ip)] = int(sender_ip_cur_chunk) + 1
          sender_ip_cur_chunk = self.ip_to_cur_chunk[str(sender_ip)]
          self._create_new_chunk_to_ip(sender_ip,sender_ip_cur_chunk)
          shares_list = self.ip_to_shares[str(sender_ip)][sender_ip_cur_chunk]
          shares_list.append(share)

      ip_to_shares_to_recover = {}
      cur_shares = 0
      recover_index = self.cur_chunk_to_recover
      for ip in self.ip_to_shares:
          if self.ip_to_shares[ip].has_key(int(recover_index)):
              ip_to_shares_to_recover[ip] = self.ip_to_shares[ip][recover_index]
              cur_shares += len(ip_to_shares_to_recover[ip])


      if cur_shares == int(self.threshold):
          shares = []
          for ip in ip_to_shares_to_recover:
              shares += ip_to_shares_to_recover[ip]

          recover_data = SS.recover_secret(shares)

          print recover_data
          self.cur_chunk_to_recover += 1
          temp = self.cur_chunk_to_recover - 1
          for ip in self.ip_to_shares:
              cur_shares_range = self.ip_to_shares_range[ip]
               #--
              if self.ip_to_shares[ip].has_key(int(temp)):
              #--
                  cur_list = self.ip_to_shares[ip][int(temp)]
                  if len(cur_list) == int(cur_shares_range):
                      self.ip_to_shares[ip][int(temp)] = []

          if self.data_type == "Text":
              self.refText.configure(state = "normal")
              self.refText.insert(END , recover_data)
              self.refText.configure(state = "disable")
              self.data_recovered = recover_data

          elif self.data_type == "File":
              self.file.write(recover_data)
              self.file.flush()
              self.cur_file_size = os.fstat(self.file.fileno()).st_size
              self.refFileProg["value"] = 0
              self.refFileProg["value"]+=self.cur_file_size
              if (self.cur_file_size == int(self.file_size)):
                  self._clear_file()

    def _create_new_chunk_to_ip(self,sender_ip,sender_ip_cur_chunk):
        if self.ip_to_shares[str(sender_ip)].has_key(int(sender_ip_cur_chunk)) == False:
            self.ip_to_shares[str(sender_ip)][int(sender_ip_cur_chunk)] = []

"""
 - Add given share to internal shares_list.
 - Recovers data from shares when internal shares_list contains threshold number of shares.
 - Clear internal data about shares_list and shares track when internal shares_list contains shares as number of shares number n.

"""
    def add_share(self,share,sender_ip):

      self._add_share(share,sender_ip)

    def _clear_shares_data(self):
      self.shares_list = []
      self.cur_shares = 0

"""
- TCP Server socket that listening for incoming packets from source host that sent by ClientSender module.
- Listening to incoming packets by the use of select module - monitor sockets until they become readable, get closed or communication error occurs.
- Handles incoming packets from readable socket and closing of sockets communication by the use of SharesMng class.
"""
class ClientReceiver():

    def __init__(self,refConInfo,refText,refFileData,refSharesData,refProg):
      self.port = 12222
      self.backlog = 5
      self.serverIp =self._get_server_ip()
      self.sharesMng = SharesMng(refConInfo,refText,refFileData,refSharesData,refProg)
      self.refConInfo = refConInfo
      self.refText = refText
      self.refSharesData = refSharesData
      self.refFileData = refFileData
      self.inputs = []
      self.isRequestToClose = False

    def _get_server_ip(self):
        ifconfigOutput = commands.getoutput("ifconfig").split()
        for line in ifconfigOutput:
            if "-eth0" in line:
                interface = line
                break
        cmd = "ifconfig " + interface +" | grep 'inet addr' | awk -F: '{print $2}' | awk '{print $1}'"
        serverIp =  commands.getoutput(cmd)
        return serverIp


    def _recvall(self,sock,n):
        data = ''
        while len(data) < n:
          packet = sock.recv(n-len(data))
          if not packet:
            return None
          data += packet
          return data

    def close_all(self):
       self.isRequestToClose = True
       for sock in self.inputs:
           self.inputs.remove(sock)
           sock.close
"""
Method opens TCP server socket and monitor sockets by use of select module:
 - Accept connections from readable sockets.
 - Receives Data from connected readable sockets and transfer data handling to SharesMng class.
 - Identify closing of connected readable sockets and notify SharesMng class about disconnection. 

"""
    def selected_tcp_server(self):

        serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        serverSocket.setblocking(0)
        serverSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR,1)
        serverSocket.bind((self.serverIp,self.port))
        serverSocket.listen(self.backlog)

        # Sockets from which we expect to read
        self.inputs = [serverSocket, sys.stdin]
        # Sockets to which we expect to write 
        outputs = []

        while self.inputs:

            if self.isRequestToClose == True:
                break

            readable, writable, exceptional = select.select(self.inputs, outputs, [])

            # Handle Inputs
            for s in readable:
                if s is serverSocket:
                     # A "readable" server socket is ready to accept a connection
                     (connection, (ip,port)) = serverSocket.accept()
                     print "Connection from : " + ip + " : " +str(port)
                     connection.setblocking(0)
                     self.inputs.append(connection)

                elif s is sys.stdin:
                    #handle standart input
                    junk = sys.stdin.readline()
                    for sock in self.inputs:
                        self.inputs.remove(sock)
                        sock.close
                    break

                else:
                    raw_msglen = self._recvall(s,4)
                    if raw_msglen:
                        msglen = struct.unpack('>I', raw_msglen)[0]
                        data = self._recvall(s, msglen)
                        peerIp= s.getpeername()[0]

                        print data
                        curConIp = self.sharesMng.handle(data, peerIp)

                    else:
                         self.inputs.remove(s)
                         peerIp = s.getpeername()[0]
                         self.sharesMng.remove(peerIp)
                         s.close()

            # Handle execptional conditions
            for s in exceptional:
                self.inputs.remove(s)
                s.close()
            if self.isRequestToClose:
                break


        self.close_all()
        serverSocket.close()

def main():
   selectedServer = SelectedServer()
   selectedServer.selected_tcp_server()

if __name__ == '__main__':
    main()

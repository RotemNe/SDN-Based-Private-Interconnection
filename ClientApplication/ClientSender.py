#!/usr/bin/python
import commands
import socket
import sys
import struct
import threading
from secretsharing import PlaintextToHexSecretSharer

"""
 Subclass of Thread class, overrides the father method run ().
 Maintain shares range and connected socket to send through it the shares. The socket is attached to one of the alias IP interfaces.
 Performs sending range of shares to destination host. 

"""
class sendSharesThread(threading.Thread):

      def __init__(self,clientSocket,shares,sharesNum,ipSendFrom):
          threading.Thread.__init__(self)
          self.clientSocket = clientSocket
          self.shares = shares
          self.sharesNum = sharesNum
          self.ipSendFrom = ipSendFrom

      def run(self):
         if self.sharesNum > 1:
           for share in self.shares:
               msg = struct.pack('>I' ,len(share)) + share
               self.clientSocket.sendall(msg)
               print ('Send Share To Dest from ip '+ self.ipSendFrom +' ' + share +'\n')
         else:
               msg = struct.pack('>I' ,len(self.shares[0])) + self.shares[0]
               self.clientSocket.sendall(msg)
               print ('Send Share To Dest from ip '+ self.ipSendFrom +' ' + self.shares[0] +'\n')


"""
    Represent client side that want to establish private interconnection 
"""
class ClientSender():

    def __init__(self,dest_ip):
        self.dest_ip = dest_ip
        self.src_ip = None
        self.min_cut = 0
        self.vlan_to_ip = {}
        self.secret = None
        self.num_shares = 0
        self.threshold = 0
        self.chunk_size = 0
        self.file_path = ''
        self.vlanToSocket = {}
        self.interface = None
        self.data_type = None
        self.fileName = None
        self.fileSize = None
        self.is_connected = False


    def set_user_input(self,numShares,threshold,chunk):
        self.num_shares = numShares
        self.threshold = threshold
        if chunk > 0:
            self.chunk_size = chunk
        else:
            self.chunk_size = 20

"""
    Validate ration of n,k by t value that returns from network administrator.
    Ratio validation ensures that each alias IP interface sends at most k-1 shares.
    Ration Validate formula: n/k < t . (n/k result is round up)
"""
    def check_ratio(self):
        if self.threshold >0:
            ratio = float(self.num_shares)/float(self.threshold)
            ratio = ratio % 1
            if ratio >= 0.5:
                rem = 1
            else:
                rem = 0
            res = (int(self.num_shares)/int(self.threshold))+rem
            if res < int(self.min_cut):
                return True
        return False
"""
    Perform request to network administrator with source & destination hosts IP’s for
    triggering creation of private interconnection through network switches.
    Get from network administrator : [t, <VlanId ,Virutal IP>] 
"""
    def request_private_interconnection_from_admin(self):

        data = ''
        port = 12345
        admin_ip = '192.168.1.5'
        pingCmd = "ping -c1 "+ self.dest_ip
        self.min_cut = 0
        output = commands.getoutput(pingCmd)
        self.src_ip = self._get_my_ip()

        s = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
        s.connect((admin_ip, port))
        s.sendall(self.src_ip +' ' + self.dest_ip + '\n')

        raw_msglen = self._recvall(s,4)
        if not raw_msglen:
            return None
        msglen = struct.unpack('>I', raw_msglen)[0]
        data = self._recvall(s,msglen)

        lines = data.split('\n')
        if lines[1].split('-')[1] is not None :
            self.min_cut = int(lines[1].split('-')[1])

        for line in lines[2:]:
            split_vlan_ip = line.split('-')
            if len(split_vlan_ip) > 0:
               self. vlan_to_ip[split_vlan_ip[0]] = split_vlan_ip[1]
        s.close

        return (self.min_cut, self.vlan_to_ip)

"""
    Configure alias IP interfaces by data received from requesting private interconnection (vlan_to_ip)
"""
    def config_infra(self):
        ifconfigOutput = commands.getoutput("ifconfig").split()
        for line in ifconfigOutput:
            if "-eth0" in line:
                self.interface = line
                break

        host_interface_name = commands.getoutput("ifconfig -a | sed 's/[ \t].*//;/^\(lo\|\)$/d'")
        for vlan_id in self.vlan_to_ip:
            config_alias_ip_cmd = "ifconfig %s:%s %s up" % (host_interface_name, vlan_id, self.vlan_to_ip[vlan_id])
            commands.getoutput(config_alias_ip_cmd)

"""
    Method execute sending of dataType to destination host by distributing data to byte chunks, 
    creating n shares from each chunk of data and shares sending by the use of threads.
"""
    def send_data(self):

        res =  self._config_connections(self.is_connected)
        if res is False:
            self.is_connected = False
            return False

        self._distribute_shares_to_ranges()

        if self.data_type is "Text":
            self._send_text()

        elif self.data_type is "File":
            self._send_file()

        self.is_connected = True
        return True

"""
 a) Create TCP connections from each alias IP interface to destination host.
    Connections are created only in case they are not existing yet.

 b) Transfer preliminary data, ’Hello Message’, towards sending shares, from each alias IP interface to destination host. 

"""
    def _config_connections(self,isConnected):

        ifconfigOutput = commands.getoutput("ifconfig").split()
        for line in ifconfigOutput:
            if "-eth0" in line:
                self.interface = line
                break

        cmd = "ifconfig " + self.interface +" | grep 'inet addr' | awk -F: '{print $2}' | awk '{print $1}'"
        actualIp =  commands.getoutput(cmd)

        if isConnected == False:
            for vlan_id in self.vlan_to_ip:
                connectedSocket = self._create_socket_connection(self.vlan_to_ip[vlan_id],self.dest_ip,str(self.threshold),actualIp,str(self.num_shares))
                if connectedSocket is None:
                    return False
                self.vlanToSocket[vlan_id] = connectedSocket
        else:
            for vlan_id in self.vlanToSocket:
                self._init_new_send(self.vlanToSocket[vlan_id], actualIp)

        return True

    def _init_new_send(self,connectedSocket,actual_ip):

        helloMsg = "Hello-" +actual_ip +"-" + str(self.threshold) +"-"+str(self.num_shares)+"-"+self.data_type
        if self.data_type is "File":
            helloMsg += "-"+self.fileName +"-"+str(self.fileSize)

        msg = struct.pack('>I' ,len(helloMsg)) + helloMsg
        connectedSocket.sendall(msg)
        print(helloMsg + '\n')


    def _create_socket_connection(self,srcIp,destIp,threshold,actual_ip,numShares):
        destPort = 12222
        clientSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        clientSocket.bind((srcIp,0))
        res = clientSocket.connect_ex((destIp, destPort))
        if res:
            return None

        self._init_new_send(clientSocket,actual_ip)
        return clientSocket


    def close_all(self):

        for clientSocket in self.vlanToSocket.values():
            clientSocket.close()

        #Remove alias ip's configured for transmission.
        for vlan_id in self.vlan_to_ip:
            if self.interface is not None:
                cmd = "ifconfig "+ self.interface +":"+vlan_id+" down"
                commands.getoutput(cmd)

"""
    Distribution of the text needed to be sent into byte chunks .
    For each text chunk:
     - Compute secrete sharing (n,k) scheme - results with n shares.
     - Invoke _send_by_threads() method for sending the chunk shares.

"""
    def _send_text(self):
        secret_chunks = [self.secret[i:i+self.chunk_size] for i in xrange(0,len(self.secret),self.chunk_size)]

        for secretStr in secret_chunks:
          if not secretStr:
            break
          shares = self._create_shares(str(secretStr),self.threshold,self.num_shares)
          self._send_shares_by_threads(shares)

"""
    Method that responsible to distribute the n shares to ranges such 
    that each alias IP interface get assign of shares range to transmit.
    Each alias IP interface get shares range that contain at most k-1 shares.

"""
    def _distribute_shares_to_ranges(self):

        whole = self.num_shares / self.min_cut
        rem = self.num_shares % self.min_cut

        self.vlan_to_range = {}
        start = 0
        for vlan in self.vlan_to_ip:
            r = 0
            if rem >= 1:
                r = 1
            self.vlan_to_range[vlan] = [start, (start+whole + r)]
            start = start + whole + r
            rem -= 1

"""
     (-) Create instances of SendSharesThread (Thread class) as number of alias IP interfaces. Each thread instance
          holds socket interface to send from and shares range.
     (-) Invoke start () to each SendSharesThread instance – each instance execute send to destination host according to data it holds.
"""
    def _send_shares_by_threads(self,shares):

        threads = []

        for vlan_id in self.vlan_to_ip:
            startRange = self.vlan_to_range[vlan_id][0]
            endRange = self.vlan_to_range[vlan_id][1]

            threadShares = shares[startRange:endRange]
            newSendThread = sendSharesThread(self.vlanToSocket[vlan_id],threadShares,endRange-startRange ,self.vlan_to_ip[vlan_id])
            newSendThread.start()
            threads.append(newSendThread)

        for t in threads:
              t.join()

"""
    1.Open the file for reading.
    2.Read chunk bytes from file (according to user input)
    3.Distribution of the text needed to be sent into chunks (according user input).

    4.For each chunk:
        4.1. Compute secrete sharing (n,k) scheme - results with n shares.
        4.2 Invoke _send_by_threads() method to send chunks shares:

"""
    def _send_file(self):

        f = open(self.file_path,'r')
        while True:
          secretStr = f.read(self.chunk_size)
          if not secretStr:
            break
          shares = self._create_shares(secretStr,self.threshold,self.num_shares)

          self._send_shares_by_threads(shares)

        f.close()

    def _create_shares(self,secret_string, share_threshold, num_shares):
        shares = PlaintextToHexSecretSharer.split_secret(secret_string, share_threshold, num_shares)
        return shares

    def _get_my_ip(self):
        ifconfigOutput = commands.getoutput("ifconfig").split()
        for line in ifconfigOutput:
          if "addr" in line:
            ipSrc = ifconfigOutput[line.index("addr")+ 6 ]
            ipSrc = ipSrc.split(':')[1]
        return ipSrc

"""
    Method receives data from socket till input n size
"""
    def _recvall(self,sock,n):
        data = ""
        while len(data) < n:
          packet = sock.recv(n - len(data))
          if not packet:
              return None
          data += packet
        return data
        
def get_my_ip():
    ifconfigOutput = commands.getoutput("ifconfig").split()
    for line in ifconfigOutput:
      if "addr" in line:
        ipSrc = ifconfigOutput[line.index("addr")+ 6 ]
        ipSrc = ipSrc.split(':')[1]
    return ipSrc

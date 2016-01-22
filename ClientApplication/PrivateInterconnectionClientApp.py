#!/usr/bin/python
import commands
import socket
import sys

ipDest = sys.argv[1]
ipSrc = ''
mac = ''
pingCmd = "ping -c1 "+ipDest
output = commands.getoutput(pingCmd)
print output
words = commands.getoutput("ifconfig").split()
for line in words:
  if "HWaddr" in line:
    mac = words[ words.index("HWaddr") + 1 ]
  elif "addr" in line:
    ipSrc = words[line.index("addr")+6 ]
    ipSrc = ipSrc.split(':')[1]

s = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
port = 12345
s.connect(('192.168.1.4', port))

s.sendall(ipSrc +' ' + ipDest + '\n')
print s.recv(1024)

s.close



#!/usr/bin/python
import ClientSender as client
import ClientReceiver as server
import os
import threading
from Tkinter import *
import tkFileDialog
import ttk
import iptools
import tkMessageBox

class app():
  def __init__(self, parent):
      self.parent = parent
      self.parent.title("SDN Based Private Interconnection")
      self.parent.geometry("550x650")
      self.parent.configure(background ="alice blue")
      self.parent.configure(relief = RIDGE,borderwidth = 2)

      self.frame_1 = Frame(parent , bg = "white",relief = GROOVE, borderwidth = 1)
      self.frame_2 = LabelFrame(self.frame_1,bg = "white",text = "Choose Mode", relief = RIDGE,font="Helvetica 18 italic",labelanchor ='n')
      self.frame_3 = Frame(self.frame_2,background ="white", relief = GROOVE)

      self.serverBtn = Button(self.frame_3, text = " Client Recevier", fg ="#a1dbcd",bg = "#383a39",command = self.serverApp, font= "Helvetica 16 bold")
      self.clientBtn = Button(self.frame_3, text = " Client Sender", fg ="#a1dbcd",bg = "#383a39", command = self.clientApp,font= "Helvetica 16 bold")
      self.lblWelcome = Label(self.parent,text = "SDN Based Private Interconnection", font= "Verdana 24 bold italic",bg = "alice blue")

      self.client = None
      self.selectedServer = None

      self.initialize()

  def initialize(self):
      self.lblWelcome.pack(fill ="both",pady = 25)
      self.serverBtn.pack(pady = 0,padx =15,side = LEFT,expand = True)
      self.clientBtn.pack(side = LEFT,expand = True)
      self.frame_3.pack(fill = BOTH,pady = 35,padx = 10)
      self.frame_2.pack(fill = BOTH,pady = 20,padx = 25)
      self.frame_1.pack(fill = BOTH,pady = 40,padx = 20)
      self.parent.protocol("WM_DELETE_WINDOW",self.on_closing)
      self.parent.resizable(0,0)


  def on_closing(self):
      if self.client is not None:
          self.client.close_all()
      elif self.selectedServer is not None:
          self.selectedServer.close_all()
      self.parent.destroy()

  def clearMainApp(self):
      self.frame_1.pack_forget()
      self.frame_2.pack_forget()
      self.frame_3.pack_forget()
      self.lblWelcome.pack_forget()
      self.parent.configure(background ="white")


  def initClientApp(self):

      # local data
      myIP = client.get_my_ip()
      self.client_status = StringVar()

 # -----------> First Section  <--------------------------      
      clientFrame = Frame(self.parent , bg = "white",relief = GROOVE, borderwidth = 1)
      labelFrame = LabelFrame(clientFrame,bg = "lavender",relief =RIDGE)
      self.client_btn_reqPrivateCon = Button(clientFrame, text = "Request Private Interconnection",bg = "azure3", command = self.validate_and_request, font= ("Helvetica",12))

      #Source IP
      lbl_sourceIP = Label(labelFrame, text = "Source IP:", font= ("Helvetica",12),bg = "lavender")
      ent_sourceIP = Label(labelFrame, text = myIP ,bg = "white", relief = GROOVE,width = 20, )
      #Dest IP
      lbl_destIP = Label(labelFrame, text = "Dest IP:", font= ("Helvetica",12),bg = "lavender")
      ent_destIP = Entry(labelFrame,width = 20)

      self.client_ip = myIP
      self.client_input_destIP = ent_destIP

      # Order First Section
      lbl_sourceIP.grid(row = 0,padx = 5,pady=5)
      ent_sourceIP.grid(row = 0, column = 1,pady = 5)
      lbl_destIP.grid(row = 0,column = 3,padx = 15,pady=5)
      ent_destIP.grid(row = 0, column = 4,pady = 5)

      labelFrame.pack(fill = "both",expand = True,padx = 10,pady = 5)
      self.client_btn_reqPrivateCon.pack(pady = 5, padx = 20)
      clientFrame.pack(fill =BOTH, padx = 10,pady = 3)

 # -----------> Second Section  <--------------------------  
      self.client_minCutStr = StringVar()

      clientFrame1 = Frame(self.parent , bg = "white",relief = GROOVE, borderwidth = 1)

      labelFrame1 = LabelFrame(clientFrame1,bg = "alice blue",relief =RIDGE)
      labelFrameIpAlias = LabelFrame(labelFrame1,bg = "alice blue",relief =GROOVE,text = "Vlan Id & Alias IP",font= ("Helvetica",12),labelanchor ='n')

      #List Box Alias IP with scrollbar
      s = Scrollbar(labelFrameIpAlias,orient=VERTICAL)
      self.client_lb = Listbox(labelFrameIpAlias,height = 3)
      s.config(command=self.client_lb.yview)
      self.client_lb.config(yscrollcommand=s.set)

      #Min Cut label
      lbl_minCut = Label(labelFrame1, text = "Min-Cut:", font= ("Helvetica",11),bg = "alice blue")
      self.client_ent_minCut = Label(labelFrame1, textvariable = self.client_minCutStr ,bg = "white", relief = GROOVE,width = 10 )

      # Order Second Section
      lbl_minCut.grid(row = 0,padx = 5,pady=5)
      self.client_ent_minCut.grid(row = 0, column = 1,pady = 5,padx = 5)
      self.client_lb.grid(row = 0,padx = 10 , pady = 5)
      s.grid(row = 0,column = 1,sticky = N+S)
      labelFrameIpAlias.grid(row = 0 , column = 3 ,padx = 40, pady = 5)

      labelFrame1.pack(fill = "both",expand = True,padx = 10,pady = 5)
      clientFrame1.pack(fill =BOTH, padx = 10,pady = 3)

 # -----------> Thired Section  <--------------------------  

      clientFrame2 = Frame(self.parent , bg = "white",relief = GROOVE, borderwidth = 1)
      labelFrame2 = LabelFrame(clientFrame2,bg = "white",relief =RIDGE,text = "Configure Seccret Sharing",font= ("Helvetica",14),labelanchor ='n')

      labelFrameConfig = LabelFrame(labelFrame2 ,bg = "misty rose",relief =GROOVE)

      lbl_sharesNum = Label(labelFrameConfig, text = "Shares Num:", font= ("Helvetica",12),bg = "misty rose")
      lbl_threshold = Label(labelFrameConfig, text = "Threshold:", font= ("Helvetica",12),bg = "misty rose")
      lbl_chunk = Label(labelFrameConfig, text = "Chunk Size:\n[Bytes]", font= ("Helvetica",12),bg = "misty rose")


      sp_sharesNum = Spinbox(labelFrameConfig,from_=0,to=100,width = 5)
      self.client_in_sharesNum=sp_sharesNum

      sp_threshold = Spinbox(labelFrameConfig,from_=0,to=100,width = 5)
      self.client_in_threshold = sp_threshold

      scale = Scale(labelFrameConfig, from_=20,to = 1000, resolution = 20,orient = HORIZONTAL,bg = "white")
      self.client_in_chunk = scale

      lbl_sharesNum.grid(row = 0, padx = 5, pady = 5)
      sp_sharesNum.grid(row = 0, column = 1,padx = 5)
      lbl_threshold.grid(row = 0, column = 2,padx = 5)
      sp_threshold.grid(row = 0 ,column = 3,padx = 5)
      lbl_chunk.grid(row = 0 , column = 4,padx = 5)
      scale.grid(row = 0 , column = 5, padx = 5,pady = 8)

      labelFrameConfig.grid(row = 0,padx = 5, pady = 5)
      labelFrame2.pack(fill =BOTH,padx =10,pady = 5,expand = True)
      clientFrame2.pack(fill = BOTH,padx = 10,pady = 3)

 # -----------> Forth Section  <--------------------------  
      self.client_in_file_path = ''
      clientFrame3 = Frame(self.parent , bg = "white",relief = GROOVE, borderwidth = 1)
      labelFrame3 = LabelFrame(clientFrame3,bg = "lemon chiffon",relief =RIDGE,text = "Secret To Send",font= ("Helvetica",14),labelanchor ='n')
      labelFrameSecret = LabelFrame(labelFrame3 ,bg = "white",relief =GROOVE)
      labelFrameSecret2 = LabelFrame(labelFrame3 ,bg = "lemon chiffon",relief =RIDGE)

      self.client_filePathStr = StringVar()

      lbl_filePath = Label(labelFrameSecret2,text = "File Path:",font= ("Helvetica",11),bg = "lemon chiffon")
      lbl_filePathStr = Label(labelFrameSecret2, textvariable = self.client_filePathStr ,bg = "white", relief = GROOVE,width = 40 )


      text =Text(labelFrameSecret ,bg = "white" ,height = 7,width = 50)
      self.client_in_secretText = text

      btn_file = Button(labelFrameSecret2,text = "Open File",command = self.handle_file,font= ("Helvetica",12))
      btn_clearFile = Button(labelFrameSecret2,text = "X",font= ("Helvetica",12),command = self.clearFilePath)

      stext = Scrollbar(labelFrameSecret)
      stext.config(command=text.yview)
      text.config(yscrollcommand=stext.set)


      btn_file.grid(row =0, column = 3)
      btn_clearFile.grid(row = 0,column = 4)
      stext.pack(side = RIGHT,fill = Y)
      text.pack(side = LEFT, fill = Y)


      lbl_filePath.grid(row = 0,column =0)
      lbl_filePathStr.grid(row = 0,column = 1)

      labelFrameSecret.pack( pady=5)
      labelFrameSecret2.pack(fill = BOTH,padx = 5,pady = 8)

      labelFrame3.pack(fill =BOTH,padx =10,pady = 5,expand = True)
      clientFrame3.pack(fill = BOTH,padx = 10,pady = 3)


 # -----------> 5th  Section  <--------------------------  

      clientFrame4 = Frame(self.parent , bg = "white",relief = GROOVE, borderwidth = 1)
      self.client_btn_send = Button(clientFrame4, text = "Send",bg = "steelBlue1", command = self.send, font= ("Helvetica",12),width = 30,state = DISABLED,disabledforeground = "gray17")

      self.client_btn_disconnect = Button(clientFrame4, text = "Disconnect",bg = "steelBlue3", command = self.disconnect, font= ("Helvetica",12),width = 30,state = DISABLED,disabledforeground = "gray17")

      self.client_btn_send.grid(row=0,column = 0,padx = 20)#,padx = 120)
      self.client_btn_disconnect.grid(row = 0,column = 1)
      clientFrame4.pack(fill = BOTH,padx =10,pady=3)


 # -----------> 6th  Section  <--------------------------  
      self.client_status.set("Offline")
      clientFrame5 = Frame(self.parent , bg = "white",relief = GROOVE, borderwidth = 1)
      self.client_lbl_status = Label(clientFrame5, textvariable = self.client_status ,bg = "red", relief = GROOVE,width = 50 )

     # self.client_btn_disconnect = Button(clientFrame5, text = "Disconnect",bg = "azure3", command = self.disconnect, font= ("Helvetica",12),width = 50,state = DISABLED)

      self.client_lbl_status.pack( pady = 5, padx = 70)
     # self.client_btn_disconnect.pack(pady = 5, padx = 60)

      clientFrame5.pack(padx =10,pady=10)



  def disconnect(self):
      self.client.close_all()
      self.client_btn_disconnect.config(state = DISABLED)
      self.client_lbl_status.config(bg ="red")
      self.client_status.set("Offline")
      self.client_btn_reqPrivateCon.config(state = NORMAL)
      self.client_btn_disconnect.config(state = DISABLED)
      self.client_btn_send.config(state = DISABLED)

      self.client_input_destIP.delete(0,END)
      self.client_minCutStr.set("")
      self.client_lb.delete(0,END)

      self.client_in_sharesNum.delete(0,END)
      self.client_in_threshold.delete(0,END)
      self.client_in_sharesNum.insert(0,'0')
      self.client_in_threshold.insert(0,'0')

      self.client_in_chunk.set(0)
      self.client_in_secretText.delete("1.0",END)
      self.client_filePathStr.set("")

      return


  def send(self):
      numShares = self.client_in_sharesNum.get()
      threshold = self.client_in_threshold.get()
      chunk = self.client_in_chunk.get()
      secretStr = self.client_in_secretText.get("1.0",END)
      filePath = self.client_in_file_path

      self.client.set_user_input(int(numShares),int(threshold),int(chunk))
      isValidRatio = self.client.check_ratio()

      if isValidRatio:
          if self.client_status.get() == "Connected":
              isConnected = True
          else:
              isConnected = False

          if filePath == '':
              if len(secretStr.split()) == 0:
                  tkMessageBox.showerror("Empty Secret","Secret Text Is Empty !!")
              else:
                  self.client.data_type = "Text"
                  self.client.is_connected = isConnected
                  self.client.secret = secretStr
                  if chunk > len(secretStr):
                      self.client.chunk_size = 20

                  res = self.client.send_data()
                  if res is False:
                      return

                  self.client_lbl_status.config(bg = "spring green")
                  self.client_status.set("Connected")

                  self.client_btn_disconnect.config(state = NORMAL)
                  self.client_btn_reqPrivateCon.config(state = DISABLED)
                  self.client_in_secretText.delete("1.0",END)

          else:
               self.client.file_path = filePath
               self.client.data_type = "File"
               self.client.is_connected = isConnected
               head,tail = os.path.split(filePath)
               self.client.fileName = tail
               self.client.fileSize = os.path.getsize(filePath)

               res = self.client.send_data()
               if res is False:
                   return

               self.client_lbl_status.config(bg = "spring green")
               self.client_status.set("Connected")
               self.client_btn_disconnect.config(state = NORMAL)
               self.client_btn_reqPrivateCon.config(state = DISABLED)

               self.client_in_secretText.delete("1.0",END)
               self.client_filePathStr.set("")
               self.client_in_file_path = ''

      else:
          tkMessageBox.showerror("Invalid Ration","You Insert Invalid Ration of number shares and threshold ! \n [num shares \ threshold < mincut]")

      return

  def clearFilePath(self):
      self.client_filePathStr.set("")
      self.client_in_file_path = ''

  def handle_file(self):
      self.client_in_file_path = tkFileDialog.askopenfilename(initialdir = 'filesToSend')
      self.client_filePathStr.set(self.client_in_file_path)


  def clientApp(self):
      self.clearMainApp()
      self.initClientApp()
      self.parent.mainloop()


  def validate_and_request(self):

      input_dest_ip = self.client_input_destIP.get()
      if "." not in input_dest_ip:
          input_dest_ip += '.'
      if not iptools.ipv4.validate_ip(input_dest_ip):
          tkMessageBox.showerror("Invalid Dest IP","You Insert Invalid Dest IP !")
          self.client_input_destIP.delete(0,END)
          return

      self.client_lb.delete(0,END)

      self.client = client.ClientSender(input_dest_ip)
      self.client.request_private_interconnection_from_admin()
      self.client_minCutStr.set(str(self.client.min_cut))

      if self.client.min_cut >=2:
          self.client_minCutStr.set(str(self.client.min_cut))

          #Delete spinBox of shares num +threshold and replace with minCut result
          self.client_in_sharesNum.delete(0,END)
          self.client_in_threshold.delete(0,END)
          self.client_in_sharesNum.insert(0,self.client.min_cut)
          self.client_in_threshold.insert(0,self.client.min_cut)

          self.client.config_infra()

          index = 1
          for vlanId in self.client.vlan_to_ip:
              self.client_lb.insert(index,vlanId + "   -   "+self.client.vlan_to_ip[vlanId] )
              index +=1
          self.client_lbl_status.config(bg = "gold")
          self.client_status.set("Network Configured")
          self.client_btn_send.config(state = NORMAL)
          self.client_btn_disconnect.config(state = NORMAL)
      else:
          tkMessageBox.showinfo("Can't Provide Private Interconnecction","Min Cut is lower then 2, cant provide private interconnection")


  def clearServerText(self):
      self.serverText.delete("1.0",END)


  def serverApp(self):
      self.clearMainApp()
      curCon = StringVar()
      curFileData = StringVar()
      curSharesData = StringVar()


      serverFrame = LabelFrame(self.parent,bg = "white",text = "Text Recived", relief = RIDGE,font= ("Helvetica",16),labelanchor ='n',width = 10,height = 10,padx = 15,pady = 10)
      text =Text(serverFrame,bg = "white" ,state ="disabled")
      self.serverText = text

      labelFrameFileData = LabelFrame(self.parent ,bg = "white",relief =RIDGE,text = "File Data ",font= ("Helvetica",16),labelanchor ='n')
      lblcurFileData = Label(labelFrameFileData,textvariable = curFileData,justify = LEFT,anchor = W,bg = "pale turquoise",relief = GROOVE,font= ("Helvetica",14))
      prog = ttk.Progressbar(labelFrameFileData,orient = HORIZONTAL,length =300, mode= 'determinate')


      self.selectedServer = server.ClientReceiver(curCon,text,curFileData,curSharesData,prog)
      serverIp = self.selectedServer.serverIp
      lblServerIpStr = serverIp

      s = Scrollbar(serverFrame)
      s.config(command=text.yview)
      text.config(yscrollcommand=s.set)

      labelFrameServerIp = LabelFrame(self.parent ,bg = "white",relief =RIDGE,text = "Server IP ",font= ("Helvetica",16),labelanchor ='n')
      lblserverIp = Label(labelFrameServerIp,text =lblServerIpStr,relief = GROOVE, bg = "#a1dbcd", font= ("Helvetica",16))

      labelFrameConnection = LabelFrame(self.parent ,bg = "white",relief =RIDGE,text = "Connection Data ",font= ("Helvetica",16),labelanchor ='n')
      lblcurCon = Label(labelFrameConnection,textvariable = curCon,justify = LEFT,bg = "deep sky blue",relief =GROOVE,font= ("Helvetica",14) )


      labelFrameSharesData = LabelFrame(self.parent ,bg = "white",relief =RIDGE,text = "Shares Data ",font= ("Helvetica",16),labelanchor ='n')
      lblcurSharesData = Label(labelFrameSharesData,textvariable = curSharesData,justify = LEFT,anchor = W,bg = "aquamarine",relief =GROOVE,font= ("Helvetica",14))



      lblserverIp.pack(fill = BOTH, pady = 3,padx = 15)
      labelFrameServerIp.pack(fill = BOTH,padx = 15,pady = 10)


      lblcurCon.pack(fill = BOTH,pady = 3,padx = 15)
      labelFrameConnection.pack(fill=BOTH,pady = 10,padx = 15)


      lblcurSharesData.pack(fill = BOTH,padx = 15,pady = 3)
      labelFrameSharesData.pack(fill = BOTH,pady = 10,padx = 15)

      lblcurFileData.pack(fill = BOTH,padx = 15,pady = 3)
      labelFrameFileData.pack(fill = BOTH,pady = 10,padx = 15)
      prog.pack()

      s.pack(side = RIGHT,fill = BOTH)
      text.pack(side = LEFT,fill = BOTH)
      serverFrame.pack(fill = BOTH,pady = 50,padx = 50)

      threads = []
      t = threading.Thread(target = self.selectedServer.selected_tcp_server)
      threads.append(t)
      t.start()
      self.parent.mainloop()
      for t in threads:
          t.join()

if __name__ == "__main__":
   root = Tk()
   app = app(root)
   root.mainloop()




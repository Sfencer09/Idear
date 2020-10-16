import pytesseract
import cv2
from numba import jit
import time
import socketserver
import struct
import mysql.connector
import threading
import os

clientTokens = dict() #keys are the tokens, value is a tuple of the user ID and time the token expires
tokenLock = threading.Lock()
TOKEN_CLEANUP_INTERVAL = 5*60 #5 minutes
TOKEN_DURATION = 60*60 #1 hour
CONFIDENCE_THRESHOLD = 90
TCP_port = 30500
UDP_port = 30501
server_address = "192.168.0.6"
image_temp_dir = "B:\\" #underlying storage medium should be as fast as possible, I use a RAM disk for testing
database = mysql.connector.connect(host="127.0.0.1", user="Idear", passwd="ReeceElliotTrey", database="Idear")

#@jit(target ="CPU") 
def imageToBlocks(image):
    startTime = time.time()
    #convert to text using tesseract
    rawData = pytesseract.image_to_data(convertedImage)
    splitData = rawData.splitlines()[1:]
    print(rawData)
    blocks = []
    for block in splitData:
        parts = block.split()
        if len(parts) > 11: #test if it has text
            b = []
            for i in range(0,11):
                b.append(int(parts[i]))
            b.append(parts[11])
            blocks.append(b)
    #print(blocks)
    print(time.time()-startTime)
    return blocks

def joinBlocks(blocks): #converts blocks to finalized text string
    finalString = ""
    #todo: sort blocks by position before joining text
    #use content and block positions to determine if rearrangement is necessary
    for block in blocks:
        text = block[11]
        #scan confidences and potentially correct those with low confidence
        if (block[10] < CONFIDENCE_THRESHOLD):
            #run spellcheck and select correction with highest probability
            pass
        finalString += " " + text
    return finalString

def blocksToString(blocks): #converts blocks to debug string, similar to original raw tesseract output
    output = ""
    for block in blocks:
        output += "%d %d %d %d %d %d %d %d %d %d %s\n" % block
    return output

def cleanupTokens():
    for token in clientTokens:
        if(clientTokens[token][1] <= time.time()):
            tokenLock.acquire()
            del clientTokens[token]
            tokenLock.release()

def validateToken(token):
    if token in clientTokens:
        if(clientTokens[token][1] <= time.time()):
            tokenLock.acquire()
            del clientTokens[token]
            tokenLock.release()
            return False
        else:
            return True
    else:
        return False

class idearTCPHandler(socketserver.StreamRequestHandler):
    def handle(self):
        cursor = database.cursor()
        requestType = self.request.recv(1)[0]
        if(requestType == 108): #'l', login part 1
            #check if email is in user table, if so get salt and send to client
            emailLen = struct.unpack("H", self.request.recv(2))[0]
            email = self.request.recv(emailLen).decode("utf-8")
            salt = cursor.callproc("getSalt", [email])[0][0]
            if(salt == ""):
                self.request.sendall("lf".encode("utf-8"))
                return
            else:
                self.request.sendall(("ls" + salt).encode("utf-8"))
            #login part 2
            responseType = self.request.recv(1)[0]
            if(responseType != 76): #'L'
                #invalid message
                pass
            emailLen2 = struct.unpack("H", self.request.recv(2))[0]
            email2 = self.request.recv(emailLen2).decode("utf-8")
            if(email2 != email):
                #did not receive same email as first message
                pass
            passHash = self.request.recv(32)
            userID = cursor.callproc("verifyHash", [email, passHash])[0][0]
            if(userID == -1):
                self.request.sendall("Lf".encode("utf-8"))
            else:
                #generate client token
                tokenLock.acquire()
                token = os.urandom(32)
                while(token in clientTokens):
                    #guarantee token's uniqueness, although this is likely not necessary
                    token = os.urandom(32)
                clientTokens[token] = (userID, time.time() + TOKEN_DURATION)
                tokenLock.release()
                self.request.sendall("Ls".encode("utf-8")+token)
        elif(requestType == 110): #'n', new user part 1
            #check if email is in user table, if not generate salt and send to client
            emailLen = struct.unpack("H", self.request.recv(2))[0]
            email = self.request.recv(emailLen).decode("utf-8")
            salt = cursor.callproc("getSalt", [email])[0][0]
            if(salt != ""):
                self.request.sendall("lf".encode("utf-8"))
            else:
                salt = os.random(8).hex()
                self.request.sendall(("ls" + salt).encode("utf-8"))
            responseType = self.request.recv(1)[0]
            if(responseType != 78): #'N'
                self.request.sendall("EN".encode("utf-8"))
                return
            emailLen2 = struct.unpack("H", self.request.recv(2))[0]
            email2 = self.request.recv(emailLen2).decode("utf-8")
            if(email2 != email):
                #did not receive same email as first message
                pass
            passHash = self.request.recv(32)
            
        elif(requestType == 116): #'t', image conversion to text
            token = self.request.recv(32)
            if not validateToken(token):
                #token is invalid, send error code for expired token
                pass
            
            requestNum = struct.unpack("I", self.request.recv(4))[0]
            ancSize = struct.unpack("H", self.request.recv(2))[0]
            ancillary = self.request.recv(ancSize).decode("utf-8")
            imageSize = struct.unpack("I", self.request.recv(4))[0]
            filename = image_temp_dir + str(token[0]) +'-' + str(requestNum)
            imageFile = open(filename, "wb+")
            imageFile.write(self.request.recv(imageSize))
            imageFile.close()
            #image written to file, apply preprocessing
            convertedImage = image
            #binarize
            
            #give image to Tesseract
            blocks = imageToBlocks(convertedImage)  #This is the expensive line
            text = joinBlocks(blocks)
            #send response back to client
            textBytes = text.encode("utf-8")
            response = struct.pack("cIHp", 't', requestNum, len(textBytes), textBytes)
            self.request.sendall(response)
            #write all to database
            #addImage(IN UsID int, IN bsImg longblob, IN fnImg longblob, IN ancData text, IN Tsblks text, IN rtnText text)
            userID = clientTokens[token][0]
            cursor.callproc("addImage", [userID, image, convertedImage, ancillary, str(blocks), text])
    pass

class idearTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    pass



class idearUDPHandler(socketserver.DatagramRequestHandler):
    def handle(self):
        requestType = self.request.read(1)
        if(requestType == b't'):
            #image conversion to text
            pass
        elif(requestType == b'a'):
            #image conversion to text+audio
            pass
        pass
    pass

class idearUDPServer(socketserver.ThreadingMixIn, socketserver.UDPServer):
    pass


def test():
    print(joinBlocks(imageToBlocks(r'C:\Servers\Other Servers\idear\test1.png')))
    #Noisy image to test Tesseract OCR
    print(joinBlocks(imageToBlocks(r'C:\Servers\Other Servers\idear\test2.JPG')))
    #You've heard of elf on a shelf,
    #but have you heard of...
    print(joinBlocks(imageToBlocks(r'C:\Servers\Other Servers\idear\test3.JPG')))
    #(215): He said he loved me more than Kel loves orange soda.
    #(610): the result of growing up in the '90's.
    #texts from last night
    print(joinBlocks(imageToBlocks(r'C:\Servers\Other Servers\idear\test4.JPG')))
    #firelorcl:
    #i scare people lots because i walk
    #very softly and they don't hear me
    #enter rooms so when they turn
    #around i'm just kind of there and
    #their fear fuels me


def main():
    tcpServer = idearTCPServer(("localhost", TCP_port), idearTCPHandler)
    tcpThread = threading.Thread(target=tcpServer.serve_forever())
    tcpThread.run()
    #udpServer = iderUDPServer(("localhost", UDP_port), idearUDPHandler)
    #udpThread = threading.Thread(target=udpServer.server_forever())
    #udpThread.run()
    tokenCleanupTimer = threading.Timer(TOKEN_CLEANUP_INTERVAL, cleanupTokens)
    tokenCleanupTimer.start()
    
    #udpThread.join()
    tcpThread.join()




if __name__ == "__main__":
    test()
    main()

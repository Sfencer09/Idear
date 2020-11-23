import pytesseract
import cv2
import numpy as np
#from numba import jit
import time
from datetime import datetime
import socketserver
import struct
import mysql.connector
import threading
import _thread
import os
from functools import partial

clientTokens = dict() #keys are the tokens, value is a tuple of the user ID and time the token expires
tokenLock = threading.Lock()
TOKEN_CLEANUP_INTERVAL = 5*60 #5 minutes
TOKEN_DURATION = 60*60 #1 hour
CONFIDENCE_THRESHOLD = 90
TCP_port = 30501
UDP_port = 30501
server_address = "192.168.0.6"
image_temp_dir = "B:\\" #underlying storage medium should be as fast as possible, I use a RAM disk for testing
#try:
#    database = mysql.connector.connect(host="localhost", user="Idear", passwd="ReeceElliotTrey", database="Idear")
#except:
#    printWithTime("unable to connect to database")
#print = partial(print, "[%s]" % time.ctime(), flush=True)

def printWithTime(*args):
    print("[%s]: " % datetime.utcnow().isoformat(sep=' ', timespec='milliseconds'), *args)

#@jit(target ="CPU") 
def imageToBlocks(image): #extracts text from the given image using Tesseract
    startTime = time.time()
    #convert to text using tesseract
    rawData = pytesseract.image_to_data(image)
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

<<<<<<< Updated upstream

def cleanupTokens(): #remove all expired tokens from the token dictionary
    print("starting token cleanup")
=======
def cleanupTokens():
    printWithTime("starting token cleanup")
>>>>>>> Stashed changes
    for token in clientTokens:
        if(clientTokens[token][1] <= time.time()):
            tokenLock.acquire()
            del clientTokens[token]
            tokenLock.release()
    printWithTime("token cleanup done")

def validateToken(token): #tests whether the given token is valid and not expired
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

<<<<<<< Updated upstream
def preprocessImage(imagepath): #apply filters to image to improve Tesseract's accuracy
=======
def preprocessImage(imagepath):
    startTime = time.time()
    #read image
>>>>>>> Stashed changes
    image = cv2.imread(imagepath)
    #convert to grayscale
    image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    #cv2.imwrite(imagepath[:-4]+"-processed-1.png", image)
    #apply median blur to 'smear' image grain, helps reduce noise after thresholding
    image = cv2.medianBlur(image, 3)
    #cv2.imwrite(imagepath[:-4]+"-processed-2.png", image)
    #canny edge here?
    #binarize the image with thresholding
    image = cv2.threshold(image, 0, 255, cv2.THRESH_BINARY | cv2.THRESH_OTSU)[1]
    #remove excess black around border
    #deskew here
    #save final processed image and return path to it
    newimagepath = imagepath[:-4]+"-processed-final.png"
    cv2.imwrite(newimagepath, image)
    printWithTime("image preprocessing took %f seconds" % (time.time() - startTime))
    return newimagepath

def shutdownServer(server):
    server.shutdown()

class idearTCPHandler(socketserver.StreamRequestHandler):
    def handle(self): 
        while True: #keep reading from socket until client closes connection
            try:
                cursor = database.cursor()
            except:
                pass
            try:
                requestType = self.request.recv(1)[0]
            except (IndexError, ConnectionResetError):
                printWithTime('Client closed connection')
                break
            except KeyboardInterrupt:
                printWithTime('Shutting down server')
                _thread.start_new_thread(shutdownServer, (tcpServer,))
            printWithTime("received connection of type ", requestType)
            if(requestType == 108): #'l', login part 1
                #check if email is in user table, if so get salt and send to client
                emailLen = struct.unpack("!H", self.request.recv(2))[0]
                printWithTime("email length is ", emailLen)
                email = self.request.recv(emailLen).decode("utf-8")
                printWithTime(type(email))
                printWithTime("got email address \"", email, "\"")
                #attempt to get salt from database, if database fails revert to testing mode
                try:
                    salt = cursor.callproc("getSalt", [email])[0][0]
                    printWithTime('Got salt from database')
                except:
                    if email == "test@gmail.com":
                        salt = "0000000000000000"
                        printWithTime('Got test email, using default salt')
                    else:
                        salt=""
                        printWithTime('No salt found')
                printWithTime("salt is \"", salt, "\"", sep='')
                if(salt == ""):
                    self.request.sendall("lf".encode("utf-8"))
                    return
                else:
                    self.request.sendall(("ls" + salt).encode("utf-8"))
                #login part 2
                printWithTime("starting part 2 of login")
                responseType = self.request.recv(1)[0]
                printWithTime("second response type is ", responseType)
                if(responseType != 76): #'L'
                    #invalid message
                    pass
                emailLen2 = struct.unpack("!H", self.request.recv(2))[0]
                email2 = self.request.recv(emailLen2).decode("utf-8")
                if(email2 != email):
                    #did not receive same email as first message
                    pass
                printWithTime("second email matches")
                passHash = self.request.recv(32)
                printWithTime("password hash ", passHash)
                try:
                    userID = cursor.callproc("verifyHash", [email, passHash])[0][0]
                except:
                    if True: #passHash == b'1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF':
                        userID = 1
                    else:
                        userID = -1
                printWithTime("user id=", userID)
                if(userID == -1):
                    self.request.sendall("Lf".encode("utf-8"))
                    printWithTime('Login failed')
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
                    printWithTime('Login succeeded')
            elif(requestType == 110): #'n', new user part 1
                #check if email is in user table, if not generate salt and send to client
                emailLen = struct.unpack("!H", self.request.recv(2))[0]
                email = self.request.recv(emailLen).decode("utf-8")
                try:
                    salt = cursor.callproc("getSalt", [email])[0][0]
                except:
                    pass
                if(salt != ""):
                    self.request.sendall("lf".encode("utf-8"))
                else:
                    salt = os.random(8).hex()
                    self.request.sendall(("ls" + salt).encode("utf-8"))
                responseType = self.request.recv(1)[0]
                if(responseType != 78): #'N'
                    self.request.sendall("EN".encode("utf-8"))
                    return
                emailLen2 = struct.unpack("!H", self.request.recv(2))[0]
                email2 = self.request.recv(emailLen2).decode("utf-8")
                if(email2 != email):
                    #did not receive same email as first message
                    pass
                passHash = self.request.recv(32)
                
                
            elif(requestType == 116): #'t', image conversion to text
                printWithTime('got image')
                token = self.request.recv(32)
                printWithTime('retrieved token')
                if not validateToken(token):
                    #token is invalid, send error code for expired token
                    pass
                userID = clientTokens[token][0]
                requestNum = struct.unpack("!I", self.request.recv(4))[0]
                printWithTime('request number=', requestNum)
                ancSize = struct.unpack("!H", self.request.recv(2))[0]
                ancillary = self.request.recv(ancSize).decode("utf-8")
                printWithTime('ancillary data: \"', ancillary, '\"', sep="")
                imageSize = struct.unpack("!I", self.request.recv(4))[0]
                printWithTime('image size=', imageSize)
                imageContents = self.request.recv(imageSize)
                while len(imageContents) < imageSize:
                    #printWithTime(len(imageContents))
                    imageContents += self.request.recv(imageSize - len(imageContents))
                filename = image_temp_dir + token.hex() + '-' + str(requestNum) + '.png'
                printWithTime('saving image to ', filename)
                imageFile = open(filename, "wb+")
                imageFile.write(imageContents)
                imageFile.close()
                printWithTime('file written')
                #preprocess image and give result to Tesseract
                blocks = imageToBlocks(preprocessImage(filename))  #This is the expensive line
                text = joinBlocks(blocks)
                printWithTime('completed processing, got \"', text, '\"')
                #send response back to client
                textBytes = text.encode("utf-8")
                response = struct.pack("!cIH", b't', requestNum, len(textBytes)) + textBytes
                self.request.sendall(response)
                #write all to database
                #addImage(IN UsID int, IN bsImg longblob, IN fnImg longblob, IN ancData text, IN Tsblks text, IN rtnText text)
                
                #cursor.callproc("addImage", [userID, image, convertedImage, ancillary, str(blocks), text])
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
    print(joinBlocks(imageToBlocks(r'.\test1.png')))
    #Noisy image to test Tesseract OCR
    print(joinBlocks(imageToBlocks(r'.\test2.JPG')))
    #You've heard of elf on a shelf,
    #but have you heard of...
    print(joinBlocks(imageToBlocks(r'.\test3.JPG')))
    #(215): He said he loved me more than Kel loves orange soda.
    #(610): the result of growing up in the '90's.
    #texts from last night
    print(joinBlocks(imageToBlocks(r'.\test4.JPG')))
    #firelorcl:
    #i scare people lots because i walk
    #very softly and they don't hear me
    #enter rooms so when they turn
    #around i'm just kind of there and
    #their fear fuels me


def main():
    global tcpServer
    tcpServer = idearTCPServer((server_address, TCP_port), idearTCPHandler)
    #tcpServer.serve_forever()
    tcpThread = threading.Thread(target=tcpServer.serve_forever)
    printWithTime("TCP thread created")
    tcpThread.start()
    printWithTime("TCP thread started")
    #udpServer = iderUDPServer((server_address, UDP_port), idearUDPHandler)
    #udpThread = threading.Thread(target=udpServer.server_forever())
    #udpThread.start()
    tokenCleanupTimer = threading.Timer(TOKEN_CLEANUP_INTERVAL, cleanupTokens)
    printWithTime("token cleanup timer created")
    tokenCleanupTimer.start()
    printWithTime("token cleanup timer started, joining to TCP thread")
    #udpThread.join()
    tcpThread.join()
    



if __name__ == "__main__":
    #test()
    main()

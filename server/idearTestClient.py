import socket
import ssl
import struct

#sslContext = ssl.create_default_context()
sock = socket.create_connection(("project-treytech.com", 30501))
#sock = sslContext.wrap_socket(, server_hostname="project-treytech.com")
print("socket connected")

email = r'test@gmail.com'
passhash = b'1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF'
emailUTF = email.encode('utf-8')


def testLogin():
    loginMessage1 = struct.pack("!cH", 'l'.encode('utf-8'), len(emailUTF)) + emailUTF
    print(loginMessage1)
    sock.sendall(loginMessage1)
    response = sock.recv(18)
    print(response)
    loginMessage2 = struct.pack("!cH", 'L'.encode('utf-8'), len(emailUTF)) + emailUTF + passhash
    print(loginMessage2)
    sock.sendall(loginMessage2)
    response = sock.recv(34)
    print(response)


testLogin()

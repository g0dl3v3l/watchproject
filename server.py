import socket
import struct
import unicodedata
import csv
import json
try:
    from types import SimpleNamespace as Namespace
except ImportError:
    from argparse import Namespace

# data settings
data_size = 138 # sending 16 bytes = 128 bits (binary touch states, for example)

# server settings
server_name = "192.168.0.100"
server_port = 5000
server_address = (server_name, server_port)
x_val = 0
# start up server
print ('Setting up server on:', server_address)
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind(server_address)
server_socket.listen(1)

# wait for connection
print ('Waiting for a client connection...')
connection, client_address = server_socket.accept()
print ('Connected to:', client_address)


count = 0

final_data = []

while True:

    while True:
        # read length of data (4 bytes)
        try:
            count += 1
            length_bytes = connection.recv(4)
            if not length_bytes:
                break
            length = struct.unpack('>I', length_bytes)[0]

            # read data itself
            data = connection.recv(length)
            if not data:
                break
        
            if data.decode('utf-8') == "done!" :
                break
            
            # do something with data
            print("////////////////////////////////////////////////////////////////////////////////////")
            print(count)
            print("Received data from client:", data.decode('utf-8'))
            result = unicodedata.normalize('NFKD', data.decode('utf-8')).encode('ascii', 'ignore')
            result_obj =  json.loads(result, object_hook=lambda d: Namespace(**d))
            
            a_x_str = unicodedata.normalize('NFKD', result_obj.x_val).encode('ascii', 'ignore')
            a_y_str = unicodedata.normalize('NFKD', result_obj.y_val).encode('ascii', 'ignore')
            a_z_str = unicodedata.normalize('NFKD', result_obj.z_val).encode('ascii', 'ignore')
            
            g_x_str = unicodedata.normalize('NFKD', result_obj.xG_val).encode('ascii', 'ignore')
            g_y_str = unicodedata.normalize('NFKD', result_obj.yG_val).encode('ascii', 'ignore')
            g_z_str = unicodedata.normalize('NFKD', result_obj.zG_val).encode('ascii', 'ignore')
            
            x_L_str = unicodedata.normalize('NFKD', result_obj.xL_val).encode('ascii', 'ignore')
            y_L_str = unicodedata.normalize('NFKD', result_obj.yL_val).encode('ascii', 'ignore')
            z_L_str = unicodedata.normalize('NFKD', result_obj.zL_val).encode('ascii', 'ignore')
            

            a_norm_str = unicodedata.normalize('NFKD', result_obj.a_Mag).encode('ascii', 'ignore')
            g_norm_str = unicodedata.normalize('NFKD', result_obj.g_Mag).encode('ascii', 'ignore')
            a_norm_str = unicodedata.normalize('NFKD', result_obj.l_Mag).encode('ascii', 'ignore')
            final_data.append([float(a_x_str), float(a_y_str), float(a_y_str), float(g_x_str), float(g_y_str), float(g_y_str), float(a_norm_str), float(g_norm_str)])
            print(final_data)
        
        except:
            continue
    
    print("Session done")
    
    with open("new_file.csv","w+") as my_csv:
        csvWriter = csv.writer(my_csv,delimiter=',')
        csvWriter.writerows(final_data)
    print("file saved")
    

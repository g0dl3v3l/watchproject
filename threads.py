import pyqtgraph as pg
from pyqtgraph.Qt import QtGui, QtCore
from PyQt5.QtWidgets import QApplication
import numpy as np
import socket
import json
import struct
from pyqtgraph.Qt.QtCore import QThread, QTimer, QDateTime

class DataThread(QThread):
    def __init__(self, parent=None):
        super().__init__(parent)
        self.x_xdata = []
        self.x_ydata = []
        self.ydata = [0]
        self.times = []
        self.server_name = "192.168.0.100"
        self.server_port = 5000
        self.server_address = (self.server_name, self.server_port)
        print('Setting up server on:', self.server_address)
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.bind(self.server_address)
        self.server_socket.listen(1)
        print('Waiting for a client connection...')
        self.connection, self.client_address = self.server_socket.accept()
        print('Connected to:', self.client_address)
    
    def run(self):
        while True:
            length_bytes = self.connection.recv(4)
            if not length_bytes:
                break
            length = struct.unpack('>I', length_bytes)[0]
            msg = self.connection.recv(length)
            print("Length received:", length)
            if not msg:
                break
            try:
                packet = json.loads(msg.decode("utf-8"))
                x_x = float(packet["x_val"])
                x_y = float(packet["y_val"])
                
            except Exception as e:
                print("Exception occurred", e)
                continue
        
            self.x_xdata.append(x_x)
            self.x_ydata.append(x_y)
            self.times.append(QDateTime.currentMSecsSinceEpoch())

class GraphWindow(pg.GraphicsLayoutWidget):
    def __init__(self):
        super().__init__(title="Signal from socket connection")
        # self.plot = self.plotItem
        # self.curve = self.plot.plot(pen='r')
        self.plot1 = self.addPlot(title="accelerometer x axis")
        self.plot2 = self.addPlot(title="accelerometer y axis")
        self.curve1 = self.plot1.plot(pen='r')
        self.curve2 = self.plot2.plot(pen='b')
        self.data_thread = DataThread()
        self.timer = QTimer()
        self.timer.timeout.connect(self.update)
        self.timer.start(50)
        self.data_thread.start()
        self.start_time = QDateTime.currentMSecsSinceEpoch()

    def update(self):
      
        x = (np.array(self.data_thread.times) - self.start_time)
        y_x = np.array(self.data_thread.x_ydata)
        x_x = np.array(self.data_thread.x_xdata)
        #print(len(y))
        #self.curve.setData(self.data_thread.xdata)
        self.curve1.setData(x=x, y=x_x)
        self.curve2.setData(x=x, y=y_x)
 
        #self.plot.setXRange(self.data_thread.xdata[-1]-5, self.data_thread.xdata[-1]+5)
        
 
    # def save_plots(self, filename):
    #     img = pg.exporters.ImageExporter(self.scene())
    #     img.export(filename)
if __name__ == '__main__':
    app = QApplication([])
    win = GraphWindow()
    win.show()
    app.exec_()

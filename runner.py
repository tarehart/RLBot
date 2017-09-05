from ctypes import *
from ctypes.wintypes import *
from multiprocessing import Process, Array, Queue
from gametick import GameTickPacket_pb2

import time
import atexit
import socket
import realTimeDisplay
import PlayHelper
import AlwaysTowardsBallAgent as agent1
import AlwaysTowardsBallAgent as agent2

TCP_IP = '127.0.0.1'
TCP_PORT = 35401
BUFFER_SIZE = 1024


OpenProcess = windll.kernel32.OpenProcess
CloseHandle = windll.kernel32.CloseHandle

ph = PlayHelper.play_helper()

def updateInputs(inputs, scoring, ph, conn):

	while(True):

		data = conn.recv(BUFFER_SIZE)
		packet = GameTickPacket_pb2.GameTickPacket()
		packet.ParseFromString(data)
		conn.send(b'\x00') # Send back an arbitrary byte. Don't know if it matters

		values = ph.GetValueVector(packet)

		# Finally update input to values
		for i in range(len(values[0])):
			inputs[i] = values[0][i]
		for i in range(len(values[1])):
			scoring[i] = values[1][i]
		time.sleep(0.01)
		
def resetInputs():
	exec(open("resetDevices.py").read())

def runAgent(inputs, scoring, team, q):
	# Deep copy inputs?
	if team == "blue":
		agent = agent1.agent("blue")
	else:
		agent = agent2.agent("orange")
	while(True):
		output = agent.get_output_vector((inputs,scoring))
		try:
			q.put(output)
		except Queue.Full:
			pass
		time.sleep(0.01)
			
if __name__ == '__main__':
	# Make sure input devices are reset to neutral whenever the script terminates
	atexit.register(resetInputs)

	inputs = Array('f', [0.0 for x in range(38)])
	scoring = Array('f', [0.0 for x in range(12)])
	q1 = Queue(1)
	q2 = Queue(1)
	
	output1 = [16383, 16383, 32767, 0, 0, 0, 0]
	output2 = [16383, 16383, 32767, 0, 0, 0, 0]
	
	rtd = realTimeDisplay.real_time_display()
	rtd.build_initial_window(agent1.BOT_NAME, agent2.BOT_NAME)

	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	s.bind((TCP_IP, TCP_PORT))
	s.listen(1)

	print("Waiting for rocket league to connect...")
	conn, addr = s.accept()
	print("Connected!")
	
	ph = PlayHelper.play_helper()
	
	p1 = Process(target=updateInputs, args=(inputs, scoring, ph, conn))
	p1.start()
	p2 = Process(target=runAgent, args=(inputs, scoring, "blue", q1))
	p2.start()
	p3 = Process(target=runAgent, args=(inputs, scoring, "orange", q2))
	p3.start()
	
	while (True):
		updateFlag = False
		
		rtd.UpdateDisplay((inputs,scoring))
		
		try:
			output1 = q1.get()
			updateFlag = True
		except Queue.Empty:
			pass
			
		try:
			output2 = q2.get()
			updateFlag = True
		except Queue.Empty:
			pass
		
		if (updateFlag):
			ph.update_controllers(output1, output2)
		
		rtd.UpdateKeyPresses(output1, output2)
		time.sleep(0.01)
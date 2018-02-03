from grpcsupport import proto_converter
from grpcsupport.protobuf import game_data_pb2_grpc
import grpc
import time

####################################################################################################
# To run this agent successfully, you need to:
# - install the python package "grpcio", e.g. pip install grpcio
# - have the ReliefBot grpc server running at the same time
####################################################################################################

class Agent:
    def __init__(self, name, team, index):
        self.name = name
        self.team = team  # use self.team to determine what team you are. I will set to "blue" or "orange"
        self.index = index
        self.stub = None
        self.myPort = '22868'
        self.connected = False

        try:
            with open("reliefbot-port.txt", "r") as portFile:
                self.myPort = portFile.readline()
        except ValueError:
            print("Failed to parse port file! Will proceed with hard-coded port number.")
        except:
            pass

        try:
            print("Will connect to ReliefBot grpc server on port " + self.myPort + '...')
            channel = grpc.insecure_channel('localhost:' + self.myPort)
            self.stub = game_data_pb2_grpc.BotStub(channel)
        except Exception as e:
            print("Exception when trying to connect to ReliefBot grpc server: " + str(e))
            pass

    def get_output_vector(self, game_tick_packet):

        proto = proto_converter.convert_game_tick(game_tick_packet, self.index)

        try:
            controller_state = self.stub.GetControllerState(proto, timeout = 1)

            if not self.connected:
                print("Connected to grpc server successfully!")
                self.connected = True

            return [
                controller_state.throttle,
                controller_state.steer,
                controller_state.pitch,
                controller_state.yaw,
                controller_state.roll,
                controller_state.jump,
                controller_state.boost,
                controller_state.handbrake
            ]
        except Exception as e:
            print("Exception when calling grpc server: " + str(e))
            print("Will try again in a few seconds...")
            self.connected = False
            time.sleep(4)

            return [0, 0, 0, 0, 0, 0, 0, 0]  # No motion

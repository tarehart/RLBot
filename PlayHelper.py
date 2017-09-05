import array
import pyvjoy
from math import sin
from math import cos


def transform_unit_x(pitch, yaw, roll):
    x = cos(yaw) * cos(pitch)
    y = sin(yaw) * cos(pitch)
    z = sin(pitch)
    return (x, y, z)

def transform_unit_y(pitch, yaw, roll):
    x = sin(roll) * sin(pitch) * cos(yaw) - cos(roll) * sin(yaw)
    y = sin(roll) * sin(pitch) * sin(yaw) + cos(roll) * cos(yaw)
    z = -sin(roll) * cos(pitch)
    return (x, y, z)

def transform_unit_z(pitch, yaw, roll):
    x = -(cos(roll) * sin(pitch) * cos(yaw) + sin(roll) * sin(yaw))
    y = cos(yaw) * sin(roll) - cos(roll) * sin(pitch) * sin(yaw)
    z = cos(roll) * cos(pitch)
    return (x, y, z)

class play_helper:

    def GetValueVector(self, packet):
        neuralInputs = array.array('f',(0,)*38) # Create a tuple with 38 float values
        scoring = array.array('f',(0,)*12) # Create a tuple with 12 float values
        # Need to read 28 values for neural inputs and calculate 9 velocities

        blueCar = None
        orangeCar = None

        for car in packet.gamecars:
            if car.Team == "blue":
                blueCar = car
            elif car.Team == "orange":
                orangeCar = car

        ball = packet.gameball

        neuralInputs[0] = blueCar.Boost / 300
        neuralInputs[1] = blueCar.Location.y
        neuralInputs[2] = ball.Location.y
        neuralInputs[3] = orangeCar.Location.y
        neuralInputs[4] = blueCar.Location.z
        neuralInputs[5] = blueCar.Location.x
        neuralInputs[6] = ball.Location.z
        neuralInputs[7] = ball.Location.x

        blueNose = transform_unit_x(blueCar.Rotation.x, blueCar.Rotation.y, blueCar.Rotation.z)
        neuralInputs[8] = blueNose[0]
        neuralInputs[11] = blueNose[1]
        neuralInputs[14] = blueNose[2]

        blueRoof = transform_unit_z(blueCar.Rotation.x, blueCar.Rotation.y, blueCar.Rotation.z)
        neuralInputs[10] = blueRoof[0]
        neuralInputs[13] = blueRoof[1]
        neuralInputs[16] = blueRoof[2]


        neuralInputs[17] = orangeCar.Location.z
        neuralInputs[18] = orangeCar.Location.x

        orangeNose = transform_unit_x(orangeCar.Rotation.x, orangeCar.Rotation.y, orangeCar.Rotation.z)
        neuralInputs[19] = orangeNose[0]
        neuralInputs[22] = orangeNose[1]
        neuralInputs[25] = orangeNose[2]

        orangeRoof = transform_unit_z(orangeCar.Rotation.x, orangeCar.Rotation.y, orangeCar.Rotation.z)
        neuralInputs[21] = orangeRoof[0]
        neuralInputs[24] = orangeRoof[1]
        neuralInputs[27] = orangeRoof[2]

        # neuralInputs[28] = blueCar.Velocity.y
        # neuralInputs[29] = blueCar.Velocity.y
        # neuralInputs[30] = blueCar.Velocity.y

        neuralInputs[31] = ball.Velocity.x
        neuralInputs[32] = ball.Velocity.z
        neuralInputs[33] = ball.Velocity.y

        # neuralInputs[34] = orangeCar.Velocity.y
        # neuralInputs[35] = orangeCar.Velocity.y
        # neuralInputs[36] = orangeCar.Velocity.y

        neuralInputs[37] = orangeCar.Boost / 300

        # Missing: 9, 12, 15, 20, 23, 26


        # Also create tuple of scoring changes/demos so I can know when reset is necessary
        scoring[0] = float(blueCar.Score.Goals) # Blue Score - This may be a breaking change, because we will no longer count non-attributed goals.
        scoring[1] = float(orangeCar.Score.Goals) # Orange Score - This may be a breaking change, because we will no longer count non-attributed goals.
        scoring[2] = float(orangeCar.Score.Demolitions) # Demos on blue, by orange
        scoring[3] = float(blueCar.Score.Demolitions) # Demos on orange, by blue
        scoring[4] = float(blueCar.Score.Score)
        scoring[5] = float(orangeCar.Score.Score)
        scoring[6] = float(blueCar.Score.Goals)
        scoring[7] = float(blueCar.Score.Saves)
        scoring[8] = float(blueCar.Score.Shots)
        scoring[9] = float(orangeCar.Score.Goals)
        scoring[10] = float(orangeCar.Score.Saves)
        scoring[11] = float(orangeCar.Score.Shots)

        return neuralInputs, scoring



    def reset_contollers(self):
        p1 = pyvjoy.VJoyDevice(1)
        p2 = pyvjoy.VJoyDevice(2)

        p1.data.wAxisX = 16383
        p1.data.wAxisY = 16383
        p1.data.wAxisYRot = 16383
        p1.data.wAxisXRot = 16383
        p1.data.wAxisZ = 0
        p1.data.wAxisZRot = 0
        p1.data.lButtons = 0

        p2.data.wAxisX = 16383
        p2.data.wAxisY = 16383
        p2.data.wAxisYRot = 16383
        p2.data.wAxisXRot = 16383
        p2.data.wAxisZ = 0
        p2.data.wAxisZRot = 0
        p2.data.lButtons = 0

        #send data to vJoy device
        p1.update()
        p2.update()
        
    def update_controllers(self, output1, output2):
        # Update controller buttons for both players

        # TODO: Sanitize input players give
        p1 = pyvjoy.VJoyDevice(1)
        p2 = pyvjoy.VJoyDevice(2)

        p1.data.wAxisX = output1[0]
        p2.data.wAxisX = output2[0]

        p1.data.wAxisY = output1[1]
        p2.data.wAxisY = output2[1]

        p1.data.wAxisZRot = output1[2]
        p2.data.wAxisZRot = output2[2]

        p1.data.wAxisZ = output1[3]
        p2.data.wAxisZ = output2[3]

        p1.data.lButtons = (1 * output1[4]) + (2 * output1[5]) + (4 * output1[6])
        p2.data.lButtons = (1 * output2[4]) + (2 * output2[5]) + (4 * output2[6])

        p1.data.wAxisXRot = 16383
        p2.data.wAxisXRot = 16383

        p1.data.wAxisYRot = 16383
        p2.data.wAxisYRot = 16383

        #send data to vJoy device
        p1.update()
        p2.update()

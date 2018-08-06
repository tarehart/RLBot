from Physics import *


def fV3a(V):
    return np.array([V.X(), V.Y(), V.Z()])


def fRa(R):
    return np.array([R.Pitch(), R.Yaw(), R.Roll()])


def pre_process(s, packet):

    player = packet.Players(s.index)
    ball = packet.Ball()
    info = packet.GameInfo()

    s.time = info.SecondsElapsed()
    s.bH = info.IsKickoffPause()

    s.pL = fV3a(player.Physics().Location())
    s.pR = fRa(player.Physics().Rotation()) * U / pi
    s.pV = fV3a(player.Physics().Velocity())
    s.paV = fV3a(player.Physics().AngularVelocity())
    s.pJ = player.Jumped()
    s.pdJ = player.DoubleJumped()
    s.poG = player.HasWheelContact()
    s.pB = player.Boost()
    s.pS = player.IsSupersonic()

    s.bL = fV3a(ball.Physics().Location())
    s.bV = fV3a(ball.Physics().Velocity())
    s.baV = fV3a(ball.Physics().AngularVelocity())

    s.bx, s.by, s.bz = local(s.bL, s.pL, s.pR)
    s.bd, s.ba, s.bi = spherical(s.bx, s.by, s.bz)
    s.iv, s.rv, s.av = local(s.paV, z3, s.pR)
    s.pxv, s.pyv, s.pzv = local(s.pV, z3, s.pR)
    s.bxv, s.byv, s.bzv = local(s.bV, z3, s.pR)
    s.pvd, s.pva, s.pvi = spherical(s.pxv, s.pyv, s.pzv)

    s.color = -sign(player.Team())

    if not hasattr(s, 'counter'):

        s.counter = -1

        s.throttle = s.steer = s.pitch = s.yaw = s.roll = s.jump = s.boost = 0
        s.powerslide = s.ljump = 0

        s.aT = s.gT = s.sjT = s.djT = s.time

        s.goal = a3([0, 5180 * s.color, 0])
        s.ogoal = a3([0, -5180 * s.color, 0])

        s.a = s.i = s.dT = 0
        s.dodge = s.jumper = s.shoot = 0
        s.brakes = 1
        s.tL = s.bL
        s.djL = 'bL'

        feedback(s)

    if s.poG and not s.lpoG:
        s.gT = s.time
    if s.lpoG and not s.poG:
        s.aT = s.time

    s.airtime = s.time - s.aT
    s.gtime = s.time - s.gT
    s.djtime = s.time - s.djT

    if s.lljump and not s.ljump or s.airtime > 0.2:
        s.sjT = s.ltime

    s.sjtime = s.time - s.sjT  # second jump timer

    if s.poG:
        s.airtime = s.sjtime = s.djtime = 0
    else:
        s.gtime = 0

    if s.poG:
        s.jcount = 2
    elif s.pdJ or (s.sjtime > 1.25 and s.pJ):
        s.jcount = 0
    else:
        s.jcount = 1

    if s.jcount == 0 or s.poG:
        s.dodge = s.jumper = 0

    s.dtime = s.time - s.ltime
    if s.dtime != 0:
        s.fps = 1 / s.dtime
    else:
        s.fps = 0

    oppIndex = not s.index

    if packet.PlayersLength() > 2:
        s.oppIndex = -1
        for i in range(packet.PlayersLength()):
            if packet.Players(i).Team() != player.Team():
                L = fV3a(packet.Players(i).Physics().Location())
                cL = fV3a(packet.Players(oppIndex).Physics().Location())
                if s.oppIndex == -1 or (d3(L, s.bL) < d3(cL, s.bL)):
                    s.oppIndex = i

    if oppIndex < packet.PlayersLength():

        opp = packet.Players(oppIndex)
        s.oL = fV3a(opp.Physics().Location())
        s.oV = fV3a(opp.Physics().Velocity())
        s.oR = fRa(opp.Physics().Rotation()) * U / pi

    else:
        s.oL = s.oV = s.oR = z3


def gather_info(s):

    gy = 5180

    # player info

    s.pdT = Range(d3(s.pL + s.pV / 60, s.bL + s.bV / 60) / 2500, 5)

    s.ptL = step(s.bL, s.bV, s.baV, s.pdT)[0]
    s.pfL = step(s.pL, s.pV, z3, s.pdT)[0]

    s.glinex = line_intersect(([0, gy * s.color], [1, gy * s.color]),
                              ([s.pL[0], s.pL[1]], [s.ptL[0], s.ptL[1]]))[0]

    s.glinez = line_intersect(([0, gy * s.color], [1, gy * s.color]),
                              ([s.pL[2], s.pL[1]], [s.ptL[2], s.ptL[1]]))[0]

    s.oglinex = line_intersect(([0, -gy * s.color], [1, -gy * s.color]),
                               ([s.pL[0], s.pL[1]], [s.ptL[0], s.ptL[1]]))[0]

    s.oglinez = line_intersect(([0, -gy * s.color], [1, -gy * s.color]),
                               ([s.pL[2], s.pL[1]], [s.ptL[2], s.ptL[1]]))[0]

    s.bfd = d3(s.pfL, s.ptL)

    # opponnent info

    s.odT = Range(d3(s.oL + s.oV / 60, s.bL + s.bV / 60) / 2500, 5)

    s.otL = step(s.bL, s.bV, s.baV, s.odT)[0]
    s.ofL = step(s.pL, s.pV, z3, s.odT)[0]

    s.ooglinex = line_intersect(([0, -gy * s.color], [1, -gy * s.color]),
                                ([s.oL[0], s.oL[1]], [s.otL[0], s.otL[1]]))[0]

    s.ooglinez = line_intersect(([0, -gy * s.color], [1, -gy * s.color]),
                                ([s.oL[2], s.oL[1]], [s.otL[2], s.otL[1]]))[0]

    s.obd = d3(s.oL, s.bL)
    s.obfd = d3(s.ofL, s.otL)

    # other

    s.goal = a3([-Range(s.glinex, 550), gy * s.color, 150])

    s.ogoal = a3([Range(s.ooglinex, 900), -gy * s.color,
                  Range(s.ooglinez * .25, 650)])

    s.gaimdx = abs(s.goal[0] - s.glinex)
    s.gaimdz = abs(s.goal[2] - s.glinez)

    s.gx, s.gy, s.gz = local(s.goal, s.pL, s.pR)
    s.gd, s.ga, s.gi = spherical(s.gx, s.gy, s.gz)

    s.ogx, s.ogy, s.ogz = local(s.ogoal, s.pL, s.pR)
    s.ogd, s.oga, s.ogi = spherical(s.ogx, s.ogy, s.ogz)

    s.ogtd = d3(s.ogoal, s.tL)
    s.ogpd = d3(s.ogoal, s.pL)

    near_post = (gx / 2 * sign(s.bL[0]), wy / 2 * s.color)

    near_post_distance = d2(near_post, [s.bL[0], s.bL[1]])

    post_angle = math.acos(R / near_post_distance)

    ball_angle = math.atan2(s.bL[1] - near_post[1], s.bL[0] - near_post[0])

    tangent_angle = (ball_angle - post_angle * s.color * sign(s.bL[0]))

    tangent_x = math.cos(tangent_angle) * R + near_post[0]
    tangent_y = math.sin(tangent_angle) * R + near_post[1]

    s.xpoint = line_intersect([(1, near_post[1]), (-1, near_post[1])],
                              [(s.bL[0], s.bL[1]), (tangent_x, tangent_y)])[0]


def feedback(s):

    s.lpoG = s.poG
    s.ltime = s.time
    s.lljump = s.ljump
    s.ljump = s.jump

    s.counter += 1

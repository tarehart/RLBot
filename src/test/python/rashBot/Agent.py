from rlbot.agents.base_flatbuffer_agent import BaseFlatbufferAgent
from rlbot.agents.base_flatbuffer_agent import SimpleControllerState
from rlbot.messages.flat import GameTickPacket

import Bot


class FlatBot(BaseFlatbufferAgent):

    def get_output(self, packet: GameTickPacket) -> SimpleControllerState:
        return Bot.Process(self, packet, 4)

from rlbot.agents.base_agent import BaseAgent, SimpleControllerState
from rlbot.utils.structures.game_data_struct import GameTickPacket


class ObserverBot(BaseAgent):

    def get_output(self, game_tick_packet: GameTickPacket) -> SimpleControllerState:

        car_to_watch = game_tick_packet.game_cars[0]
        for packet_car in game_tick_packet.game_cars[:game_tick_packet.num_cars]:
            if packet_car.team != self.team:
                car_to_watch = packet_car
                break

        # TODO: trace whatever you want
        return SimpleControllerState()

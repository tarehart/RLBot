package tarehart.rlbot.hoops.tacticalstate

import rlbot.cppinterop.RLBotDll
import rlbot.flat.QuickChatSelection
import rlbot.render.NamedRenderer
import tarehart.rlbot.AgentOutput
import tarehart.rlbot.TacticalBundle
import tarehart.rlbot.hoops.HoopsZone
import tarehart.rlbot.hoops.HoopsZoneTeamless
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.Plan
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.routing.BoostAdvisor
import tarehart.rlbot.steps.blind.BlindStep
import tarehart.rlbot.steps.GetBoostStep
import tarehart.rlbot.steps.WhileConditionStep
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.steps.strikes.MidairStrikeStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.ui.DisplayFlags
import java.awt.Color
import java.util.function.Predicate

class KickoffState : TacticalState {

    enum class KickoffStyle {
        // The kickoff has not yet been determined
        INDETERMINATE,
        // Normal kickoff from the center position
        CENTER,
        // An assisted kickoff from center position
        // The bot's job is to bump whoever is in the forward position
        CENTER_ASSIST,
        // The bot is in the center position and will take up a defensive role
        CENTER_DEFEND,
        // Normal kickoff from the forward location
        FORWARD_STANDARD,
        // Assisted kickoff from someone in the forward or center position
        FORWARD_BOOSTED,
        // Rotate to the defend location
        FORWARD_DEFEND,
        // Make an assisted kickoff from the forward location
        // The bot will bump the other player in the adjacent forward location
        FORWARD_ASSIST,
        // The bot is in the wide position and will go for kickoff
        WIDE_KICKOFF,
        // The bot is in the wide position and will defend
        WIDE_DEFEND
    }

    private var kickoffHasBegun = false
    private var kickoffRenderer: NamedRenderer? = null
    private var kickoffStyle = KickoffStyle.INDETERMINATE


    override fun muse(bundle: TacticalBundle): TacticalState {
        val input = bundle.agentInput
        if (!input.ballPosition.flatten().isZero || input.myCarData.position.z > 5) {
            return IdleState()
        } else if (input.ballPosition.flatten().isZero && input.ballVelocity.isZero && kickoffHasBegun) {
            println("Kickoff restarted")
            // The kickoff has restarted
            return KickoffState()
        }

        if (kickoffRenderer == null && !kickoffHasBegun) {
            kickoffRenderer = NamedRenderer("hoopsKickoffRenderer${input.bot.index}")
        }

        if (!kickoffHasBegun && input.ballPosition.z > 2) {
            kickoffHasBegun = true
            println("Kickoff has begun")
        }

        if (kickoffRenderer != null) {
            if (kickoffHasBegun) {
                kickoffRenderer!!.eraseFromScreen()
                kickoffRenderer = null
            } else {
                kickoffRenderer!!.startPacket()
                for (zone in HoopsZone.values()) {
                    if(DisplayFlags[DisplayFlags.HOOPS_KICKOFF] == 1) {
                        if (zone.toString().contains("ORANGE")) {
                            kickoffRenderer!!.drawRectangle3d(Color.ORANGE, zone.center.toRlbot(), 10, 10, true)
                        } else if (zone == HoopsZone.KICKOFF_BLUE_FORWARD_LEFT) {
                            kickoffRenderer!!.drawRectangle3d(Color.BLUE, zone.center.toRlbot(), 10, 10, true)
                        }
                    }
                }
                kickoffRenderer!!.finishAndSend()
            }
        }

        if (kickoffStyle == KickoffStyle.INDETERMINATE) {
            determineKickoffStyle(bundle)
            if (kickoffStyle in arrayOf(KickoffStyle.CENTER, KickoffStyle.FORWARD_STANDARD, KickoffStyle.WIDE_KICKOFF)) {
                RLBotDll.sendQuickChat(input.playerIndex, false, QuickChatSelection.Information_IGotIt)
            } else if (kickoffStyle != KickoffStyle.INDETERMINATE){
                RLBotDll.sendQuickChat(input.playerIndex, false, QuickChatSelection.Information_AllYours)
            }
            print("Kickoff Style")
            println(kickoffStyle.toString())
        }

        return this
    }

    private fun determineKickoffStyle(bundle: TacticalBundle) {

        val input = bundle.agentInput
        val situation = bundle.tacticalSituation

        // The ordinal value of HoopsZone is in kickoff priority order, I was going to use that
        // But its late and I have a matrix of who goes for kickoff in my notebook, so I'm doing that.

        val myKickoffPosition = HoopsZone.getZone(input.myCarData.position) ?: return
        val myTeamCar = input.getTeamRoster(input.team).find { it.playerIndex != input.playerIndex }

        // I just discovered the Elvis operator, and he is glorious.
        val myTeamKickoffPosition = myTeamCar?.let {
            HoopsZone.getZone(it.position)
        } ?: return {
            // No team member, so use default kickoffs
            if (myKickoffPosition in HoopsZoneTeamless.WIDE_LEFT || myKickoffPosition in HoopsZoneTeamless.WIDE_RIGHT) {
                kickoffStyle = KickoffStyle.WIDE_KICKOFF
            }
            else if (myKickoffPosition in HoopsZoneTeamless.FORWARD_LEFT || myKickoffPosition in HoopsZoneTeamless.FORWARD_RIGHT) {
                kickoffStyle = KickoffStyle.FORWARD_STANDARD
            }
            else if (myKickoffPosition in HoopsZoneTeamless.CENTER) {
                kickoffStyle = KickoffStyle.CENTER
            }
        }()

        // TODO Kotlin is cool and all, and using the contains operator is neat, but it is perhaps suboptimal.
        // You should consider evaluating the Teamless enum once, and then comparing


        if (myKickoffPosition in HoopsZoneTeamless.WIDE_LEFT) {
            // We defend unless our team is on the WIDE_RIGHT position
            if (myTeamKickoffPosition in HoopsZoneTeamless.WIDE_RIGHT) {
                kickoffStyle = KickoffStyle.WIDE_KICKOFF
            } else {
                kickoffStyle = KickoffStyle.WIDE_DEFEND
            }
        } else if (myKickoffPosition in HoopsZoneTeamless.FORWARD_LEFT) {
            // Forward left is always standard unless our team mate proposes we boogie.
            // TODO Quick Chat provides FORWARD_BOOSTED
            kickoffStyle = KickoffStyle.FORWARD_STANDARD
        } else if (myKickoffPosition in HoopsZoneTeamless.CENTER) {
            // If our team is in a worse position than us, we will go for kickoff
            if (myKickoffPosition.hasAdvantageAgainst(myTeamKickoffPosition)) {
                kickoffStyle = KickoffStyle.CENTER
            } else {
                kickoffStyle = KickoffStyle.CENTER_DEFEND
            }
            // TODO When we get quick chat support we can implement CENTER_ASSIST
        } else if (myKickoffPosition in HoopsZoneTeamless.FORWARD_RIGHT) {
            if (myKickoffPosition.hasAdvantageAgainst(myTeamKickoffPosition)) {
                kickoffStyle = KickoffStyle.FORWARD_STANDARD
            } else {
                kickoffStyle = KickoffStyle.FORWARD_DEFEND
            }
            // TODO Quick Chat provides FORWARD_ASSIST and FORWARD_BOOSTED
        } else if (myKickoffPosition in HoopsZoneTeamless.WIDE_RIGHT) {
            // WIDE_RIGHT is always a defensive position, unless we are solo (handled above)
            kickoffStyle = KickoffStyle.WIDE_DEFEND
        } else {
            println("WARN: I don't know what kickoff we are doing.")
        }
    }

    override fun urgentPlan(bundle: TacticalBundle, currentPlan: Plan?) : Plan?{
        if(!kickoffHasBegun) {
            return Plan()
        }
        return null
    }

    override fun newPlan(bundle: TacticalBundle) : Plan {
        val input = bundle.agentInput
        if (kickoffHasBegun) {
            if (kickoffStyle == KickoffStyle.CENTER) {
                return Plan(Plan.Posture.KICKOFF)
                        .withStep(BlindStep(Duration.ofMillis(800), AgentOutput().withThrottle(1.0).withBoost(true)))
                        .withStep(BlindStep(Duration.ofMillis(200), AgentOutput().withThrottle(1.0).withPitch(1.0).withBoost(true).withJump(true)))
                        .withStep(BlindStep(Duration.ofMillis(16), AgentOutput().withThrottle(1.0).withPitch(1.0).withBoost(true).withJump(false)))
                        .withStep(BlindStep(Duration.ofMillis(16), AgentOutput().withThrottle(1.0).withPitch(0.0).withBoost(true).withJump(true)))
                        .withStep(MidairStrikeStep(Duration.ofMillis(0), hasJump = false))
            } else if (kickoffStyle == KickoffStyle.FORWARD_STANDARD) {
                return Plan(Plan.Posture.KICKOFF)
                        .withStep(BlindStep(Duration.ofMillis(600), SteerUtil.steerTowardGroundPosition(input.myCarData, input.ballPosition).withBoost(false)))
                        .withStep(BlindStep(Duration.ofMillis(200), AgentOutput().withThrottle(1.0).withPitch(1.0).withBoost(false).withJump(true)))
                        .withStep(BlindStep(Duration.ofMillis(20), AgentOutput().withThrottle(1.0).withPitch(0.5).withBoost(true).withJump(false)))
                        .withStep(BlindStep(Duration.ofMillis(16), AgentOutput().withThrottle(1.0).withPitch(0.0).withBoost(true).withJump(true)))
                        .withStep(MidairStrikeStep(Duration.ofMillis(0), hasJump = false))
            } else if (kickoffStyle == KickoffStyle.WIDE_KICKOFF) {
                val closestSmallBoost = BoostAdvisor.boostData.smallBoosts.sortedBy { it.location.distance(input.myCarData.position) }.first()

                if(DisplayFlags[DisplayFlags.HOOPS_KICKOFF] == 1) {
                    val boostRenderer = NamedRenderer("kickoffTargetRenderer")
                    boostRenderer.startPacket()
                    boostRenderer.drawRectangle3d(Color.WHITE, closestSmallBoost.location.toRlbot(), 15, 15, true)
                    boostRenderer.finishAndSend()
                }

                return Plan()
                        .withStep(WhileConditionStep(Predicate {
                            closestSmallBoost.location.distance(it.agentInput.myCarData.position) > 2.5
                        }, {
                            val guidance = (closestSmallBoost.location + it.agentInput.ballPosition) / 2.0
                            SteerUtil.steerTowardGroundPosition(it.agentInput.myCarData, guidance)
                        }))
                        .withStep(BlindStep(Duration.ofMillis(20), SteerUtil.steerTowardGroundPosition(input.myCarData, input.ballPosition).withBoost(true)))
                        .withStep(BlindStep(Duration.ofMillis(200), AgentOutput().withThrottle(1.0).withPitch(1.0).withBoost(true).withJump(true)))
                        .withStep(BlindStep(Duration.ofMillis(20), AgentOutput().withThrottle(1.0).withPitch(0.5).withBoost(true).withJump(false)))
                        .withStep(BlindStep(Duration.ofMillis(16), AgentOutput().withThrottle(1.0).withPitch(0.0).withBoost(true).withJump(true)))
                        .withStep(MidairStrikeStep(Duration.ofMillis(0), hasJump = false))

            } else if (kickoffStyle == KickoffStyle.FORWARD_DEFEND) {
                return Plan()
                        .withStep(GetBoostStep())
                        /*
                        .withStep(ParkTheCarStep({
                            it.ballPosition
                        }))
                        .withStep(WhileConditionStep(Predicate {
                            it.ballVelocity.flatten().isZero
                        }), )
                        */
            } else {
                return Plan().withStep(InterceptStep())
            }
        }
        return Plan().withStep(BlindStep(Duration.ofMillis(16), AgentOutput()))
    }
}

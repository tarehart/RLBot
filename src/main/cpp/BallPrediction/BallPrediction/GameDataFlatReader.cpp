
#define NOMINMAX
#include <algorithm>
#define _USE_MATH_DEFINES
#include <math.h>
#include "flat\rlbot_generated.h"
#include "..\BallPrediction\PredictionService.hpp"

/*
Note: The code in this file looks goofy because flatbuffers requires us to construct things 'pre-order',
i.e. you have to fully create all the sub-objects before you start creating a parent object.

https://github.com/google/flatbuffers/blob/master/samples/sample_binary.cpp

*/
namespace GameDataFlatReader
{
	flatbuffers::Offset<rlbot::flat::GameInfo> createGameInfo(flatbuffers::FlatBufferBuilder* builder)
	{
		AWorldInfo* pWI = InstanceManager::GetWorldInfo();
		AGameEvent_Soccar_TA* pGES = InstanceManager::GetGameEvent();

		return rlbot::flat::CreateGameInfo(
			*builder, 
			pWI->TimeSeconds, 
			pGES->GameTimeRemaining, 
			pGES->bOverTime, 
			pGES->bUnlimitedTime, 
			pGES->bRoundActive, 
			!pGES->bBallHasBeenHit, 
			pGES->bMatchEnded);
	}

	rlbot::flat::Vector3 createVector3(Vector3 structVec) {
		return rlbot::flat::Vector3(structVec.X, structVec.Y, structVec.Z);
	}

	float convertURot(int rotation) {
		return rotation * M_PI / 32768;
	}

	rlbot::flat::Rotator createRotator(Rotator structRot) {

		return rlbot::flat::Rotator(
			convertURot(structRot.Pitch),
			convertURot(structRot.Yaw),
			convertURot(structRot.Roll));
	}

	flatbuffers::Offset<rlbot::flat::BoostPad> createBoostPad(flatbuffers::FlatBufferBuilder* builder, AVehiclePickup_Boost_TA* gameBoost)
	{
		auto location = createVector3(gameBoost->Location);
		return rlbot::flat::CreateBoostPad(*builder, &location, gameBoost->BoostAmount > 99);
	}

	flatbuffers::Offset<rlbot::flat::BoostPadState> createBoostPadState(flatbuffers::FlatBufferBuilder* builder, AVehiclePickup_Boost_TA* gameBoost)
	{
		float timer = 0;
		if (gameBoost->bPickedUp && gameBoost->Timers.Num() > 0)
		{
			timer = gameBoost->Timers[0].Count;
		}
			
		return rlbot::flat::CreateBoostPadState(*builder, !gameBoost->bPickedUp, timer);
	}

	int boostSortValue(const AVehiclePickup_TA* boost)
	{
		return boost->Location.Y * 100 + boost->Location.X;
	}

	bool compareBoostLocations(const AVehiclePickup_TA* first, const AVehiclePickup_TA* second) 
	{
		return boostSortValue(first) < boostSortValue(second);
	}

	void fillBoostList(std::vector<AVehiclePickup_Boost_TA*>* boostList) {
		UGameShare_TA* pGS = InstanceManager::GetGameShare();
		if (pGS && pGS->VehiclePickupList && pGS->VehiclePickupList->Objects.GetData())
		{
			for (int i = 0; i < pGS->VehiclePickupList->Objects.Num(); i++)
			{
				AVehiclePickup_TA* pVP = (AVehiclePickup_TA*)pGS->VehiclePickupList->Objects[i];
				if (pVP && pVP->IsA(AVehiclePickup_Boost_TA::StaticClass()) && !pVP->bHidden && !pVP->bDeleteMe)
				{
					boostList->push_back((AVehiclePickup_Boost_TA*)pVP);
				}
			}

			std::sort(boostList->begin(), boostList->end(), compareBoostLocations);
		}
	}

	bool FillFieldInfo(flatbuffers::FlatBufferBuilder* builder)
	{
		AGameEvent_Soccar_TA* pGES = InstanceManager::GetGameEvent();
		if (!pGES)
		{
			return false;
		}

		bool filled;

		std::vector<AVehiclePickup_Boost_TA*> boostList;
		fillBoostList(&boostList);

		std::vector<flatbuffers::Offset<rlbot::flat::BoostPad>> boostPads;

		for (int i = 0; i < boostList.size(); i++) {

			boostPads.push_back(createBoostPad(builder, boostList[i]));
		}

		std::vector<flatbuffers::Offset<rlbot::flat::GoalInfo>> goals;

		APylon_Soccar_TA* pPS = pGES->Pylon;

		if (pPS)
		{
			// fieldInfo.FieldOrientation = toRotStruct(pPS->FieldOrientation);
			// fieldInfo.FieldSize = toVecStruct(pPS->FieldSize);
			// fieldInfo.FieldExtent = toVecStruct(pPS->FieldExtent);
			// fieldInfo.FieldCenter = toVecStruct(pPS->FieldCenter);
			// fieldInfo.GroundZ = pPS->GroundZ;

			if (pPS->Goals.Num() >= 2)
			{

				for (int i = 0; i < 2; i++)
				{
					UGoal_TA* pGoal = pPS->Goals[i];
					auto location = createVector3(pGoal->Location);
					auto direction = createVector3(pGoal->Direction);
					goals.push_back(rlbot::flat::CreateGoalInfo(*builder, pGoal->TeamNum, &location, &direction));

					filled = true;

					// goal.Rotation = toRotStruct(pGoal->Rotation);
					// goal.LocalMin = toVecStruct(pGoal->LocalMin);
					// goal.LocalMax = toVecStruct(pGoal->LocalMax);
					// goal.LocalExtent = toVecStruct(pGoal->LocalExtent);
				}
			}
		}

		auto fieldInfo = rlbot::flat::CreateFieldInfo(*builder, builder->CreateVector(boostPads), builder->CreateVector(goals));
		builder->Finish(fieldInfo);

		return filled;
	}

	flatbuffers::Offset<rlbot::flat::Physics> createPhysics(flatbuffers::FlatBufferBuilder* builder, AActor* actor) {
		
		auto location = createVector3(actor->Location);
		auto rotation = createRotator(actor->Rotation);
		auto velocity = createVector3(actor->Velocity);
		auto angular = createVector3(actor->AngularVelocity);

		return rlbot::flat::CreatePhysics(*builder, &location, &rotation, &velocity, &angular);
	}

	std::string convertString(wchar_t* w) {
		std::wstring ws(w);
		std::string str(ws.begin(), ws.end());
		return str;
	}

	flatbuffers::Offset<rlbot::flat::PlayerInfo> createPlayerInfo(flatbuffers::FlatBufferBuilder* builder, APRI_TA* pPlayerPRI)
	{
		rlbot::flat::ScoreInfoBuilder sib(*builder);

		sib.add_score(pPlayerPRI->MatchScore);
		sib.add_goals(pPlayerPRI->MatchGoals);
		sib.add_ownGoals(pPlayerPRI->MatchOwnGoals);
		sib.add_assists(pPlayerPRI->MatchAssists);
		sib.add_saves(pPlayerPRI->MatchSaves);
		sib.add_shots(pPlayerPRI->MatchShots);
		sib.add_demolitions(pPlayerPRI->MatchDemolishes);

		auto scoreInfo = sib.Finish();

		std::string name = convertString(pPlayerPRI->PlayerName.GetData());
		auto flatName = builder->CreateString(name); // Must do this before PlayerInfoBuilder is started.
		ACar_TA* pCar = pPlayerPRI->Car;
		auto physics = createPhysics(builder, pCar);

		rlbot::flat::PlayerInfoBuilder pib(*builder);

		pib.add_scoreInfo(scoreInfo);
		pib.add_isBot(pPlayerPRI->bBot);
		pib.add_name(flatName);
		pib.add_isDemolished(pPlayerPRI->RespawnTimeRemaining > 0);
		pib.add_physics(physics);
		pib.add_hasWheelContact(pCar->bOnGround);
		pib.add_isSupersonic(pCar->bSuperSonic);
		pib.add_jumped(pCar->bJumped);
		pib.add_doubleJumped(pCar->bDoubleJumped);
		pib.add_team(pCar->TeamPaint.Team);

		if (pCar->BoostComponent)
			pib.add_boost((int)roundf(pCar->BoostComponent->CurrentBoostAmount * 100));

		return pib.Finish();
	}

	

	flatbuffers::Offset<rlbot::flat::BallInfo> createBallInfo(flatbuffers::FlatBufferBuilder* builder, ABall_TA* pBall)
	{

		bool hasTouch = pBall->Touches.Num() > 0;

		flatbuffers::Offset<rlbot::flat::Touch> touchOffset;
		if (hasTouch) {

			FBallHitInfo sBallHit = pBall->Touches[0];
			std::string name = convertString(sBallHit.PRI->PlayerName.GetData());
			auto flatName = builder->CreateString(name); // Must do this before TouchBuilder is started

			auto touch = rlbot::flat::TouchBuilder(*builder);
			touch.add_playerName(flatName);
			touch.add_gameSeconds(sBallHit.Time);

			auto location = createVector3(sBallHit.HitLocation);
			touch.add_location(&location);

			auto normal = createVector3(sBallHit.HitNormal);
			touch.add_normal(&normal);

			touchOffset = touch.Finish();
		}

		auto physics = createPhysics(builder, pBall);
		
		rlbot::flat::BallInfoBuilder bib(*builder);
		bib.add_physics(physics);

		if (hasTouch)
		{
			bib.add_latestTouch(touchOffset);
		}

		return bib.Finish();
	}

	bool FillGameDataPacket(flatbuffers::FlatBufferBuilder* builder)
	{
		APlayerControllerBase_TA* pPC = InstanceManager::GetPlayerController();
		AGameEvent_Soccar_TA* pGES = InstanceManager::GetGameEvent();
		AWorldInfo* pWI = InstanceManager::GetWorldInfo();
		UGameShare_TA* pGS = InstanceManager::GetGameShare();

		if (!(pPC && pWI && pGES && pGS))
			return false;

		if (pWI->NetMode != NM_Standalone)
			return false;


		bool hasPlayers = pGES->PRIs.GetData();

		std::vector<flatbuffers::Offset<rlbot::flat::PlayerInfo>> players;
		flatbuffers::Offset<flatbuffers::Vector<flatbuffers::Offset<rlbot::flat::PlayerInfo>>> playersOffset;

		if (hasPlayers) {
			for (int i = 0; i < pGES->PRIs.Num(); i++)
			{
				APRI_TA* pPlayerPRI = pGES->PRIs[i];
				if (pPlayerPRI && !pPlayerPRI->bDeleteMe && pPlayerPRI->PawnType != PT_Spectator && pPlayerPRI->Car && pPlayerPRI->Car->IsA(ACar_TA::StaticClass()))
				{
					players.push_back(createPlayerInfo(builder, pPlayerPRI));
				}
			}

			playersOffset = builder->CreateVector(players);
		}


		auto gameInfo = createGameInfo(builder);

		std::vector<AVehiclePickup_Boost_TA*> boostList;
		fillBoostList(&boostList);
		std::vector<flatbuffers::Offset<rlbot::flat::BoostPadState>> boosts;
		flatbuffers::Offset<flatbuffers::Vector<flatbuffers::Offset<rlbot::flat::BoostPadState>>> boostsOffset;

		if (boostList.size() > 0)
		{
			for (int i = 0; i < boostList.size(); i++)
			{
				boosts.push_back(createBoostPadState(builder, boostList[i]));
			}
			boostsOffset = builder->CreateVector(boosts);
		}

		bool hasBall = pGES->GameBalls.GetData() && pGES->GameBalls.Num() > 0;
		flatbuffers::Offset<rlbot::flat::BallInfo> ballOffset;

		if (hasBall)
		{
			ballOffset = createBallInfo(builder, pGES->GameBalls[0]);
		}


		rlbot::flat::GameTickPacketBuilder gtb(*builder);
		gtb.add_gameInfo(gameInfo);
		if (hasPlayers)
		{
			gtb.add_players(playersOffset);
		}

		if (boostList.size() > 0)
		{
			gtb.add_boostPadStates(boostsOffset);
		}

		if (hasBall)
		{
			gtb.add_ball(ballOffset);
		}

		builder->Finish(gtb.Finish());

		return true;
	}

	rlbot::flat::Vector3 convertVec(vec3 vec)
	{
		return rlbot::flat::Vector3(vec[0], vec[1], vec[2]);
	}

	static BallPrediction::PredictionService predictionService(5.0, 0.2);

	bool FillBallPrediction(flatbuffers::FlatBufferBuilder* builder)
	{

		AGameEvent_Soccar_TA* pGES = InstanceManager::GetGameEvent();
		AWorldInfo* pWI = InstanceManager::GetWorldInfo();

		if (!(pWI && pGES))
			return false;

		if (pWI->NetMode != NM_Standalone)
			return false;

		bool hasBall = pGES->GameBalls.GetData() && pGES->GameBalls.Num() > 0;

		if (hasBall)
		{
			auto gameBall = pGES->GameBalls[0];

			std::list<BallPrediction::BallSlice>* prediction = predictionService.updatePrediction(gameBall, pWI->TimeSeconds);

			std::vector<flatbuffers::Offset<rlbot::flat::PredictionSlice>> slices;

			for (std::list<BallPrediction::BallSlice>::iterator it = prediction->begin(); it != prediction->end(); it++) {

				rlbot::flat::PhysicsBuilder physicsBuilder(*builder);
				physicsBuilder.add_location(&convertVec(it->Location));
				physicsBuilder.add_velocity(&convertVec(it->Velocity));
				physicsBuilder.add_angularVelocity(&convertVec(it->AngularVelocity));

				auto physOffset = physicsBuilder.Finish();

				rlbot::flat::PredictionSliceBuilder sliceBuilder(*builder);
				sliceBuilder.add_gameSeconds(it->gameSeconds);
				sliceBuilder.add_physics(physOffset);
				slices.push_back(sliceBuilder.Finish());
			}

			auto slicesOffset = builder->CreateVector(slices);

			rlbot::flat::BallPredictionBuilder predictionBuilder(*builder);
			predictionBuilder.add_slices(slicesOffset);

			builder->Finish(predictionBuilder.Finish());

			return true;
			
		}
		

		return false;
		
	}
}
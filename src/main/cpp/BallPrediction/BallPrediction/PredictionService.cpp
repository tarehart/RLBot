#include "PredictionService.hpp"
#include <math.h>


void PredictionService::makePrediction(BallSlice ballSlice, std::list<BallSlice>* predictionOut)
{
	ball.x = ballSlice.Location;
	ball.v = ballSlice.Velocity;
	ball.w = ballSlice.AngularVelocity;

	float predictionSeconds = ballSlice.gameSeconds;
	while (predictionOut->size() < expectedNumSlices) {
		ball.step(stepInterval);
		predictionSeconds += stepInterval;

		BallSlice predicted;
		predicted.Location = ball.x;
		predicted.Velocity = ball.v;
		predicted.AngularVelocity = ball.w;
		predicted.gameSeconds = predictionSeconds;

		predictionOut->push_back(predicted);
	}
}

void PredictionService::makeFreshPrediction(BallSlice ballSlice, std::list<BallSlice>* predictionOut)
{
	makePrediction(ballSlice, predictionOut);
}

std::list<BallSlice>* PredictionService::updatePrediction(BallSlice pBall)
{
	BallSlice baseSlice;

	if (currentPredictionStillValid(pBall)) {
		baseSlice = prediction.back();

		// Trim off the parts of the old prediction that are now in the past.
		while (prediction.front().gameSeconds < pBall.gameSeconds) {
			prediction.pop_front();
		}
	}
	else {
		prediction.clear();
		baseSlice = pBall;
	}

	makePrediction(baseSlice, &prediction);

	return &prediction;
}

bool PredictionService::currentPredictionStillValid(BallSlice currentBallPosition)
{

	BallSlice* predictedSlice = new BallSlice();
	if (expectedBallPosition(currentBallPosition.gameSeconds, predictedSlice)) {
		vec3 toPredicted = predictedSlice ->Location - currentBallPosition.Location;
		return dot(toPredicted, toPredicted) < 10;
	}

	return false;
		
}

bool PredictionService::expectedBallPosition(float gameSeconds, BallSlice* outputSlice)
{
	if (prediction.empty() || prediction.front().gameSeconds > gameSeconds || prediction.back().gameSeconds <= gameSeconds) {
		return false;
	}

	float predictionStartTime = prediction.front().gameSeconds;
	float secondsIntoPrediction = gameSeconds - predictionStartTime;
	int indexBefore = (int) (secondsIntoPrediction / stepInterval);


	std::list<BallSlice>::iterator it = prediction.begin();
	std::advance(it, indexBefore);
	BallSlice sliceBefore = *it;
	std::advance(it, 1);
	BallSlice sliceAfter = *it;


	float interpolationFactor = (gameSeconds - sliceBefore.gameSeconds) / stepInterval;
	float complementaryFactor = 1 - interpolationFactor;

	outputSlice->Location = (sliceBefore.Location * interpolationFactor + sliceAfter.Location * complementaryFactor) * 0.5f;
	outputSlice->Velocity = (sliceBefore.Velocity * interpolationFactor + sliceAfter.Velocity * complementaryFactor) * 0.5f;
	outputSlice->AngularVelocity = (sliceBefore.AngularVelocity * interpolationFactor + sliceAfter.AngularVelocity * complementaryFactor) * 0.5f;
	outputSlice->gameSeconds = gameSeconds;

	return true;
}


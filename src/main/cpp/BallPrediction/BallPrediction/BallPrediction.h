#pragma once

#include <flatbuffers/flatbuffers.h>

struct ByteBuffer
{
	void* ptr;
	int size;
};


bool FillBallPrediction(BallSlice ballSlice, flatbuffers::FlatBufferBuilder* builder);

void FetchBallPrediction(void* ballSliceFlatbuffer, int size, flatbuffers::FlatBufferBuilder* builder);

#pragma once

struct ByteBuffer
{
	void* ptr;
	int size;
};

extern "C" ByteBuffer __cdecl FetchBallPrediction(void* ballSliceFlatbuffer, int size);

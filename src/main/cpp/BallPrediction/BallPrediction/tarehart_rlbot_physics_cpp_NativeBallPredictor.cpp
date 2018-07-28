#include "tarehart_rlbot_physics_cpp_NativeBallPredictor.h"
#include "handle.h"
#include "PredictionService.hpp"
#include "BallPrediction.h"
#include "flat/rlbot_generated.h"


BallSlice getBallSlice(JNIEnv *env, jobject javaSlice) {
	BallSlice initialSlice;

	jclass javaSliceClass = env->GetObjectClass(javaSlice);
	jfieldID locationId = env->GetFieldID(javaSliceClass, "space", "tarehart/rlbot/math/vector/Vector3");
	jobject location = env->GetObjectField(javaSlice, locationId);

	jclass javaVectorClass = env->GetObjectClass(location);
	jfieldID xId = env->GetFieldID(javaVectorClass, "x", "java/lang/Double");
	jfieldID yId = env->GetFieldID(javaVectorClass, "y", "java/lang/Double");
	jfieldID zId = env->GetFieldID(javaVectorClass, "z", "java/lang/Double");

	initialSlice.Location = vec3{
		(float)env->GetDoubleField(location, xId),
		(float)env->GetDoubleField(location, yId),
		(float)env->GetDoubleField(location, zId),
	};

	jfieldID velocityId = env->GetFieldID(javaSliceClass, "velocity", "tarehart/rlbot/math/vector/Vector3");
	jobject velocity = env->GetObjectField(javaSlice, velocityId);

	initialSlice.Velocity = vec3{
		(float)env->GetDoubleField(velocity, xId),
		(float)env->GetDoubleField(velocity, yId),
		(float)env->GetDoubleField(velocity, zId),
	};

	jfieldID spinId = env->GetFieldID(javaSliceClass, "spin", "tarehart/rlbot/math/vector/Vector3");
	jobject spin = env->GetObjectField(javaSlice, spinId);

	initialSlice.AngularVelocity = vec3{
		(float)env->GetDoubleField(spin, xId),
		(float)env->GetDoubleField(spin, yId),
		(float)env->GetDoubleField(spin, zId),
	};


	jfieldID timeId = env->GetFieldID(javaSliceClass, "time", "tarehart/rlbot/time/GameTime");
	jobject timeObject = env->GetObjectField(javaSlice, timeId);
	jclass javaGameTimeClass = env->GetObjectClass(timeObject);
	jfieldID millisId = env->GetFieldID(javaGameTimeClass, "millis", "J");
	jlong millis = env->GetLongField(timeObject, millisId);
	initialSlice.gameSeconds = millis / 1000.0;

	return initialSlice;
}

jbyteArray Java_tarehart_rlbot_physics_cpp_NativeBallPredictor_predictPath(JNIEnv *env, jobject that, jbyteArray sliceFlatbuffer, jfloat minSeconds)
{
	
	//BallSlice initialSlice = getBallSlice(env, javaSlice);

	//flatbuffers::FlatBufferBuilder ballPredictionBuilder;
	//FillBallPrediction(initialSlice, &ballPredictionBuilder);

	int size = env->GetArrayLength(sliceFlatbuffer);
	unsigned char* bytes = new unsigned char[size];

	env->GetByteArrayRegion(sliceFlatbuffer, 0, size, reinterpret_cast<jbyte*>(bytes));

	flatbuffers::FlatBufferBuilder predictionBuilder;

	FetchBallPrediction(bytes, size, &predictionBuilder);

	delete[] bytes;
	

	jbyteArray flatBytes = env->NewByteArray(predictionBuilder.GetSize());
	env->SetByteArrayRegion(flatBytes, 0, predictionBuilder.GetSize(), reinterpret_cast<jbyte*>(predictionBuilder.GetBufferPointer()));

	return flatBytes;

	

	/*
	BallSlice initialSlice;

	jclass javaSliceClass = env->GetObjectClass(javaSlice);
	jfieldID locationId = env->GetFieldID(javaSliceClass, "location", "[F");
	jobject location = env->GetObjectField(javaSlice, locationId);
	jfloatArray* arr = reinterpret_cast<jfloatArray*>(&location);
	float* data = env->GetFloatArrayElements(*arr)
	vec3 myLoc{arr[0], arr[1], arr[2]}


	jclass javaVectorClass = env->GetObjectClass(location);
	jfieldID xId = env->GetFieldID(javaVectorClass, "x", "java/lang/Double");
	jfieldID yId = env->GetFieldID(javaVectorClass, "y", "java/lang/Double");
	jfieldID zId = env->GetFieldID(javaVectorClass, "z", "java/lang/Double");

	initialSlice.Location = vec3{
		(float)env->GetDoubleField(location, xId),
		(float)env->GetDoubleField(location, yId),
		(float)env->GetDoubleField(location, zId),
	};
	

	jobjectArray arr = env->NewObjectArray(slices->size(), env->FindClass("tarehart/rlbot/math/BallSlice"), NULL);

	for (int i = 0; i < slices->size(); i++) {
		jobject
		env->SetObjectArrayElement(arr, i, )
	}
	*/
}

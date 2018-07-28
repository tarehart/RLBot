#include "tarehart_rlbot_physics_cpp_NativeBallPredictor.h"
#include "handle.h"

jobjectArray Java_tarehart_rlbot_physics_cpp_NativeBallPredictor_predictPath(JNIEnv *env, jobject, jobject, jfloat)
{
	return env->NewObjectArray(0, env->FindClass("tarehart/rlbot/math/BallSlice"), NULL);
}

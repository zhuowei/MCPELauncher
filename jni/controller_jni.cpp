#include <jni.h>
#include <android/log.h>
#include <android/native_activity.h>
#include "mcpe/appplatform.h"
#include "mcpe/controller.h"

// abridged android_app struct
struct android_app {
    // The application can place a pointer to its own state object
    // here if it likes.
    void* userData;

    // Fill this in with the function to process main app commands (APP_CMD_*)
    void (*onAppCmd)(struct android_app* app, int32_t cmd);

    // Fill this in with the function to process input events.  At this point
    // the event has already been pre-dispatched, and it will be finished upon
    // return.  Return 1 if you have handled the event, 0 for any default
    // dispatching.
    int32_t (*onInputEvent)(struct android_app* app, AInputEvent* event);
};

static int32_t (*inputHandlerReal)(struct android_app* app, AInputEvent* event);

static int32_t inputHandlerHook(struct android_app* app, AInputEvent* event) {
	__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "InputEvent: %p", event);
	if (AInputEvent_getType(event) == AINPUT_EVENT_TYPE_MOTION) {
		if ((AInputEvent_getSource(event) & AINPUT_SOURCE_CLASS_JOYSTICK) == AINPUT_SOURCE_CLASS_JOYSTICK) {
			int32_t actionAndPointer = AMotionEvent_getAction(event);
			int32_t action = actionAndPointer & AMOTION_EVENT_ACTION_MASK;
			if (action == AMOTION_EVENT_ACTION_MOVE) {
				float x = AMotionEvent_getX(event, 0);
				float y = AMotionEvent_getY(event, 0);
				__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Controller %f %f", x, y);
				Controller::feed(2, 1, x, y);
			}
		}
	}
	return inputHandlerReal(app, event);
}

extern "C" {

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_ControllerManager_init
  (JNIEnv *env, jclass clazz) {
	ANativeActivity* nativeActivity = *((ANativeActivity**) (((uintptr_t) AppPlatform::_singleton) + 112));
	struct android_app* app = (struct android_app*) nativeActivity->instance;
	inputHandlerReal = app->onInputEvent;
	app->onInputEvent = inputHandlerHook;
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_ControllerManager_feed
  (JNIEnv *env, jclass clazz, jint axis, jint type, jfloat x, jfloat y) {
	Controller::feed(axis, type, x, y);
}

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_ControllerManager_feedTrigger
  (JNIEnv *env, jclass clazz, jint axis, jfloat value) {
	Controller::feedTrigger(axis, value);
}

}

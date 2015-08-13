#include <jni.h>
#include <android/log.h>
#include <android/native_activity.h>
#include "mcpe/appplatform.h"
#include "mcpe/controller.h"
#include <dlfcn.h>

#include "modscript_shared.h"

static float (*bl_AMotionEvent_getAxisValue)(const AInputEvent* motion_event,
        int32_t axis, size_t pointer_index) __NDK_FPABI__;
static void injectKeyEvent(int keycode, int pressed);

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

static int dpadState = 0;
static bool triggerEmu = false;

enum {
	BL_DPAD_UP = 1,
	BL_DPAD_DOWN = 2,
	BL_DPAD_LEFT = 4,
	BL_DPAD_RIGHT = 8,
};

static void checkState(int newdpadState, int key, int akey) {
	if ((newdpadState & key) != (dpadState & key)) {
		injectKeyEvent(akey, (newdpadState & key) != 0);
	}
}

/*static int32_t inputHandlerHook(struct android_app* app, AInputEvent* event) {
	//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "InputEvent: %p", event);
	int32_t forceReturn = -1;
	if (AInputEvent_getType(event) == AINPUT_EVENT_TYPE_MOTION) {
		int32_t source = AInputEvent_getSource(event);
		if ((source & AINPUT_SOURCE_CLASS_JOYSTICK) == AINPUT_SOURCE_CLASS_JOYSTICK) {
			int32_t actionAndPointer = AMotionEvent_getAction(event);
			int32_t action = actionAndPointer & AMOTION_EVENT_ACTION_MASK;
			if (action == AMOTION_EVENT_ACTION_MOVE) {
				float x = bl_AMotionEvent_getAxisValue(event, AMOTION_EVENT_AXIS_X, 0);
				float y = bl_AMotionEvent_getAxisValue(event, AMOTION_EVENT_AXIS_Y, 0);
				float z = bl_AMotionEvent_getAxisValue(event, AMOTION_EVENT_AXIS_Z, 0);
				float rx = bl_AMotionEvent_getAxisValue(event, AMOTION_EVENT_AXIS_RZ, 0);
				float dpadX = bl_AMotionEvent_getAxisValue(event, AMOTION_EVENT_AXIS_HAT_X, 0);
				float dpadY = bl_AMotionEvent_getAxisValue(event, AMOTION_EVENT_AXIS_HAT_Y, 0);
				int newdpadState = (dpadX > 0.5f? BL_DPAD_RIGHT: (dpadX < -0.5f? BL_DPAD_LEFT: 0)) |
					(dpadY > 0.5f? BL_DPAD_DOWN: (dpadY < -0.5f? BL_DPAD_UP: 0));
				if (newdpadState != dpadState) {
					checkState(newdpadState, BL_DPAD_UP, AKEYCODE_DPAD_UP);
					checkState(newdpadState, BL_DPAD_DOWN, AKEYCODE_DPAD_DOWN);
					checkState(newdpadState, BL_DPAD_LEFT, AKEYCODE_DPAD_LEFT);
					checkState(newdpadState, BL_DPAD_RIGHT, AKEYCODE_DPAD_RIGHT);
				}
				float ltrigger = bl_AMotionEvent_getAxisValue(event, AMOTION_EVENT_AXIS_LTRIGGER, 0);
				float rtrigger = bl_AMotionEvent_getAxisValue(event, AMOTION_EVENT_AXIS_LTRIGGER, 0);

				//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Controller %f %f %x %x", x, y,
				//	dpadState, newdpadState);
				Controller::feed(1, 1, x, -y);
				Controller::feed(2, 1, z, -rx);
				if (!triggerEmu) {
					Controller::feedTrigger(1, ltrigger);
					Controller::feedTrigger(2, rtrigger);
				}
				dpadState = newdpadState;
				// handle dpad
			}
		}
	} else if (AInputEvent_getType(event) == AINPUT_EVENT_TYPE_KEY) {
		//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Key sauce");
		if ((AInputEvent_getSource(event) & AINPUT_SOURCE_GAMEPAD) == AINPUT_SOURCE_GAMEPAD) {
			int32_t keyCode = AKeyEvent_getKeyCode(event);
			int32_t action = AKeyEvent_getAction(event);
			//__android_log_print(ANDROID_LOG_INFO, "BlockLauncher", "Key %d %d", keyCode, action);

			float val;
			bool yep;
			if (action == AKEY_EVENT_ACTION_DOWN) {
				val = 1;
				yep = true;
			} else if (action == AKEY_EVENT_ACTION_UP) {
				val = 0;
				yep = true;
			} else {
				val = 0;
				yep = false;
			}


			int trigger = 0;
			if (keyCode == AKEYCODE_BUTTON_L2) {
				trigger = 1;
			} else if (keyCode == AKEYCODE_BUTTON_R2) {
				trigger = 2;
			}
			if (yep && trigger) {
				triggerEmu = true;
				Controller::feedTrigger(trigger, val);
			}
			if (yep && keyCode == AKEYCODE_BUTTON_A) {
				injectKeyEvent(AKEYCODE_DPAD_CENTER, val);
			}
			if (yep && keyCode == AKEYCODE_BUTTON_B) {
				injectKeyEvent(AKEYCODE_BACK, val);
			}
		}
	}
	int32_t retval = inputHandlerReal(app, event);
	if (forceReturn >= 0) return forceReturn;
	return retval;
}*/

static void injectKeyEvent(int keycode, int down) {
	JNIEnv* env;
	//This hook can be triggered by ModPE scripts, so don't attach/detach when already executing in Java thread
	int attachStatus = bl_JavaVM->GetEnv((void**) &env, JNI_VERSION_1_2);
	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->AttachCurrentThread(&env, NULL);
	}

	//Call back across JNI into the ScriptManager
	jmethodID mid = env->GetStaticMethodID(bl_scriptmanager_class, "injectKeyEvent", "(II)V");

	env->CallStaticVoidMethod(bl_scriptmanager_class, mid, keycode, down);

	if (attachStatus == JNI_EDETACHED) {
		bl_JavaVM->DetachCurrentThread();
	}

}

extern "C" {

JNIEXPORT void JNICALL Java_net_zhuoweizhang_mcpelauncher_api_modpe_ControllerManager_init
  (JNIEnv *env, jclass clazz) {
/*	bl_AMotionEvent_getAxisValue =
		(float (*)(const AInputEvent* motion_event, int32_t axis, size_t pointer_index))
		dlsym(RTLD_DEFAULT, "AMotionEvent_getAxisValue");
	ANativeActivity* nativeActivity = *((ANativeActivity**) (((uintptr_t) AppPlatform::_singleton) + 112));
	struct android_app* app = (struct android_app*) nativeActivity->instance;
	inputHandlerReal = app->onInputEvent;
	app->onInputEvent = inputHandlerHook;
*/
}

}

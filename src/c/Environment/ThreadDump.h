#ifndef _THREADDUMP_H_
#define _THREADDUMP_H_

#include <jvmpi.h>
#include <iostream>
#include <vector>

extern JVMPI_Interface *jvmpi_interface;
extern JNIEnv *dumpEnv;

using namespace std;

ostream& operator<<(ostream& stream, jobjectID objID);

void* readAndAdvance(void* ptr, void* dest, int size);

void* readAndAdvance_jint(void* bufPtr, jint* dest);

void* readAndAdvance_ptr(void* bufPtr, void** dest);

typedef void (*JVMPI_Event_Handler) (JVMPI_Event* event, void* extraArgs);

namespace ThreadDump {

	void setOstream(ostream& stream);
	
	void setMonitorsPtr(std::vector<jobject> * monitors);

	void monitor_dump_event(JVMPI_Event *event, void* extraArgs);

	void callForEvent(JVMPI_Event_Handler handler, jint event_type, void *eventArg, void* handlerArg);

	void eventHandler(JVMPI_Event *event);

	void printThreadName(ostream& stream, JNIEnv* thread);

	JVMPI_CallTrace* getFullCallTrace(JNIEnv * env);
}

#endif
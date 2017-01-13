/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

#include "ThreadDump.h"

#include <jni.h>
#include <jvmpi.h>
#include <string.h>

#include <Winsock2.h>


#include "MonitorDumpRecord.h"
#include "JavaMonitorDumpRecord.h"
#include "RawMonitorDumpRecord.h"

ostream* output;
JVMPI_Interface *jvmpi_interface;
JNIEnv *dumpEnv;
JVMPI_Event_Handler currentEventDelegate;
jint requiredEventType;
void *currentEventExtraArgs;

std::vector<jobject> * monitorsPtr;

void ThreadDump::setMonitorsPtr(std::vector<jobject> * monitors) {
	monitorsPtr = monitors;
}


void ThreadDump::setOstream(ostream& stream) {
	output = &stream;
}


void ThreadDump::callForEvent(JVMPI_Event_Handler handler, jint event_type, void *eventArg, void* handlerExtraArgs) {
	currentEventDelegate = handler;
	currentEventExtraArgs = handlerExtraArgs;
	requiredEventType = event_type;
	jvmpi_interface->RequestEvent(event_type, eventArg);
	currentEventDelegate = NULL;
	currentEventExtraArgs = NULL;
	requiredEventType = 0;
}

void ThreadDump::eventHandler(JVMPI_Event *event) {
    if (event->event_type == (requiredEventType | JVMPI_REQUESTED_EVENT)) {
		if (currentEventDelegate != NULL) {
			(currentEventDelegate)(event, currentEventExtraArgs);
		}
    }else{
		cerr << "unexpected event: "<<event->event_type <<endl;
	}

}


void ThreadDump::monitor_dump_event(JVMPI_Event *event, void* extraArgs)
{
	int traceCount = event->u.monitor_dump.num_traces;
	(*output) << endl << "-----------------BEGIN DUMP------------------" << endl;
	(*output) << "Got " << traceCount << " traces\n";

	void *dumpP;
	dumpP = event->u.monitor_dump.begin;
	unsigned char rec_type;


	while (dumpP < event->u.monitor_dump.end) {
		(*output) << "       -----Monitor-----" << endl;
		dumpP = readAndAdvance(dumpP, (void*)&rec_type, sizeof(unsigned char));
		if (rec_type == JVMPI_MONITOR_JAVA) {
			JavaMonitorDumpRecord dump_rec;
			dumpP = dump_rec.readAndAdvance(dumpP);

			jobject obj = jvmpi_interface->jobjectID2jobject(dump_rec.object);

			monitorsPtr->push_back(obj);

			(*output) << dump_rec;
		}else if (rec_type == JVMPI_MONITOR_RAW) {
			RawMonitorDumpRecord dump_rec;
			dumpP = dump_rec.readAndAdvance(dumpP);
		}
	}
	(*output) << endl << "-----------------END DUMP------------------" << endl;
}



JVMPI_CallTrace* ThreadDump::getFullCallTrace(JNIEnv * env) {
	jint status = jvmpi_interface->GetThreadStatus(env);
	bool needsResume = false;
//	if (!(status & JVMPI_THREAD_SUSPENDED)) {	//if not suspended
//		jvmpi_interface->SuspendThread(env);
//		needsResume = true;
//	}
	JVMPI_CallTrace * callTrace = new JVMPI_CallTrace;
	callTrace->env_id = env;

	int frameCount = 10;
	while (true) {
		JVMPI_CallFrame * callFrames = new JVMPI_CallFrame[frameCount];
		callTrace->frames = callFrames;
		jvmpi_interface->GetCallTrace(callTrace,frameCount);

		if (callTrace->num_frames < frameCount) {
//			if (needsResume) {
//				jvmpi_interface->ResumeThread(env);
//			}
			return callTrace;
		}else{
			delete[] callFrames;
			frameCount *=2;
		}
	}
}

void getClassName(JVMPI_Event* event, void* extraArgs) {
	const char * className = event->u.class_load.class_name;
	int strLen = strlen(className);	
	char* copiedStr = new char[strLen+1];
	strcpy(copiedStr, className);
	*((char**)extraArgs) = copiedStr;
}

void printThreadDetails(JVMPI_Event* event, void* extraArgs) {
	const char * threadName = event->u.thread_start.thread_name;
	const char * groupName = event->u.thread_start.group_name;
	const char * parentName = event->u.thread_start.parent_name;

	if (threadName == NULL) {
		threadName = "null";
	}
	if (groupName == NULL) {
		groupName = "null";
	}
	if (parentName == NULL) {
		parentName = "null";
	}

	ostream& stream = *(ostream*)extraArgs;
	stream << "  ThreadName = ["<<threadName<<"]";
	stream << "  GroupName = ["<< groupName<<"]";
	stream << "  ParentName = ["<< parentName<<"]";
}


void ThreadDump::printThreadName(ostream& stream, JNIEnv* thread) {
	if (thread != NULL) {
		jobjectID threadID = jvmpi_interface->GetThreadObject(thread);
		ThreadDump::callForEvent(printThreadDetails, JVMPI_EVENT_THREAD_START, threadID, &stream);
	}
}

ostream& operator<<(ostream& stream, jobjectID objID) {
	jobject obj = jvmpi_interface->jobjectID2jobject(objID);
	jclass clazz = dumpEnv->GetObjectClass(obj);
	jobjectID classID = jvmpi_interface->jobject2jobjectID(clazz);

	char* className;
	ThreadDump::callForEvent(getClassName, JVMPI_EVENT_CLASS_LOAD, classID, (void*)&className);
	
	stream << obj << " of type: " << className << endl;
	delete [] className;
	// it is not safe to call JNI from within this context:
	// http://java.sun.com/j2se/1.4.1/docs/guide/jvmpi/jvmpi.html#convertNote

/*    jmethodID method = dumpEnv->GetMethodID(clazz, "toString", "()Ljava/lang/String;");
	jstring toStringValue = (jstring)(dumpEnv->CallObjectMethod(obj, method));
	const char* str = dumpEnv->GetStringUTFChars(toStringValue, NULL);

	stream << str;

	dumpEnv->ReleaseStringUTFChars(toStringValue, str);
*/

    return stream;
}

void* readAndAdvance(void* ptr, void* dest, int size) {
    memcpy(dest, ptr, size);
    return (void *) ((char*)ptr + size);
}

void* readAndAdvance_jint(void* bufPtr, jint* dest) {
    int size = sizeof(jint);
	bufPtr = readAndAdvance(bufPtr, (void*)dest, size);
    *dest = ntohl(*dest);
	return bufPtr;
}

void* readAndAdvance_ptr(void* bufPtr, void** dest) {
    int size = sizeof(void*);
	bufPtr = readAndAdvance(bufPtr, (void*)dest, size);
	return bufPtr;
}
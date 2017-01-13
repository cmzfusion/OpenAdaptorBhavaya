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

// MonitorDumpRecord.cpp: implementation of the MonitorDumpRecord class.
//
//////////////////////////////////////////////////////////////////////

#include "MonitorDumpRecord.h"

#include "ThreadDump.h"

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
///////////////////////////////////////////////////////////////////////

typedef struct printStackTraceLineParams{
	ostream *stream;
	jint lineNo;
	jmethodID methodID;
} printStackTraceLineParams;

printStackTraceLineParams pendingPrintStackTraceLineParams;




MonitorDumpRecord::MonitorDumpRecord()
{

}

MonitorDumpRecord::~MonitorDumpRecord()
{

}

ostream & operator<<( ostream &stream, MonitorDumpRecord &rec ) {
	return rec.print(stream);
}



ostream & MonitorDumpRecord::print(ostream& stream) {
	jint statusInt = jvmpi_interface->GetThreadStatus(owner_thread);
	char * status = "unknown";
	switch (statusInt) {
		case JVMPI_THREAD_RUNNABLE: status = "runnable"; break;
		case JVMPI_THREAD_MONITOR_WAIT : status = "monitor wait"; break;
		case JVMPI_THREAD_CONDVAR_WAIT : status = "condition wait"; break;
	}

	stream << "Owner thread has entered this monitor: "<<entry_count <<" times"<<endl;
	
	stream << "Owner thread: "<< owner_thread << "(";
	ThreadDump::printThreadName(stream, owner_thread);
	stream << ")" <<endl;
	stream << "status: " << status << endl;
	
	printStackTraces(stream, &owner_thread, 1);
	stream << endl;
	stream << "Other threads awaiting entry to this monitor: " << threads_waiting_to_enter_count << " their stacktraces are:"<< endl;
	printStackTraces(stream, threads_waiting_to_enter, threads_waiting_to_enter_count);
	stream << endl;
	stream << "Other threads awaiting notify() on the monitor: " << threads_waiting_to_be_notified_count << " their stacktraces are:"<< endl;
	printStackTraces(stream, threads_waiting_to_be_notified, threads_waiting_to_be_notified_count);
	stream << endl;
	return stream;
}

void printLineOfTrace(JVMPI_Event* event, void* extraArgs) {
	const char * className = event->u.class_load.class_name;
	const char * sourceName = event->u.class_load.source_name;
	char * methodName = "";

	if (sourceName == NULL) {
		sourceName = "Unknown Source";
	}

	for (int i=0; i<event->u.class_load.num_methods; i++) {
		 JVMPI_Method method = event->u.class_load.methods[i];
		 if (method.method_id == pendingPrintStackTraceLineParams.methodID) {
			methodName = method.method_name;
			break;
		 }
	}

	jint lineNo = pendingPrintStackTraceLineParams.lineNo;
	ostream &stream = *pendingPrintStackTraceLineParams.stream;

	stream << "     " <<className<< "."<<methodName<< " (Source: "<<sourceName<<":"<<lineNo<<")"<<endl;

}


void MonitorDumpRecord::printStackTraces(ostream &stream, JNIEnv** threadArray, int count) {
	for  (int thread_idx=0; thread_idx<count; thread_idx++) {
		JNIEnv* env = threadArray[thread_idx];
		stream << "Stack trace "<<thread_idx+1<<endl;
	
		stream << "Thread:" << env << "(";
		ThreadDump::printThreadName(stream, env);
		stream << ")" <<endl;

		jvmpi_interface->SuspendThread(env);

		JVMPI_CallTrace* trace = ThreadDump::getFullCallTrace(env);
		JVMPI_CallFrame * callFrames = trace->frames;

		for (int frame_idx = 0; frame_idx<trace->num_frames; frame_idx++) {
			jint lineNo = callFrames[frame_idx].lineno;
			jmethodID methodID = callFrames[frame_idx].method_id;
			jobjectID classID = jvmpi_interface->GetMethodClass(methodID);

			pendingPrintStackTraceLineParams.methodID = methodID;
			pendingPrintStackTraceLineParams.lineNo = lineNo;
			pendingPrintStackTraceLineParams.stream = &stream;

			ThreadDump::callForEvent(printLineOfTrace, JVMPI_EVENT_CLASS_LOAD, classID, NULL);
		}
		
		jvmpi_interface->ResumeThread(env);

		delete[] callFrames;
		delete trace;
	}
}


void* MonitorDumpRecord::readAndAdvance(void* bufPtr) {
	bufPtr = readAndAdvance_ptr(bufPtr, (void**)&owner_thread);
	bufPtr = readAndAdvance_jint(bufPtr, &entry_count);
	bufPtr = readAndAdvance_jint(bufPtr, &threads_waiting_to_enter_count);
	if (threads_waiting_to_enter_count > 0) {
		threads_waiting_to_enter = (JNIEnv **)bufPtr;
		bufPtr = (void*) ((void**)bufPtr + threads_waiting_to_enter_count);
	}

	bufPtr = readAndAdvance_jint(bufPtr, &threads_waiting_to_be_notified_count);
	if (threads_waiting_to_be_notified_count > 0) {
		threads_waiting_to_be_notified = (JNIEnv **)bufPtr;
		bufPtr = (void*) ((void**)bufPtr + threads_waiting_to_be_notified_count);
	}
	return bufPtr;
}

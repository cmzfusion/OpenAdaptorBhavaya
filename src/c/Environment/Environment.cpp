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

#define _WIN32_WINNT 0x0400

#include <jvmpi.h>
#include <FCNTL.H>

#include <iostream>
#include <strstream>
#include <jni.h>
#include <signal.h>
#include <windows.h>
#include <process.h>
#include <winbase.h>
#include <vector>

#include "ThreadDump.h"

#include "org_bhavaya_util_Environment.h"

using namespace std;

//extern "C" void hprof_monitor_dump_event(JVMPI_Event *event);
//extern "C" void hprof_notify_event(JVMPI_Event *event);
//extern "C" void hprof_init_setup(char *profiler_options);
//extern "C" JNIEXPORT jint JNICALL JVM_OnLoad(JavaVM *vm, char *options, void *reserved);

JNIEXPORT jstring JNICALL Java_org_bhavaya_util_Environment_getProperty
  (JNIEnv * env, jclass aClass, jstring property)
{
   const char *prop = env->GetStringUTFChars(property, NULL);
   char * value = getenv(prop);
   env->ReleaseStringUTFChars(property, prop);

   jstring ret = env->NewStringUTF(value);
   return ret;
}

JNIEXPORT void JNICALL Java_org_bhavaya_util_Environment_setProperty
  (JNIEnv *env, jclass jClass, jstring name, jstring value)
{
	jboolean iscopy;
	const char *szName = env->GetStringUTFChars(name, &iscopy);
	const char *szValue = env->GetStringUTFChars(value, &iscopy);
	SetEnvironmentVariable(szName, szValue);
}


JNIEXPORT void JNICALL Java_org_bhavaya_util_Environment_getProperties
  (JNIEnv * env, jclass jClass, jobject arrayList)
{
	LPVOID envBlock = GetEnvironmentStrings();

	jclass cls = env->GetObjectClass(arrayList);
    jmethodID mid = env->GetMethodID(cls, "add", "(Ljava/lang/Object;)Z");

    if (mid != 0) {
		LPTSTR str = (LPTSTR) envBlock;
		while (true) {
			if (*str == 0) break;
			jstring newStr = env->NewStringUTF(str);
			env->CallObjectMethod(arrayList, mid, newStr);
			while (*str != 0) str++;
			str++;
		}
	}
}



/* ------------------------------------------------------------------------- */
/* ----------------------- CPU STUFF ----------------------------- */
static jint s_PID;
static HANDLE s_currentProcess;
static int s_numberOfProcessors;
static bool initedCPU = false;

static LONGLONG
fileTimeToInt64 (const FILETIME * time)
{
    ULARGE_INTEGER _time;

    _time.LowPart = time->dwLowDateTime;
    _time.HighPart = time->dwHighDateTime;

    return _time.QuadPart;
}


void initCPU()
{
	if (!initedCPU)
	{
		SYSTEM_INFO systemInfo;

		s_currentProcess = GetCurrentProcess ();
		GetSystemInfo (& systemInfo);
		s_numberOfProcessors = systemInfo.dwNumberOfProcessors;
		initedCPU = true;
	}
}

JNIEXPORT jlong JNICALL Java_org_bhavaya_util_Environment_getProcessCPUTime__
   (JNIEnv *env, jclass c)
{
	initCPU();
	return Java_org_bhavaya_util_Environment_getProcessCPUTime__I(env, c, (jint) s_currentProcess);
}

JNIEXPORT jlong JNICALL Java_org_bhavaya_util_Environment_getProcessCPUTime__I
  (JNIEnv *env, jclass c, jint processId)
{
	initCPU();
	FILETIME creationTime, exitTime, kernelTime, userTime;

	HANDLE hProcess = OpenProcess( PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, processId );

	GetProcessTimes (hProcess, & creationTime, & exitTime, & kernelTime, & userTime);
	CloseHandle(hProcess);
	return (jlong) ((fileTimeToInt64 (& kernelTime) + fileTimeToInt64 (& userTime)) /
				(s_numberOfProcessors * 10000));

}

JNIEXPORT jint JNICALL Java_org_bhavaya_util_Environment_getNumberOfProcessors
  (JNIEnv *env, jclass c)
{
	initCPU();
	return (jint) s_numberOfProcessors;
}


/* --------------------------- CTRL-break stuff --------------------- */
HINSTANCE hinstLib;

typedef void (JNICALL *DUMP_ALL_PROC) (JNIEnv *env, jclass unused);
//typedef void (JNICALL *DUMP_ALL_PROC) (jint sig);
//typedef void (*DUMP_ALL_PROC) (const char *fmt, ...);



DUMP_ALL_PROC DumpAll;
bool runTimeLinkSuccess = false;
bool initedDump = false;

void initDump(JNIEnv *env)
{
	if (!initedDump) {
		JavaVM * jvm;
		env->GetJavaVM(&jvm);

		int res = (*jvm).GetEnv((void **)&jvmpi_interface, JVMPI_VERSION_1);
		if (res < 0) {
			cerr << "Could not get JVMPI.  Err:" << res << "\n";
		}
		dumpEnv = env;
		jvmpi_interface->NotifyEvent = ThreadDump::eventHandler;
		initedDump = true;
	}
}



static FILE *stream;

JNIEXPORT void JNICALL
Java_org_bhavaya_util_Environment_nativeRequestThreadDump(JNIEnv *env, jclass clazz, jobject stringBuffer) {
	initDump(env);

	ostrstream os;

	std::vector<jobject> monitors;
	ThreadDump::setOstream(os);
	ThreadDump::setMonitorsPtr(&monitors);
	ThreadDump::callForEvent(ThreadDump::monitor_dump_event, JVMPI_EVENT_MONITOR_DUMP, NULL, NULL);


	char * str = os.str();

	jclass cls = env->GetObjectClass(stringBuffer);
    jmethodID mid = env->GetMethodID(cls, "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");

	//weird bug with ostrstream.str does not seem to null terminate my string!
	int strEnd = os.pcount();
	char oldValue = str[strEnd-1];
	str[strEnd-1] = '\0';

    if (mid != 0) {
		jstring newStr = env->NewStringUTF(str);
		env->CallObjectMethod(stringBuffer, mid, newStr);
	}
	str[strEnd-1] = oldValue;

	return;
}

/* --------------------------- Drive space stuff --------------------- */

JNIEXPORT void JNICALL Java_org_bhavaya_util_Environment_getDiskSpaceInfo
  (JNIEnv *env, jclass clazz, jstring path, jobject info) {

    ULARGE_INTEGER uliFreeBytesAvailable;
    ULARGE_INTEGER uliTotalNumberOfBytes;
    ULARGE_INTEGER uliTotalNumberOfFreeBytes;

    const char *_path = env->GetStringUTFChars(path, 0);
    BOOL result = GetDiskFreeSpaceEx(_path, &uliFreeBytesAvailable, &uliTotalNumberOfBytes, &uliTotalNumberOfFreeBytes);
    env->ReleaseStringUTFChars(path, _path);

    jclass infoClazz = env->GetObjectClass(info);
    jfieldID successfulFieldID = env->GetFieldID(infoClazz, "successful", "Z");
    jfieldID totalSpaceFieldID = env->GetFieldID(infoClazz, "totalSpace", "J");
    jfieldID availableSpaceFieldID = env->GetFieldID(infoClazz, "availableSpace", "J");
    if (result == TRUE) {
        env->SetBooleanField(info, successfulFieldID, JNI_TRUE);
        env->SetLongField(info, totalSpaceFieldID, (jlong)(uliTotalNumberOfBytes.QuadPart));
        env->SetLongField(info, availableSpaceFieldID, (jlong)(uliFreeBytesAvailable.QuadPart));
    } else {
        env->SetBooleanField(info, successfulFieldID, JNI_FALSE);
        env->SetLongField(info, totalSpaceFieldID, 0);
        env->SetLongField(info, availableSpaceFieldID, 0);
    }
}

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

#include <stdlib.h>
#include <jni.h>
#include <windows.h>
#include <iostream.h>

#include "org_bhavaya_util_NativeProcess.h"
#include "psapi.h"


JNIEXPORT void JNICALL Java_org_bhavaya_util_NativeProcess_terminateProcess (JNIEnv *env, jclass jClass, jint processId) {
	HANDLE hProcess = OpenProcess( PROCESS_TERMINATE, FALSE, processId);
	if (hProcess == NULL) {
		env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Insufficient rights to terminate process");
		CloseHandle(hProcess);
		return;
	}
	UINT exitCode;
	if ( !TerminateProcess(hProcess, exitCode) ) {
		env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Error terminating process");
	}
	CloseHandle(hProcess);
}

JNIEXPORT jint JNICALL Java_org_bhavaya_util_NativeProcess_waitForProcess (JNIEnv * env, jclass jClass, jint processId)
{
	HANDLE hProcess = OpenProcess( SYNCHRONIZE | PROCESS_QUERY_INFORMATION, FALSE, processId);
	if (hProcess == NULL) {
		env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Insufficient rights to synchronise on process");
		CloseHandle(hProcess);
		return -1;
	}
	DWORD exitCode;
    WaitForSingleObject(hProcess, INFINITE);
	GetExitCodeProcess(hProcess, &exitCode);
	CloseHandle(hProcess);
	return (jint) exitCode;
}


JNIEXPORT void JNICALL Java_org_bhavaya_util_NativeProcess_getProcessEnumeration (JNIEnv *env, jclass jClass, jobject arrayList) {

	// Get the list of process identifiers.
    DWORD aProcesses[1024], cbNeeded, cProcesses;
    unsigned int i;

    if ( !EnumProcesses( aProcesses, sizeof(aProcesses), &cbNeeded ) )
        return;

    // Calculate how many process identifiers were returned.
    cProcesses = cbNeeded / sizeof(DWORD);

    // Get a handle on the arrayList add method
    jclass cls = env->GetObjectClass(arrayList);
    jmethodID mid = env->GetMethodID(cls, "add", "(Ljava/lang/Object;)Z");

	// Get a handle on the NativeProcess constructor
	jclass nativeProcessClass = env->FindClass("org/bhavaya/util/NativeProcess");
	jmethodID cid = env->GetMethodID(nativeProcessClass, "<init>", "(ILjava/lang/String;)V");

	// Open the process, create a NativeProcess class
	char szProcessName[MAX_PATH] = "unknown";
    for ( i = 0; i < cProcesses; i++ ) {
		DWORD processId = aProcesses[i];

		HANDLE hProcess = OpenProcess( PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, processId );

		if (NULL != hProcess) {
			HMODULE hMod;
			DWORD cbNeeded;

			// Get the executable name
			if ( EnumProcessModules( hProcess, &hMod, sizeof(hMod), &cbNeeded)) {
				GetModuleBaseName( hProcess, hMod, szProcessName, sizeof(szProcessName));

				jstring exeName = env->NewStringUTF(szProcessName);
				env->CallObjectMethod(arrayList, mid, env->NewObject(nativeProcessClass, cid, processId, exeName));
			}
		}

		CloseHandle(hProcess);
	}

}

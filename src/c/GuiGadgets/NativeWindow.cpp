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

#include <jni.h>
#include <windows.h>
#include <jawt_md.h>

#include "org_bhavaya_ui_NativeWindow.h"



JNIEXPORT void JNICALL Java_org_bhavaya_ui_NativeWindow_setAlwaysOnTopWin32
  (JNIEnv *env, jobject obj, jint hwnd, jboolean alwaysOnTop)
{
	SetWindowPos((HWND) hwnd,alwaysOnTop ? HWND_TOPMOST : HWND_NOTOPMOST,0,0,0,0, SWP_NOMOVE|SWP_NOSIZE|SWP_NOACTIVATE);
	return;
}

JNIEXPORT void JNICALL Java_org_bhavaya_ui_NativeWindow_flashWin32
  (JNIEnv *env, jobject obj, jint hwnd, jint count, jint flags, jint flashRate)
{
	// show the taskbar
	HWND taskbarHwnd = FindWindow("Shell_TrayWnd", NULL);
  ShowWindow(taskbarHwnd, SW_SHOW); // this unfortunately doesn't work when taskbar is auto-hidden
	// flash the icon
  FLASHWINFO strFlashWinfo;
  ZeroMemory(&strFlashWinfo, sizeof(FLASHWINFO));
  strFlashWinfo.uCount = count;
  strFlashWinfo.dwFlags =  flags;
  strFlashWinfo.hwnd = (HWND)hwnd;
  strFlashWinfo.dwTimeout = flashRate;
  strFlashWinfo.cbSize = sizeof(strFlashWinfo);
  FlashWindowEx(&strFlashWinfo);
	return;
}

JNIEXPORT jint JNICALL Java_org_bhavaya_ui_NativeWindow_getWin32HwndByFrame
  (JNIEnv *env, jobject obj, jobject frame, jint javaMinorVersion)
{
	typedef jboolean (JNICALL* PJAWT_GetAWT) (JNIEnv*, JAWT*); // Function pointer

	JAWT jawt;
	JAWT_DrawingSurface* jds;
	JAWT_DrawingSurfaceInfo* jdsi;
	JAWT_Win32DrawingSurfaceInfo* jdsi_win;

	HMODULE hAWT;
	HWND hwnd;

	jboolean result;

	PJAWT_GetAWT JAWT_GetAWT;


	jclass systemClass;
	jmethodID getPropertyMethod;
	jstring javaHome;

	// This section of code is needed in order to find jawt.dll
	systemClass = env->FindClass ("java/lang/System");
	getPropertyMethod = env->GetStaticMethodID (systemClass, "getProperty", "(Ljava/lang/String;)Ljava/lang/String;");
	javaHome = (jstring)env->CallStaticObjectMethod(systemClass, getPropertyMethod, env->NewStringUTF("java.home"));

	LPTSTR javaHomeString = (LPTSTR) env->GetStringUTFChars(javaHome, NULL);
	char* jawtDllPath = new char[MAX_PATH];
	jawtDllPath[0] = 0;
	strcat (jawtDllPath, javaHomeString);
	strcat (jawtDllPath, "\\bin\\jawt.dll");
	env->ReleaseStringUTFChars(javaHome, javaHomeString);

	hAWT = LoadLibrary ((LPCTSTR) jawtDllPath);

	if (!hAWT) return 0;

	JAWT_GetAWT = (PJAWT_GetAWT)GetProcAddress(hAWT, "_JAWT_GetAWT@8");

	if (!JAWT_GetAWT) return 0;

	jawt.version = javaMinorVersion == 4 ? JAWT_VERSION_1_4 : JAWT_VERSION_1_3;
	result = JAWT_GetAWT (env, &jawt);

	jds = jawt.GetDrawingSurface (env, frame);
	jds->Lock(jds);
	jdsi = jds->GetDrawingSurfaceInfo (jds);
	jdsi_win = (JAWT_Win32DrawingSurfaceInfo *)jdsi->platformInfo;
	hwnd = jdsi_win->hwnd;

	jds->FreeDrawingSurfaceInfo(jdsi);
	jds->Unlock(jds);
	jawt.FreeDrawingSurface(jds);

	return (jint) hwnd;
}


JNIEXPORT jint JNICALL Java_org_bhavaya_ui_NativeWindow_getWin32HwndByFrameTitle
  (JNIEnv *env, jobject obj, jstring frameTitle)
{
	HWND hwnd = NULL;
	const char *str = env->GetStringUTFChars(frameTitle, 0);
	hwnd = ::FindWindow(NULL, str);
	env->ReleaseStringUTFChars(frameTitle, str);

	return (jint) hwnd;
}

/*
 *  Environment.cpp
 *  Environment
 *
 *  Created by Brendon McLean on 17/07/2005.
 *  Copyright 2005 __MyCompanyName__. All rights reserved.
 *
 */


#include <iostream>
#include <strstream>
#include "org_bhavaya_util_Environment.h"
#include <jni.h>

JNIEXPORT jstring JNICALL Java_org_bhavaya_util_Environment_getProperty
  (JNIEnv * env, jclass aClass, jstring property)
{
   const char *prop = env->GetStringUTFChars(property, NULL);
   char * value = getenv(prop);
   env->ReleaseStringUTFChars(property, prop);

   jstring ret = env->NewStringUTF(value);
   return ret;
}
/*
 * Class:     org_bhavaya_util_Environment
 * Method:    setProperty
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_bhavaya_util_Environment_setProperty
  (JNIEnv * env, jclass aClass, jstring property, jstring value)
{
   const char *prop = env->GetStringUTFChars(property, NULL);
   const char *val  = env->GetStringUTFChars(value, NULL);
   setenv(prop,val,1); /* for now we are ignoring the return value to indicate success or failure*/
   env->ReleaseStringUTFChars(property, prop);
   env->ReleaseStringUTFChars(value, val);
}




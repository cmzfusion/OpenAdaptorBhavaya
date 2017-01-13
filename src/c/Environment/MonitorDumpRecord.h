// MonitorDumpRecord.h: interface for the MonitorDumpRecord class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(AFX_MONITORDUMPRECORD_H__57662E3E_86EC_4DED_9B4E_B931634BDD17__INCLUDED_)
#define AFX_MONITORDUMPRECORD_H__57662E3E_86EC_4DED_9B4E_B931634BDD17__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include <jni.h>
#include <iostream>

using namespace std;

class MonitorDumpRecord
{
public:
	MonitorDumpRecord();
	virtual ~MonitorDumpRecord();

public:
    JNIEnv * owner_thread;
	jint entry_count;
	jint threads_waiting_to_enter_count;
	JNIEnv ** threads_waiting_to_enter;	//(JNIEnv *)[]
	jint threads_waiting_to_be_notified_count;
	JNIEnv ** threads_waiting_to_be_notified; //(JNIEnv *)[]

    virtual ostream& print(ostream&);
	friend ostream & operator<<( ostream &stream, MonitorDumpRecord &rec );
	void* readAndAdvance(void* bufPtr);


private:
	void printStackTraces(ostream &stream, JNIEnv** threadArray, int count);

};

#endif // !defined(AFX_MONITORDUMPRECORD_H__57662E3E_86EC_4DED_9B4E_B931634BDD17__INCLUDED_)

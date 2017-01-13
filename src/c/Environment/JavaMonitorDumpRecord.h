// JavaMonitorDumpRecord.h: interface for the JavaMonitorDumpRecord class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(AFX_JAVAMONITORDUMPRECORD_H__79066616_9302_4E64_AE15_AFC834BEAEA7__INCLUDED_)
#define AFX_JAVAMONITORDUMPRECORD_H__79066616_9302_4E64_AE15_AFC834BEAEA7__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include <jvmpi.h>

#include "MonitorDumpRecord.h"


class JavaMonitorDumpRecord : public MonitorDumpRecord  
{
public:
	JavaMonitorDumpRecord();
	virtual ~JavaMonitorDumpRecord();
	jobjectID object;

	
    virtual ostream& print(ostream&);
	void* readAndAdvance(void* bufPtr);
};


#endif // !defined(AFX_JAVAMONITORDUMPRECORD_H__79066616_9302_4E64_AE15_AFC834BEAEA7__INCLUDED_)

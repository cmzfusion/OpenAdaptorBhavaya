// RawMonitorDumpRecord.h: interface for the RawMonitorDumpRecord class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(AFX_RAWMONITORDUMPRECORD_H__78F5F63B_D702_473B_8A1D_9E3F550C6AE8__INCLUDED_)
#define AFX_RAWMONITORDUMPRECORD_H__78F5F63B_D702_473B_8A1D_9E3F550C6AE8__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include <jvmpi.h>
#include "MonitorDumpRecord.h"

class RawMonitorDumpRecord : public MonitorDumpRecord {
public:
	RawMonitorDumpRecord();
	virtual ~RawMonitorDumpRecord();

	char * raw_monitor_name; 
	JVMPI_RawMonitor raw_monitor;

    ostream& print(ostream&);
	void* readAndAdvance(void* bufPtr);
};

#endif // !defined(AFX_RAWMONITORDUMPRECORD_H__78F5F63B_D702_473B_8A1D_9E3F550C6AE8__INCLUDED_)

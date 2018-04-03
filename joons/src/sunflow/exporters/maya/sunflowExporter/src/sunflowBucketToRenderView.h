#ifndef _SUNFLOW_BUCKET_TO_RENDER_VIEW_H_
#define _SUNFLOW_BUCKET_TO_RENDER_VIEW_H_

#include <maya/MDagPath.h>
#include <iostream>
#include <vector>

typedef long sint32 ;
typedef unsigned long uint32 ;
typedef short int sint16 ;
typedef unsigned short int uint16 ;
typedef signed char sint8;
typedef unsigned char uint8 ;

union data32 {
	sint32 sbits32;
	uint32 ubits32;
	sint16 sbits16[2];
	uint16 ubits16[2];
	sint8  sbits8[4];
	uint8  ubits8[4];
	float  f;
	// TODO: half h[2];
};

struct DisplayPacket {
  data32 type;
  data32 data[4];
};

class bucketToRenderView
{
public:
	DisplayPacket getPacket();
	void checkStream();

	FILE* renderPipe;

	int renderWidth;
	int renderHeight;
	MDagPath renderCamera;
};

#endif /* _SUNFLOW_BUCKET_TO_RENDER_VIEW_H_ */

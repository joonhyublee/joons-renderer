#include <maya/MFnPlugin.h>
#include <maya/MGlobal.h>

#include "sunflowGlobalsNode.h"
#include "sunflowExportCmd.h"
#include "sunflowShaderNode.h"
#include "sunflowSkyNode.h"
#include "sunflowHelperNode.h"

MStatus initializePlugin( MObject obj )
{ 
	MStatus   status;
	MFnPlugin plugin( obj, "Alias", "3.0", "Any");

	status = plugin.registerNode( "sunflowGlobalsNode", sunflowGlobalsNode::id, &sunflowGlobalsNode::creator, &sunflowGlobalsNode::initialize, MPxNode::kDependNode );
	if (!status) { status.perror("registerNode: sunflowGlobalsNode"); return status;}

    status = plugin.registerCommand("sunflowExportCmd", sunflowExportCmd::creator );
	if (!status) status.perror("registerCommand: sunflowExportCmd");

	const MString UserClassify( "shader/surface" );
	status = plugin.registerNode( "sunflowShader", sunflowShaderNode::id, sunflowShaderNode::creator, sunflowShaderNode::initialize, MPxNode::kDependNode, &UserClassify );
	if (!status) status.perror("registerNode: sunflowShaderNode");

	status = plugin.registerNode( "sunflowSkyNode", sunflowSkyNode::id, &sunflowSkyNode::creator, &sunflowSkyNode::initialize, MPxNode::kLocatorNode );
	if (!status)  status.perror("registerNode: sunflowSkyNode");

	status = plugin.registerNode( "sunflowHelperNode", sunflowHelperNode::id, sunflowHelperNode::creator, sunflowHelperNode::initialize, MPxNode::kLocatorNode );	
	if (!status)  status.perror("registerNode: sunflowHelperNode");

	status = MGlobal::executeCommand("source sunflowStartup.mel");
	status = plugin.registerUI("sunflowStartup", "sunflowShutdown");
	if (!status) { status.perror("Can't register sunflowStartup and sunflowShutdown interface scripts"); return status;}
	return status;
}

MStatus uninitializePlugin( MObject obj)
{
	MStatus   status;
	MFnPlugin plugin( obj );

	status = plugin.deregisterNode( sunflowGlobalsNode::id );
	if (!status) { status.perror("deregisterNode: sunflowGlobalsNode");	return status;}

	status = plugin.deregisterCommand("sunflowExportCmd");
	if (!status) status.perror("deregisterCommand: sunflowExportCmd");

	status = plugin.deregisterNode( sunflowShaderNode::id );
	if (!status) status.perror("deregisterNode: sunflowShaderNode");

	status = plugin.deregisterNode( sunflowSkyNode::id );
	if (!status) status.perror("deregisterNode: sunflowSkyNode");

	status = plugin.deregisterNode( sunflowHelperNode::id );
	if (!status) status.perror("deregisterNode: sunflowHelperNode");

	//MGlobal::executeCommandOnIdle("unregisterSunflowRenderer");
	return status;
}

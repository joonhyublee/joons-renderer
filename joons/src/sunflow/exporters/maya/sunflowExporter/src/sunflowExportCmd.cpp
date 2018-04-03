#include "sunflowBucketToRenderView.h"
#include "sunflowExportCmd.h"
#include "sunflowShaderNode.h"
#include "sunflowConstants.h"
#include <maya/MPlug.h>
#include <maya/MGlobal.h>
#include <maya/MArgList.h>
#include <maya/MItDag.h>
#include <maya/MItDependencyNodes.h>
#include <maya/MDagPathArray.h>
#include <maya/MFnMesh.h>
#include <maya/MFnCamera.h>
#include <maya/MFnLambertShader.h>
#include <maya/MFnPhongShader.h>
#include <maya/MFnAreaLight.h>
#include <maya/MFnDirectionalLight.h>
#include <maya/MFnPointLight.h>
#include <maya/MItMeshPolygon.h>
#include <maya/MFnNurbsSurface.h>
#include <maya/MItSurfaceCV.h>
#include <maya/MIntArray.h>
#include <maya/MPointArray.h>
#include <maya/MVectorArray.h>
#include <maya/MObjectArray.h>
#include <maya/MPoint.h>
#include <maya/MVector.h>
#include <maya/MMatrix.h>
#include <maya/MAngle.h>
#include <maya/MSelectionList.h>
#include <maya/MPlugArray.h>
#include <maya/MStringArray.h>
#include <iostream>
#include <fstream>
#include <set>
#include <vector>
#include <string>

// global variables:

using namespace std;

void sunflowExportCmd::getCustomAttribute(MFloatVector &colorAttribute, MString attribute, MFnDependencyNode &node){
	MPlug paramPlug;
	MStatus status;
	paramPlug = node.findPlug( attribute, &status );
	if ( status == MS::kSuccess ){
		paramPlug.child( 0 ).getValue( colorAttribute.x );
		paramPlug.child( 1 ).getValue( colorAttribute.y );
		paramPlug.child( 2 ).getValue( colorAttribute.z );
	}else{
		colorAttribute.x = 0;
		colorAttribute.y = 0;
		colorAttribute.z = 0;
	}
	return;
}

void sunflowExportCmd::getCustomAttribute(MString &texture, MFloatVector &colorAttribute, MString attribute, MFnDependencyNode &node){
	MPlug paramPlug;
	MStatus status;
	texture = "";
	paramPlug = node.findPlug( attribute, &status );
	if ( status == MS::kSuccess ){
		MPlugArray connectedPlugs;
		if(paramPlug.connectedTo(connectedPlugs,true,false,&status) && connectedPlugs.length()){
			if(connectedPlugs[0].node().apiType() == MFn::kFileTexture){
				MFnDependencyNode texNode(connectedPlugs[0].node());
				MPlug texturePlug = texNode.findPlug( "fileTextureName", &status );
				if ( status == MS::kSuccess ){
					texturePlug.getValue( texture );
					std::cout << "Texture: " << texture.asChar() << std::endl;
				}
			}
		}
		paramPlug.child( 0 ).getValue( colorAttribute.x );
		paramPlug.child( 1 ).getValue( colorAttribute.y );
		paramPlug.child( 2 ).getValue( colorAttribute.z );
	}else{
		colorAttribute.x = 0;
		colorAttribute.y = 0;
		colorAttribute.z = 0;
	}
	return;
}

void sunflowExportCmd::getCustomAttribute(float &floatAttribute, MString attribute, MFnDependencyNode &node){
	MPlug paramPlug;
	MStatus status;
	paramPlug = node.findPlug( attribute, &status );
	if ( status == MS::kSuccess ) {
		paramPlug.getValue( floatAttribute );
	}else{
		floatAttribute=0;
	}
	return;
}

void sunflowExportCmd::getCustomAttribute(int &intAttribute, MString attribute, MFnDependencyNode &node){
	MPlug paramPlug;
	MStatus status;
	paramPlug = node.findPlug( attribute, &status );
	if ( status == MS::kSuccess ) {
		paramPlug.getValue( intAttribute );
	}else{
		intAttribute=0;
	}
	return;
}

void sunflowExportCmd::getCustomAttribute(MString &stringAttribute, MString attribute, MFnDependencyNode &node){
	MPlug paramPlug;
	MStatus status;
	paramPlug = node.findPlug( attribute, &status );
	if ( status == MS::kSuccess ) {
		paramPlug.getValue( stringAttribute );
	}else{
		stringAttribute="";
	}
	return;
}

void sunflowExportCmd::getCustomAttribute(bool &boolAttribute, MString attribute, MFnDependencyNode &node){
	MPlug paramPlug;
	MStatus status;
	paramPlug = node.findPlug( attribute, &status );
	if ( status == MS::kSuccess ) {
		paramPlug.getValue( boolAttribute );
	}else{
		boolAttribute=false;
	}
	return;
}

bool sunflowExportCmd::getBumpFromShader(MFnDependencyNode& node, MString &texturePath, float &depth, MObject &bumpObject){
	// write bump modifiers
	MStatus status;	
	MPlug paramPlug;
	paramPlug = node.findPlug( "normalCamera", &status );
	if ( status == MS::kSuccess ){		
		MPlugArray connectedPlugs;
		if(paramPlug.connectedTo(connectedPlugs,true,false,&status) && connectedPlugs.length()){			
			if(connectedPlugs[0].node().apiType() == MFn::kBump){				
				MFnDependencyNode bumpNode(connectedPlugs[0].node());
				MPlug bumpPlug = bumpNode.findPlug( "bumpDepth", &status );
				if ( status == MS::kSuccess ){					
					bumpPlug.getValue( depth );
					paramPlug = bumpNode.findPlug( "bumpValue", &status );
					if ( status == MS::kSuccess ){						
						if(paramPlug.connectedTo(connectedPlugs,true,false,&status) && connectedPlugs.length()){							
							if(connectedPlugs[0].node().apiType() == MFn::kFileTexture){								
								MFnDependencyNode texNode(connectedPlugs[0].node());
								MPlug texPlug = texNode.findPlug( "fileTextureName", &status );
								if ( status == MS::kSuccess ){									
									texPlug.getValue( texturePath );									
									bumpObject = bumpNode.object();
									return true;
								}
							}
						}
					}
				}
			}
		}
	}
	return false;
}

bool sunflowExportCmd::isObjectVisible(const MDagPath& path) {
    MStatus status;
    MFnDagNode node(path);
    MPlug vPlug = node.findPlug("visibility", &status);
    bool visible = true;
    if (status == MS::kSuccess)
        vPlug.getValue(visible);
    status.clear();
    MPlug iPlug = node.findPlug("intermediateObject", &status);
    bool intermediate = false;
    if (status == MS::kSuccess)
        iPlug.getValue(intermediate);
    return visible && !intermediate;
}

bool sunflowExportCmd::areObjectAndParentsVisible(const MDagPath& path) {
    bool result = true;
    MDagPath searchPath(path);
    for (;;) {
        if (!isObjectVisible(searchPath)) {
            result = false;
            break;
        }
        if (searchPath.length() <= 1)
            break;
        searchPath.pop();
    }
    return result;
}

bool sunflowExportCmd::isCameraRenderable(const MDagPath& path) {
    MStatus status;
    MFnDagNode node(path);
    MPlug rPlug = node.findPlug("renderable", &status);
    bool renderable = true;
    if (status == MS::kSuccess)
        rPlug.getValue(renderable);
    return renderable;
}

int sunflowExportCmd::getAttributeInt(const std::string& node, const std::string& attributeName, int defaultValue) {
    MStatus status;
    MSelectionList list;
    status = list.add((node + "." + attributeName).c_str());
    if (status != MS::kSuccess)
        return defaultValue;
    MPlug plug;
    status = list.getPlug(0, plug);
    if (status != MS::kSuccess)
        return defaultValue;
    int value;
    status = plug.getValue(value);
    if (status != MS::kSuccess)
        return defaultValue;
    return value;
}

float sunflowExportCmd::getAttributeFloat(const std::string& node, const std::string& attributeName, float defaultValue) {
    MStatus status;
    MSelectionList list;
    status = list.add((node + "." + attributeName).c_str());
    if (status != MS::kSuccess)
        return defaultValue;
    MPlug plug;
    status = list.getPlug(0, plug);
    if (status != MS::kSuccess)
        return defaultValue;
    float value;
    status = plug.getValue(value);
    if (status != MS::kSuccess)
        return defaultValue;
    return value;
}

bool sunflowExportCmd::getShaderFromEngine(const MObject& obj, MFnDependencyNode& node) {
    if (!obj.hasFn(MFn::kShadingEngine))
        return false; // not a shading engine
    MFnDependencyNode seNode(obj);
    // find connected shader
    MPlug shaderPlug = seNode.findPlug("surfaceShader");
    if (shaderPlug.isNull())
        return false; // this shading group does not contain any surface shaders
    // list all plugs which connect TO this shading group
    MPlugArray plugs; 
    shaderPlug.connectedTo(plugs, true, false);
    for (unsigned int i = 0; i < plugs.length(); i++) {
        MObject sObj = plugs[i].node();
        // FIXME: not all shaders derive from kLambert
        //if (sObj.hasFn(MFn::kLambert)) {
            // we have found a shader
            node.setObject(sObj);
            return true;
        //}
    }
    return false;
}

bool sunflowExportCmd::getShaderFromGeometry(const MDagPath& path, MFnDependencyNode& node) {
    MStatus status;
    MFnDagNode geom(path);
    MObject iog = geom.attribute("instObjGroups", &status);
    if (!status)
        return false; // not a renderable object
    MPlug iogPlug(geom.object(), iog);
    MPlugArray iogPlugs;
    iogPlug.elementByLogicalIndex(0).connectedTo(iogPlugs, false, true, &status);
    if (!status)
        return false; // no shading group defined
    for (unsigned int i = 0; i < iogPlugs.length(); i++) {
        MObject seObj = iogPlugs[i].node();
        if (seObj.hasFn(MFn::kShadingEngine))
            return getShaderFromEngine(seObj, node); // retrieve the actual shader node from the shading engine
    }
    return false; // no shading engines found
}


void sunflowExportCmd::exportMesh(const MDagPath& path, std::ofstream& file) {
	MStatus status;
    bool instancing = path.isInstanced();
    if (instancing) {
        if (path.instanceNumber() != 0)
            return; // this instance will be handled somewhere else
        else {
            MDagPathArray paths;
            path.getAllPathsTo(path.node(), paths);
            bool hasVisible = false;
            for (unsigned int i = 0; i < paths.length() && !hasVisible; i++)
                hasVisible |= areObjectAndParentsVisible(paths[i]);
            if (!hasVisible)
                return; // none of the instance are visible
        }
    } else if (!areObjectAndParentsVisible(path))
        return;

    MFnMesh mesh(path);

    int numPoints = mesh.numVertices();
    int numTriangles = 0;
    for (MItMeshPolygon it(path); !it.isDone(); it.next()) {
        int tri;
        it.numTriangles(tri);
        numTriangles += tri;
    }

    if (numPoints == 0 || numTriangles == 0) return;

    file << "object {" << std::endl;
    if (instancing) {
        // instances will be created manually later on
        file << "\tnoinstance" << std::endl;
    }
    // get shader table
    MObjectArray shaders;
    MIntArray polyShaderIndices;
    mesh.getConnectedShaders(path.instanceNumber(), shaders, polyShaderIndices);
	MStringArray shaderNames;
    //std::vector<std::string> shaderNames(shaders.length());
	MFnDependencyNode shader;
    for (unsigned int i = 0; i < shaders.length(); i++) {
        MObject engine = shaders[i];
        if (getShaderFromEngine(engine, shader))
			shaderNames.append(shader.name());
        else
           shaderNames.append("default");
    }
    if (!instancing) {		
        // write shaders		
		if (shaderNames.length() == 0){
            file << "\tshader default" << std::endl;
		}else if (shaderNames.length() == 1){			
			file << "\tshader " << shaderNames[0].asChar() << std::endl;
		}else {
            file << "\tshaders " << shaderNames.length() << std::endl;
            for (unsigned int i = 0; i < shaderNames.length(); i++)
				file << "\t\t" << shaderNames[i].asChar() << std::endl;
        }
		// write modifiers
		MString texture;
		float depth;
		MObject bumpObject;
		if(getBumpFromShader(shader, texture, depth, bumpObject)){
			MFnDependencyNode bumpNode(bumpObject);
			file << "\tmodifier " << bumpNode.name().asChar() << std::endl;
		}
        // instance this mesh directly
        file << "\ttransform col ";
        MMatrix o2w = path.inclusiveMatrix();
        file << o2w[0][0] << " " << o2w[0][1] << " " << o2w[0][2] << " " << o2w[0][3] << " ";
        file << o2w[1][0] << " " << o2w[1][1] << " " << o2w[1][2] << " " << o2w[1][3] << " ";
        file << o2w[2][0] << " " << o2w[2][1] << " " << o2w[2][2] << " " << o2w[2][3] << " ";
        file << o2w[3][0] << " " << o2w[3][1] << " " << o2w[3][2] << " " << o2w[3][3] << std::endl;
    }
    file << "\ttype generic-mesh" << std::endl;
    file << "\tname \"" << path.fullPathName().asChar() << "\"" << std::endl;
    file << "\tpoints " << numPoints << std::endl;

    // write points
    MSpace::Space space = MSpace::kObject;
    MPointArray points;
    mesh.getPoints(points, space);
    for (int i = 0; i < numPoints; i++) {
        file << "\t\t" << points[i].x << " " << points[i].y << " " << points[i].z << std::endl;
    }

    // get UVSets for this mesh
    MStringArray uvSets;
    mesh.getUVSetNames(uvSets);
    int numUVSets = uvSets.length();
    int numUVs = numUVSets > 0 ? mesh.numUVs(uvSets[0]) : 0;

    // get normals
    MFloatVectorArray normals;
    mesh.getNormals(normals, space);


    // write triangles
    file << "\ttriangles " << numTriangles << std::endl;
    unsigned int t = 0;
    MPointArray nonTweaked; // not used
    // object-relative vertex indices for each triangle
    MIntArray triangleVertices;
    // face-relative vertex indices for each triangle
    int localIndex[3];
    // object-relative indices for the vertices in each face.
    MIntArray polygonVertices;
    std::vector<float> faceNormals(3 * 3 * numTriangles);
    std::vector<float> faceUVs(numUVs > 0 ? 3 * 2 * numTriangles : 1);
    std::vector<int> faceMaterials(numTriangles);
    for (MItMeshPolygon mItMeshPolygon(path); !mItMeshPolygon.isDone(); mItMeshPolygon.next()) {
        mItMeshPolygon.getVertices(polygonVertices);
        int numVerts = (int) polygonVertices.length();
        // get triangulation of this poly.
        int numTriangles; mItMeshPolygon.numTriangles(numTriangles);
        while (numTriangles--) {
            mItMeshPolygon.getTriangle(numTriangles, nonTweaked, triangleVertices, MSpace::kObject);
            for (int gt = 0; gt < 3; gt++) {
                for (int gv = 0; gv < numVerts; gv++) {
                    if (triangleVertices[gt] == polygonVertices[gv]) {
                        localIndex[gt] = gv;
                        break;
                    }
                }
            }
            file << "\t\t" << triangleVertices[0] << " " << triangleVertices[1] << " " << triangleVertices[2] << std::endl;

            // per triangle normals
            int nidx0 = mItMeshPolygon.normalIndex(localIndex[0]);
            int nidx1 = mItMeshPolygon.normalIndex(localIndex[1]);
            int nidx2 = mItMeshPolygon.normalIndex(localIndex[2]);
            faceNormals[3 * 3 * t + 0] = normals[nidx0].x;
            faceNormals[3 * 3 * t + 1] = normals[nidx0].y;
            faceNormals[3 * 3 * t + 2] = normals[nidx0].z;
            faceNormals[3 * 3 * t + 3] = normals[nidx1].x;
            faceNormals[3 * 3 * t + 4] = normals[nidx1].y;
            faceNormals[3 * 3 * t + 5] = normals[nidx1].z;
            faceNormals[3 * 3 * t + 6] = normals[nidx2].x;
            faceNormals[3 * 3 * t + 7] = normals[nidx2].y;
            faceNormals[3 * 3 * t + 8] = normals[nidx2].z;

            // texture coordinates
            if (numUVs > 0) {
                float2 uv0;
                float2 uv1;
                float2 uv2;
                mItMeshPolygon.getUV(localIndex[0], uv0, &uvSets[0]);
                mItMeshPolygon.getUV(localIndex[1], uv1, &uvSets[0]);
                mItMeshPolygon.getUV(localIndex[2], uv2, &uvSets[0]);
                faceUVs[3 * 2 * t + 0] = uv0[0];
                faceUVs[3 * 2 * t + 1] = uv0[1];
                faceUVs[3 * 2 * t + 2] = uv1[0];
                faceUVs[3 * 2 * t + 3] = uv1[1];
                faceUVs[3 * 2 * t + 4] = uv2[0];
                faceUVs[3 * 2 * t + 5] = uv2[1];
            }

            // per face materials
            if (shaderNames.length() > 1)
                faceMaterials[t] = polyShaderIndices[mItMeshPolygon.index()];
            t++;
        }
    }
    // write normals
    file << "\tnormals facevarying" << std::endl;
    for (int t = 0, idx = 0; t < numTriangles; t++, idx += 9) {
        file << "\t\t" << faceNormals[idx + 0];
        for (int j = 1; j < 9; j++)
            file << " " << faceNormals[idx + j];
        file << std::endl;
    }
    // write uvs
    if (numUVs > 0) {
        file << "\tuvs facevarying" << std::endl;
        for (int t = 0, idx = 0; t < numTriangles; t++, idx += 6) {
            file << "\t\t" << faceUVs[idx + 0];
            for (int j = 1; j < 6; j++)
                file << " " << faceUVs[idx + j];
            file << std::endl;
        }
    } else
        file << "\tuvs none" << std::endl;
    // write per-face materials
    if (shaderNames.length() > 1) {
        file << "\tface_shaders" << std::endl;
        for (int t = 0; t < numTriangles; t++)
            file << "\t\t" << faceMaterials[t] << std::endl;
    }
    file << "}" << std::endl;
    file << std::endl;

    if (instancing) {
        MDagPathArray paths;
        path.getAllPathsTo(path.node(), paths);
        for (unsigned int i = 0; i < paths.length(); i++) {
            if (!areObjectAndParentsVisible(paths[i])) continue;
            file << "instance {" << std::endl;
            file << "\tname \"" << paths[i].fullPathName().asChar() << ".instance\"" << std::endl;
            file << "\tgeometry \"" << path.fullPathName().asChar() << "\"" <<  std::endl;
            file << "\ttransform col ";
            MMatrix o2w = paths[i].inclusiveMatrix();
            file << o2w[0][0] << " " << o2w[0][1] << " " << o2w[0][2] << " " << o2w[0][3] << " ";
            file << o2w[1][0] << " " << o2w[1][1] << " " << o2w[1][2] << " " << o2w[1][3] << " ";
            file << o2w[2][0] << " " << o2w[2][1] << " " << o2w[2][2] << " " << o2w[2][3] << " ";
            file << o2w[3][0] << " " << o2w[3][1] << " " << o2w[3][2] << " " << o2w[3][3] << std::endl;
            MObjectArray instanceShaders;
            MFnMesh instancedMesh(paths[i]);
            instancedMesh.getConnectedShaders(paths[i].instanceNumber(), instanceShaders, polyShaderIndices);
            std::vector<std::string> instanceShaderNames(instanceShaders.length());
            for (unsigned int i = 0; i < instanceShaders.length(); i++) {
                MObject engine = instanceShaders[i];
                MFnDependencyNode shader;
                if (getShaderFromEngine(engine, shader))
                    instanceShaderNames[i] = shader.name().asChar();
                else
                    instanceShaderNames[i] = "default";
            }
            if (instanceShaderNames.size() == 0)
                file << "\tshader default" << std::endl;
            else if (instanceShaderNames.size() == 1)
                file << "\tshader " << instanceShaderNames[0] << std::endl;
            else {
                file << "\tshaders " << instanceShaderNames.size() << std::endl;
                for (size_t i = 0; i < instanceShaderNames.size(); i++)
                    file << "\t\t" << instanceShaderNames[i] << std::endl;
            }
            file << "\t}" << std::endl;
        }
    }
}

void sunflowExportCmd::exportSurface(const MDagPath& path, std::ofstream& file) {
	MStatus stat;
	MFnNurbsSurface surface(path, &stat);

	if(surface.formInU() == MFnNurbsSurface::kPeriodic || surface.formInV() == MFnNurbsSurface::kPeriodic){
		cout << "No support for wrapped patches: " << surface.name().asChar() << " | Skipping!" << endl;
		return;
	}

	file << "\n\nobject {" << std::endl;
	file << "\tshader default"  << std::endl;	
	file << "\ttype bezier-mesh"  << std::endl;	
	file << "\tn " << surface.numCVsInU() << " " << surface.numCVsInV() << std::endl;
	MString wrapU = (surface.formInU() == MFnNurbsSurface::kPeriodic)?"true":"false";
	MString wrapV = (surface.formInV() == MFnNurbsSurface::kPeriodic)?"true":"false";
	file << "\twrap " << wrapU.asChar() << " " << wrapV.asChar() << std::endl;
	file << "\tpoints "  << std::endl;
	
	MItSurfaceCV cvIter( path, MObject::kNullObj, true, &stat );
    MPoint cvPos(0.0,0.0,0.0);

    if ( MS::kSuccess == stat ) {
        for ( ; !cvIter.isDone(); cvIter.nextRow() )
        {
            for ( ; !cvIter.isRowDone(); cvIter.next() )
            {
				cvPos = cvIter.position(MSpace::kWorld, &stat);
				file << "\t" << cvPos.x << " " << cvPos.y << " " << cvPos.z << std::endl;
            }
        }        
    }
    else {
        cerr << "Error creating iterator!" << endl;
    }
	file << "}" << std::endl;
}

void sunflowExportCmd::exportCamera(const MDagPath& path, std::ofstream& file) {
    MFnCamera camera(path);

    if (!isCameraRenderable(path)) return;
    if (camera.isOrtho()) return;

    MSpace::Space space = MSpace::kWorld;

    MPoint eye = camera.eyePoint(space);
    MVector dir = camera.viewDirection(space);
    MVector up = camera.upDirection(space);
    double fov = camera.horizontalFieldOfView() * (180.0 / 3.1415926535897932384626433832795);

	bool DOF = camera.isDepthOfField();
	float frameAspect = getAttributeFloat("defaultResolution", "deviceAspectRatio", 1.333333f);

	file << "\n\n% " << path.fullPathName().asChar() << std::endl;
	file << "camera {" << std::endl;
	if(DOF){
		float FocusDist = camera.focusDistance();
		float fStop = camera.fStop();
		file << "\ttype   thinlens" << std::endl;
		file << "\teye    " << eye.x << " " << eye.y << " " << eye.z << std::endl;
		file << "\ttarget " << (eye.x + dir.x) << " " << (eye.y + dir.y) << " " << (eye.z + dir.z) << std::endl;
		file << "\tup     " << up.x << " " << up.y << " " << up.z << std::endl;
		file << "\tfov    " << fov << std::endl;
		file << "\taspect " << frameAspect << std::endl;
		file << "\tfdist " << FocusDist << std::endl;
		file << "\tlensr " << fStop << std::endl;
	}else{
		file << "\ttype   pinhole" << std::endl;
		file << "\teye    " << eye.x << " " << eye.y << " " << eye.z << std::endl;
		file << "\ttarget " << (eye.x + dir.x) << " " << (eye.y + dir.y) << " " << (eye.z + dir.z) << std::endl;
		file << "\tup     " << up.x << " " << up.y << " " << up.z << std::endl;
		file << "\tfov    " << fov << std::endl;
		file << "\taspect " << frameAspect << std::endl;
	}
	file << "}" << std::endl;
}

bool sunflowExportCmd::findShaderInList(MString shader){
	for(unsigned int i=0; i<shaderList.length(); i++){		
		MFnDependencyNode currentShader(shaderList[i]);		
		if(currentShader.name()==shader){
			return true;
		}
		
	}
	return false;
}

MStatus sunflowExportCmd::doIt(const MArgList& args) {
    if (args.length() < 1) return MS::kFailure;
    MString mode;
    MString exportPath;
    const char* javaPath    = 0;
    const char* sunflowPath = 0;
    const char* javaArgs    = 0;
    if (args.length() == 1) {
        exportPath = args.asString(0);
        mode = "file";
    } else
        mode = "render";
    std::cout << "Exporting in mode: " << mode.asChar() << " ..." << std::endl;
    std::ofstream file;

	int materialOverride = 0;
	float ambOverrideDist = 1;

	MStatus status;

	// Write out globals values from the first sunflowGlobalsNode
	bool globalsWritten = false;
	for (MItDependencyNodes it(MFn::kPluginDependNode ); !it.isDone(&status); it.next()) {		
		MObject obj = it.item();		
		MFnDependencyNode globals(obj,&status);
		if (!status) { status.perror("MFnDependencyNode globals"); return status;}
		if( globals.typeName() != "sunflowGlobalsNode" )
			continue;

		std::cout << "Found globals node: "<< globals.name().asChar() <<  endl;

		//Get globals values
		MPlug paramPlug;

		// set renderMode
		int renderMode;		
		getCustomAttribute(renderMode, "renderMode", globals);
		switch (renderMode){
			case 0:
				mode="render";
				break;
			case 1:
				mode="IPR";
				break;
			case 2:
				mode="file";
				break;
			default:
				break;
		}


        if (exportPath.length() == 0) {		
            getCustomAttribute(exportPath, "exportPath", globals);		
            exportPath += "/sunflowTmp.sc";
        }

		// use environment variables if they exist:
		sunflowPath = getenv("SUNFLOW_PATH");
		if (sunflowPath == 0)
            MGlobal::displayWarning("SUNFLOW_PATH environment variable was not set!");
        else
            std::cout << "Using sunflow path: " << sunflowPath << std::endl;
		javaPath = getenv("SUNFLOW_JAVA_PATH");
		if (javaPath == 0)
            MGlobal::displayWarning("SUNFLOW_PATH environment variable was not set!");
        else
			std::cout << "Using java path:    " << javaPath << std::endl;
        javaArgs = getenv("SUNFLOW_JAVA_ARGS");
        
        

		file.open(exportPath.asChar());

		//IMAGE BLOCK OUTPUT
		int pixelFilter;		
		getCustomAttribute(pixelFilter, "pixelFilter", globals);
		if (pixelFilter < 0)
			pixelFilter = 0;
		else if (pixelFilter >= (int) NUM_FILTER_NAMES)
			pixelFilter = NUM_FILTER_NAMES - 1;

		int minSamples;		
		getCustomAttribute(minSamples, "minSamples", globals);

		int maxSamples;		
		getCustomAttribute(maxSamples, "maxSamples", globals);

		int resX = getAttributeInt("defaultResolution", "width" , 640);
		int resY = getAttributeInt("defaultResolution", "height", 480);		

		file << "image {" << std::endl;
		file << "\tresolution " << resX << " " << resY << std::endl;
		file << "\taa " << minSamples << " " << maxSamples << std::endl;
		file << "\tfilter " << FILTER_NAMES[pixelFilter] << std::endl;
		file << "}" << std::endl;
		file << std::endl;


		//TRACE_DEPTHS BLOCK OUTPUT
		int diffuseDepth;		
		getCustomAttribute(diffuseDepth, "diffuseDepth", globals);

		int reflectionDepth;		
		getCustomAttribute(reflectionDepth, "reflectionDepth", globals);

		int refractionDepth;		
		getCustomAttribute(refractionDepth, "refractionDepth", globals);

		file << "trace-depths {" << std::endl;
		file << "\tdiff " << diffuseDepth << std::endl;
		file << "\trefl " << reflectionDepth << std::endl;
		file << "\trefr " << refractionDepth << std::endl;
		file << "}" << std::endl;


		//TRACE_DEPTHS BLOCK OUTPUT
		bool enablePhotons;		
		getCustomAttribute(enablePhotons, "enablePhotons", globals);

		if(enablePhotons){
			file << "photons {" << std::endl;

			int Photons;		
			getCustomAttribute(Photons, "Photons", globals);

			float PhotonsKd;		
			getCustomAttribute(PhotonsKd, "PhotonsKd", globals);

			float PhotonsRadius;		
			getCustomAttribute(PhotonsRadius, "PhotonsRadius", globals);

			file << "\tcaustics " << Photons << std::endl;
			file << "\tkd " << PhotonsKd << " " << PhotonsRadius << std::endl;			
			file << "}" << std::endl;
			file << std::endl;
		}

		bool enableGI;		
		getCustomAttribute(enableGI, "enableGI", globals);

		if(enableGI){
			int GIMode;		
			getCustomAttribute(GIMode, "GIMode", globals);

			file << "gi {" << std::endl;
			switch (GIMode){
				case 0: { // PATH TRACING
							int PTSamples;		
							getCustomAttribute(PTSamples, "PTSamples", globals);
							file << "\ttype path" << std::endl;
							file << "\tsamples " << PTSamples << std::endl;
						}break;
				case 1: { // IGI



							int IGISamples;
							getCustomAttribute(IGISamples, "IGISamples", globals);

							int IGISets;
							getCustomAttribute(IGISets, "IGISets", globals);

							float IGIBias;
							getCustomAttribute(IGIBias, "IGIBias", globals);

							int IGIBSamples;
							getCustomAttribute(IGIBSamples, "IGIBSamples", globals);

							file << "\ttype igi" << std::endl;
							file << "\tsamples " << IGISamples << std::endl;
							file << "\tsets " << IGISets << std::endl;
							file << "\tb " << IGIBias << std::endl;
							file << "\tbias-samples " << IGIBSamples << std::endl;
						}break;
				case 2: { // IRRADIANCE CACHE
							int ICSamples;							
							getCustomAttribute(ICSamples, "ICSamples", globals);

							float ICTolerance;							
							getCustomAttribute(ICTolerance, "ICTolerance", globals);

							float ICSpacingMin;							
							getCustomAttribute(ICSpacingMin, "ICSpacingMin", globals);

							float ICSpacingMax;							
							getCustomAttribute(ICSpacingMax, "ICSpacingMax", globals);

							file << "\ttype irr-cache" << std::endl;
							file << "\tsamples " << ICSamples << std::endl;
							file << "\ttolerance 0.01" << ICTolerance << std::endl;
							file << "\tspacing 0.05 5.0" << ICSpacingMin << " " << ICSpacingMax << std::endl;							
						}break;
				default: break;
			}
			file << "}" << std::endl;
			file << std::endl;
		}

		//BUCKET BLOCK OUTPUT
		int bucketOrder;		
		getCustomAttribute(bucketOrder, "bucketOrder", globals);
		if (bucketOrder < 0)
			bucketOrder = 0;
		else if (bucketOrder >= (int) NUM_BUCKET_ORDERS)
			bucketOrder = NUM_BUCKET_ORDERS - 1;

		int bucketSize;		
		getCustomAttribute(bucketSize, "bucketSize", globals);

		bool bucketReverse;		
		getCustomAttribute(bucketReverse, "bucketReverse", globals);

		if(bucketReverse)
			file << "\n\nbucket " << bucketSize << " \"reverse " << BUCKET_ORDERS[bucketOrder] << "\"" << std::endl;			
		else
			file << "\n\nbucket " << bucketSize << " " << BUCKET_ORDERS[bucketOrder] << std::endl;

		paramPlug = globals.findPlug( "skyNode", &status );
		MPlugArray connectedPlugs;
		if(paramPlug.connectedTo(connectedPlugs,true,false,&status) && connectedPlugs.length()){
			MFnDependencyNode skyNode(connectedPlugs[0].node());
			paramPlug = skyNode.findPlug( "sunLight", &status );
			if(paramPlug.connectedTo(connectedPlugs,true,false,&status) && connectedPlugs.length()){
				if(connectedPlugs[0].node().apiType() == MFn::kDirectionalLight){					
					std::cout << "SunLight: " << connectedPlugs[0].name().asChar() << endl;
					MFnDirectionalLight light(connectedPlugs[0].node());
					MFloatVector dir(light.lightDirection(0,MSpace::kWorld, &status));					
					float turbidity;
					getCustomAttribute(turbidity, "skyTurbidity", globals);
					float samples;
					getCustomAttribute(samples, "skySamples", globals);
					file << "light {" << std::endl;
					file << "\ttype sunsky" << std::endl;
					file << "\tup 0 1 0" << std::endl;
					file << "\teast 0 0 1" << std::endl;
					file << "\tsundir " << -dir.x << " " << -dir.y << " " << -dir.z << std::endl;
					file << "\tturbidity " << turbidity << std::endl;
					file << "\tsamples " << samples << std::endl;
					file << "}" << std::endl;
					file << std::endl;
				}
			}
		}

		getCustomAttribute(materialOverride, "materialOverride", globals);
		getCustomAttribute(ambOverrideDist, "ambOverrideDist", globals);

		globalsWritten=true;
		break;
	}

	if(!globalsWritten){
		file << "image {" << std::endl;
		file << "\tresolution 640 480" << std::endl;
		file << "\taa 0 1" << std::endl;
		file << "\tfilter box" << std::endl;
		file << "}" << std::endl;
		file << std::endl;
	}
    

    // default shader
    file << "shader {" << std::endl;
    file << "\tname default" << std::endl;
    file << "\ttype diffuse" << std::endl;
    file << "\tdiff { \"sRGB nonlinear\" 0.7 0.7 0.7 }" << std::endl;
    file << "}" << std::endl; 
    file << std::endl;


	shaderList.clear();
    for (MItDependencyNodes it(MFn::kShadingEngine); !it.isDone(&status); it.next()) {
        MObject obj = it.item();		
        MFnDependencyNode sNode;
        if (getShaderFromEngine(obj, sNode)) {
			if(findShaderInList(sNode.name())){				
				continue; // already encountered a shader with the same name, skip
			}          
			std::cout << "Found surface shader: " << sNode.name().asChar() << " : " << sNode.object().apiTypeStr() << std::endl;
			// output bump modifiers
			MString bumpTexture; float bumpDepth; MObject bumpObject;
			if(getBumpFromShader(sNode, bumpTexture, bumpDepth, bumpObject)){
				MFnDependencyNode bumpNode(bumpObject);
				file << "modifier {" << std::endl;
				file << "\tname " << bumpNode.name().asChar() << std::endl;
				file << "\ttype bump" << std::endl;
				file << "\ttexture " << bumpTexture.asChar() << std::endl;
				file << "\tscale " << bumpDepth << std::endl;
				file << "}" << std::endl;
				shaderList.append(sNode.object());
			}
			// output shaders
			switch (sNode.object().apiType()) {
				case MFn::kLambert: {
					MFloatVector diffuse;
					MString diffuseTexture;
					getCustomAttribute(diffuseTexture, diffuse, "color", sNode);
					MFnLambertShader shader(sNode.object());
					file << "shader {" << std::endl;
					file << "\tname " << sNode.name().asChar() << std::endl;
					file << "\ttype diffuse" << std::endl;
					float d = shader.diffuseCoeff();
					if(diffuseTexture.length())
						file << "\ttexture " << diffuseTexture.asChar() << std::endl;
					else
						file << "\tdiff { \"sRGB nonlinear\" " << diffuse.x*d  << " " << diffuse.y*d << " " << diffuse.z*d << " }" << std::endl;
					file << "}" << std::endl; 
					file << std::endl;					
					shaderList.append(sNode.object());
					} break;
				case MFn::kPhong: {
					MFnPhongShader shader(sNode.object());
					MColor trans = shader.transparency();
					if(trans.r+trans.g+trans.b){
						file << "shader {" << std::endl;
						file << "\tname " << sNode.name().asChar() << std::endl;
						file << "\ttype glass" << std::endl;
						file << "\teta 1.5" << std::endl;
						MColor col = shader.color();
						float diffuse = shader.diffuseCoeff();					
						file << "\tcolor { \"sRGB nonlinear\" " << (trans.r * diffuse) << " " << (trans.g * diffuse) << " " << (trans.b * diffuse) << " }" << std::endl;
						file << "}" << std::endl; 
						file << std::endl;
					}else{
						MFloatVector diffuse;
						MString diffuseTexture;
						getCustomAttribute(diffuseTexture, diffuse, "color", sNode);
						file << "shader {" << std::endl;
						file << "\tname " << sNode.name().asChar() << std::endl;
						file << "\ttype phong" << std::endl;
						MColor col = shader.color();
						float d = shader.diffuseCoeff();
						MColor spec = shader.specularColor();
						float cosinePower = shader.cosPower();
						float reflectivity = shader.reflectivity();
						if(diffuseTexture.length())
							file << "\ttexture " << diffuseTexture.asChar() << std::endl;
						else
							file << "\tdiff { \"sRGB nonlinear\" " << (diffuse.x * d) << " " << (diffuse.y * d) << " " << (diffuse.z * d) << " }" << std::endl;
						file << "\tspec { \"sRGB nonlinear\" " << (spec.r * reflectivity) << " " << (spec.g * reflectivity) << " " << (spec.b * reflectivity) << " } " << cosinePower << std::endl;
						file << "}" << std::endl; 
						file << std::endl;
					}					
					shaderList.append(sNode.object());
					} break;
				case MFn::kPluginDependNode: {
					if(sNode.typeName() == "sunflowShader"){

						int sunflowShader;
						getCustomAttribute(sunflowShader, "shader", sNode);

						MStringArray sunflowShaderType;
						sunflowShaderType.append( "diffuse" );
						sunflowShaderType.append( "phong" );
						sunflowShaderType.append( "amb-occ" );
						sunflowShaderType.append( "mirror" );
						sunflowShaderType.append( "glass" );
						sunflowShaderType.append( "shiny" );
						sunflowShaderType.append( "ward" );
						sunflowShaderType.append( "view-caustics" );
						sunflowShaderType.append( "view-irradiance" );
						sunflowShaderType.append( "view-global" );
						sunflowShaderType.append( "constant" );
						sunflowShaderType.append( "janino" );
						sunflowShaderType.append( "id" );
						sunflowShaderType.append( "uber" );
						switch (sunflowShader){
							case 0: { //DIFFUSE
									MFloatVector diffuse;
									MString diffuseTexture;
									getCustomAttribute(diffuseTexture, diffuse, "color", sNode);
									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype diffuse" << std::endl;
									if(diffuseTexture.length())
										file << "\ttexture " << diffuseTexture.asChar() << std::endl;
									else
										file << "\tdiff { \"sRGB nonlinear\" " << diffuse.x  << " " << diffuse.y << " " << diffuse.z << " }" << std::endl;
									file << "}" << std::endl; 
									file << std::endl;
									} break;
							case 1: { //PHONG
									MFloatVector diffuse;
									MString diffuseTexture;
									getCustomAttribute(diffuseTexture, diffuse, "color", sNode);
									MFloatVector specular;
									getCustomAttribute(specular, "p_specular", sNode);
									float power;
									getCustomAttribute(power, "p_power", sNode);
									int samples;
									getCustomAttribute(samples, "p_samples", sNode);

									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype phong" << std::endl;
									if(diffuseTexture.length())
										file << "\ttexture " << diffuseTexture.asChar() << std::endl;
									else
										file << "\tdiff { \"sRGB nonlinear\" " << diffuse.x  << " " << diffuse.y << " " << diffuse.z << " }" << std::endl;									
									file << "\tspec { \"sRGB nonlinear\" " << specular.x  << " " << specular.y << " " << specular.z << " } " << power << std::endl;									
									file << "\tsamples " << samples << std::endl;
									file << "}" << std::endl; 
									file << std::endl;
									} break;
							case 2: { //AMB-OCC
									MFloatVector amb_bright;
									MString ambTexture;
									getCustomAttribute(ambTexture, amb_bright, "color", sNode);

									MFloatVector amb_dark;
									getCustomAttribute(amb_dark, "amb_dark", sNode);

									int amb_samples;
									getCustomAttribute(amb_samples, "amb_samples", sNode);

									float amb_maxdist;
									getCustomAttribute(amb_maxdist, "amb_maxdist", sNode);

									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype amb-occ" << std::endl;
									if(ambTexture.length())
										file << "\ttexture " << ambTexture.asChar() << std::endl;
									else
										file << "\tdiff { \"sRGB nonlinear\" " << amb_bright.x  << " " << amb_bright.y << " " << amb_bright.z << " }" << std::endl;
									file << "\tdark { \"sRGB nonlinear\" " << amb_dark.x  << " " << amb_dark.y << " " << amb_dark.z << " }" << std::endl;									
									file << "\tsamples " << amb_samples << std::endl;
									file << "\tdist " << amb_maxdist << std::endl;
									file << "}" << std::endl; 
									file << std::endl;
									} break;
							case 3: { //MIRROR
									MFloatVector refl;
									getCustomAttribute(refl, "color", sNode);
									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype mirror" << std::endl;
									file << "\trefl " << refl.x  << " " << refl.y << " " << refl.z << std::endl;									
									file << "}" << std::endl; 
									file << std::endl;									
									} break;
							case 4: { //GLASS
									float glass_eta;
									getCustomAttribute(glass_eta, "glass_eta", sNode);

									MFloatVector color;
									getCustomAttribute(color, "color", sNode);

									float glass_adist;
									getCustomAttribute(glass_adist, "glass_adist", sNode);

									MFloatVector glass_acol;
									getCustomAttribute(glass_acol, "glass_acol", sNode);

									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype glass" << std::endl;
									file << "\teta " << glass_eta << std::endl;
									file << "\tcolor { \"sRGB nonlinear\" " << color.x  << " " << color.y << " " << color.z << " }" << std::endl;
									file << "\tabsorbtion.distance " << glass_adist << std::endl;
									file << "\tabsorbtion.color { \"sRGB nonlinear\" " << glass_acol.x  << " " << glass_acol.y << " " << glass_acol.z << " }" << std::endl;									
									file << "}" << std::endl; 
									file << std::endl;
									} break;
							case 5: { //SHINY
									MFloatVector diffuse;
									MString diffuseTexture;
									getCustomAttribute(diffuseTexture, diffuse, "color", sNode);

									float shiny;
									getCustomAttribute(shiny, "shiny_shiny", sNode);
									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype shiny" << std::endl;
									if(diffuseTexture.length())
										file << "\ttexture " << diffuseTexture.asChar() << std::endl;
									else
										file << "\tdiff { \"sRGB nonlinear\" " << diffuse.x  << " " << diffuse.y << " " << diffuse.z << " }" << std::endl;
									file << "\trefl " << shiny << std::endl;									
									file << "}" << std::endl; 
									file << std::endl;									
									} break;
							case 6: { //WARD
									MFloatVector diffuse;
									MString diffuseTexture;
									getCustomAttribute(diffuseTexture, diffuse, "color", sNode);
									MFloatVector specular;
									getCustomAttribute(specular, "w_specular", sNode);
									float upower;
									getCustomAttribute(upower, "w_upower", sNode);
									float vpower;
									getCustomAttribute(vpower, "w_vpower", sNode);
									int samples;
									getCustomAttribute(samples, "w_samples", sNode);

									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype ward" << std::endl;
									if(diffuseTexture.length())
										file << "\ttexture " << diffuseTexture.asChar() << std::endl;
									else
										file << "\tdiff { \"sRGB nonlinear\" " << diffuse.x  << " " << diffuse.y << " " << diffuse.z << " }" << std::endl;
									file << "\tspec { \"sRGB nonlinear\" " << specular.x  << " " << specular.y << " " << specular.z << " } " << std::endl;
									file << "\trough " << upower << " " << vpower << std::endl;				
									file << "\tsamples " << samples << std::endl;
									file << "}" << std::endl; 
									file << std::endl;
									} break;
							case 7: { //VIEW-CAUSTICS									
									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype view-caustics" << std::endl;									
									file << "}" << std::endl; 
									file << std::endl;
									} break;
							case 8: { //VIEW-IRRADIANCE									
									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype view-irradiance" << std::endl;									
									file << "}" << std::endl; 
									file << std::endl;
									} break;
							case 9: { //VIEW-GLOBAL									
									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype view-global" << std::endl;									
									file << "}" << std::endl; 
									file << std::endl;
									} break;
							case 10: { //CONSTANT
									MFloatVector color;
									getCustomAttribute(color, "color", sNode);
									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype constant" << std::endl;
									file << "\tcolor { \"sRGB nonlinear\" " << color.x  << " " << color.y << " " << color.z << " }" << std::endl;
									file << "}" << std::endl; 
									file << std::endl;
									} break;
							case 11: { //JANINO get the code from Notes attribute
									MString code;
									getCustomAttribute(code, "notes", sNode);
									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype janino" << std::endl;
									file << "<code>" << std::endl;
									file << code.asChar() << std::endl;
									file << "</code>" << std::endl; 
									file << "}" << std::endl; 
									file << std::endl;
									} break;
							case 12: { //ID						
									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype id" << std::endl;									
									file << "}" << std::endl; 
									file << std::endl;
									} break;
							case 13: { //UBER
									MFloatVector diffuse;
									MString diffuseTexture;
									getCustomAttribute(diffuseTexture, diffuse, "color", sNode);
									MFloatVector diffuse2;
									MString diffuseTexture2;
									getCustomAttribute(diffuseTexture2, diffuse2, "u_diff_texture", sNode);

									float d_blend;
									getCustomAttribute(d_blend, "u_diff_blend", sNode);

									MFloatVector specular;
									MString specularTexture;
									getCustomAttribute(specularTexture, specular, "u_specular", sNode);

									MFloatVector specular2;
									MString specularTexture2;
									getCustomAttribute(specularTexture2, specular2, "u_spec_texture", sNode);

									float s_blend;
									getCustomAttribute(s_blend, "u_spec_blend", sNode);
									float gloss;
									getCustomAttribute(gloss, "u_gloss", sNode);
									int samples;
									getCustomAttribute(samples, "u_samples", sNode);

									file << "shader {" << std::endl;
									file << "\tname " << sNode.name().asChar() << std::endl;
									file << "\ttype uber" << std::endl;
									file << "\tdiff { \"sRGB nonlinear\" " << diffuse.x  << " " << diffuse.y << " " << diffuse.z << " }" << std::endl;
									if(diffuseTexture2.length()){
										file << "\tdiff.texture " << diffuseTexture2.asChar() << std::endl;
										file << "\tdiff.blend " << d_blend << std::endl;
									}
									file << "\tspec { \"sRGB nonlinear\" " << specular.x  << " " << specular.y << " " << specular.z << " }" << std::endl;
									if(specularTexture2.length()){
										file << "\tspec.texture " << specularTexture2.asChar() << std::endl;
										file << "\tspec.blend " << s_blend << std::endl;
									}										
									file << "\tglossy " << gloss << std::endl;
									file << "\tsamples " << samples << std::endl;
									file << "}" << std::endl; 
									file << std::endl;
									} break;
							default: break;
						}
						
						std::cout << "sunflowShaderNode: " << sunflowShaderType[sunflowShader].asChar() << std::endl;						
						shaderList.append(sNode.object());
						break;
					}	
					} 
				default: {
					std::cout << "Unsupported shader: " << sNode.name().asChar() << " : " << sNode.object().apiTypeStr() << " using lambert!" << std::endl;
					MFnLambertShader shader(sNode.object());
					file << "shader {" << std::endl;
					file << "\tname " << sNode.name().asChar() << std::endl;
					file << "\ttype diffuse" << std::endl;
					MColor col = shader.color();
					float d = shader.diffuseCoeff();
					file << "\tdiff { \"sRGB nonlinear\" " << (col.r * d) << " " << (col.g * d) << " " << (col.b * d) << " }" << std::endl;
					file << "}" << std::endl; 
					file << std::endl;					
					shaderList.append(sNode.object());
					}break;
			}

        }
    }
	bool renderCameraExported = false;
    for (MItDag mItDag = MItDag(MItDag::kBreadthFirst); !mItDag.isDone(&status); mItDag.next()) {
        MDagPath path;
        status = mItDag.getPath(path);
        switch (path.apiType(&status)) {
            case MFn::kMesh: {
                std::cout << "Exporting mesh: " << path.fullPathName().asChar() << " ..." << std::endl;
                exportMesh(path, file);
            } break;
			 case MFn::kNurbsSurface: {
                std::cout << "Exporting Surface: " << path.fullPathName().asChar() << " ..." << std::endl;
                exportSurface(path, file);
            } break;
            case MFn::kCamera: {
                std::cout << "Exporting camera: " << path.fullPathName().asChar() << " ..." << std::endl;
                exportCamera(path, file);
				if(!renderCameraExported){
					renderCamera = path;
					renderCameraExported = true;
				}

            } break;
            case MFn::kDirectionalLight: {
				if (!areObjectAndParentsVisible(path)) continue;               
				MFnDirectionalLight light(path);
				MPlug paramPlug = light.findPlug( "isSunSkyLight", &status );
				if ( status == MS::kSuccess ) continue;

				std::cout << "Exporting Directional Light: " << path.fullPathName().asChar() << " ..." << std::endl;
				MFloatVector dir(light.lightDirection(0,MSpace::kWorld, &status)*light.centerOfIllumination());				
				MMatrix o2w = path.inclusiveMatrix();
				MPoint source; source.x=o2w[3][0];source.y=o2w[3][1];source.z=o2w[3][2];
				MPoint target = source+dir;				
				MColor color = light.color();
				//float radius = light.shadowRadius();
				float intensity = light.intensity();

				file << "light {" << std::endl;
				file << "\ttype directional" << std::endl;
				file << "\tsource " << source.x << " " << source.y << " " << source.z << std::endl;
				file << "\ttarget " << target.x << " " << target.y << " " << target.z << std::endl;		
				file << "\tradius " << DIR_LIGHT_RADIUS << std::endl;	
				file << "\temit " << color.r*intensity << " " << color.b*intensity << " " << color.b*intensity << std::endl;	
				file << "}" << std::endl;
				file << std::endl;
            } break;
            case MFn::kAreaLight: {
                if (!areObjectAndParentsVisible(path)) continue;
                std::cout << "Exporting Area Light: " << path.fullPathName().asChar() << " ..." << std::endl;
                MMatrix o2w = path.inclusiveMatrix();
                MPoint lampV0(-1,  1,  0);
                MPoint lampV1( 1,  1,  0);
                MPoint lampV2( 1, -1,  0);
                MPoint lampV3(-1, -1,  0);
                lampV0 = lampV0 * o2w;
                lampV1 = lampV1 * o2w;
                lampV2 = lampV2 * o2w;
                lampV3 = lampV3 * o2w;

                MFnAreaLight area(path);
                MColor c = area.color();
                float i = area.intensity();
                file << "\n\nlight {" << std::endl;
                file << "\ttype meshlight" << std::endl;
                file << "\tname " << path.fullPathName().asChar() << std::endl;
                file << "\temit { \"sRGB nonlinear\" " << c.r << " " << c.g << " " << c.b << " }" << std::endl;
                file << "\tradiance " << i << std::endl;
                file << "\tsamples " << area.numShadowSamples() << std::endl;
                file << "\tpoints 4" << std::endl;
                file << "\t\t" << lampV0.x << " " << lampV0.y << " " << lampV0.z << std::endl;
                file << "\t\t" << lampV1.x << " " << lampV1.y << " " << lampV1.z << std::endl;
                file << "\t\t" << lampV2.x << " " << lampV2.y << " " << lampV2.z << std::endl;
                file << "\t\t" << lampV3.x << " " << lampV3.y << " " << lampV3.z << std::endl;
                file << "\ttriangles 2" << std::endl;
                file << "\t\t0 1 2" << std::endl;
                file << "\t\t0 2 3" << std::endl;
                file << "}" << std::endl;
                file << std::endl;
            } break;
            case MFn::kPointLight: {
                 if (!areObjectAndParentsVisible(path)) continue;
				 std::cout << "Exporting Point Light: " << path.fullPathName().asChar() << " ..." << std::endl;
                 MFnPointLight point(path);
                 MColor c = point.color();
                 float i = point.intensity();
                 file << "\n\nlight {" << std::endl;                
                 file << "\ttype point" << std::endl;                 
                 file << "\tcolor { \"sRGB nonlinear\" " << c.r << " " << c.g << " " << c.b << " }" << std::endl;
                 file << "\tpower " << i << std::endl;
				 MMatrix o2w = path.inclusiveMatrix();
                 file << "\tp " << o2w[3][0] << " " << o2w[3][1] << " " << o2w[3][2] << " " << std::endl;                
                 file << "}" << std::endl;
            } break;
			case MFn::kPluginLocatorNode: {
				MFnDependencyNode depNode(path.node(),&status);
				if(!status)
					break;
				if(depNode.typeName()=="sunflowHelperNode"){
					int helperType;
					getCustomAttribute(helperType, "type", depNode);					
					MPlug paramPlug;
					paramPlug = depNode.findPlug( "shaderNode", &status );
					MPlugArray connectedPlugs;
					MString materialName = "default";
					if(paramPlug.connectedTo(connectedPlugs,true,false,&status) && connectedPlugs.length()){
						MFnDependencyNode materialNode;
						if(connectedPlugs[0].node().apiType() == MFn::kShadingEngine)
							getShaderFromEngine(connectedPlugs[0].node(),materialNode);
						else
							materialNode.setObject(connectedPlugs[0].node());										
						if(findShaderInList(materialNode.name()))
							materialName = materialNode.name();
					}
					switch(helperType){
							case 0:{
								std::cout << "Exporting File Mesh: " << path.fullPathName().asChar() << " |Shader: " << materialName << std::endl;
								MMatrix o2w = path.inclusiveMatrix();
								MVector up(0,1,0);
								up *= o2w;
								MString fileName;
								getCustomAttribute(fileName, "meshPath", depNode);
								file << "\n\nobject {" << std::endl;
								file << "\tshader " << materialName.asChar() << std::endl;
								file << "\ttype file-mesh" << std::endl;
								file << "\tfilename " << fileName.asChar() << std::endl;
								file << "\tsmooth_normals true" << std::endl;
								file << "\t}" << std::endl;

								file << "\n\nobject {" << std::endl;											
								file << "\tshader " << materialName.asChar() << std::endl;
								file << "\ttype plane" << std::endl;
								file << "\tp " << o2w[3][0] << " " << o2w[3][0] << " " << o2w[3][0] << std::endl;
								file << "\tn " << up.x << " " << up.y << " " << up.z << std::endl;
								file << "}" << std::endl;														
								}break;
							case 1: {
								std::cout << "Exporting Infinite Plane: " << path.fullPathName().asChar() << " |Shader: " << materialName << std::endl;
								MMatrix o2w = path.inclusiveMatrix();
								MVector up(0,1,0);
								up *= o2w;
								file << "\n\nobject {" << std::endl;											
								file << "\tshader " << materialName.asChar() << std::endl;
								file << "\ttype plane" << std::endl;
								file << "\tp " << o2w[3][0] << " " << o2w[3][0] << " " << o2w[3][0] << std::endl;
								file << "\tn " << up.x << " " << up.y << " " << up.z << std::endl;
								file << "}" << std::endl;
								}break;
							default: break;
					}
				}
			}break;
            default: break;
        }
    }
    std::cout << "Exporting scene done." << std::endl;
    file.close();
	
    if (mode == "file") {
        std::cout << "Skipping sunflow launch - run as export only" << std::endl;
        return MS::kSuccess;
    }
    if (javaPath == 0 || sunflowPath == 0)
        return MS::kFailure;
    
    if (javaArgs == 0)
        javaArgs = "-Xmx1024M -server";

    std::cout << "Launching sunflow with JVM options: " << javaArgs << std::endl;
    
	MString cmd("\"");
    cmd += javaPath;
	cmd +="/java\"";
	MString args(" ");
    args += javaArgs; // user JVM arguments
    args += " -jar \"";
	args += sunflowPath;
	args += "/sunflow.jar\" ";
	switch (materialOverride){
		case 1: {
				args += "-quick_ambocc ";
				args += ambOverrideDist;
				args += " ";
			} break;
		case 2: {
				args += "-quick_uvs ";				
			} break;
		case 3: {
				args += "-quick_normals ";				
			} break;
		case 4: {
				args += "-quick_id ";				
			} break;		
		case 5: {
				args += "-quick_prims ";				
			} break;
		case 6: {
				args += "-quick_gray ";				
			} break;
		case 7: {
				args += "-quick_wire ";				
			} break;
		default: break;

	}
	if(mode == "IPR")
		args += "-ipr ";
	args += "-nogui -o imgpipe \"";
	args += exportPath.asChar();
	args += "\"";	
	std::cout << cmd.asChar() << args.asChar() << std::endl;


	//ShellExecute( NULL, NULL, ( LPCTSTR ) cmd.asChar(), ( LPCTSTR ) args.asChar(), ( LPCTSTR ) sunflowPath, SW_SHOWNORMAL );
	
	FILE *renderPipe;
#ifdef _WIN32
	MString cmdLine = "\"" + cmd+" "+args+"\"";
	if( (renderPipe = _popen( cmdLine.asChar(), "rb" )) == NULL )
		return MS::kFailure;
#else
	MString cmdLine = "" + cmd+" "+args;
	if( (renderPipe = popen( cmdLine.asChar(), "r" )) == NULL )
		return MS::kFailure;
#endif
	bucketToRenderView bucketObject;
	bucketObject.renderPipe = renderPipe;
	bucketObject.renderCamera = renderCamera;
	while( !feof( renderPipe ) )
	{
		bucketObject.checkStream();
	}
#ifdef _WIN32
    int returnCode = _pclose(renderPipe);
#else
	int returnCode = pclose(renderPipe);
#endif
	if (feof( renderPipe)){
		printf( "\nProcess returned %d\n", returnCode );
	}else{
		printf( "Error: Failed to read the pipe to the end.\n");
	}
	// Close pipe and print return value 
	std::cout << "\nProcess returned " << returnCode << std::endl;	

/*
	cmd = cmd + args + "&";
	system(cmd.asChar());

*/
    return MS::kSuccess;
}

//
//
///////////////////////////////////////////////////////////////////////////////
void * sunflowExportCmd::creator() { return new sunflowExportCmd(); }

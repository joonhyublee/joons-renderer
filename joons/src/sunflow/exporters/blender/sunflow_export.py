#!BPY

"""
Name: 'Sunflow Exporter 1.4.18 (.sc)...'
Blender: 2.45
Group: 'Export'
Tip: 'Export to a Sunflow Scene File'

Version         :       1.4.18 (January 2008)
Author          :       R Lindsay (hayfever) / Christopher Kulla / MADCello / 
			olivS / Eugene Reilly / Heavily Tessellated / Humfred
Description     :       Export to Sunflow renderer http://sunflow.sourceforge.net/
Usage           :       See how to use the script at http://sunflow.sourceforge.net/phpbb2/viewtopic.php?t=125

***** BEGIN GPL LICENSE BLOCK *****

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

***** END GPL LICENCE BLOCK *****
"""

####### General Script Properties Section #######
#################################################

## Imports ##
import Blender, os, sys, time #, subprocess

from Blender import Mathutils, Lamp, Object, Scene, Mesh, Material, Draw, BGL, Effect
from math import *

## Event numbers ##
CHANGE_EXP = 1
NO_EVENT   = 2
DOF_CAMERA = 3
SPHER_CAMERA = 4
FISH_CAMERA = 5
FILE_TYPE = 6
FILTER_EVENT = 7
CHANGE_AA = 8
CHANGE_SHAD = 9
CHANGE_CAM = 10
CHANGE_ACC = 11
CHANGE_LIGHT = 12
CHANGE_BCKGRD = 13
CHANGE_GI = 14
PHOTON_EVENT = 15
gPHOTON_EVENT = 16
FORCE_INSTANTGI = 17
FORCE_IRRCACHE = 18
FORCE_PATHTRACE = 19
FORCE_GLOBALS = 20
OVERRIDE_CAUSTICS = 21
OVERRIDE_GLOBALS = 22
OVERRIDE_GI = 23
EVENT_CAUSTICS = 24
IBL_EVENT = 25
EXP_EVT = 26
REND_EVT = 27
CONV_LAMP = 28
QUICK_OPT = 29
QUICK_OCC = 30
SUN_EVENT = 31
BCKGRD_EVENT = 32
CBCKGRD_EVENT = 33
LOCK_EVENT = 34
SET_PATH = 35
CHANGE_CFG = 36
SET_JAVAPATH = 37
SHADER_TYPE = 38
SHAD_OK = 39
EXPORT_ID = 40
DEFAULT_ID = 41
IP_COLOR = 42
BUCKET_EVENT = 43
IMPORT_ID = 44
FORCE_OCCLUSION = 45
FORCE_FAKEAMB = 46

## global vars ##
global FILE, SCENE, IM_HEIGHT, IM_WIDTH, TEXTURES, OBJECTS, IBLLIGHT, LAYERS, SCREEN, SCENE
global DOFDIST
FILENAME = Blender.Get('filename').replace(".blend", ".sc")
SCENE     = Blender.Scene.GetCurrent()
SFPATH = ""
JAVAPATH = ""

## start of export ##
print "\n\n"
print "blend2sunflow v1.4.18"

## Default values of buttons ##
def default_values():
        global FILETYPE, MINAA, MAXAA, AASAMPLES, AAJITTER, DSAMPLES, IMGFILTER, IMGFILTERW, IMGFILTERH, BUCKETSIZE,\
        BUCKETTYPE, REVERSE, DOF, DOFRADIUS, LENSSIDES, LENSROTATION, SPHERICALCAMERA, FISHEYECAMERA, IMP_BCKGRD,\
        IMP_SUN, BCKGRD, BACKGROUND, SUN_TURB, SUN_SAMPLES, IBL, IBLLOCK, IBLSAMPLES, INFINITEPLANE, IPLANECOLOR,\
        MESHLIGHTPOWER, LAMPPOWER, CONVLAMP, MEM, THREADS, CAUSTICS, PHOTONNUMBER, PHOTONMAP, PHOTONESTIMATE,\
        PHOTONRADIUS, INSTANTGI, IGISAMPLES, IGISETS, IGIBIAS, IGIBIASSAMPLES, IRRCACHE, IRRSAMPLES, IRRTOLERANCE,\
        IRRSPACEMIN, IRRSPACEMAX, USEGLOBALS, gPHOTONNUMBER, gPHOTONMAP, gPHOTONESTIMATE, gPHOTONRADIUS, PATHTRACE,\
        PATHSAMPLES, VIEWCAUSTICS, VIEWGLOBALS, VIEWGI, OCCLUSION, OCCBRIGHT, OCCDARK, OCCSAMPLES, OCCDIST,\
        SHADTYPE, QUICKOPT, IPR, EXP_ANIM, DEPTH_DIFF, DEPTH_REFL, DEPTH_REFR, NOGI, NOCAUSTICS, QUICKOCC, QOCCDIST,\
        NOGUI, SMALLMESH, IMGFILTERLIST, BUCKETTYPELIST, PHOTONMAPLIST, gPHOTONMAPLIST, FAKEAMB, FAMBSKY, FAMBGROUND

        FILETYPE = Draw.Create(1)

        # AA panel values
        MINAA = Draw.Create(0)
        MAXAA = Draw.Create(2)
        AASAMPLES = Draw.Create(1)
        AAJITTER = Draw.Create(0)
        DSAMPLES = Draw.Create(16)
        IMGFILTER = Draw.Create(1)
        IMGFILTERW = Draw.Create(1)
        IMGFILTERH = Draw.Create(1)
        BUCKETSIZE = Draw.Create(64)
        BUCKETTYPE = Draw.Create(1)
        REVERSE = Draw.Create(0)

        # Camera panel values
        DOF = Draw.Create(0)
        DOFRADIUS = Draw.Create(1.00)
        LENSSIDES = Draw.Create(6)
        LENSROTATION = Draw.Create(0.0)
        SPHERICALCAMERA = Draw.Create(0)
        FISHEYECAMERA = Draw.Create(0)

        # Background panel values
        IMP_BCKGRD  = Draw.Create(1)
        IMP_SUN  = Draw.Create(0)
        BCKGRD = Draw.Create(0.0, 0.0, 0.0)
        BACKGROUND = Draw.Create(0)
        SUN_TURB = Draw.Create(6.0)
        SUN_SAMPLES = Draw.Create(128)
        IBL = Draw.Create(0)
        IBLLOCK = Draw.Create(0)
        IBLSAMPLES = Draw.Create(16)
        INFINITEPLANE = Draw.Create(0)
        IPLANECOLOR= Draw.Create(1.0, 1.0, 1.0)

        # Light panel values
        MESHLIGHTPOWER = Draw.Create(100)
        LAMPPOWER = Draw.Create(100)
        CONVLAMP    = Draw.Create(0)

        # Config panel values
        MEM = Draw.Create(1024)
        THREADS    = Draw.Create(0)

        # GI/Caustic panel values
        CAUSTICS = Draw.Create(0)
        PHOTONNUMBER = Draw.Create(1000000)
        PHOTONMAP = Draw.Create(1)
        PHOTONESTIMATE = Draw.Create(100)
        PHOTONRADIUS = Draw.Create(0.5)
        INSTANTGI = Draw.Create(0)
        IGISAMPLES = Draw.Create(64)
        IGISETS = Draw.Create(1)
        IGIBIAS = Draw.Create(0.01)
        IGIBIASSAMPLES = Draw.Create(0)
        IRRCACHE = Draw.Create(0)
        IRRSAMPLES = Draw.Create(512)
        IRRTOLERANCE = Draw.Create(0.01)
        IRRSPACEMIN = Draw.Create(0.05)
        IRRSPACEMAX = Draw.Create(5.0)
        USEGLOBALS = Draw.Create(0)
        gPHOTONNUMBER = Draw.Create(1000000)
        gPHOTONMAP = Draw.Create(1)
        gPHOTONESTIMATE = Draw.Create(100)
        gPHOTONRADIUS = Draw.Create(0.5)
        PATHTRACE = Draw.Create(0)
        PATHSAMPLES = Draw.Create(32)
        VIEWCAUSTICS = Draw.Create(0)
        VIEWGLOBALS = Draw.Create(0)
        VIEWGI = Draw.Create(0)
        OCCLUSION = Draw.Create(0)
        OCCBRIGHT = Draw.Create(1.0, 1.0, 1.0)
        OCCDARK = Draw.Create(0.0, 0.0, 0.0)
        OCCSAMPLES = Draw.Create(32)
        OCCDIST = Draw.Create(3.0)
        FAKEAMB = Draw.Create(0)
        FAMBSKY = Draw.Create(1.0, 0.95, 0.65)
        FAMBGROUND = Draw.Create(0.5, 0.8, 1.0) 

        # Shader panel values
        SHADTYPE = Draw.Create(1)

        # Render panel values
        QUICKOPT = Draw.Create(1)
        IPR = Draw.Create(0)
        EXP_ANIM = Draw.Create(0)
        DEPTH_DIFF = Draw.Create(1)
        DEPTH_REFL = Draw.Create(4)
        DEPTH_REFR = Draw.Create(4)
        NOGI    = Draw.Create(0)
        NOCAUSTICS    = Draw.Create(0)
        QUICKOCC    = Draw.Create(0)
        QOCCDIST    = Draw.Create(0.5)
        NOGUI    = Draw.Create(0)
        SMALLMESH    = Draw.Create(0)

        ## Lists ##
        IMGFILTERLIST = ["box", "gaussian", "mitchell", "triangle", "catmull-rom", "blackman-harris", "sinc", "lanczos"]
        BUCKETTYPELIST = ["hilbert", "spiral", "column", "row", "diagonal", "random"]
        FILETYPE
        PHOTONMAPLIST = ["kd"]
        gPHOTONMAPLIST = ["grid"]

####### Script Settings Section #######
#######################################

## Background and AA settings ##
# Send background and AA values to Blender as IDs #
def def_output():
	global IM_HEIGHT, IM_WIDTH
	# Define Blender's values
	world = Blender.World.GetCurrent() 
	horcol = world.getHor()
	horcol0, horcol1, horcol2 = horcol[0], horcol[1], horcol[2]
	IM_HEIGHT = SCENE.getRenderingContext().imageSizeY()
	IM_WIDTH  = SCENE.getRenderingContext().imageSizeX()
	
	# Writes Scene properties
	SCENE.properties['SceneProp'] = "true"
	SCENE.properties['ImageWidth'] = IM_WIDTH
	SCENE.properties['ImageHeight'] = IM_HEIGHT
	SCENE.properties['MinAA'] = MINAA.val
	SCENE.properties['MaxAA'] = MAXAA.val
	SCENE.properties['Samples'] = AASAMPLES.val
	SCENE.properties['Filter'] = IMGFILTER.val
	if AAJITTER.val == 1:
		SCENE.properties['Jitter'] = "true"
	else:
		SCENE.properties['Jitter'] = "false"
	SCENE.properties['DepthDiff'] = DEPTH_DIFF.val
	SCENE.properties['DepthRefl'] = DEPTH_REFL.val
	SCENE.properties['DepthRefr'] = DEPTH_REFR.val
	if IMP_BCKGRD.val == 1:
		SCENE.properties['Blender Background'] = "true"
		SCENE.properties['HorizonColR'] = horcol0
		SCENE.properties['HorizonColG'] = horcol1
		SCENE.properties['HorizonColB'] = horcol2
	else:
               	SCENE.properties['Blender Background'] = "false"
	if BACKGROUND.val == 1:
		SCENE.properties['Script Background'] = "true"
		SCENE.properties['HorizonCol'] = BCKGRD.val
	else:
                SCENE.properties['Script Background'] = "false"
	SCENE.properties['Bucket Size'] = BUCKETSIZE.val
	SCENE.properties['Bucket Type'] = BUCKETTYPE.val
	if REVERSE == 1:
		SCENE.properties['Reverse Bucket'] = "true"
	else:
		SCENE.properties['Reverse Bucket'] = "false"

# Import background and AA values from Blender IDs to script #
def import_output():
	global IM_HEIGHT, IM_WIDTH
	# Retrieve Blender's Scene ID values
	world = Blender.World.GetCurrent() 
	horcol = world.getHor()
	horcol0, horcol1, horcol2 = horcol[0], horcol[1], horcol[2]
	IM_HEIGHT = SCENE.getRenderingContext().imageSizeY()
	IM_WIDTH  = SCENE.getRenderingContext().imageSizeX()

	if SCENE.properties['SceneProp'] == "true":
		IM_WIDTH = SCENE.properties['ImageWidth']
		IM_HEIGHT = SCENE.properties['ImageHeight']
		MINAA.val = SCENE.properties['MinAA']
		MAXAA.val = SCENE.properties['MaxAA']
		AASAMPLES.val = SCENE.properties['Samples']
		IMGFILTER.val = SCENE.properties['Filter']
		DEPTH_DIFF.val = SCENE.properties['DepthDiff']
		DEPTH_REFL.val = SCENE.properties['DepthRefl']
		DEPTH_REFR.val = SCENE.properties['DepthRefr']
		Draw.Redraw()
                if SCENE.properties['Jitter'] == "true":
                        AAJITTER.val = 1
                        Draw.Redraw()
                else:
                        AAJITTER.val = 0
                        Draw.Redraw()
                if  SCENE.properties['Blender Background'] == "true":
                        IMP_BCKGRD.val = 1
                        horcol0 = SCENE.properties['HorizonColR']
                        horcol1 = SCENE.properties['HorizonColG']
                        horcol2 = SCENE.properties['HorizonColB']
                        BACKGROUND.val = 0
                        Draw.Redraw()
                else:
                        IMP_BCKGRD.val = 0
                        Draw.Redraw()                        
                if SCENE.properties['Script Background'] == "true":
                        BACKGROUND.val = 1
                        BCKGRD.val = SCENE.properties['HorizonCol'][0], SCENE.properties['HorizonCol'][1], SCENE.properties['HorizonCol'][2]
                        IMP_BCKGRD.val = 0
                        Draw.Redraw()
                else:
                        BACKGROUND.val = 0
                        Draw.Redraw()
                BUCKETSIZE.val = SCENE.properties['Bucket Size']
                BUCKETTYPE.val = SCENE.properties['Bucket Type']
                if SCENE.properties['Reverse Bucket'] == "true":
                        REVERSE.val = 1
                        Draw.Redraw()	
                else:
                        REVERSE.val = 0
                        Draw.Redraw()

# Export background and AA values #                        
def export_output():
                print "o exporting output details..."
                FILE.write("image {\n")
       	        FILE.write("\tresolution %d %d\n" % (IM_WIDTH, IM_HEIGHT))
                FILE.write("\taa %s %s\n" % (MINAA.val, MAXAA.val))
                FILE.write("\tsamples %s\n" % AASAMPLES.val)
                FILE.write("\tfilter %s\n" % IMGFILTERLIST[IMGFILTER.val-1])
                if AAJITTER == 1:
                	FILE.write("\tjitter true\n")
                FILE.write("}")
                FILE.write("\n")

                print "o exporting trace-depths options..."
                FILE.write("trace-depths {\n")
                FILE.write("\tdiff %s \n" % DEPTH_DIFF)
                FILE.write("\trefl %s \n" % DEPTH_REFL)
                FILE.write("\trefr %s\n" % DEPTH_REFR)
                FILE.write("}")
                FILE.write("\n")
                if IMP_BCKGRD.val == 1:
                	print "o exporting background..."
                        world = Blender.World.GetCurrent() 
                        horcol = world.getHor()
                        horcol0, horcol1, horcol2 = horcol[0], horcol[1], horcol[2]
                        FILE.write("background {\n")
                        FILE.write("\tcolor  { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" % (horcol0, horcol1, horcol2))
                        FILE.write("}")
                        FILE.write("\n")
                elif BACKGROUND.val == 1:
                	print "o creating background..."
                        FILE.write("background {\n")
                        FILE.write("\tcolor  { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" % BCKGRD.val)
                        FILE.write("}")
                        FILE.write("\n")
                if REVERSE.val == 0:
                        FILE.write("\nbucket %s %s\n" % (BUCKETSIZE.val, BUCKETTYPELIST[BUCKETTYPE.val-1]))
                elif REVERSE.val == 1:
                        FILE.write("\nbucket %s " % BUCKETSIZE.val)
                        FILE.write("\"reverse %s\"\n" % BUCKETTYPELIST[BUCKETTYPE.val-1])
                     
## Caustic and global illumination settings ##
# Send GI and caustic values to Blender as IDs #
def def_gi():
        # Writes GI properties
        if CAUSTICS.val == 1:
                SCENE.properties['Caustics'] = "true"
		SCENE.properties['Caustics Photon Number'] = PHOTONNUMBER.val
                SCENE.properties['Caustics Photon Map'] = PHOTONMAP.val
                SCENE.properties['Caustics Photon Estimate'] = PHOTONESTIMATE.val
        	SCENE.properties['Caustics Photon Radius'] = PHOTONRADIUS.val
	else:
		SCENE.properties['Caustics'] = "false"
	if INSTANTGI.val == 1:
                SCENE.properties['IGI'] = "true"
		SCENE.properties['IGI Samples'] = IGISAMPLES.val
		SCENE.properties['IGI Sets'] = IGISETS.val
		SCENE.properties['IGI Bias'] = IGIBIAS.val
		SCENE.properties['IGI Bias Samples'] = IGIBIASSAMPLES.val
	else:
		SCENE.properties['IGI'] = "false"
	if IRRCACHE.val == 1:
                SCENE.properties['IRR'] = "true"
		SCENE.properties['IRR Samples'] = IRRSAMPLES.val
		SCENE.properties['IRR Tolerance'] = IRRTOLERANCE.val
		SCENE.properties['IRR Space Min'] = IRRSPACEMIN.val
		SCENE.properties['IRR Space Max'] = IRRSPACEMAX.val
	else:
		SCENE.properties['IRR'] = "false"
	if USEGLOBALS.val == 1:
                SCENE.properties['Global Photon'] = "true"
		SCENE.properties['Global Photon Num'] = gPHOTONNUMBER.val
		SCENE.properties['Global Photon Map'] = gPHOTONMAP.val
		SCENE.properties['Global Photon Estimate'] = gPHOTONESTIMATE.val
		SCENE.properties['Global Photon Radius'] = gPHOTONRADIUS.val
	else:
		SCENE.properties['Global Photon'] = "false"
	if PATHTRACE.val == 1:
                SCENE.properties['Path Tracing'] = "true"
		SCENE.properties['Path Tracing Samples'] = PATHSAMPLES.val
	else:
		SCENE.properties['Path Tracing'] = "false"
	if OCCLUSION.val ==1:
                SCENE.properties['Global AO'] = "true"
                SCENE.properties['Global AO Bright'] = OCCBRIGHT.val
                SCENE.properties['Global AO Dark'] = OCCDARK.val
                SCENE.properties['Global AO Samples'] = OCCSAMPLES.val
                SCENE.properties['Global AO Distance'] = OCCDIST.val
	else:
               SCENE.properties['Global AO'] = "false"
        if FAKEAMB.val ==1:
                SCENE.properties['Fake Ambient Term'] = "true"
                SCENE.properties['Fake Ambient Term Sky'] = FAMBSKY.val
                SCENE.properties['Fake Ambient Term Ground'] = FAMBGROUND.val
	else:
               SCENE.properties['Fake Ambient Term'] = "false"
	if VIEWCAUSTICS.val == 1:
		SCENE.properties['View Caustics'] = "true"
	else:
		SCENE.properties['View Caustics'] = "false"
	if VIEWGLOBALS.val == 1:
		SCENE.properties['View Globals'] = "true"
	else:
		SCENE.properties['View Globals'] = "false"
	if VIEWGI.val == 1:
		SCENE.properties['View GI'] = "true"
	else:
		SCENE.properties['View GI'] = "false"

# Import GI values from Blender IDs to script #
def import_gi():
        # Retrieve Blender's GI/Caustic ID values
        if SCENE.properties['SceneProp'] == "true":
                if SCENE.properties['Caustics'] == "true":
                        CAUSTICS.val = 1
                        PHOTONNUMBER.val = SCENE.properties['Caustics Photon Number']
                        PHOTONMAP.val = SCENE.properties['Caustics Photon Map']
                        PHOTONESTIMATE.val = SCENE.properties['Caustics Photon Estimate']
                        PHOTONRADIUS.val = SCENE.properties['Caustics Photon Radius']
                        Draw.Redraw()
                else:
                        CAUSTICS.val = 0
                        Draw.Redraw()
                if SCENE.properties['IGI'] == "true":
                        INSTANTGI.val = 1                
                        IGISAMPLES.val = SCENE.properties['IGI Samples']
                        IGISETS.val = SCENE.properties['IGI Sets']
                        IGIBIAS.val = SCENE.properties['IGI Bias']
                        IGIBIASSAMPLES.val = SCENE.properties['IGI Bias Samples']
                        Draw.Redraw()
                else:
                        INSTANTGI.val = 0
                        Draw.Redraw()
                if SCENE.properties['IRR'] == "true":
                        IRRCACHE.val = 1
                        IRRSAMPLES.val = SCENE.properties['IRR Samples']
                        IRRTOLERANCE.val = SCENE.properties['IRR Tolerance']
                        IRRSPACEMIN.val = SCENE.properties['IRR Space Min']
                        IRRSPACEMAX.val = SCENE.properties['IRR Space Max']
                        Draw.Redraw()
                else:
                        IRRCACHE.val = 0
                        Draw.Redraw()
                if SCENE.properties['Global Photon'] == "true":
                        USEGLOBALS.val = 1
                	gPHOTONNUMBER.val = SCENE.properties['Global Photon Num']
                        gPHOTONMAP.val = SCENE.properties['Global Photon Map']
                        gPHOTONESTIMATE.val = SCENE.properties['Global Photon Estimate']
                        gPHOTONRADIUS.val = SCENE.properties['Global Photon Radius']
                        Draw.Redraw()
                else:
                        USEGLOBALS.val = 0
                        Draw.Redraw()
                if SCENE.properties['Path Tracing'] == "true":
                        PATHTRACE.val = 1
                	PATHSAMPLES.val = SCENE.properties['Path Tracing Samples']
                	Draw.Redraw()
                else:
                        PATHTRACE.val = 0
                        Draw.Redraw()
                if SCENE.properties['Global AO'] == "true":
                        OCCLUSION.val = 1
                        OCCBRIGHT.val = SCENE.properties['Global AO Bright'][0], SCENE.properties['Global AO Bright'][1], SCENE.properties['Global AO Bright'][2]
                        OCCDARK.val = SCENE.properties['Global AO Dark'][0], SCENE.properties['Global AO Dark'][1], SCENE.properties['Global AO Dark'][2]
                        OCCSAMPLES.val = SCENE.properties['Global AO Samples']
                        OCCDIST.val = SCENE.properties['Global AO Distance']
                        Draw.Redraw()
                else:
                        OCCLUSION.val = 0
                        Draw.Redraw()
                if SCENE.properties['Fake Ambient Term'] == "true":
                        FAKEAMB.val = 1
                        FAMBSKY.val = SCENE.properties['Fake Ambient Term Sky'][0], SCENE.properties['Fake Ambient Term Sky'][1], SCENE.properties['Fake Ambient Term Sky'][2]
                        FAMBGROUND.val = SCENE.properties['Fake Ambient Term Ground'][0], SCENE.properties['Fake Ambient Term Ground'][1], SCENE.properties['Fake Ambient Term Ground'][2]
                        Draw.Redraw()
                else:
                        FAKEAMB.val = 0
                        Draw.Redraw() 
                if SCENE.properties['View Caustics'] == "true":
                        VIEWCAUSTICS.val = 1
                        Draw.Redraw()
                else:
                        VIEWCAUSTICS.val = 0
                        Draw.Redraw()
        	if SCENE.properties['View Globals'] == "true":
                        VIEWGLOBALS.val = 1
                        Draw.Redraw()
                else:
                        VIEWGLOBALS.val = 0
                        Draw.Redraw()        
                if SCENE.properties['View GI'] == "true":
                        VIEWGI.val = 1
                        Draw.Redraw()
                else:
                        VIEWGI.val = 0
                        Draw.Redraw() 

# Export global illumination values #		
def export_gi():
             	#Caustic Settings 
                if CAUSTICS.val == 1:
                        print "o exporting caustic settings..."
                        FILE.write("\nphotons {\n")
        		FILE.write("\tcaustics %s" % PHOTONNUMBER.val)
        		FILE.write(" %s " % PHOTONMAPLIST[PHOTONMAP.val-1])
        		FILE.write("%s %s\n" % (PHOTONESTIMATE.val, PHOTONRADIUS.val))
        		FILE.write("}\n")
            	#Instant GI Settings
                if INSTANTGI.val == 1:
                        print "o exporting Instant GI settings..."
        		FILE.write("\ngi {\n")
        		FILE.write("\ttype igi\n")
        		FILE.write("\tsamples %s\n" % IGISAMPLES.val)
        		FILE.write("\tsets %s\n" % IGISETS.val)
        		FILE.write("\tb %s\n" % IGIBIAS.val)
        		FILE.write("\tbias-samples %s\n" % IGIBIASSAMPLES.val)
        		FILE.write("}\n")
        	#Irradiance Cache GI Settings
                if IRRCACHE.val == 1:
                        print "o exporting Irradiance Cache GI settings..."
        		FILE.write("\ngi {\n")
        		FILE.write("\ttype irr-cache\n")
        		FILE.write("\tsamples %s\n" % IRRSAMPLES.val)
        		FILE.write("\ttolerance %s\n" % IRRTOLERANCE.val)
        		FILE.write("\tspacing %s %s\n" % (IRRSPACEMIN.val, IRRSPACEMAX.val))
                        if USEGLOBALS.val == 0:
        			FILE.write("}\n")
                	#No Path Tracing on Secondary Bounces in Irradiance Cache Settings
                        else:
				FILE.write("\tglobal %s" % gPHOTONNUMBER.val)
                                FILE.write(" %s " % gPHOTONMAPLIST[gPHOTONMAP.val-1])
                                FILE.write("%s %s\n" % (gPHOTONESTIMATE.val, gPHOTONRADIUS.val))
                                FILE.write("}\n")
        	#Path Tracing GI Settings
        	if PATHTRACE.val == 1:
        		print "o exporting Path Tracing GI settings..."
        		FILE.write("\ngi {\n")
        		FILE.write("\ttype path\n")
        		FILE.write("\tsamples %s\n" % PATHSAMPLES.val)
        		FILE.write("}\n")
        	#Ambient Occlusion GI Settings
        	if OCCLUSION.val == 1:
                        print "o exporting Ambient Occlusion GI settings..."
                        FILE.write("\n\ngi {\n")
                        FILE.write("\ttype ambocc\n")
                        FILE.write("\tbright { \"sRGB nonlinear\" %s %s %s }\n" % OCCBRIGHT.val)
                        FILE.write("\tdark { \"sRGB nonlinear\" %s %s %s }\n" % OCCDARK.val)
                        FILE.write("\tsamples %s\n" % OCCSAMPLES.val)
                        FILE.write("\tmaxdist %s\n}" % OCCDIST.val)
                #Fake Ambient Term GI Settings
        	if FAKEAMB.val == 1:
                        print "o exporting Fake Ambient Term GI settings..."
                        FILE.write("\n\ngi {\n")
                        FILE.write("\ttype fake\n")
                        FILE.write("\tup 0 1 0\n")
                        FILE.write("\tsky { \"sRGB nonlinear\" %s %s %s }\n" % FAMBSKY.val)
                        FILE.write("\tground { \"sRGB nonlinear\" %s %s %s }\n" % FAMBGROUND.val)
                        FILE.write("\n}")
        	#View Overrides
                if VIEWCAUSTICS.val == 1:
        		print "o exporting caustic override..."
        		FILE.write("\nshader {\n")
        		FILE.write("\tname debug_caustics\n")
        		FILE.write("\ttype view-caustics\n")
        		FILE.write("}\n")
        		FILE.write("override debug_caustics false\n")
                if VIEWGLOBALS.val == 1:
        		print "o exporting globals override..."
        		FILE.write("\nshader {\n")
        		FILE.write("\tname debug_globals\n")
        		FILE.write("\ttype view-global\n")
        		FILE.write("}\n")
        		FILE.write("override debug_globals false\n")
        	if VIEWGI.val == 1:
        		print "o exporting irradiance override..."
        		FILE.write("\nshader {\n")
        		FILE.write("\tname debug_gi\n")
        		FILE.write("\ttype view-irradiance\n")
        		FILE.write("}\n")
        		FILE.write("override debug_gi false\n")

def export_shaders():
	print "o exporting shaders..."

        #Infinite Plane
        if INFINITEPLANE.val == 1:
                FILE.write("\n\nshader {\n\tname iplane\n\ttype diffuse\n")
                FILE.write("\tdiff %s %s %s \n}" % IPLANECOLOR.val)
        # Add infinite plane object #
                FILE.write("\n\nobject {\n")
                FILE.write("\tshader iplane\n")
                FILE.write("\ttype plane\n")
                FILE.write("\tp 0 0 0\n")
                FILE.write("\tn 0 0 1\n}\n")
                        
	# default shader
	FILE.write("\n\nshader {\n\tname def\n\ttype diffuse\n\tdiff 1 1 1\n}")
	materials = Blender.Material.get()

	for mat in materials:
		RGB = mat.getRGBCol()
		speccol = mat.getSpecCol()
		textures = mat.getTextures()
		flags = mat.getMode()

		if mat.users == 0: 
			continue 

		# UBER shader
		if mat.name.startswith("sfube"):
                        textu = textures[0]
                        textu2 = textures[2]                        
			print "  o exporting uber shader "+mat.name+"..."
			FILE.write("\n\nshader {\n")
			FILE.write("\tname \""+mat.name+".shader\"\n")	
			FILE.write("\ttype uber\n")
			
			# DIFF values
			FILE.write("\tdiff %.3f %.3f %.3f\n" % (RGB[0], RGB[1], RGB[2]))
			if textures[0] <> None and textures[0].tex.getType() == "Image" and textu.tex.getImage() != None:
                                colvalue = textu.colfac
				FILE.write("\tdiff.texture \"" + textu.tex.getImage().getFilename() + "\"\n")
				FILE.write("\tdiff.blend %s\n" % (colvalue * 1.0))
			else:

        			FILE.write("\tdiff.blend 0.0\n")
			
			# SPEC values
			FILE.write("\tspec %.3f %.3f %.3f\n" % (speccol[0], speccol[1], speccol[2]))
			if textures[2] <> None and textures[2].tex.getType() == "Image" and textu2.tex.getImage() != None:
                                cspvalue = textu2.varfac
				FILE.write("\tspec.texture \"" + textu2.tex.getImage().getFilename() + "\"\n")
        			FILE.write("\tspec.blend %s\n" % (cspvalue * 0.1))
			else:
                                FILE.write("\tspec.blend 0.0\n")
			FILE.write("\tglossy .1\n")
			FILE.write("\tsamples 4\n}")

		elif textures[0] <> None and textures[0].tex.getType() == "Image":
			textu = textures[0]
			# image texture without image !!??
			if textu.tex.getImage() == None:
				print ("You material named " +mat.name+ " have an image texture with no image!")
				print ("replacing with default shader!")
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype diffuse\n")
				FILE.write("\tdiff { \"sRGB nonlinear\" %s %s %s }\n}" % (1.0, 1.0, 1.0))

			# ambocc texture shader
			elif mat.name.startswith("sfamb"):	
				print "  o exporting ambient occlusion texture shader "+mat.name+"..."
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype amb-occ\n")
				FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n")
				FILE.write("\tdark %.3f %.3f %.3f\n" % (speccol[0], speccol[1], speccol[2]))
				FILE.write("\tsamples 32\n")
				FILE.write("\tdist 3.0\n}")
				
			# diffuse texture shader
			elif mat.name.startswith("sfdif"):
				print "  o exporting diffuse texture shader "+mat.name+"..."
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype diffuse\n")
				FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n}")
				
			# phong texture shader
			elif mat.name.startswith("sfpho"):
				print "  o exporting phong texture shader "+mat.name+"..."
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype phong\n")
				FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n")
				FILE.write("\tspec { \"sRGB nonlinear\" %.3f %.3f %.3f } %s\n" %(speccol[0], speccol[1], speccol[2], mat.hard))
				FILE.write("\tsamples 4\n}")
				
			# ward texture shader
			elif mat.name.startswith("sfwar"):
				print "  o exporting ward texture shader "+mat.name+"..."
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype ward\n")
				FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n")
				speccol = mat.specCol
				FILE.write("\tspec { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" %(speccol[0], speccol[1], speccol[2]))
				FILE.write("\trough .2 .01\n")
				FILE.write("\tsamples 4\n}")

			# shiny texture shader
			elif mat.name.startswith("sfshi"):
				print "  o exporting shiny texture shader "+mat.name+"..."
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype shiny\n")
				FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n")
				FILE.write("\trefl %.2f\n}" % mat.getRayMirr())
			
			# newcommers default diffuse texture shader
			else:
				print "  o exporting diffuse texture shader "+mat.name+"..."
				FILE.write("\n\nshader {\n")
				FILE.write("\tname \""+mat.name+".shader\"\n")
				FILE.write("\ttype diffuse\n")
				FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n}")

		else:
		
			FILE.write("\n\nshader {\n")
			FILE.write("\tname \""+mat.name+".shader\"\n")
			
			## diffuse shader
			if mat.name.startswith("sfdif"):
				print "  o exporting diffuse shader "+mat.name+"..."
				FILE.write("\ttype diffuse\n")
				FILE.write("\tdiff { \"sRGB nonlinear\" %.3f %.3f %.3f }\n}" % (RGB[0], RGB[1], RGB[2]))
			
			## shiny shader
			elif mat.name.startswith("sfshi"):
				print "  o exporting shiny shader "+mat.name+"..."
				FILE.write("\ttype shiny\n")
				FILE.write("\tdiff { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" % (RGB[0], RGB[1], RGB[2]))
				FILE.write("\trefl %.2f\n}" % mat.getRayMirr())
				
			## amb-occ shader
			elif mat.name.startswith("sfamb"):
				print "  o exporting ambient occlusion shader "+mat.name+"..."
				FILE.write("\ttype amb-occ\n")
				FILE.write("\tbright %.3f %.3f %.3f\n" % (RGB[0], RGB[1], RGB[2]))
				FILE.write("\tdark %.3f %.3f %.3f\n" % (speccol[0], speccol[1], speccol[2]))
				FILE.write("\tsamples 32\n")
				FILE.write("\tdist 3.0\n}")
				
			## phong shader
			elif mat.name.startswith("sfpho"):
				print "  o exporting phong shader "+ mat.name+"..."
				FILE.write("\ttype phong\n")
				FILE.write("\tdiff { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" % (RGB[0],RGB[1],RGB[2]))
				FILE.write("\tspec { \"sRGB nonlinear\" %.3f %.3f %.3f } %s\n" %(speccol[0], speccol[1], speccol[2], mat.hard))
				FILE.write("\tsamples 4\n}")
				
			## ward shader
			elif mat.name.startswith("sfwar"):
				print "  o exporting ward shader "+ mat.name+"..."
				FILE.write("\ttype ward\n")
				FILE.write("\tdiff { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" %(RGB[0],RGB[1],RGB[2]))
				speccol = mat.specCol
				FILE.write("\tspec { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" %(speccol[0], speccol[1], speccol[2]))
				FILE.write("\trough .2 .01\n")
				FILE.write("\tsamples 4\n}")

			## reflection (mirror) shader
			elif mat.name.startswith("sfmir") and flags & Material.Modes['RAYMIRROR']:
				print "  o exporting mirror shader "+mat.name+"..."
				FILE.write("\ttype mirror\n")
				FILE.write("\trefl { \"sRGB nonlinear\" %.3f %.3f %.3f }\n}" %(RGB[0],RGB[1],RGB[2]))
				
			## glass shader
			elif mat.name.startswith("sfgla") and flags & Material.Modes['RAYTRANSP']:
				print "  o exporting glass shader "+mat.name+"..."
				FILE.write("\ttype glass\n")
				FILE.write("\teta " + str(mat.getIOR()) + "\n")
				FILE.write("\tcolor { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" %(RGB[0],RGB[1],RGB[2]))
				FILE.write("\tabsorbtion.distance 5.0\n")
				FILE.write("\tabsorbtion.color { \"sRGB nonlinear\" %.3f %.3f %.3f }\n}" %(speccol[0], speccol[1], speccol[2]))
				
			## constant shader
			elif mat.name.startswith("sfcon"):
				print "  o exporting constant shader "+mat.name+"..."
				FILE.write("\ttype constant\n")
				FILE.write("\tcolor { \"sRGB nonlinear\" %.3f %.3f %.3f }\n}" % (RGB[0], RGB[1], RGB[2]))

			## newcommers default diffuse shader
			else:
				print "  o exporting default diffuse shader "+mat.name+"..."
				FILE.write("\ttype diffuse\n")
				FILE.write("\tdiff { \"sRGB nonlinear\" %.3f %.3f %.3f }\n}" % (RGB[0], RGB[1], RGB[2]))

## Export modifiers ##
def export_modifiers():
	print "o exporting modifiers..."
		
	materials = Blender.Material.get()
	modifs_list = []
	
	for mat in materials:
		
		textures = mat.getTextures()
		flags = mat.getMode()

		if textures[1] <> None and textures[1].tex.getType() == "Image":
			textu = textures[1]
			Scale_value = str(textu.norfac * textu.mtNor*-0.001)
			
			if textu.tex.name.startswith("bump"):
				if textu.tex.getName() not in modifs_list:
					modifs_list.append (str(textu.tex.getName()))
					print "  o exporting modifier "+str(textu.tex.getName())+"..."
					FILE.write("\n\nmodifier {\n\tname "+str(textu.tex.getName())+"\n\ttype bump\n")
					FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n")
					FILE.write("\tscale %s \n}\n" % Scale_value)
				
			elif textu.tex.name.startswith("normal"):
				if textu.tex.getName() not in modifs_list:
					modifs_list.append (str(textu.tex.getName()))
					print "  o exporting modifier "+str(textu.tex.getName())+"..."
					FILE.write("\n\nmodifier {\n\tname "+str(textu.tex.getName())+"\n\ttype normalmap\n")
					FILE.write("\ttexture \"" + textu.tex.getImage().getFilename() + "\"\n}")
			else:
				pass

                if textures[3] <> None and textures[3].tex.getType() <> "Image":
                        textu = textures[3]
                        Scale_value = str(textu.norfac)
                        if textu.tex.name.startswith("perlin"):
                                if textu.tex.getName() not in modifs_list:
                                        modifs_list.append (str(textu.tex.getName()))
                                        print "  o exporting modifier "+str(textu.tex.getName())+"..."
                                        FILE.write("\n\nmodifier {\n\tname "+str(textu.tex.getName())+"\n\ttype perlin\n")
                                        FILE.write("\tfunction 0\n")
                                        FILE.write("\tsize 1\n")
                                        FILE.write("\tscale %s\n}\n" % Scale_value)
                        
                        else:
                                pass

## Light settings ##
# Send light values to Blender as IDs #
def def_lights():
        #Write light ID properties
	SCENE.properties['LightProp'] = "true"
	SCENE.properties['Lamp Multiplier'] = LAMPPOWER.val
        SCENE.properties['Meshlight Multiplier'] = MESHLIGHTPOWER.val
        SCENE.properties['Light Samples'] = DSAMPLES.val
        SCENE.properties['IBL Samples'] = IBLSAMPLES.val
        if IBL.val == 1:
                SCENE.properties['Image Based Light'] = "true"
        else:
                SCENE.properties['Image Based Light'] = "false"
        if IBLLOCK.val == 1:
                SCENE.properties['IBL Importance Sampling'] = "true"
        else:
                SCENE.properties['IBL Importance Sampling'] = "false"
	if CONVLAMP.val == 1:
        	SCENE.properties['Convert Unsupported Lamps'] = "true"
	else:
		SCENE.properties['Convert Unsupported Lamps'] = "false"
        if IMP_SUN.val == 1:
        	SCENE.properties['Sun Lamp'] = "true"
	else:
		SCENE.properties['Sun Lamp'] = "false"
	SCENE.properties['Sun Turbidity'] = SUN_TURB.val
	SCENE.properties['Sun Samples'] = SUN_SAMPLES.val
	#Infinite plane ID added here since it's mostly used with sun/sky
	if INFINITEPLANE.val == 1:
        	SCENE.properties['Infinite Plane'] = "true"
	else:
		SCENE.properties['Infinite Plane'] = "false"
	SCENE.properties['Infinite Plane Color'] = IPLANECOLOR.val

# Import light values from Blender IDs to script #
def import_lights():
        # Retrieve Blender's light ID values including IBL/LOCK
	if SCENE.properties['LightProp'] == "true":
                LAMPPOWER.val = SCENE.properties['Lamp Multiplier']
                MESHLIGHTPOWER.val = SCENE.properties['Meshlight Multiplier']
                DSAMPLES.val = SCENE.properties['Light Samples']
                IBLSAMPLES.val = SCENE.properties['IBL Samples']
                if SCENE.properties['Convert Unsupported Lamps'] == "true":
                        CONVLAMP.val = 1
                        Draw.Redraw()
                else:
                        CONVLAMP.val = 0
                        Draw.Redraw()
                if SCENE.properties['Image Based Light'] == "true":
                        IBL.val = 1
                        Draw.Redraw()
                else:
                        IBL.val = 0
                        Draw.Redraw()
                if SCENE.properties['IBL Importance Sampling'] == "true":
                        IBLLOCK.val = 1
                        Draw.Redraw()
                else:
                        IBLLOCK.val = 0
                        Draw.Redraw()
                if SCENE.properties['Sun Lamp'] == "true":
                        IMP_SUN.val = 1
                        Draw.Redraw()
                else:
                        IMP_SUN.val = 0
                        Draw.Redraw()                
                SUN_TURB.val = SCENE.properties['Sun Turbidity']
                SUN_SAMPLES.val = SCENE.properties['Sun Samples']
		#Infinite plane ID added here since it's mostly used with sun/sky
		if SCENE.properties['Infinite Plane'] == "true":
                        INFINITEPLANE.val = 1
                        Draw.Redraw()
                else:
                        INFINITEPLANE.val = 0
                        Draw.Redraw()
		IPLANECOLOR.val = SCENE.properties['Infinite Plane Color'][0], SCENE.properties['Infinite Plane Color'][1], SCENE.properties['Infinite Plane Color'][2], 

# Export light values #
def export_lights(lmp):
	# only lamps type 0, 1 and 4 supported at the moment
	# lamp types are: 0 - Lamp, 1 - Sun, 2 - Spot, 3 - Hemi, 4 - Area
	# Spots are replaced by directional (cylinrical) lights: adjust dist as close as possible to the ground receiving the
	# cone of light if you want Radius as close as possible
	lamp = lmp.getData()

        red   = lamp.col[0]
        green = lamp.col[1]
        blue  = lamp.col[2]
        power = lamp.energy * LAMPPOWER.val
        objmatrix = lmp.matrix
        lampV = Mathutils.Vector([0, 0, 0, 1])
        lampV = lampV * objmatrix
        dist = lamp.getDist()
	print "o exporting lamps"

	if lamp.type == 0:
		print "  o exporting lamp "+lmp.name+"..."
		FILE.write("\n\nlight {\n")
		FILE.write("\ttype point\n")
		FILE.write("\tcolor { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" % (red, green, blue))
		FILE.write("\tpower %s\n" % (power))
		FILE.write("\tp %.6f %.6f %.6f\n" % (lampV[0], lampV[1], lampV[2]))
		FILE.write("}")
	elif lamp.type == 1:
		if IMP_SUN.val == 1:
			print "  o exporting sun-light "+lmp.name+"..."
			invmatrix = Mathutils.Matrix(lmp.getInverseMatrix())
			FILE.write("\nlight {\n")
			FILE.write("\ttype sunsky\n")
			FILE.write("\tup 0 0 1\n")
			FILE.write("\teast 0 1 0\n")
			FILE.write("\tsundir %f %f %f\n" % (invmatrix[0][2], invmatrix[1][2], invmatrix[2][2]))
			FILE.write("\tturbidity %s\n" % SUN_TURB.val)
			FILE.write("\tsamples %s\n" % SUN_SAMPLES.val)
			FILE.write("}")
		else:
			print "  o exporting lamp "+lmp.name+"..."
			# get the location of the lamp
			FILE.write("\n\nlight {\n")
			FILE.write("\ttype point\n")
			FILE.write("\tcolor { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" % (red, green, blue))
			FILE.write("\tpower %s\n" % (power))
			FILE.write("\tp %.6f %.6f %.6f\n" % (lampV[0], lampV[1], lampV[2]))
			FILE.write("}")

	elif lamp.type == 4:
		print "  o exporting area-light "+lmp.name+"..."
		xsize = lamp.areaSizeX * 0.5
		if lamp.areaSizeY:
			# If rectangular area:
			print "o exporting rectangular area-light "+lmp.name+"..."
			ysize = lamp.areaSizeY * 0.5
		else:
			# Else, square area:
			print "o exporting square area-light "+lmp.name+"..."
			ysize = xsize
		lampV0 = Mathutils.Vector([-xsize, ysize, 0, 1])
		lampV1 = Mathutils.Vector([ xsize, ysize, 0, 1])
		lampV2 = Mathutils.Vector([ xsize, -ysize, 0, 1])
		lampV3 = Mathutils.Vector([-xsize, -ysize, 0, 1])

		lampV0 = lampV0 * objmatrix
		lampV1 = lampV1 * objmatrix
		lampV2 = lampV2 * objmatrix
		lampV3 = lampV3 * objmatrix

		radiance = lamp.energy * MESHLIGHTPOWER.val

		FILE.write("\n\nlight {\n")
		FILE.write("\ttype meshlight\n")
		FILE.write("\tname \"%s\"\n" % (lmp.name))
		FILE.write("\temit { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" % (red, green, blue))
		FILE.write("\tradiance %s\n" % (radiance))
		FILE.write("\tsamples %s\n" % DSAMPLES.val)
		FILE.write("\tpoints 4\n")
		FILE.write("\t\t%.6f %.6f %.6f\n" % (lampV0[0], lampV0[1], lampV0[2]))
		FILE.write("\t\t%.6f %.6f %.6f\n" % (lampV1[0], lampV1[1], lampV1[2]))
		FILE.write("\t\t%.6f %.6f %.6f\n" % (lampV2[0], lampV2[1], lampV2[2]))
		FILE.write("\t\t%.6f %.6f %.6f\n" % (lampV3[0], lampV3[1], lampV3[2]))
		FILE.write("\ttriangles 2\n")
		FILE.write("\t\t0 1 2\n")
		FILE.write("\t\t0 2 3\n")
		FILE.write("}")
	elif lamp.type == 2:
		print "  o exporting spotlight "+lmp.name+"..."
		targetV = Mathutils.Vector([0, 0, -1, 1])
		targetV = targetV * objmatrix
		angle = lamp.getSpotSize()*pi/360
		radius = dist/cos(angle)*sin(angle)
		radiance = lamp.energy * LAMPPOWER.val

		FILE.write("\n\nlight {\n")
		FILE.write("\ttype directional\n")
		FILE.write("\tsource %.6f %.6f %.6f\n" % (lampV[0], lampV[1], lampV[2]))
		FILE.write("\ttarget %.6f %.6f %.6f\n" % (targetV[0], targetV[1], targetV[2]))
		FILE.write("\tradius %s\n" % (radius))
		FILE.write("\temit { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" % (red, green, blue))
		FILE.write("}")
	elif lamp.type == 3:
		print "  o exporting spherical light "+lmp.name+"..."
		FILE.write("\n\nlight {\n")
		FILE.write("\ttype spherical\n")
		FILE.write("\tcolor { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" % (red, green, blue))
                FILE.write("\tradiance %s\n" % (power))
                FILE.write("\tcenter %.6f %.6f %.6f\n" % (lampV[0], lampV[1], lampV[2]))
                FILE.write("\tradius %s\n" % (dist))
                FILE.write("\tsamples %s\n" % DSAMPLES.val)
		FILE.write("}")
	else:
		print "Unsupported type lamp detected"
		if CONVLAMP.val == 1:
			print "  o exporting lamp "+lmp.name+"..."
			# get the rgb component for the lamp
			FILE.write("\n\nlight {\n")
			FILE.write("\ttype point\n")
			FILE.write("\tcolor { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" % (red, green, blue))
			FILE.write("\tpower %s\n" % (power))
			FILE.write("\tp %.6f %.6f %.6f\n" % (lampV[0], lampV[1], lampV[2]))
			FILE.write("}")

## Export of Image Based Lights ##
def export_ibl():
	global IBLLIGHT

	try:
		ibllighttext = Blender.Texture.Get("ibllight")
		if ibllighttext <> "":
			if ibllighttext.users > 0:
				if ibllighttext <> None and ibllighttext.getType() == "Image":
					IBLLIGHT = ibllighttext.getImage().getFilename()
				else:
					IBLLIGHT = ""
			else:
				IBLLIGHT = ""
	except:
		IBLLIGHT = ""

	if IBL == 1:
		if IBLLIGHT <> "":
			print "o exporting ibllight..."
			print "  o using texture %s" % (IBLLIGHT)
			FILE.write("\n\nlight {\n")
			FILE.write("\ttype ibl\n")
			FILE.write("\timage \"%s\"\n" % (IBLLIGHT))
			FILE.write("\tcenter 1 0 0\n")
			FILE.write("\tup 0 0 1\n")
			if IBLLOCK == 1:
				FILE.write("\tlock false\n")	# lock false means "use importance sampling"
			else:
				FILE.write("\tlock true\n")
			FILE.write("\tsamples %s\n" % IBLSAMPLES.val)
			FILE.write("}")

## Camera settings ##
# Send camera values to Blender as IDs #
def def_camera():
	CAMERA=SCENE.objects.camera
	# Writes Scene properties
	CAMERA.properties['CamProp'] = "true"
	CAMERA.properties['DOF Radius'] = DOFRADIUS.val
        CAMERA.properties['Lens Sides'] = LENSSIDES.val
        CAMERA.properties['Lens Rotation'] = LENSROTATION.val
        if DOF.val == 1:
		CAMERA.properties['DOF'] = "true"
	else:
		CAMERA.properties['DOF'] = "false"
        if SPHERICALCAMERA.val == 1:
		CAMERA.properties['Spherical Camera'] = "true"
	else:
		CAMERA.properties['Spherical Camera'] = "false"
	if FISHEYECAMERA.val == 1:
		CAMERA.properties['Fisheye Camera'] = "true"
	else:
		CAMERA.properties['Fisheye Camera'] = "false"

# Import Background and AA values from Blender IDs to script #
def import_camera():
	CAMERA=SCENE.objects.camera
        if CAMERA.properties['CamProp'] == "true":	
		DOFRADIUS.val = CAMERA.properties['DOF Radius']
                LENSSIDES.val = CAMERA.properties['Lens Sides']
                LENSROTATION.val = CAMERA.properties['Lens Rotation']
                if CAMERA.properties['DOF'] == "true":
                        DOF.val = 1
                        Draw.Redraw()
                else:
                        DOF.val = 0
                        Draw.Redraw()
                if CAMERA.properties['Spherical Camera'] == "true":
                        SPHERICALCAMERA.val = 1
                        Draw.Redraw()
                else:
                        SPHERICALCAMERA.val = 0
                        Draw.Redraw() 
                if CAMERA.properties['Fisheye Camera'] == "true":
                        FISHEYECAMERA.val = 1
                        Draw.Redraw()
                else:
                        FISHEYECAMERA.val = 0
                        Draw.Redraw() 

# Export camera values #        
def export_camera(cam):
	# get the camera
	camera = cam.getData()
	print "o exporting Camera "+camera.getName()+"..."

	# get the object matrix so we can calculate to and up
	objmatrix = cam.matrix
	eyeV    = Mathutils.Vector([0, 0,  0, 1])
	targetV = Mathutils.Vector([0, 0, -1, 1])
	upV     = Mathutils.Vector([0, 1,  0, 0])

	eyeV    = eyeV * objmatrix
	targetV = targetV * objmatrix
	upV     = upV * objmatrix

	# get the fov value
	# image higher than wide?
        if IM_HEIGHT > IM_WIDTH:
                factor=float(IM_WIDTH) / float(IM_HEIGHT)
        # if the image is wider than high, or of equal height and width then:
        else:
                factor=1
        # fov = 2*(degrees(atan((factor*16.0) / camera.lens))) #Same as below, just uses the python "degrees" instead:
	fov = 360.0 * atan((factor*16.0) / camera.lens) / pi
	DOFDIST=camera.dofDist

	FILE.write("\n\ncamera {\n")

	if DOF.val == 1:
		camtype = "thinlens"
		FILE.write("\ttype   %s\n" % camtype)
		FILE.write("\teye    %.6f %.6f %.6f\n" % (eyeV[0], eyeV[1], eyeV[2]))
		FILE.write("\ttarget %.6f %.6f %.6f\n" % (targetV[0], targetV[1], targetV[2]))
		FILE.write("\tup     %.6f %.6f %.6f\n" % (upV[0], upV[1], upV[2]))
		FILE.write("\tfov    %s \n" % fov)
		FILE.write("\taspect %s \n" % (1.0 * IM_WIDTH / IM_HEIGHT))
		FILE.write("\tfdist %s \n" % DOFDIST)
		FILE.write("\tlensr %s \n" % DOFRADIUS)
		FILE.write("\tsides %s \n" % LENSSIDES)
		FILE.write("\trotation %s \n" % LENSROTATION)
	elif SPHERICALCAMERA.val == 1:
		camtype = "spherical"
		FILE.write("\ttype   %s\n" % camtype)
		FILE.write("\teye    %.6f %.6f %.6f\n" % (eyeV[0], eyeV[1], eyeV[2]))
		FILE.write("\ttarget %.6f %.6f %.6f\n" % (targetV[0], targetV[1], targetV[2]))
		FILE.write("\tup     %.6f %.6f %.6f\n" % (upV[0], upV[1], upV[2]))
	elif FISHEYECAMERA.val == 1:
		camtype = "fisheye"
		FILE.write("\ttype   %s\n" % camtype)
		FILE.write("\teye    %.6f %.6f %.6f\n" % (eyeV[0], eyeV[1], eyeV[2]))
		FILE.write("\ttarget %.6f %.6f %.6f\n" % (targetV[0], targetV[1], targetV[2]))
		FILE.write("\tup     %.6f %.6f %.6f\n" % (upV[0], upV[1], upV[2]))
	else:
		camtype = "pinhole"
		FILE.write("\ttype   %s\n" % camtype)
		FILE.write("\teye    %.6f %.6f %.6f\n" % (eyeV[0], eyeV[1], eyeV[2]))
		FILE.write("\ttarget %.6f %.6f %.6f\n" % (targetV[0], targetV[1], targetV[2]))
		FILE.write("\tup     %.6f %.6f %.6f\n" % (upV[0], upV[1], upV[2]))
		FILE.write("\tfov    %s \n" % fov)
		FILE.write("\taspect %s \n" % (1.0 * IM_WIDTH / IM_HEIGHT))
                #To be uncommented when 0.07.3 is released
		#FILE.write("\tshift %.2f %.2f\n" % (camera.shiftX, camera.shiftY))
	FILE.write("}")

## Export method for meshes ##
def export_geometry(obj):
	islight = obj.name.startswith("meshlight")
        isparticleob = obj.name.startswith("particleob")
	if islight:
		print "o exporting meshlight " + obj.name+"..."
        if isparticleob:
		print "o exporting particle object " + obj.name+"..."
	# get the mesh data
	if obj.getType() <> "Empty":
                mesh = Mesh.New(obj.name) # Note the "mesh.verts = None" at the end of this if.  Used to clear the new mesh from memory
                mesh.getFromObject(obj, 0, 1)
                mesh.transform(obj.mat, 1)
		verts = mesh.verts
		faces = mesh.faces
                numverts = verts.__len__()

                if numverts > 0:
                        if islight:
                                FILE.write("\n\nlight {\n")
                                FILE.write("\ttype meshlight\n")
                                FILE.write("\tname \"" + obj.name + "\"\n")
                                if len(mesh.materials) >= 1:
                                        matrl = mesh.materials[0]
                                        FILE.write("\temit { \"sRGB nonlinear\" %.3f %.3f %.3f }\n" % (matrl.R, matrl.G, matrl.B))
                                else:
                                        FILE.write("\temit 1 1 1\n")
                                FILE.write("\tradiance %s\n" % (MESHLIGHTPOWER.val))
                                FILE.write("\tsamples %s\n" % DSAMPLES.val)
                                FILE.write("\tpoints %d\n" % (numverts))
                                for vert in verts:
                                        FILE.write("\t\t%.6f %.6f %.6f\n" % (vert.co[0], vert.co[1], vert.co[2]))
                                numtris = 0
                                for face in faces:
                                        num = len(face.verts)
                                        if num == 4:
                                                numtris = numtris + 2
                                        elif num == 3:
                                                numtris = numtris + 1
                                FILE.write("\ttriangles %d\n" % (numtris))
                                allsmooth = True
                                allflat = True
                                for face in faces:
                                        num = len(face.verts)
                                        smooth = face.smooth <> 0
                                        allsmooth &= smooth
                                        allflat &= not smooth
                                        if num == 4:
                                                FILE.write("\t\t%d %d %d\n" % (face.verts[0].index, face.verts[1].index, face.verts[2].index))
                                                FILE.write("\t\t%d %d %d\n" % (face.verts[0].index, face.verts[2].index, face.verts[3].index))
                                        elif num == 3:
                                                FILE.write("\t\t%d %d %d\n" % (face.verts[0].index, face.verts[1].index, face.verts[2].index))
                                FILE.write("}")

                        else:
                                FILE.write("\n\nobject {\n")
                                if len(mesh.materials) == 1:
                                        FILE.write("\tshader \"" + mesh.materials[0].name + ".shader\"\n")
                                        
                                        ##Check for and write modfiers##
                                        for mat in mesh.materials:
                                                textures = mat.getTextures()
                                                textu = textures[1]
                                                textp = textures[3]
                                                if textu <> None and (textu.tex.name.startswith("bump") or textu.tex.name.startswith("normal")):
                                                        FILE.write("\tmodifier \"" + str(textu.tex.getName()) + "\"\n")
                                                elif textp <> None and (textp.tex.name.startswith("perlin")):
                                                        FILE.write("\tmodifier \"" + str(textp.tex.getName()) + "\"\n")
                                        
                                elif len(mesh.materials) > 1:
                                        FILE.write("\tshaders %d\n" % (len(mesh.materials)))
                                        
                                        for mat in mesh.materials:
                                                FILE.write("\t\t\"" + mat.name + ".shader\"\n")
                                                
                                        ##Check for and write modfiers##                                        
                                        FILE.write("\tmodifiers %d\n" % (len(mesh.materials)))
                                        for mat in mesh.materials:

                                                textures = mat.getTextures()
                                                textu = textures[1]
                                                textp = textures[3]
                                                if textu <> None and (textu.tex.name.startswith("bump") or textu.tex.name.startswith("normal")):
                                                        FILE.write("\t\t\"" + textu.tex.getName() + "\"\n")
                                                elif textp <> None and (textp.tex.name.startswith("perlin")):
                                                        FILE.write("\t\t\"" + textp.tex.getName() + "\"\n")
                                                else:
                                                        FILE.write("\t\t\"" + "None" + "\"\n")

                                else:
                                        FILE.write("\tshader def\n")
                                ##Particle object section.  Works with Sunflow 0.07.3 and up. ##
                                # Check if there are particles on the object #
                          	ob = Object.Get(obj.name)
                          	if len(ob.effects) <> 0:
                                        effect = ob.effects[0]
                                        particles = effect.getParticlesLoc()
                                        effects= Effect.Get()
                                # Check that particles are points only (not static and not vectors) #
                                        if not effect.getFlag() & Effect.Flags.STATIC or not effect.getStype():
                                                FILE.write("\ttype particles\n")
                                                FILE.write("\tname \"" + obj.name + "\"\n")
                                                FILE.write("\tpoints %d\n" % (len(particles)))        
                                                for pt in particles:
                                                        FILE.write("\t\t%.6f %.6f %.6f\n" % (pt[0], pt[1], pt[2]))
                                                FILE.write("\tradius 0.05\n}\n")
                                        elif type(particles)==list: # Is it a strand or pair?
                                                if len(particles)>1:
                                                        pointnum = ((len(particles)*effect.getLifetime())*3)
                                                        print "o exporting hair object " + obj.name+"..."
                                                        FILE.write("\ttype hair\n")
                                                        FILE.write("\tname \"" + obj.name + "\"\n")
                                                        FILE.write("\tsegments %d\n" % (effect.getLifetime()-1)) 
                                                        FILE.write("\twidth .01\n")
                                                        FILE.write("\tpoints %d" % pointnum)
                                                        for eff in effects:
                                                                for p in eff.getParticlesLoc():
                                                                        if type(p)==list: # Are we a strand or a pair, then add hair data.
                                                                                if len(p)>1:
                                                                                        destData = str()
                                                                                        for vect in p:
                                                                                                for elem in vect:
                                                                                                        destData+=" "+str(elem)
                                                                                        FILE.write("\t%s\n" % (destData))
                                                        FILE.write("}\n")                                                        
                                elif isparticleob:
                                        FILE.write("\ttype particles\n")
                                        FILE.write("\tname \"" + obj.name + "\"\n")
                                        FILE.write("\tpoints %d\n" % (numverts))        
                                        for vert in verts:
                                                FILE.write("\t\t%.6f %.6f %.6f\n" % (vert.co[0], vert.co[1], vert.co[2]))
                                        FILE.write("\tradius 0.05\n}\n")

                                else:
                                        print "o exporting mesh " + obj.name+"..."
                                        FILE.write("\ttype generic-mesh\n")
                                        FILE.write("\tname \"" + obj.name + "\"\n")
                                        FILE.write("\tpoints %d\n" % (numverts))
                                        for vert in verts:
                                                FILE.write("\t\t%.6f %.6f %.6f\n" % (vert.co[0], vert.co[1], vert.co[2]))
                                        numtris = 0
                                        for face in faces:
                                                num = len(face.verts)
                                                if num == 4:
                                                        numtris = numtris + 2
                                                elif num == 3:
                                                        numtris = numtris + 1
                                        FILE.write("\ttriangles %d\n" % (numtris))
                                        allsmooth = True
                                        allflat = True
                                        for face in faces:
                                                num = len(face.verts)
                                                smooth = face.smooth <> 0
                                                allsmooth &= smooth
                                                allflat &= not smooth
                                                if num == 4:
                                                        FILE.write("\t\t%d %d %d\n" % (face.verts[0].index, face.verts[1].index, face.verts[2].index))
                                                        FILE.write("\t\t%d %d %d\n" % (face.verts[0].index, face.verts[2].index, face.verts[3].index))
                                                elif num == 3:
                                                        FILE.write("\t\t%d %d %d\n" % (face.verts[0].index, face.verts[1].index, face.verts[2].index))

                                        ## what kind of normals do we have?
                                        if not islight:
                                                if allflat:
                                                        FILE.write("\tnormals none\n")
                                                elif allsmooth:
                                                        FILE.write("\tnormals vertex\n")
                                                        for vert in verts:
                                                                FILE.write("\t\t%.6f %.6f %.6f\n" % (vert.no[0], vert.no[1], vert.no[2]))
                                                else:
                                                        FILE.write("\tnormals facevarying\n")
                                                        for face in faces:
                                                                num = len(face.verts)
                                                                if face.smooth <> 0:
                                                                        if num == 4:
                                                                                index0 = face.verts[0].index
                                                                                index1 = face.verts[1].index
                                                                                index2 = face.verts[2].index
                                                                                index3 = face.verts[3].index
                                                                                FILE.write("\t\t%.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f\n" % (verts[index0].no[0], verts[index0].no[1], verts[index0].no[2],
                                                                                                                                                                                                                                        verts[index1].no[0], verts[index1].no[1], verts[index1].no[2],
                                                                                                                                                                                                                                        verts[index2].no[0], verts[index2].no[1], verts[index2].no[2]))
                                                                                FILE.write("\t\t%.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f\n" % (verts[index0].no[0], verts[index0].no[1], verts[index0].no[2],
                                                                                                                                                                                                                                        verts[index2].no[0], verts[index2].no[1], verts[index2].no[2],
                                                                                                                                                                                                                                        verts[index3].no[0], verts[index3].no[1], verts[index3].no[2]))
                                                                        elif num == 3:
                                                                                index0 = face.verts[0].index
                                                                                index1 = face.verts[1].index
                                                                                index2 = face.verts[2].index
                                                                                FILE.write("\t\t%.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f\n" % (verts[index0].no[0], verts[index0].no[1], verts[index0].no[2],
                                                                                                                                                                                                                                        verts[index1].no[0], verts[index1].no[1], verts[index1].no[2],
                                                                                                                                                                                                                                        verts[index2].no[0], verts[index2].no[1], verts[index2].no[2]))
                                                                else:
                                                                        fnx = face.no[0]
                                                                        fny = face.no[1]
                                                                        fnz = face.no[2]
                                                                        if num == 4:
                                                                                FILE.write("\t\t%.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f\n" % (fnx, fny, fnz, fnx, fny, fnz, fnx, fny, fnz))
                                                                                FILE.write("\t\t%.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f\n" % (fnx, fny, fnz, fnx, fny, fnz, fnx, fny, fnz))
                                                                        elif num == 3:
                                                                                FILE.write("\t\t%.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f\n" % (fnx, fny, fnz, fnx, fny, fnz, fnx, fny, fnz))
                                                # Check for UVs #
                                                if mesh.faceUV <> 0:
                                                        tx = 1
                                                        ty = 1
                                                        if len(mesh.materials) >= 1:
                                                                if len(mesh.materials[0].getTextures()) >= 1:
                                                                        if mesh.materials[0].getTextures()[0] <> None:
                                                                                tx = mesh.materials[0].getTextures()[0].tex.repeat[0]
                                                                                ty = mesh.materials[0].getTextures()[0].tex.repeat[1]
                                                        FILE.write("\tuvs facevarying\n")
                                                        for face in faces:
                                                                num = len(face.verts)
                                                                if num == 4:
                                                                        FILE.write("\t\t%.6f %.6f %.6f %.6f %.6f %.6f\n" % (tx * face.uv[0][0], ty * face.uv[0][1], tx * face.uv[1][0], ty * face.uv[1][1], tx * face.uv[2][0], ty * face.uv[2][1]))
                                                                        FILE.write("\t\t%.6f %.6f %.6f %.6f %.6f %.6f\n" % (tx * face.uv[0][0], ty * face.uv[0][1], tx * face.uv[2][0], ty * face.uv[2][1], tx * face.uv[3][0], ty * face.uv[3][1]))
                                                                elif num == 3:
                                                                        FILE.write("\t\t%.6f %.6f %.6f %.6f %.6f %.6f\n" % (tx * face.uv[0][0], ty * face.uv[0][1], tx * face.uv[1][0], ty * face.uv[1][1], tx * face.uv[2][0], ty * face.uv[2][1]))
                                                else:
                                                        FILE.write("\tuvs none\n")

                                                # Check for multiple materials on the mesh (face indices) #
                                                if len(mesh.materials) > 1:
                                                        FILE.write("\tface_shaders\n")
                                                        for face in faces:
                                                                num = len(face.verts)
                                                                if num == 4:
                                                                        FILE.write("\t\t%d\n" % (face.mat))
                                                                        FILE.write("\t\t%d\n" % (face.mat))
                                                                elif num == 3:
                                                                        FILE.write("\t\t%d\n" % (face.mat))
                                        FILE.write("}\n")
                # This clears the data from the new mesh created. #
                mesh.verts = None

        # Look for Instances/Dupligroups #
        if obj.getType() == "Empty":
                ob = Object.Get(obj.name)
                dupe_obs = ob.DupObjects
		dupmatrix = Mathutils.Matrix(ob.getMatrix())
                group = ob.DupGroup
                if group <> "None":
                        for dupe_ob, dup_matrix in dupe_obs:
                                dupobmesh = Mesh.Get(dupe_ob.name)
                                instancematrix = ob.getMatrix()
                                print "o exporting instances of " + dupe_ob.name+"..."
                                FILE.write("\n\ninstance {\n")
                                FILE.write("\tname %s_%s \n" % (obj.name, dupe_ob.name))
                                FILE.write("\tgeometry %s \n" % dupe_ob.name)
                                FILE.write("\ttransform col %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s \n" % (instancematrix[0][0], instancematrix[0][1], instancematrix[0][2], instancematrix[0][3], instancematrix[1][0], instancematrix[1][1], instancematrix[1][2], instancematrix[1][3], instancematrix[2][0], instancematrix[2][1], instancematrix[2][2], instancematrix[2][3], instancematrix[3][0], instancematrix[3][1], instancematrix[3][2], instancematrix[3][3]))
                                if len(dupobmesh.materials) == 1:
                                        FILE.write("\tshader \"" + dupobmesh.materials[0].name + ".shader\"\n")
                                        
                                        ##Check for and write modfiers##
                                        for mat in dupobmesh.materials:
                                                textures = mat.getTextures()
                                                textu = textures[1]
                                                textp = textures[3]
                                                if textu <> None and (textu.tex.name.startswith("bump") or textu.tex.name.startswith("normal")):
                                                        FILE.write("\tmodifier \"" + str(textu.tex.getName()) + "\"\n")
                                                elif textp <> None and (textp.tex.name.startswith("perlin")):
                                                        FILE.write("\tmodifier \"" + str(textp.tex.getName()) + "\"\n")
                                        
                                elif len(dupobmesh.materials) > 1:
                                        FILE.write("\tshaders %d\n" % (len(dupobmesh.materials)))
                                        
                                        for mat in dupobmesh.materials:
                                                FILE.write("\t\t\"" + mat.name + ".shader\"\n")
                                                
                                        ##Check for and write modfiers##                                        
                                        FILE.write("\tmodifiers %d\n" % (len(dupobmesh.materials)))
                                        for mat in dupobmesh.materials:

                                                textures = mat.getTextures()
                                                textu = textures[1]
                                                textp = textures[3]
                                                if textu <> None and (textu.tex.name.startswith("bump") or textu.tex.name.startswith("normal")):
                                                        FILE.write("\t\t\"" + textu.tex.getName() + "\"\n")
                                                elif textp <> None and (textp.tex.name.startswith("perlin")):
                                                        FILE.write("\t\t\"" + textp.tex.getName() + "\"\n")
                                                else:
                                                        FILE.write("\t\t\"" + "None" + "\"\n")

                                else:
                                        FILE.write("\tshader def\n")
                                FILE.write("}\n")

## Main export method ##
def exportfile(filename):
	global FILE, SCENE, IM_HEIGHT, IM_WIDTH, TEXTURES, OBJECTS, LAYERS, IBLLIGHT, EXP_ANIM, fname, destname
	global PATH

	SCENE     = Blender.Scene.GetCurrent()
	IM_HEIGHT = SCENE.getRenderingContext().imageSizeY() * (SCENE.getRenderingContext().getRenderWinSize() / 100.0) 
	IM_WIDTH = SCENE.getRenderingContext().imageSizeX() * (SCENE.getRenderingContext().getRenderWinSize() / 100.0) 
	TEXTURES  = Blender.Texture.Get()

	LAYERS    = SCENE.getLayers()
	STARTFRAME = SCENE.getRenderingContext().startFrame()
	ENDFRAME  = SCENE.getRenderingContext().endFrame()
	CTX       = SCENE.getRenderingContext()
	orig_frame = CTX.currentFrame()

	if not filename.endswith(".sc"):
		filename = filename + ".sc"
	fname = filename
	destname = fname.replace(".sc", "")

	ANIM = EXP_ANIM.val

	if ANIM == 1:
		base = os.path.splitext(os.path.basename(filename))[0]
		FILE = open(filename.replace(".sc", ".java"), "wb")
		FILE.write("""
public void build() {
	parse("%s" + ".settings.sc");
	parse("%s" + "." + getCurrentFrame() + ".sc");
}
""" % (base, base))
		
		FILE.close()
		FILE = open(filename.replace(".sc", ".settings.sc"), "wb")
		export_output()
		export_gi()
		FILE.close()
	else:
		STARTFRAME = ENDFRAME = orig_frame

	for cntr in range(STARTFRAME, ENDFRAME + 1):
		filename = fname

		CTX.currentFrame(cntr)
		SCENE.makeCurrent()
		OBJECTS = SCENE.objects

		if ANIM == 1:
			filename = filename.replace(".sc", ".%d.sc" % (CTX.currentFrame()))

		print "Exporting to: %s" % (filename)

		FILE = open(filename, 'wb')

		if ANIM == 0:
			export_output()
			export_gi()
		
		export_shaders()
		export_modifiers()
		export_ibl()
		export_camera(SCENE.objects.camera)
		for object in OBJECTS:
			if object.users > 1 and object.layers[0] in LAYERS:
				if object.getType() == 'Lamp':
					export_lights(object)
				if object.getType() == 'Mesh' or object.getType() == 'Surf':
					if object.name.startswith("meshlight"):
						export_geometry(object)
						
		if ANIM == 0:
			FILE.write("\n\ninclude \"%s\"\n" % (os.path.basename(filename).replace(".sc", ".geo.sc")))
			FILE.close()
			## open geo file
			filename = filename.replace(".sc", ".geo.sc")
			print "Exporting geometry to: %s" % (filename)
			FILE = open(filename, 'wb')

		for object in OBJECTS:
			if object.users > 1 and object.layers[0] in LAYERS:
				if object.getType() == 'Mesh' or object.getType() == 'Surf':
					if not object.name.startswith("meshlight"):
						export_geometry(object)

		for object in OBJECTS:
			if object.users > 1 and object.layers[0] in LAYERS:
                                if object.getType() == 'Empty':
                                        export_geometry(object)
		FILE.close()

	if ANIM == 1:
		CTX.currentFrame(orig_frame)
		SCENE.makeCurrent()

	print "Export finished."

if __name__ == '__main__':
	Blender.Window.FileSelector(exportfile, "Export .sc", Blender.sys.makename(ext=".sc"))

## Global event handler ##
def event(evt, val):
	if evt == Draw.ESCKEY:
		print "quitting exporter..."
		Draw.Exit()

## Writing of the SF config file ##
def setpath(SFPATH):
	datadir=Blender.Get("datadir")
	# create 'path2sf.cfg in the .blend/bpydata directory
	f = open(datadir + '/path2sf.cfg', 'w')
	f.write(str(SFPATH)+"\n")
	f.write(str(MEM)+"\n")
	f.write(str(THREADS)+"\n")
	f.close()

def setjavapath(JAVAPATH):
	datadir=Blender.Get("datadir")
	f = open(datadir + '/path2sf.cfg', 'a')
	f.write(str(JAVAPATH))
	f.close()

## Render to Sunflow from inside the script ##
# Send render values to Blender as IDs #
def def_render():
	# Writes render setting properties
	SCENE.properties['RenderProp'] = "true"
	SCENE.properties['File Type'] = FILETYPE.val
        SCENE.properties['Quick Option'] = QUICKOPT.val
        SCENE.properties['Quick AO Distance'] = QOCCDIST.val
	if NOGUI.val == 1:
		SCENE.properties['-nogui'] = "true"
	else:
		SCENE.properties['-nogui'] = "false"
	if SMALLMESH.val == 1:
		SCENE.properties['-smallmesh'] = "true"
	else:
		SCENE.properties['-smallmesh'] = "false"
	if NOGI.val == 1:
		SCENE.properties['-nogi'] = "true"
	else:
		SCENE.properties['-nogi'] = "false"
	if NOCAUSTICS.val == 1:
		SCENE.properties['-nocaustics'] = "true"
	else:
		SCENE.properties['-nocaustics'] = "false"
	if QUICKOCC.val == 1:
		SCENE.properties['-quick_ambocc'] = "true"
	else:
		SCENE.properties['-quick_ambocc'] = "false"
	if IPR.val == 1:
		SCENE.properties['-ipr'] = "true"
	else:
		SCENE.properties['-ipr'] = "false"
	if EXP_ANIM.val == 1:
                SCENE.properties['animation'] = "true"
        else:
                SCENE.properties['animation'] = "false"

# Import render values from Blender IDs to script #
def import_render():
        global EXP_ANIM
        if SCENE.properties['RenderProp'] == "true":	
                FILETYPE.val = SCENE.properties['File Type']
                QUICKOPT.val = SCENE.properties['Quick Option']
                QOCCDIST.val = SCENE.properties['Quick AO Distance']
                if SCENE.properties['-nogui'] == "true":
                        NOGUI.val = 1
                        Draw.Redraw()
                else:
                        NOGUI.val = 0
                        Draw.Redraw()
                if SCENE.properties['-smallmesh'] == "true":
                        SMALLMESH.val = 1
                        Draw.Redraw()
                else:
                        SMALLMESH.val = 0
                        Draw.Redraw() 
                if SCENE.properties['-nogi'] == "true":
                        NOGI.val = 1
                        Draw.Redraw()
                else:
                        NOGI.val = 0
                        Draw.Redraw()
                if SCENE.properties['-nocaustics'] == "true":
                        NOCAUSTICS.val = 1
                        Draw.Redraw()
                else:
                        NOCAUSTICS.val = 0
                        Draw.Redraw()
                if SCENE.properties['-quick_ambocc'] == "true":
                        QUICKOCC.val = 1
                        Draw.Redraw()
                else:
                        QUICKOCC.val = 0
                        Draw.Redraw()
                if SCENE.properties['-ipr'] == "true":
                        IPR.val = 1
                        Draw.Redraw()
                else:
                        IPR.val = 0
                        Draw.Redraw()
                if SCENE.properties['animation'] == "true":
                        EXP_ANIM.val = 1
                        Draw.Redraw()
                else:
                        EXP_ANIM.val = 0
                        Draw.Redraw()

# Export render values #                        
def render():
	global COMMAND, memory, threads, sfpath, trial, javapath, fname, destname, STARTFRAME, ENDFRAME
	#exportfile(fname)
	destname = fname.replace(".sc", "")
	STARTFRAME = SCENE.getRenderingContext().startFrame()
	ENDFRAME  = SCENE.getRenderingContext().endFrame()
	# Get blenders 'bpydata' directory:
	datadir=Blender.Get("datadir")
	# Check existence of SF config file:
	trial=os.path.exists(datadir+'/path2sf.cfg')
	# Continue rendering if file exists:
	if trial == True:
		print "True"
		# open 'path2sf.cfg'
		f = open(datadir + '/path2sf.cfg', 'r+')
		lines = f.readlines()
		# Remove EOL:
		lines[0]=lines[0].rstrip("\n")
		lines[1]=lines[1].rstrip("\n")
		lines[2]=lines[2].rstrip("\n")
		lines[3]=lines[3].rstrip("\n")
		# Allocate values to variables:
		sfdir = lines[0]
		memory = lines[1]
		threads = lines[2]
		javadir = lines[3]
		f.close()

		# base command for executing SF:
		EXEC="%sjava -server -Xmx%sM -jar \"%ssunflow.jar\"" % (javadir, memory, sfdir)
		#print EXEC

		# Building the options to be passed to SF:
		if NOGUI == 1:
			option1="-nogui "
		else:
			option1=""
		option2="-threads %s " % THREADS
		if SMALLMESH == 1:
			option3="-smallmesh "
		else:
			option3=""
		if NOGI == 1:
			option4="-nogi "
		else:
			option4=""
		if NOCAUSTICS == 1:
			option5="-nocaustics "
		else:
			option5=""
		if QUICKOPT.val == 1:
			option6=""
		if QUICKOPT.val == 2:
			option6="-quick_uvs "
		elif QUICKOPT.val == 3:
			option6="-quick_normals "
		elif QUICKOPT.val == 4:
			option6="-quick_id "
		elif QUICKOPT.val == 5:
			option6="-quick_prims "
		elif QUICKOPT.val == 6:
			option6="-quick_gray "
		elif QUICKOPT.val == 7:
			option6="-quick_wire -aa 2 3 -filter mitchell "
		if QUICKOCC == 1:
			option7="-quick_ambocc %s " % (QOCCDIST)
		else:
			option7=""
		if IPR == 1:
			option8="-ipr "
		else:
			option8=""
		print option6

 		if FILETYPE == 1:
			ext="png"
		elif FILETYPE == 2:
			ext="tga"
		elif FILETYPE == 3:
			ext="hdr"
		elif FILETYPE == 4:
			ext="exr"
		elif FILETYPE == 5:
			ext="igi"	

		# Definition of the command to render the scene:
                if EXP_ANIM.val == 0:
                        COMMAND="%s %s%s%s%s%s%s%s%s -o \"%s.%s\" \"%s\"" % (EXEC, option1, option2, option3, option4, option5, option6, option7, option8, destname, ext, fname)
                        print COMMAND
                if EXP_ANIM.val == 1:
                        COMMAND="%s -anim %s %s -o \"%s.#.%s\" \"%s.java\"" % (EXEC, STARTFRAME, ENDFRAME, destname, ext, destname)
                        print COMMAND
		if FILETYPE != 1:
			COMMAND="%s -nogui %s%s%s%s%s%s%s -o \"%s.%s\" \"%s\"" % (EXEC, option2, option3, option4, option5, option6, option7, option8, destname, ext, fname)
                        print COMMAND
		# Execute the command:
		pid=os.system(COMMAND)
		#pid = subprocess.Popen(args=COMMAND, shell=True)
	
####### GUI button event handler #######
########################################
		
def buttonEvent(evt):
	global FILE, SCREEN, SETPATH, SETJAVAPATH, FILENAME, SCENE

	## Common events:
	if evt == EXP_EVT:
		Blender.Window.FileSelector(exportfile, "Export .sc", FILENAME)

	## Config file events
	if evt == SET_PATH:
		Blender.Window.FileSelector(setpath, "SET SF PATH", SFPATH)
		setpath(SFPATH)
	if evt == SET_JAVAPATH:
		Blender.Window.FileSelector(setjavapath, "SET JAVA PATH", JAVAPATH)
		setjavapath(JAVAPATH)
	if evt == REND_EVT:
		render()

	## Setting exclusive buttons rules:
	if evt == DOF_CAMERA:
		if DOF.val == 1:
			if SPHERICALCAMERA.val == 1:
				SPHERICALCAMERA.val = 0
				Draw.Redraw()
			elif FISHEYECAMERA.val == 1:
				FISHEYECAMERA.val = 0
				Draw.Redraw()
	if evt == SPHER_CAMERA:
		if SPHERICALCAMERA.val == 1:
			if DOF.val == 1:
				DOF.val = 0
				Draw.Redraw()
			elif FISHEYECAMERA.val == 1:
				FISHEYECAMERA.val = 0
				Draw.Redraw()
	if evt == FISH_CAMERA:
		if FISHEYECAMERA.val == 1:
			if DOF.val == 1:
				DOF.val = 0
				Draw.Redraw()
			elif SPHERICALCAMERA.val == 1:
				SPHERICALCAMERA.val = 0
				Draw.Redraw()
	if evt == FILTER_EVENT:
		Draw.Redraw()
	if evt == BUCKET_EVENT:
		Draw.Redraw()
	if evt == FORCE_INSTANTGI:
		if INSTANTGI.val == 1:
			if IRRCACHE.val == 1:
				IRRCACHE.val = 0
				Draw.Redraw()
			if USEGLOBALS.val == 1:
				USEGLOBALS.val = 0
				Draw.Redraw()
			if PATHTRACE.val == 1:
				PATHTRACE.val = 0
				Draw.Redraw()
			if OCCLUSION.val == 1:
				OCCLUSION.val = 0
				Draw.Redraw()
			if FAKEAMB.val == 1:
				FAKEAMB.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 0 and IRRCACHE.val == 0 and PATHTRACE.val == 0 and OCCLUSION.val == 0 and FAKEAMB.val == 0:
			if VIEWGI.val == 1:
				VIEWGI.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 1 or PATHTRACE.val == 1 or OCCLUSION.val == 1 or FAKEAMB.val == 1:
			if VIEWGLOBALS.val == 1:
				VIEWGLOBALS.val = 0
				Draw.Redraw()
	if evt == FORCE_IRRCACHE:
		if IRRCACHE.val == 1:
			if INSTANTGI.val == 1:
				INSTANTGI.val = 0
				Draw.Redraw()
			if PATHTRACE.val == 1:
				PATHTRACE.val = 0
				Draw.Redraw()
			if OCCLUSION.val == 1:
				OCCLUSION.val = 0
				Draw.Redraw()
			if FAKEAMB.val == 1:
				FAKEAMB.val = 0
				Draw.Redraw()
		if IRRCACHE.val == 0:
			if USEGLOBALS.val == 1:
				USEGLOBALS.val = 0
				Draw.Redraw()
	if INSTANTGI.val == 0 and IRRCACHE.val == 0 and PATHTRACE.val == 0 and OCCLUSION.val == 0 and FAKEAMB.val == 0:
			if VIEWGI.val == 1:
				VIEWGI.val = 0
				Draw.Redraw()
	if evt == FORCE_PATHTRACE:
		if PATHTRACE.val == 1:
			if IRRCACHE.val == 1:
				IRRCACHE.val = 0
				Draw.Redraw()
			if USEGLOBALS.val == 1:
				USEGLOBALS.val = 0
				Draw.Redraw()
			if INSTANTGI.val == 1:
				INSTANTGI.val = 0
				Draw.Redraw()
			if OCCLUSION.val == 1:
				OCCLUSION.val = 0
				Draw.Redraw()
			if FAKEAMB.val == 1:
				FAKEAMB.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 0 and IRRCACHE.val == 0 and PATHTRACE.val == 0 and OCCLUSION.val == 0 and FAKEAMB.val == 0:
			if VIEWGI.val == 1:
				VIEWGI.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 1 or PATHTRACE.val == 1 or OCCLUSION.val == 1 or FAKEAMB.val == 1:
			if VIEWGLOBALS.val == 1:
				VIEWGLOBALS.val = 0
				Draw.Redraw()
	if evt == FORCE_OCCLUSION:
		if OCCLUSION.val == 1:
			if IRRCACHE.val == 1:
				IRRCACHE.val = 0
				Draw.Redraw()
			if USEGLOBALS.val == 1:
				USEGLOBALS.val = 0
				Draw.Redraw()
			if INSTANTGI.val == 1:
				INSTANTGI.val = 0
				Draw.Redraw()
			if PATHTRACE.val == 1:
				PATHTRACE.val = 0
				Draw.Redraw()
			if FAKEAMB.val == 1:
				FAKEAMB.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 0 and IRRCACHE.val == 0 and PATHTRACE.val == 0 and OCCLUSION.val == 0 and FAKEAMB.val == 0:
			if VIEWGI.val == 1:
				VIEWGI.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 1 or PATHTRACE.val == 1 or OCCLUSION.val == 1 or FAKEAMB.val == 1:
			if VIEWGLOBALS.val == 1:
				VIEWGLOBALS.val = 0
				Draw.Redraw()
	if evt == FORCE_FAKEAMB:
		if FAKEAMB.val == 1:
			if IRRCACHE.val == 1:
				IRRCACHE.val = 0
				Draw.Redraw()
			if USEGLOBALS.val == 1:
				USEGLOBALS.val = 0
				Draw.Redraw()
			if INSTANTGI.val == 1:
				INSTANTGI.val = 0
				Draw.Redraw()
			if PATHTRACE.val == 1:
				PATHTRACE.val = 0
				Draw.Redraw()
			if OCCLUSION.val == 1:
				OCCLUSION.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 0 and IRRCACHE.val == 0 and PATHTRACE.val == 0 and OCCLUSION.val == 0 and FAKEAMB.val == 0:
			if VIEWGI.val == 1:
				VIEWGI.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 1 or PATHTRACE.val == 1 or OCCLUSION.val == 1 or FAKEAMB.val == 1:
			if VIEWGLOBALS.val == 1:
				VIEWGLOBALS.val = 0
				Draw.Redraw()
	if evt == FORCE_GLOBALS:
		if USEGLOBALS.val == 1:
			if PATHTRACE.val == 1:
				PATHTRACE.val = 0
				Draw.Redraw()
			if INSTANTGI.val == 1:
				INSTANTGI.val = 0
				Draw.Redraw()
		if IRRCACHE.val == 0:
			if USEGLOBALS.val == 1:
				USEGLOBALS.val = 0
				Draw.Redraw()
		if USEGLOBALS.val == 0:
			if VIEWGLOBALS.val == 1:
				VIEWGLOBALS.val = 0
				Draw.Redraw()
		if INSTANTGI.val == 0 and IRRCACHE.val == 0 and PATHTRACE.val == 0 and OCCLUSION.val == 0 and FAKEAMB.val == 0:
			if VIEWGI.val == 1:
				VIEWGI.val = 0
				Draw.Redraw()
	if evt == EVENT_CAUSTICS:
		if CAUSTICS.val == 0:
			if VIEWCAUSTICS.val == 1:
				VIEWCAUSTICS.val = 0
				Draw.Redraw()	
	if evt == OVERRIDE_CAUSTICS:
		if CAUSTICS.val == 0:
			if VIEWCAUSTICS.val == 1:
				VIEWCAUSTICS.val = 0
				Draw.Redraw()
	if evt == OVERRIDE_GLOBALS:
		if USEGLOBALS.val == 0:
			if VIEWGLOBALS.val == 1:
				VIEWGLOBALS.val = 0
				Draw.Redraw()
	if evt == OVERRIDE_GI:
		if INSTANTGI.val == 0 and IRRCACHE.val == 0 and PATHTRACE.val == 0 and OCCLUSION.val == 0 and FAKEAMB.val == 0:
			if VIEWGI.val == 1:
				VIEWGI.val = 0
				Draw.Redraw()
	if evt == IBL_EVENT:
			if IBL.val == 0:
				if IBLLOCK.val == 1:
					IBLLOCK.val = 0
					Draw.Redraw()
			if IMP_SUN.val == 1:
				IMP_SUN.val = 0
				Draw.Redraw()
			if IMP_BCKGRD.val == 1:
				IMP_BCKGRD.val = 0
				Draw.Redraw()
			if BACKGROUND.val == 1:
				BACKGROUND.val = 0
				Draw.Redraw()
	if evt == LOCK_EVENT:
			if IBL.val == 0:
				if IBLLOCK.val == 1:
					IBLLOCK.val = 0
					Draw.Redraw()
	if evt == SUN_EVENT:
			if IMP_BCKGRD.val == 1:
				IMP_BCKGRD.val = 0
				Draw.Redraw()
			if BACKGROUND.val == 1:
				BACKGROUND.val = 0
				Draw.Redraw()
			if IBL.val == 1:
				IBL.val = 0
				Draw.Redraw()
			if IBLLOCK.val == 1:
				IBLLOCK.val = 0
				Draw.Redraw()
	if evt == BCKGRD_EVENT:
			if IMP_SUN.val == 1:
				IMP_SUN.val = 0
				Draw.Redraw()
			if IBL.val == 1:
				IBL.val = 0
				Draw.Redraw()
			if BACKGROUND.val == 1:
				BACKGROUND.val = 0
				Draw.Redraw()
			if IBLLOCK.val == 1:
				IBLLOCK.val = 0
				Draw.Redraw()
	if evt == CBCKGRD_EVENT:
			if IMP_SUN.val == 1:
				IMP_SUN.val = 0
				Draw.Redraw()
			if IBL.val == 1:
				IBL.val = 0
				Draw.Redraw()
			if IMP_BCKGRD.val == 1:
				IMP_BCKGRD.val = 0
				Draw.Redraw()
			if IBLLOCK.val == 1:
				IBLLOCK.val = 0
				Draw.Redraw()
	if evt == QUICK_OPT:
		if QUICKOCC.val == 1:
			QUICKOCC.val = 0
			Draw.Redraw()
	if evt == QUICK_OCC:
		QUICKOPT.val = 1
		Draw.Redraw(0)              

	## Rules for displaying the different panels:
	if evt == CHANGE_AA:
		SCREEN=0
		Draw.Redraw()
		return
	if evt == CHANGE_CAM:
		SCREEN=2
		Draw.Redraw()
		return
	if evt == CHANGE_BCKGRD:
		SCREEN=3
		Draw.Redraw()
		return
	if evt == CHANGE_LIGHT:
		SCREEN=4
		Draw.Redraw()
		return
	if evt == CHANGE_GI:
		SCREEN=5
		Draw.Redraw()
		return
	if evt == CHANGE_SHAD:
		SCREEN=6
		Draw.Redraw()
		return
	if evt == CHANGE_EXP:
		SCREEN=7
		Draw.Redraw()
		return
	if evt == CHANGE_CFG:
		SCREEN=8
		Draw.Redraw()
		return
	# Go back to config panel if user tries to render without configuring SF:
	if evt == REND_EVT and trial == False:
		SCREEN=8
		Draw.Redraw()
		return
	if evt == SHAD_OK:
		Draw.Redraw()
		return
	# Events for the import and export of ID values and setting of default values
	if evt == EXPORT_ID:
                print "  o Sending script settings to .blend..."
		def_output()
		def_gi()
		def_lights()
		def_camera()
		def_render()
		print "  o Finished sending settings."
		return
	if evt == DEFAULT_ID:
                default_values()
                print "  o Default settings restored."
		return
        if evt == IMPORT_ID:
                try: 
                        if SCENE.properties['SceneProp'] == "true":
                                print "  o Script settings found in .blend, importing to script..."
                                import_output()
                                import_gi()
                                import_lights()
                                import_camera()
                                import_render()
                except:
                        print "  o No script settings in .blend, using defaults."
                        default_values()
                        return

# Same as the IMPORT_ID event, but needed it to be a function so we can load it on script start up #
def auto_import():
                default_values()
                try: 
                        if SCENE.properties['SceneProp'] == "true":
                                print "  o Script settings found in .blend, importing to script..."
                                import_output()
                                import_gi()
                                import_lights()
                                import_camera()
                                import_render()
                except:
                        print "  o No script settings in .blend, using defaults."
                        default_values()
                        return

####### Draw GUI Section #######
################################
        
## Draws the individual panels ##
def drawGUI():
	global SCREEN
	if SCREEN==0:
		drawAA() 
	if SCREEN==2:
		drawCamera()
	if SCREEN==3:
		drawBackground()
	if SCREEN==4:
		drawLights()
	if SCREEN==5:
		drawGI()
	if SCREEN==6:
		drawShad()
	if SCREEN==7:
		drawRender()
	if SCREEN==8:
		drawConfig()

## Draw AA options ##
def drawAA():
	global MINAA, MAXAA, AASAMPLES, AAJITTER, IMGFILTERW, IMGFILTERH, IMGFILTER
	col=10; line=150; BGL.glRasterPos2i(col, line); Draw.Text("AA:")
	col=100; line=145; MINAA=Draw.Number("Min AA", 2, col, line, 120, 18, MINAA.val, -4, 5); 
	col=230; MAXAA=Draw.Number("Max AA", 2, col, line, 120, 18, MAXAA.val, -4, 5)
	col=100; line=125; AASAMPLES=Draw.Number("Samples", 2, col, line, 120, 18, AASAMPLES.val, 0, 32)
	col=230; AAJITTER=Draw.Toggle("AA Jitter", 2, col, line, 120, 18, AAJITTER.val, "Use jitter for anti-aliasing")
	col=10; line=105; BGL.glRasterPos2i(col, line); Draw.Text("Image Filter:")
	col=100; line=100; IMGFILTER=Draw.Menu("%tImage Filter|box|gaussian|mitchell|triangle|catmull-rom|blackman-harris|sinc|lanczos", FILTER_EVENT, col, line, 120, 18, IMGFILTER.val)

	drawButtons()

## Draw camera options ##
def drawCamera(): 
	global DOF, DOFRADIUS, SPHERICALCAMERA, FISHEYECAMERA, LENSSIDES, LENSROTATION
	col=10; line=150; BGL.glRasterPos2i(col, line); Draw.Text("Camera:")
	col=100; line=145; DOF=Draw.Toggle("DOF", DOF_CAMERA, col, line, 120, 18, DOF.val, "Turn on depth of field")
	col=225; DOFRADIUS=Draw.Number("Radius", 2, col, line, 120, 18, DOFRADIUS.val, 0.001, 9.999)
	col=100; line=125; BGL.glRasterPos2i(col, line); Draw.Text("Bokeh shape -->")
	col=225; line=120; LENSSIDES=Draw.Number("Sides", 2, col, line, 120, 18, LENSSIDES.val, 2, 8)
	col=350; LENSROTATION=Draw.Number("Rotation", 2, col, line, 120, 18, LENSROTATION.val, 0.0, 360.0)
	col=100; line=95; SPHERICALCAMERA=Draw.Toggle("Spherical", SPHER_CAMERA, col, line, 120, 18, SPHERICALCAMERA.val, "Use the sperical camera type")
	col=100; line=70; FISHEYECAMERA=Draw.Toggle("Fisheye", FISH_CAMERA, col, line, 120, 18, FISHEYECAMERA.val, "Use the fisheye camera type")

	drawButtons()

## Draw Background options ##
def drawBackground():
	global IMP_BCKGRD, IMP_SUN, BACKGROUND, BCKGRD, IBL, IBLLOCK, IBLSAMPLES, SUN_TURB, SUN_SAMPLES, INFINITEPLANE, IPLANECOLOR
	col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("Simple background:")
	col=10; line=200; IMP_BCKGRD=Draw.Toggle("Import World", BCKGRD_EVENT, col, line, 120, 18, IMP_BCKGRD.val, "Set World's Horizon color as background")
	col=10; line=175; BACKGROUND=Draw.Toggle("Create Background", CBCKGRD_EVENT, col, line, 120, 18, BACKGROUND.val, "Define a background color")
	col=135; BCKGRD=Draw.ColorPicker(2, 135, 175, 30, 18, BCKGRD.val, "New background color")
	col=10; line=155; BGL.glRasterPos2i(col, line); Draw.Text("High Dynamic Range Illumination (Please set a texture named 'ibllight', lower case):")
	col=10; line=125; IBL=Draw.Toggle("Image Based Light", IBL_EVENT, col, line, 140, 18, IBL.val, "Use IBL/hdr type light")
	col=160; IBLLOCK=Draw.Toggle("Importance Sampling", LOCK_EVENT, col, line, 130, 18, IBLLOCK.val, "Use importance sampling (lock false), IBL must be on")
	col=300; IBLSAMPLES=Draw.Number("Samples", 2, col, line, 100, 18, IBLSAMPLES.val, 1, 1024)
	col=10; line=105; BGL.glRasterPos2i(col, line); Draw.Text("Sunsky:")
	col=10; line=75; IMP_SUN=Draw.Toggle("Import Sun", SUN_EVENT, col, line, 120, 18, IMP_SUN.val, "Import existing Sun")
	col=140; line=75; SUN_TURB=Draw.Number("Turbidity", 2, col, line, 120, 18, SUN_TURB.val, 0.0, 32.0)
	col=270; SUN_SAMPLES=Draw.Number("Samples", 2, col, line, 120, 18, SUN_SAMPLES.val, 1, 10000)
	col=400; INFINITEPLANE=Draw.Toggle("Infinite Plane", 2, col, line, 90, 18, INFINITEPLANE.val, "Use an infinite plane in the scene")
	col=500; IPLANECOLOR=Draw.ColorPicker(2, 495, 75, 30, 18, IPLANECOLOR.val, "Infinite plane color")

	drawButtons()

## Draw light options ##
def drawLights():
	global MESHLIGHTPOWER, LAMPPOWER, DSAMPLES, CONVLAMP
	col=10; line=205; BGL.glRasterPos2i(col, line); Draw.Text("Global Options:")
	col=10; line=180; BGL.glRasterPos2i(col, line); Draw.Text("Meshlight:")
	col=110; line=175; MESHLIGHTPOWER=Draw.Number("Power", 2, col, line, 200, 18, MESHLIGHTPOWER.val, 1, 10000)
	col=10; line=155; BGL.glRasterPos2i(col, line); Draw.Text("Light:")
	col=110; line=150; LAMPPOWER=Draw.Number("Power", 2, col, line, 200, 18, LAMPPOWER.val, 1, 10000)
	col=10; line=130; BGL.glRasterPos2i(col, line); Draw.Text("Lightserver:")
	col=110; line=125; DSAMPLES=Draw.Number("Direct Samples", 2, col, line, 200, 18, DSAMPLES.val, 0, 1024) 
	col=10; line=105; BGL.glRasterPos2i(col, line); Draw.Text("Unknown lamps:")
	col=10; line = 75; CONVLAMP=Draw.Toggle("Convert lamps", 2, col, line, 140, 18, CONVLAMP.val, "Convert unsupported lamps into point light")

	drawButtons()

## Draw Shader options ##
def drawShad():
	global SHADTYPE, SHADOK
	col=10; line=400; BGL.glRasterPos2i(col, line); Draw.Text("Specific instructions for exporting shaders:")
	col=10; line=375; BGL.glRasterPos2i(col, line); Draw.Text("For exporting bump and normal maps, have the second texture slot (slot 1) name")
	col=10; line=350; BGL.glRasterPos2i(col, line); Draw.Text("begin with bump or normal.  Controlled with the Map To Nor button and Nor slider.")	
	col=10; line=325; BGL.glRasterPos2i(col, line); Draw.Text("Regarding Textures: Diffuse, shiny, ambocc, phong, and ward materials will")
	col=10; line=300; BGL.glRasterPos2i(col, line); Draw.Text("use textures as the diffuse channel if the texture is in the first texture slot.")
	col=10; line=275; SHADTYPE=Draw.Menu("%tSelect shader|Uber|Diffuse|Shiny|AO|Phong|Ward|Mirror|Glass|Constant", SHADER_TYPE, col, line, 85, 18, SHADTYPE.val)
	col=100; SHADOK=Draw.Button("OK", SHAD_OK, col, line, 30, 18, "Print on screen instructions")
	if SHADTYPE == 1:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Uber: shader name should start with 'sfube' - imports Blender's Col and Spe RGB")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("values")
		col=10; line=200; BGL.glRasterPos2i(col, line); Draw.Text("\t\tIF Texture Slot 0: diffuse texture(Mapto Col value), else Col RGB values")
                col=10; line=175; BGL.glRasterPos2i(col, line); Draw.Text("\t\tThe blend factor for the diffuse texture is controlled by the Map Input Col slider")
		col=10; line=150; BGL.glRasterPos2i(col, line); Draw.Text("\t\tIF Texture Slot 2: specular texture(Mapto Var value), else Spe RGB values")
		col=10; line=125; BGL.glRasterPos2i(col, line); Draw.Text("\t\tThe blend factor for the specular texture is controlled by the Map Input Var slider")
	if SHADTYPE == 2:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Diffuse: shader name should start with 'sfdif' - imports Blender's Col RGB values")
	if SHADTYPE == 3:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Shiny: shader name sould start with 'sfshi' - imports Blender's Col RGB and")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("RayMirr values")
	if SHADTYPE == 4:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Ambient Occlusion: shader name sould start with 'sfamb' - imports Blender's")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("Col RGB (Bright) and Spe RGB (Dark) values")
	if SHADTYPE == 5:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Phong: shader name sould start with 'sfpho' - imports Blender's Col RGB and")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("Spe RGB values, as well as the spec's hard value")
	if SHADTYPE == 6:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Ward: shader name sould start with 'sfwar' - imports Blender's Col RGB and")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("Spe RGB values")
	if SHADTYPE == 7:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Mirror: shader name sould start with 'sfmir' - imports Blender's Col RGB values,")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("Ray Mir button must be on")
	if SHADTYPE == 8:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Glass: shader name sould start with 'sfgla' - imports Blender's Col RGB and")
		col=10; line=225; BGL.glRasterPos2i(col, line); Draw.Text("IOR values, Ray Transp button must be on")
	if SHADTYPE == 9:
		col=10; line=250; BGL.glRasterPos2i(col, line); Draw.Text("Constant: shader name should start with 'sfcon' - imports Blender's Col RGB values")

	drawButtons()

## Draw export and render options settings ##
def drawRender():
	global EXPORT, RENDER, SMALLMESH, NOGI, NOGUI, NOCAUSTICS, QUICKUV, QUICKNORM, QUICKID, QUICKPRIMS, QUICKGRAY, QUICKWIRE, QUICKOCC, QOCCDIST, FILETYPE, DEPTH_DIFF, DEPTH_REFL, DEPTH_REFR, QUICKOPT, EXP_ANIM, IPR, BUCKETSIZE, BUCKETTYPE, REVERSE
	col=10; line=325; BGL.glRasterPos2i(col, line); Draw.Text("Rendering actions:")
	col=10; line=300; EXPORT=Draw.Button("Export .sc", EXP_EVT, col, line, 140, 18, "Export the scene to .sc file")
	col=160; RENDER=Draw.Button("Render exported", REND_EVT, col, line, 130, 18, "Render it (Export a .sc file first)")
	col=300; FILETYPE=Draw.Menu("%tFile type|png|tga|hdr|exr|igi", FILE_TYPE, col, line, 85, 18, FILETYPE.val)
	col=10; line=280; BGL.glRasterPos2i(col, line); Draw.Text("Max raytrace depths:")
	col=10; line=255; DEPTH_DIFF=Draw.Number("Diffuse Depth", 2, col, line, 125, 18, DEPTH_DIFF.val, 1, 100)
	col=150; DEPTH_REFL=Draw.Number("Reflection Depth", 2, col, line, 125, 18, DEPTH_REFL.val, 1, 100)
	col=290; DEPTH_REFR=Draw.Number("Refraction Depth", 2, col, line, 125, 18, DEPTH_REFR.val, 1, 100)
        col=10; line=240; BGL.glRasterPos2i(col, line); Draw.Text("Bucket Order:")
	col=10; line=215; BUCKETSIZE=Draw.Number("Bucket Size", 2, col, line, 125, 18, BUCKETSIZE.val, 8, 64)
	col=150; BUCKETTYPE=Draw.Menu("%tBucket Order|hilbert (default)|spiral|column|row|diagonal|random", BUCKET_EVENT, col, line, 125, 18, BUCKETTYPE.val)
	col=290; REVERSE=Draw.Toggle("Reverse Order", 2, col, line, 125, 18, REVERSE.val, "Use reverse bucket order")
	col=10; line=195; BGL.glRasterPos2i(col, line); Draw.Text("Rendering options:")
	col=10; line=170; SMALLMESH=Draw.Toggle("Small mesh", 2, col, line, 85, 18, SMALLMESH.val, "Load triangle meshes using triangles optimized for memory use")
	col=100; NOGI=Draw.Toggle("No GI", 2, col, line, 85, 18, NOGI.val, "Disable any global illumination engines in the scene")
	col=190; NOCAUSTICS=Draw.Toggle("No Caustics", 2, col, line, 85, 18, NOCAUSTICS.val, "Disable any caustic engine in the scene")
	col=280; NOGUI=Draw.Toggle("No GUI", 2, col, line, 85, 18, NOGUI.val, "Don't open the frame showing rendering progress")
	col=370; IPR=Draw.Toggle("IPR", 2, col, line, 85, 18, IPR.val, "Render using progressive algorithm")
	col=10; line=150; BGL.glRasterPos2i(col, line); Draw.Text("Quick override options:")
	col=10; line=125; QUICKOPT=Draw.Menu("%tQuick option|None|Quick UVs|Quick Normals|Quick ID|Quick Primitives|Quick Gray|Quick Wire", QUICK_OPT, col, line, 100, 18, QUICKOPT.val)
	col=10; line=100; QUICKOCC=Draw.Toggle("Quick Amb Occ", QUICK_OCC, col, line, 85, 18, QUICKOCC.val, "Applies ambient occlusion to the scene with specified maximum distance")
	col=100; QOCCDIST=Draw.Number("Distance", 2, col, line, 125, 18, QOCCDIST.val, 0.00, 1000.00)
	col=10; line=75; EXP_ANIM=Draw.Toggle("Export As Animation", 2, col, line, 140, 18, EXP_ANIM.val, "Export the scene as animation")

	drawButtons()

## Draw the SF configuration settings ##
def drawConfig():
	global SET_PATH, THREADS, MEM, SET_JAVAPATH
        col=10; line = 315; BGL.glRasterPos2i(col, line); Draw.Text("ID properties for script settings:")
        col= 10; line = 285; Draw.Button("Send Script Settings", EXPORT_ID, col, line, 140,18, "Send script settings to the .blend")
        col= 155; Draw.Button("Restore Default Settings", DEFAULT_ID, col, line, 140,18, "Use script defaults")
        col= 300; Draw.Button("Reload ID Settings", IMPORT_ID, col, line, 140,18, "Reload ID properties from .blend")
        col=10; line = 255; BGL.glRasterPos2i(col, line); Draw.Text("Settings needed to render directly from the script:")
	col=155; line = 230; BGL.glRasterPos2i(col, line); Draw.Text("(threads=0 means auto-detect)")
	col=10; line = 225; THREADS=Draw.Number("Threads", 2, col, line, 140, 18, THREADS.val, 0, 8)
	col=10; line = 200; MEM=Draw.Number("Memory (MB)", 2, col, line, 140, 18, MEM.val, 256, 4096)
	col= 10; line = 175; Draw.Button("Save SF Path & Settings", SET_PATH, col, line, 140,18)
	col= 10; line = 150; Draw.Button("Save Java Path", SET_JAVAPATH, col, line, 140,18)
	col=155; line = 155; BGL.glRasterPos2i(col, line); Draw.Text("Set Java path after Sunflow path")
	# Get blenders 'bpydata' directory:
	datadir=Blender.Get("datadir")
	# Check if the config file exists:
	trial=os.path.exists(datadir+'/path2sf.cfg')
	# If config file exists, retrieve existing values:
	if trial == 1:
		# open 'path2sf.cfg'
		f = open(datadir + '/path2sf.cfg', 'r+')
		sfdir = f.readline()
		memory = f.readline()
		threads = f.readline()
		javadir = f.readline()
		col=10; line = 130; BGL.glRasterPos2i(col, line); Draw.Text("Sunflow path: %s" % sfdir)
		col=10; line = 110; BGL.glRasterPos2i(col, line); Draw.Text("Memory: %s MB" % memory)
		col=10; line = 90; BGL.glRasterPos2i(col, line); Draw.Text("Threads: %s" % threads)
		col=10; line = 70; BGL.glRasterPos2i(col, line); Draw.Text("Java path: %s" % javadir)
		f.close()
	# If config file doesn't exist, issue a warning:
	if trial == 0:
		col=10; line = 105; BGL.glRasterPos2i(col, line); Draw.Text("Sunflow is not configured yet - set Memory, Number of Threads and Path to sunflow.jar")

	drawButtons()

## Draw caustic and global illumination settings ##
def drawGI():
	global CAUSTICS, PHOTONNUMBER, PHOTONMAP, PHOTONESTIMATE, PHOTONRADIUS
	global INSTANTGI, IGISAMPLES, IGISETS, IGIBIAS, IGIBIASSAMPLES
	global IRRCACHE, IRRSAMPLES, IRRTOLERANCE, IRRSPACEMIN, IRRSPACEMAX
	global gPHOTONNUMBER, gPHOTONESTIMATE, gPHOTONRADIUS, gPHOTONMAP, USEGLOBALS
	global PATHTRACE, PATHSAMPLES
	global VIEWCAUSTICS, VIEWGLOBALS, VIEWGI
	global OCCLUSION, OCCBRIGHT, OCCDARK, OCCSAMPLES, OCCDIST
	global FAKEAMB, FAMBSKY, FAMBGROUND

        # GI/Caustics part #
	col=10; line=325; BGL.glRasterPos2i(col, line); Draw.Text("Caustics and Global Illumination")
        #Caustics#
	col=10; line=300; CAUSTICS=Draw.Toggle("Caustics", EVENT_CAUSTICS, col, line, 85, 18, CAUSTICS.val, "Turn on caustics in the scene")
	col=100; PHOTONNUMBER=Draw.Number("Photons", 2, col, line, 125, 18, PHOTONNUMBER.val, 0, 5000000)
	col=230; PHOTONMAP=Draw.Menu("%tCaustics Photon Map|kd", PHOTON_EVENT, col, line, 60, 18, PHOTONMAP.val)
	col=295; PHOTONESTIMATE=Draw.Number("Photon Estim.", 2, col, line, 125, 18, PHOTONESTIMATE.val, 0, 1000)
	col=425; PHOTONRADIUS=Draw.Number("Photon Radius", 2, col, line, 125, 18, PHOTONRADIUS.val, 0.00, 10.00)
        #Instant GI#
	col=10; line=275; INSTANTGI=Draw.Toggle("Instant GI", FORCE_INSTANTGI, col, line, 85, 18, INSTANTGI.val, "Enable Instant GI for GI in the scene")
	col=100; IGISAMPLES=Draw.Number("Samples", 2, col, line, 125, 18, IGISAMPLES.val, 0, 1024)
	col=230; IGISETS=Draw.Number("Number of Sets", 2, col, line, 125, 18, IGISETS.val, 1.0, 100.0)
	col=100; line=250; IGIBIAS=Draw.Number("Bias", 2, col, line, 125, 18, IGIBIAS.val, 0.000, 1.000)
	col=230; IGIBIASSAMPLES=Draw.Number("Bias Samples", 2, col, line, 125, 18, IGIBIASSAMPLES.val, 0, 64)
        #Irradiance Caching#
	col=10; line=225; IRRCACHE=Draw.Toggle("Irr. Cache", FORCE_IRRCACHE, col, line, 85, 18, IRRCACHE.val, "Enable Irradiance Caching for GI in the scene")
	col=100; IRRSAMPLES=Draw.Number("Samples", 2, col, line, 125, 18, IRRSAMPLES.val, 0, 1024)
	col=230; IRRTOLERANCE=Draw.Number("Tolerance", 2, col, line, 125, 18, IRRTOLERANCE.val, 0.0, 0.10)
	col=100; line=200; IRRSPACEMIN=Draw.Number("Min. Space", 2, col, line, 125, 18, IRRSPACEMIN.val, 0.00, 10.00)
	col=230; IRRSPACEMAX=Draw.Number("Max. Space", 2, col, line, 125, 18, IRRSPACEMAX.val, 0.00, 10.00)
	col=10; line=175; USEGLOBALS=Draw.Toggle("Use Globals", FORCE_GLOBALS, col, line, 85, 18, USEGLOBALS.val, "Use global photons instead of path tracing for Irr. Cache secondary bounces") 
	col=100; gPHOTONNUMBER=Draw.Number("Glob. Phot.", 2, col, line, 125, 18, gPHOTONNUMBER.val, 0, 5000000)
	col=230; gPHOTONMAP=Draw.Menu("%tGlobal Photon Map|grid", gPHOTON_EVENT, col, line, 60, 18, gPHOTONMAP.val)
	col=295; gPHOTONESTIMATE=Draw.Number("Global Estim.", 2, col, line, 125, 18, gPHOTONESTIMATE.val, 0, 1000)
	col=425; gPHOTONRADIUS=Draw.Number("Global Radius", 2, col, line, 125, 18, gPHOTONRADIUS.val, 0.00, 10.00)
        #Path Tracing#
	col=10; line=150; PATHTRACE=Draw.Toggle("Path Tracing", FORCE_PATHTRACE, col, line, 85, 18, PATHTRACE.val, "Enable Path Tracing for GI in the scene")
	col=100; PATHSAMPLES=Draw.Number("Samples", 2, col, line, 125, 18, PATHSAMPLES.val, 0, 1024)
        #AO#
	col=10; line=125; OCCLUSION=Draw.Toggle("Amb Occ", FORCE_OCCLUSION, col, line, 85, 18, OCCLUSION.val, "Turn on ambient occlusion for the whole scene")
	col=100; OCCBRIGHT=Draw.ColorPicker(2, 100, 125, 30, 18, OCCBRIGHT.val, "Ambient Occlusion bright color")
	col=135; OCCDARK=Draw.ColorPicker(2, 135, 125, 30, 18, OCCDARK.val, "Ambient Occlusion dark color")
	col=170; OCCSAMPLES=Draw.Number("Samples", 2, col, line, 125, 18, OCCSAMPLES.val, 0, 256)
	col=300; OCCDIST=Draw.Number("Distance", 2, col, line, 125, 18, OCCDIST.val, -1.0, 150.0)
	#Fake Ambient Term#
        col=10; line=100; FAKEAMB=Draw.Toggle("Fake Amb", FORCE_FAKEAMB, col, line, 85, 18, FAKEAMB.val, "Turn on Fake Ambient Term for the scene")
	col=100; FAMBSKY=Draw.ColorPicker(2, 100, 100, 30, 18, FAMBSKY.val, "Fake Ambient Term sky color")
	col=135; FAMBGROUND=Draw.ColorPicker(2, 135, 100, 30, 18, FAMBGROUND.val, "Fake Ambient Term ground color")
	#Overrides#
        col=100; line=75; VIEWCAUSTICS=Draw.Toggle("Just Caustics", OVERRIDE_CAUSTICS, col, line, 85, 18, VIEWCAUSTICS.val, "Render only the caustic photons in the scene (Caustics must be on)")
	col=190; VIEWGLOBALS=Draw.Toggle("Just Globals", OVERRIDE_GLOBALS, col, line, 85, 18, VIEWGLOBALS.val, "Render only the global photons in the scene (Use Globals must be on)")
	col=280; VIEWGI=Draw.Toggle("Just GI", OVERRIDE_GI, col, line, 85, 18, VIEWGI.val, "Render only the gi components in the scene (A GI engine must be selected)")
	drawButtons()

## Draw the bottom bar of buttons in the interface ##
def drawButtons():
	Draw.Button("Configure SF", CHANGE_CFG, 20, 10, 90, 50)
	Draw.Button("AA", CHANGE_AA, 115 , 40, 90, 20)
	Draw.Button("Camera", CHANGE_CAM, 210, 40, 90, 20)
	Draw.Button("Light", CHANGE_LIGHT, 305, 40, 90, 20)
	Draw.Button("Caustics/GI", CHANGE_GI, 115, 10, 90, 20)
	Draw.Button("Shader Info", CHANGE_SHAD, 210, 10, 90, 20)
	Draw.Button("Background", CHANGE_BCKGRD,305, 10, 90, 20)
	Draw.Button("Render settings", CHANGE_EXP, 400, 10, 90, 50)

####### Script Startup Section #######
######################################
	
SCREEN=7
auto_import()
Draw.Register(drawGUI, event, buttonEvent)


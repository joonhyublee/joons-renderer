; sunflowExporter installer

!include "MUI.nsh"
!include "AddToPath.nsh"

Name "SUNFLOW FOR MAYA"
OutFile "sunflowExporter.exe"

XPStyle on

AddBrandingImage left 100

LicenseText "License page"
InstallDir "$PROGRAMFILES\Sunflow\mayaExporter"

; Pages

!insertmacro MUI_PAGE_LICENSE "..\..\..\..\..\LICENSE"
!insertmacro MUI_PAGE_COMPONENTS
Page instfiles
UninstPage uninstConfirm
UninstPage instfiles

; Sections

Section "Maya 8.5"	M85Sec
    CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter"
	SetOutPath "$PROGRAMFILES\Sunflow\mayaExporter\mel"
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter\mel"
	File "..\..\mel\*.mel"
	SetOutPath "$PROGRAMFILES\Sunflow\mayaExporter\bin"
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter\bin"
	File "..\..\bin\sunflowExport_85_x32.mll"
	SetOutPath "$PROGRAMFILES\Sunflow\mayaExporter\icons"
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter\icons"
	File "..\..\icons\*.*"
  
	WriteUninstaller $PROGRAMFILES\Sunflow\mayaExporter\uninst.exe
SectionEnd

Section "Maya 8.0" M80Sec
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter"
	SetOutPath "$PROGRAMFILES\Sunflow\mayaExporter\mel"
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter\mel"
	File "..\..\mel\*.mel"
	SetOutPath "$PROGRAMFILES\Sunflow\mayaExporter\bin"
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter\bin"
	File "..\..\bin\sunflowExport_80_x32.mll"
	SetOutPath "$PROGRAMFILES\Sunflow\mayaExporter\icons"
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter\icons"
	File "..\..\icons\*.*"
SectionEnd

Section "Maya 7.0" M70Sec
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter"
	SetOutPath "$PROGRAMFILES\Sunflow\mayaExporter\mel"
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter\mel"
	File "..\..\mel\*.mel"
	SetOutPath "$PROGRAMFILES\Sunflow\mayaExporter\bin"
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter\bin"
	File "..\..\bin\sunflowExport_70_x32.mll"
	SetOutPath "$PROGRAMFILES\Sunflow\mayaExporter\icons"
	CreateDirectory "$PROGRAMFILES\Sunflow\mayaExporter\icons"
	File "..\..\icons\*.*"
SectionEnd

Section "Set Env Vars" setEnvSec
	Push "MAYA_PLUG_IN_PATH"
	Push "$PROGRAMFILES\Sunflow\mayaExporter\bin"
	Call AddToEnvVar

	Push "MAYA_SCRIPT_PATH"
	Push "$PROGRAMFILES\Sunflow\mayaExporter\mel"
	Call AddToEnvVar

	Push "XBMLANGPATH"
	Push "$PROGRAMFILES\Sunflow\mayaExporter\icons"
	Call AddToEnvVar
SectionEnd

Function .onInit
    
 
    ;==============================================================
    ; Mutually exclusive functions
    Push $0
 
    StrCpy $R9 ${M85Sec}
    SectionGetFlags ${M85Sec} $0
    IntOp $0 $0 | ${SF_SELECTED}
    SectionSetFlags ${M85Sec} $0
 
    SectionGetFlags ${M80Sec} $0
    IntOp $0 $0 & ${SECTION_OFF}
    SectionSetFlags ${M80Sec} $0
	
	 SectionGetFlags ${M70Sec} $0
    IntOp $0 $0 & ${SECTION_OFF}
    SectionSetFlags ${M70Sec} $0
 
    Pop $0
    ; END
 
FunctionEnd
;--------------------------------
;Descriptions

	;Language strings
	LangString DESC_M85Sec ${LANG_ENGLISH} "Install sunflow exporter for Maya 8.5."
	LangString DESC_M80Sec ${LANG_ENGLISH} "Install sunflow exporter for Maya 8.0."
	LangString DESC_M70Sec ${LANG_ENGLISH} "Install sunflow exporter for Maya 7.0."
	LangString DESC_setEnvSec ${LANG_ENGLISH} "Set environment variables."

	;Assign language strings to sections
	!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
	!insertmacro MUI_DESCRIPTION_TEXT ${M85Sec} $(DESC_M85Sec)
	!insertmacro MUI_DESCRIPTION_TEXT ${M80Sec} $(DESC_M80Sec)
	!insertmacro MUI_DESCRIPTION_TEXT ${M70Sec} $(DESC_M70Sec)
	!insertmacro MUI_DESCRIPTION_TEXT ${setEnvSec} $(DESC_setEnvSec)
	!insertmacro MUI_FUNCTION_DESCRIPTION_END
 
Section "uninstall"
	ClearErrors
	MessageBox MB_YESNO "Uninstall Sunflow Exporter?" IDNO end
	
	Push "MAYA_PLUGIN_PATH"
	Push "$PROGRAMFILES\Sunflow\mayaExporter\bin"
	Call un.RemoveFromEnvVar

	Push "MAYA_SCRIPT_PATH"
	Push "$PROGRAMFILES\Sunflow\mayaExporter\mel"
	Call un.RemoveFromEnvVar

	Push "XBMLANGPATH"
	Push "$PROGRAMFILES\Sunflow\mayaExporter\icons"
	Call un.RemoveFromEnvVar
	
	Delete $PROGRAMFILES\Sunflow\mayaExporter\uninst.exe
	RMDir /r "$PROGRAMFILES\Sunflow\mayaExporter"	
	end:
SectionEnd



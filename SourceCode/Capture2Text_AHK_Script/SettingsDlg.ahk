;-------------------------------------------------------------------------
;  Copyright (C) 2010-2016 Christopher Brochtrup
;
;  This file is part of Capture2Text.
;
;  Capture2Text is free software: you can redistribute it and/or modify
;  it under the terms of the GNU General Public License as published by
;  the Free Software Foundation, either version 3 of the License, or
;  (at your option) any later version.
;
;  Capture2Text is distributed in the hope that it will be useful,
;  but WITHOUT ANY WARRANTY; without even the implied warranty of
;  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;  GNU General Public License for more details.
;
;  You should have received a copy of the GNU General Public License
;  along with Capture2Text.  If not, see <http://www.gnu.org/licenses/>.
;
;-------------------------------------------------------------------------

#Include %A_ScriptDir%/Common.ahk
#Include %A_ScriptDir%/ColorDlg.ahk

; 0 = Dlg inactive, 1 = Dlg active, 2 = Dlg OK, 3 = Dlg Cancel
SETTING_DLG_INACTIVE = 0
SETTING_DLG_ACTIVE   = 1
SETTING_DLG_OK       = 2
SETTING_DLG_CANCEL   = 3
settingsDlgResult    := SETTING_DLG_INACTIVE

; Skip past contents of this file so that the subroutines are not automatically
; triggered when this file is #includes by another file
Goto, EndSettingsDlg


; Open the setting dialog
openSettingsDialog()
{
  global

  settingsDlgResult := SETTING_DLG_ACTIVE

  ; Create ocrDicTable and ocrCodeTable
  createLanguageTable()

  ; Create installedDics
  getInstalledDics()

  ; Read settings file
  readSettings()

  Gui, Settings:Destroy

  ;------------------------------------------------------------------------------
  ; Setup GUI

  ; Create tab control
  Gui, Settings:Add, Tab2, w450 h500, Hotkeys||OCR|Output

  ; -- Hotkeys tab ----------

  Gui, Settings:Add, GroupBox, x20 y35 w120 h80, Valid Modifiers
  Gui, Settings:Add, Text, xp+10 yp+15, #
  Gui, Settings:Add, Text, xp+30, (Windows Key)
  Gui, Settings:Add, Text, xp-30 yp+15, !
  Gui, Settings:Add, Text, xp+30, (Alt)
  Gui, Settings:Add, Text, xp-30 yp+15, ^
  Gui, Settings:Add, Text, xp+30, (Ctrl)
  Gui, Settings:Add, Text, xp-30 yp+15, +
  Gui, Settings:Add, Text, xp+30, (Shift)

  Gui, Settings:Add, GroupBox, x20 y120 w120 h350, Valid Keys
  Gui, Settings:Add, Text, xp+10 yp+15, a-z
  Gui, Settings:Add, Text, yp+15, 0-9
  Gui, Settings:Add, Text, yp+15, F1-F24
  Gui, Settings:Add, Text, yp+15, Space
  Gui, Settings:Add, Text, yp+15, Enter
  Gui, Settings:Add, Text, yp+15, Escape
  Gui, Settings:Add, Text, yp+15, Backspace
  Gui, Settings:Add, Text, yp+15, Delete
  Gui, Settings:Add, Text, yp+15, Insert
  Gui, Settings:Add, Text, yp+15, Home
  Gui, Settings:Add, Text, yp+15, End
  Gui, Settings:Add, Text, yp+15, PgUp
  Gui, Settings:Add, Text, yp+15, PgDn
  Gui, Settings:Add, Text, yp+15, Up
  Gui, Settings:Add, Text, yp+15, Down
  Gui, Settings:Add, Text, yp+15, Left
  Gui, Settings:Add, Text, yp+15, Right
  Gui, Settings:Add, Text, yp+15, LButton
  Gui, Settings:Add, Text, yp+15, RButton
  Gui, Settings:Add, Text, yp+15, MButton
  Gui, Settings:Font, underline
  Gui, Settings:Add, Text, yp+15 cBlue gLaunchKeyHelp, More keys...
  Gui, Settings:Font, norm

  Gui, Settings:Add, GroupBox, x150 y35 w300 h355, OCR Hotkeys
  Gui, Settings:Add, Edit, xp+10 yp+20 r1 w80 vOptStartAndEndCaptureKey, %StartAndEndCaptureKey%
  Gui, Settings:Add, Text, xp+90, Start or end capture
  Gui, Settings:Add, Edit, xp-90 yp+25 r1 w80 vOptEndOnlyCaptureKey, %EndOnlyCaptureKey%
  Gui, Settings:Add, Text, xp+90, End capture
  Gui, Settings:Add, Edit, xp-90 yp+25 r1 w80 vOptCancelCaptureKey, %CancelCaptureKey%
  Gui, Settings:Add, Text, xp+90, Cancel capture
  Gui, Settings:Add, Edit, xp-90 yp+25 r1 w80 vOptMoveCaptureKey, %MoveCaptureKey%
  Gui, Settings:Add, Text, xp+90, Drag capture box
  Gui, Settings:Add, Edit, xp-90 yp+25 r1 w80 vOptToggleActiveCaptureCornerKey, %ToggleActiveCaptureCornerKey%
  Gui, Settings:Add, Text, xp+90, Toggle active capture corner
  Gui, Settings:Add, Edit, xp-90 yp+25 r1 w80 vOptNudgeLeftKey, %NudgeLeftKey%
  Gui, Settings:Add, Text, xp+90, Nudge capture box left
  Gui, Settings:Add, Edit, xp-90 yp+25 r1 w80 vOptNudgeRightKey, %NudgeRightKey%
  Gui, Settings:Add, Text, xp+90, Nudge capture box right
  Gui, Settings:Add, Edit, xp-90 yp+25 r1 w80 vOptNudgeUpKey, %NudgeUpKey%
  Gui, Settings:Add, Text, xp+90, Nudge capture box up
  Gui, Settings:Add, Edit, xp-90 yp+25 r1 w80 vOptNudgeDownKey, %NudgeDownKey%
  Gui, Settings:Add, Text, xp+90, Nudge capture box down
  Gui, Settings:Add, Edit, xp-90 yp+25 r1 w80 vOptDictionary1Key, %Dictionary1Key%
  Gui, Settings:Add, Text, xp+90, Quick-access language 1
  Gui, Settings:Add, Edit, xp-90 yp+25 r1 w80 vOptDictionary2Key, %Dictionary2Key%
  Gui, Settings:Add, Text, xp+90, Quick-access language 2
  Gui, Settings:Add, Edit, xp-90 yp+25 r1 w80 vOptDictionary3Key, %Dictionary3Key%
  Gui, Settings:Add, Text, xp+90, Quick-access language 3
  Gui, Settings:Add, Edit, xp-90 yp+25 r1 w80 vOptTextDirectionToggleKey, %TextDirectionToggleKey%
  Gui, Settings:Add, Text, xp+90, Switch text direction (horizontal/vertical)
  Gui, Settings:Add, Text, yp+15, for Japanese/Chinese

  ; -- OCR tab ----------
  Gui, Settings:Tab, OCR
  Gui, Settings:Add, GroupBox, x20 y35 w210 h70, Current OCR Language
  dropDownitems := getLangDropdown(dictionary)
  Gui, Settings:Add, DropDownList, xp+10 yp+20 vOptDictionary Sort W190, %dropDownitems%
  Gui, Settings:Font, underline
  Gui, Settings:Add, Text, yp+25 cBlue gLaunchDownloadMoreLanguages, Additional languages...
  Gui, Settings:Font, norm

  Gui, Settings:Add, GroupBox, x20 y105 w210 h110, Quick-Access OCR Languages
  Gui, Settings:Add, Text, xp+10 yp+20, Slot 1:
  dropDownitems := getLangDropdown(dictionary1)
  Gui, Settings:Add, DropDownList, xp+40 vOptDictionary1 Sort W150, %dropDownitems%
  Gui, Settings:Add, Text, xp-40 yp+30, Slot 2:
  dropDownitems := getLangDropdown(dictionary2)
  Gui, Settings:Add, DropDownList, xp+40 vOptDictionary2 Sort W150, %dropDownitems%
  Gui, Settings:Add, Text, xp-40 yp+30, Slot 3:
  dropDownitems := getLangDropdown(dictionary3)
  Gui, Settings:Add, DropDownList, xp+40 vOptDictionary3 Sort W150, %dropDownitems%

  Gui, Settings:Add, GroupBox, x20 y215 w210 h80, Capture Box
  Gui, Settings:Add, Text, xp+10 yp+20, Color:
  Gui, Settings:Add, Edit, xp+50 w70 ReadOnly vOptCaptureBoxColor c%CaptureBoxColor%, %CaptureBoxColor%
  Gui, Settings:Add, Button, xp+75 w20 gSetCaptureBoxColor, ...
  Gui, Settings:Add, Text, xp-125 yp+30, Opacity:
  dropDownitems := getFormattedDropdown("30|40|50|60|70|80", CaptureBoxAlpha, 40)
  Gui, Settings:Add, DropDownList, xp+50 vOptCaptureBoxAlpha w70 , %dropDownitems%

  Gui, Settings:Add, GroupBox, x20 y300 w430 h165, OCR Options
  Gui, Settings:Add, Checkbox, xp+10 yp+20 vOptOcrPreProcessing Checked%OcrPreProcessing%, Enable OCR pre-processing (improves accuracy)
  Gui, Settings:Add, Checkbox, yp+20 vOptStripFurigana Checked%OcrStripFurigana%, Strip furigana (only used for Japanese)

  dropDownitems := getFormattedDropdown("Traditional (good - faster)|Cube (better - slower)|Auto (best - slowest)", OcrMethod, Traditional (good - faster))
  Gui, Settings:Add, Text, yp+22, OCR method:
  Gui, Settings:Add, DropDownList, xp+80 vOptOcrMethod w145, %dropDownitems%

  dropDownitems := getFormattedDropdown("Auto|Horizontal|Vertical", TextDirection, Horizontal)
  Gui, Settings:Add, Text, xp-80 yp+30, Text direction:
  Gui, Settings:Add, DropDownList, xp+80 vOptTextDirection w145, %dropDownitems%
  Gui, Settings:Add, Text, xp+150 yp+2, (only used for Japanese and Chinese)

  Gui, Settings:Add, Text, xp-230 yp+25, Whitelist (force OCR engine to recognize only the following subset of characters):
  Gui, Settings:Add, Edit, yp+15 w405 vOptWhitelist, %Whitelist%

  Gui, Settings:Add, GroupBox, x240 y35 w210 h260, Preview Box
  Gui, Settings:Add, Checkbox, xp+10 yp+20 vOptPreviewBoxEnabled Checked%PreviewBoxEnabled%, Enable preview box
  Gui, Settings:Add, Text, yp+25, Font family:
  Gui, Settings:Add, Edit, xp+60 r1 w120 vOptPreviewBoxFont, %PreviewBoxFont%
  Gui, Settings:Add, Text, xp-60 yp+30, Font size:
  dropDownitems := getFormattedDropdown("8|9|10|11|12|14|16|18|20|22|24|26|28|36|48|72", PreviewBoxFontSize, 16)
  Gui, Settings:Add, DropDownList, xp+60 vOptPreviewBoxFontSize w70, %dropDownitems%
  Gui, Settings:Add, Text, xp-60 yp+30, Color:
  Gui, Settings:Add, Edit, xp+60 w70 ReadOnly vOptPreviewBoxTextColor c%PreviewBoxTextColor%, %PreviewBoxTextColor%
  Gui, Settings:Add, Button, xp+75 w20 gSetPreviewBoxColor, ...
  Gui, Settings:Add, Text, xp-135 yp+30, Back color:
  Gui, Settings:Add, Edit, xp+60 w70 ReadOnly vOptPreviewBoxBackgroundColor c%PreviewBoxBackgroundColor%, %PreviewBoxBackgroundColor%
  Gui, Settings:Add, Button, xp+75 w20 gSetPreviewBoxBackColor, ...
  Gui, Settings:Add, Text, xp-135 yp+30, Opacity:
  dropDownitems := getFormattedDropdown("30|40|50|60|70|80|90|100", PreviewBoxAlpha, 100)
  Gui, Settings:Add, DropDownList, xp+60 vOptPreviewBoxAlpha w70, %dropDownitems%
  Gui, Settings:Add, Text, xp-60 yp+30, Location:
  dropDownitems := getFormattedDropdown("Fixed|Dynamic", PreviewLocation, Fixed)
  Gui, Settings:Add, DropDownList, xp+60 vOptPreviewLocation w70, %dropDownitems%
  Gui, Settings:Add, Checkbox, xp-60 yp+30 vOptPreviewRemoveCaptureBox Checked%PreviewRemoveCaptureBox%, Remove capture box before preview
  Gui, Settings:Add, Text, xp+17 yp+15, (better accuracy, causes blinking)

  ; -- Output tab ----------
  Gui, Settings:Tab, Output
  Gui, Settings:Add, GroupBox, x20 y35 w430 h65, Prepended/Appended Text
  Gui, Settings:Add, Text, xp+10 yp+20, Prepend:
  Gui, Settings:Add, Edit, xp+50 r1 w120 vOptPrependText, %PrependText%
  Gui, Settings:Add, Text, xp+140, Append:
  Gui, Settings:Add, Edit, xp+50 r1 w120 vOptAppendText, %AppendText%
  Gui, Settings:Add, Text, xp-240 yp+25, Special tokens:  ${cr} = Carriage return,  ${lf} = Linefeed,  ${tab} = Tab

  Gui, Settings:Add, GroupBox, x20 y105 w210 h40, Clipboard
  Gui, Settings:Add, Checkbox, xp+10 yp+20 vOptSaveToClipboard Checked%SaveToClipboard%, Save to clipboard

  Gui, Settings:Add, GroupBox, x240 y105 w210 h40, Newlines
  Gui, Settings:Add, Checkbox, xp+10 yp+20 vOptPreserveNewlines Checked%PreserveNewlines%, Preserve newline characters

  Gui, Settings:Add, GroupBox, x20 y150 w430 h70, Popup Window
  Gui, Settings:Add, Checkbox, xp+10 yp+20 vOptPopupWindow Checked%PopupWindow%, Enable popup window
  Gui, Settings:Add, Text, yp+20, Width:
  dropDownitems := getFormattedDropdown("300|350|400|450|500|550|600|650|700|750|800", PopupWindowWidth, 350)
  Gui, Settings:Add, DropDownList, xp+40 vOptPopupWindowWidth w70, %dropDownitems%
  Gui, Settings:Add, Text, xp+90, Height:
  dropDownitems := getFormattedDropdown("50|100|150|200|250|300|350|400|450|500|550|600|650|700|750|800", PopupWindowHeight, 100)
  Gui, Settings:Add, DropDownList, xp+40 vOptPopupWindowHeight w70, %dropDownitems%

  Gui, Settings:Add, GroupBox, x20 y225 w430 h60, Send to Cursor
  Gui, Settings:Add, Checkbox, xp+10 yp+20 vOptSendToCursor Checked%SendToCursor%, Send to cursor
  Gui, Settings:Add, Checkbox, yp+20 vOptSendToCursorApplyBeforeAndAfterCommands Checked%SendToCursorApplyBeforeAndAfterCommands%, Use the Send command before/after options below

  Gui, Settings:Add, GroupBox, x20 y290 w430 h155, Send to Control (Advanced)
  Gui, Settings:Add, Checkbox, xp+10 yp+20 vOptSendToControl Checked%SendToControl%, Save to control
  Gui, Settings:Add, Text, yp+20, Window title:
  Gui, Settings:Add, Edit, xp+70 r1 w120 vOptControlWindowTitle, %ControlWindowTitle%
  Gui, Settings:Add, Text, xp+140, ClassNN:
  Gui, Settings:Add, Edit, xp+55 r1 w120 vOptControlClassNN, %ControlClassNN%
  Gui, Settings:Add, Text, xp-265 yp+25, ClassNN can be found by using the Window Spy that is
  Gui, Settings:Add, Text, yp+15, packaged with
  Gui, Settings:Font, underline
  Gui, Settings:Add, Text, xp+75 cBlue gLaunchAutoHotKeyDownload, AutoHotkey
  Gui, Settings:Font, norm
  Gui, Settings:Add, Text, xp-75 yp+25, These options use keys from the Hotkey tab surrounded by curly braces. Ex: {Enter}
  Gui, Settings:Add, Text, yp+20, Send command before:
  Gui, Settings:Add, Edit, xp+120 r1 w85 vOptControlSendCommandBefore, %ControlSendCommandBefore%
  Gui, Settings:Add, Text, xp+100, Send command after:
  Gui, Settings:Add, Edit, xp+110 r1 w85 vOptControlSendCommandAfter, %ControlSendCommandAfter%

  ; Items outside of the tab control
  Gui, Settings:Tab ; End tab control
  Gui, Settings:Add, Text, xm
  Gui, Settings:Add, Button, xp+280 w80 gHandleSettingsOK, OK
  Gui, Settings:Add, Button,xp+90 yp+0 w80 gHandleSettingsCancel, Cancel

  ; Init the color chooser code
  initChooseColor()

  ; Show the options dialog
  Gui, Settings:Show,, %PROG_NAME% Preferences

  return
} ; openSettingsDialog


; OK button handler
HandleSettingsOK:
  Gui, Settings:Submit  ; Save each control's contents to its associated variable.
  saveSettingsGUI()
  settingsDlgResult := SETTING_DLG_OK
  Gui, Settings:Destroy
return  ; ButtonOK


; Cancel button/ESC handler
HandleSettingsCancel:
SettingsGuiClose:
SettingsGuiEscape:
  settingsDlgResult := SETTING_DLG_CANCEL
  Gui, Settings:Destroy
return ; ButtonCancel, GuiClose, GuiEscape


; Hotkeys -> More keys...
LaunchKeyHelp:
  run, http://www.autohotkey.com/docs/KeyList.htm
return ; LaunchKeyHelp

; OCR -> Download more languages...
LaunchDownloadMoreLanguages:
  run, http://capture2text.sourceforge.net/#install_additional_languages
return ; LaunchKeyHelp

; Output ->  AutoHotKey...
LaunchAutoHotKeyDownload:
  run, https://autohotkey.com/download/
return ; LaunchAutoHotKeyDownload


; OCR -> Capture Box -> Color:
SetCaptureBoxColor:
  hexColor := openChooseColorDialog()

  if(hexColor)
  {
    GuiControl,, OptCaptureBoxColor, %hexColor%
    Gui, Settings:Font, c%hexColor%
    GuiControl, Font, OptCaptureBoxColor
  }
return ; SetCaptureBoxColor


; OCR -> Preview Box -> Color:
SetPreviewBoxColor:
  hexColor := openChooseColorDialog()

  if(hexColor)
  {
    GuiControl,, OptPreviewBoxTextColor, %hexColor%
    Gui, Settings:Font, c%hexColor%
    GuiControl, Font, OptPreviewBoxTextColor
  }
return ; SetPreviewBoxColor


; OCR -> Preview Box -> Back color:
SetPreviewBoxBackColor:
  hexColor := openChooseColorDialog()

  if(hexColor)
  {
    GuiControl,, OptPreviewBoxBackgroundColor, %hexColor%
    Gui, Settings:Font, c%hexColor%
    GuiControl, Font, OptPreviewBoxBackgroundColor
  }
return ; SetPreviewBoxBackColor


; Save the settings to settings.ini
saveSettingsGUI()
{
  global

  ; Output
  IniWrite, %OptSaveToClipboard%, %SETTINGS_FILE%, Output, SaveToClipboard
  IniWrite, %OptSendToCursor%, %SETTINGS_FILE%, Output, SendToCursor
  IniWrite, %OptSendToCursorApplyBeforeAndAfterCommands%, %SETTINGS_FILE%, Output, SendToCursorApplyBeforeAndAfterCommands
  IniWrite, %OptSendToControl%, %SETTINGS_FILE%, Output, SendToControl
  IniWrite, %OptControlWindowTitle%, %SETTINGS_FILE%, Output, ControlWindowTitle
  IniWrite, %OptControlClassNN%, %SETTINGS_FILE%, Output, ControlClassNN
  ; Not saved because it is not in the GUI
  ;IniWrite, %OptReplaceControlText%, %SETTINGS_FILE%, Output, ReplaceControlText
  IniWrite, %OptControlSendCommandBefore%, %SETTINGS_FILE%, Output, ControlSendCommandBefore
  IniWrite, %OptControlSendCommandAfter%, %SETTINGS_FILE%, Output, ControlSendCommandAfter
  IniWrite, %OptPopupWindow%, %SETTINGS_FILE%, Output, PopupWindow
  IniWrite, %OptPopupWindowWidth%, %SETTINGS_FILE%, Output, PopupWindowWidth
  IniWrite, %OptPopupWindowHeight%, %SETTINGS_FILE%, Output, PopupWindowHeight
  IniWrite, %OptPrependText%, %SETTINGS_FILE%, Output, PrependText
  IniWrite, %OptAppendText%, %SETTINGS_FILE%, Output, AppendText
  IniWrite, %OptPreserveNewlines%, %SETTINGS_FILE%, Output, PreserveNewlines

  ; OCR Specific
  ; Not saved because it is not in the GUI
  ;IniWrite, %OptScaleFactor%, %SETTINGS_FILE%, OCRSpecific, ScaleFactor
  IniWrite, %OptOcrPreProcessing%, %SETTINGS_FILE%, OCRSpecific, OcrPreProcessing
  IniWrite, %OptStripFurigana%, %SETTINGS_FILE%, OCRSpecific, OcrStripFurigana
  IniWrite, %OptDictionary%, %SETTINGS_FILE%, OCRSpecific, Dictionary
  IniWrite, %OptDictionary1%, %SETTINGS_FILE%, OCRSpecific, Dictionary1
  IniWrite, %OptDictionary2%, %SETTINGS_FILE%, OCRSpecific, Dictionary2
  IniWrite, %OptDictionary3%, %SETTINGS_FILE%, OCRSpecific, Dictionary3
  IniWrite, %OptTextDirection%, %SETTINGS_FILE%, OCRSpecific, TextDirection
  IniWrite, %OptOcrMethod%, %SETTINGS_FILE%, OCRSpecific, OcrMethod
  IniWrite, %OptCaptureBoxColor%, %SETTINGS_FILE%, OCRSpecific, CaptureBoxColor
  IniWrite, %OptCaptureBoxAlpha%, %SETTINGS_FILE%, OCRSpecific, CaptureBoxAlpha
  IniWrite, %OptPreviewBoxEnabled%, %SETTINGS_FILE%, OCRSpecific, PreviewBoxEnabled
  IniWrite, %OptPreviewBoxFont%, %SETTINGS_FILE%, OCRSpecific, PreviewBoxFont
  IniWrite, %OptPreviewBoxFontSize%, %SETTINGS_FILE%, OCRSpecific, PreviewBoxFontSize
  IniWrite, %OptPreviewBoxTextColor%, %SETTINGS_FILE%, OCRSpecific, PreviewBoxTextColor
  IniWrite, %OptPreviewBoxBackgroundColor%, %SETTINGS_FILE%, OCRSpecific, PreviewBoxBackgroundColor
  IniWrite, %OptPreviewBoxAlpha%, %SETTINGS_FILE%, OCRSpecific, PreviewBoxAlpha
  IniWrite, %OptPreviewLocation%, %SETTINGS_FILE%, OCRSpecific, PreviewLocation
  IniWrite, %OptPreviewRemoveCaptureBox%, %SETTINGS_FILE%, OCRSpecific, PreviewRemoveCaptureBox

  ; IniWrite doesn't work with UTF-8, so use this as a workaround
  f := FileOpen(WHITELIST_FILE, "w", "utf-8-raw")
  whitelistFileText = tessedit_char_whitelist %Optwhitelist%
  f.Write(whitelistFileText)
  f.Close()

  ; Hotkeys
  IniWrite, %OptStartAndEndCaptureKey%, %SETTINGS_FILE%, Hotkeys, StartAndEndCaptureKey
  IniWrite, %OptEndOnlyCaptureKey%, %SETTINGS_FILE%, Hotkeys, EndOnlyCaptureKey
  IniWrite, %OptToggleActiveCaptureCornerKey%, %SETTINGS_FILE%, Hotkeys, ToggleActiveCaptureCornerKey
  IniWrite, %OptMoveCaptureKey%, %SETTINGS_FILE%, Hotkeys, MoveCaptureKey
  IniWrite, %OptCancelCaptureKey%, %SETTINGS_FILE%, Hotkeys, CancelCaptureKey
  IniWrite, %OptNudgeLeftKey%, %SETTINGS_FILE%, Hotkeys, NudgeLeftKey
  IniWrite, %OptNudgeRightKey%, %SETTINGS_FILE%, Hotkeys, NudgeRightKey
  IniWrite, %OptNudgeUpKey%, %SETTINGS_FILE%, Hotkeys, NudgeUpKey
  IniWrite, %OptNudgeDownKey%, %SETTINGS_FILE%, Hotkeys, NudgeDownKey
  IniWrite, %OptDictionary1Key%, %SETTINGS_FILE%, Hotkeys, Dictionary1Key
  IniWrite, %OptDictionary2Key%, %SETTINGS_FILE%, Hotkeys, Dictionary2Key
  IniWrite, %OptDictionary3Key%, %SETTINGS_FILE%, Hotkeys, Dictionary3Key
  IniWrite, %OptTextDirectionToggleKey%, %SETTINGS_FILE%, Hotkeys, TextDirectionToggleKey

} ; saveSettingsGUI


; Get list of langauge dropdown items
getLangDropdown(defaultItem)
{
  global

  local langItems

  ; Create list of OCR language dropdown items
  enum := installedDics._NewEnum()
  while enum[k, v]
  {
    item := k

    langItems = %langItems%%item%

    ; If this is the currently active OCR language
    if(item == defaultItem)
    {
      ; Append a ! to mark this as the default item
      langItems = %langItems%!
    }
    else
    {
      langItems = %langItems%|
    }
  }

  ; Remove first | and replace ! with ||
  langItems := Trim(langItems, "|")
  StringReplace langItems, langItems, !,||

  if(defaultItem == Spanish)
  {
    msgbox, %langItems%
  }

  return langItems

} ; getLangDropdown


; A generic dropdown formatter.
; list         - A pipe separated list of options
; defaultItem  - The default item in the list
; fallbackItem - Used in case defaultItem is not part of the list
getFormattedDropdown(list, defaultItem, fallbackItem)
{
  StringSplit, listArray, list, |,

  Loop, %listArray0%
  {
    curItem := listArray%a_index%
    output = %output%%curItem%

    if(curItem == defaultItem)
    {
      output = %output%!
      defaultFound = 1
    }
    else if(curItem == fallbackItem)
    {
      output = %output%@
    }
    else
    {
      output = %output%|
    }
  }

  ; Remove last | and replace ! or @ with ||
  output := Trim(output, "|")

  if(defaultFound)
  {
    StringReplace output, output, !,||
    StringReplace output, output, @,|
  }
  else
  {
    StringReplace output, output, @,||
  }

  return output

} ; getFormattedDropdown



EndSettingsDlg:

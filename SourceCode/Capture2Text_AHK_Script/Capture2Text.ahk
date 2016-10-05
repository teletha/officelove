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

#NoEnv                       ; Avoids checking empty variables to see if they are environment variables
#KeyHistory 0                ; Disable key history
#SingleInstance force        ; Skips the dialog box and replaces the old instance automatically
SendMode Input               ; Use faster and more reliable send method
SetWorkingDir %A_ScriptDir%  ; Ensures a consistent starting directory
CoordMode, Mouse, Screen     ; Make 0,0 the top-left of screen
SetTitleMatchMode 2          ; Allow partial window title matches
FileEncoding, UTF-8          ; Use UTF-8 file encoding

#Include %A_ScriptDir%/ScreenCapture.ahk
#Include %A_ScriptDir%/Common.ahk
#Include %A_ScriptDir%/SettingsDlg.ahk
#Include %A_ScriptDir%/PopupDlg.ahk

LEPTONICA_UTIL      = Utils/leptonica_util/leptonica_util.exe ; OCR pre-processing executable
OCR_UTIL_NHOCR      = Utils/nhocr/nhocr.exe         ; NHocr OCR executable
OCR_UTIL_TESSERACT  = Utils/tesseract/tesseract.exe ; Tesseract OCR executable
SUBSTITUTIONS_FILE  = substitutions.txt
BOX_UPDATE_RATE     = 10  ; Update rate of the capture box timer (ms).
PREVIEW_UPDATE_RATE = 10  ; Update rate of the preview box timer (ms).

captureMode        = 0   ; 1 = In capture mode
moveBothCorners    = 0   ; 1 = Move both corners at the same time
bothCornersHeight  = 0   ; Height of the capture box in "move both corners" mode
bothCornersWidth   = 0   ; Width of the capture box in "move both corners" mode
x1                 = 0   ; X1-Coord of the capture box
y1                 = 0   ; Y1-Coord of the capture box
x2                 = 0   ; X2-Coord of the capture box
y2                 = 0   ; Y2-Coord of the capture box
pivotX             = 0   ; Pivot Y-Coord of the capture box
pivotY             = 0   ; Pivot X-Coord of the capture box
dictionary         = English ; The OCR dictionary to use
subList            := [] ; Substitutions list
lastPreview_X1     = 0   ; X1-Coord of the capture box during the last preview update
lastPreview_Y1     = 0   ; Y1-Coord of the capture box during the last preview update
lastPreview_X2     = 0   ; Y2-Coord of the capture box during the last preview update
lastPreview_Y2     = 0   ; X2-Coord of the capture box during the last preview update
lastPreviewChangeTime = 0 ; Time that the capture box changed during the preview update
needToOcrForPreview = 1  ; Set to true when OCR needs to take place for the preview

; Create ocrDicTable and ocrCodeTable
createLanguageTable()

; Create installedDics
getInstalledDics()

; Read the settings in settings.ini
readSettings()

; Set the dictionary directory for the OCR tool
EnvSet, NHOCR_DICDIR, Utils/nhocr/Dic

; Read in the substitutions file
readsubstitutions()

numArg = %0% ; Number of command line arguments passed in by the user

commandLineMode := (numArg >= 4) ; 1 = In Command Line Mode

; Handle command line arguments (if provided)
if(commandLineMode)
{
  x1 = %1%
  y1 = %2%
  x2 = %3%
  y2 = %4%
  outFile = %5%

  doCommandLine(x1, y1, x2, y2, outFile)
}

; Configure the tray
configureTray()

; Create the hotkeys based on user preference
createHotKeys()

; Run HandleExit when this program is closed
OnExit, HandleExit

; Show message on the first run after install
showFirstRunMessage()

return

;-------------------------------------------------------------------------

; Process the command line arguments that user has passed in
doCommandLine(x1, y1, x2, y2, outFile="")
{
  global

  ; Remove tray icon (it will still briefly appear though)
  menu, tray, NoIcon

  ; Capture the screen and OCR it
  ocrText := captureAndOCR(x1, y1, x2, y2)

  ; Replace certain characters in the text
  ocrText := replaceOCRText(ocrText)

  ; Output the OCR'd text based on user-defined settings (default is clipboard)
  doOutput(ocrText)

  ; If the user has specified a file to write the text to
  if(StrLen(outFile) > 0)
  {
    f := FileOpen(outFile, "w", 65001)
    f.Write(ocrText)
    f.Close()
  }

  ; If the popup option is not set, close program
  ; Otherwise, program will close when popup is dismissed (see PopupDlg.ahk)
  if(!popupWindow)
  {
    ExitApp
  }

} ; doCommandLine()


; Show message on the first run after install
showFirstRunMessage()
{
  global

  if(firstRun)
  {
    ; Prevent this message from appearing again
    IniWrite, 0, %SETTINGS_FILE%, Misc, FirstRun

    firstRunMsg = Hello, this appears to be your first time running %PROG_NAME% v%VERSION%.`r`n`r`n
    firstRunMsg = %firstRunMsg%%PROG_NAME% is operated through the use of hotkeys (ie. keyboard`r`n
    firstRunMsg = %firstRunMsg%shortcuts). It is recommended that you familiarize yourself with`r`n
    firstRunMsg = %firstRunMsg%the hotkeys and other settings by right-clicking the %PROG_NAME% tray`r`n
    firstRunMsg = %firstRunMsg%icon in the bottom-right corner and selecting the "Preferences..."`r`n
    firstRunMsg = %firstRunMsg%option.`r`n`r`n
    firstRunMsg = %firstRunMsg%Thank you!`r`n`r`n(This message will not appear again)`r`n

    MsgBox, 64, %PROG_NAME%, %firstRunMsg%
  }

} ; showFirstRunMessage


; On first click start the capture box, on second click capture the image in
; the capture box and send to OCR
Capture:
  if(captureMode)
  {
    ; Update the active capture point
    updateActiveCoordWithMousePos()

    ; End capture mode
    endCaptureMode()

    ; Capture the screen and OCR it
    ocrText := captureAndOCR(x1, y1, x2, y2)

    ; Replace certain characters in the text
    ocrText := replaceOCRText(ocrText)

    ; Output the OCR'd text based on user-defined settings (default is clipboard)
    doOutput(ocrText)
  }
  else ; Not in capture mode
  {
    ; Start capture mode
    startCaptureMode()
  }
return ; Capture:


; Toggle the active capture box corner
ToggleActiveCaptureCorner:
  ; Swap current mouse position and pivot position
  MouseGetPos x, y
  MouseMove pivotX, pivotY, 0
  pivotX := x
  pivotY := y
  updateActiveCoordWithMousePos()
return ; ToggleActiveCaptureCorner:


; Start "move both corners" mode
StartMoveCapture:
  bothCornersWidth := x2 - x1
  bothCornersHeight := y2 - y1
  moveBothCorners = 1
return ; StartMoveCapture:


; End "move both corners" mode
EndMoveCapture:
  moveBothCorners = 0
return ; EndMoveCapture:


; Cancel the capture
CancelCapture:
  endCaptureMode()
return ; CancelCapture:


; Nudge the capture to the left
NudgeLeftKey:
  nudgeNonActiveCaptureCoords(-1, 0)
return ; NudgeLeftKey:


; Nudge the capture to the right
NudgeRightKey:
  nudgeNonActiveCaptureCoords(1, 0)
return ; NudgeRightKey:


; Nudge the capture up
NudgeUpKey:
  nudgeNonActiveCaptureCoords(0, -1)
return ; NudgeUpKey:


; Nudge the capture down
NudgeDownKey:
  nudgeNonActiveCaptureCoords(0, 1)
return ; NudgeDownKey:


; Switch to dictionary 1
HandleKeyDictionary1:
  switchToDic(dictionary1)
  displayOCRLanguageInTray(dictionary1)
return ; HandleKeyDictionary1:


; Switch to dictionary 2
HandleKeyDictionary2:
  switchToDic(dictionary2)
  displayOCRLanguageInTray(dictionary2)
return ; HandleKeyDictionary2:


; Switch to dictionary 3
HandleKeyDictionary3:
  switchToDic(dictionary3)
  displayOCRLanguageInTray(dictionary3)
return ; HandleKeyDictionary3:


; Toggle OCR pre-processing
HandleOcrPreprocessingKey:
  toggleOcrPreprocessing()
return ; HandleOcrPreprocessingKey:


HandleKeyTextDirectionToggle:
  toggleTextDirection()
  displayTextDirectionInTray()
return ; HandleKeyTextDirectionToggle


; Display the language of the provided dictionary in the tray
displayOCRLanguageInTray(dic)
{
  global
  SetTrayTip(dic, 1000)
} ; displayOCRLanguageInTray


; Display the current text direction in the tray
displayTextDirectionInTray()
{
  global

  if(textDirection == "Auto")
  {
    textDirectionMsg = Auto-detect
  }
  else
  {
    textDirectionMsg = %textDirection% text
  }

  SetTrayTip(textDirectionMsg, 1000)

} ; displayTextDirectionInTray


; Get the size of a text control (in pixels)
; str    - The string displayed in the text control
; size   - The font size of the text
; font   - The font of the text
; height - 1 = return height as well as width, 0 = return only the width
getTextSize(str, size, font, height=false)
{
  global

  ; Creating a temporary hidden window, add the text, get size, delete window
  Gui, TextSizeWindow:Font, s%size%, %font%
  Gui, TextSizeWindow:Add, Text, vsizeText, %str%
  GuiControlGet outSize, TextSizeWindow:Pos, sizeText
  Gui, TextSizeWindow:Destroy

  Return height ? outSizeW "," outSizeH : outSizeW

} ; GetTextSize()


; Update the preview box.
UpdatePreviewBox:

  elapsedTime := A_TickCount - lastPreviewChangeTime

  ; Always update (even when capture box is moving)?
  alwaysUpdate := (previewBoxWaitTime < 0)

  ; If capture box has not changed size/position since the last update
  if(  ((x1 == lastPreview_X1) && (y1 == lastPreview_Y1)
     && (x2 == lastPreview_X2) && (y2 == lastPreview_Y2))
     || alwaysUpdate)
  {
    ; If capture box has not moved for the last n milliseconds
    if((elapsedTime >= previewBoxWaitTime) || alwaysUpdate)
    {
      ; If we haven't already OCR'd this part of the screen
      if(needToOcrForPreview || alwaysUpdate)
      {
        if(PreviewRemoveCaptureBox)
        {
          ; Remove the capture box before screen capture. The OCR might be more
          ; accurate without the capture box partially obscuring the text. We make
          ; it transparent instead of using Capture:Hide to prevent the previously
          ; active window from becoming active again (which is quite distracting).
          Gui, Capture:+LastFound
          WinSet, Transparent, 0
        }

        ; Capture the screen and OCR it
        ocrText := captureAndOCR(x1, y1, x2, y2)

        if(PreviewRemoveCaptureBox)
        {
          ; Set the capture box transparency back to normal
          Gui, Capture:+LastFound
          WinSet, Transparent, %captureBoxAlphaScaled%
        }

        ; Replace certain characters in the text
        ocrText := replaceOCRText(ocrText, true)

        previewTextStrLen := StrLen(ocrText)

        ; Limit preview to certain number of characters
        if(previewTextStrLen > 150)
        {
          ocrText := SubStr(ocrText, 1, 150)
          ocrText = %ocrText%...
        }

        ; If still in capture mode after the OCR has finished
        if(captureMode)
        {
          ; Get the size of the OCR'd text on screen (in pixels)
          textSize := getTextSize(ocrText, previewBoxFontSize, previewBoxFont)

          ; Resize the preview box to fit the text
          GuiControl, Preview:Move, PreviewText, W%textSize%

          ; Set the text of the preview
          GuiControl, Preview:Text, PreviewText, %ocrText%

          Gui, Preview:+LastFound

          ; Only show preview if something was OCRd
          if(StrLen(ocrText) > 0)
          {
            WinSet, Transparent, %previewBoxAlphaScaled%
          }
          else
          {
            WinSet, Transparent, 0
          }

          ; Determine location of the preview box
          if(previewLocation == "Fixed")
          {
            previewX := 0
            previewY := 0
          }
          else ; Dynamic
          {
            previewX := x2 + 10
            previewY := y1
          }

          ; Show the resized preview box with the new text
          Gui, Preview:Show, X%previewX% Y%previewY% AutoSize

          ; No need to OCR again unless the capture box is moved/resized
          needToOcrForPreview = 0
        }
      }
    }
  }
  else ; Capture box has been moved/resized
  {
    lastPreviewChangeTime := A_TickCount

    if(previewLocation == "Dynamic")
    {
      ; Get the preview text
      GuiControlGet, PreviewText, Preview:

      ; Only show preview if something was OCRd
      if(StrLen(PreviewText) > 0)
      {
        ; Move the preview box to follow right edge of capture box
        previewX := x2 + 10
        previewY := y1

        Gui, Preview:Show, X%previewX% Y%previewY% AutoSize
      }
    }

    needToOcrForPreview = 1

    ; Save the coordinates of the capture box
    lastPreview_X1 := x1
    lastPreview_Y1 := y1
    lastPreview_X2 := x2
    lastPreview_Y2 := y2
  }

  ; If still in capture mode after the OCR has finished
  if(captureMode)
  {
    ; Start another one-time preview timer
    SetTimer, UpdatePreviewBox, -%PREVIEW_UPDATE_RATE%
  }

return ; UpdatePreviewBox:


; Update the position of the capture box.
UpdateCaptureBox:
  ; Set the box color
  Gui, Capture:Color, %captureBoxColor%

  ; Make the GUI window the last found window for use by the line below
  ; Also keep the window on top of other windows and don't show in the task bar
  Gui, Capture:+LastFound +AlwaysOnTop -Caption +ToolWindow -DPIScale

  ; Set the box opacity
  WinSet, Transparent, %captureBoxAlphaScaled%

  ; Update the active capture point
  updateActiveCoordWithMousePos()

  ; Display the capture box
  Gui, Capture:Show, % "X" x1 " Y" y1 " W" x2-x1 " H" y2-y1

  ; Handle the case where the capture ended but the timer still had one update left
  if(captureMode == 0)
  {
    Gui, Capture:Destroy
    return
  }

return ; UpdateCaptureBox:


; Start capture mode
startCaptureMode()
{
  global

  Gui, Preview:Destroy
  Gui, Capture:Destroy

  ; Activate hotkeys that are only needed in capture mode
  turnOnHotKeys()

  ; Set the capture mode flag
  captureMode = 1

  ; Initialize the capture box coordinates
  MouseGetPos, x1, y1
  MouseGetPos, x2, y2
  MouseGetPos, pivotX, pivotY

  ; Create a timer to update the capture box
  SetTimer, UpdateCaptureBox, %BOX_UPDATE_RATE%

  ; Make the first update immediate rather than waiting for the timer
  Gosub, UpdateCaptureBox

  if(previewBoxEnabled)
  {
    ; Create the preview window (with no text)
    Gui, Preview:Font, s%previewBoxFontSize%, %previewBoxFont%
    Gui, Preview:Margin, 3, 3
    Gui, Preview:Add, Text, vPreviewText X0 X0 c%previewBoxTextColor%
    Gui, Preview:Color, %previewBoxBackgroundColor%
    Gui, Preview:+LastFound +AlwaysOnTop -Caption +ToolWindow
    WinSet, Transparent, %previewBoxAlphaScaled%

    ; Start a one-time timer to update the preview
    SetTimer, UpdatePreviewBox, -%PREVIEW_UPDATE_RATE%
  }

} ; startCaptureMode


; End COR capture mode
endCaptureMode()
{
  global

  ; Clear the capture mode flag
  captureMode = 0

  ; Stop the timer that updates the capture box
  SetTimer, UpdateCaptureBox, Off

  ; Stop the timer that updates the preview box
  SetTimer, UpdatePreviewBox, Off

  ; Remove the capture box
  Gui, Capture:Hide

  ; Remove the preview box
  Gui, Preview:Hide

  ; Deactivate hotkeys that are only needed in capture mode
  turnOffHotKeys()

} ; endCaptureMode


; Update the active capture coordinates with the current mouse position
updateActiveCoordWithMousePos()
{
  MouseGetPos, x, y

  setActiveCaptureCoords(x, y)

} ; updateActiveCoordWithMousePos


; Nudge the non-active capture coordinates
nudgeNonActiveCaptureCoords(x, y)
{
  global

  ; Move the pivot
  pivotX += x
  pivotY += y

} ; nudgeNonActiveCaptureCoords


; Set the active capture coordinates
setActiveCaptureCoords(x, y)
{
  global

  if(x < pivotX)
  {
    ; X is above pivot
    x1 := x

    if(moveBothCorners)
    {
      pivotX := x1 + bothCornersWidth
    }

    x2 := pivotX
  }
  else
  {
    x2 := x

    if(moveBothCorners)
    {
      pivotX := x2 - bothCornersWidth
    }

    x1 := pivotX
  }

  if(y < pivotY)
  {
    ; Y is above pivot
    y1 := y

    if(moveBothCorners)
    {
      pivotY := y1 + bothCornersHeight
    }

    y2 := pivotY
  }
  else
  {
    y2 := y

    if(moveBothCorners)
    {
      pivotY := y2 - bothCornersHeight
    }

    y1 := pivotY
  }

  if(x2 - 1 < x1)
  {
    x2 := x1 + 1
  }

  if(y2 - 1 < y1)
  {
    y2 := y1 + 1
  }
} ; setActiveCaptureCoords


; Capture the screen and OCR it
; Parameters: integers; The screen coordinates that form the rectangle to capture
; Return Value: The OCR'd text
captureAndOCR(x1, y1, x2, y2)
{
  global

  captureFile = Output/screen_capture.bmp ; Name of the captured image
  dictionaryCode := ocrDicTable[dictionary]
  ocrUtil := dic2OcrUtil(dictionary)
  japConfigArg =

  ; Configure arguments based on OCR util
  if(ocrUtil == OCR_UTIL_NHOCR)
  {
    EnvSet, NHOCR_DICCODES, %dictionaryCode% ; Set the dictionary for the OCR tool

    ocrOutputFile = Output/ocr.txt

    if(ocrPreProcessing)
    {
      ocrInputFile = Output/ocr_in.pbm
    }
    else
    {
      ocrInputFile = Output/ocr_in.pgm
    }

    ocrUtilArgs = -block -o %ocrOutputFile% %ocrInputFile%
  }
  else ; Tesseract
  {
    ocrOutputFile = Output/ocr
    ocrInputFile = Output/ocr_in.tif

    ; If the vertical text direction and Chinese or Japanese dictionary
    if((dictionary == "Japanese")
       || (dictionary == "Chinese - Simplified")
       || (dictionary == "Chinese - Traditional"))
    {
      if(textDirection == "Vertical")
      {
        ; Assume a single uniform block of vertically aligned text.
        psmOption = -psm 5

        ; Use a Japanese specific config (Note: config was only tested with vertical text)
        japConfigArg = jap_config
      }
      else if(textDirection == "Horizontal")
      {
        ; Assume a single uniform block of text.
        psmOption = -psm 6
      }
      else ; Auto
      {
        autoDirection := getAutoTextDirection(x1, y1, x2, y2)

        if(autoDirection == "Vertical")
        {
          ; Assume a single uniform block of vertically aligned text.
          psmOption = -psm 5

          ; Use a Japanese specific config (Note: config was only tested with vertical text)
          japConfigArg = jap_config
        }
        else
        {
          ; Assume a single uniform block of text.
          psmOption = -psm 6
        }
      }
    }
    else ; Horizontal text or not a Chinese or Japanese dictionary
    {
      ; Fully automatic page segmentation, but no OSD (Note: OSD is orientation detection). (Default)
      psmOption = -psm 3
    }

    whitelistArg =

    ; Determine if the whitelist should be used
    if (StrLen(whitelist) != 0)
    {
      whitelistArg = %WHITELIST_FILE%
    }

    ; Determine OCR Method (Traditional, cube, or auto)
    if(ocrMethod == "Traditional (good - faster)")
    {
      ocrEngineMode = 0
    }
    else if(ocrMethod == "Cube (better - slower)")
    {
      ocrEngineMode = 1
    }
    else ; Auto
    {
      ocrEngineMode = 2
    }

    ocrUtilArgs = %ocrInputFile% %ocrOutputFile% -l %dictionaryCode% %psmOption% -c tessedit_ocr_engine_mode=%ocrEngineMode% %whitelistArg% %japConfigArg%
  }

  ; Form the screen capture rectangle
  captureRect = %x1%, %y1%, %x2%, %y2%

  ; Capture the selected portion of the screen
  CaptureScreen(captureRect, False, captureFile)

  ; Negate image? 0 = No, 1 = Yes, 2 = Auto
  if(ocrPreProcessing)
  {
    negateArg = 2
  }
  else
  {
    negateArg = 0
  }

  stripFurigana = 0

  if((dictionary == "Japanese") || (dictionary == "Japanese (NHocr)"))
  {
    ; Strip furigana from Japanese text?
    if(ocrStripFurigana)
    {
      if(textDirection == "Vertical")
      {
        stripFurigana = 1
      }
      else if(textDirection == "Horizontal")
      {
        stripFurigana = 2
      }
      else ; Auto
      {
        autoDirection := getAutoTextDirection(x1, y1, x2, y2)

        if(autoDirection == "Vertical")
        {
          stripFurigana = 1
        }
        else
        {
          stripFurigana = 2
        }
      }
    }
  }

  ; Scale image?
  if(scaleFactor == 1.0)
  {
    performScaleArg = 0
  }
  else
  {
    performScaleArg = 1
  }

  ; Pre-process file (unsharp mask + binarize) to improve OCR
  RunWait, %LEPTONICA_UTIL% %captureFile%  %ocrInputFile%  %negateArg% 0.5  %performScaleArg% %scaleFactor%  %ocrPreProcessing% 5 2.5  %ocrPreProcessing% 2000 2000 0 0 0.0  %stripFurigana%, , Hide

  ; Run OCR
  RunWait, %ocrUtil%  %ocrUtilArgs%, , Hide

  ; Read the OCR'd text from the OCR tools output
  ocrOutputFile = Output/ocr.txt
  FileRead, ocrText, %ocrOutputFile%

  return ocrText
} ; captureAndOCR


; Determine text direction for when text direction is set to Auto.
; Returns "Vertical" if vertical, "Horizontal" if horizontal
getAutoTextDirection(x1, y1, x2, y2)
{
  global

  captureWidth := x2 - x1 + 1
  captureHeight := y2 - y1 + 1
  aspectRatio := captureWidth / captureHeight

  if(aspectRatio < 2.0)
  {
    autoDirection = Vertical
  }
  else
  {
    autoDirection = Horizontal
  }

  return autoDirection

} ; getAutoTextDirection


; Replace tokens with special characters
replaceSpecialChars(text)
{
  global

  StringReplace, text, text, ${cr}, `r, All
  StringReplace, text, text, ${lf}, `n, All
  StringReplace, text, text, ${tab}, %A_TAB%, All

  return text
} ; replaceSpecialChars


; Save the settings to settings.ini
saveSettings()
{
  global

  ; Output
  IniWrite, %saveToClipboard%, %SETTINGS_FILE%, Output, SaveToClipboard
  IniWrite, %sendToCursor%, %SETTINGS_FILE%, Output, SendToCursor
  IniWrite, %popupWindow%, %SETTINGS_FILE%, Output, PopupWindow
  IniWrite, %sendToControl%, %SETTINGS_FILE%, Output, SendToControl

  ; OCR Specific
  IniWrite, %ocrPreProcessing%, %SETTINGS_FILE%, OCRSpecific, OcrPreProcessing
  IniWrite, %dictionary%, %SETTINGS_FILE%, OCRSpecific, Dictionary
  IniWrite, %textDirection%, %SETTINGS_FILE%, OCRSpecific, TextDirection
} ; saveSettings


; Create the hotkeys based on user preference.
createHotKeys()
{
  global

  ; Hotkeys cannot be blank, so assign one if needed
  if(StrLen(startAndEndCaptureKey) == 0)
    startAndEndCaptureKey = #q
  if(StrLen(dictionary1Key) == 0)
    dictionary1Key = #1
  if(StrLen(dictionary2Key) == 0)
    dictionary2Key = #2
  if(StrLen(dictionary3Key) == 0)
    dictionary3Key = #3
  if(StrLen(ocrPreprocessingKey) == 0)
    ocrPreprocessingKey = +^#b
  if(StrLen(textDirectionToggleKey) == 0)
    textDirectionToggleKey = #w
  if(StrLen(endOnlyCaptureKey) == 0)
    endOnlyCaptureKey = LButton
  if(StrLen(ToggleActiveCaptureCornerKey) == 0)
    toggleActiveCaptureCornerKey = Space
  if(StrLen(MoveCaptureKey) == 0)
    moveCaptureKey = RButton
  if(StrLen(cancelCaptureKey) == 0)
    cancelCaptureKey = Esc
  if(StrLen(nudgeLeftKey) == 0)
    nudgeLeftKey = Left
  if(StrLen(nudgeRightKey) == 0)
    nudgeRightKey = Right
  if(StrLen(nudgeUpKey) == 0)
    nudgeUpKey = Up
  if(StrLen(nudgeDownKey) == 0)
    nudgeDownKey = Down

  HotKey, %startAndEndCaptureKey%, Capture
  HotKey, %dictionary1Key%, HandleKeyDictionary1
  HotKey, %dictionary2Key%, HandleKeyDictionary2
  HotKey, %dictionary3Key%, HandleKeyDictionary3
  HotKey, %ocrPreprocessingKey%, HandleOcrPreprocessingKey
  HotKey, %textDirectionToggleKey%, HandleKeyTextDirectionToggle

  ; The following hotkeys are created but initialized to a disabled state
  HotKey, %endOnlyCaptureKey%, Capture, Off
  HotKey, %toggleActiveCaptureCornerKey%, ToggleActiveCaptureCorner, Off
  HotKey, %moveCaptureKey%, StartMoveCapture, Off
  HotKey, %moveCaptureKey% UP, EndMoveCapture, Off
  HotKey, %cancelCaptureKey%, CancelCapture, Off
  HotKey, %nudgeLeftKey%, NudgeLeftKey, Off
  HotKey, %nudgeRightKey%, NudgeRightKey, Off
  HotKey, %nudgeUpKey%, NudgeUpKey, Off
  HotKey, %nudgeDownKey%, NudgeDownKey, Off

} ; createHotKeys


; Active hotkeys that are only needed in capture mode
turnOnHotKeys()
{
  global

  HotKey, %endOnlyCaptureKey%, On, On
  HotKey, %toggleActiveCaptureCornerKey%, On, On
  HotKey, %moveCaptureKey%, On, On
  HotKey, %moveCaptureKey% UP, On, On
  HotKey, %cancelCaptureKey%, On, On
  HotKey, %nudgeLeftKey%, On, On
  HotKey, %nudgeRightKey%, On, On
  HotKey, %nudgeUpKey%, On, On
  HotKey, %nudgeDownKey%, On, On

} ; turnOnHotKeys


; Deactivate hotkeys that are only needed in capture mode
turnOffHotKeys()
{
  global

  HotKey, %endOnlyCaptureKey%, Off
  HotKey, %toggleActiveCaptureCornerKey%, Off
  HotKey, %moveCaptureKey%, Off
  HotKey, %moveCaptureKey% UP, Off
  HotKey, %cancelCaptureKey%, Off
  HotKey, %nudgeLeftKey%, Off
  HotKey, %nudgeRightKey%, Off
  HotKey, %nudgeUpKey%, Off
  HotKey, %nudgeDownKey%, Off

} ; turnOffHotKeys


; Process the text before it is output.
; Parameters:
;   text - string; The OCR'd text
processTextBeforeOutput(text)
{
  global

  ; Add special characters to Prepend Text
  StringLen, length, prependText
  if(length > 0)
  {
    prependText := replaceSpecialChars(prependText)
  }

  ; Add special characters to Append Text
  StringLen, length, appendText
  if(length > 0)
  {
    appendText := replaceSpecialChars(appendText)
  }

  ; Append/Prepend text to the OCR'd text
  text = %prependText%%text%%appendText%

  return text

} ; processTextBeforeOutput


; Output the OCR'd text based on user-defined settings (default is clipboard)
; Parameters:
;   text - string; The OCR'd text
doOutput(text)
{
  global

  ; Do final text processing (append text, prepend text, etc.)
  text := processTextBeforeOutput(text)

  ; Save the text to the clipboard
  if(saveToClipboard)
  {
    clipboard = %text%
  }

  ; Output the text to whichever control contains the blinking cursor.
  if(sendToCursor)
  {
    outputSendToCursor(text)
  }

  ; Show the text in a pop-up window
  if(popupWindow)
  {
    openOutputPopup(text, popupWindowWidth, popupWindowHeight)
  }

  ; Send the text to control
  if(sendToControl)
  {
    outputSendToControl(text)
  }

} ; doOutput


; Output the text to whichever control contains the blinking cursor.
; Parameters:
;   string - text; The OCR'd text
outputSendToCursor(text)
{
  global

  ; Send a command to the control before sending the text
  if(sendToCursorApplyBeforeAndAfterCommands && (StrLen(controlSendCommandBefore) > 0))
  {
    Send, %controlSendCommandBefore%
    Sleep, 25
  }

  ; Save the current contents of the clipboard so we can restore it later.
  ; This will also save things like pictures and formatting.
  clipSaved := ClipboardAll

  ; Put text in clipboard and paste the text to the control
  clipboard = %text%
  Sleep, 50
  Send, ^v
  Sleep, 50

  ; Restore the original clipboard
  clipboard := clipSaved

  ; Send a command to the control after sending the text
  if(sendToCursorApplyBeforeAndAfterCommands && (StrLen(controlSendCommandAfter) > 0))
  {
    Sleep, 25
    Send, %controlSendCommandAfter%
  }
} ; outputSendToCursor


; Output the text to control
; Parameters:
;   string - text; The OCR'd text
outputSendToControl(text)
{
  global

  ; Send a command to the control before sending the text
  if(StrLen(controlSendCommandBefore) > 0)
  {
    ControlSend, %controlClassNN%, %controlSendCommandBefore%, %controlWindowTitle%
    Sleep, 25
  }

  ; Send the OCR'd text to control
  if(replaceControlText)
  {
    ; Replace the text
    Sleep, 25
    ControlSetText, %controlClassNN%, %text%, %controlWindowTitle%
  }
  else
  {
    ; Send the text
    ControlSend, %controlClassNN%, %text%, %controlWindowTitle%
  }

  ; Send a command to the control after sending the text
  if(StrLen(controlSendCommandAfter) > 0)
  {
    Sleep, 25
    ControlSend, %controlClassNN%, %controlSendCommandAfter%, %controlWindowTitle%
  }

} ; outputSendToControl


; Configure the tray
configureTray()
{
  global

  ; Remove items placed automatically by AutoHotKey
  Menu, TRAY, NoStandard

  ; Set the tray icon
  Menu, TRAY, Icon, Capture2Text.ico

  ; Set the tray tooltip
  Menu, TRAY, Tip, %PROG_NAME%

  ; Add  Preferences...
  Menu, TRAY, Add, &Preferences..., MenuHandlerPreferences

  ; Add separator
  Menu, TRAY, Add

  ; Add  Save to Clipboard
  Menu, TRAY, Add, Save to &Clipboard, MenuHandlerSaveToClipboard

  ; Add Show Popup Window
  Menu, TRAY, Add, Show &Popup Window, MenuHandlerPopupWindow

  ; Add Send to Cursor
  Menu, TRAY, Add, Send to C&ursor, MenuHandlerSendToCursor

  ; Add separator
  Menu, TRAY, Add

  ; Add OCR Language items
  enum := installedDics._NewEnum()
  while enum[k, v]
  {
    ; Note: k = Language, v = Dic code
    menuItem := k
    Menu, MenuItemOCRLangSettings, Add, %menuItem%, MenuHandlerLang
  }

  ; Add OCR Language
  Menu, TRAY, Add, OCR &Language, :MenuItemOCRLangSettings

  ; Add separator
  Menu, TRAY, Add

  ; Add Help...
  Menu, TRAY, Add, &Help..., MenuHandlerHelp

  ; Add About...
  Menu, TRAY, Add, &About..., MenuHandlerAbout

  ; Add Exit
  Menu, TRAY, Add, E&xit, HandleExit

  ; Check any menu item that needs to be checked at startup
  checkMenuItemsAtStartup()

} ; configureTray


; Check any menu item that needs to be checked at startup
checkMenuItemsAtStartup()
{
  global

  ; OCR Language menu
  uncheckLangMenuItems()
  checkLangMenuItem(dictionary)

  ; Uncheck all output options
  Menu, TRAY, Uncheck, Save to &Clipboard
  Menu, TRAY, Uncheck, Send to C&ursor
  Menu, TRAY, Uncheck, Show &Popup Window

  if(saveToClipboard)
    Menu, TRAY, Check, Save to &Clipboard

  if(sendToCursor)
    Menu, TRAY, Check, Send to C&ursor

  if(popupWindow)
    Menu, TRAY, Check, Show &Popup Window

} ; checkMenuItemsAtStartup


; Check a language menu item
; langMenuItem is the language (not the dict code)
checkLangMenuItem(langMenuItem)
{
  global
  uncheckLangMenuItems()
  Menu, MenuItemOCRLangSettings, Check, %langMenuItem%
} ; checkLangMenuItem


; Uncheck all language menu items
uncheckLangMenuItems()
{
  global

  enum := installedDics._NewEnum()
  while enum[k, v]
  {
    menuItem := k
    Menu, MenuItemOCRLangSettings, Uncheck, %menuItem%

    ; Sleep or else the compiled .exe version won't remove the checkbox
    ;Sleep, 10
  }

} ; uncheckLangMenuItems


; Get the OCR tool that goes with a dictionary
; inDictionary - The language (not the dic code)
dic2OcrUtil(inDictionary)
{
  global

  if(inDictionary == "Japanese (NHocr)" or inDictionary == "Chinese (NHocr)")
  {
    theOcrUtil = %OCR_UTIL_NHOCR%
  }
  else
  {
    theOcrUtil = %OCR_UTIL_TESSERACT%
  }

  return theOcrUtil

} ; dic2OcrUtil


; Replace certain characters in the OCR'd text
replaceOCRText(ocrText, force=false)
{
  global

  if(!preserveNewlines || force)
  {
    ; If Japanese or Chinese remove newlines, otherwise, replace them with spaces
    if(  (dictionary == "Japanese")
      || (dictionary == "Japanese (NHocr)")
      || (dictionary == "Chinese (NHocr)")
      || (dictionary == "Chinese - Simplified")
      || (dictionary == "Chinese - Traditional"))
    {
      StringReplace, ocrText, ocrText, `r`n, , All
      StringReplace, ocrText, ocrText, `r, , All
      StringReplace, ocrText, ocrText, `n, , All
    }
    else ; Non-Japanese or Chinese language
    {
      StringReplace, ocrText, ocrText, `r`n, %A_Space%, All
      StringReplace, ocrText, ocrText, `r, %A_Space%, All
      StringReplace, ocrText, ocrText, `n, %A_Space%, All
    }
  }

  ; Trim whitesapce from start and end
  ocrText := Trim(ocrText, "`n")
  ocrText := Trim(ocrText, "`r")
  ocrText := Trim(ocrText, " ")

  ocrText := performsubstitutions(ocrText)

  return ocrText

} ; replaceOCRText()


; Read in the substitutions file. Parses the "All:" and current OCR language sections
readsubstitutions()
{
 global

  ; Clear substitutions list
  subList := []

  ; File parse state
  ; 0 = Searching for All: section
  ; 1 = Looping through All: section
  ; 2 = Searching for current OCR language section
  ; 3 = Looping through current OCR language section
  ; 4 = Done
  state = 0

  ; Read the substitutions file
  Loop, read, %SUBSTITUTIONS_FILE%
  {
    if(state == 0)
    {
      if(A_LoopReadLine == "All:")
      {
        state = 1
      }
    }
    else if((state == 1) || (state == 3))
    {
      StringLeft, firstChar, A_LoopReadLine, 1

      ; If the line is blank or is a comment
      if((StrLen(A_LoopReadLine) == 0) || (firstChar == "#"))
      {
        continue
      }

      ; If the line contains "="
      if(InStr(A_LoopReadLine, "="))
      {
        subList.Insert(A_LoopReadLine)
      }
      else
      {
        if(state == 1)
        {
          state = 2
        }
        else
        {
          state = 4
        }
      }
    }
    else if(state == 2)
    {
      dictLine = %dictionary%:

      if(A_LoopReadLine == dictLine)
      {
        state = 3
      }
    }
    else if(state == 4)
    {
      break
    }
  }

} ; readsubstitutions


; Perform substitutions on the provided text.
; Global inputs:
;   subList
performsubstitutions(text)
{
  global

  ; Loop through substitutions list
  Loop % subList.MaxIndex()
  {
    ; Get current substitution
    curSub := % subList[A_Index]

    ; Extract the from and to portions of the substitution
    StringSplit, fields, curSub, =
    replaceFrom = %fields1%
    replaceTo = %fields2%

    ; Replace special tokens
    StringCaseSense, On
    StringReplace, replaceFrom, replaceFrom, `%cr`%, `r, All
    StringReplace, replaceTo, replaceTo, `%cr`%, `r, All

    StringReplace, replaceFrom, replaceFrom, `%eq`%, =, All
    StringReplace, replaceTo, replaceTo, `%eq`%, =, All

    StringReplace, replaceFrom, replaceFrom, `%lf`%, `n, All
    StringReplace, replaceTo, replaceTo, `%lf`%, `n, All

    StringReplace, replaceFrom, replaceFrom, `%perc`%, `%, All
    StringReplace, replaceTo, replaceTo, `%perc`%, `%, All

    StringReplace, replaceFrom, replaceFrom, `%space`%, %A_Space%, All
    StringReplace, replaceTo, replaceTo, `%space`%, %A_Space%, All

    StringReplace, replaceFrom, replaceFrom, `%tab`%, %A_Tab%, All
    StringReplace, replaceTo, replaceTo, `%tab`%, %A_Tab%, All

    ; Perform substitution
    StringReplace, text, text, %replaceFrom%, %replaceTo%, All
    StringCaseSense, Off
  }

  return text

} ; performsubstitutions


; Handle a language menu item from Settings | OCR Language
MenuHandlerLang:
  switchToDic(A_ThisMenuItem)
return ; MenuHandlerLang:


; Switch to the given dictionary. This sets the dictionary, menu check mark, and OCR util
switchToDic(dic)
{
  global
  checkLangMenuItem(dic)
  dictionary := dic

  ; Read the substitutions file for the new language
  readsubstitutions()

  ; Force the preview to OCR again
  needToOcrForPreview = 1

} ; switchToDic


; Toggle text direction
toggleTextDirection()
{
  global

  if(textDirection == "Auto")
  {
    textDirection = Horizontal
  }
  else if(textDirection == "Horizontal")
  {
    textDirection = Vertical
  }
  else
  {
    textDirection = Auto
  }

  ; Force the preview to OCR again
  needToOcrForPreview = 1

} ; toggleTextDirection


; Toggle OCR pre-processing
toggleOcrPreprocessing()
{
  global

  if(ocrPreProcessing)
  {
    ocrPreProcessing = 0
    SetTrayTip("OCR Pre-Processing OFF", 1000)
  }
  else
  {
    ocrPreProcessing = 1
    SetTrayTip("OCR Pre-Processing ON", 1000)
  }

  ; Force the preview to OCR again
  needToOcrForPreview = 1

} ; toggleOcrPreprocessing


; Set the tray tip
SetTrayTip(txt, timeout)
{
  global

  TrayTip, , %txt%
  SetTimer, RemoveTrayTip, %timeout%

} ; SetTrayTip


; Used with SetTrayTip to remove the tray tip
RemoveTrayTip:
  SetTimer, RemoveTrayTip, Off
  TrayTip
return ; RemoveTrayTip:


; Handle Save to Clipboard
MenuHandlerSaveToClipboard:
  if(saveToClipboard)
  {
    Menu, TRAY, Uncheck, %A_ThisMenuItem%
    saveToClipboard = 0
  }
  else
  {
    Menu, TRAY, Check, %A_ThisMenuItem%
    saveToClipboard = 1
  }
return ; MenuHandlerSaveToClipboard:


; Handle Send to Cursor
MenuHandlerSendToCursor:
  if(sendToCursor)
  {
    Menu, TRAY, Uncheck, %A_ThisMenuItem%
    sendToCursor = 0
  }
  else
  {
    Menu, TRAY, Check, %A_ThisMenuItem%
    sendToCursor = 1
  }
return ; MenuHandlerSendToCursor:


; Handle Show Popup Window
MenuHandlerPopupWindow:
  if(popupWindow)
  {
    Menu, TRAY, Uncheck, %A_ThisMenuItem%
    popupWindow = 0
  }
  else
  {
    Menu, TRAY, Check, %A_ThisMenuItem%
    popupWindow = 1
  }
return ; MenuHandlerPopupWindow:


; Handle Preferences...
MenuHandlerPreferences:
  saveSettings()
  openSettingsDialog()

  ; Set timer to check result of the settings dialog
  SetTimer, CheckSettingsDlgClosed, 50
return ; MenuHandlerPreferences


; Used to check the result of the settings dialog and take the appropriate action
CheckSettingsDlgClosed:
  if((settingsDlgResult == SETTING_DLG_INACTIVE)
    || (settingsDlgResult == SETTING_DLG_CANCEL))
  {
    settingsDlgResult := SETTING_DLG_INACTIVE
    SetTimer, CheckSettingsDlgClosed, Off
  }
  else if(settingsDlgResult == SETTING_DLG_OK)
  {
    ; Disable all current hotkeys. If we don't do this, the old hotkeys
    ; will remain active alongside the new hotkeys.
    HotKey, %startAndEndCaptureKey%, Off
    HotKey, %dictionary1Key%, Off
    HotKey, %dictionary2Key%, Off
    HotKey, %dictionary3Key%, Off
    HotKey, %ocrPreprocessingKey%, Off
    HotKey, %textDirectionToggleKey%, Off
    HotKey, %endOnlyCaptureKey%, Capture, Off
    HotKey, %toggleActiveCaptureCornerKey%, ToggleActiveCaptureCorner, Off
    HotKey, %moveCaptureKey%, StartMoveCapture, Off
    HotKey, %moveCaptureKey% UP, EndMoveCapture, Off
    HotKey, %cancelCaptureKey%, CancelCapture, Off
    HotKey, %nudgeLeftKey%, NudgeLeftKey, Off
    HotKey, %nudgeRightKey%, NudgeRightKey, Off
    HotKey, %nudgeUpKey%, NudgeUpKey, Off
    HotKey, %nudgeDownKey%, NudgeDownKey, Off

    ; Read the settings that where set by the Settings dialog
    readSettings()

    ; Determine check mark in the tray context menu
    checkMenuItemsAtStartup()

    ; Re-create the hotkeys with the new settings
    createHotKeys()

    ; Enable the always on hotkeys. If we don't do this, the hotkeys that
    ; didn't change would be disabled due to the above code.
    HotKey, %startAndEndCaptureKey%, On
    HotKey, %dictionary1Key%, On
    HotKey, %dictionary2Key%, On
    HotKey, %dictionary3Key%, On
    HotKey, %ocrPreprocessingKey%, On
    HotKey, %textDirectionToggleKey%, On

    settingsDlgResult := SETTING_DLG_INACTIVE
    SetTimer, CheckSettingsDlgClosed, Off

    ; Read the substitutions file in case language changed
    readsubstitutions()
  }
return ; CheckSettingsDlgClosed:


; Handle Help...
MenuHandlerHelp:
  Run, http://capture2text.sourceforge.net/
return ; MenuHandlerHelp


; Handle About...
MenuHandlerAbout:
  MsgBox, 64, %PROG_NAME%, %PROG_NAME%`r`nVersion: %VERSION%`r`nAuthor: %AUTHOR%%A_TAB%
return ; MenuHandlerAbout


; Handle program closing
HandleExit:
  saveSettings()
  ExitApp ; Exit this program
return ; HandleExit


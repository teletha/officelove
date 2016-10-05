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

; Skip past contents of this file so that the subroutines are not automatically
; triggered when another file #includes it
Goto, EndPopupDlg


; Open the output popup and place the specified text inside
openOutputPopup(text, width, height)
{
  global

  Gui, Popup:Destroy

  Gui, Popup:Font, s12
  Gui, Popup:Add, Edit, x7 y5 w%width% h%height% vOptPopupText, %text%
  Gui, Popup:Font, s8
  Gui, Popup:Add, Button, default gHandlePopupOK w80 vOptPopupOK Default, OK
  Gui, Popup:+Resize +ToolWindow +AlwaysOnTop
  Gui, Popup:Show,, %PROG_NAME% Popup Result

  return

} ; openOutputPopup


; Close the popup dialog
closePopupDialog()
{
  global 
  
  Gui, Popup:Submit
  Gui, Popup:Destroy

  ; Close program if popup was activated in Command Line Mode
  if(commandLineMode)
  { 
    ExitApp
  }
  
  return
}


; Handle OK
HandlePopupOK:
  if(saveToClipboard)
  {
    ; Save contents of text box to clipboard
    guiControlGet, text,, OptPopupText
    clipboard = %text%
  }
  
  closePopupDialog()
return


; Handle Close, Escape
PopupGuiClose:
PopupGuiEscape:
  closePopupDialog()
return ; OK, Close, Escape


; Called when the dialog is resized
PopupGuiSize:
  newEditWidth := A_GuiWidth - 16
  newEditHeight := A_GuiHeight - 37
  GuiControl, Move, OptPopupText, w%newEditWidth% h%newEditHeight%

  newButtonY := A_GuiHeight - 26
  GuiControl, Move, OptPopupOK, y%newButtonY%
return ; PopupGuiSize:


EndPopupDlg:

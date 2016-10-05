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

; Everthing in this file came from http://www.autohotkey.com/forum/topic9436.html

initChooseColor()
{
  global
  
  ; Most of the setup for the font picker dialog needs to be done only once,
  ; which is why this section is here rather than in the button's subroutine.
  /*
  typedef struct tagCHOOSECOLOR 
  {
    DWORD lStructSize;
    HWND hwndOwner;
    HINSTANCE hInstance;
    COLORREF rgbResult;
    COLORREF* lpCustColors;
    DWORD Flags;
    LPARAM lCustData;
    LPCCHOOKPROC lpfnHook;
    LPCTSTR lpTemplateName;
  } CHOOSECOLOR, *LPCHOOSECOLOR;
  */
  
  SizeOfStructForChooseColor = 0x24
  VarSetCapacity(StructForChooseColor, SizeOfStructForChooseColor, 0)
  VarSetCapacity(StructArrayForChooseColor, 64, 0)

  Gui, +LastFound
  GuiHWND := WinExist()  ; Relies on the line above to get the unique ID of GUI window.

  InsertInteger(SizeOfStructForChooseColor, StructForChooseColor, 0)  ; DWORD lStructSize
  InsertInteger(GuiHWND, StructForChooseColor, 4)  ; HWND hwndOwner (makes dialog "modal").
  InsertInteger(0x0 ,    StructForChooseColor, 8)  ; HINSTANCE hInstance
  InsertInteger(0x0 ,    StructForChooseColor, 12)  ; clr.rgbResult =  0;
  InsertInteger(&StructArrayForChooseColor , StructForChooseColor, 16)  ; COLORREF *lpCustColors
  InsertInteger(0x00000100 , StructForChooseColor, 20)  ; Flag: Anycolor
  InsertInteger(0x0 ,    StructForChooseColor, 24)  ; LPARAM lCustData
  InsertInteger(0x0 ,    StructForChooseColor, 28)  ; LPCCHOOKPROC lpfnHook
  InsertInteger(0x0 ,    StructForChooseColor, 32)  ; LPCTSTR lpTemplateName

} ;initChooseColor

; Open the color chooser dialog and get a RRGGBB color in return or NULL if cancel was clicked.
openChooseColorDialog()
{
  global
  
  nRC := DllCall("comdlg32\ChooseColorA", str, StructForChooseColor)  ; Display the dialog.
  
  if(errorlevel <> 0) || (nRC = 0)
  {
     return
  }
  
  SetFormat, integer, Hex  ; Show RGB color extracted below in hex format.
  hexColor := BGRtoRGB(ExtractInteger(StructForChooseColor, 12))
  hexColor := SubStr(hexColor, 3) ; Remove initial 0x
  SetFormat, integer, d
  
  return %hexColor%

} ; openChooseColorDialog


ExtractInteger(ByRef pSource, pOffset = 0, pIsSigned = false, pSize = 4)
; See DllCall documentation for details.
{
  SourceAddress := &pSource + pOffset  ; Get address and apply the caller's offset.
  result := 0  ; Init prior to accumulation in the loop.
  Loop %pSize%  ; For each byte in the integer:
  {
    result := result | (*SourceAddress << 8 * (A_Index - 1))  ; Build the integer from its bytes.
    SourceAddress += 1  ; Move on to the next byte.
  }
  if (!pIsSigned OR pSize > 4 OR result < 0x80000000)
    return result  ; Signed vs. unsigned doesn't matter in these cases.
  ; Otherwise, convert the value (now known to be 32-bit) to its signed counterpart:
  return -(0xFFFFFFFF - result + 1)
  
} ; ExtractInteger


InsertInteger(pInteger, ByRef pDest, pOffset = 0, pSize = 4)
; To preserve any existing contents in pDest, only pSize number of bytes starting at
; pOffset are altered in it. The caller must ensure that pDest has sufficient capacity.
{
  mask := 0xFF  ; This serves to isolate each byte, one by one.
  Loop %pSize%  ; Copy each byte in the integer into the structure as raw binary data.
  {
    DllCall("RtlFillMemory", UInt, &pDest + pOffset + A_Index - 1, UInt, 1  ; Write one byte.
        , UChar, (pInteger & mask) >> 8 * (A_Index - 1))  ; This line is auto-merged with above at load-time.
    mask := mask << 8  ; Set it up for isolation of the next byte.
  }
  
} ; InsertInteger


BGRtoRGB(oldValue)
{
  Value := (oldValue & 0x00ff00)
  Value += ((oldValue & 0xff0000) >> 16)
  Value += ((oldValue & 0x0000ff) << 16) 
  return Value
  
} ; BGRtoRGB



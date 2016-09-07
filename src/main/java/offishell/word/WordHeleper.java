/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.word;

import java.util.function.UnaryOperator;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr;

/**
 * @version 2016/06/04 18:15:54
 */
public class WordHeleper {

    /**
     * 
     */
    private WordHeleper() {
    }

    /**
     * <p>
     * Helper method to remove all comments from the specified document.
     * </p>
     * 
     * @param document A target document.
     */
    public static void clearComment(XWPFDocument document) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            clearComment(paragraph);
        }
    }

    /**
     * <p>
     * Helper method to remove all comments from the specified paragraph.
     * </p>
     * 
     * @param paragraph A target paragraph.
     */
    public static void clearComment(XWPFParagraph paragraph) {
        if (paragraph != null) {
            CTP pContext = paragraph.getCTP();

            for (int i = pContext.sizeOfCommentRangeStartArray() - 1; 0 <= i; i--) {
                pContext.removeCommentRangeStart(i);
            }

            for (int i = pContext.sizeOfCommentRangeEndArray() - 1; 0 <= i; i--) {
                pContext.removeCommentRangeEnd(i);
            }

            for (XWPFRun run : paragraph.getRuns()) {
                CTR rContext = run.getCTR();

                for (int i = rContext.sizeOfCommentReferenceArray() - 1; 0 <= i; i--) {
                    rContext.removeCommentReference(i);
                }
            }
        }
    }

    /**
     * <p>
     * Helper method to remove all text from the specified paragraph.
     * </p>
     * 
     * @param paragraph A target paragraph.
     */
    public static void clearText(XWPFParagraph paragraph) {
        if (paragraph != null) {
            for (XWPFRun run : paragraph.getRuns()) {
                run.setText("", 0);
            }
        }
    }

    /**
     * <p>
     * Helper method to clone {@link XWPFTable}.
     * </p>
     * 
     * @param in
     * @param out
     * @param converter
     */
    public static void copy(XWPFTable in, XWPFTable out, UnaryOperator<String> converter) {
        // copy context
        CTTbl outCT = out.getCTTbl();

        CTTblPr outPR = outCT.getTblPr() != null ? outCT.getTblPr() : outCT.addNewTblPr();
        outPR.set(in.getCTTbl().getTblPr());

        // copy children
        for (XWPFTableRow inRow : in.getRows()) {
            copy(inRow, out.insertNewTableRow(out.getNumberOfRows()), converter);
        }
    }

    /**
     * <p>
     * Helper method to clone {@link XWPFTableRow}.
     * </p>
     * 
     * @param in
     * @param out
     * @param converter
     */
    public static void copy(XWPFTableRow in, XWPFTableRow out, UnaryOperator<String> converter) {
        // copy context
        CTRow outCT = out.getCtRow();
        CTTrPr outPR = outCT.isSetTrPr() ? outCT.getTrPr() : outCT.addNewTrPr();
        outPR.set(in.getCtRow().getTrPr());

        // copy children
        for (XWPFTableCell inCell : in.getTableCells()) {
            copy(inCell, out.createCell(), converter);
        }
    }

    /**
     * <p>
     * Helper method to clone {@link XWPFTableCell}.
     * </p>
     * 
     * @param in
     * @param out
     * @param converter
     */
    public static void copy(XWPFTableCell in, XWPFTableCell out, UnaryOperator<String> converter) {
        // copy context
        CTTc outCTTc = out.getCTTc();
        CTTcPr outPR = outCTTc.isSetTcPr() ? outCTTc.getTcPr() : outCTTc.addNewTcPr();
        outPR.set(in.getCTTc().getTcPr());

        // clear all elements from out cell
        for (int i = out.getParagraphs().size() - 1; 0 <= i; i--) {
            out.removeParagraph(i);
        }

        // copy children
        for (XWPFParagraph inPara : in.getParagraphs()) {
            copy(inPara, out.addParagraph(), converter);
        }
    }

    /**
     * <p>
     * Helper method to clone {@link XWPFFooter}.
     * </p>
     * 
     * @param in
     * @param out
     * @param converter
     */
    public static void copy(XWPFFooter in, XWPFFooter out, UnaryOperator<String> converter) {
        // copy context

        // copy children
        for (IBodyElement element : in.getBodyElements()) {
            if (element instanceof XWPFParagraph) {
                copy((XWPFParagraph) element, out.createParagraph(), converter);
            }
        }
    }

    /**
     * <p>
     * Helper method to clone {@link XWPFParagraph}.
     * </p>
     * 
     * @param in
     * @param out
     * @param converter
     */
    public static void copy(XWPFParagraph in, XWPFParagraph out, UnaryOperator<String> converter) {
        // copy context
        ppr(out).set(in.getCTP().getPPr());

        // copy children
        for (XWPFRun inRun : in.getRuns()) {
            copy(inRun, out.createRun(), converter);
        }
    }

    /**
     * <p>
     * Helper method to clone {@link XWPFRun}.
     * </p>
     * 
     * @param in
     * @param out
     * @param model
     */
    public static void copy(XWPFRun in, XWPFRun out, UnaryOperator<String> converter) {
        // copy
        out.setBold(in.isBold());
        out.setCapitalized(in.isCapitalized());
        out.setCharacterSpacing(in.getCharacterSpacing());
        out.setColor(in.getColor());
        out.setDoubleStrikethrough(in.isDoubleStrikeThrough());
        out.setEmbossed(in.isEmbossed());
        out.setFontFamily(in.getFontFamily());
        out.setFontSize(in.getFontSize());
        out.setImprinted(in.isImprinted());
        out.setItalic(in.isItalic());
        out.setKerning(in.getKerning());
        out.setShadow(in.isShadowed());
        out.setSmallCaps(in.isSmallCaps());
        out.setStrikeThrough(in.isStrikeThrough());
        out.setSubscript(in.getSubscript());
        out.setTextPosition(in.getTextPosition());
        out.setUnderline(in.getUnderline());

        // copy context
        CTR inCTR = in.getCTR();
        CTR outCTR = out.getCTR();
        CTRPr outPR = outCTR.isSetRPr() ? outCTR.getRPr() : outCTR.addNewRPr();
        outPR.set(inCTR.getRPr());

        // // copy tab
        // CTEmpty[] tabs = inCTR.getTabArray();
        //
        // if (tabs.length != 0) {
        // out.addTab();
        // }

        outCTR.setAnnotationRefArray(inCTR.getAnnotationRefArray());
        outCTR.setBrArray(inCTR.getBrArray());
        outCTR.setCommentReferenceArray(inCTR.getCommentReferenceArray());
        outCTR.setContinuationSeparatorArray(inCTR.getContinuationSeparatorArray());
        outCTR.setCrArray(inCTR.getCrArray());
        outCTR.setDelInstrTextArray(inCTR.getDelInstrTextArray());
        outCTR.setDrawingArray(inCTR.getDrawingArray());
        outCTR.setEndnoteRefArray(inCTR.getEndnoteRefArray());
        outCTR.setFldCharArray(inCTR.getFldCharArray());
        outCTR.setFootnoteRefArray(inCTR.getFootnoteRefArray());
        outCTR.setInstrTextArray(inCTR.getInstrTextArray());
        outCTR.setLastRenderedPageBreakArray(inCTR.getLastRenderedPageBreakArray());
        outCTR.setObjectArray(inCTR.getObjectArray());
        outCTR.setPictArray(inCTR.getPictArray());
        outCTR.setPtabArray(inCTR.getPtabArray());
        outCTR.setSymArray(inCTR.getSymArray());
        outCTR.setTabArray(inCTR.getTabArray());

        // copy text
        write(out, converter.apply(in.text()));
    }

    /**
     * <p>
     * Write the text.
     * </p>
     * 
     * @param run
     * @param text
     */
    public static void write(XWPFRun run, String text) {
        String[] lines = text.split("\n");

        if (lines.length != 0) {
            run.setText(lines[0], 0); // set first line into XWPFRun

            for (int i = 1; i < lines.length; i++) {
                run.addBreak();
                run.setText(lines[i]);
            }
        }
    }

    /**
     * <p>
     * Helper method to retrieve {@link CTPPr}.
     * </p>
     * 
     * @param paragraph
     */
    public static CTPPr ppr(XWPFParagraph paragraph) {
        CTP paraContext = paragraph.getCTP();

        return paraContext.isSetPPr() ? paraContext.getPPr() : paraContext.addNewPPr();
    }
}

/*
 * Copyright (C) 2022 The OFFISHELL Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.word;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.function.UnaryOperator;

import javax.xml.namespace.QName;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlip;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTEmpty;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkup;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPTab;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPicture;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSym;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation.Enum;

import kiss.I;
import kiss.Variable;

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

        // for table
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph para : cell.getParagraphs()) {
                        clearComment(para);
                    }
                }
            }
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

        // copy(doc, out.getDocument(), doc.getStyles().getStyle(in.getStyleID()));

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
        if (in.getFontSizeAsDouble() != null) out.setFontSize(in.getFontSizeAsDouble());
        out.setImprinted(in.isImprinted());
        out.setItalic(in.isItalic());
        out.setKerning(in.getKerning());
        out.setShadow(in.isShadowed());
        out.setSmallCaps(in.isSmallCaps());
        out.setStrikeThrough(in.isStrikeThrough());
        out.setVerticalAlignment(out.getVerticalAlignment().toString());
        out.setTextPosition(in.getTextPosition());
        out.setUnderline(in.getUnderline());

        // copy context
        CTR inCTR = in.getCTR();
        CTRPr inPR = inCTR.getRPr();
        CTR outCTR = out.getCTR();
        CTRPr outPR = outCTR.isSetRPr() ? outCTR.getRPr() : outCTR.addNewRPr();
        outPR.set(inCTR.getRPr());
        out.setVerticalAlignment(inPR == null || inPR.getVertAlignList().isEmpty() ? "baseline"
                : inPR.getVertAlignList().get(1).toString());

        // // copy tab
        // CTEmpty[] tabs = inCTR.getTabArray();
        //
        // if (tabs.length != 0) {
        // out.addTab();
        // }
        outCTR.setAnnotationRefArray(inCTR.getAnnotationRefList().toArray(CTEmpty[]::new));
        outCTR.setBrArray(inCTR.getBrList().toArray(CTBr[]::new));
        outCTR.setCommentReferenceArray(inCTR.getCommentReferenceList().toArray(CTMarkup[]::new));
        outCTR.setContinuationSeparatorArray(inCTR.getContinuationSeparatorList().toArray(CTEmpty[]::new));
        outCTR.setCrArray(inCTR.getCrList().toArray(CTEmpty[]::new));
        outCTR.setDelInstrTextArray(inCTR.getDelInstrTextList().toArray(CTText[]::new));
        outCTR.setDrawingArray(inCTR.getDrawingList().toArray(CTDrawing[]::new));
        outCTR.setEndnoteRefArray(inCTR.getEndnoteRefList().toArray(CTEmpty[]::new));
        outCTR.setFldCharArray(inCTR.getFldCharList().toArray(CTFldChar[]::new));
        outCTR.setFootnoteRefArray(inCTR.getFootnoteRefList().toArray(CTEmpty[]::new));
        outCTR.setInstrTextArray(inCTR.getInstrTextList().toArray(CTText[]::new));
        outCTR.setLastRenderedPageBreakArray(inCTR.getLastRenderedPageBreakList().toArray(CTEmpty[]::new));
        outCTR.setObjectArray(inCTR.getObjectList().toArray(CTObject[]::new));
        outCTR.setPictArray(inCTR.getPictList().toArray(CTPicture[]::new));
        outCTR.setPtabArray(inCTR.getPtabList().toArray(CTPTab[]::new));
        outCTR.setSymArray(inCTR.getSymList().toArray(CTSym[]::new));
        outCTR.setTabArray(inCTR.getTabList().toArray(CTEmpty[]::new));

        // copy image
        for (XWPFPicture inPicture : in.getEmbeddedPictures()) {
            try {
                XWPFPictureData inData = inPicture.getPictureData();
                String outId = out.getDocument().addPictureData(new ByteArrayInputStream(inData.getData()), inData.getPictureType());

                select(CTBlip.class, outCTR).to(blip -> blip.setEmbed(outId));
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }

        // copy text
        write(out, converter.apply(in.text()));
    }

    /**
     * <p>
     * Select node by XPath.
     * </p>
     * 
     * @param type
     * @param node
     * @return
     */
    private static <T extends XmlObject> Variable<T> select(Class<T> type, XmlObject node) {
        try {
            SchemaType schema = (SchemaType) type.getField("type").get(null);
            QName qName = schema.getName();
            String name = qName.getLocalPart().replace("CT_", "").toLowerCase();
            XmlObject[] nodes = node.selectPath("declare namespace ns='" + qName.getNamespaceURI() + "' .//ns:" + name);

            if (nodes.length == 0) {
                return Variable.empty();
            } else {
                return Variable.of((T) nodes[0]);
            }
        } catch (Exception e) {
            throw I.quiet(e);
        }
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
        text = WordStyle.apply(run, text);

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

    /**
     * <p>
     * Helper method to retrieve {@link CTSectPr}.
     * </p>
     * 
     * @param document
     */
    public static CTSectPr section(XWPFDocument document) {
        CTBody body = document.getDocument().getBody();

        return body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
    }

    public static void copyLayout(XWPFDocument srcDoc, XWPFDocument destDoc) {
        CTPageMar pgMar = srcDoc.getDocument().getBody().getSectPr().getPgMar();

        Object bottom = pgMar.getBottom();
        Object footer = pgMar.getFooter();
        Object gutter = pgMar.getGutter();
        Object header = pgMar.getHeader();
        Object left = pgMar.getLeft();
        Object right = pgMar.getRight();
        Object top = pgMar.getTop();

        CTPageMar addNewPgMar = destDoc.getDocument().getBody().addNewSectPr().addNewPgMar();

        addNewPgMar.setBottom(bottom);
        addNewPgMar.setFooter(footer);
        addNewPgMar.setGutter(gutter);
        addNewPgMar.setHeader(header);
        addNewPgMar.setLeft(left);
        addNewPgMar.setRight(right);
        addNewPgMar.setTop(top);

        CTPageSz pgSzSrc = srcDoc.getDocument().getBody().getSectPr().getPgSz();

        BigInteger code = pgSzSrc.getCode();
        Object h = pgSzSrc.getH();
        Enum orient = pgSzSrc.getOrient();
        Object w = pgSzSrc.getW();

        CTPageSz addNewPgSz = destDoc.getDocument().getBody().addNewSectPr().addNewPgSz();

        addNewPgSz.setCode(code);
        addNewPgSz.setH(h);
        addNewPgSz.setOrient(orient);
        addNewPgSz.setW(w);
    }
}
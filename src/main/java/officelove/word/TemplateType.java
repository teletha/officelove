/*
 * Copyright (C) 2024 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.word;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

public class TemplateType {

    public final int width;

    public final int height;

    public final String name;

    public final String direction;

    /**
     * Define template infomation.
     * 
     * @param name
     * @param width A width in portrait.
     * @param height A height in portrait.
     * @param direction
     */
    public TemplateType(String name, int width, int height, String direction) {
        this.width = width;
        this.height = height;
        this.name = name;
        this.direction = direction;
    }

    /**
     * Detect infomation from document.
     * 
     * @param document
     * @return
     */
    public static TemplateType parse(XWPFDocument document) {
        CTDocument1 doc1 = document.getDocument();
        CTBody body = doc1.getBody();
        CTSectPr section = (body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr());

        CTPageSz pageSize = (section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz());
        int width = Math.round(((BigInteger) pageSize.getW()).floatValue() / 20f);
        int height = Math.round(((BigInteger) pageSize.getH()).floatValue() / 20f);
        String kind = "Unknown";
        String direction = "Portrait";

        for (Map.Entry<String, int[]> entry : types.entrySet()) {
            String name = entry.getKey();
            int[] dimensions = entry.getValue();

            if (dimensions[0] == width && dimensions[1] == height) {
                kind = name;
                direction = "Portrait";
                break;
            } else if (dimensions[1] == width && dimensions[0] == height) {
                kind = name;
                direction = "Landscape";
                break;
            }
        }

        return new TemplateType(kind, width, height, direction);
    }

    /** The build-in types. */
    private static final Map<String, int[]> types = new HashMap<>();

    static {
        define("A1", 1685, 2384);
        define("A2", 1191, 1685);
        define("A3", 842, 1191);
        define("A4", 595, 842);
        define("A5", 420, 595);

        define("B4_ISO", 709, 1001);
        define("B5_ISO", 499, 709);

        define("B4_JIS", 729, 1032);
        define("B5_JIS", 516, 729);
    }

    /**
     * Define the template type.
     * 
     * @param name
     * @param width A width in portrait.
     * @param height A height in portrait.
     */
    public static void define(String name, int width, int height) {
        types.put(name, new int[] {width, height});
    }
}
/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.mail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import kiss.I;
import kiss.Signal;
import offishell.UI;
import offishell.excel.Excel;
import offishell.file.FileName;
import offishell.word.Word;

/**
 * import offishell.files.FileName; * @version 2016/07/11 16:13:07
 */
public class Mail {

    /** The send target. */
    private String to;

    /** The message subject. */
    private String subject;

    /** The message body. */
    private String body;

    /** The attachments. */
    private List<Path> files = new ArrayList();

    /**
     * <p>
     * Set message subject.
     * </p>
     * 
     * @param subject
     * @return
     */
    public Mail subject(Object subject) {
        this.subject = String.valueOf(subject);

        return this;
    }

    /**
     * <p>
     * Set message body.
     * </p>
     * 
     * @param body
     * @return
     */
    public Mail body(String body) {
        this.body = body;

        return this;
    }

    /**
     * <p>
     * Set message body.
     * </p>
     * 
     * @param body
     * @return
     */
    public Mail body(Consumer<Body> writer) {
        Body template = new Body();
        writer.accept(template);

        this.body = template.text.toString();

        return this;
    }

    /**
     * <p>
     * Set attachments.
     * </p>
     * 
     * @param files
     * @return
     */
    public Mail attachment(Path file) {
        if (file != null && Files.exists(file)) {
            this.files.add(file);
        }
        return this;
    }

    /**
     * <p>
     * Set attachments.
     * </p>
     * 
     * @param files
     * @return
     */
    public Mail attachment(Signal<Path> files) {
        return attachment(files.toList());
    }

    /**
     * <p>
     * Set attachments.
     * </p>
     * 
     * @param files
     * @return
     */
    public Mail attachment(List<Path> files) {
        for (Path file : files) {
            attachment(file);
        }
        return this;
    }

    /**
     * <p>
     * Set attachments.
     * </p>
     * 
     * @param files
     * @return
     */
    public Mail attachment(Word file) {
        return attachment(file, subject);
    }

    /**
     * <p>
     * Set attachments.
     * </p>
     * 
     * @param files
     * @return
     */
    public Mail attachment(Word file, String name) {
        if (name == null || name.isEmpty()) {
            name = subject;
        }

        if (!name.endsWith(".docx") && !name.endsWith(".docm")) {
            name += ".docx";
        }

        try {
            Path temp = Files.createDirectories(I.locateTemporary());
            Path created = Files.createFile(temp.resolve(name));

            file.save(created);

            return attachment(created);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Set attachments.
     * </p>
     * 
     * @param files
     * @return
     */
    public Mail attachment(Excel file) {
        return attachment(file, subject);
    }

    /**
     * <p>
     * Set attachments.
     * </p>
     * 
     * @param files
     * @return
     */
    public Mail attachment(Excel file, String name) {
        if (name == null || name.isEmpty()) {
            name = subject;
        }

        if (!name.endsWith(".xlsx") && !name.endsWith(".xlsm")) {
            name += ".xlsx";
        }

        try {
            Path temp = Files.createDirectories(I.locateTemporary());
            Path created = Files.createFile(temp.resolve(name));
            I.copy(file.path, created);

            return attachment(created);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Write mail actually.
     * </p>
     */
    public void write() {
        I.make(Mailer.class).invoke(this);
    }

    /**
     * <p>
     * Mail writer DSL.
     * </p>
     * 
     * @param address
     * @return
     */
    public static Mail to(String address) {
        Mail mail = new Mail();
        mail.to = address;

        return mail;
    }

    /**
     * @version 2016/07/11 16:09:19
     */
    private static class Mailer {

        /** The thunderbird.exe. */
        public Path exe;

        /**
         * <p>
         * Invoke mailer application.
         * </p>
         */
        private void invoke(Mail mail) {
            if (exe == null || Files.notExists(exe)) {
                exe = UI.selectFile(new FileName("thunderbird.exe"));
            }

            StringBuilder option = new StringBuilder();

            if (mail.to != null) {
                option.append("to='").append(mail.to).append("',");
            }

            if (mail.subject != null) {
                option.append("subject='").append(mail.subject).append("',");
            }

            if (mail.body != null) {
                option.append("body='").append(mail.body).append("',");
            }

            if (mail.files != null) {
                option.append("attachment='").append(I.join(",", mail.files)).append("',");
            }

            ProcessBuilder builder = new ProcessBuilder(exe.toString(), "-compose", option.toString());

            try {
                Process process = builder.start();
                process.waitFor();
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * @version 2016/07/11 17:37:12
     */
    public static class Body {

        /** The actual text. */
        private StringBuilder text = new StringBuilder();

        /**
         * <p>
         * Write single line message.
         * </p>
         * 
         * @param line
         */
        public void write(Object... line) {
            for (Object object : line) {
                text.append(object);
            }
            text.append("\r\n");
        }

        /**
         * <p>
         * Write single line message.
         * </p>
         * 
         * @param line
         */
        public void writeLine() {
            text.append("\r\n");
        }
    }
}

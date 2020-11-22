package ru.v6.mark.prototype.service;


import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.v6.mark.xsd.SferaConnectorReject;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FileService {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${config.FILE_CONTENT_ROOT}")
    String FILE_CONTENT_ROOT;

    @Value("${config.inputFileDir}")
    private String inputFileDir;

    @Value("${config.saveFileDir}")
    private String saveFileDir;

    @Value("${config.errorFileDir}")
    private String errorFileDir;

    @Value("${config.outputFileDir}")
    private String outputFileDir;

    private static JAXBContext context;
    private static Marshaller jaxbMarshaller;

    static {
        try {
            context = JAXBContext.newInstance("ru.v6.mark.xsd");
            jaxbMarshaller = context.createMarshaller();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public List<File> loadFiles() {
        final File folder = new File(inputFileDir);
        List<File> files = new ArrayList<>();
        if (folder.listFiles() != null) {
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    loadFiles();
                } else {
                    files.add(fileEntry);
                }
            }
        }
        return files;
    }

    public String transferFile(File file, boolean isWork) {
        String result = "Файл: " + file.getName();

        String dir = isWork ? saveFileDir : errorFileDir;
        try {
            Files.move(file.toPath(),
                    (new File(dir + file.getName())).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            result = "Статус: " + (isWork ? "ОБРАБОТАН\n" : "ОШИБКА\n") + result;
        } catch (IOException e) {
            logger.error("Error transferFile", e);
            result = "Статус: " + "ОШИБКА, " + result;
        }
        return result;
    }

    public String readBakFile(String fileName) {
        String result = null;
        try (InputStream in = Files.newInputStream(Paths.get(saveFileDir, fileName), StandardOpenOption.READ)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtils.copy(in, outputStream);
            outputStream.flush();
            result = outputStream.toString("windows-1251");
            if (!result.contains("encoding=\"windows-1251\"") && !result.contains("encoding=\"WINDOWS-1251\"")) {
                result = outputStream.toString("UTF-8");
            }
        } catch (Exception e) {
            result = "Ошибка получения файла.";
        }
        return result;
    }

    public String readBakFile(FilenameFilter filter) {
        String result = null;

        File dir = new File(saveFileDir);
        File[] files = dir.listFiles(filter);

        if (files != null && files.length > 0) {
            try (InputStream in = Files.newInputStream(Paths.get(files[0].toURI()), StandardOpenOption.READ)) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                IOUtils.copy(in, outputStream);
                outputStream.flush();
                result = outputStream.toString("UTF-8");
            } catch (Exception e) {
                result = "Ошибка получения файла.";
            }
        } else {
            result = "Файл не найден.";
        }
        return result;
    }

    public String read(String[] pathArr) {
        String result = null;
        Path path = getPath(pathArr);

        try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
            org.apache.commons.io.output.ByteArrayOutputStream file = new org.apache.commons.io.output.ByteArrayOutputStream();
            IOUtils.copy(in, file);
            file.flush();
            result = file.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            logger.error("protocol content reading error:" + path.toString(), e);
            result = "Данные не найдены";
        }
        return result;
    }

    public void save(String fileContentXml, String[] pathArr) {
        Path path = getPath(pathArr);
        Path dirpath = path.getParent();
        try {
            if (!Files.exists(dirpath)) {
                Files.createDirectories(dirpath);
            }
        } catch (IOException e) {
            logger.error("protocol content saving error:" + dirpath.toString(), e);
        }
        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE)) {
            ByteArrayInputStream file = new ByteArrayInputStream(fileContentXml.getBytes(StandardCharsets.UTF_8));
            IOUtils.copy(file, out);
            out.flush();
        } catch (IOException e) {
            logger.error("protocol content saving error:" + dirpath.toString(), e);
        }
    }

    private Path getPath(String[] path) {
        return getPath(path[path.length - 1], Arrays.copyOfRange(path, 0, path.length - 1));
    }

    private Path getPath(String fileName, String[] subdirs) {
        String[] parts = new String[subdirs.length + 1];
        System.arraycopy(subdirs, 0, parts, 0, subdirs.length);
        parts[subdirs.length] = fileName;// + ".xml";
        return Paths.get(FILE_CONTENT_ROOT, parts);
    }

    public void generateAperak(SferaConnectorReject sferaConnectorReject, String aperakFileName) {
        Path path = Paths.get(outputFileDir, aperakFileName);
        Path bakPath = Paths.get(saveFileDir, aperakFileName);

        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE);
             OutputStream bak = Files.newOutputStream(bakPath, StandardOpenOption.CREATE);) {
            try {
                byte[] file = objToByte(sferaConnectorReject);
                IOUtils.copy(new ByteArrayInputStream(file), out);
                IOUtils.copy(new ByteArrayInputStream(file), bak);
            } catch (JAXBException e) {
                logger.error("Error: ", e);
            }
            out.flush();
            bak.flush();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

    private static byte[] objToByte(SferaConnectorReject sferaConnectorReject) throws JAXBException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(sferaConnectorReject, byteStream);
        return byteStream.toByteArray();
    }
}

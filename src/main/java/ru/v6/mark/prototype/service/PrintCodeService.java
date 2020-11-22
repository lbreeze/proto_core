package ru.v6.mark.prototype.service;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.Utilities;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.criteria.GtinCriteria;
import ru.v6.mark.prototype.domain.entity.Gtin;
import ru.v6.mark.prototype.domain.entity.KIZMark;
import ru.v6.mark.prototype.domain.entity.KIZPosition;
import ru.v6.mark.prototype.exception.ApplicationException;
import ru.v6.mark.prototype.service.util.DocumentPdfCoords;
import uk.org.okapibarcode.backend.DataMatrix;
import uk.org.okapibarcode.backend.Symbol;
import uk.org.okapibarcode.output.Java2DRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PrintCodeService {

    @Autowired
    GtinService gtinService;

    private static String PRINT_FORM = "km_template.pdf";

    private float widthImage = 22;
    private float heightImage = 22;
    private float widthLabel = 70;
    private float heightLabel = 37;

    private static final int MAX_MARKS_TO_PRINT = 1000;

    private String sNumberLabel = "Серийный номер:";

    private static BaseFont baseFont;

    static {
        try {
            baseFont = BaseFont.createFont(
                    "FreeSans.ttf",
                    "CP1251",
                    BaseFont.EMBEDDED);
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }
    }

    public void getPdf(OutputStream outputStream, List<KIZPosition> positions
    ) throws IOException, DocumentException {

        AtomicInteger totalMarks = new AtomicInteger(0);
        positions.parallelStream().forEach(kizPosition -> {
            totalMarks.addAndGet(kizPosition.getQuantity());
        });

        if (totalMarks.get() > MAX_MARKS_TO_PRINT) {
            throw ApplicationException.build("Превышено допустимое кол-во марок для печати: {0}/{1}").parameters(totalMarks.get(), MAX_MARKS_TO_PRINT);
        }

        PdfReader reader = new PdfReader(getClass().getClassLoader().getResourceAsStream(PRINT_FORM));
        PdfStamper stamper = new PdfStamper(reader, outputStream);
        PdfWriter writer = stamper.getWriter();

        int sizePage = 24;
        int pageIndex = 0;

        for (KIZPosition position : positions) {
            List<BufferedImage> bImages = new ArrayList<>();
            List<String> sNumbers = new ArrayList<>();
            String type = "";
            String imported = "";

            Gtin gtin = getGtin(position.getEan());
            if (gtin != null && gtin.getMarkSubType() != null) {
                type = gtin.getMarkSubType().getDescription();
                imported = gtin.getImported() ? "Ввезено в РФ" : "Произведено в РФ";
            }
            String[] data = new String[]{type, imported};

            for (KIZMark mark : position.getMarks()) {
                BufferedImage bImage = getImage(mark.getMark(), 9);
                sNumbers.add(mark.getMark().substring(18,31));
                bImages.add(bImage);
            }

            Long div = Long.divideUnsigned(bImages.size(), sizePage);
            if (bImages.size() % sizePage > 0)
                div++;
            int startIndex = 0;
            int endIndex = bImages.size() < sizePage ? bImages.size() : sizePage;
            PdfImportedPage page = stamper.getImportedPage(reader, 1);

            for (int i = 0; i < div; i++) {
                if (endIndex > bImages.size()) {
                    endIndex = bImages.size();
                }

                pageIndex++;
                if (startIndex > 0 || pageIndex > 1) {
                    stamper.insertPage(pageIndex, reader.getPageSize(1));
                    printer(position, bImages.subList(startIndex, endIndex), sNumbers.subList(startIndex, endIndex), stamper, reader, writer, pageIndex, data);
                    stamper.getUnderContent(pageIndex).addTemplate(page, 0, 0);
                } else {
                    printer(position, bImages.subList(startIndex, endIndex), sNumbers.subList(startIndex, endIndex), stamper, reader, writer, pageIndex, data);
                }
                startIndex = startIndex + sizePage;
                endIndex = endIndex + sizePage;
            }
        }
        stamper.close();
        reader.close();
    }

    private void printer(KIZPosition position, List<BufferedImage> bImages, List<String> sNumbers, PdfStamper stamper, PdfReader reader, PdfWriter writer, int pageIndex, String[] data) throws IOException, DocumentException {
        int indexPosition = 0;
        int yPositionImage = 734;
        int[][] grid = new int[8][3];
        StringBuilder sb;

        PdfFormField formField = PdfFormField.createEmpty(writer);
        formField.setFieldName("Form" + pageIndex);

        for (int[] row : grid) {
            int xPositionImage = 0;
            for (int value : row) {
                if (bImages.size() <= indexPosition || bImages.get(indexPosition) == null) {
                    break;
                }
                if (indexPosition < position.getMarks().size()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bImages.get(indexPosition), "png", baos);
                    baos.flush();
                    Image image = Image.getInstance(baos.toByteArray());
                    baos.close();
                    PdfContentByte content = stamper.getOverContent(reader.getNumberOfPages());
                    content.rectangle(xPositionImage, yPositionImage, Utilities.millimetersToPoints(widthLabel), Utilities.millimetersToPoints(heightLabel));
                    content.stroke();
                    image.setAbsolutePosition(xPositionImage + 14, yPositionImage + 21);
                    image.scaleAbsolute(Utilities.millimetersToPoints(widthImage), Utilities.millimetersToPoints(heightImage));
                    content.addImage(image);
                    //
                    sb = new StringBuilder();
                    sb.append(data[0]);
                    DocumentPdfCoords typeValueParam = new DocumentPdfCoords(xPositionImage + 95, yPositionImage - 20, xPositionImage + 175, yPositionImage + 185, String.valueOf(Math.random()));
                    TextField typeValueTextField = getTextField(writer, typeValueParam, 10, true, false);
                    typeValueTextField.setText(sb.toString());
                    formField.addKid(typeValueTextField.getTextField());
                    //
                    sb = new StringBuilder();
                    sb.append(data[1]);
                    DocumentPdfCoords importValueParam = new DocumentPdfCoords(xPositionImage + 90, yPositionImage - 70, xPositionImage + 175, yPositionImage + 185, String.valueOf(Math.random()));
                    TextField importValueTextField = getTextField(writer, importValueParam, 9, true, false);
                    importValueTextField.setText(sb.toString());
                    formField.addKid(importValueTextField.getTextField());
                    //
                    sb = new StringBuilder();
                    sb.append(sNumberLabel);
                    DocumentPdfCoords sNumberLabelParam = new DocumentPdfCoords(xPositionImage + 90, yPositionImage - 115, xPositionImage + 175, yPositionImage + 185, String.valueOf(Math.random()));
                    TextField sNumberLabelTextField = getTextField(writer, sNumberLabelParam, 10, true, false);
                    sNumberLabelTextField.setText(sb.toString());
                    formField.addKid(sNumberLabelTextField.getTextField());
                    //
                    sb = new StringBuilder();
                    sb.append(sNumbers.get(indexPosition));
                    DocumentPdfCoords sNumberValueParam = new DocumentPdfCoords(xPositionImage + 95, yPositionImage - 145, xPositionImage + 175, yPositionImage + 185, String.valueOf(Math.random()));
                    TextField sNumberValueTextField = getTextField(writer, sNumberValueParam, 10, true, false);
                    sNumberValueTextField.setText(sb.toString());
                    formField.addKid(sNumberValueTextField.getTextField());
                }
                xPositionImage = xPositionImage + (int) Utilities.millimetersToPoints(widthLabel);
                indexPosition++;
            }
            yPositionImage = yPositionImage - (int) Utilities.millimetersToPoints(heightLabel);
        }
        stamper.addAnnotation(formField, pageIndex);
    }

    private TextField getTextField(PdfWriter writer, DocumentPdfCoords coords, int fontSize, boolean readOnly, boolean multiLine) throws IOException, DocumentException {
        return getTextField(writer, new Rectangle(coords.getLlx(), coords.getLly(), coords.getUrx(), coords.getUry()), coords.getDescription(), fontSize, readOnly, multiLine);
    }

    private TextField getTextField(PdfWriter writer, Rectangle rectangle, String description, int fontSize, boolean readOnly, boolean multiLine) throws IOException, DocumentException {
        TextField textField = new TextField(writer, rectangle, description + rectangle.getLeft() + rectangle.getRight());
        textField.setFontSize(fontSize);
        textField.setFont(baseFont);
        if (readOnly && multiLine) {
            textField.setOptions(TextField.MULTILINE | TextField.READ_ONLY);
        } else if (readOnly) {
            textField.setOptions(TextField.READ_ONLY);
        } else if (multiLine) {
            textField.setOptions(TextField.MULTILINE);
        }
        textField.setText(description);
        return textField;
    }

    public BufferedImage getImage(String code, int magnification) {
        DataMatrix dataMatrix = new DataMatrix();
        // We need a GS1 DataMatrix barcode.
        dataMatrix.setDataType(Symbol.DataType.GS1);
        // 0 means size will be set automatically according to amount of data (smallest possible).
        dataMatrix.setPreferredSize(0);
        // Don't want no funky rectangle shapes, if we can avoid it.
        dataMatrix.setForceMode(DataMatrix.ForceMode.SQUARE);

        dataMatrix.setPreferredSize(0);
        dataMatrix.setReaderInit(false);

        dataMatrix.setModuleWidth(4);
        dataMatrix.setQuietZoneHorizontal(5);
        dataMatrix.setQuietZoneVertical(5);


        dataMatrix.setContent(buildGS1(code));
        return getMagnifiedBarcode(dataMatrix, magnification);
    }

    private String buildGS1(String code) {
        StringBuilder result = new StringBuilder();
        result.
                append("[01]").
                append(code.substring(2, 16)).
                append("[21]").
                append(code.substring(18, 31)).
                append("[91]").
                append(code.substring(34, 38)).
                append("[92]").
                append(code.substring(41, 129));
        return result.toString();
    }

    private BufferedImage getMagnifiedBarcode(Symbol symbol, int magnification) {
        int borderSize = 0 * magnification;
        // Make DataMatrix object into bitmap
        BufferedImage image = new BufferedImage((symbol.getWidth() * magnification) + (2 * borderSize),
                (symbol.getHeight() * magnification) + (2 * borderSize),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, (symbol.getWidth() * magnification) + (2 * borderSize),
                (symbol.getHeight() * magnification) + (2 * borderSize));
        Java2DRenderer renderer = new Java2DRenderer(g2d, magnification /*BORDER_SIZE*/, Color.WHITE, Color.BLACK);
        renderer.render(symbol);
        return image;
    }

    private Gtin getGtin(String ean) {
        GtinCriteria criteria = new GtinCriteria();
        criteria.setEan(ean);
        List<Gtin> gtins = gtinService.findByCriteria(criteria);
        return gtins != null && gtins.size()> 0 ? gtins.get(0) : null;
    }

}

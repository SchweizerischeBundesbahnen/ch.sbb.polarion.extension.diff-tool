package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.conversion.PrintOrientation;
import ch.sbb.polarion.extension.diff_tool.rest.model.conversion.PrintPaperSize;
import ch.sbb.polarion.extension.pdf_exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;

public class HtmlToPdfConverterService {
    static final PrintOrientation DEFAULT_ORIENTATION = PrintOrientation.LANDSCAPE;
    static final PrintPaperSize DEFAULT_PAPER_SIZE = PrintPaperSize.A4;

    @SuppressWarnings("squid:S1166") // Initial exception swallowed intentionally
    public byte[] convertHtmlToPdf(String html, PrintOrientation printOrientation, PrintPaperSize printPaperSize) {
        try {
            return new HtmlToPdfConverter().convert(html, conversionParams(printOrientation, printPaperSize));
        } catch (NoClassDefFoundError e) {
            throw new UserFriendlyRuntimeException("No ch.sbb.polarion.extension.pdf-exporter extension found.");
        }
    }

    /** A parameter which is not given takes its documented default: landscape, A4. */
    static ConversionParams conversionParams(PrintOrientation printOrientation, PrintPaperSize printPaperSize) {
        PrintOrientation orientation = printOrientation != null ? printOrientation : DEFAULT_ORIENTATION;
        PrintPaperSize paperSize = printPaperSize != null ? printPaperSize : DEFAULT_PAPER_SIZE;
        return ConversionParams.builder()
                .orientation(Orientation.valueOf(orientation.name()))
                .paperSize(PaperSize.valueOf(paperSize.name()))
                .build();
    }
}

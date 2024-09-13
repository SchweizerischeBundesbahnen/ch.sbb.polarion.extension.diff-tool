package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.conversion.PrintOrientation;
import ch.sbb.polarion.extension.diff_tool.rest.model.conversion.PrintPaperSize;
import ch.sbb.polarion.extension.pdf.exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.PaperSize;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;

public class HtmlToPdfConverterService {
    @SuppressWarnings("squid:S1166") // Initial exception swallowed intentionally
    public byte[] convertHtmlToPdf(String html, PrintOrientation printOrientation, PrintPaperSize printPaperSize) {
        try {
            return new HtmlToPdfConverter().convert(html, Orientation.valueOf(printOrientation.name()), PaperSize.valueOf(printPaperSize.name()));
        } catch (NoClassDefFoundError e) {
            throw new UserFriendlyRuntimeException("No ch.sbb.polarion.extension.pdf-exporter extension found.");
        }
    }
}

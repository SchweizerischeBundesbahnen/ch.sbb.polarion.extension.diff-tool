package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.conversion.PrintOrientation;
import ch.sbb.polarion.extension.diff_tool.rest.model.conversion.PrintPaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlToPdfConverterServiceTest {

    @Test
    void conversionParamsTakeGivenValues() {
        ConversionParams params = HtmlToPdfConverterService.conversionParams(PrintOrientation.PORTRAIT, PrintPaperSize.A3);

        assertEquals(Orientation.PORTRAIT, params.getOrientation());
        assertEquals(PaperSize.A3, params.getPaperSize());
    }

    @Test
    void conversionParamsDefaultToLandscapeA4() {
        ConversionParams params = HtmlToPdfConverterService.conversionParams(null, null);

        assertEquals(Orientation.LANDSCAPE, params.getOrientation());
        assertEquals(PaperSize.A4, params.getPaperSize());
    }
}

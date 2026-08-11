package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.generic.GenericUiServlet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiffToolAppUIServletTest {

    @Test
    void instantiatesAsGenericUiServlet() {
        DiffToolAppUIServlet servlet = new DiffToolAppUIServlet();

        assertThat(servlet).isInstanceOf(GenericUiServlet.class);
    }
}

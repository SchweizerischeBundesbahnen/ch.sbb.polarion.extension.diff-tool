import React from 'react'
import ControlPane from "@/components/ControlPane";
import * as DiffTypes from "@/DiffTypes";

describe('<ControlPane />', () => {

  function commonAssertions() {
    cy.get('h1').should('be.visible').should('have.text', 'Export to PDF');

    cy.get('label[for="paper-size"]').should('be.visible').should('have.text', 'Paper size:');
    cy.get('select#paper-size').should('be.visible');
    cy.get('select#paper-size option').then(options => {
      const expected = ['A4', 'A3'];
      const values = [...options].map(o => o.value);
      cy.expect(values).to.deep.eq(expected);

      const textContents = [...options].map(o => o.textContent);
      cy.expect(textContents).to.deep.eq(expected);
    });

    cy.get('label[for="orientation"]').should('be.visible').should('have.text', 'Orientation:');
    cy.get('select#orientation').should('be.visible');
    cy.get('select#orientation option').then(options => {
      const values = [...options].map(o => o.value);
      cy.expect(values).to.deep.eq(['landscape', 'portrait']);

      const textContents = [...options].map(o => o.textContent);
      cy.expect(textContents).to.deep.eq(['Landscape', 'Portrait']);
    });

    cy.get('.export-controls button').should('be.visible').should('have.text', 'Export');
  }

  beforeEach(() => {
    cy.viewport(400, 600)
  })

  it('renders not accessible', () => {
    cy.mountWithMockedEnv(<ControlPane />, {});
    cy.get('svg[data-prefix="fas"][data-icon="angles-right"]').should('not.exist');
  })

  it('renders accessible', () => {
    cy.mountWithMockedEnv(<ControlPane />, { appContext: { state: { controlPaneAccessible: true }} });
    cy.get('svg[data-prefix="fas"][data-icon="angles-right"]').should('be.visible');
  })

  it('renders for collections', () => {
    cy.mountWithMockedEnv(<ControlPane diff_type={DiffTypes.COLLECTIONS_DIFF} />, { });

    cy.get('label[for="source-document"]').should('be.visible').should('have.text', 'Source documents:');
    cy.get('select#source-document').should('be.visible');

    cy.get('label[for="target-type"]').should('be.visible').should('have.text', 'Compare as:');
    cy.get('select#target-type').should('be.visible');
    cy.get('select#target-type option').then(options => {
      const expected = ['Workitems', 'Fields', 'Content'];
      const values = [...options].map(o => o.value);
      cy.expect(values).to.deep.eq(expected);

      const textContents = [...options].map(o => o.textContent);
      cy.expect(textContents).to.deep.eq(expected);
    });

    cy.get('label[for="configuration"]').should('be.visible').should('have.text', 'Configuration:');
    cy.get('select#configuration').should('be.visible');

    cy.get('label[for="outline-numbers"]').should('be.visible').should('have.text', 'Show difference in outline numbers');
    cy.get('input#outline-numbers').should('be.visible').should('not.have.attr', 'checked');

    cy.get('label[for="counterparts-differ"]').should('be.visible').should('have.text', 'Counterpart WorkItems differ');
    cy.get('input#counterparts-differ').should('be.visible').should('not.have.attr', 'checked');

    cy.get('label[for="compare-enums-by-id"]').should('be.visible').should('have.text', 'Compare enums by ID');
    cy.get('input#compare-enums-by-id').should('be.visible').should('not.have.attr', 'checked');

    cy.get('label[for="allow-reference-wi-merge"]').should('be.visible').should('have.text', 'Allow reference work item merge');
    cy.get('input#allow-reference-wi-merge').should('be.visible').should('not.have.attr', 'checked');

    cy.get('label[for="hide-chapters"]').should('be.visible').should('have.text', 'Hide chapters if no difference');
    cy.get('input#hide-chapters').should('be.visible').should('have.attr', 'checked');

    commonAssertions();
  })

  it('renders for documents content', () => {
    cy.mountWithMockedEnv(<ControlPane diff_type={DiffTypes.DOCUMENTS_CONTENT_DIFF} />, { });

    cy.get('label[for="target-type"]').should('be.visible').should('have.text', 'Compare as:');
    cy.get('select#target-type').should('be.visible');
    cy.get('select#target-type option').then(options => {
      const expected = ['Workitems', 'Fields', 'Content'];
      const values = [...options].map(o => o.value);
      cy.expect(values).to.deep.eq(expected);

      const textContents = [...options].map(o => o.textContent);
      cy.expect(textContents).to.deep.eq(expected);
    });

    cy.get('label[for="hide-chapters"]').should('be.visible').should('have.text', 'Hide chapters if no difference');
    cy.get('input#hide-chapters').should('be.visible').should('have.attr', 'checked');

    commonAssertions();
  })

  it('renders for documents fields', () => {
    cy.mountWithMockedEnv(<ControlPane diff_type={DiffTypes.DOCUMENTS_FIELDS_DIFF} />, { });

    cy.get('label[for="target-type"]').should('be.visible').should('have.text', 'Compare as:');
    cy.get('select#target-type').should('be.visible');
    cy.get('select#target-type option').then(options => {
      const expected = ['Workitems', 'Fields', 'Content'];
      const values = [...options].map(o => o.value);
      cy.expect(values).to.deep.eq(expected);

      const textContents = [...options].map(o => o.textContent);
      cy.expect(textContents).to.deep.eq(expected);
    });

    cy.get('label[for="compare-only-mutual-fields"]').should('be.visible').should('have.text', 'Compare only mutual fields');
    cy.get('input#compare-only-mutual-fields').should('be.visible').should('have.attr', 'checked');

    cy.get('label[for="compare-enums-by-id"]').should('be.visible').should('have.text', 'Compare enums by ID');
    cy.get('input#compare-enums-by-id').should('be.visible').should('not.have.attr', 'checked');

    commonAssertions();
  })

  it('renders for documents', () => {
    cy.mountWithMockedEnv(<ControlPane diff_type={DiffTypes.DOCUMENTS_DIFF} />, { });

    cy.get('label[for="target-type"]').should('be.visible').should('have.text', 'Compare as:');
    cy.get('select#target-type').should('be.visible');
    cy.get('select#target-type option').then(options => {
      const expected = ['Workitems', 'Fields', 'Content'];
      const values = [...options].map(o => o.value);
      cy.expect(values).to.deep.eq(expected);

      const textContents = [...options].map(o => o.textContent);
      cy.expect(textContents).to.deep.eq(expected);
    });

    cy.get('label[for="configuration"]').should('be.visible').should('have.text', 'Configuration:');
    cy.get('select#configuration').should('be.visible');

    cy.get('label[for="outline-numbers"]').should('be.visible').should('have.text', 'Show difference in outline numbers');
    cy.get('input#outline-numbers').should('be.visible').should('not.have.attr', 'checked');

    cy.get('label[for="counterparts-differ"]').should('be.visible').should('have.text', 'Counterpart WorkItems differ');
    cy.get('input#counterparts-differ').should('be.visible').should('not.have.attr', 'checked');

    cy.get('label[for="compare-enums-by-id"]').should('be.visible').should('have.text', 'Compare enums by ID');
    cy.get('input#compare-enums-by-id').should('be.visible').should('not.have.attr', 'checked');

    cy.get('label[for="allow-reference-wi-merge"]').should('be.visible').should('have.text', 'Allow reference work item merge');
    cy.get('input#allow-reference-wi-merge').should('be.visible').should('not.have.attr', 'checked');

    cy.get('label[for="hide-chapters"]').should('be.visible').should('have.text', 'Hide chapters if no difference');
    cy.get('input#hide-chapters').should('be.visible').should('have.attr', 'checked');

    commonAssertions();
  })

  it('renders for work items', () => {
    cy.mountWithMockedEnv(<ControlPane diff_type={DiffTypes.WORK_ITEMS_DIFF} />, { });

    cy.get('label[for="configuration"]').should('be.visible').should('have.text', 'Configuration:');
    cy.get('select#configuration').should('be.visible');

    cy.get('label[for="counterparts-differ"]').should('be.visible').should('have.text', 'Counterpart WorkItems differ');
    cy.get('input#counterparts-differ').should('be.visible').should('not.have.attr', 'checked');

    cy.get('label[for="compare-enums-by-id"]').should('be.visible').should('have.text', 'Compare enums by ID');
    cy.get('input#compare-enums-by-id').should('be.visible').should('not.have.attr', 'checked');

    commonAssertions();
  })

  it('handles compare-as change', () => {
    cy.mountWithMockedEnv(<ControlPane diff_type={DiffTypes.DOCUMENTS_DIFF} />, { });

    cy.get('select#target-type').select("Fields").then(() => {
      cy.get('@router:push').should("be.calledWithMatch", 'compareAs=Fields');
    });
  })

  it('fetches, renders configurations, preselects default one and handles configuration change', () => {
    cy.intercept('**/settings/diff/names?scope=project/sourceProjectId/', { fixture: "configs" }).as('getConfigurations');

    cy.mountWithMockedEnv(<ControlPane diff_type={DiffTypes.DOCUMENTS_DIFF} />, { searchParams: { sourceProjectId: "sourceProjectId" } });

    cy.wait('@getConfigurations');

    cy.get('select#configuration').should('be.visible');
    cy.get('select#configuration option').then(options => {
      const expected = ['aTest', 'Default', 'E-Library'];
      const values = [...options].map(o => o.value);
      cy.expect(values).to.deep.eq(expected);

      const textContents = [...options].map(o => o.textContent);
      cy.expect(textContents).to.deep.eq(expected);
    });
    cy.get('select#configuration').should('have.value', 'Default');

    cy.get('select#configuration').select("E-Library").then(() => {
      cy.get('@router:push').should("be.calledWithMatch", 'config=E-Library');
    });
  })

  it('fetches, renders configurations and preselects specified one', () => {
    cy.intercept('**/settings/diff/names?scope=project/sourceProjectId/', { fixture: "configs" }).as('getConfigurations');

    cy.mountWithMockedEnv(<ControlPane diff_type={DiffTypes.DOCUMENTS_DIFF} />, { searchParams: { sourceProjectId: "sourceProjectId", config: "E-Library" } });

    cy.wait('@getConfigurations');

    cy.get('select#configuration').should('be.visible');
    cy.get('select#configuration option').then(options => {
      const expected = ['aTest', 'Default', 'E-Library'];
      const values = [...options].map(o => o.value);
      cy.expect(values).to.deep.eq(expected);

      const textContents = [...options].map(o => o.textContent);
      cy.expect(textContents).to.deep.eq(expected);
    });
    cy.get('select#configuration').should('have.value', 'E-Library');
  })

  it('calls HTML to PDF conversion', () => {
    cy.intercept('POST', '**/conversion/html-to-pdf?orientation=landscape&paperSize=A4', (req) => {
      cy.expect(req.body).to.include('<html'); // Check that HTML was sent to conversion end-point
      req.reply(); // Send stubbed response
    }).as('htmlToPdfConversion');

    cy.mountWithMockedEnv(<ControlPane diff_type={DiffTypes.DOCUMENTS_DIFF} />, {});

    cy.get('.export-controls button').click();

    cy.wait('@htmlToPdfConversion'); // Be sure end-point was called
  })

  it('handles collection document selection', () => {
    const searchParams = {
      sourceSpaceId: "Specification",
      sourceDocument: "Catalog Specification"
    };

    const appContext = { state: {} };
    cy.fixture("pairedDocuments").then((pairedDocuments) => appContext.state.pairedDocuments = pairedDocuments);

    cy.mountWithMockedEnv(<ControlPane diff_type={DiffTypes.COLLECTIONS_DIFF} />, { searchParams: searchParams, appContext: appContext });

    cy.get('select#source-document').should('have.value', 'Specification/Catalog Specification');
    cy.get('@router:push').should("be.calledWithMatch", 'sourceSpaceId=Specification&sourceDocument=Catalog Specification');
  })
})

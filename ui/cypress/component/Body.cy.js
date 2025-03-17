import React, {useContext} from 'react'
import Body from '@/components/Body'
import AppContext from "@/components/AppContext";

function MockComponent({contextWrapper, emulateDataLoaded}) {
  contextWrapper.context = useContext(AppContext);
  if (emulateDataLoaded) {
    contextWrapper.context.state.setDataLoaded(true)
  }
  return <>Mock Component</>;
}

describe('<Body />', () => {
  beforeEach(() => {
    cy.viewport(1600, 1000)
  })

  it('renders body and children', () => {
    cy.mount(<Body><p>children</p></Body>);
    cy.get('body').should('exist');
    cy.get('p').contains('children');
  })

  it('has initial state of application context', () => {
    let contextWrapper = {};
    cy.mount(<Body><MockComponent contextWrapper={contextWrapper} /></Body>).then(() => {
      cy.expect(contextWrapper.context.state.headerPinned).to.equal(false);
      cy.expect(contextWrapper.context.state.controlPaneAccessible).to.equal(false);
      cy.expect(contextWrapper.context.state.controlPaneExpanded).to.equal(false);
      cy.expect(contextWrapper.context.state.pairedDocuments).to.deep.eq([]);
      cy.expect(contextWrapper.context.state.showOutlineNumbersDiff).to.equal(false);
      cy.expect(contextWrapper.context.state.counterpartWorkItemsDiffer).to.equal(false);
      cy.expect(contextWrapper.context.state.compareOnlyMutualFields).to.equal(true);
      cy.expect(contextWrapper.context.state.compareEnumsById).to.equal(false);
      cy.expect(contextWrapper.context.state.allowReferencedWorkItemMerge).to.equal(false);
      cy.expect(contextWrapper.context.state.hideChaptersIfNoDifference).to.equal(true);
      cy.expect(contextWrapper.context.state.dataLoaded).to.equal(false);
      cy.expect(contextWrapper.context.state.diffsExist).to.equal(false);
      cy.expect(contextWrapper.context.state.selectedItemsCount).to.equal(0);
    });
  })

  it('handles dataLoaded', () => {
    let contextWrapper = {};
    cy.mount(<Body><MockComponent contextWrapper={contextWrapper} emulateDataLoaded={true} /></Body>).then(() => {
      cy.expect(contextWrapper.context.state.dataLoaded).to.equal(true);
      cy.expect(contextWrapper.context.state.controlPaneAccessible).to.equal(true);
    });
  })
})

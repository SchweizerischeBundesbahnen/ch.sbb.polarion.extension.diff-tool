import React from 'react'
import ErrorsOverlay from "@/components/ErrorsOverlay";

describe('<ErrorsOverlay />', () => {
  beforeEach(() => {
    cy.viewport(600, 600)
  })

  it('doesnt render in case of no errors', () => {
    cy.mount(<ErrorsOverlay loadingContext={{}} />, {});
    cy.get('div div').should('not.be.visible');
  })

  it('renders in case of errors', () => {
    cy.mount(<ErrorsOverlay loadingContext={{diffsLoadingErrors: true}} />, {});
    cy.get('div div').should('be.visible');
  })
})

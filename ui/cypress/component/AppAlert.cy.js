import React from 'react'
import AppAlert from '@/components/AppAlert'

describe('<AppAlert />', () => {
  beforeEach(() => {
    cy.viewport(1600, 1000)
  })

  it('renders with title', () => {
    cy.mount(<AppAlert title="Test title" />);
    cy.get('h2').contains('Test title');
    cy.get('p').should('not.exist');
  })

  it('renders with title and message', () => {
    cy.mount(<AppAlert title="Test title" message="Test message" />);
    cy.get('h2').contains('Test title');
    cy.get('p').contains('Test message');
  })

})

import React from 'react'
import ExtensionInfo from "@/components/ExtensionInfo";

describe('<ExtensionInfo />', () => {
  beforeEach(() => {
    cy.viewport(600, 600)
  })

  it('renders extension info', () => {
    cy.intercept('**/extension/info/', { version: {
        "bundleVersion": "5.1.2",
        "bundleBuildTimestamp": "2025-03-17 14:48"
      }}).as('getExtensionInfo');

    cy.mountWithMockedEnv(<ExtensionInfo />, {});

    cy.wait('@getExtensionInfo');

    cy.get('div.extension-info').should('be.visible').should("have.text", "v5.1.2 | 2025-03-17 14:48");
  })
})

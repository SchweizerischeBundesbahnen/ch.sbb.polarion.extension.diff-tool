// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })

import { SearchParamsContext } from "next/dist/shared/lib/hooks-client-context.shared-runtime"
import { AppRouterContext } from 'next/dist/shared/lib/app-router-context.shared-runtime';
import * as Router from "next/navigation";
import AppContext from "@/components/AppContext";
import React from "react";
import {useAppContext} from "@/useAppContext";

function AppContextInitializer({appContext}) {
  appContext.state = {...useAppContext(), ...appContext.state};
  return <></>;
}

Cypress.Commands.add('mountWithMockedEnv', (component, { searchParams, appContext, router, options}) => {
  const mockedRouter = router || {};
  if (!mockedRouter.push) {
    mockedRouter.push = cy.stub().as('router:push');
  }

  cy.stub(Router, 'useRouter').returns(mockedRouter);

  appContext = appContext || {};
  return cy.mount(<AppContextInitializer appContext={appContext} />).then(() => {
    return cy.mount(
        <SearchParamsContext.Provider value={new URLSearchParams(searchParams || {})}>
          <AppRouterContext.Provider value={mockedRouter}>
            <AppContext.Provider value={{ state: { ...appContext.state } }}>
              {component}
            </AppContext.Provider>
          </AppRouterContext.Provider>
        </SearchParamsContext.Provider>, options);
  });
})

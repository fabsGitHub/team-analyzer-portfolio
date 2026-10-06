import { mount } from 'cypress/vue'
import { i18n } from '../../src/i18n'
import '@testing-library/cypress/add-commands' // findByRole/Placeholder etc.

const globalStubs = {
  RouterLink: { template: '<a><slot /></a>' },
}


Cypress.Commands.add('mount', (component, options: any = {}) => {
  return mount(component, {
    global: {
      plugins: [i18n],
      stubs: globalStubs,
      ...(options.global ?? {}),
    },
    ...options,
  })
})

declare global {
  namespace Cypress {
    interface Chainable {
      mount: typeof mount
    }
  }
}

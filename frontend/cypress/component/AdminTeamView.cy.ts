describe('AdminTeamView (CT mit API-Mock)', () => {
  it('Team anlegen, Member hinzufügen/entfernen, Leader toggeln, Team löschen', () => {
    cy.wrap(null).then(async () => {
      const api = await import('@/api/teams.api')
      expect((api as any).__isTeamsApiMock, 'api mock flag').to.eq(true)

      const { default: AdminTeamView } = await import('../../src/views/AdminTeamView.vue')
      // returnen -> Cypress wartet
      return cy.mount(AdminTeamView)
    })

    // Initial-Team (mit großzügigem Timeout einmalig)
    cy.contains('article.card', 'Initial Team', { timeout: 10000 }).should('exist')

    // Team anlegen
    cy.get('form').first().within(() => {
      cy.get('input').eq(0).type('Blue Team')
      cy.get('input').eq(1).type('leader-42')
      cy.get('button.btn.primary').click()
    })
    cy.contains('article.card', 'Blue Team').should('exist').within(() => {
      cy.contains('.meta', /leader/i).should('contain.text', '1')
    })

    // Mitglied hinzufügen
    cy.contains('article.card', 'Blue Team').within(() => {
      cy.get('form.grid').within(() => {
        cy.get('input').first().type('u-123')
        cy.get('input[type="checkbox"]').check()
        cy.get('button.btn').first().click()
      })
      cy.get('table tbody').within(() => {
        cy.contains('td code.mono', 'u-123').should('exist')
        cy.contains('tr', 'u-123').within(() => cy.get('.badge').should('exist'))
      })
    })

    // Leader toggeln
    cy.contains('article.card', 'Blue Team').within(() => {
      cy.contains('tr', 'u-123').within(() => cy.get('.cluster .btn').first().click())
      cy.contains('tr', 'u-123').within(() => cy.get('.badge').should('not.exist'))
    })

    // Member entfernen (Dialog kommt aus Alias-Stub)
    cy.contains('article.card', 'Blue Team').within(() => {
      cy.contains('tr', 'u-123').within(() => cy.get('.cluster .btn.danger').click())
    })
    cy.get('[role="dialog"]').should('exist')
    cy.get('[role="dialog"] button').last().click()

    cy.contains('article.card', 'Blue Team')
      .find('table tbody')
      .should('not.contain.text', 'u-123')

    // Team löschen
    cy.contains('article.card', 'Blue Team')
      .find('button.btn.danger.ghost')
      .click()
    cy.get('[role="dialog"]').should('exist')
    cy.get('[role="dialog"] button').last().click()
    cy.contains('article.card', 'Blue Team').should('not.exist')
  })
})
